package com.github.tommyettinger;

import java.util.Arrays;

/**
 * A CurveStrategy for a cyclical arrangement of n-dimensional Hilbert curves, each occupying a hypercube with power-of-
 * two side length (componentSideLength), but the space-filling curve they are arranged into can be stretched by an
 * integer factor (stretch) along one dimension (specified by stretchAxis) into a non-cubic shape. Total distance
 * through this Moore Curve must be no greater than 2^63 (in hex, 0x4000000000000000); this is determined by
 * ((componentSideLength * 2) ^ (the dimension count - 1)) * stretch * componentSideLength. If a non-power-of-two
 * componentSideLength is requested, the first greater power of two is used for a side length. If the
 * componentSideLength or stretch would be too large, this throws an exception. Moore Curves like the ones this
 * describes are useful when you need either the behavior of one dimension that can be stretched, or the behavior of a
 * looping pattern through the curve instead of the normal Hilbert movement from one corner to another.
 * Created by Tommy Ettinger on 2/11/2016.
 */
public class MooreGeneralStrategy extends CurveStrategy {

    private HilbertGeneralStrategy hilbert;
    private long side;
    private int stretch;
    public final int DIMENSION;

    /**
     * Constructs a MooreGeneralStrategy with 3 dimensions, componentSideLength 16, stretching axis 0 (x-axis), and
     * stretch 2, which produces a cube with total side length 32. It will pre-calculate the 2^15 points of that Moore
     * Curve and store their x coordinates, y coordinates, z coordinates, and distances in short arrays.
     */
    public MooreGeneralStrategy()
    {
        this(3, 32, 0, 2);
    }

    /**
     * Constructs a MooreGeneralStrategy with the specified dimension count and side length, potentially pre-
     * calculating the Hilbert Curve to improve performance if the total length, sideLength ^ dimension, is no greater
     * than 2 ^ 24.
     * @param dimension the number of dimensions to use, all with equal length; must be between 2 and 31
     * @param sideLength the length of a side, which will be rounded up to the next-higher power of two if it isn't
     *                   already a power of two. sideLength ^ dimension must be no greater than 2^63.
     */
    public MooreGeneralStrategy(int dimension, long sideLength, int stretchAxis, int stretch) {
        if(dimension > 31)
            dimension = 31;
        if(dimension < 2)
            dimension = 2;
        DIMENSION = dimension;
        if(stretchAxis < 0 || stretchAxis >= DIMENSION)
            throw new UnsupportedOperationException("stretchAxis " + stretchAxis + "is invalid for dimension count " +
                    dimension);
        if(stretch < 0)
            throw new UnsupportedOperationException("stretch " + stretch + " should not be negative");
        if(sideLength <= 1)
        {
            sideLength = 2;
        }

        side = HilbertUtility.nextPowerOfTwo(sideLength);
        maxDistance = (long) Math.pow((side * 2), (dimension - 1)) * stretch * side;
        if(maxDistance > 0x4000000000000000L || maxDistance < 0)
            throw new UnsupportedOperationException("Moore Curve is too large or small, given dimension " + dimension +
            ", componentSideLength " + side + ", stretchAxis " + stretchAxis + ", stretch " + stretch);
        dimensionality = new long[DIMENSION];
        Arrays.fill(dimensionality, side * 2);
        dimensionality[stretchAxis] = side * stretch;
        hilbert = new HilbertGeneralStrategy(DIMENSION, side);
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
        return hilbert.point(distance);
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
        return hilbert.coordinate(distance, dimension);
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
        return hilbert.distance(coordinates);

    }

}