package com.github.tommyettinger;

import java.nio.ByteBuffer;

/**
 * A CurveStrategy for 2D Hilbert curves occupying a square with power-of-two side length, using NIO buffers internally
 * when pre-calculating distances and positions is a viable option. Side length must be between 2 and 2^31 (in hex,
 * 0x80000000), and will be clamped to that range. If a non-power-of-two length is requested, the first greater power of
 * two is used for a side length.
 * <br>
 * Use of this class is not recommended if you create many different sizes of Hilbert Curve, since the NIO buffers are
 * more expensive to get rid of.
 * Created by Tommy Ettinger on 2/11/2016.
 */
public class Hilbert2DStrategyNIO extends CurveStrategy {

    private ByteBuffer bX, bY, bDist;

    /**
     * Equivalent to the order of this Hilbert Curve
     */
    private int bits;
    /**
     * Side length of the square.
     */
    private long side;
    private static final int DIMENSION = 2;

    /**
     * Constructs a Hilbert2DStrategy with side length 256, which will pre-calculate the 2^16 points of that Hilbert
     * Curve and store their x coordinates, y coordinates, and distances in short arrays.
     */
    public Hilbert2DStrategyNIO()
    {
        this(256);
    }
    public Hilbert2DStrategyNIO(long sideLength) {
        if(sideLength <= 0x8000000000000000L || sideLength > 0x80000000L)
        {
            sideLength = 0x80000000L;
        }
        else if(sideLength <= 1)
        {
            sideLength = 2;
        }

        side = HilbertUtility.nextPowerOfTwo(sideLength);
        dimensionality = new long[]{side, side};
        maxDistance = side * side;
        distanceByteSize = calculateByteSize();
        bits = Long.numberOfTrailingZeros(side);
        //int xCoord = bits % 2 == 0 ? 0 : 1, yCoord = bits % 2 == 1 ? 0 : 1;
        if (maxDistance <= 0x100) {
            bX = ByteBuffer.allocateDirect((int)maxDistance);
            bY = ByteBuffer.allocateDirect((int)maxDistance);
            bDist = ByteBuffer.allocateDirect((int)maxDistance);
            long[] c;
            for (int i = 0; i < maxDistance; i++) {
                c = distanceToPointSmall(i);
                bX.put((byte) c[0]);
                bY.put((byte) c[0]);
                bDist.put((int)(c[0] | c[1] << bits), (byte) i);
            }
            stored = true;
        }
        else if (maxDistance <= 0x10000) {
            bX = ByteBuffer.allocateDirect((int)maxDistance * 2);
            bY = ByteBuffer.allocateDirect((int)maxDistance * 2);
            bDist = ByteBuffer.allocateDirect((int)maxDistance * 2);
            long[] c;
            for (int i = 0; i < maxDistance; i++) {
                c = distanceToPointSmall(i);
                bX.putShort((short) c[0]);
                bY.putShort((short) c[0]);
                bDist.putShort((int)(c[0] | c[1] << bits) << 1, (short) i);
            }
            stored = true;
        }
        else if (maxDistance <= 0x100000) {
            bX = ByteBuffer.allocateDirect((int)maxDistance * 4);
            bY = ByteBuffer.allocateDirect((int)maxDistance * 4);
            bDist = ByteBuffer.allocateDirect((int)maxDistance * 4);
            long[] c;
            for (int i = 0; i < maxDistance; i++) {
                c = HilbertUtility.distanceToPoint(bits, DIMENSION, i);
                bX.putInt((int) c[0]);
                bY.putInt((int) c[0]);
                bDist.putInt((int)(c[0] | c[1] << bits) << 2, i);
            }
            stored = true;
        }
        else
        {
            stored = false;
        }
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
        if(stored)
        {
            switch (bits)
            {
                case 1:
                case 2:
                case 3:
                case 4:
                    return new long[]{bX.get((int)distance), bY.get((int)distance)};
                case 5:
                case 6:
                case 7:
                case 8:
                    return new long[]{bX.getShort((int)distance * 2), bY.getShort((int)distance * 2)};
                default:
                    return new long[]{bX.getInt((int)distance * 4), bY.getInt((int)distance * 4)};
            }
        }
        return HilbertUtility.distanceToPoint(bits, DIMENSION, distance);
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
                        return bX.get((int)distance);
                    else
                        return bY.get((int)distance);
                case 5:
                case 6:
                case 7:
                case 8:
                    if(dimension == 0)
                        return bX.getShort((int)distance * 2);
                    else
                        return bY.getShort((int)distance * 2);
                default:
                    if(dimension == 0)
                        return bX.getInt((int)distance * 4);
                    else
                        return bY.getInt((int)distance * 4);
            }
        }
        else
            return HilbertUtility.distanceToPoint(bits, DIMENSION, distance)[dimension];
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
                    return bDist.get((int)(coordinates[0] | coordinates[1] << bits));
                case 5:
                case 6:
                case 7:
                case 8:
                    return bDist.getShort((int)(coordinates[0] | coordinates[1] << bits) << 1);
                default:
                    return bDist.getInt((int)(coordinates[0] | coordinates[1] << bits) << 2);
            }
        }
        return pointToDistanceClosedForm(coordinates[0], coordinates[1]);

    }

    /**
     * Takes a distance to travel along the 2D Hilbert curve and returns a long array representing the position in 2D
     * space that corresponds to that point on the Hilbert curve. This variant does not use a lookup table.
     * <br>
     * Source: http://and-what-happened.blogspot.com/2011/08/fast-2d-and-3d-hilbert-curves-and.html
     * @param hilbert a long distance
     * @return a long array of coordinates representing a point
     */
    private long[] distanceToPointSmall( final long hilbert )
    {
        long x = 0, y = 0;
        int remap = 0xb4;
        int block = bits << 1;
        while( block > 0 )
        {
            block -= 2;
            long hcode = ( ( hilbert >>> block ) & 3 );
            long mcode = ( ( remap >>> ( hcode << 1 ) ) & 3 );
            remap ^= ( 0x330000cc >>> ( hcode << 3 ) );
            x = (x << 1) + (mcode & 1);
            y = (y << 1) + ((mcode & 2) >> 1);
        }
        return new long[]{x, y};
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
    private long pointToDistanceClosedForm(long x, long y)
    {
        long h = 0;
        long pos = side >>> 1;
        long mask = side-1;
        long tmp;
        for (long i=0; i<bits; i++) {
            long xi = (x & pos) >>> (bits-i-1);
            long yi = (y & pos) >>> (bits-i-1);
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
