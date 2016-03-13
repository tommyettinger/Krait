package com.github.tommyettinger;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Created by Tommy Ettinger on 10/1/2015.
 */
public class CurveStrategyTest {

    public static void printPoint(int... coordinates)
    {
        if(coordinates == null || coordinates.length == 0)
            return;
        System.out.print("[" + coordinates[0]);
        for (int i = 1; i < coordinates.length; i++) {
            System.out.print(", " + coordinates[i]);
        }
        System.out.print("]");
    }
    public static void printlnPoint(int... coordinates)
    {
        if(coordinates == null || coordinates.length == 0)
            return;
        System.out.print("[" + coordinates[0]);
        for (int i = 1; i < coordinates.length; i++) {
            System.out.print(", " + coordinates[i]);
        }
        System.out.println("]");
    }
    public static void printlnPoint(String name, int... coordinates)
    {
        System.out.print(name + ": ");
        if(coordinates == null || coordinates.length == 0)
            return;
        System.out.print("[" + coordinates[0]);
        for (int i = 1; i < coordinates.length; i++) {
            System.out.print(", " + coordinates[i]);
        }
        System.out.println("]");
    }
    @Test
    public void testHilbert2D()
    {
        System.out.println("Hilbert Curve 2D");
        Hilbert2DStrategy hilbert = new Hilbert2DStrategy(123);
        System.out.println("Max distance: " + Long.toHexString(hilbert.maxDistance));
        int[] pt = hilbert.point(0);
        printlnPoint("Hilbert Order 7 position 0", pt);
        pt = hilbert.point(1);
        printlnPoint("Hilbert Order 7 position 1", pt);
        pt = hilbert.point(4);
        printlnPoint("Hilbert Order 7 position 4", pt);
        pt = hilbert.point(-1);
        printlnPoint("Hilbert Order 7 position end", pt);

        hilbert = new Hilbert2DStrategy(129);
        System.out.println("Max distance: " + Long.toHexString(hilbert.maxDistance));
        pt = hilbert.point(0);
        printlnPoint("Hilbert Order 8 position 0", pt);
        pt = hilbert.point(1);
        printlnPoint("Hilbert Order 8 position 1", pt);
        pt = hilbert.point(4);
        printlnPoint("Hilbert Order 8 position 4", pt);
        pt = hilbert.point(-1);
        printlnPoint("Hilbert Order 8 position end", pt);

        hilbert = new Hilbert2DStrategy(1000);
        System.out.println("Max distance: " + Long.toHexString(hilbert.maxDistance));
        pt = hilbert.point(0);
        printlnPoint("Hilbert Order 10 position 0", pt);
        pt = hilbert.point(1);
        printlnPoint("Hilbert Order 10 position 1", pt);
        pt = hilbert.point(4);
        printlnPoint("Hilbert Order 10 position 4", pt);
        pt = hilbert.point(-1);
        printlnPoint("Hilbert Order 10 position end", pt);

        /*
        hilbert = new Hilbert2DStrategy(2000);
        System.out.println("Max distance: " + Long.toHexString(hilbert.maxDistance));
        pt = hilbert.point(0);
        printlnPoint("Hilbert Order 11 position 0", pt);
        pt = hilbert.point(1);
        printlnPoint("Hilbert Order 11 position 1", pt);
        pt = hilbert.point(4);
        printlnPoint("Hilbert Order 11 position 4", pt);
        pt = hilbert.point(-1);
        printlnPoint("Hilbert Order 11 position end", pt);
        */
    }
    //247432622
    //201614111

