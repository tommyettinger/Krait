package com.github.tommyettinger;

/**
 * A CurveStrategy for a specific 3D space-filling curve that fits in a 5x5x5 cube. This Puka Curve can be used to
 * replace all 2x2x2 sections of a Hilbert Curve and thus cover a greater amount of 3D space than the largest possible
 * 3D Hilbert Curve for some bit sizes, and with the same bit count per coordinate and distance. There are some quirks
 * when mixing odd-side-length cubes with Hilbert Curve sections, limiting the amount of fractal self-similarity for
 * these space-filling curves. Specifically, any odd-side-length cubes must be used as building blocks for other odd-
 * side-length cubes at smaller side lengths before any even-side-length cubes are filled, due to the side length
 * becoming an even number once an even-side-length cube is used (even times odd is even, only odd times odd is odd),
 * and only odd-side-length cubes can be used to replace sections of Puka Curve (probably; this isn't certain yet).
 * On its own, the Puka Curve may be useful for a adaptively recursive space-filling curve implementation, since it fits
 * snugly in 7 bits, allowing the eighth bit to be used to store adaptive size information or some other data.
 * Created by Tommy Ettinger on 2/11/2016.
 */
public class PukaStrategy extends CurveStrategy {

    /**
     * X positions for the Puka Curve, a 5x5x5 potential "atom" for Hilbert Curves.
     * One possible variant is http://i.imgur.com/IJzIkio.png
     */
    public final byte[] pukaX;
    /**
     * Y positions for the Puka Curve, a 5x5x5 potential "atom" for Hilbert Curves.
     * One possible variant is http://i.imgur.com/IJzIkio.png
     */
    public final byte[] pukaY;
    /**
     * Z positions for the Puka Curve, a 5x5x5 potential "atom" for Hilbert Curves.
     * One possible variant is http://i.imgur.com/IJzIkio.png
     */
    public final byte[] pukaZ;
    /**
     * Distances for positions on the Puka Curve, a 5x5x5 potential "atom" for Hilbert Curves.
     * Indexed with x + 5 * y + 25 * z to get the distance to the point x,y,z .
     * One possible variant is http://i.imgur.com/IJzIkio.png
     */
    public final byte[] pukaDist;

    /**
     * Side length of the cube.
     */
    private static final long side = 5;
    private static final int DIMENSION = 3;

    /**
     * Constructs a PukaStrategy with side length 5, which will pre-calculate the 125 points of that Puka
     * Curve and store their x coordinates, y coordinates, z coordinates, and distances in short arrays.
     */
    public PukaStrategy() {

        dimensionality = new long[]{side, side, side};
        maxDistance = 125;

        pukaX = new byte[]
                {
                        0, 0, 0, 0, 1, 1, 1, 1, 2, 2, 3, 3, 2, 2, 2, 3, 3, 4, 4, 4, 4, 4, 4, 3, 3, 2, 2,
                        3, 3, 4, 4, 4, 4, 4, 4, 3, 3, 3, 3, 2, 2, 2, 2, 2, 2, 1, 1, 0, 0, 0, 1, 1, 0, 0,
                        1, 1, 0, 0, 0, 1, 1, 2, 2, 2, 2, 2, 2, 2, 3, 3, 3, 3, 4, 4, 4, 4, 4, 4, 3, 3, 4,
                        4, 4, 3, 3, 3, 4, 4, 3, 3, 4, 4, 3, 3, 2, 2, 2, 2, 2, 1, 1, 0, 0, 0, 0, 1, 1, 1,
                        0, 0, 1, 1, 0, 0, 1, 1, 0, 0, 1, 1, 0, 0, 1, 1, 0
                };

        pukaY = new byte[]
                {
                        0, 1, 1, 0, 0, 1, 1, 0, 0, 0, 0, 1, 1, 1, 0, 0, 1, 1, 0, 0, 1, 1, 0, 0, 1, 1, 2,
                        2, 2, 2, 2, 3, 3, 4, 4, 4, 4, 3, 3, 3, 4, 4, 3, 3, 4, 4, 3, 3, 4, 4, 4, 4, 4, 3,
                        3, 3, 3, 2, 2, 2, 2, 2, 2, 2, 1, 1, 0, 0, 0, 1, 1, 0, 0, 0, 1, 1, 2, 2, 2, 2, 2,
                        3, 3, 3, 3, 4, 4, 4, 4, 4, 4, 3, 3, 2, 2, 3, 3, 4, 4, 4, 4, 4, 4, 3, 3, 3, 3, 2,
                        2, 1, 1, 1, 1, 2, 2, 2, 2, 1, 1, 0, 0, 0, 0, 0, 0
                };

        pukaZ = new byte[]
                {
                        0, 0, 1, 1, 1, 1, 0, 0, 0, 1, 1, 1, 1, 2, 2, 2, 2, 2, 2, 1, 1, 0, 0, 0, 0, 0, 0,
                        0, 1, 1, 0, 0, 1, 1, 0, 0, 1, 1, 0, 0, 0, 1, 1, 2, 2, 2, 2, 2, 2, 1, 1, 0, 0, 0,
                        0, 1, 1, 1, 0, 0, 1, 1, 2, 3, 3, 4, 4, 3, 3, 3, 4, 4, 4, 3, 3, 4, 4, 3, 3, 2, 2,
                        2, 3, 3, 2, 2, 2, 3, 3, 4, 4, 4, 4, 4, 4, 4, 3, 3, 4, 4, 3, 3, 4, 4, 3, 3, 4, 4,
                        4, 4, 4, 3, 3, 3, 3, 2, 2, 2, 2, 2, 2, 3, 3, 4, 4
                };
        pukaDist = new byte[125];
            for (byte i = 0; i < maxDistance; i++) {
                pukaDist[pukaX[i] + 5 * pukaY[i] + 25 * pukaZ[i]] = i;
            }
            stored = true;
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
        distance = (distance + maxDistance) % maxDistance;
        return new long[]{pukaX[(int)distance], pukaY[(int)distance], pukaZ[(int)distance]};
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
        dimension %= 3;
        distance = (distance + maxDistance) % maxDistance;

        switch (dimension) {
            case 0:
                return pukaX[(int)distance];
            case 1:
                return pukaY[(int)distance];
            default:
                return pukaZ[(int)distance];
        }
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
        if(coordinates.length != 3)
            return -1;
        return pukaDist[(int) coordinates[0] + 5 * (int)coordinates[1] + 25 * (int)coordinates[2]];
    }

