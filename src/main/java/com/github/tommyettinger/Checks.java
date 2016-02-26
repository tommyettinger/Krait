package com.github.tommyettinger;

import java.util.Arrays;
import java.util.Collection;

/**
 * Contains pre-defined Check and CheckArray implementations, and methods that can produce adapted versions for various
 * purposes.
 * Created by Tommy Ettinger on 2/25/2016.
 */
public class Checks {
    public static Check<Number> isZero = n -> n.doubleValue() == 0.0;
    public static Check<Number> isNonZero = n -> n.doubleValue() != 0.0;
    public static Check<Collection<?>> isNonEmpty = c -> !c.isEmpty();

    public static <T> Check<T> not(Check<T> check)
    {
        return t -> !check.test(t);
    }
    public static <T> Check<T> and(Check<T> first, Check<T> second)
    {
        return t -> first.test(t) && second.test(t);
    }

    public static <T> Check<T> or(Check<T> first, Check<T> second)
    {
        return t -> first.test(t) || second.test(t);
    }

    public static <T> Check<T> xor(Check<T> first, Check<T> second)
    {
        return t -> first.test(t) ^ second.test(t);
    }

    public static <T> Check<T> andNot(Check<T> first, Check<T> second)
    {
        return t -> first.test(t) && !second.test(t);
    }

    public static CheckArray<byte[]> zeroByte = a -> {boolean[] b = new boolean[a.length];
        for (int i = 0; i < a.length; i++) {
            b[i] = a[i] == 0;
        }
        return b;
    };
    public static CheckArray<byte[]> nonZeroByte = a -> {boolean[] b = new boolean[a.length];
        for (int i = 0; i < a.length; i++) {
            b[i] = a[i] != 0;
        }
        return b;
    };
    public static CheckArray<short[]> zeroShort = a -> {boolean[] b = new boolean[a.length];
        for (int i = 0; i < a.length; i++) {
            b[i] = a[i] == 0;
        }
        return b;
    };
    public static CheckArray<short[]> nonZeroShort = a -> {boolean[] b = new boolean[a.length];
        for (int i = 0; i < a.length; i++) {
            b[i] = a[i] != 0;
        }
        return b;
    };
    public static CheckArray<int[]> zeroInt = a -> {boolean[] b = new boolean[a.length];
        for (int i = 0; i < a.length; i++) {
            b[i] = a[i] == 0;
        }
        return b;
    };
    public static CheckArray<int[]> nonZeroInt = a -> {boolean[] b = new boolean[a.length];
        for (int i = 0; i < a.length; i++) {
            b[i] = a[i] != 0;
        }
        return b;
    };
    public static CheckArray<long[]> zeroLong = a -> {boolean[] b = new boolean[a.length];
        for (int i = 0; i < a.length; i++) {
            b[i] = a[i] == 0;
        }
        return b;
    };
    public static CheckArray<long[]> nonZeroLong = a -> {boolean[] b = new boolean[a.length];
        for (int i = 0; i < a.length; i++) {
            b[i] = a[i] != 0;
        }
        return b;
    };
    public static CheckArray<double[]> zeroDouble = a -> {boolean[] b = new boolean[a.length];
        for (int i = 0; i < a.length; i++) {
            b[i] = a[i] == 0.0;
        }
        return b;
    };
    public static CheckArray<double[]> nonZeroDouble = a -> {boolean[] b = new boolean[a.length];
        for (int i = 0; i < a.length; i++) {
            b[i] = a[i] != 0.0;
        }
        return b;
    };
    public static CheckArray<float[]> zeroFloat = a -> {boolean[] b = new boolean[a.length];
        for (int i = 0; i < a.length; i++) {
            b[i] = a[i] == 0f;
        }
        return b;
    };
    public static CheckArray<float[]> nonZeroFloat = a -> {boolean[] b = new boolean[a.length];
        for (int i = 0; i < a.length; i++) {
            b[i] = a[i] != 0f;
        }
        return b;
    };
    public static CheckArray<byte[]> matchByte(final byte that)
    {
        return a -> {boolean[] b = new boolean[a.length];
            for (int i = 0; i < a.length; i++) {
                b[i] = a[i] == that;
            }
            return b;
        };
    }
    public static CheckArray<short[]> matchShort(final short that)
    {
        return a -> {boolean[] b = new boolean[a.length];
            for (int i = 0; i < a.length; i++) {
                b[i] = a[i] == that;
            }
            return b;
        };
    }
    public static CheckArray<int[]> matchInt(final int that)
    {
        return a -> {boolean[] b = new boolean[a.length];
            for (int i = 0; i < a.length; i++) {
                b[i] = a[i] == that;
            }
            return b;
        };
    }
    public static CheckArray<long[]> matchLong(final long that)
    {
        return a -> {boolean[] b = new boolean[a.length];
            for (int i = 0; i < a.length; i++) {
                b[i] = a[i] == that;
            }
            return b;
        };
    }
    public static CheckArray<float[]> matchFloat(final float that)
    {
        return a -> {boolean[] b = new boolean[a.length];
            for (int i = 0; i < a.length; i++) {
                b[i] = a[i] == that;
            }
            return b;
        };
    }
    public static CheckArray<double[]> matchDouble(final double that)
    {
        return a -> {boolean[] b = new boolean[a.length];
            for (int i = 0; i < a.length; i++) {
                b[i] = a[i] == that;
            }
            return b;
        };
    }

