package com.github.tommyettinger;

import com.googlecode.javaewah.IntIterator;
import com.googlecode.javaewah32.EWAHCompressedBitmap32;
import it.unimi.dsi.fastutil.ints.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


/**
 * Provides static methods to encode regions of 2D and 3D arrays of various sorts in extremely memory-efficient
 * representations, and decode those representations to various types of 2D or 3D array on-demand. Also provides static
 * methods to encode Coord and Coord3D objects as single primitive numbers in various ways.
 *<br>
 * The pack() methods in this class take a 2D array with a clear division between cells in an "on" state and cells in an
 * "off" state, and they produce a very tightly compressed short array that can be losslessly decompressed with the
 * unpack() methods to a boolean 2D array that stores equivalent on/off data to the input. The packMulti() method in
 * this class takes a double 2D array that has more than two states that may need to be encoded, such as an FOV map that
 * stores light level as a value between 0.0 and 1.0 instead of just on or off, and an additional double array that
 * defines what states should be distinguished in the result (for example, if the FOV can store values that differ by
 * 0.1 for a FOV radius of 10, you could pass the array of 10 levels: 0.1, 0.2, 0.3, ... 0.9, 1.0). The value returned
 * by packMulti() is a short[][], but with different array lengths for each sub-array (a jagged array); the length of
 * the short[][] is the same as the length of the levels array, and each sub-array corresponds to a different level of
 * FOV lighting or other gradation as defined in levels. This short[][] can be passed to the unpackMultiByte() method in
 * this class to produce a byte 2D array where the original levels correspond to progressively greater bytes, with 0
 * used for cells that were less than the smallest value in levels, 1 for values that were only greater than the
 * smallest value, and no others, in levels, then 2 for larger values, etc. until it places a byte with a value equal to
 * the length of levels in the cells that are the highest. There is also the unpackMultiDouble() method in this class
 * that takes the same short[][] unpackMultiByte() can take, but also takes a levels double array that should be the
 * same as the one used to compress short[][]. It will return a double 2D array with any cells that were smaller than
 * the smallest value in levels assigned 0.0, and any other cells will be assigned a double that corresponds to the
 * highest value in levels that does not exceed the original double at that location in the unpacked data. To make this
 * more clear, if you have 4 levels: [0.25, 0.5, 0.75, 1.0] and you packMulti() on an FOV with a large radius and
 * sample values 0.1, 0.45, 0.8, 1.0, you will get a packed short[][] with 4 sub-arrays to match the 4 levels. If you
 * then pass the short[][] and levels to unpackMultiDouble later, much of the same radius will be filled, but because
 * the sample value 0.1 was less than the smallest value in levels, its cell will be given 0.0. What was originally 0.45
 * will be given the next-lower levels value, 0.25; 0.8 will be given 0.75, and 1.0 will remain 1.0.
 *<br>
 * This compression is meant to produce a short[] or short[][] that uses as little memory as possible for the specific
 * case of compressing maps with these qualities:
 * <ul>
 *     <li>Maps are not especially large for a grid-based game; the maximum size is 256x256 cells.</li>
 *     <li>The vast majority of that 256x256 space is either unused or filled with cells no greater than 0.</li>
 *     <li>The cells that are greater than 0 are mostly near each other, though separate areas are possible.</li>
 * </ul>
 * <br>
 * The technique used by this class is to walk along a Hilbert Curve, storing whether the walk is traveling through
 * "on" or "off" cells, which can be determined by a comparison to a number or a boolean, then encoding alternate shorts
 * into the short[] to be returned, with even-number indices (starting at 0) in the array corresponding to the number of
 * contiguous cells walked through in the "off" state, and odd-number indices corresponding to the number of
 * contiguous cells walked through in the "on" state. A user of this library does not need to understand the details
 * and properties of this algorithm unless they want to generate maps that will compress more optimally. In short:
 * <ul>
 * <li>Smaller maps tend to be processed faster by pack(), since the nature of a Hilbert Curve means a map that
 * fits in one half the width and one half the height of the curve only needs to walk one quarter of the Curve to
 * get all the needed information.</li>
 * <li>Smaller maps also compress less optimally ratio-wise than larger maps with the same area of "on" cells. The
 * compression ratio approaches its best when using very large maps, such as 240x240, and encoding just a few
 * cells on that map (such as for a small FOV radius or a cramped room). A map that is entirely "off" uses only 16
 * bytes of RAM (the minimum for any array on the JVM).</li>
 * <li>Unusually shaped maps can cause compression problems by forcing adjacent cells to sometimes require walking
 * more cells than needed to get to an adjacent cell. For example, a map greater than 64 cells tall, but less than
 * 33 cells wide, has properties that require walking through a large empty area to get to sometimes only a few
 * cells that are "on" before it walks back through empty space. Similarly, a map that is greater than 128 cells
 * tall but is otherwise narrow has the same property of requiring walking through empty space, but also requires
 * the entire Curve to be walked even if the map's width is only a tiny fraction of the Curve's 256 cells.</li>
 * </ul>
 * <b>In shorter-than-short</b>, you'll get particularly good results for compression speed and compressed size with
 * maps approximately these sizes: 240x240, 240x120, 120x120, 60x120, 60x60, 60x30, 30x30. The biggest maps have the
 * best relative gain on compressed memory usage, and the smallest maps have the best compression speed.
 *<br>
 * The details of the algorithm are not terribly complex once you understand the Hilbert Curve. The simplified
 * version of the Hilbert Curve that Krait employs is essentially a path through a square grid (it must have side
 * lengths that are powers of 2, and Krait always uses 256), starting in the corner cell (x=0,y=0), ending in the
 * corner cell (x=0,y=255), and traversing every other cell on the grid along its path without ever traveling in a
 * loop, crossing the path it walked, or moving in any direction but one cell up, down, left, or right. The shape
 * of the path this takes has the useful property of keeping most groups of cells walked through with similar x and
 * y at similar distances traveled from the start of the curve, and most groups of cells with very dissimilar x and
 * y at very different distances traveled. Since FOV and several other things you might want to encode with RegionPacker
 * tends to be clustered in small areas and occupy more complicated shapes than straight lines due to dungeon layout
 * blocking sections of FOV, the simplest paths of a wide zigzag from side-to-side, or an outward-going-in spiral, have
 * rather poor behavior when determining how much of an area they pass through contiguously. The contiguous area trait
 * is important because of the next step: Run-Length Encoding.
 *<br>
 * Run-Length Encoding is much simpler to explain than the Hilbert Curve, especially without visual aids. In the version
 * Krait uses, only on or off states need to be recorded, so the method used here is smaller and more efficient than
 * most methods that need to store repeated characters in strings (and letters, numbers, and punctuation clearly have
 * more than 2 states). The technique works like this:
 *<br>
 * Start in the "off" state, walk down the Hilbert Curve counting how many cells you walk through that are still "off,"
 * and when you encounter a cell that is "on," you write down how many cells were off, transition to the "on" state. Now
 * keep walking the Hilbert Curve, but counting how many cells you walk through that are still "on." When you reach
 * an "off" cell, write down how many were "on," then start walking and counting again, with your count starting at 0.
 * Repeat until you reach the end of the Hilbert Curve, but if you reach the end while counting "off" cells, you don't
 * need to write down that number (a shortcut allows many maps to stop sooner than the 65,536th element of the Curve).
 *<br>
 * There are some additional traits that relate to the edge of the map being treated as "off" even though no
 * calculations are done for cells out of map bounds, and some optimizations that ensure that maps that are smaller than
 * a half, a quarter, or an eighth of the 256x256 curve in both dimensions (and sometimes just one) only need to walk a
 * portion of the Hilbert Curve and simply skip the rest without walking it.
 *<br>
 * The Hilbert Curve has not been definitively proven to be the best possible path to ensure 1D distance and 2D location
 * are similar, but it has been extensively used for tasks that require similar locations for similar distances (in
 * particular, it has become useful in supercomputing clusters for allocating related work to physically nearby
 * machines), and since there hasn't been anything with better spatial properties discovered yet, this technique should
 * remain useful for some time.
 * <br>
 * One contribution this class makes to the large amount of resources already present for space-filling curves is the
 * Puka Curve, a 5x5x5 potential "atom" for Hilbert Curves. This means it can replace an order-1 3D Hilbert Curve as
 * the building block for larger 3D Hilbert Curves, changing the size of an order-n curve made of Hilbert Curves as
 * atoms from a side length of 2 raised to the n, to a side length of 2 raised to the (n minus 1) times 5. An order-4
 * Hilbert Curve with the Hilbert atoms replaced with Puka atoms (thus having a side length of 40) is supplied here as
 * the Puka-Hilbert Curve, or PH Curve. You can fetch x, y, and z positions for distances from it using the fields ph3X,
 * ph3Y, and ph3Z. You can fetch distances as unsigned shorts given a coded x, y, z index; the distance for an x,y,z
 * point is stored with x in the most significant position, y in the middle, and z as least significant, so this index:
 * {@code ph3Distances[x * 1600 + y * 40 + z]} will equal the distance to travel along the PH Curve to get to that
 * x,y,z position. One possible variant of the Puka Curve is http://i.imgur.com/IJzIkio.png
 * Created by Tommy Ettinger on 10/1/2015.
 * @author Tommy Ettinger
 */
public class RegionPacker {

    public final EWAHCompressedBitmap32 ALL_OFF,
            ALL_ON;
    private static final int[] manhattan_100 = new int[]
            {
                    0, 1, 3, 6, 10, 15, 21, 28, 36, 45, 55, 66, 78, 91, 105, 120, 136,
                    153, 171, 190, 210, 231, 253, 276, 300, 325, 351, 378, 406, 435, 465,
                    496, 528, 561, 595, 630, 666, 703, 741, 780, 820, 861, 903, 946, 990,
                    1035, 1081, 1128, 1176, 1225, 1275, 1326, 1378, 1431, 1485, 1540,
                    1596, 1653, 1711, 1770, 1830, 1891, 1953, 2016, 2080, 2145, 2211,
                    2278, 2346, 2415, 2485, 2556, 2628, 2701, 2775, 2850, 2926, 3003,
                    3081, 3160, 3240, 3321, 3403, 3486, 3570, 3655, 3741, 3828, 3916,
                    4005, 4095, 4186, 4278, 4371, 4465, 4560, 4656, 4753, 4851, 4950
            };
    public CurveStrategy curve;
    public RegionPacker()
    {
        this(new Hilbert2DStrategy(256));
    }
    public RegionPacker(CurveStrategy curveStrategy)
    {
        curve = curveStrategy;
        ALL_OFF = new EWAHCompressedBitmap32(1);
        ALL_ON = new EWAHCompressedBitmap32();
        ALL_ON.not();
    }

