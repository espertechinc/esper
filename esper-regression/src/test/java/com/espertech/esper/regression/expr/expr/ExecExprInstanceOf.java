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
package com.espertech.esper.regression.expr.expr;

import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.client.soda.*;
import com.espertech.esper.supportregression.bean.*;
import com.espertech.esper.supportregression.execution.RegressionExecution;
import com.espertech.esper.util.SerializableObjectCopier;

import static org.junit.Assert.*;

public class ExecExprInstanceOf implements RegressionExecution {
    public void run(EPServiceProvider epService) throws Exception {
        runAssertionInstanceofSimple(epService);
        runAssertionInstanceofStringAndNull_OM(epService);
        runAssertionInstanceofStringAndNull_Compile(epService);
        runAssertionDynamicPropertyJavaTypes(epService);
        runAssertionDynamicSuperTypeAndInterface(epService);
    }

    private void runAssertionInstanceofSimple(EPServiceProvider epService) {
        String stmtText = "select instanceof(theString, string) as t0, " +
                " instanceof(intBoxed, int) as t1, " +
                " instanceof(floatBoxed, java.lang.Float) as t2, " +
                " instanceof(theString, java.lang.Float, char, byte) as t3, " +
                " instanceof(intPrimitive, java.lang.Integer) as t4, " +
                " instanceof(intPrimitive, long) as t5, " +
                " instanceof(intPrimitive, long, long, java.lang.Number) as t6, " +
                " instanceof(floatBoxed, long, float) as t7 " +
                " from " + SupportBean.class.getName();

        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        for (int i = 0; i < 7; i++) {
            assertEquals(Boolean.class, stmt.getEventType().getPropertyType("t" + i));
        }

        SupportBean bean = new SupportBean("abc", 100);
        bean.setFloatBoxed(100F);
        epService.getEPRuntime().sendEvent(bean);
        assertResults(listener.assertOneGetNewAndReset(), new boolean[]{true, false, true, false, true, false, true, true});

        bean = new SupportBean(null, 100);
        bean.setFloatBoxed(null);
        epService.getEPRuntime().sendEvent(bean);
        assertResults(listener.assertOneGetNewAndReset(), new boolean[]{false, false, false, false, true, false, true, false});

        stmt.destroy();
    }

    private void runAssertionInstanceofStringAndNull_OM(EPServiceProvider epService) throws Exception {
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

        EPStatement stmt = epService.getEPAdministrator().create(model);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean("abc", 100));
        EventBean theEvent = listener.assertOneGetNewAndReset();
        assertTrue((Boolean) theEvent.get("t0"));
        assertTrue((Boolean) theEvent.get("t1"));

        epService.getEPRuntime().sendEvent(new SupportBean(null, 100));
        theEvent = listener.assertOneGetNewAndReset();
        assertFalse((Boolean) theEvent.get("t0"));
        assertFalse((Boolean) theEvent.get("t1"));