    //195763046
    public void testHilbert2DSpeed()
    {
        long time = System.nanoTime(), l = 0L;
        //System.out.println("Hilbert Curve 2D");
        Hilbert2DStrategy hilbert = new Hilbert2DStrategy(0x200);
        //System.out.println("Max distance: " + Long.toHexString(hilbert.maxDistance));
        int[] pt = hilbert.point(0);
        //printlnPoint("Hilbert Order 7 position 0", pt);
        pt = hilbert.point(1);
        //printlnPoint("Hilbert Order 7 position 1", pt);
        pt = hilbert.point(4);
        //printlnPoint("Hilbert Order 7 position 4", pt);
        pt = hilbert.point(-1);
        //printlnPoint("Hilbert Order 7 position end", pt);

        for (int i = 0; i < hilbert.maxDistance; i++) {
            pt = hilbert.point(i);
            l ^= pt[0] ^ pt[1];
        }
/*
        hilbert = new Hilbert2DStrategy(129);
        //System.out.println("Max distance: " + Long.toHexString(hilbert.maxDistance));
        pt = hilbert.point(0);
        //printlnPoint("Hilbert Order 8 position 0", pt);
        pt = hilbert.point(1);
        //printlnPoint("Hilbert Order 8 position 1", pt);
        pt = hilbert.point(4);
        //printlnPoint("Hilbert Order 8 position 4", pt);
        pt = hilbert.point(-1);
        //printlnPoint("Hilbert Order 8 position end", pt);

        for (int i = 0; i < hilbert.maxDistance; i++) {
            pt = hilbert.point(i);
            l ^= pt[0] ^ pt[1];
        }

        hilbert = new Hilbert2DStrategy(1000);
        //System.out.println("Max distance: " + Long.toHexString(hilbert.maxDistance));
        pt = hilbert.point(0);
        //printlnPoint("Hilbert Order 10 position 0", pt);
        pt = hilbert.point(1);
        //printlnPoint("Hilbert Order 10 position 1", pt);
        pt = hilbert.point(4);
        //printlnPoint("Hilbert Order 10 position 4", pt);
        pt = hilbert.point(-1);
        //printlnPoint("Hilbert Order 10 position end", pt);

        for (int i = 0; i < hilbert.maxDistance; i++) {
            pt = hilbert.point(i);
            l ^= pt[0] ^ pt[1];
        }
*/
        System.out.println((System.nanoTime() - time) + ", " + l);
    }
    public void testHilbert3DSpeed()
    {
        long time = System.nanoTime(), l = 0L;
        //System.out.println("Hilbert Curve 2D");
        HilbertGeneralStrategy hilbert = new HilbertGeneralStrategy(3, 0x40);
        //System.out.println("Max distance: " + Long.toHexString(hilbert.maxDistance));
        int[] pt = hilbert.point(0);
        //printlnPoint("Hilbert Order 7 position 0", pt);
        pt = hilbert.point(1);
        //printlnPoint("Hilbert Order 7 position 1", pt);
        pt = hilbert.point(4);
        //printlnPoint("Hilbert Order 7 position 4", pt);
        pt = hilbert.point(-1);
        //printlnPoint("Hilbert Order 7 position end", pt);

        for (int i = 0; i < hilbert.maxDistance; i++) {
            pt = hilbert.point(i);
            l ^= pt[0] ^ pt[1];
        }
/*
        hilbert = new Hilbert2DStrategy(129);
        //System.out.println("Max distance: " + Long.toHexString(hilbert.maxDistance));
        pt = hilbert.point(0);
        //printlnPoint("Hilbert Order 8 position 0", pt);
        pt = hilbert.point(1);
        //printlnPoint("Hilbert Order 8 position 1", pt);
        pt = hilbert.point(4);
        //printlnPoint("Hilbert Order 8 position 4", pt);
        pt = hilbert.point(-1);
        //printlnPoint("Hilbert Order 8 position end", pt);

        for (int i = 0; i < hilbert.maxDistance; i++) {
            pt = hilbert.point(i);
            l ^= pt[0] ^ pt[1];
        }

        hilbert = new Hilbert2DStrategy(1000);
        //System.out.println("Max distance: " + Long.toHexString(hilbert.maxDistance));
        pt = hilbert.point(0);
        //printlnPoint("Hilbert Order 10 position 0", pt);
        pt = hilbert.point(1);
        //printlnPoint("Hilbert Order 10 position 1", pt);
        pt = hilbert.point(4);
        //printlnPoint("Hilbert Order 10 position 4", pt);
        pt = hilbert.point(-1);
        //printlnPoint("Hilbert Order 10 position end", pt);

        for (int i = 0; i < hilbert.maxDistance; i++) {
            pt = hilbert.point(i);
            l ^= pt[0] ^ pt[1];
        }
*/
        System.out.println((System.nanoTime() - time) + ", " + l);
    }

    @Test
    public void testSpeed()
    {
        testHilbert2DSpeed();
        testHilbert3DSpeed();
    }

