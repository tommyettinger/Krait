/*
Written in 2015 by Sebastiano Vigna (vigna@acm.org)

To the extent possible under law, the author has dedicated all copyright
and related and neighboring rights to this software to the public domain
worldwide. This software is distributed without any warranty.

See <http://creativecommons.org/publicdomain/zero/1.0/>. */
package com.github.tommyettinger;

import it.unimi.dsi.fastutil.ints.IntArrayList;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * This is a SplittableRandom-style generator, meant to have a tiny state
 * that permits storing many different generators with low overhead.
 * It should be rather fast, particularly here on the JVM; it isn't the
 * fastest algorithm when implemented in C but seems very competitive on
 * the JVM.
 * Written in 2015 by Sebastiano Vigna (vigna@acm.org)
 * @author Sebastiano Vigna
 * @author Tommy Ettinger
 */
public class RNG extends Random
{
	/** 2 raised to the 53, - 1. */
    private static final long DOUBLE_MASK = ( 1L << 53 ) - 1;
    /** 2 raised to the -53. */
    private static final double NORM_53 = 1. / ( 1L << 53 );
    /** 2 raised to the 24, -1. */
    private static final long FLOAT_MASK = ( 1L << 24 ) - 1;
    /** 2 raised to the -24. */
    private static final double NORM_24 = 1. / ( 1L << 24 );

	private static final long serialVersionUID = -1656615589113474497L;

    public long state; /* The state can be seeded with any value. */

    /** Creates a new generator seeded using Math.random. */
    public RNG() {
        this((long) Math.floor(Math.random() * Long.MAX_VALUE));
    }

    public RNG(final long seed ) {
        setState(seed);
    }

    @Override
    public int next( int bits ) {
        return (int)( nextLong() & ( 1L << bits ) - 1 );
    }

    public long next( long bits ) {
        return nextLong() & ( 1L << bits ) - 1;
    }

    /**
     * Can return any long, positive or negative, of any size permissible in a 64-bit signed integer.
     * @return any long, all 64 bits are random
     */
    @Override
    public long nextLong() {
        long z = ( state += 0x9E3779B97F4A7C15L );
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        return z ^ (z >>> 31);
    }

    /**
     * Can return any int, positive or negative, of any size permissible in a 32-bit signed integer.
     * @return any int, all 32 bits are random
     */
    @Override
    public int nextInt() {
        return (int)nextLong();
    }

    /**
     * Exclusive on the upper bound n.  The lower bound is 0.
     * @param bound the upper bound (exclusive); must be positive
     * @return a random int less than n and at least equal to 0
     */
    @Override
    public int nextInt( final int bound ) {
        if ( bound <= 0 ) return 0;
        int threshold = (0x7fffffff - bound + 1) % bound;
        for (;;) {
            int bits = (int)(nextLong() & 0x7fffffff);
            if (bits >= threshold)
                return bits % bound;
        }
    }

    /**
     * Inclusive lower, exclusive upper.
     * @param lower the lower bound, inclusive, can be positive or negative
     * @param upper the upper bound, exclusive, should be positive, must be greater than lower
     * @return a random int at least equal to lower and less than upper
     */
    public int nextInt( final int lower, final int upper ) {
        if ( upper - lower <= 0 ) return lower;
        return lower + nextInt(upper - lower);
    }

    /**
     * Exclusive on the upper bound n. The lower bound is 0.
     * @param bound the upper bound (exclusive); must be positive
     * @return a random long less than n
     */
    public long nextLong( final long bound ) {
        if ( bound <= 0 ) return 0;
        long threshold = (0x7fffffffffffffffL - bound + 1) % bound;
        for (;;) {
            long bits = nextLong() & 0x7fffffffffffffffL;
            if (bits >= threshold)
                return bits % bound;
        }
    }

    /**
     * Inclusive lower, exclusive upper.
     * @param lower the lower bound, inclusive, can be positive or negative
     * @param upper the upper bound, exclusive, should be positive, must be greater than lower
     * @return a random long at least equal to lower and less than upper
     */
    public long nextLong( final long lower, final long upper ) {
        if ( upper - lower <= 0 ) return lower;
        return lower + nextLong(upper - lower);
    }
    /**
     * Gets a uniform random double in the range [0.0,1.0)
     * @return a random double at least equal to 0.0 and less than 1.0
     */
    @Override
    public double nextDouble() {
        return ( nextLong() & DOUBLE_MASK ) * NORM_53;
    }

