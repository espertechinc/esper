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
package com.espertech.esper.common.internal.collection;

public class IntSeqKeyRoot implements IntSeqKey {
    public final static IntSeqKeyRoot INSTANCE = new IntSeqKeyRoot();

    private IntSeqKeyRoot() {
    }

    public boolean isParentTo(IntSeqKey other) {
        return other.length() == 1;
    }

    public IntSeqKey addToEnd(int num) {
        return new IntSeqKeyOne(num);
    }

    public IntSeqKey removeFromEnd() {
        throw new UnsupportedOperationException("Not applicable to this key");
    }

    public int length() {
        return 0;
    }

    public int last() {
        throw new UnsupportedOperationException("Not applicable to this key");
    }

    public int[] asIntArray() {
        return new int[0];
    }
}

