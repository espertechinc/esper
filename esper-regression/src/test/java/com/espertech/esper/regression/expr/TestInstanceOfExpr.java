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
package com.espertech.esper.regression.expr;

import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import junit.framework.TestCase;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPServiceProviderManager;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.soda.*;
import com.espertech.esper.supportregression.bean.*;
import com.espertech.esper.supportregression.client.SupportConfigFactory;
import com.espertech.esper.util.SerializableObjectCopier;

public class TestInstanceOfExpr extends TestCase
{
    private EPServiceProvider epService;
    private SupportUpdateListener listener;

    public void setUp()
    {
        listener = new SupportUpdateListener();
        epService = EPServiceProviderManager.getDefaultProvider(SupportConfigFactory.getConfiguration());
        epService.initialize();
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.startTest(epService, this.getClass(), getName());}
    }

    protected void tearDown() throws Exception {
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.endTest();}
        listener = null;
    }

    public void testInstanceofSimple()
    {
        String stmtText = "select instanceof(theString, string) as t0, " +
                          " instanceof(intBoxed, int) as t1, " +
                          " instanceof(floatBoxed, java.lang.Float) as t2, " +
                          " instanceof(theString, java.lang.Float, char, byte) as t3, " +
                          " instanceof(intPrimitive, java.lang.Integer) as t4, " +
                          " instanceof(intPrimitive, long) as t5, " +
                          " instanceof(intPrimitive, long, long, java.lang.Number) as t6, " +
                          " instanceof(floatBoxed, long, float) as t7 " +
                          " from " + SupportBean.class.getName();

        EPStatement selectTestCase = epService.getEPAdministrator().createEPL(stmtText);
        selectTestCase.addListener(listener);

        for (int i = 0; i < 7; i++)
        {
            assertEquals(Boolean.class, selectTestCase.getEventType().getPropertyType("t" + i));
        }

        SupportBean bean = new SupportBean("abc", 100);
        bean.setFloatBoxed(100F);
        epService.getEPRuntime().sendEvent(bean);
        assertResults(listener.assertOneGetNewAndReset(), new boolean[] {true, false, true, false, true, false, true, true});

        bean = new SupportBean(null, 100);
        bean.setFloatBoxed(null);
        epService.getEPRuntime().sendEvent(bean);
        assertResults(listener.assertOneGetNewAndReset(), new boolean[] {false, false, false, false, true, false, true, false});

        Float f = null;
        assertFalse(f instanceof Float);
    }

    public void testInstanceofStringAndNull_OM() throws Exception
    {
        String stmtText = "select instanceof(theString,string) as t0, " +
                          "instanceof(theString,float,string,int) as t1 " +
                          "from " + SupportBean.class.getName();

        EPStatementObjectModel model = new EPStatementObjectModel();
        model.setSelectClause(SelectClause.create()
                .add(Expressions.instanceOf("theString", "string"), "t0")
                .add(Expressions.instanceOf(Expressions.property("theString"), "float", "string", "int"), "t1"));
        model.setFromClause(FromClause.create(FilterStream.create(SupportBean.class.getName())));
        model = (EPStatementObjectModel) SerializableObjectCopier.copy(model);
        assertEquals(stmtText, model.toEPL());

        EPStatement selectTestCase = epService.getEPAdministrator().create(model);
        selectTestCase.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean("abc", 100));
        EventBean theEvent = listener.assertOneGetNewAndReset();
        assertTrue((Boolean) theEvent.get("t0"));
        assertTrue((Boolean) theEvent.get("t1"));

        epService.getEPRuntime().sendEvent(new SupportBean(null, 100));
        theEvent = listener.assertOneGetNewAndReset();
        assertFalse((Boolean) theEvent.get("t0"));
        assertFalse((Boolean) theEvent.get("t1"));
    }

    public void testInstanceofStringAndNull_Compile() throws Exception
    {
        String stmtText = "select instanceof(theString,string) as t0, " +
                          "instanceof(theString,float,string,int) as t1 " +
                          "from " + SupportBean.class.getName();

        EPStatementObjectModel model = epService.getEPAdministrator().compileEPL(stmtText);
        assertEquals(stmtText, model.toEPL());

        EPStatement selectTestCase = epService.getEPAdministrator().create(model);
        selectTestCase.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean("abc", 100));
        EventBean theEvent = listener.assertOneGetNewAndReset();
        assertTrue((Boolean) theEvent.get("t0"));
        assertTrue((Boolean) theEvent.get("t1"));

        epService.getEPRuntime().sendEvent(new SupportBean(null, 100));
        theEvent = listener.assertOneGetNewAndReset();
        assertFalse((Boolean) theEvent.get("t0"));
        assertFalse((Boolean) theEvent.get("t1"));
    }

    public void testDynamicPropertyJavaTypes()
    {
        String stmtText = "select instanceof(item?, string) as t0, " +
                          " instanceof(item?, int) as t1, " +
                          " instanceof(item?, java.lang.Float) as t2, " +
                          " instanceof(item?, java.lang.Float, char, byte) as t3, " +
                          " instanceof(item?, java.lang.Integer) as t4, " +
                          " instanceof(item?, long) as t5, " +
                          " instanceof(item?, long, long, java.lang.Number) as t6, " +
                          " instanceof(item?, long, float) as t7 " +
                          " from " + SupportMarkerInterface.class.getName();

        EPStatement selectTestCase = epService.getEPAdministrator().createEPL(stmtText);
        selectTestCase.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBeanDynRoot("abc"));
        assertResults(listener.assertOneGetNewAndReset(), new boolean[] {true, false, false, false, false, false, false, false});

        epService.getEPRuntime().sendEvent(new SupportBeanDynRoot(100f));
        assertResults(listener.assertOneGetNewAndReset(), new boolean[] {false, false, true, true, false, false, true, true});

        epService.getEPRuntime().sendEvent(new SupportBeanDynRoot(null));
        assertResults(listener.assertOneGetNewAndReset(), new boolean[] {false, false, false, false, false, false, false, false});

        epService.getEPRuntime().sendEvent(new SupportBeanDynRoot(10));
        assertResults(listener.assertOneGetNewAndReset(), new boolean[] {false, true, false, false, true, false, true, false});

        epService.getEPRuntime().sendEvent(new SupportBeanDynRoot(99l));
        assertResults(listener.assertOneGetNewAndReset(), new boolean[] {false, false, false, false, false, true, true, true});
    }

    public void testDynamicSuperTypeAndInterface()
    {
        String stmtText = "select instanceof(item?, " + SupportMarkerInterface.class.getName() + ") as t0, " +
                          " instanceof(item?, " + ISupportA.class.getName() + ") as t1, " +
                          " instanceof(item?, " + ISupportBaseAB.class.getName() + ") as t2, " +
                          " instanceof(item?, " + ISupportBaseABImpl.class.getName() + ") as t3, " +
                          " instanceof(item?, " + ISupportA.class.getName() + ", " + ISupportB.class.getName() + ") as t4, " +
                          " instanceof(item?, " + ISupportBaseAB.class.getName() + ", " + ISupportB.class.getName() + ") as t5, " +
                          " instanceof(item?, " + ISupportAImplSuperG.class.getName() + ", " + ISupportB.class.getName() + ") as t6, " +
                          " instanceof(item?, " + ISupportAImplSuperGImplPlus.class.getName() + ", " + SupportBeanBase.class.getName() + ") as t7 " +

                          " from " + SupportMarkerInterface.class.getName();

        EPStatement selectTestCase = epService.getEPAdministrator().createEPL(stmtText);
        selectTestCase.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBeanDynRoot(new SupportBeanDynRoot("abc")));
        assertResults(listener.assertOneGetNewAndReset(), new boolean[] {true, false, false, false, false, false, false, false});

        epService.getEPRuntime().sendEvent(new SupportBeanDynRoot(new ISupportAImplSuperGImplPlus()));
        assertResults(listener.assertOneGetNewAndReset(), new boolean[] {false, true, true, false, true, true, true, true});

        epService.getEPRuntime().sendEvent(new SupportBeanDynRoot(new ISupportAImplSuperGImpl("", "", "")));
        assertResults(listener.assertOneGetNewAndReset(), new boolean[] {false, true, true, false, true, true, true, false});

        epService.getEPRuntime().sendEvent(new SupportBeanDynRoot(new ISupportBaseABImpl("")));
        assertResults(listener.assertOneGetNewAndReset(), new boolean[] {false, false, true, true, false, true, false, false});

        epService.getEPRuntime().sendEvent(new SupportBeanDynRoot(new ISupportBImpl("", "")));
        assertResults(listener.assertOneGetNewAndReset(), new boolean[] {false, false, true, false, true, true, true, false});

        epService.getEPRuntime().sendEvent(new SupportBeanDynRoot(new ISupportAImpl("", "")));
        assertResults(listener.assertOneGetNewAndReset(), new boolean[] {false, true, true, false, true, true, false, false});
    }

    private void assertResults(EventBean theEvent, boolean[] result)
    {
        for (int i = 0; i < result.length; i++)
        {
            assertEquals("failed for index " + i, result[i], theEvent.get("t" + i));
        }
    }
}
