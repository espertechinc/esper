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
 * A hash key that include a name and a count, wherein the combination of name and count defines
 * the uniqueness.
 */
public class NameParameterCountKey {

    private final String name;
    private final int count;

    public NameParameterCountKey(String name, int count) {
        this.name = name;
        this.count = count;
    }

    public String getName() {
        return name;
    }

    public int getCount() {
        return count;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        NameParameterCountKey that = (NameParameterCountKey) o;

        if (count != that.count) return false;
        if (!name.equals(that.name)) return false;

        return true;
    }

    public int hashCode() {
        int result = name.hashCode();
        result = 31 * result + count;
        return result;
    }
}
