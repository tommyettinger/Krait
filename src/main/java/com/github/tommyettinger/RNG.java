/*
Written in 2015 by Sebastiano Vigna (vigna@acm.org)

To the extent possible under law, the author has dedicated all copyright
and related and neighboring rights to this software to the public domain
worldwide. This software is distributed without any warranty.

See <http://creativecommons.org/publicdomain/zero/1.0/>. */
package com.github.tommyettinger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * This is a SplittableRandom-style generator, meant to have a tiny state
 * that permits storing many different generators with low overhead.
 * It should be rather fast, though no guarantees can be made.
 * Written in 2015 by Sebastiano Vigna (vigna@acm.org)
 * @author Sebastiano Vigna
 * @author Tommy Ettinger
 */
public class RNG
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

    public int next( int bits ) {
        return (int)( nextLong() & ( 1L << bits ) - 1 );
    }

    /**
     * Can return any long, positive or negative, of any size permissible in a 64-bit signed integer.
     * @return any long, all 64 bits are random
     */
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
    public int nextInt() {
        return (int)nextLong();
    }

    /**
     * Exclusive on the upper bound n.  The lower bound is 0.
     * @param n the upper bound; should be positive
     * @return a random int less than n and at least equal to 0
     */
    public int nextInt( final int n ) {
        if ( n <= 0 ) throw new IllegalArgumentException();
            final int bits = nextInt() >>> 1;
        return bits % n;
    }

    /**
     * Inclusive lower, exclusive upper.
     * @param lower the lower bound, inclusive, can be positive or negative
     * @param upper the upper bound, exclusive, should be positive, must be greater than lower
     * @return a random int at least equal to lower and less than upper
     */
    public int nextInt( final int lower, final int upper ) {
        if ( upper - lower <= 0 ) throw new IllegalArgumentException();
        return lower + nextInt(upper - lower);
    }

    /**
     * Exclusive on the upper bound n. The lower bound is 0.
     * @param n the upper bound; should be positive
     * @return a random long less than n
     */
    public long nextLong( final long n ) {
        if ( n <= 0 ) throw new IllegalArgumentException();
        //for(;;) {
            final long bits = nextLong() >>> 1;
        return bits % n;
            //long value = bits % n;
            //value = (value < 0) ? -value : value;
            //if ( bits - value + ( n - 1 ) >= 0 ) return value;
        //}
    }

    /**
     * Inclusive lower, exclusive upper.
     * @param lower the lower bound, inclusive, can be positive or negative
     * @param upper the upper bound, exclusive, should be positive, must be greater than lower
     * @return a random long at least equal to lower and less than upper
     */
    public long nextLong( final long lower, final long upper ) {
        if ( upper - lower <= 0 ) throw new IllegalArgumentException();
        return lower + nextLong(upper - lower);
    }
    /**
     * Gets a uniform random double in the range [0.0,1.0)
     * @return a random double at least equal to 0.0 and less than 1.0
     */
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
     * Shuffle an array using the Fisher-Yates algorithm.
     * @param elements an array of T; will not be modified
     * @param <T> can be any non-primitive type.
     * @return a shuffled copy of elements
     */
    public <T> T[] shuffle(T[] elements)
    {
        T[] array = elements.clone();
        int n = array.length;
        for (int i = 0; i < n; i++)
        {
            int r = i + nextInt(n - i);
            T t = array[r];
            array[r] = array[i];
            array[i] = t;
        }
        return array;
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
     * Gets a random portion of an array and returns it as a new array. Will only use a given position in the given
     * array at most once; does this by shuffling a copy of the array and getting a section of it.
     * @param data an array of T; will not be modified.
     * @param count the non-negative number of elements to randomly take from data
     * @param <T> can be any non-primitive type.
     * @return an array of T that has length equal to the smaller of count or data.length
     */
    public <T> T[] randomPortion(T[] data, int count)
    {
        T[] array = Arrays.copyOf(data, Math.min(count, data.length));
        System.arraycopy(shuffle(data), 0, array, 0, Math.min(count, data.length));
        return array;
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
     * @param count the total number of elements to use; will be less if the range is smaller than count
     * @return an int array that contains at most one of each number in the range
     */
    public int[] randomRange(int start, int end, int count)
    {
        if(end <= start || start < 0)
            return new int[0];
        int[] data = new int[end - start];
        for (int e = start, i = 0; e < end; e++) {
            data[i++] = e;
        }
        int n = data.length;
        for (int i = 0; i < n; i++)
        {
            int r = i + nextInt(n - i);
            int t = data[r];
            data[r] = data[i];
            data[i] = t;
        }
        int[] array = new int[Math.min(count, n)];
        System.arraycopy(data, 0, array, 0, Math.min(count, n));
        return array;
    }
    /**
     * Sets the current state of this generator (equivalent in behavior to making a new RNG with state as the seed, but
     * this is more efficient).
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
     * or backward a number of steps specified by advance, where a step is equal to one call to nextInt().
     * @param advance Number of future generations to skip past. Can be negative to backtrack.
     * @return the state after skipping.
     */
    public long skip(long advance)
    {
        return state += 0x9E3779B97F4A7C15L * advance;
    }

}