package com.github.tommyettinger;

/**
 * Created by Tommy Ettinger on 2/21/2016.
 */
public class LinearData {

    public boolean[] data;
    public int[] bounds;
    /**
     * Given a point as an array or vararg of int coordinates and the bounds as an array of int dimension lengths,
     * computes an index into a 1D array that matches bounds. The value this returns will be between 0 (inclusive) and
     * the product of each element in bounds (exclusive), unless the given point does not fit in bounds. If point is
     * out-of-bounds, this always returns -1, and callers should check for -1 as an output.
     * @param bound the bounding dimension lengths as an int array
     * @param point the coordinates of the point to encode as a int array or vararg; must have a length at least equal
     *              to the length of bounds, and only items that correspond to dimensions in bounds will be used
     * @return an index into a 1D array that is sized to contain all of bounds, or -1 if point is invalid
     */
    public static int boundedIndex(int[] bound, int... point)
    {
        int u = 0;
        for (int a = 0; a < bound.length; a++) {
            if(point[a] >= bound[a])
                return -1;
            else
            {
                u *= bound[a];
                u += point[a];
            }
        }
        return u;
    }

    /**
     * Given an index into a bounded 1D array as produced by boundedIndex(), reproduces the point at the bounded index.
     * The point is represented as an int array with equivalent dimension count as bounds. The earliest dimension, at
     * index 0, is considered the most significant for the integer value of index.
     * @param bound the bounding dimension lengths as an int array
     * @param index the index, as could be produced by boundedIndex()
     * @return the point referred to by index within the given bounds.
     */
    public static int[] fromBounded(int[] bound, int index)
    {
        if(index < 0)
            throw new ArrayIndexOutOfBoundsException("Index must not be negative");
        int[] point = new int[bound.length];
        int u = 1;
        for (int a = bound.length - 1; a >= 0; a--) {
            point[a] = (index / u) % bound[a];
            u *= bound[a];
        }
        return point;
    }

    /**
     * Given an index into a bounded 1D array as produced by boundedIndex() if it used oldBounds, converts the index to
     * instead use newBounds. The two bounds arrays need to be the same length (referring to the same number of
     * dimensions), and if oldBounds is larger in any dimension than newBounds, indexes that refer to points outside the
     * limits of newBounds will be invalid (-1).
     * @param oldBounds the bounding dimension lengths used to create index, as an int array
     * @param newBounds the bounding dimension lengths to convert index to use, as an int array
     * @param index the index, as could be produced by boundedIndex() with oldBounds
     * @return an index into a 1D array that could fit all of newBounds.
     */
    public static int convertBounded(int[] oldBounds, int[] newBounds, int index)
    {
        if(index < 0)
            throw new ArrayIndexOutOfBoundsException("Index must not be negative");
        if(oldBounds == null || newBounds == null || oldBounds.length != newBounds.length)
            throw new UnsupportedOperationException("Invalid bounds when converting");

        return boundedIndex(newBounds, fromBounded(oldBounds, index));
    }


    public static int validateBounds(int[] bound, int dimensions)
    {
        if(bound == null || dimensions != bound.length)
            throw new UnsupportedOperationException("Invalid bounds; should be an array with " +
                    dimensions + " elements");
        long b = 1;
        for (int i = 0; i < dimensions; i++) {
            b *= bound[i];
        }
        if(b > 1L << 30)
            throw new UnsupportedOperationException("Bounds are too big!");

        return (int)b;
    }

    /**
     * Constructor given a 1D array of boolean items and an int array of bounds. Typically used when something returns
     * a boolean array that already uses the same bounds to encode multiple dimensions of data. While bounds can have
     * larger volume than {@code items.length}, it must not have smaller, and it almost all cases should be equal.
     * Consider using the constructor that takes a boolean array and two bounds arrays if you need to convert an array
     * to use larger bounds with the same number of dimensions.
     * @param items the array of items
     * @param bound the bounds to use for the data, which must have a total volume at least equal to items.length
     */
    public LinearData(boolean[] items, int[] bound)
    {
        if(items == null || items.length == 0
                || bound == null || bound.length == 0) {
            bounds = new int[]{0};
            data = new boolean[0];
            return;
        }
        int b = validateBounds(bound, bound.length);
        if(b < items.length)
            throw new UnsupportedOperationException("Bounds are too small to fit given items");
        data = new boolean[b];
        System.arraycopy(items, 0, data, 0, items.length);
    }

