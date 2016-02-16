package com.github.tommyettinger;

import gnu.trove.TLongCollection;
import gnu.trove.function.TLongFunction;
import gnu.trove.list.array.TLongArrayList;
import gnu.trove.procedure.TLongProcedure;

import java.util.Collection;
import java.util.Random;

/**
 * The primary form of data produced by RegionPacker, meant to be primarily used by that class.
 * Created by Tommy Ettinger on 2/15/2016.
 */
public class Region extends TLongArrayList {
    protected boolean frozen = false;
    public Region()
    {
        super();
    }
    public Region(int capacity)
    {
        super(capacity);
    }
    public Region(long... values)
    {
        super(values);
    }
    public Region(byte[] values)
    {
        super(Conversion.toLongs(values));
    }
    public Region(short[] values)
    {
        super(Conversion.toLongs(values));
    }
    public Region(int[] values)
    {
        super(Conversion.toLongs(values));
    }

    /**
     * Creates a new <code>TLongArrayList</code> instance with the
     * specified capacity.
     *
     * @param capacity       an <code>int</code> value
     * @param no_entry_value an <code>long</code> value that represents null.
     */
    public Region(int capacity, long no_entry_value) {
        super(capacity, no_entry_value);
    }

    /**
     * Creates a new <code>TLongArrayList</code> instance that contains
     * a copy of the collection passed to us.
     *
     * @param collection the collection to copy
     */
    public Region(TLongCollection collection) {
        super(collection);
    }

    protected Region(long[] values, long no_entry_value, boolean wrap) {
        super(values, no_entry_value, wrap);
    }

    /**
     * Sheds any excess capacity above and beyond the current size of the list.
     */
    @Override
    public void trimToSize() {
        if(frozen) throw new UnsupportedOperationException("This Region has been frozen and cannot be mutated.");
        super.trimToSize();
    }

    /**
     * {@inheritDoc}
     *
     * @param val
     */
    @Override
    public boolean add(long val) {
        if(frozen) throw new UnsupportedOperationException("This Region has been frozen and cannot be mutated.");
        return super.add(val);
    }

    /**
     * {@inheritDoc}
     *
     * @param vals
     */
    @Override
    public void add(long[] vals) {
        if(frozen) throw new UnsupportedOperationException("This Region has been frozen and cannot be mutated.");
        super.add(vals);
    }

    /**
     * {@inheritDoc}
     *
     * @param vals
     * @param offset
     * @param length
     */
    @Override
    public void add(long[] vals, int offset, int length) {
        if(frozen) throw new UnsupportedOperationException("This Region has been frozen and cannot be mutated.");
        super.add(vals, offset, length);
    }

    /**
     * {@inheritDoc}
     *
     * @param offset
     * @param value
     */
    @Override
    public void insert(int offset, long value) {
        if(frozen) throw new UnsupportedOperationException("This Region has been frozen and cannot be mutated.");
        super.insert(offset, value);
    }

    /**
     * {@inheritDoc}
     *
     * @param offset
     * @param values
     */
    @Override
    public void insert(int offset, long[] values) {
        if(frozen) throw new UnsupportedOperationException("This Region has been frozen and cannot be mutated.");
        super.insert(offset, values);
    }

    /**
     * {@inheritDoc}
     *
     * @param offset
     * @param values
     * @param valOffset
     * @param len
     */
    @Override
    public void insert(int offset, long[] values, int valOffset, int len) {
        if(frozen) throw new UnsupportedOperationException("This Region has been frozen and cannot be mutated.");
        super.insert(offset, values, valOffset, len);
    }

    /**
     * {@inheritDoc}
     *
     * @param offset
     * @param val
     */
    @Override
    public long replace(int offset, long val) {
        if(frozen) throw new UnsupportedOperationException("This Region has been frozen and cannot be mutated.");
        return super.replace(offset, val);
    }

