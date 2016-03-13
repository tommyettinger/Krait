package com.github.tommyettinger.krait.curves;

import com.github.tommyettinger.HilbertUtility;

import java.util.Arrays;

/**
 * A Curve for n-dimensional Hilbert curves occupying a hypercube with power-of-two side length. Total distance
 * through the Hilbert Curve must be no greater than 2^63 (in hex, 0x4000000000000000); this is determined by the side
 * length raised to the power of the dimension count. If a non-power-of-two length is requested, the first greater power
 * of two is used for a side length. If the side length would be too large, the size is shrunk to fit in a total
 * distance of 2^63 or less, potentially much less if there is no possible Hilbert Curve until a smaller one is found.
 * One example of this is if 8 dimensions are requested, then side length can be no greater than 2^7, with a total
 * distance of 2^56, because any larger side length that is a power of two would move the total distance up to past the
 * maximum of 2^63.
 * Created by Tommy Ettinger on 2/11/2016.
 */
public class HilbertCurve extends Curve {

    /**
     * Equivalent to the order of this Hilbert Curve.
     */
    public int bits;
    /**
     * Side length of the square.
     */
    public int side;

    private int dims;

    /**
     * Constructs a HilbertCurve with 3 dimensions and side length 32, which will pre-calculate the 2^15 points of that Hilbert
     * Curve and store their x coordinates, y coordinates, z coordinates, and distances in short arrays.
     */
    public HilbertCurve()
    {
        this(3, 32);
    }

    /**
     * Constructs a HilbertCurve with the specified dimension count and side length
     * @param dimension the number of dimensions to use, all with equal length; must be between 2 and 31
     * @param sideLength the length of a side, which will be rounded up to the next-higher power of two if it isn't
     *                   already a power of two. sideLength ^ dimension must be less than 2^31.
     */
    public HilbertCurve(int dimension, int sideLength) {
        if(dimension > 31)
            dimension = 31;
        dims = dimension;
        if(sideLength <= 1)
        {
            sideLength = 2;
        }

        side = HilbertUtility.nextPowerOfTwo(sideLength);
        bits = Long.numberOfTrailingZeros(side);
        if(bits * dims >= 31)
        {
            bits = 31 / dims;
            side = 1 << bits;
        }

        dimensionality = new int[dims];
        offsets = new int[dims];
        Arrays.fill(dimensionality, side);
        maxDistance = 1 << (bits * dims);
    }

    /**
     * Given a distance to travel along this space-filling curve, gets the corresponding point as an array of int
     * coordinates, typically in x, y, z... order. The length of the int array this returns is equivalent to the length
     * of the dimensionality field, and no elements in the returned array should be equal to or greater than the
     * corresponding element of dimensionality.
     *
     * @param distance the distance to travel along the space-filling curve
     * @return a int array, containing the x, y, z, etc. coordinates as elements to match the length of dimensionality
     */
    @Override
    public int[] point(int distance) {
        distance = (distance >= maxDistance || distance < 0) ? maxDistance - 1 : distance;

        return HilbertUtility.distanceToPoint(bits, dims, distance, offsets);
    }

    /**
     * Given a distance to travel along this space-filling curve and an int array of coordinates to modify, changes the
     * coordinates to match the point at the specified distance through this curve. The coordinates should typically be
     * in x, y, z... order. The length of the coordinates array must be equivalent to the length of the dimensionality
     * field, and no elements in the returned array will be equal to or greater than the corresponding element of
     * dimensionality. Returns the modified coordinates as well as modifying them in-place.
     *
     * @param coordinates an array of int coordinates that will be modified to match the specified total distance
     * @param distance    the distance (from the start) to travel along the space-filling curve
     * @return the modified coordinates (modified in-place, not a copy)
     */
    @Override
    public int[] alter(int[] coordinates, int distance) {
        distance = (distance >= maxDistance || distance < 0) ? maxDistance - 1 : distance;

        return HilbertUtility.distanceToPoint(coordinates, bits, dims, distance, offsets);
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
    public int coordinate(int distance, int dimension) {
        dimension %= dims;
        distance = (distance >= maxDistance || distance < 0) ? maxDistance - 1 : distance;
        return HilbertUtility.distanceToPoint(bits, dims, distance, offsets)[dimension];
    }

    /**
     * Given an array or vararg of coordinates, which must have the same length as dimensionality, finds the distance
     * to travel along the space-filling curve to reach that distance.
     *
     * @param coordinates an array or vararg of int coordinates; must match the length of dimensionality
     * @return the distance to travel along the space-filling curve to reach the given coordinates, as a int, or -1 if
     * coordinates are invalid
     */
    @Override
    public int distance(int... coordinates) {
        if(coordinates.length != dims)
            return -1;

        return HilbertUtility.pointToDistance(bits, dims, coordinates, offsets, side);

    }

}