    /**
     * Given a point as an array or vararg of int coordinates and the bounds as an array of int dimension lengths,
     * computes an index into a 1D array that matches bounds. The value this returns will be between 0 (inclusive) and
     * the product of each element in bounds (exclusive), unless the given point does not fit in bounds. If point is
     * out-of-bounds, this always returns -1, and callers should check for -1 as an output.
     * @param bounds the bounding dimension lengths as an int array
     * @param point the coordinates of the point to encode as a int array or vararg; must have a length at least equal
     *              to the length of bounds, and only items that correspond to dimensions in bounds will be used
     * @return an index into a 1D array that is sized to contain all of bounds, or -1 if point is invalid
     */
    public static int boundedIndex(int[] bounds, int... point)
    {
        int u = 0;
        for (int a = 0; a < bounds.length; a++) {
            if(point[a] >= bounds[a])
                return -1;
            else
            {
                u *= bounds[a];
                u += point[a];
            }
        }
        return u;
    }

    /**
     * Given an index into a bounded 1D array as produced by boundedIndex(), reproduces the point at the bounded index.
     * The point is represented as an int array with equivalent dimension count as bounds. The earliest dimension, at
     * index 0, is considered the most significant for the integer value of index.
     * @param bounds the bounding dimension lengths as an int array
     * @param index the index, as could be produced by boundedIndex()
     * @return the point referred to by index within the given bounds.
     */
    public static int[] fromBounded(int[] bounds, int index)
    {
        if(index < 0)
            throw new ArrayIndexOutOfBoundsException("Index must not be negative");
        int[] point = new int[bounds.length];
        int u = 1;
        for (int a = bounds.length - 1; a >= 0; a--) {
            point[a] = (index / u) % bounds[a];
            u *= bounds[a];
        }
        return point;
    }
    private int validateBounds(int[] bounds)
    {
        if(bounds == null || curve.dimensionality.length != bounds.length)
            throw new UnsupportedOperationException("Invalid bounds; should be an array with " +
                    curve.dimensionality.length + " elements");
        long b = 1;
        for (int i = 0; i < bounds.length; i++) {
            if(bounds[i] > curve.dimensionality[i])
                throw new UnsupportedOperationException("Bound size at dimension " + i
                        + " is too large for given CurveStrategy, should be no more than " + curve.dimensionality[i]);
            b *= bounds[i];
        }
        if(b > 1L << 30)
            throw new UnsupportedOperationException("Bounds are too big!");

        return (int)b;
    }
    /**
     * Compresses a boolean array of data encoded so the lowest-index dimensions are the most significant, using the
     * specified bounds to determine the conversion from n-dimensional to 1-dimensional, returning a compressed bitmap
     * from the JavaEWAH library, EWAHCompressedBitmap32, which can be passed to most methods of this class
     * (RegionPacker). In most cases, if you have data not produced by this library, you will find it easier to use the
     * overload of pack() that takes a LinearData object. You can construct a LinearData object using a
     * multi-dimensional boolean array, which may match how you produce your data more closely.
     *
     * @param data a boolean array that is encoded so the first dimension in bounds is the most significant for an index
     * @param bounds the limits of the dimensions for the space that data refers to
     * @return a packed short[] that should, in most circumstances, be passed to unpack() when it needs to be used.
     */
    public EWAHCompressedBitmap32 pack(boolean[] data, int[] bounds)
    {
        if(data == null || data.length == 0)
            throw new UnsupportedOperationException("RegionPacker.pack() must be given a non-empty array");
        validateBounds(bounds);

        EWAHCompressedBitmap32 packing = new EWAHCompressedBitmap32();
        int[] pt = new int[bounds.length];
        for (int i = 0, len = 0, idx; i < curve.maxDistance && len < data.length; i++) {
            idx = boundedIndex(bounds, curve.alter(pt, i));
            if(idx >= 0)
            {
                len++;
                if(data[idx])
                    packing.set(i);
            }
        }
        if(packing.isEmpty())
            return ALL_OFF;
        return packing;
    }

    /**
     * When you have some data in a multi-dimensional boolean array, you can pass it to a LinearData constructor and
     * pass that LinearData to this method if you want to manipulate it with this class. This variant of pack() handles
     * bounds automatically, using the LinearData's stored bounds (which typically match the dimension lengths for the
     * multi-dimensional boolean array that was passed to the LinearData constructor) to determine whether positions are
     * in the encoded area or not. Returns a compressed bitmap from the JavaEWAH library, EWAHCompressedBitmap32, which
     * can be passed to most methods of this class (RegionPacker).
     * @param linear a LinearData object that can be constructed using a multi-dimensional boolean array
     * @return a packed bitmap
     */
    public EWAHCompressedBitmap32 pack(LinearData linear)
    {
        if(linear == null || linear.data == null || linear.data.length == 0)
            throw new UnsupportedOperationException("RegionPacker.pack() was given an invalid LinearData object");
        return pack(linear.data, linear.bounds);
    }

    /**
     * Decompresses a packed bitmap returned by pack(), as described in
     * the {@link RegionPacker} class documentation. This returns a boolean[] that stores the same values that were
     * packed if the overload of pack() taking a boolean[] was used.
     * @param packed a packed bitmap encoded by calling one of this class' packing methods.
     * @param bounds the dimensions of the area to encode; must be no larger in any dimension than the dimensionality of
     *               the CurveStrategy this RegionPacker was constructed with.
     * @return a 1D boolean array representing the multi-dimensional area of bounds, where true is on and false is off
     */
    public boolean[] unpack(EWAHCompressedBitmap32 packed, final int[] bounds)
    {
        if(packed == null)
            throw new UnsupportedOperationException("RegionPacker.unpack() must be given a non-null Region");

        int b = validateBounds(bounds);

        final boolean[] unpacked = new boolean[b];

        if(packed.isEmpty())
            return unpacked;

        IntIterator it = packed.intIterator();
        int[] pt = new int[bounds.length];
        int idx;
        while (it.hasNext())
        {
            idx = boundedIndex(bounds, curve.alter(pt, it.next()));
            if(idx >= 0)
                unpacked[idx] = true;
        }
        return unpacked;
    }
    public LinearData unpackLinearData(EWAHCompressedBitmap32 packed, final int[] bounds)
    {
        return new LinearData(unpack(packed, bounds), bounds);
    }

    /*
     * Decompresses a short[] returned by pack() or a sub-array of a short[][] returned by packMulti(), as described in
     * the {@link RegionPacker} class documentation. This returns a double[][] that stores 1.0 for true and 0.0 for
     * false if the overload of pack() taking a boolean[][] was used. If a double[][] was compressed with pack(), the
     * double[][] this returns will have 1.0 for all values greater than 0 and 0.0 for all others. If this is one
     * of the sub-arrays compressed by packMulti(), the index of the sub-array will correspond to an index in the levels
     * array passed to packMulti(), and any cells that were at least equal to the corresponding value in levels will be
     * 1.0, while all others will be 0.0. Width and height do not technically need to match the dimensions of the
     * original 2D array, but under most circumstances where they don't match, the data produced will be junk.
     * @param packed a short[] encoded by calling one of this class' packing methods on a 2D array.
     * @param width the width of the 2D array that will be returned; should match the unpacked array's width.
     * @param height the height of the 2D array that will be returned; should match the unpacked array's height.
     * @return a double[][] storing which cells encoded by packed are on (1.0) or off (0.0).
     * /
    public static double[][] unpackDoubleConical(short[] packed, int width, int height,  int centerX, int centerY,
                                                 double angle, double span)
    {
        if(packed == null)
            throw new ArrayIndexOutOfBoundsException("RegionPacker.unpack() must be given a non-null array");
        double[][] unpacked = new double[width][height];
        if(packed.length == 0)
            return unpacked;
        boolean on = false;
        int idx = 0;
        short x =0, y = 0;
        double angle2 = Math.toRadians((angle > 360.0 || angle < 0.0) ? Math.IEEEremainder(angle + 720.0, 360.0) : angle);
        double span2 = Math.toRadians(span);

        for(int p = 0; p < packed.length; p++, on = !on) {
            if (on) {
                for (int toSkip = idx +(packed[p] & 0xffff); idx < toSkip && idx < 0x10000; idx++) {
                    x = hilbertX[idx];
                    y = hilbertY[idx];
                    if(x >= width || y >= height)
                        continue;
                    double newAngle = Math.atan2(y - centerY, x - centerX) + Math.PI * 2;
                    if(Math.abs(Math.IEEEremainder(angle2 - newAngle, Math.PI * 2)) > span2 / 2.0)
                        unpacked[x][y] = 0.0;
                    else
                        unpacked[x][y] = 1.0;
                }
            } else {
                idx += packed[p] & 0xffff;
            }
        }
        return unpacked;
    }
    */
    /*
     * Decompresses a short[][] returned by packMulti() and produces an approximation of the double[][] it compressed
     * using the given levels double[] as the values to assign, as described in the {@link RegionPacker} class
     * documentation. The length of levels and the length of the outer array of packed must be equal. However, the
     * levels array passed to this method should not be identical to the levels array passed to packMulti(); for FOV
     * compression, you should get an array for levels using generatePackingLevels(), but for decompression, you should
     * create levels using generateLightLevels(), which should more appropriately fit the desired output. Reusing the
     * levels array used to pack the FOV will usually produce values at the edge of FOV that are less than 0.01 but
     * greater than 0, and will have a maximum value somewhat less than 1.0; neither are usually desirable, but using a
     * different array made with generateLightLevels() will produce doubles ranging from 1.0 / levels.length to 1.0 at
     * the highest. Width and height do not technically need to match the dimensions of the original 2D array, but under
     * most circumstances where they don't match, the data produced will be junk.
     * @param packed a short[][] encoded by calling this class' packMulti() method on a 2D array.
     * @param width the width of the 2D array that will be returned; should match the unpacked array's width.
     * @param height the height of the 2D array that will be returned; should match the unpacked array's height.
     * @param levels a double[] that must have the same length as packed, and will be used to assign cells in the
     *               returned double[][] based on what levels parameter was used to compress packed
     * @return a double[][] where the values that corresponded to the nth value in the levels parameter used to
     * compress packed will now correspond to the nth value in the levels parameter passed to this method.
     * /
    public static double[][] unpackMultiDouble(short[][] packed, int width, int height, double[] levels)
    {
        if(packed == null || packed.length == 0)
            throw new ArrayIndexOutOfBoundsException(
                    "RegionPacker.unpackMultiDouble() must be given a non-empty array");
        if (levels == null || levels.length != packed.length)
            throw new UnsupportedOperationException("The lengths of packed and levels must be equal");
        if (levels.length > 63)
            throw new UnsupportedOperationException(
                    "Too many levels to be packed by RegionPacker; should be less than 64 but was given " +
                            levels.length);
        double[][] unpacked = new double[width][height];
        short x= 0, y = 0;
        for(int l = 0; l < packed.length; l++) {
            boolean on = false;
            int idx = 0;
            for (int p = 0; p < packed[l].length; p++, on = !on) {
                if (on) {
                    for (int toSkip = idx + (packed[l][p] & 0xffff); idx < toSkip && idx < 0x10000; idx++) {
                        x = hilbertX[idx];
                        y = hilbertY[idx];
                        if(x >= width || y >= height)
                            continue;
                        unpacked[x][y] = levels[l];
                    }
                } else {
                    idx += packed[l][p] & 0xffff;
                }
            }
        }
        return unpacked;
    }
    */

