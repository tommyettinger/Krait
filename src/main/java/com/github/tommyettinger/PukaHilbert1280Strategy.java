package com.github.tommyettinger;

/**
 * A CurveStrategy for a specific 3D space-filling curve that fits in a 1280x1280x1280 cube, coming extremely close to
 * the maximum number of cells RegionPacker can pack (closer than a 3D Hilbert curve). This Puka-Hilbert Curve builds
 * onto a 256x256x256 Hilbert Curve by replacing each cell with a 5x5x5 Puka curve, allowing the final 1280x1280x1280
 * curve to cover a greater amount of 3D space (2,097,152,000 points) than the largest possible cubic 3D Hilbert Curve
 * in 31 bits (1,073,741,824 points). Internally, this does not cache the 1280x1280x1280 curve, but does cache the
 * smaller 40x40x40 Puka-Hilbert curve and a 64x64x64 Hilbert curve, using the two to get a position without extensive
 * recalculation per point, though some is needed.
 * Created by Tommy Ettinger on 2/11/2016.
 */
public class PukaHilbert1280Strategy extends CurveStrategy {

    /**
     * Side length of the cube.
     */
    private static final int side = 1280;
    private static final int DIMENSION = 3;
    private final HilbertGeneralStrategy hilbert;
    private final PukaHilbert40Strategy puka;

