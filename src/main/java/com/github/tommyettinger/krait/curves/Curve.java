package com.github.tommyettinger.krait.curves;

/**
 * Created by Tommy Ettinger on 3/12/2016.
 */
public abstract class Curve {
    /*
    public interface Point {
        int[] point(int distance);
    }
    public interface Alter {
        int[] alter(int[] coordinate, int distance);
    }
    public interface Coordinate {
        int coordinate(int distance, int dimension);
    }
    public interface Distance {
        int distance(int... coordinates);
    }
    */
    /**
     * An array of dimension lengths, which are often (but not always) all equal. For a 5x5x5 space-filling curve, this
     * would have a value of 5, 5, 5. For a 32x32x16 space-filing curve, this would have a value of 32, 32, 16.
     */
    public int[] dimensionality;

    /**
     * An array of offsets per dimension, which push the space this curve fills in some positive or negative
     * direction(s). This must have equal length to dimensionality.
     */
    public int[] offsets;

    /**
     * The exclusive upper bound for distances that can be given to this CurveStrategy. For a space-filling curve that
     * covers a 16x16 grid of 256 points, this variable should be 256. If the space-filling curve covers a 5x5x5 area of
     * 125 points, then this variable should be 125, and so on.
     */
    public int maxDistance;

    /**
     * Given a distance to travel along this space-filling curve, gets the corresponding point as an array of int
     * coordinates, typically in x, y, z... order. The length of the int array this returns is equivalent to the length
     * of the dimensionality field, and no elements in the returned array should be equal to or greater than the
     * corresponding element of dimensionality.
     * @param distance the distance to travel along the space-filling curve
     * @return am int array, containing the x, y, z, etc. coordinates as elements to match the length of dimensionality
     */
    public abstract int[] point(int distance);

    /**
     * Given a distance to travel along this space-filling curve and an int array of coordinates to modify, changes the
     * coordinates to match the point at the specified distance through this curve. The coordinates should typically be
     * in x, y, z... order. The length of the coordinates array must be equivalent to the length of the dimensionality
     * field, and no elements in the returned array will be equal to or greater than the corresponding element of
     * dimensionality. Returns the modified coordinates as well as modifying them in-place.
     * @param coordinates an array of int coordinates that will be modified to match the specified total distance
     * @param distance the distance (from the start) to travel along the space-filling curve
     * @return the modified coordinates (modified in-place, not a copy)
     */
    public abstract int[] alter(int[] coordinates, int distance);

    /**
     * Given a distance to travel along this space-filling curve and a dimension index (in 2D, x is 0 and y is 1; in
     * higher dimensions the subsequent dimensions have subsequent indices), gets that dimension's coordinate for the
     * point corresponding to the distance traveled along this space-filling curve.
     * @param distance the distance to travel along the space-filling curve
     * @param dimension the dimension index to get, such as 0 for x, 1 for y, 2 for z, etc.
     * @return the appropriate dimension's coordinate for the point corresponding to distance traveled
     */
    public abstract int coordinate(int distance, int dimension);

    /**
     * Given an array or vararg of coordinates, which must have the same length as dimensionality, finds the distance
     * to travel along the space-filling curve to reach that distance.
     *
     * @param coordinates an array or vararg of int coordinates; must match the length of dimensionality
     * @return the distance to travel along the space-filling curve to reach the given coordinates, as a int, or -1 if
     * coordinates are invalid
     */
    public abstract int distance(int... coordinates);
}