    public static CheckArray<byte[]> lessByte(final byte that)
    {
        return a -> {boolean[] b = new boolean[a.length];
            for (int i = 0; i < a.length; i++) {
                b[i] = a[i] < that;
            }
            return b;
        };
    }
    public static CheckArray<short[]> lessShort(final short that)
    {
        return a -> {boolean[] b = new boolean[a.length];
            for (int i = 0; i < a.length; i++) {
                b[i] = a[i] < that;
            }
            return b;
        };
    }
    public static CheckArray<int[]> lessInt(final int that)
    {
        return a -> {boolean[] b = new boolean[a.length];
            for (int i = 0; i < a.length; i++) {
                b[i] = a[i] < that;
            }
            return b;
        };
    }
    public static CheckArray<long[]> lessLong(final long that)
    {
        return a -> {boolean[] b = new boolean[a.length];
            for (int i = 0; i < a.length; i++) {
                b[i] = a[i] < that;
            }
            return b;
        };
    }
    public static CheckArray<float[]> lessFloat(final float that)
    {
        return a -> {boolean[] b = new boolean[a.length];
            for (int i = 0; i < a.length; i++) {
                b[i] = a[i] < that;
            }
            return b;
        };
    }
    public static CheckArray<double[]> lessDouble(final double that)
    {
        return a -> {boolean[] b = new boolean[a.length];
            for (int i = 0; i < a.length; i++) {
                b[i] = a[i] < that;
            }
            return b;
        };
    }
    public static CheckArray<float[]> lessOrEqualFloat(final float that)
    {
        return a -> {boolean[] b = new boolean[a.length];
            for (int i = 0; i < a.length; i++) {
                b[i] = a[i] <= that;
            }
            return b;
        };
    }
    public static CheckArray<double[]> lessOrEqualDouble(final double that)
    {
        return a -> {boolean[] b = new boolean[a.length];
            for (int i = 0; i < a.length; i++) {
                b[i] = a[i] <= that;
            }
            return b;
        };
    }

    public static CheckArray<byte[]> greaterByte(final byte that)
    {
        return a -> {boolean[] b = new boolean[a.length];
            for (int i = 0; i < a.length; i++) {
                b[i] = a[i] > that;
            }
            return b;
        };
    }
    public static CheckArray<short[]> greaterShort(final short that)
    {
        return a -> {boolean[] b = new boolean[a.length];
            for (int i = 0; i < a.length; i++) {
                b[i] = a[i] > that;
            }
            return b;
        };
    }
    public static CheckArray<int[]> greaterInt(final int that)
    {
        return a -> {boolean[] b = new boolean[a.length];
            for (int i = 0; i < a.length; i++) {
                b[i] = a[i] > that;
            }
            return b;
        };
    }
    public static CheckArray<long[]> greaterLong(final long that)
    {
        return a -> {boolean[] b = new boolean[a.length];
            for (int i = 0; i < a.length; i++) {
                b[i] = a[i] > that;
            }
            return b;
        };
    }
    public static CheckArray<float[]> greaterFloat(final float that)
    {
        return a -> {boolean[] b = new boolean[a.length];
            for (int i = 0; i < a.length; i++) {
                b[i] = a[i] > that;
            }
            return b;
        };
    }
    public static CheckArray<double[]> greaterDouble(final double that)
    {
        return a -> {boolean[] b = new boolean[a.length];
            for (int i = 0; i < a.length; i++) {
                b[i] = a[i] > that;
            }
            return b;
        };
    }
    public static CheckArray<float[]> greaterOrEqualFloat(final float that)
    {
        return a -> {boolean[] b = new boolean[a.length];
            for (int i = 0; i < a.length; i++) {
                b[i] = a[i] >= that;
            }
            return b;
        };
    }
    public static CheckArray<double[]> greaterOrEqualDouble(final double that)
    {
        return a -> {boolean[] b = new boolean[a.length];
            for (int i = 0; i < a.length; i++) {
                b[i] = a[i] >= that;
            }
            return b;
        };
    }

    public static CheckArray<char[]> matchChar(final char that)
    {
        return a -> {boolean[] b = new boolean[a.length];
            for (int i = 0; i < a.length; i++) {
                b[i] = a[i] < that;
            }
            return b;
        };
    }
    public static CheckArray<char[]> inChars(char... chars)
    {
        char[] container = Arrays.copyOf(chars, chars.length);
        Arrays.sort(container);
        return a -> {boolean[] b = new boolean[a.length];
            for (int i = 0; i < a.length; i++) {
                b[i] = Arrays.binarySearch(container, a[i]) >= 0;
            }
            return b;
        };
    }
}
