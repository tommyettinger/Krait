package com.github.tommyettinger;

import java.util.Arrays;

/**
 * Utilities for dealing with n-dimensional Hilbert Curves.
 * Created by Tommy Ettinger on 2/12/2016.
 */
public class HilbertUtility {


    /**
     * Finds the first power of two that is equal to or greater than n.
     * @param n the number to find the next power of two for
     * @return the next power of two after or equal to n
     */
    public static int nextPowerOfTwo(int n)
    {
        int highest = Integer.highestOneBit(n);
        return  (highest == Integer.lowestOneBit(n)) ? highest : highest << 1;
    }
    /**
     * Gray codes are a kind of error-correcting code but are also closely linked to the Hilbert Curve.
     * <br>
     * Source: this is everywhere.
     * @param v value to gray encode
     * @return the gray code
     */
    public static int grayCode(int v)
    {
        return v ^ (v >>> 1);
    }

    /**
     * Reverses a gray code.
     * Source: https://github.com/cortesi/scurve
     * @param v the gray code to invert
     * @return the inverse gray code of v
     */
    public static int inverseGrayCode(int v)
    {
        if(v == 0)
            return v;
        int m = Integer.numberOfTrailingZeros(nextPowerOfTwo(v)) + 1;
        int i = v;
        for (int j = 1; j < m; j++) {
            i ^= (v >>> j);
        }
        return i;
    }

    /**
     * Source: https://github.com/cortesi/scurve
     * @param v value to rotate right
     * @param i amount to rotate by
     * @param width width of area to rotate
     * @return rotated int
     */
    private static int rotateRight(int v, int i, int width)
    {
        i %= width;
        v = (v >>> i) | (v << width - i);
        return v & ((1 << width) - 1);

    }

    /**
     * Source: https://github.com/cortesi/scurve
     * @param v value to rotate left
     * @param i amount to rotate by
     * @param width width of area to rotate
     * @return rotated int
     */
    private static int rotateLeft(int v, int i, int width)
    {
        i %= width;
        v = (v << i) | (v >>> width - i);
        return v & ((1 << width)-1);
    }

    /**
     * Get the number of trailing set bits up to and including width
     * <br>
     * Source: https://github.com/cortesi/scurve
     * @param x the number to evaluate
     * @param width the maximum width to check for set bits
     * @return the number of consecutive one bits from the least significant bit in x
     */
    private static int trailingOnes(int x, int width)
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
    private static int transform(int entry, int direction, int width, int x)
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
    private static int inverseTransform(int entry, int direction, int width, int x)
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
    private static int direction(int x, int n)
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
    private static int entry(int x)
    {
        if(x == 0)
            return 0;
        return grayCode(2 * ((x - 1) / 2));
    }

    /**
     * Takes a distance to travel along a Hilbert curve with the specified dimension count and order (number of bits
     * needed for the length of a side), and returns a int array representing the position in n-dimensional space that
     * corresponds to the point at that distance on this Hilbert curve. This variant does not use a lookup table.
     * <br>
     * Source: https://github.com/cortesi/scurve
     * @param order the number of bits used for a side length, also the order of this Hilbert curve
     * @param dimension the number of dimensions, all of equal length; should be between 2 and 31 inclusive
     * @param distance a int distance
     * @return a int array of coordinates representing a point
     */
    public static int[] distanceToPoint(int order, int dimension, int distance) {
        int hwidth = order * dimension, e = 0, d = 0, w, l, b, w2;
        int[] p = new int[dimension];
        for (int i = 0; i < order; i++) {
            //        w = utils.bitrange(h, hwidth, i*dimension, i*dimension+dimension)
            //x >> (width-end) & ((2**(end-start))-1)
            w = distance >>> (hwidth - (i * dimension + dimension))
                    & ((1 << ((i * dimension + dimension) - i * dimension)) -1);
            l = grayCode(w);
            l = inverseTransform(e, d, dimension, l);
            for (int j = 0; j < dimension; j++) {
                b = l >>> (dimension - (j + 1)) & 1;
                p[j] ^= (-b ^ p[j]) & (1 << (order - 1 - i)); //(order - 1 - i)
            }
            e ^= rotateLeft(entry(w), d+1, dimension);
            d = (d + direction(w, dimension) + 1)% dimension;
        }
        return p;
    }
    /**
     * Takes a distance to travel along a Hilbert curve with the specified dimension count and order (number of bits
     * needed for the length of a side), and returns a int array representing the position in n-dimensional space that
     * corresponds to the point at that distance on this Hilbert curve. This variant does not use a lookup table.
     * <br>
     * Source: https://github.com/cortesi/scurve
     * @param modifying the int array of coordinates to modify
     * @param order the number of bits used for a side length, also the order of this Hilbert curve
     * @param dimension the number of dimensions, all of equal length; should be between 2 and 31 inclusive
     * @param distance a int distance
     * @return a int array of coordinates representing a point, the same value assigned to modifying
     */
    public static int[] distanceToPoint(int[] modifying, int order, int dimension, int distance) {
        int hwidth = order * dimension, e = 0, d = 0, w, l, b, w2;
        Arrays.fill(modifying, 0);
        for (int i = 0; i < order; i++) {
            //        w = utils.bitrange(h, hwidth, i*dimension, i*dimension+dimension)
            //x >> (width-end) & ((2**(end-start))-1)
            w = distance >>> (hwidth - (i * dimension + dimension))
                    & ((1 << ((i * dimension + dimension) - i * dimension)) -1);
            l = grayCode(w);
            l = inverseTransform(e, d, dimension, l);
            for (int j = 0; j < dimension; j++) {
                b = l >>> (dimension - (j + 1)) & 1;
                modifying[j] ^= (-b ^ modifying[j]) & (1 << (order - 1 - i)); //(order - 1 - i)
            }
            e ^= rotateLeft(entry(w), d+1, dimension);
            d = (d + direction(w, dimension) + 1)% dimension;
        }
        return modifying;
    }

    /**
     * Finds the distance along the Hilbert Curve for a point specified as an array or vararg of coordinates, which can
     * have any number of dimensions (equal to the dimension parameter) between 2 and 31. The Hilbert Curve used for
     * the distance will have a side length equal to 2^order; order must be greater than 0.
     * <br>
     * Source: https://github.com/cortesi/scurve
     * @param order the number of bits used for a side length, also the order of this Hilbert curve
     * @param dimension the number of dimensions, all of equal length; should be between 2 and 31 inclusive
     * @param coordinates an array or vararg of int coordinates, starting with x, then y, etc. up to dimension length
     * @return corresponding distance
     */
    public static int pointToDistance(int order, int dimension, int... coordinates)
    {
        int h = 0, e = 0, d = 0, l, w, b;
        for (int i = 0; i < order; i++) {
            l = 0;
            for (int j = 0; j < dimension; j++) {
                b = coordinates[dimension - j - 1] >>> (order - (i + 1)) & 1;
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
