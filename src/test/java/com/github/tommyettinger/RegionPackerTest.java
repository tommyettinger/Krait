package com.github.tommyettinger;

import org.junit.Test;

import java.nio.ByteBuffer;

import static com.github.tommyettinger.RegionPacker.*;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;


/**
 * Created by Tommy Ettinger on 10/1/2015.
 */
public class RegionPackerTest {

    public static short[] dataCross = unionPacked(rectangle(25, 2, 14, 60), rectangle(2, 25, 60, 14));
    @Test
    public void testBasics() {
        //printPacked(dataCross, 64, 64);
        assertArrayEquals(dataCross, unionPacked(rectangle(25, 2, 14, 60), rectangle(2, 25, 60, 14)));
        short[] singleNegative = negatePacked(unionPacked(rectangle(25, 2, 14, 60), rectangle(2, 25, 60, 14))),
                doubleNegative = negatePacked(singleNegative);
        assertArrayEquals(dataCross, doubleNegative);
    }

    public static int FOV_RANGE = 12;
    public static Radius RADIUS = Radius.SQUARE;

    public void printBits16(int n) {
        for (int i = 0x8000; i > 0; i >>= 1)
            System.out.print((n & i) > 0 ? 1 : 0);
    }

    public void printBits32(int n) {
        for (int i = 1 << 31; i != 0; i >>>= 1)
            System.out.print((n & i) != 0 ? 1 : 0);
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
        assertEquals(0, posToHilbert(0, 0));
        assertEquals(21845, posToHilbert(255, 0));
        assertEquals(65535, posToHilbert(0, 255));
        assertEquals(43690, posToHilbert(255, 255));

        assertEquals(43690, coordToHilbert(Coord.get(255, 255)));
        assertEquals(posToHilbert(255, 255), coordToHilbert(Coord.get(255, 255)));
        assertEquals(Coord.get(255, 255), hilbertToCoord(coordToHilbert(Coord.get(255, 255))));
    }
    public void testHilbertCurve3D() {
        for(int i : new int[]{0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,31,32,33,63,64,255,256,4092,4093,4094,4095})
            System.out.println("index " + i + ", x:" + hilbert3X[i] +
                    ", y:" + hilbert3Y[i] +
                    ", z:" + hilbert3Z[i]);
    }
    //@Test
    public void testMooreCurve3D() {
        for (int s = 0; s < 12; s++) {

            for (int i0 : new int[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 31, 32, 33, 63, 64, 255, 256, 511, 512, 1023, 1024, 4092, 4093, 4094, 4095}) {
                int i = i0 + s * 4096;
                System.out.println("index " + i + ", sector " + (i >> 12) + ", x:" + getXMoore3D(i, 3) +
                        ", y:" + getYMoore3D(i, 3) +
                        ", z:" + getZMoore3D(i, 3));
            }
        }
    }
    @Test
    public void testPHCurve3D() {
        assertEquals (ph3Distances[0], 0);
        for (int i = 1; i < 64000; i++) {
            assertEquals ((ph3Distances[ph3X[i] * 1600 + ph3Y[i] * 40 + ph3Z[i]] & 0xffff), i);
            assertEquals(Math.abs(ph3X[i] - ph3X[i - 1] + ph3Y[i] - ph3Y[i - 1] + ph3Z[i] - ph3Z[i - 1]), 1);
        }

    }
    //@Test
    public void testMooreCurve() {
        for (int i = 0; i < 256; i++) {
            System.out.println("index " + i + "x:" + mooreX[i] + ", y:" + mooreY[i] +
            ", dist:" + mooreDistances[mooreX[i] + (mooreY[i] << 4)]);
        }
    }

