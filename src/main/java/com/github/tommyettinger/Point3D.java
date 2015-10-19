package com.github.tommyettinger;

/**
 * Created by Tommy Ettinger on 10/18/2015.
 */
public class Point3D {
    public int x;
    public int y;
    public int z;

    public Point3D(int x, int y, int z)
    {
        this.x = x;
        this.y = y;
        this.z = z;
    }
    public int getMorton()
    {
        return encodeMorton(x, y, z);
    }
    public static int encodeMorton( int index1, int index2, int index3 )
    { // pack 3 10-bit indices into a 30-bit Morton code
        index1 &= 0x000003ff;
        index2 &= 0x000003ff;
        index3 &= 0x000003ff;
        index1 |= ( index1 << 16 );
        index2 |= ( index2 << 16 );
        index3 |= ( index3 << 16 );
        index1 &= 0x030000ff;
        index2 &= 0x030000ff;
        index3 &= 0x030000ff;
        index1 |= ( index1 << 8 );
        index2 |= ( index2 << 8 );
        index3 |= ( index3 << 8 );
        index1 &= 0x0300f00f;
        index2 &= 0x0300f00f;
        index3 &= 0x0300f00f;
        index1 |= ( index1 << 4 );
        index2 |= ( index2 << 4 );
        index3 |= ( index3 << 4 );
        index1 &= 0x030c30c3;
        index2 &= 0x030c30c3;
        index3 &= 0x030c30c3;
        index1 |= ( index1 << 2 );
        index2 |= ( index2 << 2 );
        index3 |= ( index3 << 2 );
        index1 &= 0x09249249;
        index2 &= 0x09249249;
        index3 &= 0x09249249;
        return( index1 | ( index2 << 1 ) | ( index3 << 2 ) );
    }
    public static Point3D decodeMorton(final int morton)
    {
        // unpack 3 10-bit indices from a 30-bit Morton code
        int value1 = morton;
        int value2 = ( value1 >> 1 );
        int value3 = ( value1 >> 2 );
        value1 &= 0x09249249;
        value2 &= 0x09249249;
        value3 &= 0x09249249;
        value1 |= ( value1 >> 2 );
        value2 |= ( value2 >> 2 );
        value3 |= ( value3 >> 2 );
        value1 &= 0x030c30c3;
        value2 &= 0x030c30c3;
        value3 &= 0x030c30c3;
        value1 |= ( value1 >> 4 );
        value2 |= ( value2 >> 4 );
        value3 |= ( value3 >> 4 );
        value1 &= 0x0300f00f;
        value2 &= 0x0300f00f;
        value3 &= 0x0300f00f;
        value1 |= ( value1 >> 8 );
        value2 |= ( value2 >> 8 );
        value3 |= ( value3 >> 8 );
        value1 &= 0x030000ff;
        value2 &= 0x030000ff;
        value3 &= 0x030000ff;
        value1 |= ( value1 >> 16 );
        value2 |= ( value2 >> 16 );
        value3 |= ( value3 >> 16 );
        value1 &= 0x000003ff;
        value2 &= 0x000003ff;
        value3 &= 0x000003ff;
        return new Point3D(value1, value2, value3);
    }
}
