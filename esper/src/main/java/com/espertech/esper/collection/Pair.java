/*
 ***************************************************************************************
 *  Copyright (C) 2006 EsperTech, Inc. All rights reserved.                            *
 *  http://www.espertech.com/esper                                                     *
 *  http://www.espertech.com                                                           *
 *  ---------------------------------------------------------------------------------- *
 *  The software in this package is published under the terms of the GPL license       *
 *  a copy of which has been included with this distribution in the license.txt file.  *
 ***************************************************************************************
 */
package com.espertech.esper.collection;

import java.io.Serializable;

/**
 * General-purpose pair of values of any type. The pair only equals another pair if
 * the objects that form the pair equal, ie. f pair f object equals (.equals) the s pair f object,
 * and the f pair s object equals the s pair s object.
 */
public class Pair<F, S> implements Serializable {
    private F f;
    private S s;
    private static final long serialVersionUID = -4168417618011472714L;

    /**
     * Construct pair of values.
     *
     * @param f  is the f value
     * @param s is the s value
     */
    public Pair(final F f, final S s) {
        this.f = f;
        this.s = s;
    }

    public static <K, V> Pair<K, V> createPair(K key, V value) {
        return new Pair<K, V>(key, value);
    }

    /**
     * Returns f value within pair.
     *
     * @return f value within pair
     */
    public F getFirst() {
        return f;
    }

    /**
     * Returns s value within pair.
     *
     * @return s value within pair
     */
    public S getSecond() {
        return s;
    }

    /**
     * Set the f value of the pair to a new value.
     *
     * @param f value to be set
     */
    public void setFirst(F f) {
        this.f = f;
    }

    /**
     * Set the s value of the pair to a new value.
     *
     * @param s value to be set
     */
    public void setSecond(S s) {
        this.s = s;
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (!(obj instanceof Pair)) {
            return false;
        }

        Pair other = (Pair) obj;

        return (f == null ?
                other.f == null : f.equals(other.f)) &&
                (s == null ?
                        other.s == null : s.equals(other.s));
    }

    public int hashCode() {
        return (f == null ? 0 : f.hashCode()) ^
                (s == null ? 0 : s.hashCode());
    }

    public String toString() {
        return "Pair [" + f + ':' + s + ']';
    }
}