    @Test
    public void testHilbert3D()
    {
        System.out.println("Hilbert Curve 3D");
        HilbertGeneralStrategy hilbert = new HilbertGeneralStrategy(3, 30);
        System.out.println("Max distance: " + Long.toHexString(hilbert.maxDistance));
        int[] pt = hilbert.point(0);
        printlnPoint("Hilbert Order 5 position 0", pt);
        pt = hilbert.point(1);
        printlnPoint("Hilbert Order 5 position 1", pt);
        pt = hilbert.point(4);
        printlnPoint("Hilbert Order 5 position 4", pt);
        pt = hilbert.point(-1);
        printlnPoint("Hilbert Order 5 position end", pt);

        hilbert = new HilbertGeneralStrategy(3, 33);
        System.out.println("Max distance: " + Long.toHexString(hilbert.maxDistance));
        pt = hilbert.point(0);
        printlnPoint("Hilbert Order 6 position 0", pt);
        pt = hilbert.point(1);
        printlnPoint("Hilbert Order 6 position 1", pt);
        pt = hilbert.point(4);
        printlnPoint("Hilbert Order 6 position 4", pt);
        pt = hilbert.point(-1);
        printlnPoint("Hilbert Order 6 position end", pt);
    }

    @Test
    public void testHilbert4D()
    {
        System.out.println("Hilbert Curve 4D");
        HilbertGeneralStrategy hilbert = new HilbertGeneralStrategy(4, 30);
        System.out.println("Max distance: " + Long.toHexString(hilbert.maxDistance));
        int[] pt = hilbert.point(0);
        printlnPoint("Hilbert Order 5 position 0", pt);
        pt = hilbert.point(1);
        printlnPoint("Hilbert Order 5 position 1", pt);
        pt = hilbert.point(4);
        printlnPoint("Hilbert Order 5 position 4", pt);
        pt = hilbert.point(-1);
        printlnPoint("Hilbert Order 5 position end", pt);

        hilbert = new HilbertGeneralStrategy(4, 33);
        System.out.println("Max distance: " + Long.toHexString(hilbert.maxDistance));
        pt = hilbert.point(0);
        printlnPoint("Hilbert Order 6 position 0", pt);
        pt = hilbert.point(1);
        printlnPoint("Hilbert Order 6 position 1", pt);
        pt = hilbert.point(4);
        printlnPoint("Hilbert Order 6 position 4", pt);
        pt = hilbert.point(-1);
        printlnPoint("Hilbert Order 6 position end", pt);
    }
    @Test
    public void testMoore3D()
    {
        System.out.println("Moore Curve 3D");
        MooreGeneralStrategy moore = new MooreGeneralStrategy(3, 3, 1, 3);
        System.out.println("Max distance: 0x" + Long.toHexString(moore.maxDistance));
        int[] pt = moore.point(0), pt2;
        printlnPoint("Moore position 0", pt);
        pt = moore.point(1);
        printlnPoint("Moore position 1", pt);
        pt = moore.point(4);
        printlnPoint("Moore position 4", pt);
        pt = moore.point(-1);
        printlnPoint("Moore position end", pt);
        pt = moore.point(0);
        for (int i = 1; i < moore.maxDistance; i++) {
            pt2 = moore.point(i);
            assertEquals(1, Math.abs(pt[0] - pt2[0]) + Math.abs(pt[1] - pt2[1]) + Math.abs(pt[2] - pt2[2]));
            pt = pt2;
        }
        System.out.println();
        moore = new MooreGeneralStrategy(3, 30, 1, 3);
        System.out.println("Max distance: 0x" + Long.toHexString(moore.maxDistance));
        pt = moore.point(0);
        printlnPoint("Moore Order 6 position 0", pt);
        pt = moore.point(1);
        printlnPoint("Moore Order 6 position 1", pt);
        pt = moore.point(4);
        printlnPoint("Moore Order 6 position 4", pt);
        pt = moore.point(-1);
        printlnPoint("Moore Order 6 position end", pt);
    }


