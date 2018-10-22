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
package com.espertech.esper.common.internal.util;

class MethodExecutableRank {
    private final int conversionCount;
    private final boolean varargs;

    public MethodExecutableRank(int conversionCount, boolean varargs) {
        this.conversionCount = conversionCount;
        this.varargs = varargs;
    }

    public int compareTo(int conversionCount, boolean varargs) {
        int compareCount = Integer.compare(this.conversionCount, conversionCount);
        if (compareCount != 0) {
            return compareCount;
        }
        return Boolean.compare(this.varargs, varargs);
    }

    public int compareTo(MethodExecutableRank other) {
        return compareTo(other.conversionCount, other.varargs);
    }

    public int getConversionCount() {
        return conversionCount;
    }

    public boolean isVarargs() {
        return varargs;
    }

    public String toString() {
        return "MethodExecutableRank{" +
                "conversionCount=" + conversionCount +
                ", varargs=" + varargs +
                '}';
    }
}
