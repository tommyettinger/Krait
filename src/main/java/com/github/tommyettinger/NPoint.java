package com.github.tommyettinger;

/**
 * Created by Tommy Ettinger on 10/18/2015.
 */
public class NPoint {
    protected final long[] coordinates;

    public NPoint(long[] coordinates) {
        this.coordinates = coordinates.clone();
    }

    public long at(int dimension) {
        return coordinates[dimension];
    }

    public NPoint validate(SpatialBounds bounds)
    {
        return new NPoint(bounds.clamp(coordinates));
    }

    public NPoint validateWrapAround(SpatialBounds bounds)
    {
        return new NPoint(bounds.cycle(coordinates));
    }
}
