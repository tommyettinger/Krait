package com.github.tommyettinger;

/**
 *
 * Created by Tommy Ettinger on 10/18/2015.
 */
public class SpatialBounds {
    /**
     * An array of maximum bits to use per dimension
     */
    private final int[] dimensionality;
    private final long[] sizeLimits;

    private final int rank;
    private final int maxBits;
    private final double classifier;
    public SpatialBounds()
    {
        this(8, 8);
    }
    public SpatialBounds(int... bitLengths)
    {
        dimensionality = bitLengths;
        rank = dimensionality.length;
        sizeLimits = new long[rank];
        int maxBits0 = 0;
        double classifier0 = 0;
        int dim;
        for (int i = 0; i < rank; i++) {
            dim = dimensionality[i];
            sizeLimits[i] = 1L << (dim - 1);
            maxBits0 += dim;
            classifier0 += dim * Math.log1p(classifier0 + 0.2357);
        }
        maxBits = maxBits0;
        classifier = classifier0;
    }
    public SpatialBounds(SpatialBounds other)
    {
        dimensionality = other.dimensionality;
        maxBits = other.maxBits;
        rank = other.rank;
        classifier = other.classifier;
        sizeLimits = other.sizeLimits;
        /*
        storage = new EWAHCompressedBitmap[other.storage.length];
        for (int i = 0; i < storage.length; i++) {
            try {
                storage[i] = other.storage[i].clone();
            } catch (CloneNotSupportedException e) {
                e.printStackTrace();
                storage[i] = new EWAHCompressedBitmap();
            }
        }*/
    }
    private SpatialBounds(int[] dimensionality, long[] sizeLimits, int rank, int maxBits, double classifier)
    {
        this.dimensionality = dimensionality;
        this.maxBits = maxBits;
        this.rank = rank;
        this.classifier = classifier;
        this.sizeLimits = sizeLimits;
    }

    public int storageRequired()
    {
        return (maxBits - 1) / 31 + 1;
    }
    public long maxSizeAt(int dimension)
    {
        return sizeLimits[dimension];
    }

    public long[] clamp(long... positions)
    {
        long[] temp = new long[rank];
        for (int i = 0; i < rank && i < positions.length; i++) {
            temp[i] = Math.min(Math.max(0, positions[i]), sizeLimits[i] - 1);
        }
        return temp;
    }
    public long[] cycle(long... positions)
    {
        long[] temp = new long[rank];
        for (int i = 0; i < rank && i < positions.length; i++) {
            temp[i] = positions[i] % sizeLimits[i];
        }
        return temp;
    }
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SpatialBounds that = (SpatialBounds) o;

        return rank == that.rank && Double.compare(that.classifier, classifier) == 0;

    }

    @Override
    public int hashCode() {
        int result;
        long temp;
        result = rank;
        temp = Double.doubleToLongBits(classifier);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        return result;
    }
}
