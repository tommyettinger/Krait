package com.github.tommyettinger;

/**
 * A technique for translating 1-dimensional distances into n-dimensional positions using a space-filling curve.
 * Created by Tommy Ettinger on 2/11/2016.
 */
public abstract class CurveStrategy {
    /**
     * The exclusive upper bound for distances that can be given to this CurveStrategy. For a space-filling curve that
     * covers a 16x16 grid of 256 points, this variable should be 256. If the space-filling curve covers a 5x5x5 area of
     * 125 points, then this variable should be 125, and so on.
     */
    public long maxDistance;
    /**
     * An array of dimension lengths, which are often (but not always) all equal. For a 5x5x5 space-filling curve, this
     * would have a value of 5L, 5L, 5L. For a 24x24x16 space-filing curve, this would have a value of 24L, 24L, 16L.
     */
    public long[] dimensionality;
    /**
     * Should be true if this CurveStrategy pre-calculates its items, which shouldn't be done if maxDistance is higher
     * than about 2^24, depending on hardware. Storing each dimension of a 2^24 length curve uses 64 MB of RAM for each
     * int array, plus another 64 MB for the distances. Going larger than that gets into very difficult-to-justify
     * territory of memory usage, especially since the initial caching contributes to startup time.
     */
    public boolean stored;

    protected static final long MASK = 0xffffffffffffffffL;

    /**
     * Given a distance to travel along this space-filling curve, gets the corresponding point as an array of long
     * coordinates, typically in x, y, z... order. The length of the long array this returns is equivalent to the length
     * of the dimensionality field, and no elements in the returned array should be equal to or greater than the
     * corresponding element of dimensionality.
     * @param distance the distance to travel along the space-filling curve
     * @return a long array, containing the x, y, z, etc. coordinates as elements to match the length of dimensionality
     */
    public abstract long[] getPoint(long distance);

    /**
     * Given a distance to travel along this space-filling curve and a dimension index (in 2D, x is 0 and y is 1; in
     * higher dimensions the subsequent dimensions have subsequent indices), gets that dimension's coordinate for the
     * point corresponding to the distance traveled along this space-filling curve.
     * @param distance the distance to travel along the space-filling curve
     * @param dimension the dimension index to get, such as 0 for x, 1 for y, 2 for z, etc.
     * @return the appropriate dimension's coordinate for the point corresponding to distance traveled
     */
    public abstract long coordinate(long distance, int dimension);

    /**
     * Given an array or vararg of coordinates, which must have the same length as dimensionality, finds the distance
     * to travel along the space-filling curve to reach that distance.
     * @param coordinates an array or vararg of long coordinates; must match the length of dimensionality
     * @return the distance to travel along the space-filling curve to reach the given coordinates, as a long
     */
    public abstract long distance(long... coordinates);
}
