package com.github.tommyettinger;

import com.gs.collections.api.block.procedure.primitive.IntBooleanProcedure;
import com.gs.collections.api.block.procedure.primitive.IntIntProcedure;
import com.gs.collections.api.list.primitive.MutableIntList;
import com.gs.collections.impl.list.mutable.primitive.IntArrayList;

/**
 * The primary form of data produced by RegionPacker, meant to be primarily used by that class.
 * Created by Tommy Ettinger on 2/15/2016.
 */
public class RegionDIY extends IntArrayList {
    private static final short MASK8 = 0xff;
    private static final int MASK16  = 0xffff;
    private static final long MASK32 = 0xffffffffL;
    public RegionDIY()
    {
        super(64);
    }
    public RegionDIY(int capacity, boolean ignored)
    {
        super(capacity);
    }

    public RegionDIY(MutableIntList list)
    {
        super(list.toArray());
    }

    public RegionDIY(int... values)
    {
        super(values);
    }

    public void addRange(int start, int end)
    {
        if(end <= start)
            return;
        for (int i = start; i < end; i++) {
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
        return toArray();
    }

    public long[] asLongs()
    {
        return Conversion.toLongs(toArray());
    }

    public RegionDIY copy()
    {
        return new RegionDIY(this);
    }
    public IntArrayList onIndices()
    {
        RegionDIY list = new RegionDIY(size() * 4, true);
        boolean on = false;
        int idx = 0;
        for(int p = 0, g = get(p); p < size; ++p, on = !on, g = get(p)) {
            if (on) {
                list.addRange(idx, idx + g);
            }
            idx += g;
        }
        return list;
    }

    public RegionDIY forEachAlternating(final IntBooleanProcedure procedure)
    {
        forEachWithIndex(new IntIntProcedure() {
            public void value(int each, int index) {
                procedure.value(each, index % 2 == 1);
            }
        });

        return this;
    }


}