package com.github.tommyettinger;

import com.googlecode.javaewah32.EWAHCompressedBitmap32;
import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;


/**
 * Created by Tommy Ettinger on 10/1/2015.
 */
public class RegionPackerTest {

    public static PukaHilbert1280Strategy ph1280 = new PukaHilbert1280Strategy();
    public static Hilbert2DStrategy hilbert = new Hilbert2DStrategy(256);
    public static RegionPacker rp = new RegionPacker(hilbert), rp3 = new RegionPacker(ph1280);
    public static EWAHCompressedBitmap32 dataCross = rp.union(
            rp.rectangle(new int[]{25, 2}, new int[] {14 + 25, 60 + 2}), rp.rectangle(new int[] {2, 25}, new int[] {60 + 2, 14 + 25}));
    public static int[] crossBounds = new int[]{64, 64};
    public static int[] point(int... coordinates)
    {
        return coordinates;
    }
    @Test
    public void testBasics() {
        //printPacked(dataCross, 64, 64);
        assertEquals(dataCross, rp.union(rp.rectangle(new int[]{25, 2}, new int[] {14 + 25, 60 + 2}),
                rp.rectangle(new int[] {2, 25}, new int[] {60 + 2, 14 + 25})));
        EWAHCompressedBitmap32 singleNegative = rp.negate(rp.union(rp.rectangle(new int[]{25, 2}, new int[] {14 + 25, 60 + 2}),
                rp.rectangle(new int[] {2, 25}, new int[] {60 + 2, 14 + 25}))),
                doubleNegative = rp.negate(singleNegative);
        assertEquals(dataCross, doubleNegative);
    }

    public void printBits16(int n) {
        for (int i = 0x8000; i > 0; i >>= 1)
            System.out.print((n & i) > 0 ? 1 : 0);
    }

    public void printBits32(int n) {
        for (int i = 1 << 31; i != 0; i >>>= 1)
            System.out.print((n & i) != 0 ? 1 : 0);
    }
    public static void print2D(EWAHCompressedBitmap32 area)
    {
        for (int y = 0; y < 64; y++) {
            for (int x = 0; x < 64; x++) {
                System.out.print(area.get(hilbert.distance(x, y)) ? '.' : '#');
            }
            System.out.println();
        }
    }
    public static void printOverlay(EWAHCompressedBitmap32 area1, EWAHCompressedBitmap32 area2)
    {
        for (int y = 0; y < 64; y++) {
            for (int x = 0; x < 64; x++) {
                int which = area1.get(hilbert.distance(x, y)) ? 1 : 0;
                which |= area2.get(hilbert.distance(x, y)) ? 2 : 0;
                switch (which)
                {
                    case 1:
                        System.out.print('.');
                        break;
                    case 2:
                        System.out.print('2');
                        break;
                    case 3:
                        System.out.print('!');
                        break;
                    default:
                        System.out.print('#');
                }
            }
            System.out.println();
        }
    }

    public long arrayMemoryUsage(int length, long bytesPerItem)
    {
        return (((bytesPerItem * length + 12 - 1) / 8) + 1) * 8L;
    }
    public long arrayMemoryUsage2D(int xSize, int ySize, long bytesPerItem)
    {
        return arrayMemoryUsage(xSize, (((bytesPerItem * ySize + 12 - 1) / 8) + 1) * 8L);
    }
    public int arrayMemoryUsageJagged(short[][] arr)
    {
        int ctr = 0;
        for (int i = 0; i < arr.length; i++) {
            ctr += arrayMemoryUsage(arr[i].length, 2);
        }
        return (((ctr + 12 - 1) / 8) + 1) * 8;
    }

