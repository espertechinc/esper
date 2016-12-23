/*
 * *************************************************************************************
 *  Copyright (C) 2006-2015 EsperTech, Inc. All rights reserved.                       *
 *  http://www.espertech.com/esper                                                     *
 *  http://www.espertech.com                                                           *
 *  ---------------------------------------------------------------------------------- *
 *  The software in this package is published under the terms of the GPL license       *
 *  a copy of which has been included with this distribution in the license.txt file.  *
 * *************************************************************************************
 */

package com.espertech.esper.regression.event;

import com.espertech.esper.client.*;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.supportregression.bean.*;
import com.espertech.esper.supportregression.client.SupportConfigFactory;
import com.espertech.esper.util.EventRepresentationEnum;
import junit.framework.TestCase;

import java.util.Map;

public class TestEventPropertyDynamicBean extends TestCase
{
    private SupportUpdateListener listener;
    private EPServiceProvider epService;

    public void setUp()
    {
        epService = EPServiceProviderManager.getDefaultProvider(SupportConfigFactory.getConfiguration());
        epService.initialize();
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.startTest(epService, this.getClass(), getName());}
        listener = new SupportUpdateListener();
    }

    protected void tearDown() throws Exception {
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.endTest();}
        listener = null;
    }

    public void testGetValueDynamic() throws Exception
    {
        runAssertionGetDynamicWObjectArr(EventRepresentationEnum.OBJECTARRAY);
        runAssertionGetDynamicWObjectArr(EventRepresentationEnum.MAP);
        runAssertionGetDynamicWObjectArr(EventRepresentationEnum.DEFAULT);
    }

    public void testGetValueNested() throws Exception
    {
        String stmtText = "select item.nested?.nestedValue as n1, " +
                          " item.nested?.nestedValue? as n2, " +
                          " item.nested?.nestedNested.nestedNestedValue as n3, " +
                          " item.nested?.nestedNested?.nestedNestedValue as n4, " +
                          " item.nested?.nestedNested.nestedNestedValue? as n5, " +
                          " item.nested?.nestedNested?.nestedNestedValue? as n6 " +
                          " from " + SupportBeanDynRoot.class.getName();
        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        stmt.addListener(listener);

        // check type
        assertEquals(Object.class, stmt.getEventType().getPropertyType("n1"));
        assertEquals(Object.class, stmt.getEventType().getPropertyType("n2"));
        assertEquals(Object.class, stmt.getEventType().getPropertyType("n3"));
        assertEquals(Object.class, stmt.getEventType().getPropertyType("n4"));
        assertEquals(Object.class, stmt.getEventType().getPropertyType("n5"));
        assertEquals(Object.class, stmt.getEventType().getPropertyType("n6"));

        SupportBeanComplexProps bean = SupportBeanComplexProps.makeDefaultBean();
        epService.getEPRuntime().sendEvent(new SupportBeanDynRoot(bean));

        EventBean theEvent = listener.assertOneGetNewAndReset();
        assertEquals(bean.getNested().getNestedValue(), theEvent.get("n1"));
        assertEquals(bean.getNested().getNestedValue(), theEvent.get("n2"));
        assertEquals(bean.getNested().getNestedNested().getNestedNestedValue(), theEvent.get("n3"));
        assertEquals(bean.getNested().getNestedNested().getNestedNestedValue(), theEvent.get("n4"));
        assertEquals(bean.getNested().getNestedNested().getNestedNestedValue(), theEvent.get("n5"));
        assertEquals(bean.getNested().getNestedNested().getNestedNestedValue(), theEvent.get("n6"));

        bean = SupportBeanComplexProps.makeDefaultBean();
        bean.getNested().setNestedValue("nested1");
        bean.getNested().getNestedNested().setNestedNestedValue("nested2");
        epService.getEPRuntime().sendEvent(new SupportBeanDynRoot(bean));

        theEvent = listener.assertOneGetNewAndReset();
        assertEquals("nested1", theEvent.get("n1"));
        assertEquals("nested1", theEvent.get("n2"));
        assertEquals("nested2", theEvent.get("n3"));
        assertEquals("nested2", theEvent.get("n4"));
        assertEquals("nested2", theEvent.get("n5"));
        assertEquals("nested2", theEvent.get("n6"));
    }

    public void testGetValueTop() throws Exception
    {
        String stmtText = "select id? as myid from " + SupportMarkerInterface.class.getName();
        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        stmt.addListener(listener);

        // check type
        assertEquals(Object.class, stmt.getEventType().getPropertyType("myid"));

        epService.getEPRuntime().sendEvent(new SupportMarkerImplA("e1"));
        assertEquals("e1", listener.assertOneGetNewAndReset().get("myid"));

        epService.getEPRuntime().sendEvent(new SupportMarkerImplB(1));
        assertEquals(1, listener.assertOneGetNewAndReset().get("myid"));

        epService.getEPRuntime().sendEvent(new SupportMarkerImplC());
        assertEquals(null, listener.assertOneGetNewAndReset().get("myid"));
    }

    public void testGetValueTopNested() throws Exception
    {
        String stmtText = "select simpleProperty? as simple, "+
                          " nested?.nestedValue as nested, " +
                          " nested?.nestedNested.nestedNestedValue as nestedNested " +
                          "from " + SupportMarkerInterface.class.getName();
        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        stmt.addListener(listener);

        // check type
        assertEquals(Object.class, stmt.getEventType().getPropertyType("simple"));
        assertEquals(Object.class, stmt.getEventType().getPropertyType("nested"));
        assertEquals(Object.class, stmt.getEventType().getPropertyType("nestedNested"));

        epService.getEPRuntime().sendEvent(SupportBeanComplexProps.makeDefaultBean());
        EventBean theEvent = listener.assertOneGetNewAndReset();
        assertEquals("simple", theEvent.get("simple"));
        assertEquals("nestedValue", theEvent.get("nested"));
        assertEquals("nestedNestedValue", theEvent.get("nestedNested"));
    }

    public void testGetValueTopComplex() throws Exception
    {
        String stmtText = "select item?.indexed[0] as indexed1, " +
                          "item?.indexed[1]? as indexed2, " +
                          "item?.arrayProperty[1]? as array, " +
                          "item?.mapped('keyOne') as mapped1, " +
                          "item?.mapped('keyTwo')? as mapped2,  " +
                          "item?.mapProperty('xOne')? as map " +
                          "from " + SupportBeanDynRoot.class.getName();
        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        stmt.addListener(listener);

        assertEquals(Object.class, stmt.getEventType().getPropertyType("indexed1"));
        assertEquals(Object.class, stmt.getEventType().getPropertyType("indexed2"));
        assertEquals(Object.class, stmt.getEventType().getPropertyType("mapped1"));
        assertEquals(Object.class, stmt.getEventType().getPropertyType("mapped2"));
        assertEquals(Object.class, stmt.getEventType().getPropertyType("array"));

        SupportBeanComplexProps inner = SupportBeanComplexProps.makeDefaultBean();
        epService.getEPRuntime().sendEvent(new SupportBeanDynRoot(inner));
        EventBean theEvent = listener.assertOneGetNewAndReset();
        assertEquals(inner.getIndexed(0), theEvent.get("indexed1"));
        assertEquals(inner.getIndexed(1), theEvent.get("indexed2"));
        assertEquals(inner.getMapped("keyOne"), theEvent.get("mapped1"));
        assertEquals(inner.getMapped("keyTwo"), theEvent.get("mapped2"));
        assertEquals(inner.getMapProperty().get("xOne"), theEvent.get("map"));
        assertEquals(inner.getArrayProperty()[1], theEvent.get("array"));
    }

    public void testGetValueRootComplex() throws Exception
    {
        String stmtText = "select indexed[0]? as indexed1, " +
                          "indexed[1]? as indexed2, " +
                          "mapped('keyOne')? as mapped1, " +
                          "mapped('keyTwo')? as mapped2  " +
                          "from " + SupportBeanComplexProps.class.getName();
        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        stmt.addListener(listener);

        assertEquals(Object.class, stmt.getEventType().getPropertyType("indexed1"));
        assertEquals(Object.class, stmt.getEventType().getPropertyType("indexed2"));
        assertEquals(Object.class, stmt.getEventType().getPropertyType("mapped1"));
        assertEquals(Object.class, stmt.getEventType().getPropertyType("mapped2"));

        SupportBeanComplexProps inner = SupportBeanComplexProps.makeDefaultBean();
        epService.getEPRuntime().sendEvent(inner);
        EventBean theEvent = listener.assertOneGetNewAndReset();
        assertEquals(inner.getIndexed(0), theEvent.get("indexed1"));
        assertEquals(inner.getIndexed(1), theEvent.get("indexed2"));
        assertEquals(inner.getMapped("keyOne"), theEvent.get("mapped1"));
        assertEquals(inner.getMapped("keyTwo"), theEvent.get("mapped2"));
    }

    public void testPerformance() throws Exception
    {
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.endTest();} // exclude test
        String stmtText = "select simpleProperty?, " +
                          "indexed[1]? as indexed, " +
                          "mapped('keyOne')? as mapped " +
                          "from " + SupportBeanComplexProps.class.getName();
        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        stmt.addListener(listener);

        EventType type = stmt.getEventType();
        assertEquals(Object.class, type.getPropertyType("simpleProperty?"));
        assertEquals(Object.class, type.getPropertyType("indexed"));
        assertEquals(Object.class, type.getPropertyType("mapped"));

        SupportBeanComplexProps inner = SupportBeanComplexProps.makeDefaultBean();
        epService.getEPRuntime().sendEvent(inner);
        EventBean theEvent = listener.assertOneGetNewAndReset();
        assertEquals(inner.getSimpleProperty(), theEvent.get("simpleProperty?"));
        assertEquals(inner.getIndexed(1), theEvent.get("indexed"));
        assertEquals(inner.getMapped("keyOne"), theEvent.get("mapped"));

        long start = System.currentTimeMillis();
        for (int i = 0; i < 10000; i++)
        {
            epService.getEPRuntime().sendEvent(inner);
            if (i % 1000 == 0)
            {
                listener.reset();
            }
        }
        long end = System.currentTimeMillis();
        long delta = end - start;
        assertTrue("delta=" + delta, delta < 1000);
    }

    private void runAssertionGetDynamicWObjectArr(EventRepresentationEnum eventRepresentationEnum) {
        String stmtText = eventRepresentationEnum.getAnnotationText() + " select item.id? as myid from " + SupportBeanDynRoot.class.getName();
        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        stmt.addListener(listener);

        // check type
        assertEquals(Object.class, stmt.getEventType().getPropertyType("myid"));

        // check value with an object that has the property as an int
        epService.getEPRuntime().sendEvent(new SupportBeanDynRoot(new SupportBean_S0(101)));
        assertEquals(101, listener.assertOneGetNewAndReset().get("myid"));

        // check value with an object that doesn't have the property
        epService.getEPRuntime().sendEvent(new SupportBeanDynRoot(new String("abc")));
        assertEquals(null, listener.assertOneGetNewAndReset().get("myid"));

        // check value with an object that has the property as a string
        epService.getEPRuntime().sendEvent(new SupportBeanDynRoot(new SupportBean_A("e1")));
        assertEquals("e1", listener.assertOneGetNewAndReset().get("myid"));

        epService.getEPRuntime().sendEvent(new SupportBeanDynRoot(new SupportBean_B("e2")));
        assertEquals("e2", listener.assertOneGetNewAndReset().get("myid"));

        epService.getEPRuntime().sendEvent(new SupportBeanDynRoot(new SupportBean_S1(102)));
        assertEquals(102, listener.assertOneGetNewAndReset().get("myid"));

        if (eventRepresentationEnum.isObjectArrayEvent()) {
            assertEquals(Object[].class, stmt.getEventType().getUnderlyingType());
        }
        else {
            assertEquals(Map.class, stmt.getEventType().getUnderlyingType());
        }

        stmt.destroy();
    }
}
