package eu.ase.jcard;

import javacard.framework.APDU;
import javacard.framework.Applet;
import javacard.framework.ISO7816;
import javacard.framework.ISOException;
import javacard.framework.JCSystem;
import javacard.framework.Util;
import javacard.security.AESKey;
import javacard.security.KeyBuilder;
import javacardx.annotations.StringDef;
import javacardx.annotations.StringPool;
import javacardx.crypto.Cipher;

/**
 * APDU Structure CLA INS P1 P2 Lc InputData Le OutputData; ex: 0x80 0x50 0x00
 * 0x00 0x14 ......... 0x7F ..........; Lc = Input data length | Le = Output data length
 * 
 * Conventional rules for our case:
 	* INS shows which action should be taken: 
 		* 0x50 - set password & initialize key 
 		* 0x52 - encrypt chunk
 		* 0x54 - decrypt chunk
 		* 0x56 - remove key
 	* P1 shows if the current chunk is the last one
 		* 0x00 - last block
 		* 0x01 - more blocks to come
 **/
@StringPool(value = { @StringDef(name = "Package", value = "eu.ase.jcard"),
		@StringDef(name = "AppletName", value = "MyApplet") }, name = "MyAppletStrings")
public class MyApplet extends Applet {

	private static final short BLOCK_SIZE = 16;

	private AESKey key;
	private Cipher cipher;
	private byte[] previousBlock;
	private byte[] data;
	
	/**
	 * 
	 * Will store the actual size of usable bytes in the buffers.
	 * 
	 **/
	private int dataSize;

	public static void install(byte[] bArray, short bOffset, byte bLength) {
		new MyApplet().register();
	}

	protected MyApplet() {
		/**
		 * Because a smart card has a very limited memory, a big file cannot be encrypted
		 * at once. An easy workaround would be to use ECB, which can encrypt each block
		 * independently. 
		 * ECB is not secure, so CBC was the only option. 
		 * To avoid memory limitations, instead of applying AES on the whole message, I applied it
		 * on maximum 7 blocks at a time using the last encrypted block in the previous round as IV.
		 * 
		 * In the end, it works like a regular AES-128-CBC.
		 * 
		 * I also implemented PKCS7 manually, because the simulator supports only NO_PAD
		 * 
		 **/
		this.key = null;
		this.cipher = Cipher.getInstance(Cipher.ALG_AES_BLOCK_128_CBC_NOPAD, false);

		/**
		 * 
		 * As the card cannot read, at once, more than 128 bytes without calling the additional receiveBytes()
		 * More so, the simulator throws an error each time the total size of an APDU is bigger than 261
		 * 4 header bytes + Lc + input data + Le + output data.
		 * I couldn't find more information regarding the limits of APDU size, but the following post 
		 * confirmed the "experiments" I've been doing and the results I've obtained: 
		 	https://stackoverflow.com/questions/32994936/safe-max-java-card-apdu-data-command-and-respond-size
		 * 
		 **/
		this.data = JCSystem.makeTransientByteArray((short) 128, JCSystem.CLEAR_ON_RESET);
		
		/**
		 * 
		 *  Will be used as IV for each sequence of blocks and will store the last encrypted block.
		 *  
		 **/
		this.previousBlock = JCSystem.makeTransientByteArray(BLOCK_SIZE, JCSystem.CLEAR_ON_RESET);

		/**
		 * Initial value for IV is 0x00 - it was easier to test this way, but any other
		 * value can be used. A possible improvement would be to receive the IV from the
		 * client.
		 * 
		 **/
		Util.arrayFillNonAtomic(this.previousBlock, (short) 0, BLOCK_SIZE, (byte) 0x00);
	}

	@Override
	public void process(APDU apdu) {
		if (selectingApplet()) {
			return;
		}

		byte[] buffer = apdu.getBuffer();
		switch (buffer[ISO7816.OFFSET_INS]) {
		case (byte) 0x50:
			setKey(buffer, apdu.setIncomingAndReceive());
			break;
		case (byte) 0x52:
			this.processMessage(apdu, Cipher.MODE_ENCRYPT);
			break;
		case (byte) 0x54:
			this.processMessage(apdu, Cipher.MODE_DECRYPT);
			break;
		case (byte) 0x56:
			this.removeKey();
			break;
		default:
			ISOException.throwIt(ISO7816.SW_INS_NOT_SUPPORTED);
		}
	}