    @Test
    public void testPuka()
    {
        System.out.println("Puka Curve");
        PukaStrategy puka = new PukaStrategy();
        System.out.println("Max distance: " + Long.toHexString(puka.maxDistance));
        int[] pt = puka.point(0), pt2;
        printlnPoint("Puka position 0", pt);
        pt = puka.point(1);
        printlnPoint("Puka position 1", pt);
        pt = puka.point(4);
        printlnPoint("Puka position 4", pt);
        pt = puka.point(-1);
        printlnPoint("Puka position end", pt);
        pt = puka.point(0);
        for (int i = 1; i < puka.maxDistance; i++) {
            pt2 = puka.point(i);
            assertEquals(1, Math.abs(pt[0] - pt2[0]) + Math.abs(pt[1] - pt2[1]) + Math.abs(pt[2] - pt2[2]));
            pt = pt2;
        }
    }

    @Test
    public void testPukaHilbert40()
    {
        System.out.println("Puka-Hilbert Curve");
        PukaHilbert40Strategy ph = new PukaHilbert40Strategy();
        System.out.println("Max distance: " + Long.toHexString(ph.maxDistance));
        int[] pt = ph.point(0), pt2;
        printlnPoint("Puka-Hilbert position 0", pt);
        pt = ph.point(1);
        printlnPoint("Puka-Hilbert position 1", pt);
        pt = ph.point(4);
        printlnPoint("Puka-Hilbert position 4", pt);
        pt = ph.point(-1);
        printlnPoint("Puka-Hilbert position end", pt);
        pt = ph.point(0);
        for (int i = 1; i < ph.maxDistance; i++) {
            pt2 = ph.point(i);
            assertEquals(1, Math.abs(pt[0] - pt2[0]) + Math.abs(pt[1] - pt2[1]) + Math.abs(pt[2] - pt2[2]));
            pt = pt2;
            int d = ph.distance(pt);
            if(i != d)
            {
                ph.distance(pt);
                printPoint(pt);
                System.out.println(" -> " + i + " but gives " + d);
            }
        }
    }

    @Test
    public void testPukaHilbert1280()
    {
        System.out.println("Large Puka-Hilbert Curve");
        PukaHilbert1280Strategy ph = new PukaHilbert1280Strategy();
        System.out.println("Max distance: " + Long.toHexString(ph.maxDistance));
        int[] pt = ph.point(0), pt2;
        printlnPoint("Puka-Hilbert position 0", pt);
        pt = ph.point(1);
        printlnPoint("Puka-Hilbert position 1", pt);
        pt = ph.point(4);
        printlnPoint("Puka-Hilbert position 4", pt);
        pt = ph.point(-1);
        printlnPoint("Puka-Hilbert position end", pt);

        pt = ph.point(0);
        int cx, cy, cz;
        for (int i = 1; i < ph.maxDistance; i++) {
            pt2 = ph.point(i);
            cx = ph.coordinate(i, 0);
            cy = ph.coordinate(i, 1);
            cz = ph.coordinate(i, 2);
            if(1 != Math.abs(pt[0] - pt2[0]) + Math.abs(pt[1] - pt2[1]) + Math.abs(pt[2] - pt2[2]))
                assertEquals("BAD BAD " + i + "!!!", "");
            assertEquals(pt2[0], cx);
            assertEquals(pt2[1], cy);
            assertEquals(pt2[2], cz);
            pt = pt2;
        }
    }
    @Test
    public void testPukaHilbert1280Problems()
    {
        System.out.println("Large Puka-Hilbert Curve Problems");
        PukaHilbert1280Strategy ph = new PukaHilbert1280Strategy();
        System.out.println("Max distance: " + Long.toHexString(ph.maxDistance));
        int[] pt = ph.point(0), pt2;
        pt = ph.point(0);
        int d;
//        for (int i = 1; i < ph.maxDistance; i++) {
        for (int i = 1; i < 65000; i++) {
            pt2 = ph.point(i);
            assertEquals(1, Math.abs(pt[0] - pt2[0]) + Math.abs(pt[1] - pt2[1]) + Math.abs(pt[2] - pt2[2]));
            pt = pt2;
            d = ph.distance(pt);
            if(i != d)
            {
                ph.distance(pt);
                //printPoint(pt);
                //System.out.println(" -> " + i + " but gives " + d);
            }
        }

    }

    @Test
    public void testGray() {
        for (int i = 0; i < 70; i++) {
            assertEquals(i, HilbertUtility.inverseGrayCode(HilbertUtility.grayCode(i)));
        }
    }

}