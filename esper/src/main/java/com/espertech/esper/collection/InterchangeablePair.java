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

/**
 * General-purpose pair of values of any type. The pair equals another pair if
 * the objects that form the pair equal in any order, ie. f pair f object equals (.equals)
 * the s pair f object or s object, and the f pair s object equals the s pair f object
 * or s object.
 */
public final class InterchangeablePair<F, S> {
    private F f;
    private S s;

    /**
     * Construct pair of values.
     *
     * @param f is the f value
     * @param s is the s value
     */
    public InterchangeablePair(final F f, final S s) {
        this.f = f;
        this.s = s;
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

        if (!(obj instanceof InterchangeablePair)) {
            return false;
        }

        InterchangeablePair other = (InterchangeablePair) obj;

        if (f == null && s == null) {
            return other.f == null && other.s == null;
        }

        if (f == null) {
            if (other.s != null) {
                return (other.f == null) && s.equals(other.s);
            } else {
                return s.equals(other.f);
            }
        }

        if (s == null) {
            if (other.f != null) {
                return f.equals(other.f) && (other.s == null);
            } else {
                return f.equals(other.s);
            }
        }

        return (f.equals(other.f) && s.equals(other.s)) ||
                (f.equals(other.s) && s.equals(other.f));
    }

    public int hashCode() {
        return (f == null ? 0 : f.hashCode()) ^
                (s == null ? 0 : s.hashCode());
    }

    public String toString() {
        return "Pair [" + f + ':' + s + ']';
    }
}
