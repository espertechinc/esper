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
import com.espertech.esper.client.EventType;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.client.soda.*;
import com.espertech.esper.regression.epl.other.ExecEPLSelectExpr;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.execution.RegressionExecution;
import com.espertech.esper.util.SerializableObjectCopier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertEquals;

public class ExecExprMinMaxNonAgg implements RegressionExecution {
    public void run(EPServiceProvider epService) throws Exception {
        runAssertionMinMaxWindowStats(epService);
        runAssertionMinMaxWindowStats_OM(epService);
        runAssertionMinMaxWindowStats_Compile(epService);
    }

    private void runAssertionMinMaxWindowStats(EPServiceProvider epService) {
        EPStatement stmt = setUpMinMax(epService);
        EventType type = stmt.getEventType();
        assertEquals(Long.class, type.getPropertyType("myMax"));
        assertEquals(Long.class, type.getPropertyType("myMin"));
        assertEquals(Long.class, type.getPropertyType("myMinEx"));
        assertEquals(Long.class, type.getPropertyType("myMaxEx"));

        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        tryMinMaxWindowStats(epService, listener);

        stmt.destroy();
    }

    private void runAssertionMinMaxWindowStats_OM(EPServiceProvider epService) throws Exception {
        String epl = "select max(longBoxed,intBoxed) as myMax, " +
                "max(longBoxed,intBoxed,shortBoxed) as myMaxEx, " +
                "min(longBoxed,intBoxed) as myMin, " +
                "min(longBoxed,intBoxed,shortBoxed) as myMinEx" +
                " from " + SupportBean.class.getName() + "#length(3)";

        EPStatementObjectModel model = new EPStatementObjectModel();
        model.setSelectClause(SelectClause.create()
                .add(Expressions.max("longBoxed", "intBoxed"), "myMax")
                .add(Expressions.max(Expressions.property("longBoxed"), Expressions.property("intBoxed"), Expressions.property("shortBoxed")), "myMaxEx")
                .add(Expressions.min("longBoxed", "intBoxed"), "myMin")
                .add(Expressions.min(Expressions.property("longBoxed"), Expressions.property("intBoxed"), Expressions.property("shortBoxed")), "myMinEx")
        );
        model.setFromClause(FromClause.create(FilterStream.create(SupportBean.class.getName()).addView("length", Expressions.constant(3))));
        model = (EPStatementObjectModel) SerializableObjectCopier.copy(model);
        assertEquals(epl, model.toEPL());

        EPStatement stmt = epService.getEPAdministrator().create(model);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        tryMinMaxWindowStats(epService, listener);

        stmt.destroy();
    }

    private void runAssertionMinMaxWindowStats_Compile(EPServiceProvider epService) throws Exception {
        String epl = "select max(longBoxed,intBoxed) as myMax, " +
                "max(longBoxed,intBoxed,shortBoxed) as myMaxEx, " +
                "min(longBoxed,intBoxed) as myMin, " +
                "min(longBoxed,intBoxed,shortBoxed) as myMinEx" +
                " from " + SupportBean.class.getName() + "#length(3)";

        EPStatementObjectModel model = epService.getEPAdministrator().compileEPL(epl);
        model = (EPStatementObjectModel) SerializableObjectCopier.copy(model);
        assertEquals(epl, model.toEPL());

        EPStatement stmt = epService.getEPAdministrator().create(model);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        tryMinMaxWindowStats(epService, listener);

        stmt.destroy();
    }

    private void tryMinMaxWindowStats(EPServiceProvider epService, SupportUpdateListener listener) {
        sendEvent(epService, 10, 20, (short) 4);
        EventBean received = listener.getAndResetLastNewData()[0];
        assertEquals(20L, received.get("myMax"));
        assertEquals(10L, received.get("myMin"));
        assertEquals(4L, received.get("myMinEx"));
        assertEquals(20L, received.get("myMaxEx"));

        sendEvent(epService, -10, -20, (short) -30);
        received = listener.getAndResetLastNewData()[0];
        assertEquals(-10L, received.get("myMax"));
        assertEquals(-20L, received.get("myMin"));
        assertEquals(-30L, received.get("myMinEx"));
        assertEquals(-10L, received.get("myMaxEx"));
    }

    private EPStatement setUpMinMax(EPServiceProvider epService) {
        String epl = "select max(longBoxed, intBoxed) as myMax, " +
                "max(longBoxed, intBoxed, shortBoxed) as myMaxEx," +
                "min(longBoxed, intBoxed) as myMin," +
                "min(longBoxed, intBoxed, shortBoxed) as myMinEx" +
                " from " + SupportBean.class.getName() + "#length(3) ";
        return epService.getEPAdministrator().createEPL(epl);
    }

    private void sendEvent(EPServiceProvider epService, long longBoxed, int intBoxed, short shortBoxed) {
        sendBoxedEvent(epService, longBoxed, intBoxed, shortBoxed);
    }

    private void sendBoxedEvent(EPServiceProvider epService, Long longBoxed, Integer intBoxed, Short shortBoxed) {
        SupportBean bean = new SupportBean();
        bean.setLongBoxed(longBoxed);
        bean.setIntBoxed(intBoxed);
        bean.setShortBoxed(shortBoxed);
        epService.getEPRuntime().sendEvent(bean);
    }

    private static final Logger log = LoggerFactory.getLogger(ExecEPLSelectExpr.class);
}
