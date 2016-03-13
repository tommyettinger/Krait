package com.github.tommyettinger.krait.curves;

import com.github.tommyettinger.CurveStrategy;

/**
 * Not currently implemented.
 * A CurveStrategy for a specific 3D space-filling curve that fits in a 40x40x40 cube. This Puka-Hilbert Curve builds
 * onto an 8x8x8 Hilbert Curve by replacing each cell with a 5x5x5 Puka curve, allowing the final 40x40x40 curve to
 * cover a greater amount of 3D space (64000 points) than the largest possible cubic 3D Hilbert Curve in under 16 bits
 * (32768 points) while still using 16-bit short values for distance.
 * Created by Tommy Ettinger on 2/11/2016.
 */
public class PukaHilbert40Strategy extends CurveStrategy {

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
    private final HilbertCurve hilbert;
    private final PukaCurveAtom puka;

    /**
     * Constructs a PukaHilbert40Strategy, representing a 40x40x40 cube, which will pre-calculate the 64000 points of
     * that Puka-Hilbert Curve, storing their x coordinates, y coordinates, and z coordinates in byte arrays and their
     * distances in a short array (treating the short values as unsigned).
     */
    public PukaHilbert40Strategy() {

        dimensionality = new int[]{side, side, side};
        maxDistance = 64000;
        distanceByteSize = 2;
        hilbert = new HilbertCurve(3, 16);
        puka = new PukaCurveAtom();
        int[] start, end;
        for (int h = 0, p = 0; h < 0x1000; h += 8, p += 125) {
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
        distance = (distance >= maxDistance || distance < 0) ? maxDistance - 1 : distance;
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
        distance = (distance >= maxDistance || distance < 0) ? maxDistance - 1 : distance;
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
        distance = (distance >= maxDistance || distance < 0) ? maxDistance - 1 : distance;

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
     * @return the distance to travel along the space-filling curve to reach the given coordinates, as an int, or -1 if
     * coordinates are invalid
     */
    @Override
    public int distance(int... coordinates) {
        if(coordinates.length != 3)
            return -1;
        return 0xffff & pukaHilbertDist[ coordinates[0] + 40 * coordinates[1] + 1600 * coordinates[2]];
    }

    /**
     * Not intended for external use. Part of the implementation of the larger Puka-Hilbert Curve.
     *
     * @param distance the distance to travel along the space-filling curve
     * @param direction between 0 and 5, inclusive
     * @param rotation between 0 and 3, inclusive
     * @return a int array, containing the x, y, z, etc. coordinates as elements to match the length of dimensionality
     */
    public int[] pointRotated(int distance, int direction, int rotation) {
        distance = (distance >= maxDistance || distance < 0) ? maxDistance - 1 : distance;
        switch (4 * direction + rotation)
        {
            case 0:
                return new int[]{pukaHilbertZ[distance], pukaHilbertX[distance], pukaHilbertY[distance]};
            case 1:
                return new int[]{pukaHilbertZ[distance], 39 - pukaHilbertY[distance], pukaHilbertX[distance]};
            case 2:
                return new int[]{pukaHilbertZ[distance], 39 - pukaHilbertX[distance], 39 - pukaHilbertY[distance]};
            case 3:
                return new int[]{pukaHilbertZ[distance], pukaHilbertY[distance], 39 - pukaHilbertX[distance]};
            case 4:
                return new int[]{pukaHilbertX[distance], pukaHilbertZ[distance], pukaHilbertY[distance]};
            case 5:
                return new int[]{39 - pukaHilbertY[distance], pukaHilbertZ[distance], pukaHilbertX[distance]};
            case 6:
                return new int[]{39 - pukaHilbertX[distance], pukaHilbertZ[distance], 39 - pukaHilbertY[distance]};
            case 7:
                return new int[]{pukaHilbertY[distance], pukaHilbertZ[distance], 39 - pukaHilbertX[distance]};
            case 8:
                return new int[]{pukaHilbertX[distance], pukaHilbertY[distance], pukaHilbertZ[distance]};
            case 9:
                return new int[]{39 - pukaHilbertY[distance], pukaHilbertX[distance], pukaHilbertZ[distance]};
            case 10:
                return new int[]{39 - pukaHilbertX[distance], 39 - pukaHilbertY[distance], pukaHilbertZ[distance]};
            case 11:
                return new int[]{pukaHilbertY[distance], 39 - pukaHilbertX[distance], pukaHilbertZ[distance]};


            case 12:
                return new int[]{39 - pukaHilbertZ[distance], pukaHilbertX[distance], pukaHilbertY[distance]};
            case 13:
                return new int[]{39 - pukaHilbertZ[distance], 39 - pukaHilbertY[distance], pukaHilbertX[distance]};
            case 14:
                return new int[]{39 - pukaHilbertZ[distance], 39 - pukaHilbertX[distance], 39 - pukaHilbertY[distance]};
            case 15:
                return new int[]{39 - pukaHilbertZ[distance], pukaHilbertY[distance], 39 - pukaHilbertX[distance]};
            case 16:
                return new int[]{pukaHilbertX[distance], 39 - pukaHilbertZ[distance], pukaHilbertY[distance]};
            case 17:
                return new int[]{39 - pukaHilbertY[distance], 39 - pukaHilbertZ[distance], pukaHilbertX[distance]};
            case 18:
                return new int[]{39 - pukaHilbertX[distance], 39 - pukaHilbertZ[distance], 39 - pukaHilbertY[distance]};
            case 19:
                return new int[]{pukaHilbertY[distance], 39 - pukaHilbertZ[distance], 39 - pukaHilbertX[distance]};
            case 20:
                return new int[]{pukaHilbertX[distance], pukaHilbertY[distance], 39 - pukaHilbertZ[distance]};
            case 21:
                return new int[]{39 - pukaHilbertY[distance], pukaHilbertX[distance], 39 - pukaHilbertZ[distance]};
            case 22:
                return new int[]{39 - pukaHilbertX[distance], 39 - pukaHilbertY[distance], 39 - pukaHilbertZ[distance]};
            default:
                return new int[]{pukaHilbertY[distance], 39 - pukaHilbertX[distance], 39 - pukaHilbertZ[distance]};

        }
    }
    /**
     * Not intended for external use. Part of the implementation of the larger Puka-Hilbert Curve.
     *
     * @param distance the distance to travel along the space-filling curve
     * @param dimension which dimension to get the rotated coordinate for
     * @param direction between 0 and 5, inclusive
     * @param rotation between 0 and 3, inclusive
     * @return the appropriate dimension's rotated coordinate for the point corresponding to distance traveled
     */
    public int coordinateRotated(int distance, int dimension, int direction, int rotation) {
        distance = (distance >= maxDistance || distance < 0) ? maxDistance - 1 : distance;
        switch (dimension) {
            case 0:
                switch (4 * direction + rotation) {
                    case 0:
                        return pukaHilbertZ[distance];
                    case 1:
                        return pukaHilbertZ[distance];
                    case 2:
                        return pukaHilbertZ[distance];
                    case 3:
                        return pukaHilbertZ[distance];
                    case 4:
                        return pukaHilbertX[distance];
                    case 5:
                        return 39 - pukaHilbertY[distance];
                    case 6:
                        return 39 - pukaHilbertX[distance];
                    case 7:
                        return pukaHilbertY[distance];
                    case 8:
                        return pukaHilbertX[distance];
                    case 9:
                        return 39 - pukaHilbertY[distance];
                    case 10:
                        return 39 - pukaHilbertX[distance];
                    case 11:
                        return pukaHilbertY[distance];
                    case 12:
                        return 39 - pukaHilbertZ[distance];
                    case 13:
                        return 39 - pukaHilbertZ[distance];
                    case 14:
                        return 39 - pukaHilbertZ[distance];
                    case 15:
                        return 39 - pukaHilbertZ[distance];
                    case 16:
                        return pukaHilbertX[distance];
                    case 17:
                        return 39 - pukaHilbertY[distance];
                    case 18:
                        return 39 - pukaHilbertX[distance];
                    case 19:
                        return pukaHilbertY[distance];
                    case 20:
                        return pukaHilbertX[distance];
                    case 21:
                        return 39 - pukaHilbertY[distance];
                    case 22:
                        return 39 - pukaHilbertX[distance];
                    default:
                        return pukaHilbertY[distance];

                }
            case 1:
                switch (4 * direction + rotation) {
                    case 0:
                        return pukaHilbertX[distance];
                    case 1:
                        return 39 - pukaHilbertY[distance];
                    case 2:
                        return 39 - pukaHilbertX[distance];
                    case 3:
                        return pukaHilbertY[distance];
                    case 4:
                        return pukaHilbertZ[distance];
                    case 5:
                        return pukaHilbertZ[distance];
                    case 6:
                        return pukaHilbertZ[distance];
                    case 7:
                        return pukaHilbertZ[distance];
                    case 8:
                        return pukaHilbertY[distance];
                    case 9:
                        return pukaHilbertX[distance];
                    case 10:
                        return 39 - pukaHilbertY[distance];
                    case 11:
                        return 39 - pukaHilbertX[distance];
                    case 12:
                        return pukaHilbertX[distance];
                    case 13:
                        return 39 - pukaHilbertY[distance];
                    case 14:
                        return 39 - pukaHilbertX[distance];
                    case 15:
                        return pukaHilbertY[distance];
                    case 16:
                        return 39 - pukaHilbertZ[distance];
                    case 17:
                        return 39 - pukaHilbertZ[distance];
                    case 18:
                        return 39 - pukaHilbertZ[distance];
                    case 19:
                        return 39 - pukaHilbertZ[distance];
                    case 20:
                        return pukaHilbertY[distance];
                    case 21:
                        return pukaHilbertX[distance];
                    case 22:
                        return 39 - pukaHilbertY[distance];
                    default:
                        return 39 - pukaHilbertX[distance];

                }
            default:
                switch (4 * direction + rotation) {
                    case 0:
                        return pukaHilbertY[distance];
                    case 1:
                        return pukaHilbertX[distance];
                    case 2:
                        return 39 - pukaHilbertY[distance];
                    case 3:
                        return 39 - pukaHilbertX[distance];
                    case 4:
                        return pukaHilbertY[distance];
                    case 5:
                        return pukaHilbertX[distance];
                    case 6:
                        return 39 - pukaHilbertY[distance];
                    case 7:
                        return 39 - pukaHilbertX[distance];
                    case 8:
                        return pukaHilbertZ[distance];
                    case 9:
                        return pukaHilbertZ[distance];
                    case 10:
                        return pukaHilbertZ[distance];
                    case 11:
                        return pukaHilbertZ[distance];
                    case 12:
                        return pukaHilbertY[distance];
                    case 13:
                        return pukaHilbertX[distance];
                    case 14:
                        return 39 - pukaHilbertY[distance];
                    case 15:
                        return 39 - pukaHilbertX[distance];
                    case 16:
                        return pukaHilbertY[distance];
                    case 17:
                        return pukaHilbertX[distance];
                    case 18:
                        return 39 - pukaHilbertY[distance];
                    case 19:
                        return 39 - pukaHilbertX[distance];
                    case 20:
                        return 39 - pukaHilbertZ[distance];
                    case 21:
                        return 39 - pukaHilbertZ[distance];
                    case 22:
                        return 39 - pukaHilbertZ[distance];
                    default:
                        return 39 - pukaHilbertZ[distance];

                }
        }
    }

    /**
     * Not intended for external use. Part of the implementation of the larger Puka-Hilbert Curve.
     *
     * @param direction between 0 and 5, inclusive
     * @param rotation between 0 and 3, inclusive
     * @param x x component of the distance to find
     * @param y y component of the distance to find
     * @param z z component of the distance to find
     * @return the distance to travel along the space-filling curve to reach the given coordinates at the specified
     * rotation, as an int, or -1 if coordinates are invalid
     */
    public int distanceRotated(int direction, int rotation, int x, int y, int z) {
        int a, b, c;

        switch (4 * direction + rotation)
        {
            case 0:
                c = x;
                a = y;
                b = z;
                break;
            case 1:
                c = x;
                b = 39 - y;
                a = z;
                break;
            case 2:
                c = x;
                a = 39 - y;
                b = 39 - z;
                break;
            case 3:
                c = x;
                b = y;
                a = 39 - z;
                break;
            case 4:
                a = x;
                c = y;
                b = z;
                break;
            case 5:
                b = 39 - x;
                c = y;
                a = z;
                break;
            case 6:
                a = 39 - x;
                c = y;
                b = 39 - z;
                break;
            case 7:
                b = x;
                c = y;
                a = 39 - z;
                break;
            case 8:
                a = x;
                b = y;
                c = z;
                break;
            case 9:
                b = 39 - x;
                a = y;
                c = z;
                break;
            case 10:
                a = 39 - x;
                b = 39 - y;
                c = z;
                break;
            case 11:
                b = x;
                a = 39 - y;
                c = z;
                break;


            case 12:
                c = 39 - x;
                a = y;
                b = z;
                break;
            case 13:
                c = 39 - x;
                b = 39 - y;
                a = z;
                break;
            case 14:
                c = 39 - x;
                a = 39 - y;
                b = 39 - z;
                break;
            case 15:
                c = 39 - x;
                b = y;
                a = 39 - z;
                break;
            case 16:
                a = x;
                c = 39 - y;
                b = z;
                break;
            case 17:
                b = 39 - x;
                c = 39 - y;
                a = z;
                break;
            case 18:
                a = 39 - x;
                c = 39 - y;
                b = 39 - z;
                break;
            case 19:
                b = x;
                c = 39 - y;
                a = 39 - z;
                break;
            case 20:
                a = x;
                b = y;
                c = 39 - z;
                break;
            case 21:
                b = 39 - x;
                a = y;
                c = 39 - z;
                break;
            case 22:
                a = 39 - x;
                b = 39 - y;
                c = 39 - z;
                break;
            default:
                b = x;
                a = 39 - y;
                c = 39 - z;
                break;
        }

        return 0xffff & pukaHilbertDist[a + 40 * b + 1600 * c];
    }

}
