package io;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class IOManager {
	private static String initScript = "AES.script";

	private File inputFile;
	private InputStream inputStream;

	private File outputFile;
	private OutputStream outputStream;

	private byte[] inputFileChunk;
	private ByteArrayInputStream innerInputStream;

	int outputOffset = 0;
	private byte[] outputFileChunk;

	private byte[] currentChunk;
	private byte[] previousChunk;

	/**
	 *
	 * By default, read 10MB at once to avoid high RAM consumption
	 * 
	 **/
	private int bufferSize = 10000000;
	
	/**
	 * 
	 * By default, send 112 bytes at once, the highest value available that can be handled in one transaction
	 * 
	 **/
	private int chunkSize = 112;
	private boolean lastBlock = false;

	public IOManager() {
	}

	public IOManager(int bufferSize) {
		this.bufferSize = bufferSize;
	}

	public void setInputFile(File file) {
		this.inputFile = file;
	}

	public static List<String> loadInitScript() throws IOException {
		List<String> commands = new ArrayList<>();

		try (InputStream is = ClassLoader.getSystemResourceAsStream(initScript);
				InputStreamReader isr = new InputStreamReader(is);
				BufferedReader br = new BufferedReader(isr);) {
			while (br.ready()) {
				commands.add(br.readLine());
			}
		}

		return commands;
	}

	public void initializeStreams(boolean encryption) throws IOException {
		if (this.inputFile != null) {
			this.inputStream = new FileInputStream(this.inputFile);

			String outputFilepath = "";
			String originalFilePath = this.inputFile.getAbsolutePath();

			if (encryption) {
				outputFilepath = originalFilePath + ".enc";
			} else {
				String[] splittedPath = inputFile.getAbsolutePath().split("\\.");
				outputFilepath = splittedPath[0] + "_decrypted";
				for (int i = 1; i < splittedPath.length - 1; i++) {
					outputFilepath += "." + splittedPath[i];
				}
			}

			this.outputFile = new File(outputFilepath);
			outputFile.createNewFile();
			this.outputStream = new FileOutputStream(this.outputFile);

			this.initializeOutputFileChunk();
			this.readChunkFromFile();
		}
	}

	public boolean isLastBlock() throws IOException {
		return this.lastBlock;
	}

	private void readChunkFromFile() throws IOException {
		this.inputFileChunk = this.inputStream.readNBytes(bufferSize);
		this.innerInputStream = new ByteArrayInputStream(this.inputFileChunk);
		this.currentChunk = this.innerInputStream.readNBytes(chunkSize);
		this.lastBlock = false;
	}

	public byte[] readChunk() throws IOException {
		this.previousChunk = this.currentChunk;
		this.currentChunk = this.innerInputStream.readNBytes(chunkSize);

		if (this.currentChunk.length == 0) {
			this.readChunkFromFile();
			if (this.currentChunk.length == 0) {
				this.lastBlock = true;
			}
		}

		return previousChunk;
	}

	private void initializeOutputFileChunk() {
		this.outputFileChunk = new byte[bufferSize];
		Arrays.fill(this.outputFileChunk, (byte) 0x00);
		this.outputOffset = 0;
	}

	public void writeChunk(byte[] chunk) throws IOException {
		if (this.outputOffset + chunk.length >= bufferSize) {
			this.outputStream.write(this.outputFileChunk, 0, this.outputOffset);
			this.initializeOutputFileChunk();
		}

		System.arraycopy(chunk, 0, this.outputFileChunk, this.outputOffset, chunk.length);
		this.outputOffset += chunk.length;

		if (this.lastBlock) {
			this.outputStream.write(this.outputFileChunk, 0, this.outputOffset);
		}
	}

	public void closeStreams() throws IOException {
		this.innerInputStream.close();
		this.outputStream.close();
		this.inputStream.close();
	}
}