    /**
     * Quickly determines if an x,y position is true or false in the given packed data, without unpacking it.
     * @param packed a packed bitmap returned by pack() or a related method; must not be null.
     * @param coordinates a vararg or array of coordinates; should have length equal to curve dimensions
     * @return true if the packed data stores true at the given x,y location, or false in any other case.
     */
    public boolean query(EWAHCompressedBitmap32 packed, int... coordinates)
    {
        int hilbertDistance = curve.distance(coordinates), total = 0;
        if(hilbertDistance < 0)
            return false;
        return packed.get(hilbertDistance);
    }

    /**
     * Quickly determines if a space-filling curve index corresponds to true or false in the given packed data, without
     * unpacking it.
     * @param packed a packed bitmap returned by pack() or a related method; must not be null.
     * @param hilbert a space-filling curve index, such as one taken directly from packed data without extra processing
     * @return true if the packed data stores true at the given space-filling curve index, or false in any other case.
     */
    public boolean queryCurve(EWAHCompressedBitmap32 packed, int hilbert)
    {
        if(hilbert < 0 || hilbert >= curve.maxDistance)
            return false;
        return packed.get(hilbert);
    }

    /**
     * Gets all positions that are "on" in the given packed array, without unpacking it, and returns them as a Coord[].
     * @param packed a packed bitmap returned by pack() or a similar method
     * @return an array of int arrays representing points, ordered by distance for those points along the space-filling
     * curve, corresponding to all "on" cells in packed.
     */
    public int[][] positions(EWAHCompressedBitmap32 packed)
    {
        int[][] cs = new int[packed.cardinality()][curve.dimensionality.length];
        IntIterator it = packed.intIterator();
        int i = 0;
        while (it.hasNext())
            cs[i++] = curve.point(it.next());
        return cs;
    }
    /**
     * Gets all positions that are "on" in the given packed array, without unpacking it, and returns them as an array of
     * space-filling curve indices.
     * @param packed a packed bitmap returned by pack() or a similar method
     * @return a space-filling crve index array, in ascending distance order, corresponding to all "on" cells in packed.
     */
    public int[] positionsCurve(EWAHCompressedBitmap32 packed)
    {
        return packed.toArray();
    }

    public List<EWAHCompressedBitmap32> containing(int[] point, EWAHCompressedBitmap32... packedMany)
    {
        List<EWAHCompressedBitmap32> results = new ArrayList<EWAHCompressedBitmap32>(packedMany.length);
        EWAHCompressedBitmap32 single = packOne(point);
        for (int i = 0; i < packedMany.length; i++) {
            if(single.intersects(packedMany[i]))
                results.add(packedMany[i]);
        }
        return results;
    }
    /**
     * Given a packed bitmap and a fraction (as a numerator and denominator), copies a portion of the positions in
     * packed and returns the copy as a new packed bitmap. For every (denominator) positions in packed, (numerator) will
     * be used, with slightly more than that fraction being used if the number of positions in packed does not divide
     * evenly by denominator. The number of positions in the returned packed bitmap, if it has position count c, will be
     * between {@code (c * numerator) / denominator} and {@code (c * numerator) / denominator + numerator}.
     * Returns a new packed bitmap and does not modify packed.
     * @param packed the packed bitmap to copy and get a fraction of
     * @param numerator the numerator of the fraction to use; should be positive and less than denominator
     * @param denominator the denominator of the fraction to use; should be positive and greater than numerator
     * @return a new packed bitmap that encodes the specified fraction of positions from packed
     */
    public EWAHCompressedBitmap32 fraction(EWAHCompressedBitmap32 packed, int numerator, int denominator)
    {
        if(numerator >= denominator || denominator <= 0)
            return copy(packed);
        EWAHCompressedBitmap32 next = new EWAHCompressedBitmap32();
        if(numerator <= 0)
            return next;
        IntIterator it = packed.intIterator();
        int c = 0, i;
        while (it.hasNext())
        {
            i = it.next();
            if(c < numerator)
                next.set(i);
            c = (c + 1) % denominator;
        }
        return next;
    }

    private static int clamp(int n, int min, int max)
    {
        return Math.min(Math.max(min, n), max - 1);
    }

    private int clampedDistanceTranslateInPlace(int[] pt, int[] bounds, int[] movement)
    {
        for (int i = 0; i < pt.length; i++) {
            pt[i] = clamp(pt[i] + movement[i], 0, bounds[i]);
        }
        return curve.distance(pt);
    }

    private int clampedDistanceTranslate(int[] pt, int[] bounds, int[] movement)
    {
        int[] pt2 = new int[pt.length];
        for (int i = 0; i < pt.length; i++) {
            pt2[i] = clamp(pt[i] + movement[i], 0, bounds[i]);
        }
        return curve.distance(pt2);
    }

    private int edgyDistanceTranslate(int[] pt, int[] bounds, int[] movement, int multiplier)
    {
        int[] pt2 = new int[pt.length];
        for (int i = 0; i < pt.length; i++) {
            pt2[i] = pt[i] + movement[i] * multiplier;
            if(pt2[i] < 0 || pt2[i] >= bounds[i])
                return -1;
        }
        return curve.distance(pt2);
    }

    private void assignExpand(IntSortedSet values, int[] pt, int[] bounds, int[][] movers)
    {
        for (int i = 0; i < movers.length; i++) {
            values.add(clampedDistanceTranslate(pt, bounds, movers[i]));
        }
    }

    private void assignExpand(IntSortedSet values, IntSet checks, int[] pt, int[] bounds, int[][] movers)
    {
        int temp;
        for (int i = 0; i < movers.length; i++) {
            temp = clampedDistanceTranslate(pt, bounds, movers[i]);
            if (checks.add(temp)) {
                values.add(temp);
            }
        }
    }
    private void assignOpposed(IntSortedSet values, IntSet checks, int[] pt, int[] bounds, int[][] movers)
    {
        int temp, beyond;
        for (int i = 0; i < movers.length; i++) {
            temp = edgyDistanceTranslate(pt, bounds, movers[i], 1);
            if(temp < 0 || checks.contains(temp))
                continue;
            beyond = edgyDistanceTranslate(pt, bounds, movers[i], 2);
            if (beyond < 0 || checks.contains(beyond)) {
                values.add(temp);
            }
        }
    }
    private void assignOpposed(IntSortedSet values, IntSet checks, int[] pt, int[] bounds, int[][] movers,
                               IntSet temporary, int depth)
    {
        int beyond, tempPos;
        CELL_WISE:
        for (int i = 0; i < movers.length; i++) {
            temporary.clear();
            for (int j = 1; j <= depth; j++) {
                tempPos = edgyDistanceTranslate(pt, bounds, movers[i], j);
                if(tempPos < 0 || checks.contains(tempPos)) {
                    temporary.clear();
                    continue CELL_WISE;
                }
                temporary.add(tempPos);
            }
            beyond = edgyDistanceTranslate(pt, bounds, movers[i], depth + 1);
            if (beyond < 0 || checks.contains(beyond)) {
                values.addAll(temporary);
            }
        }
    }

    private void assignFlood(IntSortedSet values, IntSet checks, IntSet edge, IntSet needs, int[] pt, int[][] movers)
    {
        int temp;
        for (int i = 0; i < movers.length; i++) {
            temp = clampedDistanceTranslate(pt, curve.dimensionality, movers[i]);
            if (needs.contains(temp) && checks.add(temp)) {
                values.add(temp);
                edge.add(temp);
            }
        }
    }

    private void computeEdge(IntSet values, IntSet edge, IntSet needs, int[] pt, int[][] movers)
    {
        int temp;
        for (int i = 0; i < movers.length; i++) {
            temp = clampedDistanceTranslate(pt, curve.dimensionality, movers[i]);
            if (needs.contains(temp) && !values.contains(temp)) {
                edge.add(temp);
            }
        }
    }

    private int assignRandomFlood(IntSortedSet values, IntSet checks, IntSet edge, IntSet needs,
                                   int[][] movers, RNG rng)
    {
        int temp = rng.getRandomElement(edge);
        if (needs.contains(temp) && checks.add(temp)) {
            values.add(temp);
            edge.remove(temp);
            computeEdge(values, edge, needs, curve.point(temp), movers);
            return 1;
        }
        return 0;
    }

    private int[][] expandChebyshev(int expansion)
    {
        int[] move = new int[curve.dimensionality.length];
        int side = expansion * 2 + 1, exp = (int)Math.pow(side, move.length), run;
        int[][] movers = new int[exp][move.length];
        for (int i = 0; i < exp; i++) {
            run = 1;
            for (int d = 0; d < move.length; d++) {
                move[d] = ((i / run) % side) - expansion;
                run *= side;
            }
            System.arraycopy(move, 0, movers[i], 0, move.length);
        }
        return movers;
    }

