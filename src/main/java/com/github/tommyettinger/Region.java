package com.github.tommyettinger;

import com.gs.collections.api.PrimitiveIterable;
import com.gs.collections.api.list.primitive.MutableByteList;
import com.gs.collections.api.list.primitive.MutableIntList;
import com.gs.collections.api.list.primitive.MutableLongList;
import com.gs.collections.api.list.primitive.MutableShortList;
import com.gs.collections.impl.list.mutable.primitive.ByteArrayList;
import com.gs.collections.impl.list.mutable.primitive.IntArrayList;
import com.gs.collections.impl.list.mutable.primitive.LongArrayList;
import com.gs.collections.impl.list.mutable.primitive.ShortArrayList;

/**
 * The primary form of data produced by RegionPacker, meant to be primarily used by that class.
 * Created by Tommy Ettinger on 2/15/2016.
 */
public class Region {
    protected PrimitiveIterable list;
    protected int bytesPer;
    public int size = 0;
    private static final short MASK8 = 0xff;
    private static final int MASK16  = 0xffff;
    private static final long MASK32 = 0xffffffffL;
    public Region()
    {
        bytesPer = 8;
        list = new LongArrayList(16);
    }
    public Region(int bytesPerItem)
    {
        if(bytesPerItem != 1 && bytesPerItem != 2 && bytesPerItem != 4)
            bytesPer = 8;
        else
            bytesPer = bytesPerItem;

        switch (bytesPer)
        {
            case 1: list = new ByteArrayList(16);
                break;
            case 2: list = new ShortArrayList(16);
                break;
            case 4: list = new IntArrayList(16);
                break;
            default: list = new LongArrayList(16);
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
            case 1: list = new ByteArrayList(capacity);
                break;
            case 2: list = new ShortArrayList(capacity);
                break;
            case 4: list = new IntArrayList(capacity);
                break;
            default: list = new LongArrayList(capacity);
                break;
        }
    }

    public Region(MutableByteList list)
    {
        bytesPer = 1;
        this.list = list;
        this.size = list.size();
    }

    public Region(MutableShortList list)
    {
        bytesPer = 2;
        this.list = list;
        this.size = list.size();
    }

    public Region(MutableIntList list)
    {
        bytesPer = 4;
        this.list = list;
        this.size = list.size();
    }

    public Region(MutableLongList list)
    {
        bytesPer = 8;
        this.list = list;
        this.size = list.size();
    }

    public Region(long... values)
    {
        bytesPer = 8;
        list = new LongArrayList(values);
        size = values.length;
    }
    public Region(byte[] values)
    {
        bytesPer = 1;
        list = new ByteArrayList(values);
        size = values.length;
    }
    public Region(short[] values)
    {
        bytesPer = 2;
        list = new ShortArrayList(values);
        size = values.length;
    }
    public Region(int[] values)
    {
        bytesPer = 4;
        list = new IntArrayList(values);
        size = values.length;
    }
    public void add(long value)
    {
        switch (bytesPer)
        {
            case 1: ((MutableByteList)list).add((byte)value);
                break;
            case 2: ((MutableShortList)list).add((short)value);
                break;
            case 4: ((MutableIntList)list).add((int)value);
                break;
            default: ((MutableLongList)list).add(value);
                break;
        }
        size++;
    }
    public void addBytes(byte... values)
    {
        switch (bytesPer)
        {
            case 1:
                ((MutableByteList)list).addAll(values);
                break;
            case 2:
                for (int i = 0; i < values.length; i++) {
                    ((MutableShortList)list).add((short) (MASK8 & values[i]));
                }
                break;
            case 4:
                for (int i = 0; i < values.length; i++) {
                    ((MutableIntList)list).add(MASK8 & values[i]);
                }
                break;
            default:
                for (int i = 0; i < values.length; i++) {
                    ((MutableLongList)list).add(MASK8 & values[i]);
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
                    ((MutableByteList)list).add((byte) values[i]);
                }
                break;
            case 2:
                ((MutableShortList)list).addAll(values);
                break;
            case 4:
                for (int i = 0; i < values.length; i++) {
                    ((MutableIntList)list).add(MASK16 & values[i]);
                }
                break;
            default:
                for (int i = 0; i < values.length; i++) {
                    ((MutableLongList)list).add(MASK16 & values[i]);
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
                    ((MutableByteList)list).add((byte) values[i]);
                }
                break;
            case 2:
                for (int i = 0; i < values.length; i++) {
                    ((MutableShortList)list).add((short) values[i]);
                }
                break;
            case 4:
                ((MutableIntList)list).addAll(values);
                break;
            default:
                for (int i = 0; i < values.length; i++) {
                    ((MutableLongList)list).add(MASK32 & values[i]);
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
                    ((MutableByteList)list).add((byte) values[i]);
                }
                break;
            case 2:
                for (int i = 0; i < values.length; i++) {
                    ((MutableShortList)list).add((short) values[i]);
                }
                break;
            case 4:
                for (int i = 0; i < values.length; i++) {
                    ((MutableIntList)list).add((int) values[i]);
                }
                break;
            default: ((MutableLongList)list).addAll(values);
                break;
        }
        size += values.length;
    }