    /**
     * {@inheritDoc}
     *
     * @param offset
     * @param values
     */
    @Override
    public void set(int offset, long[] values) {
        if(frozen) throw new UnsupportedOperationException("This Region has been frozen and cannot be mutated.");
        super.set(offset, values);
    }

    /**
     * {@inheritDoc}
     *
     * @param offset
     * @param values
     * @param valOffset
     * @param length
     */
    @Override
    public void set(int offset, long[] values, int valOffset, int length) {
        if(frozen) throw new UnsupportedOperationException("This Region has been frozen and cannot be mutated.");
        super.set(offset, values, valOffset, length);
    }

    /**
     * Sets the value at the specified offset without doing any bounds checking.
     *
     * @param offset
     * @param val
     */
    @Override
    public void setQuick(int offset, long val) {
        if(frozen) throw new UnsupportedOperationException("This Region has been frozen and cannot be mutated.");
        super.setQuick(offset, val);
    }

    /**
     * {@inheritDoc}
     *
     * @param value
     */
    @Override
    public boolean remove(long value) {
        if(frozen) throw new UnsupportedOperationException("This Region has been frozen and cannot be mutated.");
        return super.remove(value);
    }

    /**
     * {@inheritDoc}
     *
     * @param offset
     */
    @Override
    public long removeAt(int offset) {
        if(frozen) throw new UnsupportedOperationException("This Region has been frozen and cannot be mutated.");
        return super.removeAt(offset);
    }

    /**
     * {@inheritDoc}
     *
     * @param offset
     * @param length
     */
    @Override
    public void remove(int offset, int length) {
        if(frozen) throw new UnsupportedOperationException("This Region has been frozen and cannot be mutated.");
        super.remove(offset, length);
    }

    /**
     * {@inheritDoc}
     *
     * @param collection
     */
    @Override
    public boolean addAll(Collection<? extends Long> collection) {
        if(frozen) throw new UnsupportedOperationException("This Region has been frozen and cannot be mutated.");
        return super.addAll(collection);
    }

    /**
     * {@inheritDoc}
     *
     * @param collection
     */
    @Override
    public boolean addAll(TLongCollection collection) {
        if(frozen) throw new UnsupportedOperationException("This Region has been frozen and cannot be mutated.");
        return super.addAll(collection);
    }

    /**
     * {@inheritDoc}
     *
     * @param array
     */
    @Override
    public boolean addAll(long[] array) {
        if(frozen) throw new UnsupportedOperationException("This Region has been frozen and cannot be mutated.");
        return super.addAll(array);
    }

    /**
     * {@inheritDoc}
     *
     * @param collection
     */
    @Override
    public boolean retainAll(Collection<?> collection) {
        if(frozen) throw new UnsupportedOperationException("This Region has been frozen and cannot be mutated.");
        return super.retainAll(collection);
    }

    /**
     * {@inheritDoc}
     *
     * @param array
     */
    @Override
    public boolean retainAll(long[] array) {
        if(frozen) throw new UnsupportedOperationException("This Region has been frozen and cannot be mutated.");
        return super.retainAll(array);
    }

    /**
     * {@inheritDoc}
     *
     * @param collection
     */
    @Override
    public boolean retainAll(TLongCollection collection) {
        if(frozen) throw new UnsupportedOperationException("This Region has been frozen and cannot be mutated.");
        return super.retainAll(collection);
    }

    /**
     * {@inheritDoc}
     *
     * @param collection
     */
    @Override
    public boolean removeAll(Collection<?> collection) {
        if(frozen) throw new UnsupportedOperationException("This Region has been frozen and cannot be mutated.");
        return super.removeAll(collection);
    }

    /**
     * {@inheritDoc}
     *
     * @param collection
     */
    @Override
    public boolean removeAll(TLongCollection collection) {
        if(frozen) throw new UnsupportedOperationException("This Region has been frozen and cannot be mutated.");
        return super.removeAll(collection);
    }