    private int[][] expandManhattan(int expansion)
    {
        int[] move = new int[curve.dimensionality.length];
        if(expansion < 0) expansion = 0;
        else if(expansion > 100) expansion = 100;
        int side = expansion * 2 + 1, cube = (int)Math.pow(side, move.length),
                exp = 1 + 2 * move.length * manhattan_100[expansion], m = 0, length, run;
        int[][] movers = new int[exp][move.length];
        CELL_WISE:
        for (int i = 0; i < cube && m < exp; i++)
        {
            run = 1;
            length = 0;
            for (int d = 0; d < move.length; d++) {
                move[d] = ((i / run) % side) - expansion;
                if((length += Math.abs(move[d])) > expansion) continue CELL_WISE;
                run *= side;
            }
            System.arraycopy(move, 0, movers[m++], 0, move.length);
        }
        return movers;
    }

    private int[][] expandMetric(Metric metric, int expansion)
    {

        if(metric == Metric.CHEBYSHEV) return expandChebyshev(expansion);
        else if(metric == Metric.MANHATTAN && expansion <= 100) return expandManhattan(expansion);

        int[] move = new int[curve.dimensionality.length];
        if(expansion < 0) expansion = 0;
        int side = expansion * 2 + 1, exp = (int)Math.pow(side, move.length), m = 0, run;
        int[][] movers = new int[exp][move.length];
        for (int i = 0; i < exp; i++)
        {
            run = 1;
            for (int d = 0; d < move.length; d++) {
                move[d] = ((i / run) % side) - expansion;
                run *= side;
            }
            if(metric.withinGridDistance(expansion, move))
                System.arraycopy(move, 0, movers[m++], 0, move.length);
        }
        return Arrays.copyOf(movers, m);
    }
    /**
     * Move all "on" positions in packed by the number of cells given in xMove and yMove, unless the move
     * would take them further than 0, width - 1 (for xMove) or height - 1 (for yMove), in which case that
     * cell is stopped at the edge (moving any shape by an xMove greater than width or yMove greater than
     * height will move all "on" cells to that edge, in a 1-cell thick line). Returns a new packed short[]
     * and does not modify packed.
     * @param packed a packed bitmap returned by pack() or a similar method
     * @param bounds the bounds of the positions to translate; bits will stop before they hit the bounds or go negative
     * @param movement an array that should have identical length to bounds; stores movement in each dimension to apply
     * @return new packed data that encodes "on" for cells that were moved from cells that were "on" in packed
     */
    public EWAHCompressedBitmap32 translate(EWAHCompressedBitmap32 packed, int[] bounds, int[] movement)
    {
        if(packed == null || packed.isEmpty())
        {
            return ALL_OFF;
        }
        IntSortedSet ints = new IntRBTreeSet();
        IntIterator it = packed.intIterator();
        int[] pt = new int[bounds.length];
        while (it.hasNext()) {
            ints.add(clampedDistanceTranslateInPlace(curve.alter(pt, it.next()), bounds, movement));
        }
        if(ints.size() < 1)
            return ALL_OFF;

        return EWAHCompressedBitmap32.bitmapOf(ints.toIntArray());
    }

    /**
     * Expand each "on" position in packed to cover a diamond in 2D, octahedron in 3D, or cross polytope in higher
     * dimensions, with the center of each cell and the cells with a Manhattan distance of expansion or less
     * included, unless the expansion would take a cell further than 0 or out of the appropriate dimension in bounds, in
     * which case that cell is stopped at the edge.
     * Returns a new packed bitmap and does not modify packed.
     * @param packed a packed bitmap returned by pack() or a similar method
     * @param expansion the positive (diamond) radius, in cells, to expand each cell out by; clamped at 100
     * @param bounds the bounds of the positions to expand; bits will stop before they hit the bounds or go negative
     * @return a packed bitmap that encodes "on" for packed and cells that expanded from cells that were "on" in packed
     */
    public EWAHCompressedBitmap32 expand(EWAHCompressedBitmap32 packed, int expansion, int[] bounds)
    {
        if(packed == null || packed.isEmpty())
        {
            return ALL_OFF;
        }
        int[][] movers = expandManhattan(expansion);
        IntSortedSet ints = new IntRBTreeSet();
        IntIterator it = packed.intIterator();
        int[] pt = new int[bounds.length];
        while (it.hasNext()) {
            assignExpand(ints, curve.alter(pt, it.next()), bounds, movers);
        }

        if(ints.isEmpty())
            return ALL_OFF;

        return EWAHCompressedBitmap32.bitmapOf(ints.toIntArray());
    }


    /**
     * Expand each "on" position in packed to cover an n-dimensional region, one of square/cube/hypercube,
     * diamond/octahedron/cross polytope, or circle/sphere/hypersphere (for 2D/3D/higher dimensions) depending on the
     * metric parameter (CHEBYSHEV produces a square, MANHATTAN produces a diamond, while EUCLIDEAN and EUCLIDEAN_STRICT
     * produce circles in 2D). The center of each cell and the cells with a distance of expansion or less (using metric)
     * are included, unless the expansion would take a cell further than 0 or out of the appropriate dimension in
     * bounds, in which case that cell is stopped at the edge.
     * Returns a new packed bitmap and does not modify packed.
     * @param packed a packed bitmap returned by pack() or a similar method
     * @param expansion the positive (square, diamond, or circular) radius, in cells, to expand each cell out by
     * @param bounds the bounds of the positions to expand; bits will stop before they hit the bounds or go negative
     * @param metric the distance metric to use; a Metric enum from this package
     * @return a packed bitmap that encodes "on" for packed and cells that expanded from cells that were "on" in packed
     */
    public EWAHCompressedBitmap32 expand(EWAHCompressedBitmap32 packed, int expansion, int[] bounds, Metric metric)
    {
        if(packed == null || packed.isEmpty())
        {
            return ALL_OFF;
        }
        int[][] movers = expandMetric(metric, expansion);
        IntSortedSet ints = new IntRBTreeSet();
        IntIterator it = packed.intIterator();
        int[] pt = new int[bounds.length];
        while (it.hasNext()) {
            assignExpand(ints, curve.alter(pt, it.next()), bounds, movers);
        }

        if(ints.isEmpty())
            return ALL_OFF;

        return EWAHCompressedBitmap32.bitmapOf(ints.toIntArray());
    }


    /**
     * Finds the area around the cells encoded in packed, without including those cells. Searches the area around each
     * "on" position in packed to cover a diamond in 2D, octahedron in 3D, or cross polytope in higher
     * dimensions, with the cells with a Manhattan distance of expansion or less included, unless the expansion would
     * take a cell further than 0 or out of the appropriate dimension in bounds, in which case that cell is stopped at
     * the edge, or a cell would overlap with the cells in packed, in which case it is not included at all.
     * Returns a new packed bitmap and does not modify packed.
     * @param packed a packed bitmap returned by pack() or a similar method
     * @param expansion the positive (diamond) radius, in cells, to push each cell out by; clamped at 100
     * @param bounds the bounds of the positions to expand; bits will stop before they hit the bounds or go negative
     * @return a packed bitmap that encodes "on" for cells that were pushed from the edge of packed's "on" cells
     */
    public EWAHCompressedBitmap32 fringe(EWAHCompressedBitmap32 packed, int expansion, int[] bounds)
    {
        if(packed == null || packed.isEmpty())
        {
            return ALL_OFF;
        }
        int[][] movers = expandManhattan(expansion);
        IntSet checks = new IntOpenHashSet(packed.toArray());
        IntSortedSet ints = new IntRBTreeSet();
        IntIterator it = packed.intIterator();
        int[] pt = new int[bounds.length];
        while (it.hasNext()) {
            assignExpand(ints, checks, curve.alter(pt, it.next()), bounds, movers);
        }

        if(ints.isEmpty())
            return ALL_OFF;

        return EWAHCompressedBitmap32.bitmapOf(ints.toIntArray());
    }

    /**
     * Finds the area around the cells encoded in packed, without including those cells. Searches the area around each
     * "on" position in packed to cover an n-dimensional region, one of square/cube/hypercube,
     * diamond/octahedron/cross polytope, or circle/sphere/hypersphere (for 2D/3D/higher dimensions) depending on the
     * metric parameter (CHEBYSHEV produces a square, MANHATTAN produces a diamond, while EUCLIDEAN and EUCLIDEAN_STRICT
     * produce circles in 2D). The center of each cell and the cells with a distance of expansion or less (using metric)
     * are included, unless the expansion would take a cell further than 0 or out of the appropriate dimension in
     * bounds, in which case that cell is stopped at the edge, or a cell would overlap with the cells in packed, in
     * which case it is not included at all.
     * Returns a new packed bitmap and does not modify packed.
     * @param packed a packed bitmap returned by pack() or a similar method
     * @param expansion the positive (square, diamond, or circular) radius, in cells, to push each cell out by
     * @param bounds the bounds of the positions to expand; bits will stop before they hit the bounds or go negative
     * @param metric the distance metric to use; a Metric enum from this package
     * @return a packed bitmap that encodes "on" for cells that were pushed from the edge of packed's "on" cells
     */
    public EWAHCompressedBitmap32 fringe(EWAHCompressedBitmap32 packed, int expansion, int[] bounds, Metric metric)
    {
        if(packed == null || packed.isEmpty())
        {
            return ALL_OFF;
        }
        int[][] movers = expandMetric(metric, expansion);

        IntSet checks = new IntOpenHashSet(packed.toArray());
        IntSortedSet ints = new IntRBTreeSet();
        IntIterator it = packed.intIterator();
        int[] pt = new int[bounds.length];
        while (it.hasNext()) {
            assignExpand(ints, checks, curve.alter(pt, it.next()), bounds, movers);
        }

        if(ints.isEmpty())
            return ALL_OFF;

        return EWAHCompressedBitmap32.bitmapOf(ints.toIntArray());
    }