    /**
     * Constructs a PukaHilbert40Strategy, representing a 40x40x40 cube, which will pre-calculate the 64000 points of
     * that Puka-Hilbert Curve, storing their x coordinates, y coordinates, and z coordinates in byte arrays and their
     * distances in a short array (treating the short values as unsigned).
     */
    public PukaHilbert1280Strategy() {

        dimensionality = new int[]{side, side, side};
        maxDistance = 2097152000;
        distanceByteSize = 4;
        hilbert = new HilbertGeneralStrategy(3, 64);
        puka = new PukaHilbert40Strategy();
        stored = false;
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
        if(distance < 0 || distance > maxDistance)
            distance = maxDistance - 1;

        int h = (distance / 64000) * 8; // divide by 64000 to skip PH40 curves, multiply by 8 for higher order Hilbert
        int p = distance % 64000; // distance through the current inner PH40 curve

        int startX = hilbert.coordinate(h, 0), startY = hilbert.coordinate(h, 1), startZ = hilbert.coordinate(h, 2),
                bottomX = startX >> 1, bottomY = startY >> 1, bottomZ = startZ >> 1,
                endX = hilbert.coordinate(h + 7, 0), endY = hilbert.coordinate(h + 7, 1), endZ = hilbert.coordinate(h + 7, 2);

        int direction, rotation;
        if(startX < endX) {
            direction = 0;
            rotation = ((startZ & 1) << 1) | (startY & 1);
        }
        else if(startX > endX) {
            direction = 3;
            rotation = ((startZ & 1) << 1) | (startY & 1);
        }
        else if(startY < endY) {
            direction = 1;
            rotation = ((startZ & 1) << 1) | (startX & 1);
        }
        else if(startY > endY) {
            direction = 4;
            rotation = ((startZ & 1) << 1) | (startX & 1);
        }
        else if(startZ < endZ) {
            direction = 2;
            rotation = ((startY & 1) << 1) | (startX & 1);
        }
        else {
            direction = 5;
            rotation = ((startY & 1) << 1) | (startX & 1);
        }
        rotation = rotation ^ (rotation >> 1);

        int[] pt = puka.pointRotated(p, direction, rotation);
        pt[0] += bottomX * 40;
        pt[1] += bottomY * 40;
        pt[2] += bottomZ * 40;
        return pt;
        /*
        for (int i = 0; i < 125; i++) {
            pukaHilbertX[p + i] = x = (byte)(puka.coordinateRotated(i, 0, direction, rotation) + bottomX * 5);
            pukaHilbertY[p + i] = y = (byte)(puka.coordinateRotated(i, 1, direction, rotation) + bottomY * 5);
            pukaHilbertZ[p + i] = z = (byte)(puka.coordinateRotated(i, 2, direction, rotation) + bottomZ * 5);
            pukaHilbertDist[x + y * 40 + z * 1600] = (short)(p + i);
        }
        */

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
        if(distance < 0 || distance > maxDistance)
            distance = maxDistance - 1;

        int h = (distance / 64000) * 8; // divide by 64000 to skip PH40 curves, multiply by 8 for higher order Hilbert
        int p = distance % 64000; // distance through the current inner PH40 curve

        int startX = hilbert.coordinate(h, 0), startY = hilbert.coordinate(h, 1), startZ = hilbert.coordinate(h, 2),
                bottomX = startX >> 1, bottomY = startY >> 1, bottomZ = startZ >> 1,
                endX = hilbert.coordinate(h + 7, 0), endY = hilbert.coordinate(h + 7, 1), endZ = hilbert.coordinate(h + 7, 2);

        int direction, rotation;
        if(startX < endX) {
            direction = 0;
            rotation = ((startZ & 1) << 1) | (startY & 1);
        }
        else if(startX > endX) {
            direction = 3;
            rotation = ((startZ & 1) << 1) | (startY & 1);
        }
        else if(startY < endY) {
            direction = 1;
            rotation = ((startZ & 1) << 1) | (startX & 1);
        }
        else if(startY > endY) {
            direction = 4;
            rotation = ((startZ & 1) << 1) | (startX & 1);
        }
        else if(startZ < endZ) {
            direction = 2;
            rotation = ((startY & 1) << 1) | (startX & 1);
        }
        else {
            direction = 5;
            rotation = ((startY & 1) << 1) | (startX & 1);
        }
        rotation = rotation ^ (rotation >> 1);

        coordinates[0] = puka.coordinateRotated(p, 0, direction, rotation) + bottomX * 40;
        coordinates[1] = puka.coordinateRotated(p, 1, direction, rotation) + bottomY * 40;
        coordinates[2] = puka.coordinateRotated(p, 2, direction, rotation) + bottomZ * 40;
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
        dimension %= 3;
        if(distance < 0 || distance > maxDistance)
            distance = maxDistance - 1;

        int h = (distance / 64000) * 8; // divide by 64000 to skip PH40 curves, multiply by 8 for higher order Hilbert
        int p = distance % 64000; // distance through the current inner PH40 curve

        int startX = hilbert.coordinate(h, 0), startY = hilbert.coordinate(h, 1), startZ = hilbert.coordinate(h, 2),
                bottomX = startX >> 1, bottomY = startY >> 1, bottomZ = startZ >> 1,
                endX = hilbert.coordinate(h + 7, 0), endY = hilbert.coordinate(h + 7, 1), endZ = hilbert.coordinate(h + 7, 2);

        int direction, rotation;
        if(startX < endX) {
            direction = 0;
            rotation = ((startZ & 1) << 1) | (startY & 1);
        }
        else if(startX > endX) {
            direction = 3;
            rotation = ((startZ & 1) << 1) | (startY & 1);
        }
        else if(startY < endY) {
            direction = 1;
            rotation = ((startZ & 1) << 1) | (startX & 1);
        }
        else if(startY > endY) {
            direction = 4;
            rotation = ((startZ & 1) << 1) | (startX & 1);
        }
        else if(startZ < endZ) {
            direction = 2;
            rotation = ((startY & 1) << 1) | (startX & 1);
        }
        else {
            direction = 5;
            rotation = ((startY & 1) << 1) | (startX & 1);
        }
        rotation = rotation ^ (rotation >> 1);

        switch (dimension) {
            case 0:  return puka.coordinateRotated(p, 0, direction, rotation) + bottomX * 40;
            case 1:  return puka.coordinateRotated(p, 1, direction, rotation) + bottomY * 40;
            default: return puka.coordinateRotated(p, 2, direction, rotation) + bottomZ * 40;
        }
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
        if(coordinates.length != 3)
            return -1;
        int hx = coordinates[0] / 40, hy = coordinates[1] / 40, hz = coordinates[2] / 40,
                px = coordinates[0] % 40, py = coordinates[1] % 40, pz = coordinates[2] % 40,
                hdist64 = hilbert.distance(hx * 2, hy * 2, hz * 2), h = hdist64 - (hdist64 & 7);

        int startX = hilbert.coordinate(h, 0), startY = hilbert.coordinate(h, 1), startZ = hilbert.coordinate(h, 2),
                bottomX = startX >> 1, bottomY = startY >> 1, bottomZ = startZ >> 1,
                endX = hilbert.coordinate(h + 7, 0), endY = hilbert.coordinate(h + 7, 1), endZ = hilbert.coordinate(h + 7, 2);

        int direction, rotation;
        if(startX < endX) {
            direction = 0;
            rotation = ((startZ & 1) << 1) | (startY & 1);
        }
        else if(startX > endX) {
            direction = 3;
            rotation = ((startZ & 1) << 1) | (startY & 1);
        }
        else if(startY < endY) {
            direction = 1;
            rotation = ((startZ & 1) << 1) | (startX & 1);
        }
        else if(startY > endY) {
            direction = 4;
            rotation = ((startZ & 1) << 1) | (startX & 1);
        }
        else if(startZ < endZ) {
            direction = 2;
            rotation = ((startY & 1) << 1) | (startX & 1);
        }
        else {
            direction = 5;
            rotation = ((startY & 1) << 1) | (startX & 1);
        }
        rotation = rotation ^ (rotation >> 1);

        return (h >>> 3) * 64000 + puka.distanceRotated(direction, rotation, px, py, pz);
    }
}
