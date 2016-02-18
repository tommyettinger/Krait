package com.github.tommyettinger;

/**
 * A CurveStrategy for 2D Hilbert curves occupying a square with power-of-two side length. Side length must be between
 * 2 and 2^31 (in hex, 0x80000000), and will be clamped to that range. If a non-power-of-two length is requested, the
 * first greater power of two is used for a side length.
 * Created by Tommy Ettinger on 2/11/2016.
 */
public class Hilbert2DStrategy extends CurveStrategy {

    private byte[] bX, bY, bDist;
    private short[] sX, sY, sDist;
    private int[] iX, iY, iDist;

    /**
     * Equivalent to the order of this Hilbert Curve
     */
    private int bits;
    /**
     * Side length of the square.
     */
    private int side;
    private static final int DIMENSION = 2;

    /**
     * Constructs a Hilbert2DStrategy with side length 256, which will pre-calculate the 2^16 points of that Hilbert
     * Curve and store their x coordinates, y coordinates, and distances in short arrays.
     */
    public Hilbert2DStrategy()
    {
        this(256);
    }
    public Hilbert2DStrategy(int sideLength) {
        if(sideLength > 0x40000000)
        {
            sideLength = 0x40000000;
        }
        else if(sideLength <= 1)
        {
            sideLength = 2;
        }

        side = HilbertUtility.nextPowerOfTwo(sideLength);
        dimensionality = new int[]{side, side};
        maxDistance = side * side;
        distanceByteSize = calculateByteSize();
        bits = Long.numberOfTrailingZeros(side);
        //int xCoord = bits % 2 == 0 ? 0 : 1, yCoord = bits % 2 == 1 ? 0 : 1;
        if (maxDistance <= 0x100) {
            bX = new byte[maxDistance];
            bY = new byte[maxDistance];
            bDist = new byte[maxDistance];
            int[] c;
            for (int i = 0; i < maxDistance; i++) {
                c = distanceToPointSmall(i);
                bX[i] = (byte) c[0];
                bY[i] = (byte) c[1];
                bDist[c[0] + ((c[1]) << bits)] = (byte) i;
            }
            stored = true;
        }
        else if (maxDistance <= 0x10000) {
            sX = new short[maxDistance];
            sY = new short[maxDistance];
            sDist = new short[maxDistance];
            int[] c;
            for (int i = 0; i < maxDistance; i++) {
                c = distanceToPointSmall(i);
                sX[i] = (short) c[0];
                sY[i] = (short) c[1];
                sDist[c[0] + ((c[1]) << bits)] = (short) i;
            }
            stored = true;
        }
        else if (maxDistance <= 0x100000) {
            iX = new int[maxDistance];
            iY = new int[maxDistance];
            iDist = new int[maxDistance];
            int[] c;
            for (int i = 0; i < maxDistance; i++) {
                c = HilbertUtility.distanceToPoint(bits, DIMENSION, i);
                iX[i] =  c[0];
                iY[i] =  c[1];
                iDist[c[0] + ((c[1]) << bits)] = i;
            }
            stored = true;
        }
        else
        {
            stored = false;
        }
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
        if(stored)
        {
            switch (bits)
            {
                case 1:
                case 2:
                case 3:
                case 4:
                    return new int[]{bX[distance], bY[distance]};
                case 5:
                case 6:
                case 7:
                case 8:
                    return new int[]{sX[distance], sY[distance]};
                default:
                    return new int[]{iX[distance], iY[distance]};
            }
        }
        return HilbertUtility.distanceToPoint(bits, DIMENSION, distance);
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
        if(stored)
        {
            switch (bits)
            {
                case 1:
                case 2:
                case 3:
                case 4:
                    coordinates[0] = bX[distance];
                    coordinates[1] = bY[distance];
                    return coordinates;
                case 5:
                case 6:
                case 7:
                case 8:
                    coordinates[0] = sX[distance];
                    coordinates[1] = sY[distance];
                    return coordinates;
                default:
                    coordinates[0] = iX[distance];
                    coordinates[1] = iY[distance];
                    return coordinates;
            }
        }
        return HilbertUtility.distanceToPoint(coordinates, bits, DIMENSION, distance);
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
        dimension %= 2;
        distance = (distance + maxDistance) % maxDistance;
        if(stored)
        {
            switch (bits)
            {
                case 1:
                case 2:
                case 3:
                case 4:
                    if(dimension == 0)
                        return bX[distance];
                    else
                        return bY[distance];
                case 5:
                case 6:
                case 7:
                case 8:
                    if(dimension == 0)
                        return sX[distance];
                    else
                        return sY[distance];
                default:
                    if(dimension == 0)
                        return iX[distance];
                    else
                        return iY[distance];
            }
        }
        else
            return HilbertUtility.distanceToPoint(bits, DIMENSION, distance)[dimension];
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
        if(coordinates.length != 2)
            return -1;
        if(stored)
        {
            switch (bits)
            {
                case 1:
                case 2:
                case 3:
                case 4:
                    return bDist[coordinates[0] + (coordinates[1] << bits)];
                case 5:
                case 6:
                case 7:
                case 8:
                    return sDist[coordinates[0] + (coordinates[1] << bits)];
                default:
                    return iDist[coordinates[0] + (coordinates[1] << bits)];
            }
        }
        return pointToDistanceClosedForm(coordinates[0], coordinates[1]);

    }