    /**
     * Finds the concentric areas around the cells encoded in packed, without including those cells. Searches the area
     * around each "on" position in packed to cover a diamond in 2D, octahedron in 3D, or cross polytope in higher
     * dimensions, with the cells with a Manhattan distance of 1 included in the first element of the returned array of
     * packed bitmaps, the cells with a Manhattan distance of 2 in the second element of that array (and not cells at
     * any other distance), and so on up to a distance equal to expansion. If a cell in a packed bitmap would overlap
     * with the cells in packed or has already been included in an earlier packed bitmap in the returned array, it is
     * not added to a packed bitmap. If the expansion would take a cell further than 0 or out of the appropriate
     * dimension in bounds, that cell is technically stopped at the edge, but in all cases it will have been included in
     * an earlier fringe or in packed itself, so those cells won't be included anyway.
     * Returns an array of new packed bitmaps and does not modify packed.
     * @param packed a packed bitmap returned by pack() or a similar method
     * @param expansion the positive (diamond) radius, in cells, to push the furthest cells out by; clamped at 100
     * @param bounds the bounds of the positions to expand; bits will stop before they hit the bounds or go negative
     * @return an array of packed bitmaps, with length equal to expansion, where each bitmap encodes "on" for cells that
     * have a Manhattan distance to the nearest "on" cell in packed equal to the index in the array plus 1.
     */
    public EWAHCompressedBitmap32[] fringes(EWAHCompressedBitmap32 packed, int expansion, int[] bounds)
    {
        EWAHCompressedBitmap32[] values = new EWAHCompressedBitmap32[expansion];
        if(packed == null || packed.isEmpty())
        {
            Arrays.fill(values, ALL_OFF);
            return values;
        }
        IntSet checks = new IntOpenHashSet(packed.toArray());
        IntSortedSet ints;
        for (int i = 1; i <= expansion; i++) {
            int[][] movers = expandManhattan(i);
            ints = new IntRBTreeSet();
            IntIterator it = packed.intIterator();
            int[] pt = new int[bounds.length];
            while (it.hasNext()) {
                assignExpand(ints, checks, curve.alter(pt, it.next()), bounds, movers);
            }

            if(ints.isEmpty())
                values[i - 1] = ALL_OFF;
            else
                values[i - 1] = EWAHCompressedBitmap32.bitmapOf(ints.toIntArray());
        }

        return values;
    }

    /**
     * Finds the concentric areas around the cells encoded in packed, without including those cells. Searches the area
     * around each "on" position in packed to cover an n-dimensional region, one of square/cube/hypercube,
     * diamond/octahedron/cross polytope, or circle/sphere/hypersphere (for 2D/3D/higher dimensions) depending on the
     * metric parameter (CHEBYSHEV produces a square, MANHATTAN produces a diamond, while EUCLIDEAN and EUCLIDEAN_STRICT
     * produce circles in 2D). The distance measurement is based on metric. Cells with a distance of 1 are included in
     * the first element of the returned array of packed bitmaps, cells with a distance of 2 are included in the second
     * element of that array (and not cells at any other distance), and so on up to a distance equal to expansion. If a
     * cell in a packed bitmap would overlap with the cells in packed or has already been included in an earlier packed
     * bitmap in the returned array, it is not added to a packed bitmap. If the expansion would take a cell further than
     * 0 or out of the appropriate dimension in bounds, that cell is technically stopped at the edge, but in all cases
     * it will have been included in an earlier fringe or in packed itself, so those cells won't be included anyway.
     * Returns a new packed bitmap and does not modify packed.
     * @param packed a packed bitmap returned by pack() or a similar method
     * @param expansion the positive (square, diamond, or circular) radius, in cells, to push the furthest cell out by
     * @param bounds the bounds of the positions to expand; bits will stop before they hit the bounds or go negative
     * @param metric the distance metric to use; a Metric enum from this package
     * @return an array of packed bitmaps, with length equal to expansion, where each bitmap encodes "on" for cells that
     * have a distance to the nearest "on" cell in packed equal to the index in the array plus 1.
     */
    public EWAHCompressedBitmap32[] fringes(EWAHCompressedBitmap32 packed, int expansion, int[] bounds, Metric metric)
    {
        EWAHCompressedBitmap32[] values = new EWAHCompressedBitmap32[expansion];
        if(packed == null || packed.isEmpty())
        {
            Arrays.fill(values, ALL_OFF);
            return values;
        }
        IntSet checks = new IntOpenHashSet(packed.toArray());
        IntSortedSet ints;
        int[][] movers;
        for (int i = 1; i <= expansion; i++) {
            movers = expandMetric(metric, i);
            ints = new IntRBTreeSet();
            IntIterator it = packed.intIterator();
            int[] pt = new int[bounds.length];
            while (it.hasNext()) {
                assignExpand(ints, checks, curve.alter(pt, it.next()), bounds, movers);
            }

            if(ints.isEmpty())
                values[i - 1] = ALL_OFF;
            else
                values[i - 1] = EWAHCompressedBitmap32.bitmapOf(ints.toIntArray());
        }

        return values;
    }

    /**
     * Finds the area that has a cell encoded in packed on two opposing sides, without including the cells in packed.
     * Searches the area around each "on" position in packed to cover a diamond in 2D, octahedron in 3D, or cross
     * polytope in higher dimensions, with the cells that are not in packed but have Manhattan distance 1 from two "on"
     * positions along the same line included. Cells outside of bounds count as "on" for this purpose.
     * Returns a new packed bitmap and does not modify packed.
     * @param packed a packed bitmap returned by pack() or a similar method
     * @param bounds the bounds of the positions to expand; out-of-bounds cells count as "on" here
     * @return a packed bitmap that encodes "on" for cells that are the single-width "filling" between two cells along
     * one line in packed.
     */
    public EWAHCompressedBitmap32 filling(EWAHCompressedBitmap32 packed, int[] bounds)
    {
        if(packed == null || packed.isEmpty())
        {
            return ALL_OFF;
        }
        int[][] movers = expandManhattan(1);
        IntSet checks = new IntOpenHashSet(packed.toArray());
        IntSortedSet ints = new IntRBTreeSet();
        IntIterator it = packed.intIterator();
        int[] pt = new int[bounds.length];
        while (it.hasNext()) {
            assignOpposed(ints, checks, curve.alter(pt, it.next()), bounds, movers);
        }

        if(ints.isEmpty())
            return ALL_OFF;

        return EWAHCompressedBitmap32.bitmapOf(ints.toIntArray());
    }

    /**
     * Finds the area that has a span of cells exactly depth in length encoded in packed on two opposing sides, without
     * including the cells in packed. Searches the area around each "on" position in packed to cover a diamond in 2D,
     * octahedron in 3D, or cross polytope in higher dimensions, with the cells that are not in packed but are between
     * two "on" positions along the same orthogonal line included. Cells outside of bounds count as "on" for this
     * purpose. Returns a new packed bitmap and does not modify packed.
     * @param packed a packed bitmap returned by pack() or a similar method
     * @param bounds the bounds of the positions to expand; out-of-bounds cells count as "on" here
     * @param depth the distance that must be met exactly between "on" cells for this to count a cell as "filling"
     * @return a packed bitmap that encodes "on" for cells that are the single-width "filling" between two cells along
     * one line in packed.
     */
    public EWAHCompressedBitmap32 filling(EWAHCompressedBitmap32 packed, int[] bounds, int depth)
    {
        if(packed == null || packed.isEmpty())
        {
            return ALL_OFF;
        }
        int[][] movers = expandManhattan(1);
        IntSet checks = new IntOpenHashSet(packed.toArray()), group = new IntOpenHashSet(depth);
        IntSortedSet ints = new IntRBTreeSet();
        IntIterator it = packed.intIterator();
        int[] pt = new int[bounds.length];
        while (it.hasNext()) {
            assignOpposed(ints, checks, curve.alter(pt, it.next()), bounds, movers, group, depth);
        }

        if(ints.isEmpty())
            return ALL_OFF;

        return EWAHCompressedBitmap32.bitmapOf(ints.toIntArray());
    }

    /**
     * Finds the area that has a cell encoded in packed on two opposing sides, without including the cells in packed.
     * Searches the area around each "on" position in packed to cover an n-dimensional region based on metric, either
     * diamond/octahedron/cross polytope for MANHATTAN or square/cube/hypercube for any other (for 2D/3D/higher
     * dimensions). The cells that are not in packed but are between two "on" positions along the same orthogonal (for
     * Manhattan) or any (for other Metric values) line are all that is included in the result. Cells outside of bounds
     * count as "on" for this purpose.
     * Returns a new packed bitmap and does not modify packed.
     * @param packed a packed bitmap returned by pack() or a similar method
     * @param bounds the bounds of the positions to expand; out-of-bounds cells count as "on" here
     * @param metric the distance metric to use; a Metric enum from this package
     * @return a packed bitmap that encodes "on" for cells that were pushed from the edge of packed's "on" cells
     */
    public EWAHCompressedBitmap32 filling(EWAHCompressedBitmap32 packed, int[] bounds, Metric metric)
    {
        if(packed == null || packed.isEmpty())
        {
            return ALL_OFF;
        }
        int[][] movers = expandMetric(metric, 1);

        IntSet checks = new IntOpenHashSet(packed.toArray());
        IntSortedSet ints = new IntRBTreeSet();
        IntIterator it = packed.intIterator();
        int[] pt = new int[bounds.length];
        while (it.hasNext()) {
            assignOpposed(ints, checks, curve.alter(pt, it.next()), bounds, movers);
        }

        if(ints.isEmpty())
            return ALL_OFF;

        return EWAHCompressedBitmap32.bitmapOf(ints.toIntArray());
    }

    /**
     * Finds the area that has a span of cells exactly depth in length encoded in packed on two opposing sides, without
     * including the cells in packed. Searches the area around each "on" position in packed to cover an n-dimensional
     * region based on metric, either diamond/octahedron/cross polytope for MANHATTAN or square/cube/hypercube for any
     * other (for 2D/3D/higher dimensions). The cells that are not in packed but have a distance of 1 using the given metric from two "on"
     * positions along the same line are all that is included in the result. Cells outside of bounds count as "on" for
     * this purpose.
     * Returns a new packed bitmap and does not modify packed.
     * @param packed a packed bitmap returned by pack() or a similar method
     * @param bounds the bounds of the positions to expand; out-of-bounds cells count as "on" here
     * @param depth the distance that must be met exactly between "on" cells for this to count a cell as "filling"
     * @param metric the distance metric to use; a Metric enum from this package
     * @return a packed bitmap that encodes "on" for cells that were pushed from the edge of packed's "on" cells
     */
    public EWAHCompressedBitmap32 filling(EWAHCompressedBitmap32 packed, int[] bounds, int depth, Metric metric)
    {
        if(packed == null || packed.isEmpty())
        {
            return ALL_OFF;
        }
        int[][] movers = expandMetric(metric, 1);

        IntSet checks = new IntOpenHashSet(packed.toArray()), group = new IntOpenHashSet(depth);
        IntSortedSet ints = new IntRBTreeSet();
        IntIterator it = packed.intIterator();
        int[] pt = new int[bounds.length];
        while (it.hasNext()) {
            assignOpposed(ints, checks, curve.alter(pt, it.next()), bounds, movers, group, depth);
        }

        if(ints.isEmpty())
            return ALL_OFF;

        return EWAHCompressedBitmap32.bitmapOf(ints.toIntArray());
    }


