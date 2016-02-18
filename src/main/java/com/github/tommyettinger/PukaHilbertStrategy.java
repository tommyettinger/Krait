package com.github.tommyettinger;

/**
 * A CurveStrategy for a specific 3D space-filling curve that fits in a 40x40x40 cube. This Puka-Hilbert Curve builds
 * onto an 8x8x8 Hilbert Curve by replacing each cell with a 5x5x5 Puka curve, allowing the final 40x40x40 curve to
 * cover a greater amount of 3D space (64000 points) than the largest possible cubic 3D Hilbert Curve in under 16 bits
 * (32768 points) while still using 16-bit short values for distance.
 * Created by Tommy Ettinger on 2/11/2016.
 */
public class PukaHilbertStrategy extends CurveStrategy {

    /**
     * X positions for the Puka-Hilbert Curve, a 40x40x40 combination of Puka Curve and Hilbert Curve.
     */
    public final byte[] pukaHilbertX = new byte[64000];
    /**
     * Y positions for the Puka-Hilbert Curve, a 40x40x40 combination of Puka Curve and Hilbert Curve.
     */
    public final byte[] pukaHilbertY = new byte[64000];
    /**
     * Z positions for the Puka-Hilbert Curve, a 40x40x40 combination of Puka Curve and Hilbert Curve.
     */
    public final byte[] pukaHilbertZ = new byte[64000];
    /**
     * Distances for positions on the Puka-Hilbert Curve, a 40x40x40 combination of Puka Curve and Hilbert Curve.
     * Indexed with x + 40 * y + 1600 * z to get the distance to the point x,y,z .
     */
    public final short[] pukaHilbertDist = new short[64000];

    /**
     * Side length of the cube.
     */
    private static final int side = 40;
    private static final int DIMENSION = 3;
    private final HilbertGeneralStrategy hilbert;
    private final PukaStrategy puka;

    /**
     * Constructs a PukaHilbertStrategy with side length 40, which will pre-calculate the 64000 points of that Puka-
     * Hilbert Curve and store their x coordinates, y coordinates, z coordinates, and distances in short arrays.
     */
    public PukaHilbertStrategy() {

        dimensionality = new int[]{side, side, side};
        maxDistance = 64000;
        distanceByteSize = 2;
        hilbert = new HilbertGeneralStrategy(3, 16);
        puka = new PukaStrategy();
        int[] start, end;
        for (int h = 0, p = 0; h < 0x1000; h += 8, p += 125) {
            start = hilbert.point(h);
            end = hilbert.point(h + 7);
            int startX = start[0], startY = start[1], startZ = start[2],
                    endX = end[0], endY = end[1], endZ = end[2],
                    bottomX = startX >> 1, bottomY = startY >> 1, bottomZ = startZ >> 1;
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
            byte x, y, z;
            for (int i = 0; i < 125; i++) {
                pukaHilbertX[p + i] = x = (byte)(puka.coordinateRotated(i, 0, direction, rotation) + bottomX * 5);
                pukaHilbertY[p + i] = y = (byte)(puka.coordinateRotated(i, 1, direction, rotation) + bottomY * 5);
                pukaHilbertZ[p + i] = z = (byte)(puka.coordinateRotated(i, 2, direction, rotation) + bottomZ * 5);
                pukaHilbertDist[x + y * 40 + z * 1600] = (short)(p + i);
            }
        }
        stored = true;
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
        distance = (distance + maxDistance) % maxDistance;
        return new int[]{pukaHilbertX[distance], pukaHilbertY[distance], pukaHilbertZ[distance]};
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
        distance = (distance + maxDistance) % maxDistance;
        coordinates[0] = pukaHilbertX[distance];
        coordinates[1] = pukaHilbertY[distance];
        coordinates[2] = pukaHilbertZ[distance];
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
        distance = (distance + maxDistance) % maxDistance;

        switch (dimension) {
            case 0:
                return pukaHilbertX[distance];
            case 1:
                return pukaHilbertY[distance];
            default:
                return pukaHilbertZ[distance];
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
        return pukaHilbertDist[ coordinates[0] + 40 * coordinates[1] + 1600 * coordinates[2]];
    }
}
