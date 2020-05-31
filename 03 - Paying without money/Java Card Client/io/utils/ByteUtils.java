package io.utils;

public class ByteUtils {
	private static byte hexToByte(String hex) {
		int first = Character.digit(hex.charAt(0), 16);
		int second = Character.digit(hex.charAt(1), 16);
		return (byte) ((first << 4) + second);
	}

	public static byte[] hexArrayToByteArray(String hex) {
		String[] hexBytes = hex.replaceAll("0x", "").split(" ");
		byte[] result = new byte[hexBytes.length];

		for (int i = 0; i < hexBytes.length; i++) {
			result[i] = hexToByte(hexBytes[i]);
		}

		return result;
	}
}