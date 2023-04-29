package com.filtershekanha.tlsfragmenter.utility;

import java.math.BigInteger;

public class HexFormatter {

    public static String toHexString(byte[] byteArray) {
        BigInteger bigInt = new BigInteger(1, byteArray);
        return bigInt.toString(16);
    }

    public static String arrayToHexString(int[] intArray) {
        StringBuilder sb = new StringBuilder();
        sb.append('[');
        for (int i = 0; i < intArray.length; i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(String.format("0x%04X", intArray[i]));
        }
        sb.append(']');
        return sb.toString();
    }

    public static String arrayToHexString(short[] shortArray) {
        StringBuilder sb = new StringBuilder();
        sb.append('[');
        for (int i = 0; i < shortArray.length; i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(String.format("0x%04X", shortArray[i] & 0xFFFF));
        }
        sb.append(']');
        return sb.toString();
    }
}
