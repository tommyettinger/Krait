package com.github.tommyettinger;

import gnu.trove.list.array.TByteArrayList;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.list.array.TLongArrayList;
import gnu.trove.list.array.TShortArrayList;

/**
 * The primary form of data produced by RegionPacker, meant to be primarily used by that class.
 * Created by Tommy Ettinger on 2/15/2016.
 */
public class Region {
    protected TByteArrayList byteList;
    protected TShortArrayList shortList;
    protected TIntArrayList intList;
    protected TLongArrayList longList;
    protected int bytesPer;
    public int size = 0;
    protected boolean frozen = false;
    public Region()
    {
        bytesPer = 8;
        longList = new TLongArrayList(16);
    }
    public Region(int bytesPerItem)
    {
        if(bytesPerItem != 1 && bytesPerItem != 2 && bytesPerItem != 4)
            bytesPer = 8;
        else
            bytesPer = bytesPerItem;

        switch (bytesPer)
        {
            case 1: byteList = new TByteArrayList(16);
                break;
            case 2: shortList = new TShortArrayList(16);
                break;
            case 4: intList = new TIntArrayList(16);
                break;
            default: longList = new TLongArrayList(16);
                break;
        }
    }
    public Region(int bytesPerItem, int capacity)
    {
        if(bytesPerItem != 1 && bytesPerItem != 2 && bytesPerItem != 4)
            bytesPer = 8;
        else
            bytesPer = bytesPerItem;

        switch (bytesPer)
        {
            case 1: byteList = new TByteArrayList(capacity);
                break;
            case 2: shortList = new TShortArrayList(capacity);
                break;
            case 4: intList = new TIntArrayList(capacity);
                break;
            default: longList = new TLongArrayList(capacity);
                break;
        }
    }
    public Region(long... values)
    {
        bytesPer = 8;
        longList = new TLongArrayList(values);
        size = values.length;
    }
    public Region(byte[] values)
    {
        bytesPer = 1;
        byteList = new TByteArrayList(values);
        size = values.length;
    }
    public Region(short[] values)
    {
        bytesPer = 2;
        shortList = new TShortArrayList(values);
        size = values.length;
    }
    public Region(int[] values)
    {
        bytesPer = 4;
        intList = new TIntArrayList(values);
        size = values.length;
    }
    public void add(long value)
    {
        if(frozen)
            throw new UnsupportedOperationException("This Region has been frozen and cannot be mutated.");
        switch (bytesPer)
        {
            case 1: byteList.add((byte)value);
                break;
            case 2: shortList.add((short)value);
                break;
            case 4: intList.add((int)value);
                break;
            default: longList.add(value);
                break;
        }
        size++;
    }
    public void addAll(long... values)
    {
        if(frozen)
            throw new UnsupportedOperationException("This Region has been frozen and cannot be mutated.");
        switch (bytesPer)
        {
            case 1:
                for (int i = 0; i < values.length; i++) {
                    byteList.add((byte) values[i]);
                }
                break;
            case 2:
                for (int i = 0; i < values.length; i++) {
                    shortList.add((short) values[i]);
                }
                break;
            case 4:
                for (int i = 0; i < values.length; i++) {
                    intList.add((int) values[i]);
                }
                break;
            default: longList.addAll(values);
                break;
        }
        size += values.length;
    }
    public void addRange(long start, long end)
    {
        if(frozen)
            throw new UnsupportedOperationException("This Region has been frozen and cannot be mutated.");
        if(end <= start)
            return;
        switch (bytesPer)
        {
            case 1:
                for (long i = start; i < end; i++) {
                    byteList.add((byte) i);
                }
                break;
            case 2:
                for (long i = start; i < end; i++) {
                    shortList.add((short) i);
                }
                break;
            case 4:
                for (long i = start; i < end; i++) {
                    intList.add((int) i);
                }
                break;
            default:
                for (long i = start; i < end; i++) {
                    longList.add(i);
                }
                break;
        }
        size += end - start;
    }

    public long get(int index)
    {
        switch (bytesPer)
        {
            case 1: return byteList.getQuick(index);
            case 2: return shortList.getQuick(index);
            case 4: return intList.getQuick(index);
            default: return longList.getQuick(index);
        }
    }
    public int getBytesPerItem()
    {
        return bytesPer;
    }

    public void set(int index, long value)
    {
        if(frozen)
            throw new UnsupportedOperationException("This Region has been frozen and cannot be mutated.");
        switch (bytesPer)
        {
            case 1: byteList.setQuick(index, (byte)value);
                break;
            case 2: shortList.setQuick(index, (short)value);
                break;
            case 4: intList.setQuick(index, (int)value);
                break;
            default: longList.setQuick(index, value);
                break;
        }
    }
    public byte[] asBytes()
    {
        switch (bytesPer)
        {
            case 1: return byteList.toArray();
            case 2: return Conversion.toBytes(shortList.toArray());
            case 4: return Conversion.toBytes(intList.toArray());
            default: return Conversion.toBytes(longList.toArray());
        }
    }

    public short[] asShorts()
    {
        switch (bytesPer)
        {
            case 1: return Conversion.toShorts(byteList.toArray());
            case 2: return shortList.toArray();
            case 4: return Conversion.toShorts(intList.toArray());
            default: return Conversion.toShorts(longList.toArray());
        }
    }

    public int[] asInts()
    {
        switch (bytesPer)
        {
            case 1: return Conversion.toInts(byteList.toArray());
            case 2: return Conversion.toInts(shortList.toArray());
            case 4: return intList.toArray();
            default: return Conversion.toInts(longList.toArray());
        }
    }

    public long[] asLongs()
    {
        switch (bytesPer)
        {
            case 1: return Conversion.toLongs(byteList.toArray());
            case 2: return Conversion.toLongs(shortList.toArray());
            case 4: return Conversion.toLongs(intList.toArray());
            default: return longList.toArray();
        }
    }

    public void freeze()
    {
        frozen = true;
    }
    public boolean isFrozen()
    {
        return frozen;
    }
    public Region thaw()
    {
        Region next = new Region(bytesPer, size);
        switch (bytesPer)
        {
            case 1: next.byteList = byteList;
                break;
            case 2: next.shortList = shortList;
                break;
            case 4: next.intList = intList;
                break;
            default: next.longList = longList;
        }
        return next;
    }

    public Region copy()
    {
        Region next = new Region(bytesPer, size);
        switch (bytesPer)
        {
            case 1: next.byteList = byteList;
                break;
            case 2: next.shortList = shortList;
                break;
            case 4: next.intList = intList;
                break;
            default: next.longList = longList;
        }
        next.frozen = frozen;
        return next;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Region region = (Region) o;

        if (bytesPer != region.bytesPer) return false;
        if (size != region.size) return false;
        if (frozen != region.frozen) return false;
        if (byteList != null ? !byteList.equals(region.byteList) : region.byteList != null) return false;
        if (shortList != null ? !shortList.equals(region.shortList) : region.shortList != null) return false;
        if (intList != null ? !intList.equals(region.intList) : region.intList != null) return false;
        return longList != null ? longList.equals(region.longList) : region.longList == null;

    }

    @Override
    public int hashCode() {
        if(!frozen)
            return 0;
        int result = byteList != null ? byteList.hashCode() : 0;
        result = 31 * result + (shortList != null ? shortList.hashCode() : 0);
        result = 31 * result + (intList != null ? intList.hashCode() : 0);
        result = 31 * result + (longList != null ? longList.hashCode() : 0);
        result = 31 * result + bytesPer;
        result = 31 * result + size;
        return result;
    }
}