    @Test
    public void testHilbertCurve() {
        Hilbert2DStrategy h = new Hilbert2DStrategy(256);
        assertEquals(0, h.distance(0, 0));
        assertEquals(21845, h.distance(255, 0));
        assertEquals(65535, h.distance(0, 255));
        assertEquals(43690, h.distance(255, 255));

        assertArrayEquals(new int[]{255,255}, h.point(h.distance(255, 255)));
    }
    @Test
    public void testHilbertCurve3D() {
        HilbertGeneralStrategy h = new HilbertGeneralStrategy(3, 16);
        for(int i : new int[]{0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,31,32,33,63,64,255,256,4092,4093,4094,4095})
            System.out.println("index " + i + ", x:" + h.coordinate(i, 0) +
                    ", y:" + h.coordinate(i, 1) +
                    ", z:" + h.coordinate(i, 2));
    }
    @Test
    public void testMooreCurve3D() {
        MooreGeneralStrategy m = new MooreGeneralStrategy(3, 16, 2, 3);
        for (int s = 0; s < 12; s++) {

            for (int i0 : new int[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 31, 32, 33, 63, 64, 255, 256, 511, 512, 1023, 1024, 4092, 4093, 4094, 4095}) {
                int i = i0 + s * 4096;
                System.out.println("index " + i + ", sector " + (i >> 12) + ", x:" + m.coordinate(i, 0) +
                        ", y:" + m.coordinate(i, 1) +
                        ", z:" + m.coordinate(i, 2));
            }
        }
    }
    @Test
    public void testPHCurve3D() {
        PukaHilbert40Strategy ph = new PukaHilbert40Strategy();
        assertEquals (ph.distance(0, 0, 0), 0);
        for (int i = 1; i < 64000; i++) {
            assertEquals (ph.distance(ph.point(i)), i);
            assertEquals(Math.abs(ph.coordinate(i, 0) - ph.coordinate(i - 1, 0))
                    + Math.abs(ph.coordinate(i, 1) - ph.coordinate(i - 1, 1))
                    + Math.abs(ph.coordinate(i, 2) - ph.coordinate(i - 1, 2)), 1);
        }

    }
    @Test
    public void testMooreCurve() {
        MooreGeneralStrategy m = new MooreGeneralStrategy(2, 8, 0, 2);
        for (int i = 0; i < 256; i++) {
            System.out.println("index " + i + "x:" + m.coordinate(i, 0) + ", y:" + m.coordinate(i, 1) +
            ", dist:" + m.distance(m.point(i)));
        }
    }

    @Test
    public void testTranslate() {
        EWAHCompressedBitmap32 crossZeroTranslated = rp.translate(dataCross, crossBounds, new int[]{0, 0});
        EWAHCompressedBitmap32 crossTranslated = rp.translate(dataCross, crossBounds, new int[]{1, 1});
        EWAHCompressedBitmap32 crossUnTranslated = rp.translate(crossTranslated, crossBounds, new int[]{-1, -1});

        assertEquals(dataCross, crossZeroTranslated);
        assertEquals(dataCross, crossUnTranslated);

        EWAHCompressedBitmap32 crossBox = rp.translate(rp.translate(dataCross, crossBounds, new int[]{25, 25}),
                crossBounds, new int[]{-50, -50});
        //printPacked(crossBox, 64, 64);
        assertEquals(crossBox, rp.rectangle(new int[]{14, 14}));
    }

    @Test
    public void testUnion() {
        EWAHCompressedBitmap32 box = rp.translate(
                rp.translate(
                        rp.translate(dataCross, crossBounds, new int[]{25, 25})
                        , crossBounds, new int[]{-50, -50}),
                crossBounds, new int[]{25, 25});
        assertEquals(box, rp.intersect(rp.rectangle(new int[]{25, 2}, new int[]{14 + 25, 60 + 2}), rp.rectangle(new int[]{2, 25}, new int[]{60 + 2, 14 + 25})));
        EWAHCompressedBitmap32 minus = rp.difference(dataCross, box);
        EWAHCompressedBitmap32 xor = rp.xor(rp.rectangle(new int[]{25, 2}, new int[]{14 + 25, 60 + 2}), rp.rectangle(new int[]{2, 25}, new int[]{60 + 2, 14 + 25}));
        assertEquals(minus, xor);

        EWAHCompressedBitmap32 edge = rp.fringe(dataCross, 1, crossBounds);
        //printPacked(edge, 64, 64);
        EWAHCompressedBitmap32 bonus = rp.expand(dataCross, 1, crossBounds);
        //printPacked(bonus, 64, 64);
        assertEquals(rp.difference(bonus, edge), dataCross);
        EWAHCompressedBitmap32 flooded = rp.flood(rp.packOne(26, 2), dataCross, 2);
        EWAHCompressedBitmap32 manual = rp.packSeveral(point(25, 2), point(26, 2), point(27, 2), point(28, 2),
                point(25, 3), point(26, 3), point(27, 3),
                point(26, 4));
        //printPacked(flooded, 64, 64);
        assertEquals(flooded, manual);
    }

