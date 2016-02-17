package com.github.tommyettinger;

/**
 * Created by Tommy Ettinger on 2/15/2016.
 */
public class Conversion {


    private static final short MASK8 = 0xFF;
    private static final int MASK16 = 0xFFFF;
    private static final long MASK32 = 0xFFFFFFFFL;

    public static byte[] toBytes(short[] values)
    {
        byte[] out = new byte[values.length];
        for (int i = 0; i < values.length; i++) {
            out[i] = (byte)values[i];
        }
        return out;
    }
    public static byte[] toBytes(int[] values)
    {
        byte[] out = new byte[values.length];
        for (int i = 0; i < values.length; i++) {
            out[i] = (byte)values[i];
        }
        return out;
    }
    public static byte[] toBytes(long[] values)
    {
        byte[] out = new byte[values.length];
        for (int i = 0; i < values.length; i++) {
            out[i] = (byte)values[i];
        }
        return out;
    }


    public static short[] toShorts(byte[] values)
    {
        short[] out = new short[values.length];
        for (int i = 0; i < values.length; i++) {
            out[i] = (short) (MASK8 & values[i]);
        }
        return out;
    }
    public static short[] toShorts(int[] values)
    {
        short[] out = new short[values.length];
        for (int i = 0; i < values.length; i++) {
            out[i] = (short) values[i];
        }
        return out;
    }
    public static short[] toShorts(long[] values)
    {
        short[] out = new short[values.length];
        for (int i = 0; i < values.length; i++) {
            out[i] = (short) values[i];
        }
        return out;
    }

    public static int[] toInts(byte[] values)
    {
        int[] out = new int[values.length];
        for (int i = 0; i < values.length; i++) {
            out[i] = (MASK16 & values[i]);
        }
        return out;
    }
    public static int[] toInts(short[] values)
    {
        int[] out = new int[values.length];
        for (int i = 0; i < values.length; i++) {
            out[i] = (MASK16 & values[i]);
        }
        return out;
    }
    public static int[] toInts(long[] values)
    {
        int[] out = new int[values.length];
        for (int i = 0; i < values.length; i++) {
            out[i] = (int)values[i];
        }
        return out;
    }

    public static long[] toLongs(byte[] values)
    {
        long[] out = new long[values.length];
        for (int i = 0; i < values.length; i++) {
            out[i] = (MASK32 & values[i]);
        }
        return out;
    }
    public static long[] toLongs(short[] values)
    {
        long[] out = new long[values.length];
        for (int i = 0; i < values.length; i++) {
            out[i] = (MASK32 & values[i]);
        }
        return out;
    }
    public static long[] toLongs(int[] values)
    {
        long[] out = new long[values.length];
        for (int i = 0; i < values.length; i++) {
            out[i] = (MASK32 & values[i]);
        }
        return out;
    }
}