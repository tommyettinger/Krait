package com.github.tommyettinger;

/**
 * Created by Tommy Ettinger on 2/25/2016.
 */
public interface Check<T> {
    boolean test(T t);
}