    /**
     * Gets a uniform random double in the range [0.0,outer) given a positive parameter outer. If outer
     * is negative, it will be the (exclusive) lower bound and 0.0 will be the (inclusive) upper bound.
     * @param outer the exclusive outer bound, can be negative
     * @return a random double between 0.0 (inclusive) and outer (exclusive)
     */
    public double nextDouble(final double outer) {
        return nextDouble() * outer;
    }

    /**
     * Gets a uniform random float in the range [0.0,1.0)
     * @return a random float at least equal to 0.0 and less than 1.0
     */
    @Override
    public float nextFloat() {
        return (float)( ( nextLong() & FLOAT_MASK ) * NORM_24 );
    }

    /**
     * Gets a random value, true or false.
     * Calls nextLong() once.
     * @return a random true or false value.
     */
    public boolean nextBoolean() {
        return ( nextLong() & 1 ) != 0L;
    }

    /**
     * Given a byte array as a parameter, this will fill the array with random bytes (modifying it
     * in-place). Calls nextLong() {@code Math.ceil(bytes.length / 8.0)} times.
     * @param bytes a byte array that will have its contents overwritten with random bytes.
     */
    @Override
    public void nextBytes( final byte[] bytes ) {
        int i = bytes.length, n = 0;
        while( i != 0 ) {
            n = Math.min( i, 8 );
            for ( long bits = nextLong(); n-- != 0; bits >>= 8 ) bytes[ --i ] = (byte)bits;
        }
    }


    /**
     * Returns a value from a even distribution from min (inclusive) to max
     * (exclusive).
     *
     * @param min the minimum bound on the return value (inclusive)
     * @param max the maximum bound on the return value (exclusive)
     * @return the found value
     */
    public double between(double min, double max) {
        return min + (max - min) * nextDouble();
    }

    /**
     * Returns a value between min (inclusive) and max (exclusive).
     *
     * The inclusive and exclusive behavior is to match the behavior of the
     * similar method that deals with floating point values.
     *
     * @param min the minimum bound on the return value (inclusive)
     * @param max the maximum bound on the return value (exclusive)
     * @return the found value
     */
    public int between(int min, int max) {
        return nextInt(max - min) + min;
    }

    /**
     * Returns the average of a number of randomly selected numbers from the
     * provided range, with min being inclusive and max being exclusive. It will
     * sample the number of times passed in as the third parameter.
     *
     * The inclusive and exclusive behavior is to match the behavior of the
     * similar method that deals with floating point values.
     *
     * This can be used to weight RNG calls to the average between min and max.
     *
     * @param min the minimum bound on the return value (inclusive)
     * @param max the maximum bound on the return value (exclusive)
     * @param samples the number of samples to take
     * @return the found value
     */
    public int betweenWeighted(int min, int max, int samples) {
        int sum = 0;
        for (int i = 0; i < samples; i++) {
            sum += between(min, max);
        }

        return Math.round((float) sum / samples);
    }

    /**
     * Returns a random element from the provided array and maintains object
     * type.
     *
     * @param <T> the type of the returned object
     * @param array the array to get an element from
     * @return the randomly selected element
     */
    public <T> T getRandomElement(T[] array) {
        if (array.length < 1) {
            return null;
        }
        return array[nextInt(array.length)];
    }

    /**
     * Returns a random element from the provided list. If the list is empty
     * then null is returned.
     *
     * @param <T> the type of the returned object
     * @param list the list to get an element from
     * @return the randomly selected element
     */
    public <T> T getRandomElement(List<T> list) {
        if (list.size() <= 0) {
            return null;
        }
        return list.get(nextInt(list.size()));
    }
    /**
     * Shuffle an array using the "inside-out" Fisher-Yates algorithm.
     * <br>
     * https://en.wikipedia.org/wiki/Fisher%E2%80%93Yates_shuffle#The_.22inside-out.22_algorithm
     * @param elements an array of T; will not be modified
     * @param <T> can be any non-primitive type.
     * @param dest
     * 			Where to put the shuffle. It MUST have the same length as {@code elements}
     * @return {@code dest} after modifications
     */
    public <T> T[] shuffle(T[] elements, T[] dest)
    {
        if (dest.length != elements.length)
            return randomPortion(elements, dest);

        for (int i = 0; i < elements.length; i++)
        {
            int r = nextInt(i + 1);
            if(r != i)
                dest[i] = dest[r];
            dest[r] = elements[i];
        }
        return dest;
    }

