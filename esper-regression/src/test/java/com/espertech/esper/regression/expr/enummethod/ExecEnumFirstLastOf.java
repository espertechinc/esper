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
package com.espertech.esper.regression.expr.enummethod;

import com.espertech.esper.client.Configuration;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.supportregression.bean.SupportBean_ST0;
import com.espertech.esper.supportregression.bean.SupportBean_ST0_Container;
import com.espertech.esper.supportregression.bean.SupportCollection;
import com.espertech.esper.supportregression.bean.lambda.LambdaAssertionUtil;
import com.espertech.esper.supportregression.execution.RegressionExecution;

import static org.junit.Assert.*;

public class ExecEnumFirstLastOf implements RegressionExecution {

    public void configure(Configuration configuration) throws Exception {
        configuration.addEventType("Bean", SupportBean_ST0_Container.class);
        configuration.addEventType("SupportCollection", SupportCollection.class);
    }

    public void run(EPServiceProvider epService) throws Exception {
        runAssertionFirstLastScalar(epService);
        runAssertionFirstLastProperty(epService);
        runAssertionFirstLastNoPred(epService);
        runAssertionFirstLastPredicate(epService);
    }

    private void runAssertionFirstLastScalar(EPServiceProvider epService) {

        String[] fields = "val0,val1,val2,val3".split(",");
        String eplFragment = "select " +
                "strvals.firstOf() as val0, " +
                "strvals.lastOf() as val1, " +
                "strvals.firstOf(x => x like '%1%') as val2, " +
                "strvals.lastOf(x => x like '%1%') as val3 " +
                " from SupportCollection";
        EPStatement stmtFragment = epService.getEPAdministrator().createEPL(eplFragment);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmtFragment.addListener(listener);
        LambdaAssertionUtil.assertTypes(stmtFragment.getEventType(), fields, new Class[]{String.class, String.class, String.class, String.class});

        epService.getEPRuntime().sendEvent(SupportCollection.makeString("E1,E2,E3"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E1", "E3", "E1", "E1"});

        epService.getEPRuntime().sendEvent(SupportCollection.makeString("E1"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E1", "E1", "E1", "E1"});

        epService.getEPRuntime().sendEvent(SupportCollection.makeString("E2,E3,E4"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E2", "E4", null, null});

        epService.getEPRuntime().sendEvent(SupportCollection.makeString(""));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{null, null, null, null});

        epService.getEPRuntime().sendEvent(SupportCollection.makeString(null));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{null, null, null, null});

        stmtFragment.destroy();
    }

    private void runAssertionFirstLastProperty(EPServiceProvider epService) {

        String[] fields = "val0,val1".split(",");
        String eplFragment = "select " +
                "contained.firstOf().p00 as val0, " +
                "contained.lastOf().p00 as val1 " +
                " from Bean";
        EPStatement stmtFragment = epService.getEPAdministrator().createEPL(eplFragment);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmtFragment.addListener(listener);
        LambdaAssertionUtil.assertTypes(stmtFragment.getEventType(), fields, new Class[]{Integer.class, Integer.class});

        epService.getEPRuntime().sendEvent(SupportBean_ST0_Container.make2Value("E1,1", "E2,9", "E3,3"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{1, 3});

        epService.getEPRuntime().sendEvent(SupportBean_ST0_Container.make2Value("E1,1"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{1, 1});

        epService.getEPRuntime().sendEvent(SupportBean_ST0_Container.make2Value(null));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{null, null});

        epService.getEPRuntime().sendEvent(SupportBean_ST0_Container.make2Value());
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{null, null});

        stmtFragment.destroy();
    }

    private void runAssertionFirstLastNoPred(EPServiceProvider epService) {

        String eplFragment = "select " +
                "contained.firstOf() as val0, " +
                "contained.lastOf() as val1 " +
                " from Bean";
        EPStatement stmtFragment = epService.getEPAdministrator().createEPL(eplFragment);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmtFragment.addListener(listener);
        LambdaAssertionUtil.assertTypes(stmtFragment.getEventType(), "val0,val1".split(","), new Class[]{SupportBean_ST0.class, SupportBean_ST0.class});

        epService.getEPRuntime().sendEvent(SupportBean_ST0_Container.make2Value("E1,1", "E3,9", "E2,9"));
        assertId(listener, "val0", "E1");
        assertId(listener, "val1", "E2");
        listener.reset();

        epService.getEPRuntime().sendEvent(SupportBean_ST0_Container.make2Value("E2,2"));
        assertId(listener, "val0", "E2");
        assertId(listener, "val1", "E2");
        listener.reset();

        epService.getEPRuntime().sendEvent(SupportBean_ST0_Container.make2Value(null));
        assertNull(listener.assertOneGetNew().get("val0"));
        assertNull(listener.assertOneGetNewAndReset().get("val1"));

        epService.getEPRuntime().sendEvent(SupportBean_ST0_Container.make2Value());
        assertNull(listener.assertOneGetNew().get("val0"));
        assertNull(listener.assertOneGetNewAndReset().get("val1"));

        stmtFragment.destroy();
    }

    private void runAssertionFirstLastPredicate(EPServiceProvider epService) {

        String eplFragment = "select contained.firstOf(x => p00 = 9) as val from Bean";
        EPStatement stmtFragment = epService.getEPAdministrator().createEPL(eplFragment);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmtFragment.addListener(listener);
        LambdaAssertionUtil.assertTypes(stmtFragment.getEventType(), "val".split(","), new Class[]{SupportBean_ST0.class});

        SupportBean_ST0_Container bean = SupportBean_ST0_Container.make2Value("E1,1", "E2,9", "E2,9");
        epService.getEPRuntime().sendEvent(bean);
        SupportBean_ST0 result = (SupportBean_ST0) listener.assertOneGetNewAndReset().get("val");
        assertSame(result, bean.getContained().get(1));

        epService.getEPRuntime().sendEvent(SupportBean_ST0_Container.make2Value(null));
        assertNull(listener.assertOneGetNewAndReset().get("val"));

        epService.getEPRuntime().sendEvent(SupportBean_ST0_Container.make2Value());
        assertNull(listener.assertOneGetNewAndReset().get("val"));

        epService.getEPRuntime().sendEvent(SupportBean_ST0_Container.make2Value("E1,1", "E2,1", "E2,1"));
        assertNull(listener.assertOneGetNewAndReset().get("val"));

        stmtFragment.destroy();
    }

    private void assertId(SupportUpdateListener listener, String property, String id) {
        SupportBean_ST0 result = (SupportBean_ST0) listener.assertOneGetNew().get(property);
        assertEquals(id, result.getId());
    }
}
