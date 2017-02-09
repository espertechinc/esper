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

import com.espertech.esper.client.EventType;
import com.espertech.esper.supportunit.bean.SupportBean;
import com.espertech.esper.supportunit.bean.SupportMarketDataBean;
import com.espertech.esper.supportunit.event.SupportEventTypeFactory;
import junit.framework.TestCase;

public class TestPropertyCheckHelper extends TestCase {
    public void testCheckNumeric() {
        EventType mySchema = SupportEventTypeFactory.createBeanType(SupportMarketDataBean.class);

        assertTrue(PropertyCheckHelper.checkNumeric(mySchema, "dummy") != null);
        assertTrue(PropertyCheckHelper.checkNumeric(mySchema, "symbol") != null);

        assertTrue(PropertyCheckHelper.checkNumeric(mySchema, "volume") == null);
        assertTrue(PropertyCheckHelper.checkNumeric(mySchema, "price") == null);

        assertTrue(PropertyCheckHelper.checkNumeric(mySchema, "dummy", "dummy2") != null);
        assertTrue(PropertyCheckHelper.checkNumeric(mySchema, "symbol", "dummy2") != null);
        assertTrue(PropertyCheckHelper.checkNumeric(mySchema, "symbol", "price") != null);
        assertTrue(PropertyCheckHelper.checkNumeric(mySchema, "price", "dummy") != null);
        assertTrue(PropertyCheckHelper.checkNumeric(mySchema, "dummy", "price") != null);

        assertTrue(PropertyCheckHelper.checkNumeric(mySchema, "price", "price") == null);
        assertTrue(PropertyCheckHelper.checkNumeric(mySchema, "price", "volume") == null);
        assertTrue(PropertyCheckHelper.checkNumeric(mySchema, "volume", "price") == null);
    }

    public void testCheckLong() {
        EventType mySchema = SupportEventTypeFactory.createBeanType(SupportBean.class);

        assertEquals(null, PropertyCheckHelper.checkLong(mySchema, "longPrimitive"));
        assertEquals(null, PropertyCheckHelper.checkLong(mySchema, "longBoxed"));
        assertEquals(null, PropertyCheckHelper.checkLong(mySchema, "longBoxed"));
        assertTrue(PropertyCheckHelper.checkLong(mySchema, "dummy") != null);
        assertTrue(PropertyCheckHelper.checkLong(mySchema, "intPrimitive") != null);
        assertTrue(PropertyCheckHelper.checkLong(mySchema, "doubleBoxed") != null);
    }

    public void testFieldExist() {
        EventType mySchema = SupportEventTypeFactory.createBeanType(SupportBean.class);

        assertEquals(null, PropertyCheckHelper.exists(mySchema, "longPrimitive"));
        assertTrue(PropertyCheckHelper.exists(mySchema, "dummy") != null);
    }

    public void test2FieldExist() {
        EventType mySchema = SupportEventTypeFactory.createBeanType(SupportBean.class);

        assertEquals(null, PropertyCheckHelper.exists(mySchema, "longPrimitive", "longBoxed"));
        assertTrue(PropertyCheckHelper.exists(mySchema, "dummy", "longPrimitive") != null);
        assertTrue(PropertyCheckHelper.exists(mySchema, "longPrimitive", "dummy") != null);
        assertTrue(PropertyCheckHelper.exists(mySchema, "dummy", "dummy") != null);
    }
}