    /**
     * Takes a distance to travel along the 2D Hilbert curve and returns a int array representing the position in 2D
     * space that corresponds to that point on the Hilbert curve. This variant does not use a lookup table.
     * <br>
     * Source: http://and-what-happened.blogspot.com/2011/08/fast-2d-and-3d-hilbert-curves-and.html
     * @param hilbert a int distance
     * @return a int array of coordinates representing a point
     */
    private int[] distanceToPointSmall( final int hilbert )
    {
        int x = 0, y = 0;
        int remap = 0xb4;
        int block = bits << 1;
        while( block > 0 )
        {
            block -= 2;
            int hcode = ( ( hilbert >>> block ) & 3 );
            int mcode = ( ( remap >>> ( hcode << 1 ) ) & 3 );
            remap ^= ( 0x330000cc >>> ( hcode << 3 ) );
            x = (x << 1) + (mcode & 1);
            y = (y << 1) + ((mcode & 2) >> 1);
        }
        return new int[]{x, y};
    }

    /**
     * Finds the distance along the Hilbert Curve for an x, y point. Should be faster than the n-dimensional general
     * case solution when only used for 2D Hilbert Curves.
     * <br>
     * Source: A Closed-Form Algorithm for Converting Hilbert Space-Filling Curve Indices
     * by Chih-Sheng Chen, Shen-Yi Lin, Min-Hsuan Fan, and Chua-Huang Huang
     * Retrieved from http://www.iaeng.org/IJCS/issues_v37/issue_1/IJCS_37_1_02.pdf
     * @param x x coordinate
     * @param y y coordinate
     * @return corresponding distance
     */
    private int pointToDistanceClosedForm(int x, int y)
    {
        int h = 0;
        int pos = side >>> 1;
        int mask = side-1;
        int tmp;
        for (int i=0; i<bits; i++) {
            int xi = (x & pos) >>> (bits-i-1);
            int yi = (y & pos) >>> (bits-i-1);
            x = x & mask; y = y & mask;
            if (xi==0 && yi==0) { // Case 1
                tmp = x; x = y; y = tmp;
            }
            else if (xi==1 && yi==0) { // Case 3
                xi = 1; yi = 1;
                tmp = x; x = y; y = tmp;
                x = ~x & mask; y = ~y & mask;
            }
            else if(!(xi==0 && yi==1)){ // Case 4
                xi = 1; yi = 0;
            }
            pos >>>= 1; mask >>>= 1;
            h = (((h << 1) | xi) << 1) | yi;
        }
        return h;
    }
}
