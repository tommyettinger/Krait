package com.github.tommyettinger;

/**
 * Created by Tommy Ettinger on 2/12/2016.
 */
public class HilbertUtility {


    /**
     * Finds the first power of two that is equal to or greater than n.
     * @param n the number to find the next power of two for
     * @return the next power of two after or equal to n
     */
    public static long nextPowerOfTwo(long n)
    {
        long highest = Long.highestOneBit(n);
        return  (highest == Long.lowestOneBit(n)) ? highest : highest << 1;
    }
    /**
     * Gray codes are a kind of error-correcting code but are also closely linked to the Hilbert Curve.
     * <br>
     * Source: this is everywhere.
     * @param v value to gray encode
     * @return the gray code
     */
    public static long grayCode(long v)
    {
        return v ^ (v >>> 1);
    }

    /**
     * Source: https://github.com/cortesi/scurve
     * @param v value to rotate right
     * @param i amount to rotate by
     * @param width width of area to rotate
     * @return rotated long
     */
    private static long rotateRight(long v, long i, long width)
    {
        i %= width;
        v = (v >>> i) | (v << width - i);
        return v & (1 << width);

    }

    /**
     * Source: https://github.com/cortesi/scurve
     * @param v value to rotate left
     * @param i amount to rotate by
     * @param width width of area to rotate
     * @return rotated long
     */
    private static long rotateLeft(long v, long i, long width)
    {
        i %= width;
        v = (v << i) | (v >>> width - i);
        return v & (1 << width);
    }

    /**
     * Get the number of trailing set bits up to and including width
     * <br>
     * Source: https://github.com/cortesi/scurve
     * @param x the number to evaluate
     * @param width the maximum width to check for set bits
     * @return the number of consecutive one bits from the least significant bit in x
     */
    private static int trailingOnes(long x, long width)
    {
        int i = 0;
        while((x & 1) == 1 && i <= width) {
            x >>>= 1;
            i++;
        }
        return i;
    }

    /**
     * Hilbert stuff
     * <br>
     * Source: https://github.com/cortesi/scurve
     * @param entry index we're entering from
     * @param direction direction the sub-region heads in
     * @param width related to order/bits
     * @param x a gray code thing maybe?
     * @return something hilbert-y, probably part of an index
     */
    private static long transform(long entry, long direction, long width, long x)
    {
        return rotateRight((x ^ entry), direction + 1, width);
    }
    /**
     * Hilbert stuff backwards
     * <br>
     * Source: https://github.com/cortesi/scurve
     * @param entry index we're entering from
     * @param direction direction the sub-region heads in
     * @param width related to order/bits
     * @param x a gray code thing maybe?
     * @return something hilbert-y, probably part of an index, but inverted
     */
    private static long inverseTransform(long entry, long direction, long width, long x)
    {
        return rotateLeft(x, direction + 1, width) ^ entry;
    }

    /**
     * Finds the direction the curve will head in next.
     * <br>
     * Source: https://github.com/cortesi/scurve
     * @param x a hilbert index probably
     * @param n related to dimensions
     * @return the direction to go in
     */
    private static long direction(long x, long n)
    {
        if(x == 0) return 0;
        if(x % 2 == 0) return trailingOnes(x - 1, n) % n;
        return trailingOnes(x, n) % n;
    }

    /**
     * I have no idea. Probably something for finding what part of a section of the curve is the entry?
     * <br>
     * Source: https://github.com/cortesi/scurve
     * @param x probably a hilbert index
     * @return a weird gray code for some reason, related to other stuff I suppose
     */
    private static long entry(long x)
    {
        if(x == 0)
            return 0;
        return grayCode(2 * ((x - 1) / 2));
    }

    /**
     * Takes a distance to travel along the 2D Hilbert curve and returns a long array representing the position in 2D
     * space that corresponds to that point on the Hilbert curve. This variant does not use a lookup table, but is
     * likely slower than other versions that only work on curves with at most 2^16 side length.
     * <br>
     * Source: https://github.com/cortesi/scurve
     * @param distance a long distance
     * @return a long array of coordinates representing a point
     */
    public static long[] distanceToPoint(int bits, int dimension, long distance) {
        long hwidth = bits * dimension, e = 0, d = 0, w, l, b;
        long[] p = new long[dimension];
        for (int i = 0; i < bits; i++) {
            w = distance >>> (hwidth - (i * dimension + dimension))
                    & ((2 << ((i * dimension + dimension) - i * dimension)) -1);
            l = grayCode(w);
            l = inverseTransform(e, d, 2, l);
            for (int j = 0; j < dimension; j++) {
                b = l >>> (dimension - (j + 1)) & 1;
                p[j] ^= (-b ^ p[j]) & (1 << (bits - 1 - i));
            }
            e ^= rotateLeft(entry(w), d+1, dimension);
            d = (d + direction(w, dimension) + 1)% dimension;
        }
        return p;
    }

    /**
     * Finds the distance along the Hilbert Curve for an x, y point.
     * <br>
     * Source: https://github.com/cortesi/scurve
     * @param coordinates an array or vararg of long coordinates, which should always be two elements (x, y)
     * @return corresponding distance
     */
    public static long pointToDistance(int bits, int dimension, long... coordinates)
    {
        long h = 0, e = 0, d = 0, l, w, b;
        for (int i = 0; i < bits; i++) {
            l = 0;
            for (int j = 0; j < dimension; j++) {
                b = coordinates[dimension - j - 1] >>> (bits - (i + 1)) & 1;
                l |= b << j;
            }
            l = transform(e, d, dimension, l);
            w = grayCode(l);
            e ^= rotateLeft(entry(w), d + 1, dimension);
            d = (d + direction(w, dimension) + 1) % dimension;
            h = (h << dimension) | w;
        }
        return h;
    }
}