    /**
     * Shuffle a {@link List} using the Fisher-Yates algorithm.
     * @param elements a List of T; will not be modified
     * @param <T> can be any non-primitive type.
     * @return a shuffled ArrayList containing the whole of elements in pseudo-random order.
     */
    public <T> ArrayList<T> shuffle(List<T> elements)
    {
        ArrayList<T> al = new ArrayList<T>(elements);
        int n = al.size();
        for (int i = 0; i < n; i++)
        {
            Collections.swap(al, i + nextInt(n - i), i);
        }
        return al;
    }
    /**
     * Gets a random portion of data (an array), assigns that portion to output (an array) so that it fills as much as
     * it can, and then returns output. Will only use a given position in the given data at most once; does this by
     * generating random indices for data's elements, but only as much as needed, assigning the copied section to output
     * and not modifying data.
     * <br>
     * Based on http://stackoverflow.com/a/21460179 , credit to Vincent van der Weele; modifications were made to avoid
     * copying or creating a new generic array (a problem on GWT).
     * @param data an array of T; will not be modified.
     * @param output an array of T that will be overwritten; should always be instantiated with the portion length
     * @param <T> can be any non-primitive type.
     * @return an array of T that has length equal to output's length and may contain unchanged elements (null if output
     * was empty) if data is shorter than output
     */
    public <T> T[] randomPortion(T[] data, T[] output)
    {
        int length = data.length;
        int[] mapping = new int[length];
        for (int i = 0; i < length; i++) {
            mapping[i] = i;
        }

        for (int i = 0; i < output.length && length > 0; i++) {
            int r = nextInt(length);

            output[i] = data[mapping[r]];

            mapping[r] = length-1;
            length--;
        }

        return output;
    }


    /**
     * Gets a random portion of data (an array), and returns that portion as a new int array with length size, or length
     * equal to data.length if it is smaller than size. Will only use a given position in the given data at most once.
     * Does not modify data.
     * <br>
     * Based on http://stackoverflow.com/a/21460179 , credit to Vincent van der Weele
     * @param data an array of int; will not be modified.
     * @param size the length of the portion to use
     * @return an int array of random items from data; has length equal to size, or data.length if it is smaller
     */
    public int[] randomPortion(int[] data, int size) {
        if(size >= data.length)
            size = data.length;
        int[] result = new int[size];
        int length = data.length;

        for (int i = 0; i < size; i++) {
            int r = nextInt(length);

            result[i] = data[r];

            data[r] = data[length-1];
            length--;
        }

        return result;
    }
    /**
     * Gets a random portion of a List and returns it as a new List. Will only use a given position in the given
     * List at most once; does this by shuffling a copy of the List and getting a section of it.
     * @param data a List of T; will not be modified.
     * @param count the non-negative number of elements to randomly take from data
     * @param <T> can be any non-primitive type
     * @return a List of T that has length equal to the smaller of count or data.length
     */
    public <T> List<T> randomPortion(List<T> data, int count)
    {
        return shuffle(data).subList(0, Math.min(count, data.size()));
    }

    /**
     * Gets a random subrange of the non-negative ints from start (inclusive) to end (exclusive), using count elements.
     * May return an empty array if the parameters are invalid (end is less than/equal to start, or start is negative).
     * @param start the start of the range of numbers to potentially use (inclusive)
     * @param end  the end of the range of numbers to potentially use (exclusive)
     * @param chance the odds, from 0.0 to 1.0, of any number being used
     * @return an int array that contains at most one of each number in the range
     */
    public int[] randomSamples(int start, int end, double chance)
    {
        if(end <= start || start < 0)
            return new int[0];
        IntArrayList vla = new IntArrayList(end - start);
        for (int e = start; e < end; e++) {
            if(nextDouble() < chance)
                vla.add(e);
        }
        return vla.toIntArray();
    }
    /**
     * Gets a random subrange of the non-negative ints from start (inclusive) to end (exclusive), using count elements.
     * May return an empty array if the parameters are invalid (end is less than/equal to start, or start is negative).
     * @param start the start of the range of numbers to potentially use (inclusive)
     * @param end  the end of the range of numbers to potentially use (exclusive)
     * @param count the total number of elements to use; will be less if the range is smaller than count
     * @return an int array that contains at most one of each number in the range
     */
    public int[] randomRange(int start, int end, int count)
    {
        if(end <= start || start < 0)
            return new int[0];
        int[] data = new int[end - start];
        for (int e = start, i = 0; e < end; e++, i++) {
            data[i] = e;
        }
        return randomPortion(data, Math.min(count, end - start));
    }

