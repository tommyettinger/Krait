package com.github.tommyettinger;

import java.util.Arrays;

/**
 * A CurveStrategy for n-dimensional Hilbert curves occupying a hypercube with power-of-two side length. Total distance
 * through the Hilbert Curve must be no greater than 2^63 (in hex, 0x4000000000000000); this is determined by the side
 * length raised to the power of the dimension count. If a non-power-of-two length is requested, the first greater power
 * of two is used for a side length. If the side length would be too large, the size is shrunk to fit in a total
 * distance of 2^63 or less, potentially much less if there is no possible Hilbert Curve until a smaller one is found.
 * One example of this is if 8 dimensions are requested, then side length can be no greater than 2^7, with a total
 * distance of 2^56, because any larger side length that is a power of two would move the total distance up to past the
 * maximum of 2^63.
 * Created by Tommy Ettinger on 2/11/2016.
 */
public class HilbertGeneralStrategy extends CurveStrategy {

    private byte[] bDist;
    private short[] sDist;
    private int[] iDist;
    private byte[][] bVals;
    private short[][] sVals;
    private int[][] iVals;

    /**
     * Equivalent to the order of this Hilbert Curve
     */
    private int bits;
    /**
     * Side length of the square.
     */
    private long side;
    public final int DIMENSION;

    /**
     * Constructs a HilbertGeneralStrategy with 3 dimensions and side length 32, which will pre-calculate the 2^15 points of that Hilbert
     * Curve and store their x coordinates, y coordinates, z coordinates, and distances in short arrays.
     */
    public HilbertGeneralStrategy()
    {
        this(3, 32);
    }

    /**
     * Constructs a HilbertGeneralStrategy with the specified dimension count and side length, potentially pre-
     * calculating the Hilbert Curve to improve performance if the total length, sideLength ^ dimension, is no greater
     * than 2 ^ 24.
     * @param dimension the number of dimensions to use, all with equal length; must be between 2 and 31
     * @param sideLength the length of a side, which will be rounded up to the next-higher power of two if it isn't
     *                   already a power of two. sideLength ^ dimension must be no greater than 2^63.
     */
    public HilbertGeneralStrategy(int dimension, long sideLength) {
        if(dimension > 31)
            dimension = 31;
        DIMENSION = dimension;
        if(sideLength <= 1)
        {
            sideLength = 2;
        }

        side = HilbertUtility.nextPowerOfTwo(sideLength);
        bits = Long.numberOfTrailingZeros(side);
        if(side <= 0x8000000000000000L || bits * DIMENSION > 62)
        {
            bits = 62 / DIMENSION;
            side = 1 << bits;
        }

        dimensionality = new long[DIMENSION];
        Arrays.fill(dimensionality, side);
        maxDistance = 1 << (bits * DIMENSION);
        if (maxDistance <= 0x100) {
            bVals = new byte[(int)maxDistance][DIMENSION];
            bDist = new byte[(int)maxDistance];
            long[] c;
            for (int i = 0; i < maxDistance; i++) {
                c = HilbertUtility.distanceToPoint(bits, DIMENSION, i);
                for (int j = 0; j < DIMENSION; j++) {
                    bVals[i][j] = (byte) c[j];
                }
                bDist[lookup(c)] = (byte) i;
            }
            stored = true;
        }
        else if (maxDistance <= 0x10000) {
            sVals = new short[(int)maxDistance][DIMENSION];
            sDist = new short[(int)maxDistance];
            long[] c;
            for (int i = 0; i < maxDistance; i++) {
                c = HilbertUtility.distanceToPoint(bits, DIMENSION, i);
                for (int j = 0; j < DIMENSION; j++) {
                    sVals[i][j] = (short) c[j];
                }
                sDist[lookup(c)] = (short) i;
            }
            stored = true;
        }
        else if (maxDistance <= 0x1000000) {
            iVals = new int[(int)maxDistance][DIMENSION];
            iDist = new int[(int)maxDistance];
            long[] c;
            for (int i = 0; i < maxDistance; i++) {
                c = HilbertUtility.distanceToPoint(bits, DIMENSION, i);
                for (int j = 0; j < DIMENSION; j++) {
                    iVals[i][j] = (int) c[j];
                }
                iDist[lookup(c)] = (int) i;
            }
            stored = true;

        }
        else
        {
            stored = false;
        }
    }