    public Region with(long... values)
    {
        switch (bytesPer)
        {
            case 1:
                for (int i = 0; i < values.length; i++) {
                    ((MutableByteList)list).add((byte) values[i]);
                }
                break;
            case 2:
                for (int i = 0; i < values.length; i++) {
                    ((MutableShortList)list).add((short) values[i]);
                }
                break;
            case 4:
                for (int i = 0; i < values.length; i++) {
                    ((MutableIntList)list).add((int) values[i]);
                }
                break;
            default: ((MutableLongList)list).addAll(values);
                break;
        }
        size += values.length;
        return this;
    }

    public Region resetTo(long... values)
    {
        bytesPer = 8;
        list = new LongArrayList(values);
        size = values.length;
        return this;
    }

    public Region resetTo(byte[] values)
    {
        bytesPer = 1;
        list = new ByteArrayList(values);
        size = values.length;
        return this;
    }

    public Region resetTo(short[] values)
    {
        bytesPer = 2;
        list = new ShortArrayList(values);
        size = values.length;
        return this;
    }

    public Region resetTo(int[] values)
    {
        bytesPer = 4;
        list = new IntArrayList(values);
        size = values.length;
        return this;
    }
    public void addRange(long start, long end)
    {
        if(end <= start)
            return;
        switch (bytesPer)
        {
            case 1:
                for (long i = start; i < end; i++) {
                    ((MutableByteList)list).add((byte) i);
                }
                break;
            case 2:
                for (long i = start; i < end; i++) {
                    ((MutableShortList)list).add((short) i);
                }
                break;
            case 4:
                for (long i = start; i < end; i++) {
                    ((MutableIntList)list).add((int) i);
                }
                break;
            default:
                for (long i = start; i < end; i++) {
                    ((MutableLongList)list).add(i);
                }
                break;
        }
        size += end - start;
    }

    public long get(int index)
    {
        switch (bytesPer)
        {
            case 1: return ((MutableByteList)list).get(index);
            case 2: return ((MutableShortList)list).get(index);
            case 4: return ((MutableIntList)list).get(index);
            default: return ((MutableLongList)list).get(index);
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
            case 1: ((MutableByteList)list).set(index, (byte)value);
                break;
            case 2: ((MutableShortList)list).set(index, (short)value);
                break;
            case 4: ((MutableIntList)list).set(index, (int)value);
                break;
            default: ((MutableLongList)list).set(index, value);
                break;
        }
    }
    public byte[] asBytes()
    {
        switch (bytesPer)
        {
            case 1: return ((MutableByteList)list).toArray();
            case 2: return Conversion.toBytes(((MutableShortList)list).toArray());
            case 4: return Conversion.toBytes(((MutableIntList)list).toArray());
            default: return Conversion.toBytes(((MutableLongList)list).toArray());
        }
    }

    public short[] asShorts()
    {
        switch (bytesPer)
        {
            case 1: return Conversion.toShorts(((MutableByteList)list).toArray());
            case 2: return ((MutableShortList)list).toArray();
            case 4: return Conversion.toShorts(((MutableIntList)list).toArray());
            default: return Conversion.toShorts(((MutableLongList)list).toArray());
        }
    }

    public int[] asInts()
    {
        switch (bytesPer)
        {
            case 1: return Conversion.toInts(((MutableByteList)list).toArray());
            case 2: return Conversion.toInts(((MutableShortList)list).toArray());
            case 4: return ((MutableIntList)list).toArray();
            default: return Conversion.toInts(((MutableLongList)list).toArray());
        }
    }

    public long[] asLongs()
    {
        switch (bytesPer)
        {
            case 1: return Conversion.toLongs(((MutableByteList)list).toArray());
            case 2: return Conversion.toLongs(((MutableShortList)list).toArray());
            case 4: return Conversion.toLongs(((MutableIntList)list).toArray());
            default: return ((MutableLongList)list).toArray();
        }
    }

    public Region copy()
    {
        switch (bytesPer) {
            case 1:
                return new Region((MutableByteList) list);
            case 2:
                return new Region((MutableShortList) list);
            case 4:
                return new Region((MutableIntList) list);
            default:
                return new Region((MutableLongList) list);
        }
    }

}