    /**
     * Given the packed data start and container, where start encodes some area to expand out from and container encodes
     * the (typically irregularly shaped) region of viable positions that can be filled, and an amount to expand outward
     * by, expands each cell in start by a Manhattan (diamond) radius equal to expansion, limiting any expansion to
     * within container and returning the final expanded (limited) packed data. Because this goes in 1-distance steps of
     * expansion, and this won't expand into any areas not present in container, any gaps in container will take more
     * steps to move around than a normal expansion would moving through. This can be useful for a number of effects
     * where contiguous movement needs to be modeled.
     * Returns a new packed bitmap and does not modify start or container.
     * @param start a packed bitmap returned by pack() or a similar method that stores the start points of the flood
     * @param container a packed bitmap that represents all viable cells that this is allowed to flood into
     * @param expansion the positive (diamond) radius, in cells, to flood out by
     * @return a packed bitmap that does not extend beyond container and encodes the stepwise flood out from start by
     * a number of steps equal to expansion.
     */
    public EWAHCompressedBitmap32 flood(EWAHCompressedBitmap32 start, EWAHCompressedBitmap32 container,
                                        int expansion)
    {
        if(start == null || start.isEmpty() || container == null || container.isEmpty())
        {
            return ALL_OFF;
        }
        IntSet checks = new IntOpenHashSet(), edge = new IntOpenHashSet(), start2 = new IntOpenHashSet(start.toArray()),
                surround = new IntOpenHashSet(container.toArray());
        IntSortedSet ints = new IntRBTreeSet();
        int[][] movers = expandManhattan(1);

        for (int i = 1; i <= expansion; i++) {
            IntegerIterator it = start2.iterator();
            int[] pt = new int[curve.dimensionality.length];
            while (it.hasNext()) {
                assignFlood(ints, checks, edge, surround, curve.alter(pt, it.nextInt()), movers);
            }

            if(edge.isEmpty())
                break;
            else {
                start2.clear();
                start2.addAll(edge);
            }
        }

        return EWAHCompressedBitmap32.bitmapOf(ints.toIntArray());
    }

    /**
     * Given the packed data start and container, where start encodes some area to expand out from and container encodes
     * the (typically irregularly shaped) region of viable positions that can be filled, an amount to expand outward by,
     * and a distance metric (Chebyshev, Manhattan, or Euclidean distance, in a Metric enum), expands each cell in start
     * by a radius using the specified distance metric equal to expansion, limiting any expansion to within container
     * and returning the final expanded (limited) packed data. Because this goes in 1-distance steps of expansion, and
     * this won't expand into any areas not present in container, any gaps in container will take more steps to move
     * around than a normal expansion would moving through. This can be useful for a number of effects where contiguous
     * movement needs to be modeled. Also, the EUCLIDEAN, EUCLIDEAN_STRICT, and CHEBYSHEV Metric values will all be
     * equivalent here because of the 1-step rule.
     * Returns a new packed bitmap and does not modify start or container.
     * @param start a packed bitmap returned by pack() or a similar method that stores the start points of the flood
     * @param container a packed bitmap that represents all viable cells that this is allowed to flood into
     * @param expansion the positive (square, diamond or circular) radius, in cells, to flood out by
     * @param metric the distance metric to use; a Metric enum from this package
     * @return a packed bitmap that does not extend beyond container and encodes the stepwise flood out from start by
     * a number of steps equal to expansion.
     */
    public EWAHCompressedBitmap32 flood(EWAHCompressedBitmap32 start, EWAHCompressedBitmap32 container,
                                        int expansion, Metric metric)
    {
        if(start == null || start.isEmpty() || container == null || container.isEmpty())
        {
            return ALL_OFF;
        }
        IntSet checks = new IntOpenHashSet(), edge = new IntOpenHashSet(), start2 = new IntOpenHashSet(start.toArray()),
                surround = new IntOpenHashSet(container.toArray());
        IntSortedSet ints = new IntRBTreeSet();
        int[][] movers = expandMetric(metric, 1);

        for (int i = 1; i <= expansion; i++) {
            IntegerIterator it = start2.iterator();
            int[] pt = new int[curve.dimensionality.length];
            while (it.hasNext()) {
                assignFlood(ints, checks, edge, surround, curve.alter(pt, it.nextInt()), movers);
            }

            if(edge.isEmpty())
                break;
            else {
                start2.clear();
                start2.addAll(edge);
            }
        }

        return EWAHCompressedBitmap32.bitmapOf(ints.toIntArray());
    }


    /**
     * Given the packed data start and container, where start encodes some area to expand out from and container encodes
     * the (typically irregularly shaped) region of viable positions that can be filled, a volume to try to reach by
     * expanding start in random orthogonal directions and an rng to determine random numbers, expands a random cell in
     * start in a random orthogonal direction, repeating until volume is reached or container is filled, and returning
     * the final expanded (limited) packed data. Any expansion is limited to within container. Because this goes in
     * 1-distance random steps of expansion, and this won't expand into any areas not present in container, any gaps in
     * container are likely to take longer to move around than a normal expansion would moving through. This can be
     * useful for a number of effects where contiguous random movement needs to be modeled, such as basic gas-filling.
     * Returns a new packed bitmap and does not modify start or container.
     * @param start a packed bitmap returned by pack() or a similar method that stores the start points of the flood
     * @param container a packed bitmap that represents all viable cells that this is allowed to flood into
     * @param volume the total number of positions this should try to flood out to; should be greater than count(start)
     * @return a packed bitmap that does not extend beyond container and encodes the stepwise flood out from start by
     * a number of steps equal to expansion.
     */
    public EWAHCompressedBitmap32 randomFlood(EWAHCompressedBitmap32 start, EWAHCompressedBitmap32 container,
                                        int volume, RNG rng)
    {
        if(start == null || start.isEmpty() || container == null || container.isEmpty())
        {
            return ALL_OFF;
        }
        IntSet checks = new IntOpenHashSet(), edge = new IntOpenHashSet(),
                surround = new IntOpenHashSet(container.toArray());
        IntSortedSet ints = new IntRBTreeSet(start.toArray());
        int[][] movers = expandManhattan(1);
        int[] pt = new int[curve.dimensionality.length];

        IntIterator it = start.intIterator();
        while(it.hasNext())
        {
            computeEdge(ints, edge, surround, curve.alter(pt, it.next()), movers);
        }
        int s = ints.size(), danger = 0;
        while (s < volume && danger++ < volume * 20) {
            s += assignRandomFlood(ints, checks, edge, surround, movers, rng);

            if(edge.isEmpty())
                break;
        }

        return EWAHCompressedBitmap32.bitmapOf(ints.toIntArray());
    }
    /**
     * Finds the area made by removing the "on" positions in packed that are within the specified retraction distance of
     * an "off" position or the edge of the data, as reported by bounds. This essentially finds a shrunken version of
     * packed. Uses Manhattan distance.
     * Returns a new packed bitmap and does not modify packed.
     * @param packed a packed bitmap returned by pack() or a similar method
     * @param retraction the positive (diamond) radius, in cells, to pull each cell in by
     * @param bounds the bounds of the positions in packed
     * @return a packed bitmap that encodes "on" for cells that were "on" in packed and were far from an "off" cell
     */
    public EWAHCompressedBitmap32 retract(EWAHCompressedBitmap32 packed, int retraction, int[] bounds)
    {
        return difference(packed, expand(negate(packed), retraction, bounds));
    }
    /**
     * Finds the area made by removing the "on" positions in packed that are within the specified retraction distance of
     * an "off" position or the edge of the data, as reported by bounds. This essentially finds a shrunken version of
     * packed. Uses metric to measure distance.
     * Returns a new packed bitmap and does not modify packed.
     * @param packed a packed bitmap returned by pack() or a similar method
     * @param retraction the positive (square, diamond, or circular) radius, in cells, to pull each cell in by
     * @param bounds the bounds of the positions in packed
     * @param metric the distance metric to use; a Metric enum from this package
     * @return a packed bitmap that encodes "on" for cells that were "on" in packed and were far from an "off" cell
     */
    public EWAHCompressedBitmap32 retract(EWAHCompressedBitmap32 packed, int retraction, int[] bounds, Metric metric)
    {
        return difference(packed, expand(negate(packed), retraction, bounds, metric));
    }

    /**
     * Finds the area consisting of the "on" positions in packed that are within the specified depth distance of an
     * "off" position or the edge of the data, as reported by bounds. This essentially finds the part of packed that is
     * close to its edge. Uses Manhattan distance.
     * Returns a new packed bitmap and does not modify packed.
     * @param packed a packed bitmap returned by pack() or a similar method
     * @param depth the positive (diamond) radius, in cells, to go inward from an "off" cell into the "on" cells
     * @param bounds the bounds of the positions in packed
     * @return a packed bitmap that encodes "on" for cells that were "on" in packed and were close to an "off" cell
     */
    public EWAHCompressedBitmap32 surface(EWAHCompressedBitmap32 packed, int depth, int[] bounds)
    {
        return intersect(packed, expand(negate(packed), depth, bounds));
    }
    /**
     * Finds the area consisting of the "on" positions in packed that are within the specified depth distance of an
     * "off" position or the edge of the data, as reported by bounds. This essentially finds the part of packed that is
     * close to its edge. Uses metric to measure distance.
     * Returns a new packed bitmap and does not modify packed.
     * @param packed a packed bitmap returned by pack() or a similar method
     * @param depth the positive (square, diamond, or circular) radius, in cells, to go inward from an "off" cell into
     *              the "on" cells
     * @param bounds the bounds of the positions in packed
     * @param metric the distance metric to use; a Metric enum from this package
     * @return a packed bitmap that encodes "on" for cells that were "on" in packed and were close to an "off" cell
     */
    public EWAHCompressedBitmap32 surface(EWAHCompressedBitmap32 packed, int depth, int[] bounds, Metric metric)
    {
        return intersect(packed, expand(negate(packed), depth, bounds, metric));
    }