    @Test
    public void testTranslate() {
        short[] packed = new short[]{0, 4}, squashed = new short[]{0, 1};
        short[] translated = translate(packed, -2, -2, 60, 60);
        assertArrayEquals(squashed, translated);


        /*
        false true
        true  false
         */
        /* MOVE OVER, X 1, limit width to 2
        false true
        false true
         */
        boolean[][] grid = new boolean[][]{new boolean[]{false, true}, new boolean[]{true, false}};
        boolean[][] grid2 = new boolean[][]{new boolean[]{false, false}, new boolean[]{true, true}};
        short[] packed2 = pack(grid), packed3 = pack(grid2);

        short[] translated2 = translate(packed2, 1, 0, 2, 2);
        assertArrayEquals(packed3, translated2);
        short[] crossZeroTranslated = translate(dataCross, 0, 0, 64, 64);
        short[] crossTranslated = translate(dataCross, 1, 1, 64, 64);
        short[] crossUnTranslated = translate(crossTranslated, -1, -1, 64, 64);

        assertArrayEquals(dataCross, crossZeroTranslated);
        assertArrayEquals(dataCross, crossUnTranslated);

        short[] crossBox = translate(translate(dataCross, 25, 25, 64, 64), -50, -50, 64, 64);
        //printPacked(crossBox, 64, 64);
        assertArrayEquals(crossBox, rectangle(14, 14));
    }

    @Test
    public void testUnion() {
        short[] union = unionPacked(new short[]{300, 5, 6, 8, 2, 4}, new short[]{290, 12, 9, 1});
        // 300, 5, 6, 8, 2, 4
        // 290, 12, 9, 1
        // =
        // 290, 15, 6, 8, 2, 4
        /*
        System.out.println("Union: ");
        for (int i = 0; i < union.length; i++) {
            System.out.print(union[i] + ", ");
        }
        System.out.println();
        */
        assertArrayEquals(new short[]{290, 15, 6, 8, 2, 4}, union);

        union = unionPacked(new short[]{300, 5, 6, 8, 2, 4}, new short[]{290, 10, 10, 1});
        /*
        System.out.println("Union: ");
        for (int i = 0; i < union.length; i++) {
            System.out.print(union[i] + ", ");
        }
        System.out.println();
        */
        assertArrayEquals(new short[]{290, 15, 5, 9, 2, 4}, union);

        short[] intersect = intersectPacked(new short[]{300, 5, 6, 8, 2, 4}, new short[]{290, 12, 9, 1});
        // 300, 5, 6, 8, 2, 4
        // 290, 12, 9, 1
        // =
        // 300, 2, 9, 1
        /*
        System.out.println("Intersect: ");
        for (int i = 0; i < intersect.length; i++) {
            System.out.print(intersect[i] + ", ");
        }
        System.out.println();
        */
        assertArrayEquals(new short[]{300, 2, 9, 1}, intersect);

        intersect = intersectPacked(new short[]{300, 5, 6, 8, 2, 4}, new short[]{290, 10, 11, 1});
        /*
        System.out.println("Intersect: ");
        for (int i = 0; i < intersect.length; i++) {
            System.out.print(intersect[i] + ", ");
        }
        System.out.println();
        */
        assertArrayEquals(new short[]{311, 1}, intersect);

        /*
        StatefulRNG rng = new StatefulRNG(new LightRNG(0xAAAA2D2));

        DungeonGenerator dungeonGenerator = new DungeonGenerator(60, 60, rng);
        char[][] map = dungeonGenerator.generate();
        short[] floors = pack(map, '.');
        Coord viewer = dungeonGenerator.utility.randomCell(floors);
        FOV fov = new FOV(FOV.SHADOW);
        double[][] seen = fov.calculateFOV(DungeonUtility.generateResistances(map), viewer.x, viewer.y,
                FOV_RANGE, RADIUS);
        short[] visible = pack(seen);

        short[] fringe = fringe(visible, 1, 60, 60);
        printPacked(fringe, 60, 60);


        short[][] fringes = fringes(visible, 6, 60, 60);
        for (int i = 0; i < 6; i++) {
            printPacked(intersectPacked(fringes[i], floors), 60, 60);
        }
        */
        short[] box = translate(translate(translate(dataCross, 25, 25, 64, 64), -50, -50, 64, 64), 25, 25, 64, 64);
        assertArrayEquals(box, intersectPacked(rectangle(25, 2, 14, 60), rectangle(2, 25, 60, 14)));
        short[] minus = differencePacked(dataCross, box);
        short[] xor = xorPacked(rectangle(25, 2, 14, 60), rectangle(2, 25, 60, 14));
        assertArrayEquals(minus, xor);

        short[] edge = fringe(dataCross, 1, 64, 64);
        //printPacked(edge, 64, 64);
        short[] bonus = expand(dataCross, 1, 64, 64);
        //printPacked(bonus, 64, 64);
        assertArrayEquals(differencePacked(bonus, edge), dataCross);
        short[] flooded = flood(dataCross, packSeveral(Coord.get(26, 2)), 2);
        short[] manual = packSeveral(Coord.get(25, 2), Coord.get(26, 2), Coord.get(27, 2), Coord.get(28, 2),
                Coord.get(25, 3), Coord.get(26, 3), Coord.get(27, 3),
                Coord.get(26, 4));
        //printPacked(flooded, 64, 64);
        assertArrayEquals(flooded, manual);
    }
    /*
    @Test
    public void testFloodRadiate()
    {
        /*
        short[] flooded = flood(dataCross, packSeveral(Coord.get(26, 2)), 2);
        short[] manual = packSeveral(Coord.get(25, 2), Coord.get(26, 2), Coord.get(27, 2), Coord.get(28, 2),
                Coord.get(25, 3), Coord.get(26, 3), Coord.get(27, 3),
                Coord.get(26, 4));
        //printPacked(flooded, 64, 64);
        assertArrayEquals(flooded, manual);
        * /
        for (int i = 10; i < 50; i++) {
            for (int j = 0; j < 10; j++) {
                short[] radiated = radiate(removeSeveralPacked(dataCross, Coord.get(28+j, i), Coord.get(27+j, i+1), Coord.get(28+j, i+1)), packOne(26, 23), 10);
                count(radiated);
            }
        }
        //printPacked(radiated, 64, 64);
    }
    */
    @Test
    public void testRadiate()
    {
        short[] groupFOVed = radiate(removeSeveralPacked(dataCross, Coord.get(30, 25), Coord.get(29, 26), Coord.get(30, 26)), packOne(26, 23), 10);
        printPacked(groupFOVed, 64, 64);
    }


