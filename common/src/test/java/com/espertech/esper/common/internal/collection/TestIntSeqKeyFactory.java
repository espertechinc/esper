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

import junit.framework.TestCase;

import java.util.Arrays;

public class TestIntSeqKeyFactory extends TestCase {
    public void testFactory() {
        assertTrue(IntSeqKeyFactory.from(new int[0]) instanceof IntSeqKeyRoot);
        assertKey(IntSeqKeyFactory.from(new int[]{1}), IntSeqKeyOne.class, 1);
        assertKey(IntSeqKeyFactory.from(new int[]{1, 2}), IntSeqKeyTwo.class, 1, 2);
        assertKey(IntSeqKeyFactory.from(new int[]{1, 2, 3}), IntSeqKeyThree.class, 1, 2, 3);
        assertKey(IntSeqKeyFactory.from(new int[]{1, 2, 3, 4}), IntSeqKeyFour.class, 1, 2, 3, 4);
        assertKey(IntSeqKeyFactory.from(new int[]{1, 2, 3, 4, 5}), IntSeqKeyFive.class, 1, 2, 3, 4, 5);
        assertKey(IntSeqKeyFactory.from(new int[]{1, 2, 3, 4, 5, 6}), IntSeqKeySix.class, 1, 2, 3, 4, 5, 6);
        assertKey(IntSeqKeyFactory.from(new int[]{1, 2, 3, 4, 5, 6, 7}), IntSeqKeyMany.class, 1, 2, 3, 4, 5, 6, 7);
    }

    private void assertKey(IntSeqKey key, Class clazz, int... expected) {
        assertEquals(key.getClass(), clazz);
        assertTrue(Arrays.equals(expected, key.asIntArray()));
    }
}
