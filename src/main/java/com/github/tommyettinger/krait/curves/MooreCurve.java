package com.github.tommyettinger.krait.curves;

import com.github.tommyettinger.HilbertUtility;

import java.util.Arrays;

/**
 * A Curve for a cyclical arrangement of n-dimensional Hilbert curves, each occupying a hypercube with power-of-
 * two sideLength, but the space-filling curve they are arranged into can be stretched by an integer factor (stretch)
 * along one dimension (specified by stretchAxis) into a non-cubic shape. Total distance through this Moore Curve must
 * be no greater than 2^63 (in hex, 0x4000000000000000); this is determined by pow((sideLength * 2), (the dimension
 * count - 1)) * stretch * sideLength. If a non-power-of-two sideLength is requested, the first greater power of two is
 * used for a side length. If the sideLength or stretch would be too large, this throws an exception. Moore Curves like
 * the ones this describes are useful when you need either the behavior of one dimension that can be stretched, or the
 * behavior of a looping pattern through the curve instead of the normal Hilbert movement from one corner to another.
 * Created by Tommy Ettinger on 2/11/2016.
 */
public class MooreCurve extends Curve {

    private HilbertCurve hilbert;
    private int side;
    private int stretch, stretchAxis;
    private int innerMask, innerBits;
    private int dims;

    /**
     * Constructs a MooreCurve with 3 dimensions, sideLength 32, stretching axis 0 (x-axis), and
     * stretch 2, which produces a cube with total side length 64. It will pre-calculate the 2^15 points of the
     * component Hilbert curves and store their x coordinates, y coordinates, z coordinates, and distances in short
     * arrays, but will calculate anything related to the Moore curve as it goes, using the pre-calculated Hilbert Curve
     * to speed things up.
     */
    public MooreCurve()
    {
        this(3, 16, 0, 2);
    }