    /**
     * {@inheritDoc}
     *
     * @param array
     */
    @Override
    public boolean removeAll(long[] array) {
        if(frozen) throw new UnsupportedOperationException("This Region has been frozen and cannot be mutated.");
        return super.removeAll(array);
    }

    /**
     * {@inheritDoc}
     *
     * @param function
     */
    @Override
    public void transformValues(TLongFunction function) {
        if(frozen) throw new UnsupportedOperationException("This Region has been frozen and cannot be mutated.");
        super.transformValues(function);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void reverse() {
        if(frozen) throw new UnsupportedOperationException("This Region has been frozen and cannot be mutated.");
        super.reverse();
    }

    /**
     * {@inheritDoc}
     *
     * @param from
     * @param to
     */
    @Override
    public void reverse(int from, int to) {
        if(frozen) throw new UnsupportedOperationException("This Region has been frozen and cannot be mutated.");
        super.reverse(from, to);
    }

    /**
     * {@inheritDoc}
     *
     * @param rand
     */
    @Override
    public void shuffle(Random rand) {
        if(frozen) throw new UnsupportedOperationException("This Region has been frozen and cannot be mutated.");
        super.shuffle(rand);
    }

    /**
     * {@inheritDoc}
     *
     * @param procedure
     */
    @Override
    public boolean forEach(TLongProcedure procedure) {
        if(frozen) throw new UnsupportedOperationException("This Region has been frozen and cannot be mutated.");
        return super.forEach(procedure);
    }

    /**
     * {@inheritDoc}
     *
     * @param procedure
     */
    @Override
    public boolean forEachDescending(TLongProcedure procedure) {
        if(frozen) throw new UnsupportedOperationException("This Region has been frozen and cannot be mutated.");
        return super.forEachDescending(procedure);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void sort() {
        if(frozen) throw new UnsupportedOperationException("This Region has been frozen and cannot be mutated.");
        super.sort();
    }

    /**
     * {@inheritDoc}
     *
     * @param fromIndex
     * @param toIndex
     */
    @Override
    public void sort(int fromIndex, int toIndex) {
        if(frozen) throw new UnsupportedOperationException("This Region has been frozen and cannot be mutated.");
        super.sort(fromIndex, toIndex);
    }

    /**
     * {@inheritDoc}
     *
     * @param val
     */
    @Override
    public void fill(long val) {
        if(frozen) throw new UnsupportedOperationException("This Region has been frozen and cannot be mutated.");
        super.fill(val);
    }

    /**
     * {@inheritDoc}
     *
     * @param fromIndex
     * @param toIndex
     * @param val
     */
    @Override
    public void fill(int fromIndex, int toIndex, long val) {
        if(frozen) throw new UnsupportedOperationException("This Region has been frozen and cannot be mutated.");
        super.fill(fromIndex, toIndex, val);
    }

    public void addRange(long start, long end)
    {
        if(frozen) throw new UnsupportedOperationException("This Region has been frozen and cannot be mutated.");
        if(end <= start)
            return;
        for (long i = start; i < end; i++) {
            add(i);
        }
    }

    public byte[] asBytes()
    {
        return Conversion.toBytes(toArray());

    }

    public short[] asShorts()
    {
        return Conversion.toShorts(toArray());

    }

    public int[] asInts()
    {
         return Conversion.toInts(toArray());
    }

    public long[] asLongs()
    {
        return toArray();
    }

    public Region freeze()
    {
        frozen = true;
        return this;
    }
    public boolean isFrozen()
    {
        return frozen;
    }
    public Region thaw()
    {
        return new Region(this);
    }

    public Region copy()
    {
        Region next = new Region(this);
        next.frozen = frozen;
        return next;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        Region region = (Region) o;

        return frozen == region.frozen;

    }

    @Override
    public int hashCode() {
        if(!frozen)
            return 0;
        return super.hashCode();
    }
}
