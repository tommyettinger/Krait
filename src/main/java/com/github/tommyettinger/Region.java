package com.github.tommyettinger;

import com.gs.collections.api.list.primitive.*;
import com.gs.collections.impl.list.mutable.primitive.ByteArrayList;
import com.gs.collections.impl.list.mutable.primitive.ShortArrayList;
import com.gs.collections.impl.list.mutable.primitive.IntArrayList;
import com.gs.collections.impl.list.mutable.primitive.LongArrayList;

/**
 * The primary form of data produced by RegionPacker, meant to be primarily used by that class.
 * Created by Tommy Ettinger on 2/15/2016.
 */
public class Region {
    protected MutableByteList byteList;
    protected MutableShortList shortList;
    protected MutableIntList intList;
    protected MutableLongList longList;
    protected int bytesPer;
    public int size = 0;
    public static final short MASK8 = 0xff;
    public static final int MASK16  = 0xffff;
    public static final long MASK32 = 0xffffffffL;
    public Region()
    {
        bytesPer = 8;
        longList = new LongArrayList(16);
    }
    public Region(int bytesPerItem)
    {
        if(bytesPerItem != 1 && bytesPerItem != 2 && bytesPerItem != 4)
            bytesPer = 8;
        else
            bytesPer = bytesPerItem;

        switch (bytesPer)
        {
            case 1: byteList = new ByteArrayList(16);
                break;
            case 2: shortList = new ShortArrayList(16);
                break;
            case 4: intList = new IntArrayList(16);
                break;
            default: longList = new LongArrayList(16);
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
            case 1: byteList = new ByteArrayList(capacity);
                break;
            case 2: shortList = new ShortArrayList(capacity);
                break;
            case 4: intList = new IntArrayList(capacity);
                break;
            default: longList = new LongArrayList(capacity);
                break;
        }
    }
    public Region(long... values)
    {
        bytesPer = 8;
        longList = new LongArrayList(values);
        size = values.length;
    }
    public Region(byte[] values)
    {
        bytesPer = 1;
        byteList = new ByteArrayList(values);
        size = values.length;
    }
    public Region(short[] values)
    {
        bytesPer = 2;
        shortList = new ShortArrayList(values);
        size = values.length;
    }
    public Region(int[] values)
    {
        bytesPer = 4;
        intList = new IntArrayList(values);
        size = values.length;
    }
    public void add(long value)
    {
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
    public void addBytes(byte... values)
    {
        switch (bytesPer)
        {
            case 1:
                byteList.addAll(values);
                break;
            case 2:
                for (int i = 0; i < values.length; i++) {
                    shortList.add((short) (MASK8 & values[i]));
                }
                break;
            case 4:
                for (int i = 0; i < values.length; i++) {
                    intList.add(MASK8 & values[i]);
                }
                break;
            default:
                for (int i = 0; i < values.length; i++) {
                    longList.add(MASK8 & values[i]);
                }
                break;
        }
        size += values.length;
    }

    public void addShorts(short... values)
    {
        switch (bytesPer)
        {
            case 1:
                for (int i = 0; i < values.length; i++) {
                    byteList.add((byte) values[i]);
                }
                break;
            case 2:
                shortList.addAll(values);
                break;
            case 4:
                for (int i = 0; i < values.length; i++) {
                    intList.add(MASK16 & values[i]);
                }
                break;
            default:
                for (int i = 0; i < values.length; i++) {
                    longList.add(MASK16 & values[i]);
                }
                break;
        }
        size += values.length;
    }

    public void addInts(int... values)
    {
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
                intList.addAll(values);
                break;
            default:
                for (int i = 0; i < values.length; i++) {
                    longList.add(MASK32 & values[i]);
                }
                break;
        }
        size += values.length;
    }

    public void addLongs(long... values)
    {
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
            case 1: return byteList.get(index);
            case 2: return shortList.get(index);
            case 4: return intList.get(index);
            default: return longList.get(index);
        }
    }
    public int getBytesPerItem()
    {
        return bytesPer;
    }

    public void set(int index, long value)
    {
        switch (bytesPer)
        {
            case 1: byteList.set(index, (byte)value);
                break;
            case 2: shortList.set(index, (short)value);
                break;
            case 4: intList.set(index, (int)value);
                break;
            default: longList.set(index, value);
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
        return next;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Region region = (Region) o;

        if (bytesPer != region.bytesPer) return false;
        if (size != region.size) return false;
        if (byteList != null ? !byteList.equals(region.byteList) : region.byteList != null) return false;
        if (shortList != null ? !shortList.equals(region.shortList) : region.shortList != null) return false;
        if (intList != null ? !intList.equals(region.intList) : region.intList != null) return false;
        return longList != null ? longList.equals(region.longList) : region.longList == null;

    }

    @Override
    public int hashCode() {
        int result = byteList != null ? byteList.hashCode() : 0;
        result = 31 * result + (shortList != null ? shortList.hashCode() : 0);
        result = 31 * result + (intList != null ? intList.hashCode() : 0);
        result = 31 * result + (longList != null ? longList.hashCode() : 0);
        result = 31 * result + bytesPer;
        result = 31 * result + size;
        return result;
    }
}