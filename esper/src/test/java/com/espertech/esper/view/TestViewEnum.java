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
package com.espertech.esper.view;

import junit.framework.TestCase;

public class TestViewEnum extends TestCase {
    public void testForName() {
        ViewEnum enumValue = ViewEnum.forName(ViewEnum.CORRELATION.getNamespace(), ViewEnum.CORRELATION.getName());
        assertEquals(enumValue, ViewEnum.CORRELATION);

        enumValue = ViewEnum.forName(ViewEnum.CORRELATION.getNamespace(), "dummy");
        assertNull(enumValue);

        enumValue = ViewEnum.forName("dummy", ViewEnum.CORRELATION.getName());
        assertNull(enumValue);
    }
}
