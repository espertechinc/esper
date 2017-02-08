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
package com.espertech.esper.filter;

import junit.framework.TestCase;

public class TestRangeValueDouble extends TestCase
{
    private FilterSpecParamRangeValue params[] = new FilterSpecParamRangeValue[5];

    public void setUp()
    {
        params[0] = new RangeValueDouble(5.5);
        params[1] = new RangeValueDouble(0);
        params[2] = new RangeValueDouble(5.5);
    }

    public void testGetFilterValue()
    {
        assertEquals(5.5, params[0].getFilterValue(null, null));
    }

    public void testEquals()
    {
        assertFalse(params[0].equals(params[1]));
        assertFalse(params[1].equals(params[2]));
        assertTrue(params[0].equals(params[2]));
    }
}
