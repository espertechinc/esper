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

import com.espertech.esper.client.*;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.client.soda.*;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.bean.SupportBean_S0;
import com.espertech.esper.supportregression.execution.RegressionExecution;
import com.espertech.esper.supportregression.util.SupportMessageAssertUtil;
import com.espertech.esper.util.SerializableObjectCopier;

import static junit.framework.TestCase.*;
import static org.junit.Assert.assertEquals;

public class ExecExprEqualsIs implements RegressionExecution {
    public void run(EPServiceProvider epService) throws Exception {
        epService.getEPAdministrator().getConfiguration().addEventType(SupportBean.class);
        epService.getEPAdministrator().getConfiguration().addEventType(SupportBean_S0.class);

        // more extensive testing elsewhere especially as part of filters
        // these tests are for independent select-clause expression tests
        runAssertionSameType(epService);
        runAssertionCoercion(epService);
    }

    private void runAssertionCoercion(EPServiceProvider epService) {
        EPStatement stmt = epService.getEPAdministrator().createEPL("select intPrimitive=longPrimitive as c0, intPrimitive is longPrimitive as c1 from SupportBean");
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);
        String[] fields = "c0,c1".split(",");

        makeSendBean(epService, 1, 1L);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[] {true, true});

        makeSendBean(epService, 1, 2L);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[] {false, false});

        stmt.destroy();
    }

    private void runAssertionSameType(EPServiceProvider epService) {
        EPStatement stmt = epService.getEPAdministrator().createEPL("select p00 = p01 as c0, id = id as c1, p02 is not null as c2 from SupportBean_S0");
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);
        String[] fields = "c0,c1,c2".split(",");

        epService.getEPRuntime().sendEvent(new SupportBean_S0(1, "a", "a", "a"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[] {true, true, true});

        epService.getEPRuntime().sendEvent(new SupportBean_S0(1, "a", "b", null));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[] {false, true, false});

        stmt.destroy();
    }

    private void makeSendBean(EPServiceProvider epService, int intPrimitive, long longPrimitive) {
        SupportBean bean = new SupportBean();
        bean.setIntPrimitive(intPrimitive);
        bean.setLongPrimitive(longPrimitive);
        epService.getEPRuntime().sendEvent(bean);
    }
}