    /**
     * Converting constructor that takes a 1D boolean array that had multiple dimensions encoded with oldBounds, those
     * same oldBounds, and newBounds to use in the converted new object.  In order to run successfully, oldBounds and
     * newBounds need to have the same length (referring to the same number of dimensions), and if any indices in items
     * refer to points that are not within newBounds, those indices will be discarded (this can shrink bounds by doing
     * so, removing anything out of bounds). If newBounds has only dimension lengths that are at least equal to the
     * corresponding length in oldBounds, every element in items will be copied over to the same position encoded
     * differently.
     * @param items the array of items encoded with oldBounds
     * @param oldBounds the bounds as an int array used to encode items
     * @param newBounds the bounds as an int array to use to encode this object
     */
    public LinearData(boolean[] items, int[] oldBounds, int[] newBounds)
    {
        if(items == null || items.length == 0
                || oldBounds == null || oldBounds.length == 0
                || newBounds == null || newBounds.length == 0) {
            bounds = new int[]{0};
            data = new boolean[0];
            return;
        }
        validateBounds(oldBounds, oldBounds.length);
        int b = validateBounds(newBounds, newBounds.length);
        bounds = new int[newBounds.length];
        System.arraycopy(newBounds, 0, bounds, 0, newBounds.length);

        data = new boolean[b];
        int j;
        for (int i = 0; i < items.length; i++) {
            j = convertBounded(oldBounds, newBounds, i);
            if(j >= 0)
                data[j] = items[i];
        }
    }

    /**
     * Constructor given a 2D array of boolean items. Uses the array lengths of the first array in each dimension of
     * items to determine the bounds, and assumes it has been given a non-jagged array.
     * @param items the array of items
     */
    public LinearData(boolean[][] items)
    {
        if(items == null || items.length == 0 || items[0].length == 0) {
            bounds = new int[]{0, 0};
            data = new boolean[0];
            return;
        }
        int l0 = items.length, l1 = items[0].length;
        bounds = new int[]{l0, l1};
        int b = validateBounds(bounds, 2);
        data = new boolean[b];
        for (int d0 = 0; d0 < l0; d0++) {
            for (int d1 = 0; d1 < l1; d1++) {
                data[d0 * l1
                        + d1] = items[d0][d1];
            }
        }
    }

    /**
     * Constructor given a 3D array of boolean items. Uses the array lengths of the first array in each dimension of
     * items to determine the bounds, and assumes it has been given a non-jagged array.
     * @param items the array of items
     */
    public LinearData(boolean[][][] items)
    {
        if(items == null || items.length == 0 || items[0].length == 0 || items[0][0].length == 0) {
            bounds = new int[]{0, 0, 0};
            data = new boolean[0];
            return;
        }
        int l0 = items.length, l1 = items[0].length, l2 = items[0][0].length;
        bounds = new int[]{l0, l1, l2};
        int b = validateBounds(bounds, 3);
        data = new boolean[b];
        for (int d0 = 0; d0 < l0; d0++) {
            for (int d1 = 0; d1 < l1; d1++) {
                for (int d2 = 0; d2 < l2; d2++) {
                    data[d0 * l1 * l2
                            + d1 * l2
                            + d2]
                            = items[d0][d1][d2];
                }
            }
        }
    }

