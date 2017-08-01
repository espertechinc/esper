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
package com.espertech.esper.type;

import junit.framework.TestCase;

import java.util.Set;

public class TestWildcardParameter extends TestCase {
    private WildcardParameter wildcard;

    public void setUp() {
        wildcard = WildcardParameter.INSTANCE;
    }

    public void testIsWildcard() {
        assertTrue(wildcard.isWildcard(1, 10));
    }

    public void testGetValuesInRange() {
        Set<Integer> result = wildcard.getValuesInRange(1, 10);
        for (int i = 1; i <= 10; i++) {
            assertTrue(result.contains(i));
        }
        assertEquals(10, result.size());
    }

    public void testContainsPoint() {
        assertTrue(wildcard.containsPoint(3));
        assertTrue(wildcard.containsPoint(2));
    }

    public void testFormat() {
        assertEquals("*", wildcard.formatted());
    }
}
