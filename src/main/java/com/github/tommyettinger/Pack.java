package com.github.tommyettinger;

import com.googlecode.javaewah.EWAHCompressedBitmap;

/**
 * Created by Tommy Ettinger on 10/18/2015.
 */
public class Pack {
    private EWAHCompressedBitmap[] storage;
    private SpatialBounds bounds;
    public Pack()
    {
        this(new SpatialBounds());
    }
    public Pack(SpatialBounds bounds)
    {
        this.bounds = bounds;
        storage = new EWAHCompressedBitmap[bounds.storageRequired()];
        for (int i = 0; i < storage.length; i++) {
            storage[i] = new EWAHCompressedBitmap();
        }
    }
}