    /**
     * Constructor given a 4D array of boolean items. Uses the array lengths of the first array in each dimension of
     * items to determine the bounds, and assumes it has been given a non-jagged array.
     * @param items the array of items
     */
    public LinearData(boolean[][][][] items)
    {
        if(items == null || items.length == 0 || items[0].length == 0 || items[0][0].length == 0
                || items[0][0][0].length == 0) {
            bounds = new int[]{0, 0, 0, 0};
            data = new boolean[0];
            return;
        }
        int l0 = items.length, l1 = items[0].length, l2 = items[0][0].length, l3 = items[0][0][0].length;
        bounds = new int[]{l0, l1, l2, l3};
        int b = validateBounds(bounds, 4);
        data = new boolean[b];
        for (int d0 = 0; d0 < l0; d0++) {
            for (int d1 = 0; d1 < l1; d1++) {
                for (int d2 = 0; d2 < l2; d2++) {
                    for (int d3 = 0; d3 < l3; d3++) {
                        data[d0 * l1 * l2 * l3
                                + d1 * l2 * l3
                                + d2 * l3
                                + d3]
                                = items[d0][d1][d2][d3];
                    }
                }
            }
        }
    }

    /**
     * Constructor given a 5D array of boolean items. Uses the array lengths of the first array in each dimension of
     * items to determine the bounds, and assumes it has been given a non-jagged array.
     * @param items the array of items
     */
    public LinearData(boolean[][][][][] items)
    {
        if(items == null || items.length == 0 || items[0].length == 0 || items[0][0].length == 0
                || items[0][0][0].length == 0 || items[0][0][0][0].length == 0) {
            bounds = new int[]{0, 0, 0, 0, 0};
            data = new boolean[0];
            return;
        }
        int l0 = items.length, l1 = items[0].length, l2 = items[0][0].length, l3 = items[0][0][0].length,
                l4 = items[0][0][0][0].length;
        bounds = new int[]{l0, l1, l2, l3, l4};
        int b = validateBounds(bounds, 5);
        data = new boolean[b];
        for (int d0 = 0; d0 < l0; d0++) {
            for (int d1 = 0; d1 < l1; d1++) {
                for (int d2 = 0; d2 < l2; d2++) {
                    for (int d3 = 0; d3 < l3; d3++) {
                        for (int d4 = 0; d4 < l4; d4++) {
                            data[d0 * l1 * l2 * l3 * l4
                                    + d1 * l2 * l3 * l4
                                    + d2 * l3 * l4
                                    + d3 * l4
                                    + d4]
                                    = items[d0][d1][d2][d3][d4];
                        }
                    }
                }
            }
        }
    }

