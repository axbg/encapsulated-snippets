package io;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.List;

import com.sun.javacard.apduio.Apdu;
import com.sun.javacard.apduio.CadClientInterface;
import com.sun.javacard.apduio.CadDevice;
import com.sun.javacard.apduio.CadTransportException;

import io.utils.AESMode;
import io.utils.ByteUtils;
import io.utils.JCardException;

public class JCardManager {
	private int port = 9025;
	private String address = "localhost";

	private boolean initialized = false;

	private AESMode mode;

	private CadClientInterface cad;
	private Socket socket;
	private InputStream is;
	private OutputStream os;

	public JCardManager() throws UnknownHostException, IOException, CadTransportException {
		initializeConnection();
	}

	public JCardManager(String address, int port) throws UnknownHostException, IOException, CadTransportException {
		this.address = address;
		this.port = port;
		initializeConnection();
	}

	public void setMode(AESMode mode) {
		this.mode = mode;
	}

	private void initializeConnection() throws UnknownHostException, IOException, CadTransportException {
		this.socket = new Socket(address, port);
		this.is = socket.getInputStream();
		this.os = socket.getOutputStream();

		cad = CadDevice.getCadClientInstance(CadDevice.PROTOCOL_T1, is, os);
		cad.powerUp();
	}

	public void closeConnection() throws IOException, CadTransportException {
		if (initialized) {
			cad.powerDown();
			os.close();
			is.close();
			socket.close();
			initialized = false;
		}
	}

	public void loadScripts() throws IOException, CadTransportException, JCardException {
		List<String> commands = IOManager.loadInitScript();

		this.initialized = true;

		for (String s : commands) {
			exchangeCommand(ByteUtils.hexArrayToByteArray(s));
		}
	}

	private byte[] buildHeader(boolean lastBlock) {
		byte[] header = new byte[4];
		header[0] = (byte) 0x80;
		header[1] = (byte) 0x50;

		switch (this.mode) {
		case SET_PASSWORD:
			break;
		case ENCRYPT:
			header[1] = (byte) 0x52;
			break;
		case DECRYPT:
			header[1] = (byte) 0x54;
			break;
		case RESET_PASSWORD:
			header[1] = (byte) 0x56;
			break;
		}

		header[2] = lastBlock ? (byte) 0x00 : (byte) 0x01;
		header[3] = (byte) 0x00;
		return header;
	}

	public byte[] prepareCommand(byte[] message, boolean lastBlock) {
		if (message == null) {
			message = new byte[] { 0x00 };
		}

		byte[] command = new byte[5 + message.length];

		byte[] header = buildHeader(lastBlock);
		for (int i = 0; i < 4; i++) {
			command[i] = header[i];
		}

		command[4] = (byte) message.length;

		for (int i = 0; i < message.length; i++) {
			command[i + 5] = message[i];
		}

		return command;
	}

	public byte[] exchangeCommand(byte[] message) throws IOException, CadTransportException, JCardException {
		Apdu apdu = new Apdu();

		if (initialized) {
			apdu.command = Arrays.copyOfRange(message, 0, 4);
			apdu.setDataIn(Arrays.copyOfRange(message, 5, 5 + message[4]), message[4]);
			apdu.setLe(127);

			cad.exchangeApdu(apdu);
		} else {
			throw new JCardException("The card was not initialized");
		}

		if (apdu.sw1sw2[0] != -112 || apdu.sw1sw2[1] != 0) {
			throw new JCardException(
					"The card returned an error code: " + String.format("%02X %02X", apdu.sw1sw2[0], apdu.sw1sw2[1]));
		}

		return apdu.dataOut;
	}
}