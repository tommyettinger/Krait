package com.github.tommyettinger;

import org.junit.Test;


/**
 * Created by Tommy Ettinger on 10/1/2015.
 */
public class CurveStrategyTest {

    public static void printPoint(long... coordinates)
    {
        if(coordinates == null || coordinates.length == 0)
            return;
        System.out.print("[" + coordinates[0]);
        for (int i = 1; i < coordinates.length; i++) {
            System.out.print(", " + coordinates[i]);
        }
        System.out.print("]");
    }
    public static void printlnPoint(long... coordinates)
    {
        if(coordinates == null || coordinates.length == 0)
            return;
        System.out.print("[" + coordinates[0]);
        for (int i = 1; i < coordinates.length; i++) {
            System.out.print(", " + coordinates[i]);
        }
        System.out.println("]");
    }
    public static void printlnPoint(String name, long... coordinates)
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
        long[] pt = hilbert.point(0);
        printlnPoint("Hilbert Order 7 position 0", pt);
        pt = hilbert.point(1);
        printlnPoint("Hilbert Order 7 position 1", pt);
        pt = hilbert.point(4);
        printlnPoint("Hilbert Order 7 position 4", pt);
        pt = hilbert.point(-1);
        printlnPoint("Hilbert Order 7 position end", pt);

        hilbert = new Hilbert2DStrategy(129);
        pt = hilbert.point(0);
        printlnPoint("Hilbert Order 8 position 0", pt);
        pt = hilbert.point(1);
        printlnPoint("Hilbert Order 8 position 1", pt);
        pt = hilbert.point(4);
        printlnPoint("Hilbert Order 8 position 4", pt);
        pt = hilbert.point(-1);
        printlnPoint("Hilbert Order 8 position end", pt);
    }
    @Test
    public void testHilbert3D()
    {
        System.out.println("Hilbert Curve 3D");
        HilbertGeneralStrategy hilbert = new HilbertGeneralStrategy(3, 30);
        long[] pt = hilbert.point(0);
        printlnPoint("Hilbert Order 5 position 0", pt);
        pt = hilbert.point(1);
        printlnPoint("Hilbert Order 5 position 1", pt);
        pt = hilbert.point(4);
        printlnPoint("Hilbert Order 5 position 4", pt);
        pt = hilbert.point(-1);
        printlnPoint("Hilbert Order 5 position end", pt);

        hilbert = new HilbertGeneralStrategy(3, 33);
        pt = hilbert.point(0);
        printlnPoint("Hilbert Order 6 position 0", pt);
        pt = hilbert.point(1);
        printlnPoint("Hilbert Order 6 position 1", pt);
        pt = hilbert.point(4);
        printlnPoint("Hilbert Order 6 position 4", pt);
        pt = hilbert.point(-1);
        printlnPoint("Hilbert Order 6 position end", pt);
    }
}