    /**
     * Constructs a MooreCurve with the specified dimension count and side length, potentially pre-
     * calculating the Hilbert Curve to improve performance if the length of the component Hilbert curves,
     * pow(sideLength, dimension), is no greater than pow(2,20). The total length of this Moore curve, as determined by
     * {@code pow(sideLength * 2, dimension) * stretch * sideLength}, must be no more than than pow(2, 30).
     * <br>
     * REMEMBER, sideLength refers to the component cubes' side length, not the dimensions of the Moore curve! Only if
     * stretch is 2 will the Moore curve actually be a cube, and then the length of that cube's sides will be sideLength
     * * 2; in all other cases the shape of the curve will be squat or tall.
     * @param dimension the number of dimensions to use, all with equal length; must be between 2 and 30
     * @param sideLength the length of a side of one of the internal Hilbert curves, which will be doubled for
     *                   non-stretched axes and multiplied by stretch for the stretched axis; will be rounded up to the
     *                   next-higher power of two if it isn't already a power of two.
     * @param stretchAxis the dimension this Moore curve can stretch along, between 0 and (dimension - 1)
     * @param stretch the integer amount to stretch this curve by along stretchAxis; a value of 2 results in a cubic
     *                shape, a value of 1 results in a wide, flat shape, and values greater than 2 are tall
     */
    public MooreCurve(int dimension, int sideLength, int stretchAxis, int stretch) {
        if(dimension > 31)
            dimension = 31;
        if(dimension < 2)
            dimension = 2;
        dims = dimension;
        if(stretchAxis < 0 || stretchAxis >= dims)
            throw new UnsupportedOperationException("stretchAxis " + stretchAxis + "is invalid for dimension count " +
                    dimension);
        if(stretch <= 0)
            throw new UnsupportedOperationException("stretch " + stretch + " should be positive");
        if(sideLength <= 1)
        {
            sideLength = 2;
        }

        side = HilbertUtility.nextPowerOfTwo(sideLength);
        maxDistance = (int) Math.pow((side * 2), (dimension - 1)) * stretch * side;
        if(maxDistance <= 0)
            throw new UnsupportedOperationException("Moore Curve is too large or small, given dimension " + dimension +
            ", sideLength " + side + ", stretchAxis " + stretchAxis + ", stretch " + stretch);
        dimensionality = new int[dims];
        offsets = new int[dims];
        Arrays.fill(dimensionality, side * 2);
        dimensionality[stretchAxis] = side * stretch;
        this.stretchAxis = stretchAxis;
        this.stretch = stretch;
        hilbert = new HilbertCurve(dims, side);
        innerBits = hilbert.bits * dims;
        innerMask = hilbert.maxDistance - 1;
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
        int h = distance & innerMask;
        int sector = distance >>> innerBits, arrange = HilbertUtility.grayCode(sector * 2 / stretch);
        int[] minor = hilbert.point(h), pt = new int[dims];
        for (int d = 0, a = (stretchAxis + 1) % dims; d < dims; d++, a = (a + 1) % dims) {
            if (a == stretchAxis) {
                pt[a] = ((sector / stretch) % 2 == 0
                        ? side * (sector % stretch) + minor[d]
                        : side * (stretch - (sector % stretch)) - 1 - minor[d]) + offsets[a];
            } else {
                pt[a] = (((arrange >> (dims - 1 - d)) & 1) == 0 ? side - 1 - minor[d] : side + minor[d]) + offsets[a];
            }
        }
        return pt;
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
        int h = distance & innerMask;
        int sector = distance >>> innerBits, arrange = HilbertUtility.grayCode(sector * 2 / stretch);
        int[] minor = hilbert.point(h);
        for (int d = 0, a = (stretchAxis + 1) % dims; d < dims; d++, a = (a + 1) % dims) {
            if (a == stretchAxis) {
                coordinates[a] = ((sector / stretch) % 2 == 0
                        ? side * (sector % stretch) + minor[d]
                        : side * (stretch - (sector % stretch)) - 1 - minor[d]) + offsets[a];
            } else {
                coordinates[a] = (((arrange >> (dims - 1 - d)) & 1) == 0 ? side - 1 - minor[d] : side + minor[d]) + offsets[a];
            }
        }
        return coordinates;
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
        dimension = (dimension + dims) % dims;
        distance = (distance >= maxDistance || distance < 0) ? maxDistance - 1 : distance;
        int h = distance & innerMask;
        int sector = distance >>> innerBits, arrange = HilbertUtility.grayCode(sector * 2 / stretch);
        int d = (stretchAxis + 1 + dimension) % dims;
        int minor = hilbert.coordinate(h, d), co;
        if (d == dims - 1) {
            co = ((sector / stretch) & 1) == 0
                    ? side * (sector % stretch) + minor
                    : side * (stretch - (sector % stretch)) - minor;
        } else {
            co = ((arrange >> (dims - 1 - d)) & 1) == 0 ? side - 1 - minor : side + minor;
        }

        return co + offsets[dimension];

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
        int arrange = 0, sector = 0, bonus = 0, dist = 0;
        int[] co = new int[coordinates.length];
        for (int c = 0; c < coordinates.length; c++) {
            co[c] = coordinates[c] - offsets[c];
            if(co[c] < 0 || co[c] > dimensionality[c])
                return -1;
        }
        for (int d = 0, a = (stretchAxis + 1) % dims; d < dims; d++, a = (a + 1) % dims) {
            if(a == stretchAxis)
            {
                sector = HilbertUtility.inverseGrayCode(arrange) >>> 1;
                bonus = (sector * stretch);
                if((sector & 1) == 0)
                {
                    co[a] %= side;
                    bonus += coordinates[a] / side;
                }
                else
                {
                    co[a] = (side * stretch - 1 - co[a]) % side;
                    bonus += (side * stretch - 1 - coordinates[a]) / side;
                }
                dist = hilbert.distance(co) + (bonus << innerBits);
            }
            else {
                if (coordinates[a] >= side) {
                    arrange |= 1 << (dims - 1 - d);
                    co[a] -= side;
                }
                else
                {
                    co[a] = (side - 1) - co[a];
                }
            }
        }
        return dist;

    }

}
