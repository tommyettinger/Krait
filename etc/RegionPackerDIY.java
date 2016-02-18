package com.github.tommyettinger;

import com.gs.collections.api.block.procedure.primitive.IntIntProcedure;
import com.gs.collections.api.iterator.IntIterator;
import com.gs.collections.impl.list.mutable.primitive.IntArrayList;

import java.util.ArrayList;
import java.util.Arrays;


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
public class RegionPackerDIY {

    public static final Region ALL_WALL = new Region(0), ALL_ON = new Region(0, -1);
    public CurveStrategy curve;
    public RegionPackerDIY()
    {
        this(new Hilbert2DStrategy(256));
    }
    public RegionPackerDIY(CurveStrategy curveStrategy)
    {
        curve = curveStrategy;
    }

    /**
     * Given a point as an array or vararg of long coordinates and the bounds as an array of long dimension lengths,
     * computes an index into a 1D array that matches bounds. The value this returns will be between 0 (inclusive) and
     * the product of each element in bounds (exclusive), unless the given point does not fit in bounds. If point is
     * out-of-bounds, this always returns -1, and callers should check for -1 as an output.
     * @param bounds the bounding dimension lengths as a long array
     * @param point the coordinates of the point to encode as a long array or vararg; must have a length at least equal
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

    public static int[] fromBounded(int[] bounds, int index)
    {
        int[] point = new int[bounds.length];
        int u = 1;
        for (int a = bounds.length - 1; a >= 0; a--) {
            point[a] = (index / u) % bounds[a];
            u *= bounds[a];
        }
        return point;
    }

    /**
     * Compresses a double[][] that only stores two
     * relevant states (one of which should be 0 or less, the other greater than 0), returning a short[] as described in
     * the {@link RegionPackerDIY} class documentation. This short[] can be passed to RegionPacker.unpack() to restore the
     * relevant states and their positions as a boolean[][] (with false meaning 0 or less and true being any double
     * greater than 0). As stated in the class documentation, the compressed result is intended to use as little memory
     * as possible for 2D arrays with contiguous areas of "on" cells.
     *<br>
     * <b>To store more than two states</b>, you should use packMulti().
     *
     * @param map a double[][] that probably was returned by FOV. If you obtained a double[][] from DijkstraMap, it
     *            will not meaningfully compress with this method.
     * @return a packed short[] that should, in most circumstances, be passed to unpack() when it needs to be used.
     */
    public Region pack(double[][] map)
    {
        if(map == null || map.length == 0)
            throw new ArrayIndexOutOfBoundsException("RegionPacker.pack() must be given a non-empty array");
        int xSize = map.length, ySize = map[0].length;
        if(xSize > curve.dimensionality[0] || ySize > curve.dimensionality[1])
            throw new UnsupportedOperationException("Array size is too large for given CurveStrategy, aborting");
        Region packing = new Region();
        boolean on = false, anyAdded = false, current;
        int skip = 0, limit = curve.maxDistance, mapLimit = xSize * ySize;
        int[] pt;
        for(int i = 0, ml = 0; i < limit && ml < mapLimit; i++, skip++)
        {
            pt = curve.point(i);
            if(pt[0] >= xSize || pt[1] >= ySize) {
                if(on) {
                    on = false;
                    packing.add(skip);
                    skip = 0;
                    anyAdded = true;
                }
                continue;
            }
            ml++;
            current = map[pt[0]][pt[1]] > 0.0;
            if(current != on)
            {
                packing.add(skip);
                skip = 0;
                on = current;
                anyAdded = true;
            }
        }
        if(on)
            packing.add(skip);
        else if(!anyAdded)
            return ALL_WALL;
        return packing;
    }


    /**
     * Compresses a boolean[][], returning a short[] as described in the {@link RegionPackerDIY} class documentation. This
     * short[] can be passed to RegionPacker.unpack() to restore the relevant states and their positions as a boolean[][]
     * As stated in the class documentation, the compressed result is intended to use as little memory as possible for
     * 2D arrays with contiguous areas of "on" cells.
     *
     * @param map a boolean[][] that should ideally be mostly false.
     * @return a packed short[] that should, in most circumstances, be passed to unpack() when it needs to be used.
     */
    public Region pack(boolean[][] map)
    {
        if(map == null || map.length == 0)
            throw new ArrayIndexOutOfBoundsException("RegionPacker.pack() must be given a non-empty array");
        int xSize = map.length, ySize = map[0].length;
        if(xSize > curve.dimensionality[0] || ySize > curve.dimensionality[1])
            throw new UnsupportedOperationException("Array size is too large for given CurveStrategy, aborting");
        Region packing = new Region();
        boolean on = false, anyAdded = false, current;
        int skip = 0, limit = curve.maxDistance, mapLimit = xSize * ySize;
        int[] pt;

        for(int i = 0, ml = 0; i < limit && ml < mapLimit; i++, skip++)
        {
            pt = curve.point(i);
            if(pt[0] >= xSize || pt[1] >= ySize) {
                if(on) {
                    on = false;
                    packing.add(skip);
                    skip = 0;
                    anyAdded = true;
                }
                continue;
            }
            ml++;
            current = map[pt[0]][pt[1]];
            if(current != on)
            {
                packing.add(skip);
                skip = 0;
                on = current;
                anyAdded = true;
            }
        }
        if(on)
            packing.add(skip);
        else if(!anyAdded)
            return ALL_WALL;
        return packing;
    }

    /**
     * Compresses a char[][] (typically one generated by a map generating method) so only the cells that equal the yes
     * parameter will be encoded as "on", returning a short[] as described in
     * the {@link RegionPackerDIY} class documentation. This short[] can be passed to RegionPacker.unpack() to restore the
     * positions of chars that equal the parameter yes as a boolean[][] (with false meaning not equal and true equal to
     * yes). As stated in the class documentation, the compressed result is intended to use as little memory
     * as possible for 2D arrays with contiguous areas of "on" cells.
     *
     * @param map a char[][] that may contain some area of cells that you want stored as packed data
     * @param yes the char to encode as "on" in the result; all others are encoded as "off"
     * @return a Region that can be passed to other methods in this class
     */
    public Region pack(char[][] map, char yes)
    {
        if(map == null || map.length == 0)
            throw new ArrayIndexOutOfBoundsException("RegionPacker.pack() must be given a non-empty array");
        if(curve.dimensionality.length != 2)
            throw new UnsupportedOperationException("2D array methods can only be used when the CurveStrategy is 2D");
        int xSize = map.length, ySize = map[0].length;
        if(xSize > curve.dimensionality[0] || ySize > curve.dimensionality[1])
            throw new UnsupportedOperationException("Array size is too large for given CurveStrategy, aborting");
        Region packing = new Region(64);
        boolean on = false, anyAdded = false, current;
        int skip = 0, limit = curve.maxDistance, mapLimit = xSize * ySize;
        int[] pt;

        for(int i = 0, ml = 0; i < limit && ml < mapLimit; i++, skip++)
        {
            pt = curve.point(i);
            if(pt[0] >= xSize || pt[1] >= ySize) {
                if(on) {
                    on = false;
                    packing.add((short) skip);
                    skip = 0;
                    anyAdded = true;
                }
                continue;
            }
            ml++;
            current = map[pt[0]][pt[1]] == yes;
            if(current != on)
            {
                packing.add((short) skip);
                skip = 0;
                on = current;
                anyAdded = true;
            }
        }
        if(on)
            packing.add((short)skip);
        else if(!anyAdded)
            return ALL_WALL;
        return packing;
    }

