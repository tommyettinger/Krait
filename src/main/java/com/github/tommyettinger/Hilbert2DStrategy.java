package com.github.tommyettinger;

/**
 * Created by Tommy Ettinger on 2/11/2016.
 */
public class Hilbert2DStrategy extends CurveStrategy {

    private byte[] bX, bY, bDist;
    private short[] sX, sY, sDist;
    private int[] iX, iY, iDist;
    private int bits;
    private long side;
    private static final int DIMENSION = 2;
    public Hilbert2DStrategy()
    {
        this(256);
    }
    public Hilbert2DStrategy(long sideLength) {
        if(sideLength <= 1)
        {
            throw new UnsupportedOperationException("sideLength must be at least 2");
        }
        side = HilbertUtility.nextPowerOfTwo(sideLength);
        dimensionality = new long[]{side, side};
        maxDistance = side * side;
        bits = Long.numberOfTrailingZeros(side);
        int xCoord = bits % 2 == 0 ? 0 : 1, yCoord = bits % 2 == 1 ? 0 : 1;
        if (maxDistance <= 0x100) {
            bX = new byte[(int)maxDistance];
            bY = new byte[(int)maxDistance];
            bDist = new byte[(int)maxDistance];
            long[] c;
            for (int i = 0; i < maxDistance; i++) {
                c = distanceToPointSmall(i);
                bX[i] = (byte) c[xCoord];
                bY[i] = (byte) c[yCoord];
                bDist[(int)c[xCoord] + (((int)c[yCoord]) << bits)] = (byte) i;
            }
            stored = true;
        }
        else if (maxDistance <= 0x10000) {
            sX = new short[(int)maxDistance];
            sY = new short[(int)maxDistance];
            sDist = new short[(int)maxDistance];
            long[] c;
            for (int i = 0; i < maxDistance; i++) {
                c = distanceToPointSmall(i);
                sX[i] = (short) c[xCoord];
                sY[i] = (short) c[yCoord];
                sDist[(int)c[xCoord] + (((int)c[yCoord]) << bits)] = (short) i;
            }
            stored = true;
        }
        else if (maxDistance <= 0x1000000) {
            iX = new int[(int)maxDistance];
            iY = new int[(int)maxDistance];
            iDist = new int[(int)maxDistance];
            long[] c;
            for (int i = 0; i < maxDistance; i++) {
                c = HilbertUtility.distanceToPoint(bits, DIMENSION, i);
                iX[i] = (int) c[xCoord];
                iY[i] = (int) c[yCoord];
                iDist[(int)c[xCoord] + (((int)c[yCoord]) << bits)] = (short) i;
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
    public long[] getPoint(long distance) {
        distance %= maxDistance;
        if(stored)
        {
            switch (bits)
            {
                case 1:
                case 2:
                case 3:
                case 4:
                case 5:
                case 6:
                case 7:
                case 8:
                    return new long[]{bX[(int)distance], bY[(int)distance]};
                case 9:
                case 10:
                case 11:
                case 12:
                case 13:
                case 14:
                case 15:
                case 16:
                    return new long[]{sX[(int)distance], sY[(int)distance]};
                default:
                    return new long[]{iX[(int)distance], iY[(int)distance]};
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
        distance %= maxDistance;
        if(stored)
        {
            switch (bits)
            {
                case 1:
                case 2:
                case 3:
                case 4:
                case 5:
                case 6:
                case 7:
                case 8:
                    if(dimension == 0)
                        return bX[(int)distance];
                    else
                        return bY[(int)distance];
                case 9:
                case 10:
                case 11:
                case 12:
                case 13:
                case 14:
                case 15:
                case 16:
                    if(dimension == 0)
                        return sX[(int)distance];
                    else
                        return sY[(int)distance];
                default:
                    if(dimension == 0)
                        return iX[(int)distance];
                    else
                        return iY[(int)distance];
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
        int block = bits;
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
            else if (xi==0 && yi==1) { // Case 2
            }
            else if (xi==1 && yi==0) { // Case 3
                xi = 1; yi = 1;
                tmp = x; x = y; y = tmp;
                x = ~x & mask; y = ~y & mask;
            }
            else { // Case 4
                xi = 1; yi = 0;
            }
            pos >>>= 1; mask >>>= 1;
            h = (((h << 1) | xi) << 1) | yi;
        }
        return h;
    }
}