	/**
	 *
	 * Computes the PKCS7 padding value and extends the last block.
	 *
	 **/
	private void applyPKCS7Padding() {
		short quotient = (short) (this.dataSize / BLOCK_SIZE);
		short remainder = (short) (this.dataSize % BLOCK_SIZE);

		short offset = (short) (quotient * BLOCK_SIZE);
		short length = remainder == 0 ? BLOCK_SIZE : remainder;

		if (length == BLOCK_SIZE) {
			Util.arrayFillNonAtomic(this.data, offset, BLOCK_SIZE, (byte) 0x10);
			this.dataSize += BLOCK_SIZE;
		} else {
			int requiredPadBytes = BLOCK_SIZE - length;
			for (short i = (short) (offset + BLOCK_SIZE); i >= offset + length; i--) {
				this.data[i] = (byte) requiredPadBytes;
			}
			
			this.dataSize += requiredPadBytes;
		}
	}
	
	/**
	 *
	 * Removes the PKCS7 padding from the last decrypted block.
	 * 
	 **/
	private void removePKCS7Padding() {
		short encounters = 1;
		byte padValue = this.data[(short)(this.dataSize - 1)];
		for (short i = (short) (this.dataSize - 2); i >= 0; i--) {
			if (this.data[i] == padValue) {
				encounters += 1;
			} else {
				break;
			}
		}

		this.dataSize -= encounters;
	}

	private void updateData(byte[] buffer, short dataOffset) {
		Util.arrayCopy(buffer, dataOffset, this.data, (short) 0, (short) this.dataSize);
	}

	private void processLastBlock(byte[] buffer, short dataOffset, short len, byte mode) {
		/**
		 * 
		 * If we process the last sequence of blocks, padding should be applied or removed 
		 	* encryption - padding should be applied before encryption 
		 	* decryption - padding should be removed after decryption
		 * 
		 **/
		if (mode == Cipher.MODE_ENCRYPT) {
			applyPKCS7Padding();
			computeAES(mode);
		} else if (mode == Cipher.MODE_DECRYPT) {
			computeAES(mode);
			removePKCS7Padding();
		}

		/**
		 * 
		 * After the last block, the previoous block is removed
		 * 
		 **/
		Util.arrayFillNonAtomic(this.previousBlock, (short) 0, BLOCK_SIZE, (byte) 0x00);
	}

	private void updateChunk(byte[] buffer, short dataOffset, short len, boolean lastBlock, byte mode) {
		this.dataSize = len;
		updateData(buffer, dataOffset);

		if (lastBlock) {
			processLastBlock(buffer, dataOffset, len, mode);
		} else {
			computeAES(mode);
		}
	}

	private void computeAES(byte mode) {
		cipher.init(this.key, mode, this.previousBlock, (short) 0, BLOCK_SIZE);

		/**
		 * 
		 * Saves the last encrypted block from current sequence in the previousBlock buffer before decryption
		 * 
		 **/
		if (mode == Cipher.MODE_DECRYPT) {
			Util.arrayCopy(this.data, (short) (this.dataSize - BLOCK_SIZE), this.previousBlock, (short) 0, BLOCK_SIZE);
		}

		cipher.doFinal(this.data, (short) 0, (short) this.dataSize, this.data, (short) 0);

		/**
		 * 
		 * Saves the last encrypted block from current sequence in the previousBlock buffer after encryption
		 * 
		 **/
		if (mode == Cipher.MODE_ENCRYPT) {
			Util.arrayCopy(this.data, (short) (this.dataSize - BLOCK_SIZE), this.previousBlock, (short) 0, BLOCK_SIZE);
		}
	}

	private void sendResponse(APDU apdu) {
		Util.arrayCopy(this.data, (short) 0, apdu.getBuffer(), (short) 0, (short) this.dataSize);
		apdu.setOutgoingAndSend((short) 0, (short) this.dataSize);
	}

	private void setKey(byte[] buffer, short len) {
		if (len != 16) {
			ISOException.throwIt(ISO7816.SW_DATA_INVALID);
		}

		byte[] password = JCSystem.makeTransientByteArray((short) BLOCK_SIZE, JCSystem.CLEAR_ON_DESELECT);
		Util.arrayCopy(buffer, ISO7816.OFFSET_CDATA, password, (short) 0, BLOCK_SIZE);

		this.key = (AESKey) KeyBuilder.buildKey(KeyBuilder.TYPE_AES, KeyBuilder.LENGTH_AES_128, false);
		this.key.setKey(password, (short) 0);
	}

	private void processMessage(APDU apdu, byte mode) {
		boolean lastBlock = ((apdu.getBuffer()[ISO7816.OFFSET_P1] & 0x01) == 0);
		short length = apdu.setIncomingAndReceive();

		if (this.key == null) {
			ISOException.throwIt(ISO7816.SW_DATA_INVALID);
		}

		updateChunk(apdu.getBuffer(), ISO7816.OFFSET_CDATA, length, lastBlock, mode);
		sendResponse(apdu);
	}

	private void removeKey() {
		this.key = null;
	}
}