    private int lookup(long... point)
    {
        int v = 0;
        for (int i = 0; i < DIMENSION; i++) {
            v += point[i] << (bits * i);
        }
        return v;
    }
    private long[] toPoint(byte... coordinates)
    {
        long[] pt = new long[coordinates.length];
        for (int i = 0; i < coordinates.length; i++) {
            pt[i] = coordinates[i];
        }
        return pt;
    }
    private long[] toPoint(short... coordinates)
    {
        long[] pt = new long[coordinates.length];
        for (int i = 0; i < coordinates.length; i++) {
            pt[i] = coordinates[i];
        }
        return pt;
    }
    private long[] toPoint(int... coordinates)
    {
        long[] pt = new long[coordinates.length];
        for (int i = 0; i < coordinates.length; i++) {
            pt[i] = coordinates[i];
        }
        return pt;
    }

    /**
     * Given a distance to travel along this space-filling curve, gets the corresponding point as an array of long
     * coordinates, typically in x, y, z... order. The length of the long array this returns is equivalent to the length
     * of the dimensionality field, and no elements in the returned array should be equal to or greater than the
     * corresponding element of dimensionality.
     *
     * @param distance the distance to travel along the space-filling curve
     * @return a long array, containing the x, y, z, etc. coordinates as elements to match the length of dimensionality
     */
    @Override
    public long[] point(long distance) {
        distance %= maxDistance;
        if(stored)
        {
            switch (bits)
            {
                case 1:
                case 2:
                case 3:
                case 4:
                    return toPoint(bVals[(int)distance]);
                case 5:
                case 6:
                case 7:
                case 8:
                    return toPoint(sVals[(int)distance]);
                default:
                    return toPoint(iVals[(int)distance]);
            }
        }
        return HilbertUtility.distanceToPoint(bits, DIMENSION, distance);
    }

    /**
     * Given a distance to travel along this space-filling curve and a dimension index (in 2D, x is 0 and y is 1; in
     * higher dimensions the subsequent dimensions have subsequent indices), gets that dimension's coordinate for the
     * point corresponding to the distance traveled along this space-filling curve.
     *
     * @param distance  the distance to travel along the space-filling curve
     * @param dimension the dimension index to get, such as 0 for x, 1 for y, 2 for z, etc.
     * @return the appropriate dimension's coordinate for the point corresponding to distance traveled
     */
    @Override
    public long coordinate(long distance, int dimension) {
        dimension %= DIMENSION;
        distance %= maxDistance;
        if(stored)
        {
            switch (bits)
            {
                case 1:
                case 2:
                case 3:
                case 4:
                    return bVals[(int)distance][dimension];
                case 5:
                case 6:
                case 7:
                case 8:
                    return sVals[(int)distance][dimension];
                default:
                    return iVals[(int)distance][dimension];
            }
        }
        else
            return HilbertUtility.distanceToPoint(bits, DIMENSION, distance)[dimension];
    }

    /**
     * Given an array or vararg of coordinates, which must have the same length as dimensionality, finds the distance
     * to travel along the space-filling curve to reach that distance.
     *
     * @param coordinates an array or vararg of long coordinates; must match the length of dimensionality
     * @return the distance to travel along the space-filling curve to reach the given coordinates, as a long, or -1 if
     * coordinates are invalid
     */
    @Override
    public long distance(long... coordinates) {
        if(coordinates.length != DIMENSION)
            return -1;
        if(stored)
        {
            switch (bits)
            {
                case 1:
                case 2:
                case 3:
                case 4:
                    return bDist[lookup(coordinates)];
                case 5:
                case 6:
                case 7:
                case 8:
                    return sDist[lookup(coordinates)];
                default:
                    return iDist[lookup(coordinates)];
            }
        }
        return HilbertUtility.pointToDistance(bits, DIMENSION, coordinates);

    }

}
