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

public class IntSeqKeyFactory {
    public static IntSeqKey from(int[] array) {
        if (array.length == 0) {
            return IntSeqKeyRoot.INSTANCE;
        }
        if (array.length == 1) {
            return new IntSeqKeyOne(array[0]);
        }
        if (array.length == 2) {
            return new IntSeqKeyTwo(array[0], array[1]);
        }
        if (array.length == 3) {
            return new IntSeqKeyThree(array[0], array[1], array[2]);
        }
        if (array.length == 4) {
            return new IntSeqKeyFour(array[0], array[1], array[2], array[3]);
        }
        if (array.length == 5) {
            return new IntSeqKeyFive(array[0], array[1], array[2], array[3], array[4]);
        }
        if (array.length == 6) {
            return new IntSeqKeySix(array[0], array[1], array[2], array[3], array[4], array[5]);
        }
        return new IntSeqKeyMany(array);
    }
}