    /**
     * Decompresses a short[] returned by pack() or a sub-array of a short[][] returned by packMulti(), as described in
     * the {@link RegionPackerDIY} class documentation. This returns a boolean[][] that stores the same values that were
     * packed if the overload of pack() taking a boolean[][] was used. If a double[][] was compressed with pack(), the
     * boolean[][] this returns will have true for all values greater than 0 and false for all others. If this is one
     * of the sub-arrays compressed by packMulti(), the index of the sub-array will correspond to an index in the levels
     * array passed to packMulti(), and any cells that were at least equal to the corresponding value in levels will be
     * true, while all others will be false. Width and height do not technically need to match the dimensions of the
     * original 2D array, but under most circumstances where they don't match, the data produced will be junk.
     * @param packed a Region encoded by calling one of this class' packing methods.
     * @param bounds the dimensions of the area to encode; must be no larger in any dimension than the dimensionality of
     *               the CurveStrategy this RegionPacker was constructed with.
     * @return a 1D boolean array representing the multi-dimensional area of bounds, where true is on and false is off
     */
    public boolean[] unpack(Region packed, final int[] bounds)
    {
        if(packed == null)
            throw new ArrayIndexOutOfBoundsException("RegionPacker.unpack() must be given a non-null Region");
        if(bounds == null || bounds.length != curve.dimensionality.length)
            throw new UnsupportedOperationException("Invalid bounds; should be an array with " +
                    curve.dimensionality.length + " elements");
        int b = bounds[0];
        for (int i = 1; i < bounds.length; i++) {
            b *= bounds[i];
        }
        if(b >= 1L << 31)
            throw new UnsupportedOperationException("Bounds are too big!");
        final boolean[] unpacked = new boolean[b];
        if(packed.size() == 0)
            return unpacked;
        packed.forEachWithIndex(new IntIntProcedure() {
            int idx = 0;
            public void value(int each, int odd) {
                if(odd % 2 == 1)
                {
                    for (int toSkip = idx + each; idx < toSkip && idx < curve.maxDistance; idx++) {
                        int u = boundedIndex(bounds, curve.point(idx));
                        if(u >= 0)
                            unpacked[u] = true;
                    }
                }
                else
                {
                    idx += each;
                }
            }
        });
        return unpacked;
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
     * Quickly determines if an x,y position is true or false in the given packed array, without unpacking it.
     * @param packed a short[] returned by pack() or one of the sub-arrays in what is returned by packMulti(); must
     *               not be null (this method does not check due to very tight performance constraints).
     * @param coordinates a vararg or array of coordinates; should have length equal to curve dimensions
     * @return true if the packed data stores true at the given x,y location, or false in any other case.
     */
    public boolean queryPacked(Region packed, int... coordinates)
    {
        int hilbertDistance = curve.distance(coordinates), total = 0;
        if(hilbertDistance < 0)
            return false;
        boolean on = false;
        IntIterator it = packed.intIterator();
        while(it.hasNext())
        {
            total += it.next();
            if(hilbertDistance < total)
                return on;
            on = !on;
        }
        return false;
    }
    /**
     * Quickly determines if a Hilbert Curve index corresponds to true or false in the given packed array, without
     * unpacking it.
     * <br>
     * Typically this method will not be needed by library-consuming code unless that code deals with Hilbert Curves in
     * a frequent and deeply involved manner. It does have the potential to avoid converting to and from x,y coordinates
     * and Hilbert Curve indices unnecessarily, which could matter for high-performance code.
     * @param packed a short[] returned by pack() or one of the sub-arrays in what is returned by packMulti(); must
     *               not be null (this method does not check due to very tight performance constraints).
     * @param hilbert a Hilbert Curve index, such as one taken directly from a packed short[] without extra processing
     * @return true if the packed data stores true at the given Hilbert Curve index, or false in any other case.
     */
    public boolean queryPackedHilbert(Region packed, int hilbert)
    {
        if(hilbert < 0 || hilbert >= curve.maxDistance)
            return false;
        int total = 0;
        boolean on = false;
        IntIterator it = packed.intIterator();
        while(it.hasNext())
        {
            total += it.next();
            if(hilbert < total)
                return on;
            on = !on;
        }
        return false;
    }

    /**
     * Gets all positions that are "on" in the given packed array, without unpacking it, and returns them as a Coord[].
     * @param packed a short[] returned by pack() or one of the sub-arrays in what is returned by packMulti(); must
     *               not be null (this method does not check due to very tight performance constraints).
     * @return a Coord[], ordered by distance along the Hilbert Curve, corresponding to all "on" cells in packed.
     */
    public int[][] allPacked(Region packed)
    {
        IntArrayList distances = packed.onIndices();
        int[][] cs = new int[distances.size()][curve.dimensionality.length];
        IntIterator it = distances.intIterator();
        int i = 0;
        while (it.hasNext())
            cs[i++] = curve.point(it.next());
        return cs;
    }
    /**
     * Gets all positions that are "on" in the given packed array, without unpacking it, and returns them as an array of
     * Hilbert Curve indices.
     * <br>
     * Typically this method will not be needed by library-consuming code unless that code deals with Hilbert Curves in
     * a frequent and deeply involved manner. It does have the potential to avoid converting to and from x,y coordinates
     * and Hilbert Curve indices unnecessarily, which could matter for high-performance code.
     * @param packed a short[] returned by pack() or one of the sub-arrays in what is returned by packMulti(); must
     *               not be null (this method does not check due to very tight performance constraints).
     * @return a Hilbert Curve index array, in ascending distance order, corresponding to all "on" cells in packed.
     */
    public int[] allPackedHilbert(Region packed)
    {
        return packed.onIndices().toArray();
    }

    private static int clamp(int n, int min, int max)
    {
        return Math.min(Math.max(min, n), max - 1);
    }

    /**
     * Move all "on" positions in packed by the number of cells given in xMove and yMove, unless the move
     * would take them further than 0, width - 1 (for xMove) or height - 1 (for yMove), in which case that
     * cell is stopped at the edge (moving any shape by an xMove greater than width or yMove greater than
     * height will move all "on" cells to that edge, in a 1-cell thick line). Returns a new packed short[]
     * and does not modify packed.
     * @param packed a short[] returned by pack() or one of the sub-arrays in what is returned by packMulti()
     * @param xMove distance to move the x-coordinate; can be positive or negative
     * @param yMove distance to move the y-coordinate; can be positive or negative
     * @param width the maximum width; if a cell would move to x at least equal to width, it stops at width - 1
     * @param height the maximum height; if a cell would move to y at least equal to height, it stops at height - 1
     * @return a packed array that encodes "on" for cells that were moved from cells that were "on" in packed
     */
    public static short[] translate(short[] packed, int xMove, int yMove, int width, int height)
    {
        if(packed == null || packed.length <= 1)
        {
            return ALL_WALL;
        }
        ShortVLA vla = new ShortVLA(256);
        boolean on = false;
        int idx = 0, x, y;
        for(int p = 0; p < packed.length; p++, on = !on) {
            if (on) {
                for (int i = idx; i < idx + (packed[p] & 0xffff); i++) {
                    x = clamp(hilbertX[i] + xMove, 0, width);
                    y = clamp(hilbertY[i] + yMove, 0, height);
                    vla.add(hilbertDistances[x + (y << 8)]);
                }
            }
            idx += packed[p] & 0xffff;
        }
        int[] indices = vla.asInts();
        if(indices.length < 1)
            return ALL_WALL;
        Arrays.sort(indices);
        vla = new ShortVLA(128);
        int current, past = indices[0], skip = 0;

        vla.add((short)indices[0]);
        for (int i = 1; i < indices.length; i++) {
            current = indices[i];
            if (current - past > 1)
            {
                vla.add((short) (skip+1));
                skip = 0;
                vla.add((short)(current - past - 1));
            }
            else if(current != past)
                skip++;
            past = current;
        }
        vla.add((short)(skip+1));

        return vla.toArray();
    }

    /**
     * Expand each "on" position in packed to cover a a square with side length equal to 1 + expansion * 2,
     * centered on the original "on" position, unless the expansion would take a cell further than 0,
     * width - 1 (for xMove) or height - 1 (for yMove), in which case that cell is stopped at the edge.
     * Returns a new packed short[] and does not modify packed.
     * @param packed a short[] returned by pack() or one of the sub-arrays in what is returned by packMulti()
     * @param expansion the positive (square) radius, in cells, to expand each cell out by
     * @param width the maximum width; if a cell would move to x at least equal to width, it stops at width - 1
     * @param height the maximum height; if a cell would move to y at least equal to height, it stops at height - 1
     * @return a packed array that encodes "on" for packed and cells that expanded from cells that were "on" in packed
     */
    public static short[] expand(short[] packed, int expansion, int width, int height)
    {
        if(packed == null || packed.length <= 1)
        {
            return ALL_WALL;
        }
        ShortVLA vla = new ShortVLA(256);
        ShortSet ss = new ShortSet(256);
        boolean on = false;
        int idx = 0, x, y;
        short dist;
        for(int p = 0; p < packed.length; p++, on = !on) {
            if (on) {
                for (int i = idx; i < idx + (packed[p] & 0xffff); i++) {
                    x = hilbertX[i];
                    y = hilbertY[i];
                    for (int j = Math.max(0, x - expansion); j <= Math.min(width - 1, x + expansion); j++) {
                        for (int k = Math.max(0, y - expansion); k <= Math.min(height - 1, y + expansion); k++) {
                            dist = hilbertDistances[j + (k << 8)];
                            if (ss.add(dist))
                                vla.add(dist);
                        }
                    }
                }
            }
            idx += packed[p] & 0xffff;
        }

        int[] indices = vla.asInts();
        if(indices.length < 1)
            return ALL_WALL;
        Arrays.sort(indices);

        vla = new ShortVLA(128);
        int current, past = indices[0], skip = 0;

        vla.add((short)indices[0]);
        for (int i = 1; i < indices.length; i++) {
            current = indices[i];
            if (current - past > 1)
            {
                vla.add((short) (skip+1));
                skip = 0;
                vla.add((short)(current - past - 1));
            }
            else if(current != past)
                skip++;
            past = current;
        }
        vla.add((short)(skip+1));
        return vla.toArray();
    }


    /**
     * Expand each "on" position in packed to cover a a square with side length equal to 1 + expansion * 2,
     * centered on the original "on" position, unless the expansion would take a cell further than 0,
     * width - 1 (for xMove) or height - 1 (for yMove), in which case that cell is stopped at the edge.
     * Returns a new packed short[] and does not modify packed.
     * @param packed a short[] returned by pack() or one of the sub-arrays in what is returned by packMulti()
     * @param expansion the positive (square) radius, in cells, to expand each cell out by
     * @param width the maximum width; if a cell would move to x at least equal to width, it stops at width - 1
     * @param height the maximum height; if a cell would move to y at least equal to height, it stops at height - 1
     * @param eightWay true if the expansion should be both diagonal and orthogonal; false for just orthogonal
     * @return a packed array that encodes "on" for packed and cells that expanded from cells that were "on" in packed
     */
    public static short[] expand(short[] packed, int expansion, int width, int height, boolean eightWay)
    {
        if(eightWay)
            return expand(packed, expansion, width, height);
        if(packed == null || packed.length <= 1)
        {
            return ALL_WALL;
        }
        ShortVLA vla = new ShortVLA(256);
        ShortSet ss = new ShortSet(256);
        boolean on = false;
        int idx = 0, x, y;
        short dist;
        int[] xOffsets = new int[]{0, 1, 0, -1, 0}, yOffsets = new int[]{1, 0, -1, 0, 1};
        for(int p = 0; p < packed.length; p++, on = !on) {
            if (on) {
                for (int i = idx; i < idx + (packed[p] & 0xffff); i++) {
                    x = hilbertX[i];
                    y = hilbertY[i];
                    for (int d = 0; d < 4; d++) {
                        for (int e = 1; e <= expansion; e++) {
                            for (int e2 = 0; e2 < expansion; e2++) {
                                int j = Math.min(width - 1, Math.max(0, x + xOffsets[d] * e + yOffsets[d + 1] * e2));
                                int k = Math.min(height - 1, Math.max(0, y + yOffsets[d] * e + xOffsets[d + 1] * e2));
                                dist = hilbertDistances[j + (k << 8)];
                                if (ss.add(dist))
                                    vla.add(dist);
                            }
                        }
                    }
                }
            }
            idx += packed[p] & 0xffff;
        }

        int[] indices = vla.asInts();
        if(indices.length < 1)
            return ALL_WALL;
        Arrays.sort(indices);

        vla = new ShortVLA(128);
        int current, past = indices[0], skip = 0;

        vla.add((short)indices[0]);
        for (int i = 1; i < indices.length; i++) {
            current = indices[i];
            if (current - past > 1)
            {
                vla.add((short) (skip+1));
                skip = 0;
                vla.add((short)(current - past - 1));
            }
            else if(current != past)
                skip++;
            past = current;
        }
        vla.add((short)(skip+1));
        return vla.toArray();
    }


    /**
     * Finds the area around the cells encoded in packed, without including those cells. For each "on"
     * position in packed, expand it to cover a a square with side length equal to 1 + expansion * 2,
     * centered on the original "on" position, unless the expansion would take a cell further than 0,
     * width - 1 (for xMove) or height - 1 (for yMove), in which case that cell is stopped at the edge.
     * If a cell is "on" in packed, it will always be "off" in the result.
     * Returns a new packed short[] and does not modify packed.
     * @param packed a short[] returned by pack() or one of the sub-arrays in what is returned by packMulti()
     * @param expansion the positive (square-shaped) radius, in cells, to expand each cell out by
     * @param width the maximum width; if a cell would move to x at least equal to width, it stops at width - 1
     * @param height the maximum height; if a cell would move to y at least equal to height, it stops at height - 1
     * @return a packed array that encodes "on" for cells that were pushed from the edge of packed's "on" cells
     */
    public static short[] fringe(short[] packed, int expansion, int width, int height)
    {
        if(packed == null || packed.length <= 1)
        {
            return ALL_WALL;
        }
        ShortVLA vla = new ShortVLA(256);
        ShortSet ss = new ShortSet(256);
        boolean on = false;
        int idx = 0;
        short x, y, dist;
        for(int p = 0; p < packed.length; p++, on = !on) {
            if (on) {
                for (int i = idx; i < idx + (packed[p] & 0xffff); i++) {
                    ss.add((short) i);
                }
            }
            idx += packed[p] & 0xffff;
        }
        on = false;
        idx = 0;
        for(int p = 0; p < packed.length; p++, on = !on) {
            if (on) {
                for (int i = idx; i < idx + (packed[p] & 0xffff); i++) {
                    x = hilbertX[i];
                    y = hilbertY[i];
                    for (int j = Math.max(0, x - expansion); j <= Math.min(width - 1, x + expansion); j++) {
                        for (int k = Math.max(0, y - expansion); k <= Math.min(height - 1, y + expansion); k++) {
                            dist = hilbertDistances[j + (k << 8)];
                            if (ss.add(dist))
                                vla.add(dist);
                        }
                    }
                }
            }
            idx += packed[p] & 0xffff;
        }
        int[] indices = vla.asInts();
        if(indices.length < 1)
            return ALL_WALL;
        Arrays.sort(indices);

        vla = new ShortVLA(128);
        int current, past = indices[0], skip = 0;

        vla.add((short)indices[0]);
        for (int i = 1; i < indices.length; i++) {
            current = indices[i];
            if (current - past > 1)
            {
                vla.add((short) (skip+1));
                skip = 0;
                vla.add((short)(current - past - 1));
            }
            else if(current != past)
                skip++;
            past = current;
        }
        vla.add((short)(skip+1));

        return vla.toArray();
    }

    /**
     * Finds the area around the cells encoded in packed, without including those cells. For each "on"
     * position in packed, expand it to cover a a square with side length equal to 1 + expansion * 2,
     * centered on the original "on" position, unless the expansion would take a cell further than 0,
     * width - 1 (for xMove) or height - 1 (for yMove), in which case that cell is stopped at the edge.
     * If a cell is "on" in packed, it will always be "off" in the result.
     * Returns a new packed short[] and does not modify packed.
     * @param packed a short[] returned by pack() or one of the sub-arrays in what is returned by packMulti()
     * @param expansion the positive (square-shaped) radius, in cells, to expand each cell out by
     * @param width the maximum width; if a cell would move to x at least equal to width, it stops at width - 1
     * @param height the maximum height; if a cell would move to y at least equal to height, it stops at height - 1
     * @param eightWay true if the expansion should be both diagonal and orthogonal; false for just orthogonal
     * @return a packed array that encodes "on" for cells that were pushed from the edge of packed's "on" cells
     */
    public static short[] fringe(short[] packed, int expansion, int width, int height, boolean eightWay)
    {
        if(eightWay)
            return fringe(packed, expansion, width, height);
        if(packed == null || packed.length <= 1)
        {
            return ALL_WALL;
        }
        ShortVLA vla = new ShortVLA(256);
        ShortSet ss = new ShortSet(256);
        boolean on = false;
        int idx = 0;
        short x, y, dist;
        int[] xOffsets = new int[]{0, 1, 0, -1, 0}, yOffsets = new int[]{1, 0, -1, 0, 1};
        for(int p = 0; p < packed.length; p++, on = !on) {
            if (on) {
                for (int i = idx; i < idx + (packed[p] & 0xffff); i++) {
                    ss.add((short) i);
                }
            }
            idx += packed[p] & 0xffff;
        }
        on = false;
        idx = 0;
        for(int p = 0; p < packed.length; p++, on = !on) {
            if (on) {
                for (int i = idx; i < idx + (packed[p] & 0xffff); i++) {
                    x = hilbertX[i];
                    y = hilbertY[i];
                    for (int d = 0; d < 4; d++) {
                        for (int e = 1; e <= expansion; e++) {
                            for (int e2 = 0; e2 < expansion; e2++) {
                                int j = Math.min(width - 1, Math.max(0, x + xOffsets[d] * e + yOffsets[d + 1] * e2));
                                int k = Math.min(height - 1, Math.max(0, y + yOffsets[d] * e + xOffsets[d + 1] * e2));
                                dist = hilbertDistances[j + (k << 8)];
                                if (ss.add(dist))
                                    vla.add(dist);
                            }
                        }
                    }

                }
            }
            idx += packed[p] & 0xffff;
        }
        int[] indices = vla.asInts();
        if(indices.length < 1)
            return ALL_WALL;
        Arrays.sort(indices);

        vla = new ShortVLA(128);
        int current, past = indices[0], skip = 0;

        vla.add((short)indices[0]);
        for (int i = 1; i < indices.length; i++) {
            current = indices[i];
            if (current - past > 1)
            {
                vla.add((short) (skip+1));
                skip = 0;
                vla.add((short)(current - past - 1));
            }
            else if(current != past)
                skip++;
            past = current;
        }
        vla.add((short)(skip+1));

        return vla.toArray();
    }

    /**
     * Finds the concentric areas around the cells encoded in packed, without including those cells. For each "on"
     * position in packed, expand it to cover a a square with side length equal to 1 + n * 2, where n starts at 1 and
     * goes up to include the expansions parameter, with each expansion centered on the original "on" position, unless
     * the expansion would take a cell further than 0, width - 1 (for xMove) or height - 1 (for yMove), in which case
     * that cell is stopped at the edge. If a cell is "on" in packed, it will always be "off" in the results.
     * Returns a new packed short[][] where the outer array has length equal to expansions and the inner arrays are
     * packed data encoding a one-cell-wide concentric fringe region. Does not modify packed.
     * @param packed a short[] returned by pack() or one of the sub-arrays in what is returned by packMulti()
     * @param expansions the positive (square-shaped) radius, in cells, to expand each cell out by, also the length
     *                   of the outer array returned by this method
     * @param width the maximum width; if a cell would move to x at least equal to width, it stops at width - 1
     * @param height the maximum height; if a cell would move to y at least equal to height, it stops at height - 1
     * @return an array of packed arrays that encode "on" for cells that were pushed from the edge of packed's "on"
     *          cells; the outer array will have length equal to expansions, and inner arrays will normal packed data
     */
    public static short[][] fringes(short[] packed, int expansions, int width, int height) {
        short[][] finished = new short[expansions][];
        if (packed == null || packed.length <= 1) {
            Arrays.fill(finished, ALL_WALL);
            return finished;
        }
        ShortSet ss = new ShortSet(256);
        boolean on = false;
        int idx = 0;
        short x, y, dist;
        for (int p = 0; p < packed.length; p++, on = !on) {
            if (on) {
                for (int i = idx; i < idx + (packed[p] & 0xffff); i++) {
                    ss.add((short) i);
                }
            }
            idx += packed[p] & 0xffff;
        }
        for (int expansion = 1; expansion <= expansions; expansion++) {
            ShortVLA vla = new ShortVLA(256);
            on = false;
            idx = 0;
            for (int p = 0; p < packed.length; p++, on = !on) {
                if (on) {
                    for (int i = idx; i < idx + (packed[p] & 0xffff); i++) {
                        x = hilbertX[i];
                        y = hilbertY[i];
                        for (int j = Math.max(0, x - expansion); j <= Math.min(width - 1, x + expansion); j++) {
                            for (int k = Math.max(0, y - expansion); k <= Math.min(height - 1, y + expansion); k++) {
                                dist = hilbertDistances[j + (k << 8)];
                                if (ss.add(dist))
                                    vla.add(dist);
                            }
                        }
                    }
                }
                idx += packed[p] & 0xffff;
            }
            int[] indices = vla.asInts();
            if(indices.length < 1)
            {
                finished[expansion - 1] = ALL_WALL;
                continue;
            }
            Arrays.sort(indices);

            vla = new ShortVLA(128);
            int current, past = indices[0], skip = 0;

            vla.add((short) indices[0]);
            for (int i = 1; i < indices.length; i++) {
                current = indices[i];

                if (current != past)
                    skip++;
                if (current - past > 1) {
                    vla.add((short) (skip + 1));
                    skip = 0;
                    vla.add((short) (current - past - 1));
                }
                past = current;
            }
            vla.add((short) (skip + 1));

            finished[expansion-1] = vla.toArray();
        }
        return finished;
    }


    /**
     * Finds the concentric areas around the cells encoded in packed, without including those cells. For each "on"
     * position in packed, expand it to cover a a square with side length equal to 1 + n * 2, where n starts at 1 and
     * goes up to include the expansions parameter, with each expansion centered on the original "on" position, unless
     * the expansion would take a cell further than 0, width - 1 (for xMove) or height - 1 (for yMove), in which case
     * that cell is stopped at the edge. If a cell is "on" in packed, it will always be "off" in the results.
     * Returns a new packed short[][] where the outer array has length equal to expansions and the inner arrays are
     * packed data encoding a one-cell-wide concentric fringe region. Does not modify packed.
     * @param packed a short[] returned by pack() or one of the sub-arrays in what is returned by packMulti()
     * @param expansions the positive (square-shaped) radius, in cells, to expand each cell out by, also the length
     *                   of the outer array returned by this method
     * @param width the maximum width; if a cell would move to x at least equal to width, it stops at width - 1
     * @param height the maximum height; if a cell would move to y at least equal to height, it stops at height - 1
     * @param eightWay true if the expansion should be both diagonal and orthogonal; false for just orthogonal
     * @return an array of packed arrays that encode "on" for cells that were pushed from the edge of packed's "on"
     *          cells; the outer array will have length equal to expansions, and inner arrays will normal packed data
     */
    public static short[][] fringes(short[] packed, int expansions, int width, int height, boolean eightWay) {
        short[][] finished = new short[expansions][];
        if (packed == null || packed.length <= 1) {
            Arrays.fill(finished, ALL_WALL);
            return finished;
        }
        ShortSet ss = new ShortSet(256);
        boolean on = false;
        int idx = 0;
        short x, y, dist;
        int[] xOffsets = new int[]{0, 1, 0, -1, 0}, yOffsets = new int[]{1, 0, -1, 0, 1};
        for (int p = 0; p < packed.length; p++, on = !on) {
            if (on) {
                for (int i = idx; i < idx + (packed[p] & 0xffff); i++) {
                    ss.add((short) i);
                }
            }
            idx += packed[p] & 0xffff;
        }
        for (int expansion = 1; expansion <= expansions; expansion++) {
            ShortVLA vla = new ShortVLA(256);
            on = false;
            idx = 0;
            for (int p = 0; p < packed.length; p++, on = !on) {
                if (on) {
                    for (int i = idx; i < idx + (packed[p] & 0xffff); i++) {
                        x = hilbertX[i];
                        y = hilbertY[i];
                        for (int d = 0; d < 4; d++) {
                            for (int e = 1; e <= expansion; e++) {
                                for (int e2 = 0; e2 < expansion; e2++) {
                                    int j = Math.min(width - 1, Math.max(0, x + xOffsets[d] * e + yOffsets[d + 1] * e2));
                                    int k = Math.min(height - 1, Math.max(0, y + yOffsets[d] * e + xOffsets[d + 1] * e2));
                                    dist = hilbertDistances[j + (k << 8)];
                                    if (ss.add(dist))
                                        vla.add(dist);
                                }
                            }
                        }
                    }
                }
                idx += packed[p] & 0xffff;
            }
            int[] indices = vla.asInts();
            if(indices.length < 1)
            {
                finished[expansion - 1] = ALL_WALL;
                continue;
            }
            Arrays.sort(indices);

            vla = new ShortVLA(128);
            int current, past = indices[0], skip = 0;

            vla.add((short) indices[0]);
            for (int i = 1; i < indices.length; i++) {
                current = indices[i];

                if (current != past)
                    skip++;
                if (current - past > 1) {
                    vla.add((short) (skip + 1));
                    skip = 0;
                    vla.add((short) (current - past - 1));
                }
                past = current;
            }
            vla.add((short) (skip + 1));

            finished[expansion-1] = vla.toArray();
        }
        return finished;
    }


    /**
     * Given a packed array encoding a larger area, a packed array encoding one or more points inside bounds, and an
     * amount of expansion, expands each cell in start by a Manhattan (diamond) radius equal to expansion, limiting any
     * expansion to within bounds and returning the final expanded (limited) packed data.  Notably, if a small area is
     * not present within bounds, then the flood will move around the "hole" similarly to DijkstraMap's behavior;
     * essentially, it needs to expand around the hole to get to the other side, and this takes more steps of expansion
     * than crossing straight over.
     * Returns a new packed short[] and does not modify bounds or start.
     * @param bounds packed data representing the maximum extent of the region to flood-fill; often floors
     * @param start a packed array that encodes position(s) that the flood will spread outward from
     * @param expansion the positive (square) radius, in cells, to expand each cell out by
     * @return a packed array that encodes "on" for cells that are "on" in bounds and are within expansion Manhattan
     * distance from a Coord in start
     */
    public static short[] flood(short[] bounds, short[] start, int expansion)
    {
        if(bounds == null || bounds.length <= 1)
        {
            return ALL_WALL;
        }
        int boundSize = count(bounds);
        ShortVLA vla = new ShortVLA(256);
        ShortSet ss = new ShortSet(boundSize), quickBounds = new ShortSet(boundSize);
        boolean on = false;
        int idx = 0;
        short x, y, dist;
        for(int p = 0; p < bounds.length; p++, on = !on) {
            if (on) {
                for (int i = idx; i < idx + (bounds[p] & 0xffff); i++) {
                    quickBounds.add((short) i);
                }
            }
            idx += bounds[p] & 0xffff;
        }
        short[] s2 = allPackedHilbert(start);
        int[] xOffsets = new int[]{0, 1, 0, -1}, yOffsets = new int[]{1, 0, -1, 0};
        for (int e = 0; e < expansion; e++) {
            ShortVLA edge = new ShortVLA(128);
            for (int s = 0; s < s2.length; s++) {
                int i = s2[s] & 0xffff;
                x = hilbertX[i];
                y = hilbertY[i];
                for (int d = 0; d < 4; d++) {
                    int j = Math.min(255, Math.max(0, x + xOffsets[d]));
                    int k = Math.min(255, Math.max(0, y + yOffsets[d]));
                    dist = hilbertDistances[j + (k << 8)];
                    if (quickBounds.contains(dist)) {
                        if (ss.add(dist)) {
                            vla.add(dist);
                            edge.add(dist);
                        }
                    }
                }
            }
            s2 = edge.toArray();
        }

        int[] indices = vla.asInts();
        if(indices.length < 1)
            return ALL_WALL;
        Arrays.sort(indices);

        vla = new ShortVLA(128);
        int current, past = indices[0], skip = 0;

        vla.add((short)indices[0]);
        for (int i = 1; i < indices.length; i++) {
            current = indices[i];
            if (current - past > 1)
            {
                vla.add((short) (skip+1));
                skip = 0;
                vla.add((short)(current - past - 1));
            }
            else if(current != past)
                skip++;
            past = current;
        }
        vla.add((short)(skip+1));

        return vla.toArray();
    }


    /**
     * Given a packed array encoding a larger area, a packed array encoding one or more points inside bounds, and an
     * amount of expansion, expands each cell in start by a radius (if eightWay is true, it uses Chebyshev distance; if
     * it is false, it uses Manhattan distance) equal to expansion, limiting any expansion to within bounds and
     * returning the final expanded (limited) packed data. Notably, if a small area is not present within bounds, then
     * the flood will move around the "hole" similarly to DijkstraMap's behavior; essentially, it needs to expand around
     * the hole to get to the other side, and this takes more steps of expansion than crossing straight over.
     * Returns a new packed short[] and does not modify bounds or start.
     * @param bounds packed data representing the maximum extent of the region to flood-fill; often floors
     * @param start a packed array that encodes position(s) that the flood will spread outward from
     * @param expansion the positive (square) radius, in cells, to expand each cell out by
     * @param eightWay true to flood-fill out in all eight directions at each step, false for just orthogonal
     * @return a packed array that encodes "on" for cells that are "on" in bounds and are within expansion either
     * Chebyshev (if eightWay is true) or Manhattan (otherwise) distance from a Coord in start
     */
    public static short[] flood(short[] bounds, short[] start, int expansion, boolean eightWay)
    {
        if(!eightWay)
            return flood(bounds, start, expansion);
        if(bounds == null || bounds.length <= 1)
        {
            return ALL_WALL;
        }
        int boundSize = count(bounds);
        ShortVLA vla = new ShortVLA(256);
        ShortSet ss = new ShortSet(boundSize), quickBounds = new ShortSet(boundSize);
        boolean on = false;
        int idx = 0;
        short x, y, dist;
        for(int p = 0; p < bounds.length; p++, on = !on) {
            if (on) {
                for (int i = idx; i < idx + (bounds[p] & 0xffff); i++) {
                    quickBounds.add((short) i);
                }
            }
            idx += bounds[p] & 0xffff;
        }
        short[] s2 = allPackedHilbert(start);
        int[] xOffsets = new int[]{-1, 0, 1, -1,    1, -1, 0, 1}, yOffsets = new int[]{-1, -1, -1, 0,    0, 1, 1, 1};
        for (int e = 0; e < expansion; e++) {
            ShortVLA edge = new ShortVLA(128);
            for (int s = 0; s < s2.length; s++) {
                int i = s2[s] & 0xffff;
                x = hilbertX[i];
                y = hilbertY[i];
                for (int d = 0; d < 8; d++) {
                    int j = Math.min(255, Math.max(0, x + xOffsets[d]));
                    int k = Math.min(255, Math.max(0, y + yOffsets[d]));
                    dist = hilbertDistances[j + (k << 8)];
                    if (quickBounds.contains(dist)) {
                        if (ss.add(dist)) {
                            vla.add(dist);
                            edge.add(dist);
                        }
                    }
                }
            }
            s2 = edge.toArray();
        }

        int[] indices = vla.asInts();
        if(indices.length < 1)
            return ALL_WALL;
        Arrays.sort(indices);

        vla = new ShortVLA(128);
        int current, past = indices[0], skip = 0;

        vla.add((short)indices[0]);
        for (int i = 1; i < indices.length; i++) {
            current = indices[i];
            if (current - past > 1)
            {
                vla.add((short) (skip+1));
                skip = 0;
                vla.add((short)(current - past - 1));
            }
            else if(current != past)
                skip++;
            past = current;
        }
        vla.add((short)(skip+1));

        return vla.toArray();
    }


    private static void modifiedShadowFOV(int expansion, int viewerX, int viewerY, Radius metric, ShortSet bounds, ShortSet storedSet, ShortVLA vla)
    {
        if(expansion < 1)
            return;
        short start = hilbertDistances[viewerX + (viewerY << 8)];
        if(storedSet.add(start))
            vla.add(start);

        for (Direction d : Direction.DIAGONALS) {
            modifiedShadowCast(expansion, 1, 1.0, 0.0, 0, d.deltaX, d.deltaY, 0, viewerX, viewerY, metric, bounds, storedSet, vla);
            modifiedShadowCast(expansion, 1, 1.0, 0.0, d.deltaX, 0, 0, d.deltaY, viewerX, viewerY, metric, bounds, storedSet, vla);
        }
    }

    private static void modifiedShadowCast(int expansion, int row, double start, double end, int xx, int xy, int yx, int yy,
                                     int viewerX, int viewerY, Radius metric, ShortSet bounds, ShortSet storedSet, ShortVLA vla) {
        double newStart = 0;
        if (start < end) {
            return;
        }

        boolean blocked = false;
        int dist;
        short currentPos;
        for (int distance = row; distance <= expansion && !blocked; distance++) {
            int deltaY = -distance;
            for (int deltaX = -distance; deltaX <= 0; deltaX++) {
                int currentX = viewerX + deltaX * xx + deltaY * xy;
                int currentY = viewerY + deltaX * yx + deltaY * yy;
                double leftSlope = (deltaX - 0.5f) / (deltaY + 0.5f);
                double rightSlope = (deltaX + 0.5f) / (deltaY - 0.5f);
                currentPos = hilbertDistances[currentX + (currentY << 8)];

                /*
                if (!bounds.contains(currentPos)) {
                    newStart = rightSlope;
                    continue;
                }
                else
                 */
                if(!(currentX - viewerX + expansion >= 0 && currentX - viewerX <= expansion
                        && currentY - viewerY + expansion >= 0 && currentY - viewerY <= expansion)
                        || start < rightSlope) {
                    continue;
                } else if (end > leftSlope) {
                    break;
                }

                if (blocked) { //previous cell was a blocking one
                    if (!bounds.contains(currentPos)) {//hit a wall
                        newStart = rightSlope;
                    } else {
                        blocked = false;
                        start = newStart;
                        dist = metric.roughDistance(currentX - viewerX, currentY - viewerY);
                        //check if it's within the lightable area and light if needed
                        if (dist <= expansion * 2) {
                            if(storedSet.add(currentPos))
                                vla.add(currentPos);
                        }
                    }
                } else {
                    if (!bounds.contains(currentPos) && distance < expansion) {//hit a wall within sight line
                        blocked = true;
                        modifiedShadowCast(expansion, distance + 1, start, leftSlope, xx, xy, yx, yy, viewerX, viewerY, metric, bounds, storedSet, vla);
                        newStart = rightSlope;
                    }
                    else
                    {
                        if(bounds.contains(currentPos)) {
                            dist = metric.roughDistance(currentX - viewerX, currentY - viewerY);
                            //check if it's within the lightable area and light if needed
                            if (dist <= expansion * 2) {
                                if (storedSet.add(currentPos))
                                    vla.add(currentPos);
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Given a packed array encoding a larger area, a packed array encoding one or more points inside bounds, and an
     * amount of expansion, expands each cell in start by a Manhattan (diamond) radius equal to expansion, limiting any
     * expansion to within bounds and returning the final expanded (limited) packed data.
     * Though this is otherwise similar to flood(), radiate() behaves like FOV and will not move around obstacles and
     * will instead avoid expanding if it would go into any cell that cannot be reached by a straight line (drawn
     * directly, not in grid steps) that is mostly unobstructed.
     * Returns a new packed short[] and does not modify bounds or start.
     * @param bounds packed data representing the maximum extent of the region to flood-fill; often floors
     * @param start a packed array that encodes position(s) that the flood will spread outward from
     * @param expansion the positive (square) radius, in cells, to expand each cell out by
     * @return a packed array that encodes "on" for cells that are "on" in bounds and are within expansion Manhattan
     * distance from a Coord in start
     */
    public static short[] radiate(short[] bounds, short[] start, int expansion)
    {
        return radiate(bounds, start, expansion, Radius.DIAMOND);
    }
    /**
     * Given a packed array encoding a larger area, a packed array encoding one or more points inside bounds, and an
     * amount of expansion, expands each cell in start by a radius, with a shape determined by metric, equal to
     * expansion, limiting any expansion to within bounds and returning the final expanded (limited) packed data.
     * Though this is otherwise similar to flood(), radiate() behaves like FOV and will not move around obstacles and
     * will instead avoid expanding if it would go into any cell that cannot be reached by a straight line (drawn
     * directly, not in grid steps) that is mostly unobstructed.
     * Returns a new packed short[] and does not modify bounds or start.
     * @param bounds packed data representing the maximum extent of the region to flood-fill; often floors
     * @param start a packed array that encodes position(s) that the flood will spread outward from
     * @param expansion the positive (square) radius, in cells, to expand each cell out by
     * @param metric a Radius that defines how this should expand, SQUARE for 8-way, DIAMOND for 4-way, CIRCLE for
     *               Euclidean expansion (not guaranteed to be perfectly circular)
     * @return a packed array that encodes "on" for cells that are "on" in bounds and are within expansion Manhattan
     * distance from a Coord in start
     */
    public static short[] radiate(short[] bounds, short[] start, int expansion, Radius metric)
    {
        if(bounds == null || bounds.length <= 1)
        {
            return ALL_WALL;
        }
        int boundSize = count(bounds);
        ShortVLA vla = new ShortVLA(256);
        ShortSet storedSet = new ShortSet(boundSize), quickBounds = new ShortSet(boundSize);
        boolean on = false;
        int idx = 0, i;
        short x, y;
        for(int p = 0; p < bounds.length; p++, on = !on) {
            if (on) {
                for (i = idx; i < idx + (bounds[p] & 0xffff); i++) {
                    quickBounds.add((short) i);
                }
            }
            idx += bounds[p] & 0xffff;
        }
        short[] s2 = allPackedHilbert(start);
        for (int s = 0; s < s2.length; s++) {
            i = s2[s] & 0xffff;
            x = hilbertX[i];
            y = hilbertY[i];

            modifiedShadowFOV(expansion, x, y, metric, quickBounds, storedSet, vla);
        }

        int[] indices = vla.asInts();
        if(indices.length < 1)
            return ALL_WALL;
        Arrays.sort(indices);

        vla = new ShortVLA(128);
        int current, past = indices[0], skip = 0;

        vla.add((short)indices[0]);
        for (i = 1; i < indices.length; i++) {
            current = indices[i];
            if (current - past > 1)
            {
                vla.add((short) (skip+1));
                skip = 0;
                vla.add((short)(current - past - 1));
            }
            else if(current != past)
                skip++;
            past = current;
        }
        vla.add((short)(skip+1));

        return vla.toArray();
    }


    /**
     * Given a packed array encoding a larger area, a packed array encoding one or more points inside bounds, and an
     * amount of expansion, expands each cell in start by a radius, with a square shape if eightWay is true or a diamond
     * otherwise, equal to expansion, limiting any expansion to within bounds and returning the final expanded (limited)
     * packed data. Though this is otherwise similar to flood(), radiate() behaves like FOV and will not move around
     * obstacles and will instead avoid expanding if it would go into any cell that cannot be reached by a straight line
     * (drawn directly, not in grid steps) that is mostly unobstructed.
     * Returns a new packed short[] and does not modify bounds or start.
     * @param bounds packed data representing the maximum extent of the region to flood-fill; often floors
     * @param start a packed array that encodes position(s) that the flood will spread outward from
     * @param expansion the positive (square) radius, in cells, to expand each cell out by
     * @param eightWay true to flood-fill out in all eight directions at each step, false for just orthogonal
     * @return a packed array that encodes "on" for cells that are "on" in bounds and are within expansion either
     * Chebyshev (if eightWay is true) or Manhattan (otherwise) distance from a Coord in start
     */
    public static short[] radiate(short[] bounds, short[] start, int expansion, boolean eightWay)
    {
        if(eightWay)
            return radiate(bounds, start, expansion, Radius.SQUARE);
        return radiate(bounds, start, expansion, Radius.DIAMOND);
    }

    /**
     * Given a width and height, returns a packed array that encodes "on" for the rectangle from (0,0) to
     * (width - 1, height - 1). Primarily useful with intersectPacked() to ensure things like negatePacked() that can
     * encode "on" cells in any position are instead limited to the bounds of the map.
     * @param width the width of the rectangle
     * @param height the height of the rectangle
     * @return a packed short[] encoding "on" for all cells with x less than width and y less than height.
     */
    public static short[] rectangle(int width, int height)
    {
        if(width > 256 || height > 256)
            throw new UnsupportedOperationException("Map size is too large to efficiently pack, aborting");
        boolean[][] rect = new boolean[width][height];
        for (int i = 0; i < width; i++) {
            Arrays.fill(rect[i], true);
        }
        return pack(rect);
    }
    /**
     * Given x, y, width and height, returns a packed array that encodes "on" for the rectangle from (x,y) to
     * (width - 1, height - 1). Primarily useful with intersectPacked() to ensure things like negatePacked() that can
     * encode "on" cells in any position are instead limited to the bounds of the map, but also handy for basic "box
     * drawing" for other uses.
     * @param x the minimum x coordinate
     * @param y the minimum y coordinate
     * @param width the width of the rectangle
     * @param height the height of the rectangle
     * @return a packed short[] encoding "on" for all cells with x less than width and y less than height.
     */
    public static short[] rectangle(int x, int y, int width, int height)
    {
        int width2 = width, height2 = height;
        if(x + width >= 256)
            width2 = 255 - x;
        if(y + height >= 256)
            height2 = 255 - y;
        if(width2 < 0 || height2 < 0 || x < 0 || y < 0)
            return ALL_WALL;
        boolean[][] rect = new boolean[x + width2][y + height2];
        for (int i = x; i < x + width2; i++) {
            Arrays.fill(rect[i], y, y + height2, true);
        }
        return pack(rect);
    }
    /**
     * Given x, y, width and height, returns an array of all Hilbert distance within the rectangle from (x,y) to
     * (width - 1, height - 1).
     * @param x the minimum x coordinate
     * @param y the minimum y coordinate
     * @param width the width of the rectangle
     * @param height the height of the rectangle
     * @return a short[] that is not packed, and instead stores individual Hilbert distances in the rectangle
     */
    public static short[] rectangleHilbert(int x, int y, int width, int height)
    {
        int width2 = width, height2 = height;
        if(x + width >= 256)
            width2 = 256 - x;
        if(y + height >= 256)
            height2 = 256 - y;
        if(width2 <= 0 || height2 <= 0 || x < 0 || y < 0)
            return new short[0];
        short[] hilberts = new short[width2 * height2];
        int idx = 0;
        for (int i = x; i < x + width2; i++) {
            for (int j = y; j < y + height2; j++) {
                hilberts[idx++] = hilbertDistances[i + (j << 8)];
            }
        }
        return hilberts;
    }

    /**
     * Counts the number of "on" cells encoded in a packed array without unpacking it.
     * @param packed a packed short array, as produced by pack()
     * @return the number of "on" cells.
     */
    public static int count(short[] packed)
    {
        return count(packed, true);
    }

    /**
     * Counts the number of cells encoding a boolean equal to wanted in a packed array without unpacking it.
     * @param packed a packed short array, as produced by pack()
     * @param wanted the boolean you want to count, true for "on" and false for "off"
     * @return the number of cells that encode a value equal to wanted.
     */
    public static int count(short[] packed, boolean wanted)
    {
        int c = 0;
        boolean on = false;
        for (int i = 0; i < packed.length; i++, on = !on) {
            if(on == wanted)
                c += packed[i] & 0xffff;
        }
        return c;
    }
    /**
     * Finds how many cells are encoded in a packed array (both on and off) without unpacking it.
     * @param packed a packed short array, as produced by pack()
     * @return the number of cells that are encoded explicitly in the packed data as either on or off.
     */
    public static int covered(short[] packed)
    {
        int c = 0;
        for (int i = 0; i < packed.length; i++) {
            c += packed[i] & 0xffff;
        }
        return c;
    }
    /**
     * Given two packed short arrays, left and right, this produces a packed short array that encodes "on" for any cell
     * that was "on" in either left or in right, and only encodes "off" for cells that were off in both. This method
     * does not do any unpacking (which can be somewhat computationally expensive) and so should be strongly preferred
     * when merging two pieces of packed data.
     * @param left A packed array such as one produced by pack()
     * @param right A packed array such as one produced by pack()
     * @return A packed array that encodes "on" for all cells that were "on" in either left or right
     */
    public static short[] unionPacked(short[] left, short[] right)
    {
        if(left.length == 0)
            return right;
        if(right.length == 0)
            return left;
        ShortVLA packing = new ShortVLA(64);
        boolean on = false, onLeft = false, onRight = false;
        int idx = 0, skip = 0, elemLeft = 0, elemRight = 0, totalLeft = 0, totalRight = 0;
        while ((elemLeft < left.length || elemRight < right.length) && idx <= 0xffff) {
            if (elemLeft >= left.length) {
                totalLeft = 0xffff;
                onLeft = false;
            }
            else if(totalLeft <= idx) {
                totalLeft += left[elemLeft] & 0xffff;
            }
            if(elemRight >= right.length) {
                totalRight = 0xffff;
                onRight = false;
            }
            else if(totalRight <= idx) {
                totalRight += right[elemRight] & 0xffff;
            }
            // 300, 5, 6, 8, 2, 4
            // 290, 12, 9, 1
            // =
            // 290, 15, 6, 8, 2, 4
            // 290 off in both, 10 in right, 2 in both, 3 in left, 6 off in both, 1 on in both, 7 on in left, 2 off in
            //     both, 4 on in left
            if(totalLeft < totalRight)
            {
                onLeft = !onLeft;
                skip += totalLeft - idx;
                idx = totalLeft;
                if(on != (onLeft || onRight)) {
                    packing.add((short) skip);
                    skip = 0;
                    on = !on;
                }
                elemLeft++;
            }
            else if(totalLeft == totalRight)
            {
                onLeft = !onLeft;
                onRight = !onRight;
                skip += totalLeft - idx;
                idx = totalLeft;
                if(on != (onLeft || onRight)) {
                    packing.add((short) skip);
                    skip = 0;
                    on = !on;
                }
                elemLeft++;
                elemRight++;

            }
            else
            {
                onRight = !onRight;
                skip += totalRight - idx;
                idx = totalRight;
                if(on != (onLeft || onRight)) {
                    packing.add((short) skip);
                    skip = 0;
                    on = !on;
                }
                elemRight++;
            }
        }
        return packing.toArray();
    }

    /**
     * Given two packed short arrays, left and right, this produces a packed short array that encodes "on" for any cell
     * that was "on" in both left and in right, and encodes "off" for cells that were off in either array. This method
     * does not do any unpacking (which can be somewhat computationally expensive) and so should be strongly preferred
     * when finding the intersection of two pieces of packed data.
     * @param left A packed array such as one produced by pack()
     * @param right A packed array such as one produced by pack()
     * @return A packed array that encodes "on" for all cells that were "on" in both left and right
     */
    public static short[] intersectPacked(short[] left, short[] right)
    {
        if(left.length == 0 || right.length == 0)
            return ALL_WALL;
        ShortVLA packing = new ShortVLA(64);
        boolean on = false, onLeft = false, onRight = false;
        int idx = 0, skip = 0, elemLeft = 0, elemRight = 0, totalLeft = 0, totalRight = 0;
        while ((elemLeft < left.length && elemRight < right.length) && idx <= 0xffff) {
            if (elemLeft >= left.length) {
                totalLeft = 0xffff;
                onLeft = false;
            }
            else if(totalLeft <= idx) {
                totalLeft += left[elemLeft] & 0xffff;
            }
            if(elemRight >= right.length) {
                totalRight = 0xffff;
                onRight = false;
            }
            else if(totalRight <= idx) {
                totalRight += right[elemRight] & 0xffff;
            }
            // 300, 5, 6, 8, 2, 4
            // 290, 12, 9, 1
            // =
            // 300, 2, 9, 1
            // 300 off, 2 on, 9 off, 1 on
            if(totalLeft < totalRight)
            {
                onLeft = !onLeft;
                skip += totalLeft - idx;
                idx = totalLeft;
                if(on != (onLeft && onRight)) {
                    packing.add((short) skip);
                    skip = 0;
                    on = !on;
                }
                elemLeft++;
            }
            else if(totalLeft == totalRight)
            {
                onLeft = !onLeft;
                onRight = !onRight;
                skip += totalLeft - idx;
                idx = totalLeft;
                if(on != (onLeft && onRight)) {
                    packing.add((short) skip);
                    skip = 0;
                    on = !on;
                }
                elemLeft++;
                elemRight++;

            }
            else
            {
                onRight = !onRight;
                skip += totalRight - idx;
                idx = totalRight;
                if(on != (onLeft && onRight)) {
                    packing.add((short) skip);
                    skip = 0;
                    on = !on;
                }
                elemRight++;
            }
        }
        return packing.toArray();
    }

    /**
     * Given one packed short array, this produces a packed short array that is the exact opposite of the one passed in,
     * that is, every "on" cell becomes "off" and every "off" cell becomes "on", including cells that were "off" because
     * they were beyond the boundaries of the original 2D array passed to pack() or a similar method. This method does
     * not do any unpacking (which can be somewhat computationally expensive), and actually requires among the lowest
     * amounts of computation to get a result of any methods in RegionPacker. However, because it will cause cells to be
     * considered "on" that would cause an exception if directly converted to x,y positions and accessed in the source
     * 2D array, this method should primarily be used in conjunction with operations such as intersectPacked(), or have
     * the checking for boundaries handled internally by unpack() or related methods such as unpackMultiDouble().
     * @param original A packed array such as one produced by pack()
     * @return A packed array that encodes "on" all cells that were "off" in original
     */
    public static short[] negatePacked(short[] original) {
        if (original.length <= 1) {
            return ALL_ON;
        }
        if (original[0] == 0) {
            short[] copy = new short[original.length - 2];
            System.arraycopy(original, 1, copy, 0, original.length - 2);
            return copy;
        }
        short[] copy = new short[original.length + 2];
        copy[0] = 0;
        System.arraycopy(original, 0, copy, 1, original.length);
        copy[copy.length - 1] = (short) (0xFFFF - covered(copy));
        return copy;
    }

    /**
     * Given two packed short arrays, left and right, this produces a packed short array that encodes "on" for any cell
     * that was "on" in left but "off" in right, and encodes "off" for cells that were "on" in right or "off" in left.
     * This method does not do any unpacking (which can be somewhat computationally expensive) and so should be strongly
     * preferred when finding a region of one packed array that is not contained in another packed array.
     * @param left A packed array such as one produced by pack()
     * @param right A packed array such as one produced by pack()
     * @return A packed array that encodes "on" for all cells that were "on" in left and "off" in right
     */
    public static short[] differencePacked(short[] left, short[] right)
    {
        if(left.length <= 1)
            return ALL_WALL;
        if(right.length <= 1)
            return left;
        ShortVLA packing = new ShortVLA(64);
        boolean on = false, onLeft = false, onRight = false;
        int idx = 0, skip = 0, elemLeft = 0, elemRight = 0, totalLeft = 0, totalRight = 0;
        while ((elemLeft < left.length || elemRight < right.length) && idx <= 0xffff) {
            if (elemLeft >= left.length) {
                totalLeft = 0xffff;
                onLeft = false;
            }
            else if(totalLeft <= idx) {
                totalLeft += left[elemLeft] & 0xffff;
            }
            if(elemRight >= right.length) {
                totalRight = 0xffff;
                onRight = false;
            }
            else if(totalRight <= idx) {
                totalRight += right[elemRight] & 0xffff;
            }
            if(totalLeft < totalRight)
            {
                onLeft = !onLeft;
                skip += totalLeft - idx;
                idx = totalLeft;
                if(on != (onLeft && !onRight)) {
                    packing.add((short) skip);
                    skip = 0;
                    on = !on;
                }
                elemLeft++;
            }
            else if(totalLeft == totalRight)
            {
                onLeft = !onLeft;
                onRight = !onRight;
                skip += totalLeft - idx;
                idx = totalLeft;
                if(on != (onLeft && !onRight)) {
                    packing.add((short) skip);
                    skip = 0;
                    on = !on;
                }
                elemLeft++;
                elemRight++;

            }
            else
            {
                onRight = !onRight;
                skip += totalRight - idx;
                idx = totalRight;
                if(on != (onLeft && !onRight)) {
                    packing.add((short) skip);
                    skip = 0;
                    on = !on;
                }
                elemRight++;
            }
        }
        return packing.toArray();
    }

    /**
     * Given two packed short arrays, left and right, this produces a packed short array that encodes "on" for any cell
     * that was "on" only in left or only in right, but not a cell that was "off" in both or "on" in both. This method
     * does not do any unpacking (which can be somewhat computationally expensive) and so should be strongly preferred
     * when performing an exclusive-or operation on two pieces of packed data.
     * <br>
     * Could more-correctly be called exclusiveDisjunctionPacked to match the other terms, but... seriously?
     * @param left A packed array such as one produced by pack()
     * @param right A packed array such as one produced by pack()
     * @return A packed array that encodes "on" for all cells such that left's cell ^ right's cell returns true
     */
    public static short[] xorPacked(short[] left, short[] right)
    {
        if(left.length == 0)
            return right;
        if(right.length == 0)
            return left;
        ShortVLA packing = new ShortVLA(64);
        boolean on = false, onLeft = false, onRight = false;
        int idx = 0, skip = 0, elemLeft = 0, elemRight = 0, totalLeft = 0, totalRight = 0;
        while ((elemLeft < left.length || elemRight < right.length) && idx <= 0xffff) {
            if (elemLeft >= left.length) {
                totalLeft = 0xffff;
                onLeft = false;
            }
            else if(totalLeft <= idx) {
                totalLeft += left[elemLeft] & 0xffff;
            }
            if(elemRight >= right.length) {
                totalRight = 0xffff;
                onRight = false;
            }
            else if(totalRight <= idx) {
                totalRight += right[elemRight] & 0xffff;
            }
            // 300, 5, 6, 8, 2, 4
            // 290, 12, 9, 1
            // =
            // 290, 15, 6, 8, 2, 4
            // 290 off in both, 10 in right, 2 in both, 3 in left, 6 off in both, 1 on in both, 7 on in left, 2 off in
            //     both, 4 on in left
            if(totalLeft < totalRight)
            {
                onLeft = !onLeft;
                skip += totalLeft - idx;
                idx = totalLeft;
                if(on != (onLeft ^ onRight)) {
                    packing.add((short) skip);
                    skip = 0;
                    on = !on;
                }
                elemLeft++;
            }
            else if(totalLeft == totalRight)
            {
                onLeft = !onLeft;
                onRight = !onRight;
                skip += totalLeft - idx;
                idx = totalLeft;
                if(on != (onLeft ^ onRight)) {
                    packing.add((short) skip);
                    skip = 0;
                    on = !on;
                }
                elemLeft++;
                elemRight++;

            }
            else
            {
                onRight = !onRight;
                skip += totalRight - idx;
                idx = totalRight;
                if(on != (onLeft ^ onRight)) {
                    packing.add((short) skip);
                    skip = 0;
                    on = !on;
                }
                elemRight++;
            }
        }
        return packing.toArray();
    }

    /**
     * Returns a new packed short[] containing the Hilbert distance hilbert as "on", and all other cells "off".
     * Much more efficient than packSeveral called with only one argument.
     * @param hilbert a Hilbert distance that will be encoded as "on"
     * @return the point given to this encoded as "on" in a packed short array
     */
    public static short[] packOne(int hilbert)
    {
        return new short[]{(short) hilbert, 1};
    }
    /**
     * Returns a new packed short[] containing the Coord point as "on", and all other cells "off".
     * Much more efficient than packSeveral called with only one argument.
     * @param point a Coord that will be encoded as "on"
     * @return the point given to this encoded as "on" in a packed short array
     */
    public static short[] packOne(Coord point)
    {
        return new short[]{(short) coordToHilbert(point), 1};
    }
    /**
     * Returns a new packed short[] containing the given x,y cell as "on", and all other cells "off".
     * Much more efficient than packSeveral called with only one argument.
     * @param x the x component of the point that will be encoded as "on"
     * @param y the y component of the point that will be encoded as "on"
     * @return the point given to this encoded as "on" in a packed short array
     */
    public static short[] packOne(int x, int y)
    {
        return new short[]{(short) posToHilbert(x, y), 1};
    }
    /**
     * Returns a new packed short[] containing the Hilbert distances in hilbert as "on" cells, and all other cells "off"
     * @param hilbert a vararg or array of Hilbert distances that will be encoded as "on"
     * @return the points given to this encoded as "on" in a packed short array
     */
    public static short[] packSeveral(int... hilbert)
    {
        if(hilbert.length == 0)
            return ALL_WALL;
        Arrays.sort(hilbert);
        ShortVLA vla = new ShortVLA(128);
        int current, past = hilbert[0], skip = 0;

        vla.add((short)hilbert[0]);
        for (int i = 1; i < hilbert.length; i++) {
            current = hilbert[i];
            if (current - past > 1)
            {
                vla.add((short) (skip+1));
                skip = 0;
                vla.add((short)(current - past - 1));
            }
            else if(current != past)
                skip++;
            past = current;
        }
        vla.add((short)(skip+1));
        return vla.toArray();
    }

    /**
     * Returns a new packed short[] containing the Coords in points as "on" cells, and all other cells "off"
     * @param points a vararg or array of Coords that will be encoded as "on"
     * @return the points given to this encoded as "on" in a packed short array
     */
    public static short[] packSeveral(Coord... points)
    {
        if(points.length == 0)
            return ALL_WALL;
        int[] hilbert = new int[points.length];
        for (int i = 0; i < points.length; i++) {
            hilbert[i] = coordToHilbert(points[i]);
        }

        Arrays.sort(hilbert);
        ShortVLA vla = new ShortVLA(128);
        int current, past = hilbert[0], skip = 0;

        vla.add((short)hilbert[0]);
        for (int i = 1; i < hilbert.length; i++) {
            current = hilbert[i];
            if (current - past > 1)
            {
                vla.add((short) (skip+1));
                skip = 0;
                vla.add((short)(current - past - 1));
            }
            else if(current != past)
                skip++;
            past = current;
        }
        vla.add((short)(skip+1));
        return vla.toArray();
    }
    /**
     * Given one packed short array, original, and a Hilbert Curve index, hilbert, this produces a packed short array
     * that encodes "on" for any cell that was "on" in original, always encodes "on" for the position referred
     * to by hilbert, and encodes "off" for cells that were "off" in original and are not the cell hilbert refers to.
     * This method does not do any unpacking (which can be somewhat computationally expensive) and so should be strongly
     * preferred when finding a region of one packed array that is not contained in another packed array.
     * @param original A packed array such as one produced by pack()
     * @param hilbert A Hilbert Curve index that should be inserted into the result
     * @return A packed array that encodes "on" for all cells that are "on" in original or correspond to hilbert
     */
    public static short[] insertPacked(short[] original, short hilbert)
    {
        return unionPacked(original, new short[]{hilbert, 1});
    }
    /**
     * Given one packed short array, original, and a position as x,y numbers, this produces a packed short array
     * that encodes "on" for any cell that was "on" in original, always encodes "on" for the position referred
     * to by x and y, and encodes "off" for cells that were "off" in original and are not the cell x and y refer to.
     * This method does not do any unpacking (which can be somewhat computationally expensive) and so should be strongly
     * preferred when finding a region of one packed array that is not contained in another packed array.
     * @param original A packed array such as one produced by pack()
     * @param x The x position at which to insert the "on" cell
     * @param y The y position at which to insert the "on" cell
     * @return A packed array that encodes "on" for all cells that are "on" in original or correspond to x,y
     */
    public static short[] insertPacked(short[] original, int x, int y)
    {
        return unionPacked(original, new short[]{(short)posToHilbert(x, y), 1});
    }

    /**
     * Given one packed short array, original, and a number of Hilbert Curve indices, hilbert, this produces a packed
     * short array that encodes "on" for any cell that was "on" in original, always encodes "on" for the position
     * referred to by any element of hilbert, and encodes "off" for cells that were "off" in original and are not in any
     * cell hilbert refers to. This method does not do any unpacking (which can be somewhat computationally expensive)
     * and so should be strongly preferred when you have several Hilbert Curve indices, possibly nearby each other but
     * just as possibly not, that you need inserted into a packed array.
     * <br>
     *     NOTE: this may not produce an optimally packed result, though the difference in memory consumption is likely
     *     to be exceedingly small unless there are many nearby elements in hilbert (which may be a better use case for
     *     unionPacked() anyway).
     * @param original A packed array such as one produced by pack()
     * @param hilbert an array or vararg of Hilbert Curve indices that should be inserted into the result
     * @return A packed array that encodes "on" for all cells that are "on" in original or are contained in hilbert
     */
    public static short[] insertSeveralPacked(short[] original, int... hilbert)
    {
        return unionPacked(original, packSeveral(hilbert));
    }
    /**
     * Given one packed short array, original, and a number of Coords, points, this produces a packed
     * short array that encodes "on" for any cell that was "on" in original, always encodes "on" for the position
     * referred to by any element of points, and encodes "off" for cells that were "off" in original and are not in any
     * cell points refers to. This method does not do any unpacking (which can be somewhat computationally expensive)
     * and so should be strongly preferred when you have several Coords, possibly nearby each other but
     * just as possibly not, that you need inserted into a packed array.
     * <br>
     *     NOTE: this may not produce an optimally packed result, though the difference in memory consumption is likely
     *     to be exceedingly small unless there are many nearby elements in hilbert (which may be a better use case for
     *     unionPacked() anyway).
     * @param original A packed array such as one produced by pack()
     * @param points an array or vararg of Coords that should be inserted into the result
     * @return A packed array that encodes "on" for all cells that are "on" in original or are contained in hilbert
     */
    public static short[] insertSeveralPacked(short[] original, Coord... points)
    {
        return unionPacked(original, packSeveral(points));
    }
    /**
     * Given one packed short array, original, and a Hilbert Curve index, hilbert, this produces a packed short array
     * that encodes "on" for any cell that was "on" in original, unless it was the position referred to by hilbert, and
     * encodes "off" for cells that were "off" in original or are the cell hilbert refers to.
     * This method does not do any unpacking (which can be somewhat computationally expensive) and so should be strongly
     * preferred when finding a region of one packed array that is not contained in another packed array.
     * @param original A packed array such as one produced by pack()
     * @param hilbert A Hilbert Curve index that should be removed from the result
     * @return A packed array that encodes "on" for all cells that are "on" in original and don't correspond to hilbert
     */
    public static short[] removePacked(short[] original, short hilbert)
    {
        return differencePacked(original, new short[]{hilbert, 1});
    }
    /**
     * Given one packed short array, original, and a position as x,y numbers, this produces a packed short array that
     * encodes "on" for any cell that was "on" in original, unless it was the position referred to by x and y, and
     * encodes "off" for cells that were "off" in original or are the cell x and y refer to.
     * This method does not do any unpacking (which can be somewhat computationally expensive) and so should be strongly
     * preferred when finding a region of one packed array that is not contained in another packed array.
     * @param original A packed array such as one produced by pack()
     * @param x The x position at which to remove any "on" cell
     * @param y The y position at which to remove any "on" cell
     * @return A packed array that encodes "on" for all cells that are "on" in original and don't correspond to x,y
     */
    public static short[] removePacked(short[] original, int x, int y)
    {
        int dist = posToHilbert(x, y);
        return differencePacked(original, new short[]{(short)dist, 1});
    }

    /**
     * Given one packed short array, original, and a number of Hilbert Curve indices, hilbert, this produces a packed
     * short array that encodes "on" for any cell that was "on" in original, unless it was a position referred to by
     * hilbert, and encodes "off" for cells that were "off" in original and are a cell hilbert refers to. This method
     * does not do any unpacking (which can be somewhat computationally expensive) and so should be strongly preferred
     * when you have several Hilbert Curve indices, possibly nearby each other but just as possibly not, that you need
     * removed from a packed array.
     * <br>
     *     NOTE: this may not produce an optimally packed result, though the difference in memory consumption is likely
     *     to be exceedingly small unless there are many nearby elements in hilbert (which may be a better use case for
     *     differencePacked() anyway).
     * @param original A packed array such as one produced by pack()
     * @param hilbert an array or vararg of Hilbert Curve indices that should be inserted into the result
     * @return A packed array that encodes "on" for all cells that are "on" in original and aren't contained in hilbert
     */
    public static short[] removeSeveralPacked(short[] original, int... hilbert)
    {
        return differencePacked(original, packSeveral(hilbert));
    }

    /**
     * Given one packed short array, original, and a number of Hilbert Curve indices, hilbert, this produces a packed
     * short array that encodes "on" for any cell that was "on" in original, unless it was a position referred to by
     * hilbert, and encodes "off" for cells that were "off" in original and are a cell hilbert refers to. This method
     * does not do any unpacking (which can be somewhat computationally expensive) and so should be strongly preferred
     * when you have several Hilbert Curve indices, possibly nearby each other but just as possibly not, that you need
     * removed from a packed array.
     * <br>
     *     NOTE: this may not produce an optimally packed result, though the difference in memory consumption is likely
     *     to be exceedingly small unless there are many nearby elements in hilbert (which may be a better use case for
     *     differencePacked() anyway).
     * @param original A packed array such as one produced by pack()
     * @param points an array or vararg of Coords that should be inserted into the result
     * @return A packed array that encodes "on" for all cells that are "on" in original and aren't contained in points
     */
    public static short[] removeSeveralPacked(short[] original, Coord... points)
    {
        return differencePacked(original, packSeveral(points));
    }

    /**
     * Gets a random subset of positions that are "on" in the given packed array, without unpacking it, and returns
     * them as a Coord[]. Random numbers are generated by the rng parameter.
     * @param packed a short[] returned by pack() or one of the sub-arrays in what is returned by packMulti(); must
     *               not be null (this method does not check).
     * @param fraction the likelihood to return one of the "on" cells, from 0.0 to 1.0
     * @param rng the random number generator used to decide random factors.
     * @return a Coord[], ordered by distance along the Hilbert Curve, corresponding to a random section of "on" cells
     * in packed that has a random length approximately equal to the count of all "on" cells in packed times fraction.
     */
    public static Coord[] randomSample(short[] packed, double fraction, RNG rng)
    {
        int counted = count(packed);
        ShortVLA vla = new ShortVLA((int)(counted * fraction) + 1);
        boolean on = false;
        int idx = 0;
        for(int p = 0; p < packed.length; p++, on = !on) {
            if (on) {
                for (int i = idx; i < idx + (packed[p] & 0xffff); i++) {
                    if(rng.nextDouble() < fraction)
                        vla.add((short)i);
                }
            }
            idx += packed[p] & 0xffff;
        }
        int[] distances = vla.asInts();
        Coord[] cs = new Coord[distances.length];
        for (int i = 0; i < distances.length; i++) {
            cs[i] = Coord.get(hilbertX[distances[i]], hilbertY[distances[i]]);
        }
        return cs;
    }
    /**
     * Gets a single randomly chosen position that is "on" in the given packed array, without unpacking it, and returns
     * it as a Coord or returns null of the array is empty. Random numbers are generated by the rng parameter.
     * More efficient in most cases than randomSample(), and will always return at least one Coord for non-empty arrays.
     * @param packed a short[] returned by pack() or one of the sub-arrays in what is returned by packMulti(); must
     *               not be null (this method does not check).
     * @param rng the random number generator used to decide random factors
     * @return a Coord corresponding to a random "on" cell in packed
     */
    public static Coord singleRandom(short[] packed, RNG rng)
    {
        int counted = count(packed);
        if(counted == 0)
            return null;
        int r = rng.nextInt(counted);
        int c = 0, idx = 0;
        boolean on = false;
        for (int i = 0; i < packed.length; on = !on, idx += packed[i] & 0xFFFF, i++) {
            if (on) {
                if(c + (packed[i] & 0xFFFF) > r)
                {
                    idx += r - c;
                    return Coord.get(hilbertX[idx], hilbertY[idx]);
                }
                c += packed[i] & 0xFFFF;
            }
        }
        return null;

    }

    /**
     * Gets a fixed number of randomly chosen positions that are "on" in the given packed array, without unpacking it,
     * and returns a List of Coord with a count equal to size (or less if there aren't enough "on" cells). Random
     * numbers are generated by the rng parameter. This orders the returned array in the order the Hilbert Curve takes,
     * and you may want to call RNG.shuffle() with it as a parameter to randomize the order.
     *
     * @param packed a short[] returned by pack() or one of the sub-arrays in what is returned by packMulti(); must
     *               not be null (this method does not check).
     * @param size the desired size of the List to return; may be smaller if there aren't enough elements
     * @param rng the random number generator used to decide random factors.
     * @return a List of Coords, ordered by distance along the Hilbert Curve, corresponding to randomly "on" cells in
     * packed, with a length equal to the smaller of size and the count of all "on" cells in packed
     */
    public static ArrayList<Coord> randomPortion(short[] packed, int size, RNG rng)
    {
        int counted = count(packed);
        ArrayList<Coord> coords = new ArrayList<Coord>(Math.min(counted, size));
        if(counted == 0 || size == 0)
            return coords;
        int[] data = rng.randomRange(0, counted, Math.min(counted, size));
        Arrays.sort(data);
        int r = data[0];
        int c = 0, idx = 0;
        boolean on = false;
        for (int i = 0, ri = 0; i < packed.length; on = !on, idx += packed[i] & 0xffff, i++) {
            if (on) {
                while (c + (packed[i] & 0xffff) > r)
                {
                    int n = idx + r - c;
                    coords.add(Coord.get(hilbertX[n], hilbertY[n]));
                    if(++ri < data.length)
                        r = data[ri];
                    else
                        return coords;
                }
                c += packed[i] & 0xffff;
            }
        }
        return coords;
    }

    /**
     * Quick utility method for printing packed data as a grid of 1 (on) and/or 0 (off). Useful primarily for debugging.
     * @param packed a packed short[] such as one produced by pack()
     * @param width the width of the packed 2D array
     * @param height the height of the packed 2D array
     */
    public static void printPacked(short[] packed, int width, int height)
    {
        boolean[][] unpacked = unpack(packed, width, height);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                System.out.print(unpacked[x][y] ? '1' : '0');
            }
            System.out.println();
        }
    }

    public static void printCompressedData(short[] packed)
    {
        if(packed == null || packed.length == 0)
        {
            System.out.println("[]");
            return;
        }
        System.out.print("[" + packed[0]);
        for (int i = 1; i < packed.length; i++) {
            System.out.print(", " + packed[i]);
        }
        System.out.println("]");
    }

    /**
     * Encodes a short array of packed data as a (larger, more memory-hungry) ASCII string, which can be decoded using
     * RegionPacker.decodeASCII() . Uses 64 printable chars, from ';' (ASCII 59) to 'z' (ASCII 122).
     * @param packed a packed data item produced by pack() or some other method from this class.
     * @return a printable String, which can be decoded with RegionPacker.decodeASCII()
     */
    public static String encodeASCII(short[] packed)
    {
        int len = packed.length * 3;
        char[] chars = new char[len];
        for (int i = 0, c = 0; c < len; i++, c += 3) {
            chars[c] = (char)((packed[i] & 31) + 59);
            chars[c+1] = (char)(((packed[i] >> 5) & 31) + 59);
            chars[c+2] = (char)(((packed[i] >>> 10) & 63) + 59);
        }
        return new String(chars);
    }
    /**
     * Given a String specifically produced by RegionPacker.encodeASCII(), this will produce a packed data array.
     * @param text a String produced by RegionPacker.encodeASCII(); this will almost certainly fail on other strings.
     * @return the packed data as a short array that was originally used to encode text
     */
    public static short[] decodeASCII(String text)
    {
        int len = text.length();
        if(len % 3 != 0)
            return ALL_WALL;
        char[] chars = text.toCharArray();
        short[] packed = new short[len / 3];
        for (int c = 0, i = 0; c < len; i++, c += 3) {
            packed[i] = (short)(((chars[c] - 59) & 31) | (((chars[c+1] - 59) & 31) << 5) | (((chars[c+2] - 59) & 63) << 10));
        }
        return packed;
    }


    /**
     * Encode a number n as a Gray code; Gray codes have a relation to the Hilbert curve and may be useful.
     * Source: http://xn--2-umb.com/15/hilbert , http://aggregate.org/MAGIC/#Gray%20Code%20Conversion
     * @param n any int
     * @return the gray code for n
     */
    public static int grayEncode(int n){
        return n ^ (n >> 1);
    }

    /**
     * Decode a number from a Gray code n; Gray codes have a relation to the Hilbert curve and may be useful.
     * Source: http://xn--2-umb.com/15/hilbert , http://aggregate.org/MAGIC/#Gray%20Code%20Conversion
     * @param n a gray code, as produced by grayEncode
     * @return the decoded int
     */
    public static int grayDecode(int n) {
        int p = n;
        while ((n >>= 1) != 0)
            p ^= n;
        return p;
    }

    /**
     * Not currently used, may be used in the future.
     * Source: https://www.cs.dal.ca/research/techreports/cs-2006-07 ; algorithm provided in pseudocode
     * @param n any int
     * @param mask a bitmask that has some significance to the compacting algorithm
     * @param i i is I have no clue
     * @return some kind of magic
     */
    public static int grayCodeRank(int n, int mask, int i)
    {
        int r = 0;
        for (int k = n - 1; k >= 0; k--)
        {
            if(((mask >> k) & 1) == 1)
                r = (r << 1) | ((i >> k) & 1);
        }
        return  r;
    }

    /**
     *
     * Source: https://www.cs.dal.ca/research/techreports/cs-2006-07 ; algorithm provided in pseudocode
     * @param n a gray code, I think
     * @param mask some bitmask or something? check the paper
     * @param altMask another bitmask I guess, again, check the paper
     * @param rank if I had to wager a guess, this is something about rank
     * @return some other kind of magic
     */
    public static int grayCodeRankInverse(int n, int mask, int altMask, int rank)
    {
        int i = 0, g = 0, j = Integer.bitCount(mask) - 1;
        for(int k = n - 1; k >= 0; k--)
        {
            if(((mask >> k) & 1) == 1)
            {
                i ^= (-((rank >> j) & 1) ^ i) & (1 << k);
                g ^= (-((((i >> k) & 1) + ((i >> k) & 1)) % 2) ^ g) & (1 << k);
                --j;
            }
            else
            {
                g ^= (-((altMask >> k) & 1) ^ g) & (1 << k);
                i ^= (-((((g >> k) & 1) + ((i >> (k+1)) & 1)) % 2) ^ i) & (1 << k);
            }
        }
        return  i;
    }

    /**
     * Takes an x, y position and returns the length to travel along the 256x256 Hilbert curve to reach that position.
     * This assumes x and y are between 0 and 255, inclusive.
     * This uses a lookup table for the 256x256 Hilbert Curve, which should make it faster than calculating the
     * distance along the Hilbert Curve repeatedly.
     * Source: http://and-what-happened.blogspot.com/2011/08/fast-2d-and-3d-hilbert-curves-and.html
     * @param x between 0 and 255 inclusive
     * @param y between 0 and 255 inclusive
     * @return the distance to travel along the 256x256 Hilbert Curve to get to the given x, y point.
     */
    public static int posToHilbert( final int x, final int y ) {
        //int dist = posToHilbertNoLUT(x, y);
        //return dist;
        return hilbertDistances[x + (y << 8)] & 0xffff;
    }
    /**
     * Takes an x, y, z position and returns the length to travel along the 16x16x16 Hilbert curve to reach that
     * position. This assumes x, y, and z are between 0 and 15, inclusive.
     * This uses a lookup table for the 16x16x16 Hilbert Curve, which should make it faster than calculating the
     * distance along the Hilbert Curve repeatedly.
     * Source: http://and-what-happened.blogspot.com/2011/08/fast-2d-and-3d-hilbert-curves-and.html
     * @param x between 0 and 15 inclusive
     * @param y between 0 and 15 inclusive
     * @param z between 0 and 15 inclusive
     * @return the distance to travel along the 32x32x32 Hilbert Curve to get to the given x, y, z point.
     */
    public static int posToHilbert3D( final int x, final int y, final int z ) {
        return hilbert3Distances[x + (y << 4) + (z << 8)];
    }
    /**
     * Takes an x, y position and returns the length to travel along the 16x16 Moore curve to reach that position.
     * This assumes x and y are between 0 and 15, inclusive.
     * This uses a lookup table for the 16x16 Moore Curve, which should make it faster than calculating the
     * distance along the Moore Curve repeatedly.
     * @param x between 0 and 15 inclusive
     * @param y between 0 and 15 inclusive
     * @return the distance to travel along the 16x16 Moore Curve to get to the given x, y point.
     */
    public static int posToMoore( final int x, final int y ) {
        return mooreDistances[x + (y << 4)] & 0xff;
    }
    /*
     * Takes an x, y position and returns the length to travel along the 256x256 Hilbert curve to reach that position.
     * This assumes x and y are between 0 and 255, inclusive.
     * Source: http://and-what-happened.blogspot.com/2011/08/fast-2d-and-3d-hilbert-curves-and.html
     * @param x between 0 and 255 inclusive
     * @param y between 0 and 255 inclusive
     * @return the distance to travel along the 256x256 Hilbert Curve to get to the given x, y point.
     */

    private static int posToHilbertNoLUT( final int x, final int y )
    {
        int hilbert = 0, remap = 0xb4, mcode, hcode;
        /*
        while( block > 0 )
        {
            --block;
            mcode = ( ( x >> block ) & 1 ) | ( ( ( y >> ( block ) ) & 1 ) << 1);
            hcode = ( ( remap >> ( mcode << 1 ) ) & 3 );
            remap ^= ( 0x82000028 >> ( hcode << 3 ) );
            hilbert = ( ( hilbert << 2 ) + hcode );
        }
         */

        mcode = ( ( x >> 7 ) & 1 ) | ( ( ( y >> ( 7 ) ) & 1 ) << 1);
        hcode = ( ( remap >> ( mcode << 1 ) ) & 3 );
        remap ^= ( 0x82000028 >> ( hcode << 3 ) );
        hilbert = ( ( hilbert << 2 ) + hcode );

        mcode = ( ( x >> 6 ) & 1 ) | ( ( ( y >> ( 6 ) ) & 1 ) << 1);
        hcode = ( ( remap >> ( mcode << 1 ) ) & 3 );
        remap ^= ( 0x82000028 >> ( hcode << 3 ) );
        hilbert = ( ( hilbert << 2 ) + hcode );

        mcode = ( ( x >> 5 ) & 1 ) | ( ( ( y >> ( 5 ) ) & 1 ) << 1);
        hcode = ( ( remap >> ( mcode << 1 ) ) & 3 );
        remap ^= ( 0x82000028 >> ( hcode << 3 ) );
        hilbert = ( ( hilbert << 2 ) + hcode );

        mcode = ( ( x >> 4 ) & 1 ) | ( ( ( y >> ( 4 ) ) & 1 ) << 1);
        hcode = ( ( remap >> ( mcode << 1 ) ) & 3 );
        remap ^= ( 0x82000028 >> ( hcode << 3 ) );
        hilbert = ( ( hilbert << 2 ) + hcode );

        mcode = ( ( x >> 3 ) & 1 ) | ( ( ( y >> ( 3 ) ) & 1 ) << 1);
        hcode = ( ( remap >> ( mcode << 1 ) ) & 3 );
        remap ^= ( 0x82000028 >> ( hcode << 3 ) );
        hilbert = ( ( hilbert << 2 ) + hcode );

        mcode = ( ( x >> 2 ) & 1 ) | ( ( ( y >> ( 2 ) ) & 1 ) << 1);
        hcode = ( ( remap >> ( mcode << 1 ) ) & 3 );
        remap ^= ( 0x82000028 >> ( hcode << 3 ) );
        hilbert = ( ( hilbert << 2 ) + hcode );

        mcode = ( ( x >> 1 ) & 1 ) | ( ( ( y >> ( 1 ) ) & 1 ) << 1);
        hcode = ( ( remap >> ( mcode << 1 ) ) & 3 );
        remap ^= ( 0x82000028 >> ( hcode << 3 ) );
        hilbert = ( ( hilbert << 2 ) + hcode );

        mcode = ( x & 1 ) | ( ( y & 1 ) << 1);
        hcode = ( ( remap >> ( mcode << 1 ) ) & 3 );

        hilbert = ( ( hilbert << 2 ) + hcode );

        return hilbert;
    }

    /**
     * Takes a position as a Morton code, with interleaved x and y bits and x in the least significant bit, and returns
     * the length to travel along the 256x256 Hilbert Curve to reach that position.
     * This uses 16 bits of the Morton code and requires that the code is non-negative.
     * Source: http://and-what-happened.blogspot.com/2011/08/fast-2d-and-3d-hilbert-curves-and.html
     * @param morton a Morton code that interleaves two 8-bit unsigned numbers, with x as index1 and y as index2.
     * @return a distance to travel down the Hilbert Curve to reach the location that can be decoded from morton.
     */
    public static int mortonToHilbert( final int morton )
    {
        int hilbert = 0;
        int remap = 0xb4;
        int block = BITS;
        while( block > 0 )
        {
            block -= 2;
            int mcode = ( ( morton >> block ) & 3 );
            int hcode = ( ( remap >> ( mcode << 1 ) ) & 3 );
            remap ^= ( 0x82000028 >> ( hcode << 3 ) );
            hilbert = ( ( hilbert << 2 ) + hcode );
        }
        return hilbert;
    }

    /**
     * Takes a distance to travel along the 256x256 Hilbert curve and returns a Morton code representing the position
     * in 2D space that corresponds to that point on the Hilbert Curve; the Morton code will have interleaved x and y
     * bits and x in the least significant bit. This uses a lookup table for the 256x256 Hilbert curve, which should
     * make it faster than calculating the position repeatedly.
     * The parameter hilbert is an int but only 16 unsigned bits are used.
     * @param hilbert a distance to travel down the Hilbert Curve
     * @return a Morton code that stores x and y interleaved; can be converted to a Coord with other methods.
     */

    public static int hilbertToMorton( final int hilbert )
    {
        return mortonEncode(hilbertX[hilbert], hilbertY[hilbert]);
    }

    /**
     * Takes a distance to travel along the 256x256 Hilbert curve and returns a Coord representing the position
     * in 2D space that corresponds to that point on the Hilbert curve. This uses a lookup table for the
     * 256x256 Hilbert curve, which should make it faster than calculating the position repeatedly.
     * The parameter hilbert is an int but only 16 unsigned bits are used.
     * @param hilbert a distance to travel down the Hilbert Curve
     * @return a Coord corresponding to the position in 2D space at the given distance down the Hilbert Curve
     */
    public static Coord hilbertToCoord( final int hilbert )
    {
        return Coord.get(hilbertX[hilbert], hilbertY[hilbert]);
    }

    /**
     * Takes a distance to travel along the 16x16 Hilbert curve and returns a Coord representing the position
     * in 2D space that corresponds to that point on the Hilbert curve. This uses a lookup table for the
     * 16x16 Hilbert curve, which should make it faster than calculating the position repeatedly.
     * The parameter moore is an int but only 8 unsigned bits are used, and since the Moore Curve loops, it is
     * calculated as {@code moore % 256}.
     * @param moore a distance to travel down the Moore Curve
     * @return a Coord corresponding to the position in 2D space at the given distance down the Hilbert Curve
     */
    public static Coord mooreToCoord( final int moore )
    {
        return Coord.get(mooreX[moore % 256], mooreY[moore % 256]);
    }


    /*
     * Takes a distance to travel along the 256x256 Hilbert curve and returns a Morton code representing the position
     * in 2D space that corresponds to that point on the Hilbert curve; the Morton code will have interleaved x and y
     * bits and x in the least significant bit. This variant does not use a lookup table, and is likely slower.
     * The parameter hilbert is an int but only 16 unsigned bits are used.
     * @param hilbert
     * @return
     */
    /*
    public static int hilbertToMortonNoLUT( final int hilbert )
    {
        int morton = 0;
        int remap = 0xb4;
        int block = BITS;
        while( block > 0 )
        {
            block -= 2;
            int hcode = ( ( hilbert >> block ) & 3 );
            int mcode = ( ( remap >> ( hcode << 1 ) ) & 3 );
            remap ^= ( 0x330000cc >> ( hcode << 3 ) );
            morton = ( ( morton << 2 ) + mcode );
        }
        return morton;
    }
    */

    /**
     * Takes a distance to travel along the 256x256 Hilbert curve and returns a Coord representing the position
     * in 2D space that corresponds to that point on the Hilbert curve. This variant does not use a lookup table,
     * and is likely slower.
     * The parameter hilbert is an int but only 16 unsigned bits are used.
     * @param hilbert
     * @return
     */
    private static Coord hilbertToCoordNoLUT( final int hilbert )
    {
        int x = 0, y = 0;
        int remap = 0xb4;
        int block = BITS;
        while( block > 0 )
        {
            block -= 2;
            int hcode = ( ( hilbert >> block ) & 3 );
            int mcode = ( ( remap >> ( hcode << 1 ) ) & 3 );
            remap ^= ( 0x330000cc >> ( hcode << 3 ) );
            x = (x << 1) + (mcode & 1);
            y = (y << 1) + ((mcode & 2) >> 1);
        }
        return Coord.get(x, y);
    }

    /**
     * Takes a position as a Coord called pt and returns the length to travel along the 256x256 Hilbert curve to reach
     * that position.
     * This assumes pt.x and pt.y are between 0 and 255, inclusive.
     * Source: http://and-what-happened.blogspot.com/2011/08/fast-2d-and-3d-hilbert-curves-and.html
     * @param pt a Coord with values between 0 and 255, inclusive
     * @return a distance from the start of the 256x256 Hilbert curve to get to the position of pt
     */
    public static int coordToHilbert(final Coord pt)
    {
        return posToHilbert(pt.x, pt.y);
    }

    /**
     * Takes a position as a Coord called pt and returns the length to travel along the 16x16 Moore curve to reach
     * that position.
     * This assumes pt.x and pt.y are between 0 and 15, inclusive.
     * @param pt a Coord with values between 0 and 15, inclusive
     * @return a distance from the "start" of the 16x16 Moore curve to get to the position of pt
     */
    public static int coordToMoore(final Coord pt)
    {
        return posToMoore(pt.x, pt.y);
    }

    private static int mortonEncode3D( int index1, int index2, int index3 )
    { // pack 3 5-bit indices into a 15-bit Morton code
        index1 &= 0x0000001f;
        index2 &= 0x0000001f;
        index3 &= 0x0000001f;
        index1 *= 0x01041041;
        index2 *= 0x01041041;
        index3 *= 0x01041041;
        index1 &= 0x10204081;
        index2 &= 0x10204081;
        index3 &= 0x10204081;
        index1 *= 0x00011111;
        index2 *= 0x00011111;
        index3 *= 0x00011111;
        index1 &= 0x12490000;
        index2 &= 0x12490000;
        index3 &= 0x12490000;
        return( ( index1 >> 16 ) | ( index2 >> 15 ) | ( index3 >> 14 ) );
    }
    private static void computeHilbert3D(int x, int y, int z)
    {
        int hilbert = mortonEncode3D(x, y, z);
        int block = 9;
        int hcode = ( ( hilbert >> block ) & 7 );
        int mcode, shift, signs;
        shift = signs = 0;
        while( block > 0 )
        {
            block -= 3;
            hcode <<= 2;
            mcode = ( ( 0x20212021 >> hcode ) & 3 );
            shift = ( ( 0x48 >> ( 7 - shift - mcode ) ) & 3 );
            signs = ( ( signs | ( signs << 3 ) ) >> mcode );
            signs = ( ( signs ^ ( 0x53560300 >> hcode ) ) & 7 );
            mcode = ( ( hilbert >> block ) & 7 );
            hcode = mcode;
            hcode = ( ( ( hcode | ( hcode << 3 ) ) >> shift ) & 7 );
            hcode ^= signs;
            hilbert ^= ( ( mcode ^ hcode ) << block );
        }

        hilbert ^= ( ( hilbert >> 1 ) & 0x92492492 );
        hilbert ^= ( ( hilbert & 0x92492492 ) >> 1 );

        hilbert3X[hilbert] = (short)x;
        hilbert3Y[hilbert] = (short)y;
        hilbert3Z[hilbert] = (short)z;
        hilbert3Distances[x + (y << 4) + (z << 8)] = (short)hilbert;
    }
    private static int nextGray(int gray)
    {
        return gray ^ (((Integer.bitCount(gray) & 1) == 1) ? (Integer.highestOneBit(gray & (-gray)) << 1) : 1);
    }
    private static int grayAxis(int gray)
    {
        // yes, that's octal. it occasionally is useful?
        if((gray & 0444444444) > 0)
            return 0; //x
        else if((gray & 0222222222) > 0)
            return 1; //y
        return 2; //z
    }

    private static int getDim(int element, int x, int y, int z) {
        switch (element) {
            case 0:
                return x;
            case 1:
                return y;
            default:
                return z;
        }
    }
    private static int[] g_mask = new int[]{4,2,1};

    /**
     * See http://www.dcs.bbk.ac.uk/TriStarp/pubs/JL1_00.pdf
     * @param i ???
     * @param x ????
     * @param y ?????
     * @param z ??????
     * @return ???????????????????????
     */
    private static int calc_P (int i, int x, int y, int z)
    {
        int element= i / 0x3;
        int P, temp1, temp2;
        P = getDim(element, x, y, z);
        if (i % 0x3 > 0x3 - 03)
        {
            temp1 = P = getDim(element + 1, x, y, z);
            P >>= i % 0x3;
            temp1 <<= 0x3 - i % 0x3;
            P |= temp1;
        }
        else
            P >>= i % 0x3; /* P is a 03 bit hcode */
        return P;
    }
    private static int calc_J (int P)
    {
        int i = 0;
        int J = 03;
        for (i = 1; i < 03; i++) {
            if ((P >> i & 1) != (P & 1))
                break;
        }
        if (i != 03)
            J -= i;
        return J;
    }
    private static int calc_T (int P)
    {
        if (P < 3)
            return 0;
        if (P % 2 > 0)
            return (P - 1) ^ (P - 1) / 2;
        return (P - 2) ^ (P - 2) / 2;
    }
    private  static int calc_tS_tT(int xJ, int val)
    {
        int retval = val, temp1, temp2;
        if (xJ % 03 != 0)
        {
            temp1 = val >> xJ % 03;
            temp2 = val << 03 - xJ % 03;
            retval = temp1 | temp2;
            retval &= ((int)1 << 03) - 1;
        }
        return retval;
    }

    int H_encode(int x, int y, int z) {
        int mask = (int) 1 << 0x3 - 1, W = 0, P = 0, h = 0, i = 0x3 * 03 - 03,
                A, S, tS, T, tT, J, xJ, j;
        for (j = A = 0; j < 03; j++)
            if ((getDim(j, x, y, z) & mask) > 0)
                A |= g_mask[j];
        S = A;
        P = grayEncode(S); // gray code
        h |= P << i;
        J = calc_J(P);
        xJ = J - 1;
        T = calc_T(P);
        tT = T;
        for (i -= 03, mask >>= 1; i >= 0; i -= 03, mask >>= 1) {
            for (j = A = 0; j < 03; j++) {
                if ((getDim(j, x, y, z) & mask) > 0)
                    A |= g_mask[j];
            }
            W ^= tT;
            tS = A ^ W;
            S = calc_tS_tT(xJ, tS);
            P = grayEncode(S);

            h |= P << i;

            if (i > 0) {
                T = calc_T(P);
                tT = calc_tS_tT(xJ, T);
                J = calc_J(P);
                xJ += J - 1;
            }
        }
        return h;
    }


    private static void computePukaHilbert3D() {
        for (int h = 0, p = 0; h < 0x1000; h += 8, p += 125) {
            int startX = hilbert3X[h], startY = hilbert3Y[h], startZ = hilbert3Z[h],
                    endX = hilbert3X[h+7], endY = hilbert3Y[h+7], endZ = hilbert3Z[h+7],
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
                ph3X[p + i] = x = (byte)(pukaRotations[direction * 4 + rotation][0][i] + bottomX * 5);
                ph3Y[p + i] = y = (byte)(pukaRotations[direction * 4 + rotation][1][i] + bottomY * 5);
                ph3Z[p + i] = z = (byte)(pukaRotations[direction * 4 + rotation][2][i] + bottomZ * 5);
                ph3Distances[x + y * 40 + z * 1600] = (short)(p + i);
            }
        }
    }

    /**
     * Gets the x coordinate for a given index into the 16x16x(8*n) Moore curve. Expects indices to touch the following
     * corners of the 16x16x(8*n) cube in this order, using x,y,z syntax:
     * (0,0,0) (0,0,(8*n)) (0,16,(8*n)) (0,16,0) (16,16,0) (16,16,(8*n)) (16,0,(8*n)) (16,0,0)
     * @param index the index into the 3D 16x16x(8*n) Moore Curve, must be less than 0x1000
     * @param n the number of 8-deep layers to use as part of the box shape this travels through
     * @return the x coordinate of the given distance traveled through the 3D 16x16x(8*n) Moore Curve
     */
    public static int getXMoore3D(final int index, final int n) {
        int hilbert = index & 0x1ff;
        int sector = index >> 9;
        if (sector < 2 * n)
            return 7 - hilbert3X[hilbert];
        else
            return 8 + hilbert3X[hilbert];
    }

    /**
     * Gets the y coordinate for a given index into the 16x16x(8*n) Moore curve. Expects indices to touch the following
     * corners of the 16x16x(8*n) cube in this order, using x,y,z syntax:
     * (0,0,0) (0,0,(8*n)) (0,16,(8*n)) (0,16,0) (16,16,0) (16,16,(8*n)) (16,0,(8*n)) (16,0,0)
     * @param index the index into the 3D 16x16x(8*n) Moore Curve, must be less than 0x1000
     * @param n the number of 8-deep layers to use as part of the box shape this travels through
     * @return the y coordinate of the given distance traveled through the 3D 16x16x(8*n) Moore Curve
     */
    public static int getYMoore3D(final int index, final int n)
    {
        int hilbert = index & 0x1ff;
        int sector = index >> 9;
        if (sector < n || sector >= 3 * n)
            return 7 - hilbert3Y[hilbert];
        else
            return 8 + hilbert3Y[hilbert];

    }
    /**
     * Gets the z coordinate for a given index into the 16x16x(8*n) Moore curve. Expects indices to touch the following
     * corners of the 16x16x(8*n) cube in this order, using x,y,z syntax:
     * (0,0,0) (0,0,(8*n)) (0,16,(8*n)) (0,16,0) (16,16,0) (16,16,(8*n)) (16,0,(8*n)) (16,0,0)
     * @param index the index into the 3D 16x16x(8*n) Moore Curve, must be less than 0x1000
     * @param n the number of 8-deep layers to use as part of the box shape this travels through
     * @return the z coordinate of the given distance traveled through the 3D 16x16x(8*n) Moore Curve
     */
    public static int getZMoore3D(final int index, final int n) {
        int hilbert = index & 0x1ff;
        int sector = index >> 9;
        if (sector / n < 2)
            return hilbert3Z[hilbert] + 8 * (sector % n);
        else
            return (8 * n - 1) - hilbert3Z[hilbert] - 8 * (sector % n);
    }



    /**
     * Takes two 8-bit unsigned integers index1 and index2, and returns a Morton code, with interleaved index1 and
     * index2 bits and index1 in the least significant bit. With this method, index1 and index2 can have up to 8 bits.
     * This returns a 16-bit Morton code and WILL encode information in the sign bit if the inputs are large enough.
     * Source: http://and-what-happened.blogspot.com/2011/08/fast-2d-and-3d-hilbert-curves-and.html
     * @param index1 a non-negative integer using at most 8 bits, to be placed in the "x" slots
     * @param index2 a non-negative integer using at most 8 bits, to be placed in the "y" slots
     * @return a Morton code/Z-Code that interleaves the two numbers into one 16-bit short
     */
    public static short zEncode(short index1, short index2)
    { // pack 2 8-bit (unsigned) indices into a 16-bit (signed...) Morton code/Z-Code
        index1 &= 0x000000ff;
        index2 &= 0x000000ff;
        index1 |= ( index1 << 4 );
        index2 |= ( index2 << 4 );
        index1 &= 0x00000f0f;
        index2 &= 0x00000f0f;
        index1 |= ( index1 << 2 );
        index2 |= ( index2 << 2 );
        index1 &= 0x00003333;
        index2 &= 0x00003333;
        index1 |= ( index1 << 1 );
        index2 |= ( index2 << 1 );
        index1 &= 0x00005555;
        index2 &= 0x00005555;
        return (short)(index1 | ( index2 << 1 ));
    }
    /**
     * Takes two 8-bit unsigned integers index1 and index2, and returns a Morton code, with interleaved index1 and
     * index2 bits and index1 in the least significant bit. With this method, index1 and index2 can have up to 8 bits.
     * This returns a 32-bit Morton code but only uses 16 bits, and will not encode information in the sign bit.
     * Source: http://and-what-happened.blogspot.com/2011/08/fast-2d-and-3d-hilbert-curves-and.html
     * @param index1 a non-negative integer using at most 8 bits, to be placed in the "x" slots
     * @param index2 a non-negative integer using at most 8 bits, to be placed in the "y" slots
     * @return a Morton code that interleaves the two numbers as one 32-bit int, but only in 16 bits of it
     */
    public static int mortonEncode(int index1, int index2)
    { // pack 2 8-bit (unsigned) indices into a 32-bit (signed...) Morton code
        index1 &= 0x000000ff;
        index2 &= 0x000000ff;
        index1 |= ( index1 << 4 );
        index2 |= ( index2 << 4 );
        index1 &= 0x00000f0f;
        index2 &= 0x00000f0f;
        index1 |= ( index1 << 2 );
        index2 |= ( index2 << 2 );
        index1 &= 0x00003333;
        index2 &= 0x00003333;
        index1 |= ( index1 << 1 );
        index2 |= ( index2 << 1 );
        index1 &= 0x00005555;
        index2 &= 0x00005555;
        return index1 | ( index2 << 1 );
    }
    /**
     * Takes two 16-bit unsigned integers index1 and index2, and returns a Morton code, with interleaved index1 and
     * index2 bits and index1 in the least significant bit. With this method, index1 and index2 can have up to 16 bits.
     * This returns a 32-bit Morton code and may encode information in the sign bit.
     * Source: http://and-what-happened.blogspot.com/2011/08/fast-2d-and-3d-hilbert-curves-and.html
     * @param index1 a non-negative integer using at most 16 bits, to be placed in the "x" slots
     * @param index2 a non-negative integer using at most 16 bits, to be placed in the "y" slots
     * @return a Morton code that interleaves the two numbers as one 32-bit int
     */
    public static int mortonEncode16(int index1, int index2)
    { // pack 2 16-bit indices into a 32-bit Morton code
        index1 &= 0x0000ffff;
        index2 &= 0x0000ffff;
        index1 |= ( index1 << 8 );
        index2 |= ( index2 << 8 );
        index1 &= 0x00ff00ff;
        index2 &= 0x00ff00ff;
        index1 |= ( index1 << 4 );
        index2 |= ( index2 << 4 );
        index1 &= 0x0f0f0f0f;
        index2 &= 0x0f0f0f0f;
        index1 |= ( index1 << 2 );
        index2 |= ( index2 << 2 );
        index1 &= 0x33333333;
        index2 &= 0x33333333;
        index1 |= ( index1 << 1 );
        index2 |= ( index2 << 1 );
        index1 &= 0x55555555;
        index2 &= 0x55555555;
        return index1 | ( index2 << 1 );
    }

    /**
     * Takes a Morton code, with interleaved x and y bits and x in the least significant bit, and returns the short
     * representing the x position.
     * This uses 16 bits of the 32-bit Morton code/Z-Code.
     * Source: http://and-what-happened.blogspot.com/2011/08/fast-2d-and-3d-hilbert-curves-and.html
     * @param morton A Morton code or Z-Code that interleaves two 8-bit numbers
     * @return A short that represents the x position extracted from the Morton code/Z-Code
     */
    public static short zDecodeX( final int morton )
    { // unpack the 8-bit (unsigned) first index from a 16-bit (unsigned) Morton code/Z-Code
        short value1 = (short)(morton & 0xffff);
        value1 &= 0x5555;
        value1 |= ( value1 >> 1 );
        value1 &= 0x3333;
        value1 |= ( value1 >> 2 );
        value1 &= 0x0f0f;
        value1 |= ( value1 >> 4 );
        value1 &= 0x00ff;
        return value1;
    }
    /**
     * Takes a Morton code, with interleaved x and y bits and x in the least significant bit, and returns the short
     * representing the y position.
     * This uses 16 bits of the 32-bit Morton code/Z-Code.
     * Source: http://and-what-happened.blogspot.com/2011/08/fast-2d-and-3d-hilbert-curves-and.html
     * @param morton A Morton code or Z-Code that interleaves two 8-bit numbers
     * @return A short that represents the y position extracted from the Morton code/Z-Code
     */
    public static short zDecodeY( final int morton )
    { // unpack the 8-bit (unsigned) second index from a 16-bit (unsigned) Morton code/Z-Code
        short value2 = (short)((morton & 0xffff) >>> 1 );
        value2 &= 0x5555;
        value2 |= ( value2 >> 1 );
        value2 &= 0x3333;
        value2 |= ( value2 >> 2 );
        value2 &= 0x0f0f;
        value2 |= ( value2 >> 4 );
        value2 &= 0x00ff;
        return value2;
    }

    /**
     * Takes a Morton code, with interleaved x and y bits and x in the least significant bit, and returns the Coord
     * representing the same x, y position.
     * This uses 16 bits of the Morton code and requires that the code is non-negative.
     * Source: http://and-what-happened.blogspot.com/2011/08/fast-2d-and-3d-hilbert-curves-and.html
     * @param morton an int containing two interleaved numbers, from 0 to 255 each
     * @return a Coord matching the x and y extracted from the Morton code
     */
    public static Coord mortonDecode( final int morton )
    { // unpack 2 8-bit (unsigned) indices from a 32-bit (signed...) Morton code
        int value1 = morton;
        int value2 = ( value1 >> 1 );
        value1 &= 0x5555;
        value2 &= 0x5555;
        value1 |= ( value1 >> 1 );
        value2 |= ( value2 >> 1 );
        value1 &= 0x3333;
        value2 &= 0x3333;
        value1 |= ( value1 >> 2 );
        value2 |= ( value2 >> 2 );
        value1 &= 0x0f0f;
        value2 &= 0x0f0f;
        value1 |= ( value1 >> 4 );
        value2 |= ( value2 >> 4 );
        value1 &= 0x00ff;
        value2 &= 0x00ff;
        return Coord.get(value1, value2);
    }

    /**
     * Takes a Morton code, with interleaved x and y bits and x in the least significant bit, and returns the Coord
     * representing the same x, y position.
     * This takes a a 16-bit Z-Code with data in the sign bit, as returned by zEncode().
     * Source: http://and-what-happened.blogspot.com/2011/08/fast-2d-and-3d-hilbert-curves-and.html
     * @param morton a short containing two interleaved numbers, from 0 to 255 each
     * @return a Coord matching the x and y extracted from the Morton code
     */
    public static Coord zDecode( final short morton )
    { // unpack 2 8-bit (unsigned) indices from a 32-bit (signed...) Morton code
        int value1 = morton & 0xffff;
        int value2 = ( value1 >> 1 );
        value1 &= 0x5555;
        value2 &= 0x5555;
        value1 |= ( value1 >> 1 );
        value2 |= ( value2 >> 1 );
        value1 &= 0x3333;
        value2 &= 0x3333;
        value1 |= ( value1 >> 2 );
        value2 |= ( value2 >> 2 );
        value1 &= 0x0f0f;
        value2 &= 0x0f0f;
        value1 |= ( value1 >> 4 );
        value2 |= ( value2 >> 4 );
        value1 &= 0x00ff;
        value2 &= 0x00ff;
        return Coord.get(value1, value2);
    }
    /**
     * Takes a Morton code, with interleaved x and y bits and x in the least significant bit, and returns the Coord
     * representing the same x, y position. With this method, x and y can have up to 16 bits, but Coords returned by
     * this method will not be cached if they have a x or y component greater than 255.
     * This uses 32 bits of the Morton code and will treat the sign bit as the most significant bit of y, unsigned.
     * Source: http://and-what-happened.blogspot.com/2011/08/fast-2d-and-3d-hilbert-curves-and.html
     * @param morton an int containing two interleaved shorts.
     * @return a Coord matching the x and y extracted from the Morton code
     */
    public static Coord mortonDecode16( final int morton )
    { // unpack 2 16-bit indices from a 32-bit Morton code
        int value1 = morton;
        int value2 = ( value1 >>> 1 );
        value1 &= 0x55555555;
        value2 &= 0x55555555;
        value1 |= ( value1 >>> 1 );
        value2 |= ( value2 >>> 1 );
        value1 &= 0x33333333;
        value2 &= 0x33333333;
        value1 |= ( value1 >>> 2 );
        value2 |= ( value2 >>> 2 );
        value1 &= 0x0f0f0f0f;
        value2 &= 0x0f0f0f0f;
        value1 |= ( value1 >>> 4 );
        value2 |= ( value2 >>> 4 );
        value1 &= 0x00ff00ff;
        value2 &= 0x00ff00ff;
        value1 |= ( value1 >>> 8 );
        value2 |= ( value2 >>> 8 );
        value1 &= 0x0000ffff;
        value2 &= 0x0000ffff;
        return Coord.get(value1, value2);
    }
}