    /**
     * Given a packed bitmap that encodes multiple unconnected "on" areas, this finds each isolated area and returns
     * it as an element in a List of packed bitmaps, with one bitmap per isolated area.
     *
     * @param packed a packed bitmap that probably encodes multiple unconnected "on" areas
     * @return an ArrayList of packed bitmaps containing each unconnected area from packed as a bitmap element
     */
    public List<EWAHCompressedBitmap32> split(EWAHCompressedBitmap32 packed)
    {
        ArrayList<EWAHCompressedBitmap32> parts = new ArrayList<EWAHCompressedBitmap32>(32);

        EWAHCompressedBitmap32 remaining = copy(packed), area;

        while (!remaining.isEmpty()) {
            area = flood(packOneCurve(remaining.getFirstSetBit()), packed, curve.maxDistance);
            parts.add(area);
            remaining = difference(remaining, area);
        }
        return parts;
    }

    /**
     * Given an array of bounds that should have the same length as the dimensionality of the space-filling curve this
     * uses, returns a packed array that encodes "on" for the rectangle/rectangular prism/stretched hypercube from the
     * origin to the point that has coordinates each 1 less than the limit in bounds. Primarily useful with intersect()
     * to ensure things like negate() that can encode "on" cells in any position are instead limited to the desired
     * bounding area.
     * @param bounds the bounding dimensions of the (n-dimensional) rectangle, implicitly using the origin for the
     *               opposite corner
     * @return packed data encoding "on" for all cells with coordinates less than their counterpart in bounds.
     */
    public EWAHCompressedBitmap32 rectangle(int[] bounds)
    {
        int b = validateBounds(bounds);
        boolean[] rect = new boolean[b];
        Arrays.fill(rect, true);

        return pack(rect, bounds);
    }

    /**
     * Given arrays of start positions and bounds that should each have the same length as the dimensionality of the
     * space-filling curve this uses, returns a packed array that encodes "on" for the
     * rectangle/rectangular prism/stretched hypercube from start to the point that has coordinates each 1 less than the
     * limit in bounds. Useful with intersect() to ensure things like negate() that can encode "on" cells in any
     * position are instead limited to the desired bounding area, but also for general "box drawing."
     * @param start the least corner of the (n-dimensional) rectangle, inclusive
     * @param bounds the greatest corner of the (n-dimensional) rectangle, exclusive
     * @return packed data encoding "on" for all cells with coordinates between their counterparts in start and bounds.
     */
    public EWAHCompressedBitmap32 rectangle(int[] start, int[] bounds)
    {
        int s = validateBounds(start), b = validateBounds(bounds), run;
        if(s > b)
            throw new UnsupportedOperationException("Starting corner of (hyper-)rectangle must not be beyond bounds");
        boolean[] rect = new boolean[b];
        int[] pt = new int[bounds.length], sides = new int[bounds.length];
        for (int i = 0; i < bounds.length; i++) {
            int l = bounds[i] - start[i];
            if(l < 1)
                return ALL_OFF;
            sides[i] = l;
        }

        for (int i = 0; i < b; i++) {
            run = 1;
            for (int d = pt.length - 1; d >= 0; d--) {
                pt[d] = ((i / run) % sides[d]) + start[d];
                run *= sides[d];
            }
            run = boundedIndex(bounds, pt);
            if(run >= 0)
                rect[run] = true;
        }

        return pack(rect, bounds);
    }

    /**
     * Counts the number of "on" cells encoded in a packed array without unpacking it. Equivalent to calling
     * {@code packed.cardinality()}.
     * @param packed a packed bitmap, as produced by pack()
     * @return the number of "on" cells.
     */
    public int count(EWAHCompressedBitmap32 packed)
    {
        return packed.cardinality();
    }

    /**
     * Gets a new packed bitmap that encodes all cells that are "on" in either left or right; the logical "or"
     * operation. Equivalent to calling {@code left.or(right)}.
     * @param left a bitmap to get the union of
     * @param right a bitmap to get the union of
     * @return the union of the two bitmaps
     */
    public EWAHCompressedBitmap32 union(EWAHCompressedBitmap32 left, EWAHCompressedBitmap32 right)
    {
        return left.or(right);
    }

    /**
     * Gets a new packed bitmap that encodes all cells that are "on" in any of the bitmaps passed to it as an array or
     * vararg; the logical "or" operation. Equivalent to calling {@code EWAHCompressedBitmap32.or(bitmaps)}.
     * @param bitmaps an array or vararg of bitmaps to get the union of
     * @return the union of all of the bitmaps
     */
    public EWAHCompressedBitmap32 unionMany(EWAHCompressedBitmap32... bitmaps)
    {
        return EWAHCompressedBitmap32.or(bitmaps);
    }

    /**
     * Gets a new packed bitmap that encodes all cells that are "on" in both left and right; the logical "and"
     * operation. Equivalent to calling {@code left.and(right)}.
     * @param left a bitmap to get the intersection of
     * @param right a bitmap to get the intersection of
     * @return the intersection of the two bitmaps
     */
    public EWAHCompressedBitmap32 intersect(EWAHCompressedBitmap32 left, EWAHCompressedBitmap32 right)
    {
        return left.and(right);
    }

    /**
     * Gets a new packed bitmap that encodes all cells that are "on" in all of the bitmaps passed to it as an array or
     * vararg; the logical "and" operation. Equivalent to calling {@code EWAHCompressedBitmap32.and(bitmaps)}.
     * @param bitmaps an array or vararg of bitmaps to get the intersection of
     * @return the intersection of all of the bitmaps
     */
    public EWAHCompressedBitmap32 intersectMany(EWAHCompressedBitmap32... bitmaps)
    {
        return EWAHCompressedBitmap32.and(bitmaps);
    }

    /**
     * Gets a new packed bitmap that encodes all cells that are "on" in exactly one of left or right; the logical "xor"
     * operation. Equivalent to calling {@code left.xor(right)}.
     * @param left a bitmap to get the exclusive disjunction (xor) of
     * @param right a bitmap to get the exclusive disjunction (xor) of
     * @return the exclusive disjunction (xor) of the two bitmaps
     */
    public EWAHCompressedBitmap32 xor(EWAHCompressedBitmap32 left, EWAHCompressedBitmap32 right)
    {
        return left.xor(right);
    }

    /**
     * Gets a new packed bitmap that encodes all cells that are "on" in an odd number of the bitmaps passed to it as an
     * array or vararg; the logical "xor" operation. Equivalent to calling {@code EWAHCompressedBitmap32.xor(bitmaps)}.
     * @param bitmaps an array or vararg of bitmaps to get the exclusive disjunction (xor) of
     * @return the exclusive disjunction (xor) of all of the bitmaps
     */
    public EWAHCompressedBitmap32 xorMany(EWAHCompressedBitmap32... bitmaps)
    {
        return EWAHCompressedBitmap32.xor(bitmaps);
    }

    /**
     * Gets a new packed bitmap that encodes all cells that are "on" in left and "off" in right; the logical "nand"
     * operation. Equivalent to calling {@code left.andNot(right)}.
     * @param left a bitmap to subtract bits from
     * @param right a bitmap that will be subtracted
     * @return the difference of the two bitmaps
     */
    public EWAHCompressedBitmap32 difference(EWAHCompressedBitmap32 left, EWAHCompressedBitmap32 right)
    {
        return left.andNot(right);
    }


    /**
     * Copies packed without extra boilerplate, returning a new packed bitmap containing the same data.
     * @param packed a packed bitmap to copy
     * @return the copied bitmap
     */
    public EWAHCompressedBitmap32 copy(EWAHCompressedBitmap32 packed)
    {
        EWAHCompressedBitmap32 n;
        try {
            n = packed.clone();
        } catch (CloneNotSupportedException e) {
            return packed;
        }

        return n;
    }

    /**
     * Gets a negated copy of the packed bitmap, turning all "on" bits to "off" and all "off" bits to "on." Not
     * equivalent to {@code packed.not()}, which would mutate packed in place; instead equivalent to the example given
     * in the documentation for not(), which copies packed first.
     * @param packed the packed bitmap to find the negation of
     * @return a negated copy of packed
     */
    public EWAHCompressedBitmap32 negate(EWAHCompressedBitmap32 packed)
    {
        EWAHCompressedBitmap32 n = copy(packed);
        n.not();
        return n;
    }

    /**
     * Given an array or vararg of coordinates that must have length equal to the number of dimensions in this
     * RegionPacker's CurveStrategy, returns a new packed bitmap that encodes only that point.
     * @param coordinates an array or vararg of coordinates that defines a point; must match the curve's dimension count
     * @return a new packed bitmap encoding only the point with the given coordinates
     */
    public EWAHCompressedBitmap32 packOne(int... coordinates)
    {
        EWAHCompressedBitmap32 bmp = new EWAHCompressedBitmap32();
        bmp.set(curve.distance(coordinates));
        return bmp;
    }

    /**
     * Given an int distance to travel down the space-filling curve given as this RegionPacker's CurveStrategy, returns
     * a new packed bitmap that encodes only the point at that distance.
     * @param distance a space-filling curve index using this RegionPacker's CurveStrategy.
     * @return a new packed bitmap encoding only the point with the given distance along this space-filling curve
     */
    public EWAHCompressedBitmap32 packOneCurve(int distance)
    {
        EWAHCompressedBitmap32 bmp = new EWAHCompressedBitmap32();
        bmp.set(distance);
        return bmp;
    }

    /**
     * Given an array or vararg of int arrays for points, where each internal array must have length equal to the number
     * of dimensions in this RegionPacker's CurveStrategy, returns a new packed bitmap that encodes only those points.
     * @param points an array or vararg of int arrays that each defines a point; there can be any number of arrays but
     *               each inner array must match the curve's dimension count
     * @return a new packed bitmap encoding only the given points
     */
    public EWAHCompressedBitmap32 packSeveral(int[]... points)
    {
        int[] distances = new int[points.length];
        for (int i = 0; i < points.length; i++) {
            distances[i] = curve.distance(points[i]);
        }
        Arrays.sort(distances);
        return EWAHCompressedBitmap32.bitmapOf(distances);
    }