    @Test
    public void testRandomFlood() {
        RNG rng = new RNG(0x1337BEEFBABEL);
        EWAHCompressedBitmap32 flooded = rp.randomFlood(rp.packOne(26, 2), dataCross, 20, rng);
        print2D(flooded);
        System.out.println();
        EWAHCompressedBitmap32 flooded2 = rp.randomFlood(flooded, dataCross, 40, rng);
        printOverlay(flooded, flooded2);
    }

    @Test
    public void testFilling() {
        //EWAHCompressedBitmap32 dc2 = rp.retract(dataCross, 1, crossBounds, Metric.CHEBYSHEV);
        //print2D(dc2);
        //System.out.println();

        EWAHCompressedBitmap32 corners = rp.gap(dataCross, crossBounds, Metric.CHEBYSHEV);
        print2D(corners);
        System.out.println();
        corners = rp.gap(dataCross, crossBounds, 2, Metric.CHEBYSHEV);
        print2D(corners);
        System.out.println();
        printOverlay(dataCross, corners);
    }
    /*
    @Test
    public void testFloodRadiate()
    {
        /*
        EWAHCompressedBitmap32 flooded = flood(dataCross, packSeveral(Coord.get(26, 2)), 2);
        EWAHCompressedBitmap32 manual = packSeveral(Coord.get(25, 2), Coord.get(26, 2), Coord.get(27, 2), Coord.get(28, 2),
                Coord.get(25, 3), Coord.get(26, 3), Coord.get(27, 3),
                Coord.get(26, 4));
        //printPacked(flooded, 64, 64);
        assertArrayEquals(flooded, manual);
        * /
        for (int i = 10; i < 50; i++) {
            for (int j = 0; j < 10; j++) {
                EWAHCompressedBitmap32 radiated = radiate(removeSeveralPacked(dataCross, Coord.get(28+j, i), Coord.get(27+j, i+1), Coord.get(28+j, i+1)), packOne(26, 23), 10);
                count(radiated);
            }
        }
        //printPacked(radiated, 64, 64);
    }
    */
    /*
    @Test
    public void testRadiate()
    {
        EWAHCompressedBitmap32 groupFOVed = radiate(removeSeveralPacked(dataCross, Coord.get(30, 25), Coord.get(29, 26), Coord.get(30, 26)), packOne(26, 23), 10);
        printPacked(groupFOVed, 64, 64);
    }
    */
    public static int boundedIndex(int[] bounds, int... point)
    {
        int u = 0;
        for (int a = 0; a < bounds.length; a++) {
            if(point[a] >= bounds[a])
                return -1;
            else
            {
                u *= bounds[a];
                u += point[a];
            }
        }
        return u;
    }

    public static int[] fromBounded(int[] bounds, int index)
    {
        int[] point = new int[bounds.length];
        int u = 1;
        for (int a = bounds.length - 1; a >= 0; a--) {
            point[a] = (index / u) % bounds[a];
            u *= bounds[a];
        }
        System.out.print(point[0]);

        for (int a = 1; a < bounds.length; a++) {
            System.out.print(", " + point[a]);
        }
        System.out.println();
        return point;
    }

    //@Test
    public void testBounds()
    {
        int[] bounds = new int[]{10, 20, 30};
        System.out.println(boundedIndex(bounds, 0, 0, 0));
        System.out.println(boundedIndex(bounds, 1, 2, 3));
        System.out.println(boundedIndex(bounds, 5, 6, 7));
        fromBounded(bounds, 0);
        fromBounded(bounds, 663);
        fromBounded(bounds, 3187);
    }

    @Test
    public void testLinear()
    {
        int[][] foo = new int[8][8];
        foo[1][1] = 1;
        foo[2][1] = 2;
        foo[2][2] = 4;
        LinearData l = new LinearData(foo, Checks.greaterInt(1), 8);
        EWAHCompressedBitmap32 packed = rp.pack(l);
        print2D(packed);
    }

}