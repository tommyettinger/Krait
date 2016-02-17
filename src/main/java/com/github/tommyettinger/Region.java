package com.github.tommyettinger;

import com.gs.collections.api.PrimitiveIterable;
import com.gs.collections.api.block.procedure.primitive.*;
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
    public void prepend(long value)
    {
        switch (bytesPer)
        {
            case 1: ((MutableByteList)list).addAtIndex(0, (byte)value);
                break;
            case 2: ((MutableShortList)list).addAtIndex(0, (short)value);
                break;
            case 4: ((MutableIntList)list).addAtIndex(0, (int)value);
                break;
            default: ((MutableLongList)list).addAtIndex(0, value);
                break;
        }
        size++;
    }

    public void addAt(int index, long value)
    {
        switch (bytesPer)
        {
            case 1: ((MutableByteList)list).addAtIndex(index, (byte)value);
                break;
            case 2: ((MutableShortList)list).addAtIndex(index, (short)value);
                break;
            case 4: ((MutableIntList)list).addAtIndex(index, (int)value);
                break;
            default: ((MutableLongList)list).addAtIndex(index, value);
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
                return new Region(ByteArrayList.newList((MutableByteList) list));
            case 2:
                return new Region(ShortArrayList.newList((MutableShortList) list));
            case 4:
                return new Region(IntArrayList.newList((MutableIntList) list));
            default:
                return new Region(LongArrayList.newList((MutableLongList) list));
        }
    }

    public Region freeze()
    {
        switch (bytesPer) {
            case 1:
                list = ((MutableByteList)list).asUnmodifiable();
                break;
            case 2:
                list = ((MutableShortList)list).asUnmodifiable();
                break;
            case 4:
                list = ((MutableIntList)list).asUnmodifiable();
                break;
            default:
                list = ((MutableLongList)list).asUnmodifiable();
                break;
        }
        return this;
    }
    public Region forEach(final LongProcedure procedure)
    {
        switch (bytesPer) {
            case 1:
                ((MutableByteList)list).forEach(new ByteProcedure() {
                    public void value(byte each) { procedure.value(MASK32 & each); }
                });
                break;
            case 2:
                ((MutableShortList)list).forEach(new ShortProcedure() {
                    public void value(short each) { procedure.value(MASK32 & each); }
                });
                break;
            case 4:
                ((MutableIntList)list).forEach(new IntProcedure() {
                    public void value(int each) { procedure.value(MASK32 & each); }
                });
                break;
            default:
                ((MutableLongList)list).forEach(procedure);
                break;
        }
        return this;
    }

    public Region forEach(final IntProcedure procedure)
    {
        switch (bytesPer) {
            case 1:
                ((MutableByteList)list).forEach(new ByteProcedure() {
                    public void value(byte each) { procedure.value(MASK16 & each); }
                });
                break;
            case 2:
                ((MutableShortList)list).forEach(new ShortProcedure() {
                    public void value(short each) { procedure.value(MASK16 & each); }
                });
                break;
            case 4:
                ((MutableIntList)list).forEach(procedure);
                break;
            default:
                ((MutableLongList)list).forEach(new LongProcedure() {
                    public void value(long each) { procedure.value((int)each); }
                });
                break;
        }
        return this;
    }

    public Region forEach(final ShortProcedure procedure)
    {
        switch (bytesPer) {
            case 1:
                ((MutableByteList)list).forEach(new ByteProcedure() {
                    public void value(byte each) { procedure.value((short) (MASK8 & each)); }
                });
                break;
            case 2:
                ((MutableShortList)list).forEach(procedure);
                break;
            case 4:
                ((MutableIntList)list).forEach(new IntProcedure() {
                    public void value(int each) { procedure.value((short) each); }
                });
                break;
            default:
                ((MutableLongList)list).forEach(new LongProcedure() {
                    public void value(long each) { procedure.value((short) each); }
                });
                break;
        }
        return this;
    }

    public Region forEach(final ByteProcedure procedure)
    {
        switch (bytesPer) {
            case 1:
                ((MutableByteList)list).forEach(procedure);
                break;
            case 2:
                ((MutableShortList)list).forEach(new ShortProcedure() {
                    public void value(short each) { procedure.value((byte) each); }
                });
                break;
            case 4:
                ((MutableIntList)list).forEach(new IntProcedure() {
                    public void value(int each) { procedure.value((byte) each); }
                });
                break;
            default:
                ((MutableLongList)list).forEach(new LongProcedure() {
                    public void value(long each) { procedure.value((byte) each); }
                });
                break;
        }
        return this;
    }

    public Region forEachAlternating(final LongBooleanProcedure procedure)
    {
        switch (bytesPer) {
            case 1:
                ((MutableByteList)list).forEachWithIndex(new ByteIntProcedure() {
                    public void value(byte each, int index) {
                        procedure.value(MASK32 & each, index % 2 == 1);
                    }
                });
                break;
            case 2:
                ((MutableShortList)list).forEachWithIndex(new ShortIntProcedure() {
                    public void value(short each, int index) {
                        procedure.value(MASK32 & each, index % 2 == 1);
                    }
                });
                break;
            case 4:
                ((MutableIntList)list).forEachWithIndex(new IntIntProcedure() {
                    public void value(int each, int index) {
                        procedure.value(MASK32 & each, index % 2 == 1);
                    }
                });
                break;
            default:
                ((MutableLongList)list).forEachWithIndex(new LongIntProcedure() {
                    public void value(long each, int index) {
                        procedure.value(each, index % 2 == 1);
                    }
                });
                break;
        }
        return this;
    }

    public Region forEachAlternating(final IntBooleanProcedure procedure)
    {
        switch (bytesPer) {
            case 1:
                ((MutableByteList)list).forEachWithIndex(new ByteIntProcedure() {
                    public void value(byte each, int index) {
                        procedure.value(MASK16 & each, index % 2 == 1);
                    }
                });
                break;
            case 2:
                ((MutableShortList)list).forEachWithIndex(new ShortIntProcedure() {
                    public void value(short each, int index) {
                        procedure.value(MASK16 & each, index % 2 == 1);
                    }
                });
                break;
            case 4:
                ((MutableIntList)list).forEachWithIndex(new IntIntProcedure() {
                    public void value(int each, int index) {
                        procedure.value(each, index % 2 == 1);
                    }
                });
                break;
            default:
                ((MutableLongList)list).forEachWithIndex(new LongIntProcedure() {
                    public void value(long each, int index) {
                        procedure.value((int)each, index % 2 == 1);
                    }
                });
                break;
        }
        return this;
    }

    public Region forEachAlternating(final ShortBooleanProcedure procedure)
    {
        switch (bytesPer) {
            case 1:
                ((MutableByteList)list).forEachWithIndex(new ByteIntProcedure() {
                    public void value(byte each, int index) {
                        procedure.value((short) (MASK8 & each), index % 2 == 1);
                    }
                });
                break;
            case 2:
                ((MutableShortList)list).forEachWithIndex(new ShortIntProcedure() {
                    public void value(short each, int index) {
                        procedure.value(each, index % 2 == 1);
                    }
                });
                break;
            case 4:
                ((MutableIntList)list).forEachWithIndex(new IntIntProcedure() {
                    public void value(int each, int index) {
                        procedure.value((short) each, index % 2 == 1);
                    }
                });
                break;
            default:
                ((MutableLongList)list).forEachWithIndex(new LongIntProcedure() {
                    public void value(long each, int index) {
                        procedure.value((short) each, index % 2 == 1);
                    }
                });
                break;
        }
        return this;
    }


    public Region forEachAlternating(final ByteBooleanProcedure procedure)
    {
        switch (bytesPer) {
            case 1:
                ((MutableByteList)list).forEachWithIndex(new ByteIntProcedure() {
                    public void value(byte each, int index) {
                        procedure.value(each, index % 2 == 1);
                    }
                });
                break;
            case 2:
                ((MutableShortList)list).forEachWithIndex(new ShortIntProcedure() {
                    public void value(short each, int index) {
                        procedure.value((byte)each, index % 2 == 1);
                    }
                });
                break;
            case 4:
                ((MutableIntList)list).forEachWithIndex(new IntIntProcedure() {
                    public void value(int each, int index) {
                        procedure.value((byte) each, index % 2 == 1);
                    }
                });
                break;
            default:
                ((MutableLongList)list).forEachWithIndex(new LongIntProcedure() {
                    public void value(long each, int index) {
                        procedure.value((byte) each, index % 2 == 1);
                    }
                });
                break;
        }
        return this;
    }
}