    public static void generateHilbert() {
        int sideLength = (1 << 8);
        int capacity = sideLength * sideLength;
        short[] out = new short[capacity];// xOut = new short[capacity], yOut = new short[capacity];
        /*
        Coord c;
        for (int i = 0; i < capacity; i++) {
            c = hilbertToCoord(i);
            xOut[i] = (short) c.x;
            yOut[i] = (short) c.y;
        }

        for (int y = 0; y < sideLength; y++) {
            for (int x = 0; x < sideLength; x++) {
                out[y * sideLength + x] = (short) posToHilbert(x, y);
            }
        }
        try {
            FileChannel channel = new FileOutputStream("target/distance").getChannel();
            channel.write(shortsToBytes(out));
            channel.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        */
        /*
        try {
            FileChannel channel = new FileOutputStream("target/hilbertx").getChannel();
            channel.write(shortsToBytes(xOut));
            channel.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            FileChannel channel = new FileOutputStream("target/hilberty").getChannel();
            channel.write(shortsToBytes(yOut));
            channel.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        */
    }
/*
StringBuilder text = new StringBuilder(0xffff * 11);
        text.append("private static final short[] hilbertX = new short[] {\n");
        text.append(xOut[0]);
        for (int i = 1, ln = 0; i < capacity; i++, ln +=4) {
            text.append(',');
            if(ln > 75)
            {
                ln = 0;
                text.append('\n');
            }
            text.append(xOut[i]);

        }
        text.append("\n},\n");
        text.append("hilbertY = new short[] {\n");
        text.append(yOut[0]);
        for (int i = 1, ln = 0; i < capacity; i++, ln +=4) {
            text.append(',');
            if(ln > 75)
            {
                ln = 0;
                text.append('\n');
            }
            text.append(yOut[i]);

        }
        text.append("\n}\n\n");
 */
    public static ByteBuffer shortsToBytes(short[] arr) {
        java.nio.ByteBuffer bb = java.nio.ByteBuffer.allocate(arr.length * 2);
        bb.asShortBuffer().put(arr);
        return bb;
    }
}