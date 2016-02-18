package com.github.tommyettinger;

/**
 * Created by Tommy Ettinger on 10/18/2015.
 */
public class Hilbert {


    /**
     * Encode a number n as a Gray code; Gray codes have a relation to the Hilbert curve and may be useful.
     * Source: http://xn--2-umb.com/15/hilbert , http://aggregate.org/MAGIC/#Gray%20Code%20Conversion
     * @param n any int
     * @return the gray code for n
     */
    public static int grayEncode(int n){
        return n ^ (n >> 1);
    }

    /**
     * Decode a number from a Gray code n; Gray codes have a relation to the Hilbert curve and may be useful.
     * Source: http://xn--2-umb.com/15/hilbert , http://aggregate.org/MAGIC/#Gray%20Code%20Conversion
     * @param n a gray code, as produced by grayEncode
     * @return the decoded int
     */
    public static int grayDecode(int n) {
        int p = n;
        while ((n >>= 1) != 0)
            p ^= n;
        return p;
    }

    /**
     * Based off https://github.com/cortesi/scurve/blob/master/scurve/hilbert.py
     * @param entry
     * @param direction
     * @param x
     * @return
     */
    public static int transform(int entry, int direction, int x)
    {
        return Integer.rotateRight(x ^ entry, direction + 1);
    }

    /**
     * Based off https://github.com/cortesi/scurve/blob/master/scurve/hilbert.py
     * @param entry
     * @param direction
     * @param x
     * @return
     */
    public static int transformInverse(int entry, int direction, int x)
    {
        return Integer.rotateLeft(x, direction + 1) ^ entry;
    }

    /**
     * Based off https://github.com/cortesi/scurve/blob/master/scurve/hilbert.py
     * @param x
     * @param n
     * @return
     */
    public static int direction(int x, int n)
    {
        if(x <= 0)
            return 0;
        return Integer.numberOfTrailingZeros(~((x-1)|1)) % n;
    }

    /**
     * Based off https://github.com/cortesi/scurve/blob/master/scurve/hilbert.py
     * @param x
     * @return
     */
    public static int entry(int x)
    {
        if(x <= 0)
            return 0;
        return grayEncode(2 * ((x - 1) / 2));
    }

    /**
     * From http://and-what-happened.blogspot.nl/2011/08/fast-2d-and-3d-hilbert-curves-and.html
     * @param point a Morton-coded 3D point in 1024^3 space
     * @return a distance along the 3D Hilbert Curve
     */
    public static int point3DToHilbert(final int point) {
        int hilbert = point;
        int block = 27;
        int hcode = ((hilbert >> block) & 7);
        int mcode, shift, signs;
        shift = signs = 0;
        while (block > 0) {
            block -= 3;
            hcode <<= 2;
            mcode = ((0x20212021 >> hcode) & 3);
            shift = ((0x48 >> (7 - shift - mcode)) & 3);
            signs = ((signs | (signs << 3)) >> mcode);
            signs = ((signs ^ (0x53560300 >> hcode)) & 7);
            mcode = ((hilbert >> block) & 7);
            hcode = mcode;
            hcode = (((hcode | (hcode << 3)) >> shift) & 7);
            hcode ^= signs;
            hilbert ^= ((mcode ^ hcode) << block);
        }

        hilbert ^= ((hilbert >> 1) & 0x92492492);
        hilbert ^= ((hilbert & 0x92492492) >> 1);
        return (hilbert);
    }

    /**
     * From http://and-what-happened.blogspot.nl/2011/08/fast-2d-and-3d-hilbert-curves-and.html
     * @param point a Coord3D in 1024^3 space
     * @return a distance along the 3D Hilbert Curve
     */
    public static int point3DToHilbert(final Coord3D point) {
        return point3DToHilbert(point.getMorton());
    }
    /**
     * From http://and-what-happened.blogspot.nl/2011/08/fast-2d-and-3d-hilbert-curves-and.html
     * @param hilbert a distance along the 3D Hilbert Curve
     * @return a Morton-coded 3D point in 1024^3 space.
     */
    public static int hilbertToMorton(final int hilbert) {
        int morton = hilbert;
        morton ^= ((morton & 0x92492492) >> 1);
        morton ^= ((morton >> 1) & 0x92492492);

        int block = 27;
        int hcode = ((morton >> block) & 7);
        int mcode, shift, signs;
        shift = signs = 0;
        while (block > 0) {
            block -= 3;
            hcode <<= 2;
            mcode = ((0x20212021 >> hcode) & 3);
            shift = ((0x48 >> (4 - shift + mcode)) & 3);
            signs = ((signs | (signs << 3)) >> mcode);
            signs = ((signs ^ (0x53560300 >> hcode)) & 7);
            hcode = ((morton >> block) & 7);
            mcode = hcode;
            mcode ^= signs;
            mcode = (((mcode | (mcode << 3)) >> shift) & 7);
            morton ^= ((hcode ^ mcode) << block);
        }

        return morton;
    }

    /**
     * From http://and-what-happened.blogspot.nl/2011/08/fast-2d-and-3d-hilbert-curves-and.html
     * @param hilbert a distance along the 3D Hilbert Curve
     * @return a 3D point in 1024^3 space.
     */
    public static Coord3D hilbertToPoint3D(final int hilbert) {
        return Coord3D.decodeMorton(hilbertToMorton(hilbert));
    }
}