        stmt.destroy();
    }

    private void runAssertionInstanceofStringAndNull_Compile(EPServiceProvider epService) throws Exception {
        String stmtText = "select instanceof(theString,string) as t0, " +
                "instanceof(theString,float,string,int) as t1 " +
                "from " + SupportBean.class.getName();

        EPStatementObjectModel model = epService.getEPAdministrator().compileEPL(stmtText);
        assertEquals(stmtText, model.toEPL());

        EPStatement stmt = epService.getEPAdministrator().create(model);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean("abc", 100));
        EventBean theEvent = listener.assertOneGetNewAndReset();
        assertTrue((Boolean) theEvent.get("t0"));
        assertTrue((Boolean) theEvent.get("t1"));

        epService.getEPRuntime().sendEvent(new SupportBean(null, 100));
        theEvent = listener.assertOneGetNewAndReset();
        assertFalse((Boolean) theEvent.get("t0"));
        assertFalse((Boolean) theEvent.get("t1"));

        stmt.destroy();
    }

    private void runAssertionDynamicPropertyJavaTypes(EPServiceProvider epService) {
        String stmtText = "select instanceof(item?, string) as t0, " +
                " instanceof(item?, int) as t1, " +
                " instanceof(item?, java.lang.Float) as t2, " +
                " instanceof(item?, java.lang.Float, char, byte) as t3, " +
                " instanceof(item?, java.lang.Integer) as t4, " +
                " instanceof(item?, long) as t5, " +
                " instanceof(item?, long, long, java.lang.Number) as t6, " +
                " instanceof(item?, long, float) as t7 " +
                " from " + SupportMarkerInterface.class.getName();

        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBeanDynRoot("abc"));
        assertResults(listener.assertOneGetNewAndReset(), new boolean[]{true, false, false, false, false, false, false, false});

        epService.getEPRuntime().sendEvent(new SupportBeanDynRoot(100f));
        assertResults(listener.assertOneGetNewAndReset(), new boolean[]{false, false, true, true, false, false, true, true});

        epService.getEPRuntime().sendEvent(new SupportBeanDynRoot(null));
        assertResults(listener.assertOneGetNewAndReset(), new boolean[]{false, false, false, false, false, false, false, false});

        epService.getEPRuntime().sendEvent(new SupportBeanDynRoot(10));
        assertResults(listener.assertOneGetNewAndReset(), new boolean[]{false, true, false, false, true, false, true, false});

        epService.getEPRuntime().sendEvent(new SupportBeanDynRoot(99L));
        assertResults(listener.assertOneGetNewAndReset(), new boolean[]{false, false, false, false, false, true, true, true});

        stmt.destroy();
    }

    private void runAssertionDynamicSuperTypeAndInterface(EPServiceProvider epService) {
        String stmtText = "select instanceof(item?, " + SupportMarkerInterface.class.getName() + ") as t0, " +
                " instanceof(item?, " + ISupportA.class.getName() + ") as t1, " +
                " instanceof(item?, " + ISupportBaseAB.class.getName() + ") as t2, " +
                " instanceof(item?, " + ISupportBaseABImpl.class.getName() + ") as t3, " +
                " instanceof(item?, " + ISupportA.class.getName() + ", " + ISupportB.class.getName() + ") as t4, " +
                " instanceof(item?, " + ISupportBaseAB.class.getName() + ", " + ISupportB.class.getName() + ") as t5, " +
                " instanceof(item?, " + ISupportAImplSuperG.class.getName() + ", " + ISupportB.class.getName() + ") as t6, " +
                " instanceof(item?, " + ISupportAImplSuperGImplPlus.class.getName() + ", " + SupportBeanBase.class.getName() + ") as t7 " +

                " from " + SupportMarkerInterface.class.getName();

        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBeanDynRoot(new SupportBeanDynRoot("abc")));
        assertResults(listener.assertOneGetNewAndReset(), new boolean[]{true, false, false, false, false, false, false, false});

        epService.getEPRuntime().sendEvent(new SupportBeanDynRoot(new ISupportAImplSuperGImplPlus()));
        assertResults(listener.assertOneGetNewAndReset(), new boolean[]{false, true, true, false, true, true, true, true});

        epService.getEPRuntime().sendEvent(new SupportBeanDynRoot(new ISupportAImplSuperGImpl("", "", "")));
        assertResults(listener.assertOneGetNewAndReset(), new boolean[]{false, true, true, false, true, true, true, false});

        epService.getEPRuntime().sendEvent(new SupportBeanDynRoot(new ISupportBaseABImpl("")));
        assertResults(listener.assertOneGetNewAndReset(), new boolean[]{false, false, true, true, false, true, false, false});

        epService.getEPRuntime().sendEvent(new SupportBeanDynRoot(new ISupportBImpl("", "")));
        assertResults(listener.assertOneGetNewAndReset(), new boolean[]{false, false, true, false, true, true, true, false});

        epService.getEPRuntime().sendEvent(new SupportBeanDynRoot(new ISupportAImpl("", "")));
        assertResults(listener.assertOneGetNewAndReset(), new boolean[]{false, true, true, false, true, true, false, false});

        stmt.destroy();
    }

    private void assertResults(EventBean theEvent, boolean[] result) {
        for (int i = 0; i < result.length; i++) {
            assertEquals("failed for index " + i, result[i], theEvent.get("t" + i));
        }
    }
}