    /**
     * Given an array or vararg of space-filling curve indices, returns a new packed bitmap that encodes only the points
     * corresponding to the distances traveled for each index.
     * @param distances an array or vararg of space-filling curve indices
     * @return a new packed bitmap encoding only the points with the given distances along this space-filling curve
     */
    public EWAHCompressedBitmap32 packSeveralCurve(int... distances)
    {
        int[] distances2 = new int[distances.length];
        System.arraycopy(distances, 0, distances2, 0, distances.length);
        Arrays.sort(distances2);
        return EWAHCompressedBitmap32.bitmapOf(distances2);
    }

    /**
     * Given a packed bitmap and an array or vararg of coordinates that must have length equal to the number of
     * dimensions in this RegionPacker's CurveStrategy, returns a new packed bitmap that encodes the given point as "on"
     * in addition to any contents of packed.
     * Copies packed and does not modify the original; to add to packed bitmaps in place, you can use the set() method.
     * @param packed a packed bitmap to copy and add another point to
     * @param coordinates an array or vararg of coordinates that defines a point; must match the curve's dimension count
     * @return a new packed bitmap encoding a copy of packed's data and also the point with the given coordinates
     */
    public EWAHCompressedBitmap32 insertOne(EWAHCompressedBitmap32 packed, int... coordinates)
    {
        EWAHCompressedBitmap32 next = copy(packed);
        next.set(curve.distance(coordinates));
        return next;
    }

    /**
     * Given a packed bitmap and an int distance to travel down the space-filling curve given as this RegionPacker's
     * CurveStrategy, returns a new packed bitmap that encodes the point at that distance as "on" in addition to any
     * contents of packed.
     * Copies packed and does not modify the original; to add to packed bitmaps in place, you can use the set() method.
     * @param packed a packed bitmap to copy and add another point to
     * @param distance a space-filling curve index using this RegionPacker's CurveStrategy.
     * @return a new packed bitmap encoding a copy of packed's data and also the point with the given distance along
     * this space-filling curve
     */
    public EWAHCompressedBitmap32 insertOneCurve(EWAHCompressedBitmap32 packed, int distance)
    {
        EWAHCompressedBitmap32 next = copy(packed);
        next.set(distance);
        return next;
    }

    /**
     * Given a packed bitmap and an array or vararg of int arrays for points, where each internal array must have length
     * equal to the number of dimensions in this RegionPacker's CurveStrategy, returns a new packed bitmap that encodes
     * those points in addition to any contents of packed.
     * Copies packed and does not modify the original; to add to packed bitmaps in place, you can use the set() method.
     * @param packed a packed bitmap to copy and add the given points to
     * @param points an array or vararg of int arrays that each defines a point; there can be any number of arrays but
     *               each inner array must match the curve's dimension count
     * @return a new packed bitmap encoding a copy of packed's data and also the given points
     */
    public EWAHCompressedBitmap32 insertSeveral(EWAHCompressedBitmap32 packed, int[]... points)
    {
        return packed.or(packSeveral(points));
    }

    /**
     * Given a packed bitmap and an array or vararg of space-filling curve indices, returns a new packed bitmap that
     * encodes the points corresponding to the distances traveled for each index in addition to any contents of packed.
     * Copies packed and does not modify the original; to add to packed bitmaps in place, you can use the set() method.
     * @param packed a packed bitmap to copy and add points to
     * @param distances an array or vararg of space-filling curve indices
     * @return a new packed bitmap encoding a copy of packed's data and also the points with the given distances along
     * this space-filling curve
     */
    public EWAHCompressedBitmap32 insertSeveralCurve(EWAHCompressedBitmap32 packed, int... distances)
    {
        return packed.or(packSeveralCurve(distances));
    }

    /**
     * Given a packed bitmap and an array or vararg of coordinates that must have length equal to the number of
     * dimensions in this RegionPacker's CurveStrategy, returns a new packed bitmap that ensures the given point is
     * "off" after including any contents of packed.
     * Copies packed and does not modify the original; to remove from packed bitmaps destructively, you can use the
     * clear() method.
     * @param packed a packed bitmap to copy and remove a point from
     * @param coordinates an array or vararg of coordinates that defines a point; must match the curve's dimension count
     * @return a new packed bitmap encoding a copy of packed's data but not the point with the given coordinates
     */
    public EWAHCompressedBitmap32 removeOne(EWAHCompressedBitmap32 packed, int... coordinates)
    {
        EWAHCompressedBitmap32 next = copy(packed);
        next.clear(curve.distance(coordinates));
        return next;
    }

    /**
     * Given a packed bitmap and an int distance to travel down the space-filling curve given as this RegionPacker's
     * CurveStrategy, returns a new packed bitmap that ensures the point at that distance is "off" after including any
     * contents of packed.
     * Copies packed and does not modify the original; to remove from packed bitmaps destructively, you can use the
     * clear() method.
     * @param packed a packed bitmap to copy and remove a point from
     * @param distance a space-filling curve index using this RegionPacker's CurveStrategy.
     * @return a new packed bitmap encoding a copy of packed's data but not the point with the given distance along
     * this space-filling curve
     */
    public EWAHCompressedBitmap32 removeOneCurve(EWAHCompressedBitmap32 packed, int distance)
    {
        EWAHCompressedBitmap32 next = copy(packed);
        next.clear(distance);
        return next;
    }

    /**
     * Given a packed bitmap and an array or vararg of int arrays for points, where each internal array must have length
     * equal to the number of dimensions in this RegionPacker's CurveStrategy, returns a new packed bitmap that ensures
     * those points are "off" after including any contents of packed.
     * Copies packed and does not modify the original; to remove from packed bitmaps destructively, you can use the
     * clear() method.
     * @param packed a packed bitmap to copy and remove the given points from
     * @param points an array or vararg of int arrays that each defines a point; there can be any number of arrays but
     *               each inner array must match the curve's dimension count
     * @return a new packed bitmap encoding a copy of packed's data but not the given points
     */
    public EWAHCompressedBitmap32 removeSeveral(EWAHCompressedBitmap32 packed, int[]... points)
    {
        return packed.andNot(packSeveral(points));
    }

    /**
     * Given a packed bitmap and an array or vararg of space-filling curve indices, returns a new packed bitmap that
     * ensures the points corresponding to the distances traveled for each index are "off" after including any contents
     * of packed.
     * Copies packed and does not modify the original; to remove from packed bitmaps destructively, you can use the
     * clear() method.
     * @param packed a packed bitmap to copy and remove points from
     * @param distances an array or vararg of space-filling curve indices
     * @return a new packed bitmap encoding a copy of packed's data but not the points with the given distances along
     * this space-filling curve
     */
    public EWAHCompressedBitmap32 removeSeveralCurve(EWAHCompressedBitmap32 packed, int... distances)
    {
        return packed.andNot(packSeveralCurve(distances));
    }

    /**
     * Randomly selects positions that are "on" in the given packed bitmap , with a position being chosen if a random
     * double generated per-position is less than chance, and returns a new packed bitmap that encodes those random
     * positions. Random numbers are generated by the rng parameter.
     * @param packed a packed bitmap returned by pack() or a related method
     * @param chance between 0.0 and 1.0, with higher numbers resulting in more of packed being used
     * @param rng the random number generator used to decide random factors
     * @return a new packed bitmap that encodes positions randomly selected from packed, or a copy of packed if chance
     * is 1.0 or greater
     */
    public EWAHCompressedBitmap32 randomSample(EWAHCompressedBitmap32 packed, double chance, RNG rng) {
        if(chance >= 1.0)
            return copy(packed);
        int[] using = rng.randomSamples(0, packed.cardinality(), chance);
        return packed.compose(EWAHCompressedBitmap32.bitmapOf(using));
    }
    /**
     * Gets a fixed number of randomly chosen positions that are "on" in the given packed bitmap, and returns a new
     * packed bitmap that encodes those random positions. Random numbers are generated by the rng parameter.
     * @param packed a packed bitmap returned by pack() or a related method
     * @param size the desired number of points (as int arrays) to return; may be smaller if there aren't enough "on"
     * @param rng the random number generator used to decide random factors
     * @return a new packed bitmap that encodes a number of positions equal to size, randomly selected from packed (or
     * this returns a copy of packed if size is not less than the number of "on" positions in packed)
     */
    public EWAHCompressedBitmap32 randomPortion(EWAHCompressedBitmap32 packed, int size, RNG rng) {
        int counted = packed.cardinality(), len = Math.min(counted, size);
        if(len == counted)
            return copy(packed);
        int[] using = rng.randomRange(0, counted, len);
        Arrays.sort(using);
        return packed.compose(EWAHCompressedBitmap32.bitmapOf(using));
    }

    /**
     * Given a packed bitmap and an RNG, gets a point corresponding to a random "on" position in packed. Returns an int
     * array representing the point, or null if packed is null or has no "on" positions.
     * @param packed a packed bitmap
     * @param rng the random number generator used to decide random factors
     * @return a random point corresponding to an "on" position in packed, or null if positions is null or empty
     */
    public int[] singleRandom(EWAHCompressedBitmap32 packed, RNG rng)
    {
        if(packed == null || packed.isEmpty())
            return null;
        int n = rng.nextInt(packed.cardinality());
        EWAHCompressedBitmap32 comp = packOneCurve(n);
        int found = packed.compose(comp).getFirstSetBit();
        if(found >= 0)
            return curve.point(found);
        else
            return null;
    }

    /**
     * If you expect to get many random values one at a time from a packed bitmap, you can get all positions that are
     * "on" with this class' positionsCurve method or the packed bitmap's toArray method. In either case, you can get
     * a random point that packed bitmap encoded by passing that resulting int array and an RNG to this method. Returns
     * an int array representing the point, or null if positions is null or empty.
     * @param positions the positions in a packed bitmap as obtained by this.positionsCurve() or packed.toArray()
     * @param rng the random number generator used to decide random factors
     * @return a random point corresponding to an "on" position in the original packed bitmap, or null if positions is
     * null or empty
     */
    public int[] singleRandom(int[] positions, RNG rng)
    {
        if(positions == null || positions.length == 0)
            return null;
        int n = rng.nextInt(positions.length);
        return curve.point(positions[n]);
    }
}