    /**
     * Constructor given a 6D array of boolean items. Uses the array lengths of the first array in each dimension of
     * items to determine the bounds, and assumes it has been given a non-jagged array.
     * @param items the array of items
     */
    public LinearData(boolean[][][][][][] items)
    {
        if(items == null || items.length == 0 || items[0].length == 0 || items[0][0].length == 0
                || items[0][0][0].length == 0 || items[0][0][0][0].length == 0 || items[0][0][0][0][0].length == 0) {
            bounds = new int[]{0, 0, 0, 0, 0, 0};
            data = new boolean[0];
            return;
        }
        int l0 = items.length, l1 = items[0].length, l2 = items[0][0].length, l3 = items[0][0][0].length,
                l4 = items[0][0][0][0].length, l5 = items[0][0][0][0][0].length;
        bounds = new int[]{l0, l1, l2, l3, l4, l5};
        int b = validateBounds(bounds, 6);
        data = new boolean[b];
        for (int d0 = 0; d0 < l0; d0++) {
            for (int d1 = 0; d1 < l1; d1++) {
                for (int d2 = 0; d2 < l2; d2++) {
                    for (int d3 = 0; d3 < l3; d3++) {
                        for (int d4 = 0; d4 < l4; d4++) {
                            for (int d5 = 0; d5 < l5; d5++) {
                                data[d0 * l1 * l2 * l3 * l4 * l5
                                        + d1 * l2 * l3 * l4 * l5
                                        + d2 * l3 * l4 * l5
                                        + d3 * l4 * l5
                                        + d4 * l5
                                        + d5]
                                        = items[d0][d1][d2][d3][d4][d5];
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Constructor given a 7D array of boolean items. Uses the array lengths of the first array in each dimension of
     * items to determine the bounds, and assumes it has been given a non-jagged array.
     * @param items the array of items
     */
    public LinearData(boolean[][][][][][][] items)
    {
        if(items == null || items.length == 0 || items[0].length == 0 || items[0][0].length == 0
                || items[0][0][0].length == 0 || items[0][0][0][0].length == 0 || items[0][0][0][0][0].length == 0
                || items[0][0][0][0][0][0].length == 0) {
            bounds = new int[]{0, 0, 0, 0, 0, 0, 0};
            data = new boolean[0];
            return;
        }
        int l0 = items.length, l1 = items[0].length, l2 = items[0][0].length, l3 = items[0][0][0].length,
                l4 = items[0][0][0][0].length, l5 = items[0][0][0][0][0].length, l6 = items[0][0][0][0][0][0].length;
        bounds = new int[]{l0, l1, l2, l3, l4, l5, l6};
        int b = validateBounds(bounds, 7);
        data = new boolean[b];
        for (int d0 = 0; d0 < l0; d0++) {
            for (int d1 = 0; d1 < l1; d1++) {
                for (int d2 = 0; d2 < l2; d2++) {
                    for (int d3 = 0; d3 < l3; d3++) {
                        for (int d4 = 0; d4 < l4; d4++) {
                            for (int d5 = 0; d5 < l5; d5++) {
                                for (int d6 = 0; d6 < l6; d6++) {
                                    data[d0 * l1 * l2 * l3 * l4 * l5 * l6
                                            + d1 * l2 * l3 * l4 * l5 * l6
                                            + d2 * l3 * l4 * l5 * l6
                                            + d3 * l4 * l5 * l6
                                            + d4 * l5 * l6
                                            + d5 * l6
                                            + d6]
                                            = items[d0][d1][d2][d3][d4][d5][d6];
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Constructor given an 8D array of boolean items. Uses the array lengths of the first array in each dimension of
     * items to determine the bounds, and assumes it has been given a non-jagged array.
     * @param items the array of items
     */
    public LinearData(boolean[][][][][][][][] items)
    {
        if(items == null || items.length == 0 || items[0].length == 0 || items[0][0].length == 0
                || items[0][0][0].length == 0 || items[0][0][0][0].length == 0 || items[0][0][0][0][0].length == 0
                || items[0][0][0][0][0][0].length == 0 || items[0][0][0][0][0][0][0].length == 0) {
            bounds = new int[]{0, 0, 0, 0, 0, 0, 0, 0};
            data = new boolean[0];
            return;
        }
        int l0 = items.length, l1 = items[0].length, l2 = items[0][0].length, l3 = items[0][0][0].length,
                l4 = items[0][0][0][0].length, l5 = items[0][0][0][0][0].length, l6 = items[0][0][0][0][0][0].length,
                l7 = items[0][0][0][0][0][0][0].length;
        bounds = new int[]{l0, l1, l2, l3, l4, l5, l6, l7};
        int b = validateBounds(bounds, 8);
        data = new boolean[b];
        for (int d0 = 0; d0 < l0; d0++) {
            for (int d1 = 0; d1 < l1; d1++) {
                for (int d2 = 0; d2 < l2; d2++) {
                    for (int d3 = 0; d3 < l3; d3++) {
                        for (int d4 = 0; d4 < l4; d4++) {
                            for (int d5 = 0; d5 < l5; d5++) {
                                for (int d6 = 0; d6 < l6; d6++) {
                                    for (int d7 = 0; d7 < l7; d7++) {
                                        data[d0 * l1 * l2 * l3 * l4 * l5 * l6 * l7
                                                + d1 * l2 * l3 * l4 * l5 * l6 * l7
                                                + d2 * l3 * l4 * l5 * l6 * l7
                                                + d3 * l4 * l5 * l6 * l7
                                                + d4 * l5 * l6 * l7
                                                + d5 * l6 * l7
                                                + d6 * l7
                                                + d7]
                                                = items[d0][d1][d2][d3][d4][d5][d6][d7];
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

}