    /**
     * Not intended for external use. Part of the implementation of the Puka-Hilbert Curve.
     *
     * @param distance the distance to travel along the space-filling curve
     * @param direction between 0 and 5, inclusive
     * @param rotation between 0 and 3, inclusive
     * @return a long array, containing the x, y, z, etc. coordinates as elements to match the length of dimensionality
     */
    public long[] pointRotated(int distance, int direction, int rotation) {
        distance = (int)((distance + maxDistance) % maxDistance);
        switch (4 * direction + rotation)
        {
            case 0:
                return new long[]{pukaZ[distance], pukaX[distance], pukaY[distance]};
            case 1:
                return new long[]{pukaZ[distance], 4 - pukaY[distance], pukaX[distance]};
            case 2:
                return new long[]{pukaZ[distance], 4 - pukaX[distance], 4 - pukaY[distance]};
            case 3:
                return new long[]{pukaZ[distance], pukaY[distance], 4 - pukaX[distance]};
            case 4:
                return new long[]{pukaX[distance], pukaZ[distance], pukaY[distance]};
            case 5:
                return new long[]{4 - pukaY[distance], pukaZ[distance], pukaX[distance]};
            case 6:
                return new long[]{4 - pukaX[distance], pukaZ[distance], 4 - pukaY[distance]};
            case 7:
                return new long[]{pukaY[distance], pukaZ[distance], 4 - pukaX[distance]};
            case 8:
                return new long[]{pukaX[distance], pukaY[distance], pukaZ[distance]};
            case 9:
                return new long[]{4 - pukaY[distance], pukaX[distance], pukaZ[distance]};
            case 10:
                return new long[]{4 - pukaX[distance], 4 - pukaY[distance], pukaZ[distance]};
            case 11:
                return new long[]{pukaY[distance], 4 - pukaX[distance], pukaZ[distance]};


            case 12:
                return new long[]{4 - pukaZ[distance], pukaX[distance], pukaY[distance]};
            case 13:
                return new long[]{4 - pukaZ[distance], 4 - pukaY[distance], pukaX[distance]};
            case 14:
                return new long[]{4 - pukaZ[distance], 4 - pukaX[distance], 4 - pukaY[distance]};
            case 15:
                return new long[]{4 - pukaZ[distance], pukaY[distance], 4 - pukaX[distance]};
            case 16:
                return new long[]{pukaX[distance], 4 - pukaZ[distance], pukaY[distance]};
            case 17:
                return new long[]{4 - pukaY[distance], 4 - pukaZ[distance], pukaX[distance]};
            case 18:
                return new long[]{4 - pukaX[distance], 4 - pukaZ[distance], 4 - pukaY[distance]};
            case 19:
                return new long[]{pukaY[distance], 4 - pukaZ[distance], 4 - pukaX[distance]};
            case 20:
                return new long[]{pukaX[distance], pukaY[distance], 4 - pukaZ[distance]};
            case 21:
                return new long[]{4 - pukaY[distance], pukaX[distance], 4 - pukaZ[distance]};
            case 22:
                return new long[]{4 - pukaX[distance], 4 - pukaY[distance], 4 - pukaZ[distance]};
            default:
                return new long[]{pukaY[distance], 4 - pukaX[distance], 4 - pukaZ[distance]};

        }
    }
    /**
     * Not intended for external use. Part of the implementation of the Puka-Hilbert Curve.
     *
     * @param distance the distance to travel along the space-filling curve
     * @param dimension which dimension to get the rotated coordinate for
     * @param direction between 0 and 5, inclusive
     * @param rotation between 0 and 3, inclusive
     * @return the appropriate dimension's rotated coordinate for the point corresponding to distance traveled
     */
    public long coordinateRotated(int distance, int dimension, int direction, int rotation) {
        distance = (int)((distance + maxDistance) % maxDistance);
        switch (dimension) {
            case 0:
                switch (4 * direction + rotation) {
                    case 0:
                        return pukaZ[distance];
                    case 1:
                        return pukaZ[distance];
                    case 2:
                        return pukaZ[distance];
                    case 3:
                        return pukaZ[distance];
                    case 4:
                        return pukaX[distance];
                    case 5:
                        return 4 - pukaY[distance];
                    case 6:
                        return 4 - pukaX[distance];
                    case 7:
                        return pukaY[distance];
                    case 8:
                        return pukaX[distance];
                    case 9:
                        return 4 - pukaY[distance];
                    case 10:
                        return 4 - pukaX[distance];
                    case 11:
                        return pukaY[distance];
                    case 12:
                        return 4 - pukaZ[distance];
                    case 13:
                        return 4 - pukaZ[distance];
                    case 14:
                        return 4 - pukaZ[distance];
                    case 15:
                        return 4 - pukaZ[distance];
                    case 16:
                        return pukaX[distance];
                    case 17:
                        return 4 - pukaY[distance];
                    case 18:
                        return 4 - pukaX[distance];
                    case 19:
                        return pukaY[distance];
                    case 20:
                        return pukaX[distance];
                    case 21:
                        return 4 - pukaY[distance];
                    case 22:
                        return 4 - pukaX[distance];
                    default:
                        return pukaY[distance];

                }
            case 1:
                switch (4 * direction + rotation) {
                    case 0:
                        return pukaX[distance];
                    case 1:
                        return 4 - pukaY[distance];
                    case 2:
                        return 4 - pukaX[distance];
                    case 3:
                        return pukaY[distance];
                    case 4:
                        return pukaZ[distance];
                    case 5:
                        return pukaZ[distance];
                    case 6:
                        return pukaZ[distance];
                    case 7:
                        return pukaZ[distance];
                    case 8:
                        return pukaY[distance];
                    case 9:
                        return pukaX[distance];
                    case 10:
                        return 4 - pukaY[distance];
                    case 11:
                        return 4 - pukaX[distance];
                    case 12:
                        return pukaX[distance];
                    case 13:
                        return 4 - pukaY[distance];
                    case 14:
                        return 4 - pukaX[distance];
                    case 15:
                        return pukaY[distance];
                    case 16:
                        return 4 - pukaZ[distance];
                    case 17:
                        return 4 - pukaZ[distance];
                    case 18:
                        return 4 - pukaZ[distance];
                    case 19:
                        return 4 - pukaZ[distance];
                    case 20:
                        return pukaY[distance];
                    case 21:
                        return pukaX[distance];
                    case 22:
                        return 4 - pukaY[distance];
                    default:
                        return 4 - pukaX[distance];

                }
            default:
                switch (4 * direction + rotation) {
                    case 0:
                        return pukaY[distance];
                    case 1:
                        return pukaX[distance];
                    case 2:
                        return 4 - pukaY[distance];
                    case 3:
                        return 4 - pukaX[distance];
                    case 4:
                        return pukaY[distance];
                    case 5:
                        return pukaX[distance];
                    case 6:
                        return 4 - pukaY[distance];
                    case 7:
                        return 4 - pukaX[distance];
                    case 8:
                        return pukaZ[distance];
                    case 9:
                        return pukaZ[distance];
                    case 10:
                        return pukaZ[distance];
                    case 11:
                        return pukaZ[distance];
                    case 12:
                        return pukaY[distance];
                    case 13:
                        return pukaX[distance];
                    case 14:
                        return 4 - pukaY[distance];
                    case 15:
                        return 4 - pukaX[distance];
                    case 16:
                        return pukaY[distance];
                    case 17:
                        return pukaX[distance];
                    case 18:
                        return 4 - pukaY[distance];
                    case 19:
                        return 4 - pukaX[distance];
                    case 20:
                        return 4 - pukaZ[distance];
                    case 21:
                        return 4 - pukaZ[distance];
                    case 22:
                        return 4 - pukaZ[distance];
                    default:
                        return 4 - pukaZ[distance];

                }
        }
    }
}