    private double nextNextGaussian;
    private boolean haveNextNextGaussian = false;
    /**
     * Returns the next pseudorandom, Gaussian ("normally") distributed
     * {@code double} value with mean {@code 0.0} and standard
     * deviation {@code 1.0} from this random number generator's sequence.
     * <p>
     * Please do not use methods that directly change the state, such as setSeed(), setState(), or skip(), if you use
     * this method, because the value this returns is dependent on an extra variable unrelated to the RNG state. If
     * <p>
     * The general contract of {@code nextGaussian} is that one
     * {@code double} value, chosen from (approximately) the usual
     * normal distribution with mean {@code 0.0} and standard deviation
     * {@code 1.0}, is pseudorandomly generated and returned.
     * <p>
     * <p>The method {@code nextGaussian} is implemented by class
     * {@code RNG} as if by the following:
     * <pre> {@code
     * private double nextNextGaussian;
     * private boolean haveNextNextGaussian = false;
     *
     * public double nextGaussian() {
     *   if (haveNextNextGaussian) {
     *     haveNextNextGaussian = false;
     *     return nextNextGaussian;
     *   } else {
     *     double v1, v2, s;
     *     do {
     *       v1 = 2 * nextDouble() - 1;   // between -1.0 and 1.0
     *       v2 = 2 * nextDouble() - 1;   // between -1.0 and 1.0
     *       s = v1 * v1 + v2 * v2;
     *     } while (s >= 1 || s == 0);
     *     double multiplier = StrictMath.sqrt(-2 * StrictMath.log(s)/s);
     *     nextNextGaussian = v2 * multiplier;
     *     haveNextNextGaussian = true;
     *     return v1 * multiplier;
     *   }
     * }}</pre>
     * This uses the <i>polar method</i> of G. E. P. Box, M. E. Muller, and
     * G. Marsaglia, as described by Donald E. Knuth in <i>The Art of
     * Computer Programming</i>, Volume 3: <i>Seminumerical Algorithms</i>,
     * section 3.4.1, subsection C, algorithm P. Note that it generates two
     * independent values at the cost of only one call to {@code StrictMath.log}
     * and one call to {@code StrictMath.sqrt}.
     *
     * NOTE: This is not synchronized like java.util.Random's implementation.
     *
     * @return the next pseudorandom, Gaussian ("normally") distributed
     * {@code double} value with mean {@code 0.0} and
     * standard deviation {@code 1.0} from this random number
     * generator's sequence
     */
    @Override
    public double nextGaussian() {
        // See Knuth, ACP, Section 3.4.1 Algorithm C.
        // Implementation given in javadoc for java.util.Random.nextGaussian()
        if (haveNextNextGaussian) {
            haveNextNextGaussian = false;
            return nextNextGaussian;
        } else {
            double v1, v2, s;
            do {
                v1 = 2 * nextDouble() - 1; // between -1 and 1
                v2 = 2 * nextDouble() - 1; // between -1 and 1
                s = v1 * v1 + v2 * v2;
            } while (s >= 1 || s == 0);
            double multiplier = StrictMath.sqrt(-2 * StrictMath.log(s)/s);
            nextNextGaussian = v2 * multiplier;
            haveNextNextGaussian = true;
            return v1 * multiplier;
        }
    }

    /**
     * Sets the current state of this generator (equivalent in behavior to making a new RNG with state as the seed, but
     * this is more efficient).
     * <br>
     * Should not be used in conjunction with nextGaussian(), which relies on a peripheral value generated from every
     * other call to that method.
     * @param state the seed to use for this RNG, as if it was constructed with this seed.
     */
    @Override
    public void setSeed(final long state ) {
        this.state = state;
    }
    /**
     * Sets the current state of this generator (equivalent in behavior to making a new RNG with state as the seed, but
     * this is more efficient).
     * <br>
     * Should not be used in conjunction with nextGaussian(), which relies on a peripheral value generated from every
     * other call to that method.
     * @param state the seed to use for this RNG, as if it was constructed with this seed.
     */
    public void setState(final long state ) {
        this.state = state;
    }
    /**
     * Gets the current state of this generator.
     * @return the current seed of this RNG, changed once per call to nextLong()
     */
    public long getState( ) {
        return state;
    }

    /**
     * Advances or rolls back the RNG's state without actually generating numbers. Skip forward
     * or backward a number of steps specified by advance, where a step is equal to one call to nextLong().
     * <br>
     * Should not be used in conjunction with nextGaussian(), which relies on a peripheral value generated from every
     * other call to that method.
     * @param advance Number of future generations to skip past. Can be negative to backtrack.
     * @return the state after skipping.
     */
    public long skip(long advance)
    {
        return state += 0x9E3779B97F4A7C15L * advance;
    }

}