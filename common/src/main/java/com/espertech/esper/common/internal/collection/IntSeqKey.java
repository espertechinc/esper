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

public interface IntSeqKey {
    boolean isParentTo(IntSeqKey other);

    IntSeqKey addToEnd(int num);

    IntSeqKey removeFromEnd();

    int length();

    int last();

    int[] asIntArray();
}
