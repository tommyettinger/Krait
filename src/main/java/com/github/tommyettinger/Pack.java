package com.github.tommyettinger;

/**
 * Created by Tommy Ettinger on 10/18/2015.
 */
public class Pack {
    private SpatialBounds bounds;
    public Pack()
    {
        this(new SpatialBounds());
    }
    public Pack(SpatialBounds bounds)
    {
        this.bounds = bounds;
    }
}
