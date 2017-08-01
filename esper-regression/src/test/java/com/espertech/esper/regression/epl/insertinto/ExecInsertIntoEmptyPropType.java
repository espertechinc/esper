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
package com.espertech.esper.regression.epl.insertinto;

import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportSubscriber;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.bean.SupportBean_S0;
import com.espertech.esper.supportregression.bean.SupportBean_S1;
import com.espertech.esper.supportregression.execution.RegressionExecution;
import com.espertech.esper.supportregression.util.SupportModelHelper;

import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Test for populating an empty type:
 * - an empty insert-into property list is allowed, i.e. "insert into EmptySchema()"
 * - an empty select-clause is not allowed, i.e. "select from xxx" fails
 * - we require "select null from" (unnamed null column) for populating an empty type
 */
public class ExecInsertIntoEmptyPropType implements RegressionExecution {
    public void run(EPServiceProvider epService) throws Exception {
        epService.getEPAdministrator().getConfiguration().addEventType(SupportBean.class);

        runAssertionNamedWindowModelAfter(epService);
        runAssertionCreateSchemaInsertInto(epService);
    }

    private void runAssertionNamedWindowModelAfter(EPServiceProvider epService) {
        epService.getEPAdministrator().getConfiguration().addEventType(SupportBean_S0.class);
        epService.getEPAdministrator().getConfiguration().addEventType(SupportBean_S1.class);

        epService.getEPAdministrator().createEPL("create schema EmptyPropSchema()");
        EPStatement stmtCreateWindow = epService.getEPAdministrator().createEPL("create window EmptyPropWin#keepall as EmptyPropSchema");
        epService.getEPAdministrator().createEPL("insert into EmptyPropWin() select null from SupportBean");

        epService.getEPRuntime().sendEvent(new SupportBean());

        EventBean[] events = EPAssertionUtil.iteratorToArray(stmtCreateWindow.iterator());
        assertEquals(1, events.length);
        assertEquals("EmptyPropWin", events[0].getEventType().getName());

        // try fire-and-forget query
        epService.getEPRuntime().executeQuery("insert into EmptyPropWin select null");
        assertEquals(2, EPAssertionUtil.iteratorToArray(stmtCreateWindow.iterator()).length);
        epService.getEPRuntime().executeQuery("delete from EmptyPropWin"); // empty window

        // try on-merge
        epService.getEPAdministrator().createEPL("on SupportBean_S0 merge EmptyPropWin " +
                "when not matched then insert select null");
        epService.getEPRuntime().sendEvent(new SupportBean_S0(0));
        assertEquals(1, EPAssertionUtil.iteratorToArray(stmtCreateWindow.iterator()).length);

        // try on-insert
        epService.getEPAdministrator().createEPL("on SupportBean_S1 insert into EmptyPropWin select null");
        epService.getEPRuntime().sendEvent(new SupportBean_S1(0));
        assertEquals(2, EPAssertionUtil.iteratorToArray(stmtCreateWindow.iterator()).length);

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionCreateSchemaInsertInto(EPServiceProvider epService) {
        tryAssertionInsertMap(epService, true);
        tryAssertionInsertMap(epService, false);
        tryAssertionInsertOA(epService);
        tryAssertionInsertBean(epService);
    }

    private void tryAssertionInsertBean(EPServiceProvider epService) {
        epService.getEPAdministrator().createEPL("create schema MyBeanWithoutProps as " + MyBeanWithoutProps.class.getName());
        epService.getEPAdministrator().createEPL("insert into MyBeanWithoutProps select null from SupportBean");

        EPStatement stmt = epService.getEPAdministrator().createEPL("select * from MyBeanWithoutProps");
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean());
        assertTrue(listener.assertOneGetNewAndReset().getUnderlying() instanceof MyBeanWithoutProps);

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void tryAssertionInsertMap(EPServiceProvider epService, boolean soda) {
        SupportModelHelper.createByCompileOrParse(epService, soda, "create map schema EmptyMapSchema as ()");
        epService.getEPAdministrator().createEPL("insert into EmptyMapSchema() select null from SupportBean");

        EPStatement stmt = epService.getEPAdministrator().createEPL("select * from EmptyMapSchema");
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean());
        EventBean event = listener.assertOneGetNewAndReset();
        assertTrue(((Map) event.getUnderlying()).isEmpty());
        assertEquals(0, event.getEventType().getPropertyDescriptors().length);

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void tryAssertionInsertOA(EPServiceProvider epService) {
        epService.getEPAdministrator().createEPL("create objectarray schema EmptyOASchema()");
        epService.getEPAdministrator().createEPL("insert into EmptyOASchema select null from SupportBean");

        SupportSubscriber supportSubscriber = new SupportSubscriber();
        EPStatement stmt = epService.getEPAdministrator().createEPL("select * from EmptyOASchema");
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);
        stmt.setSubscriber(supportSubscriber);

        epService.getEPRuntime().sendEvent(new SupportBean());
        assertEquals(0, ((Object[]) listener.assertOneGetNewAndReset().getUnderlying()).length);

        Object[] lastNewSubscriberData = supportSubscriber.getLastNewData();
        assertEquals(1, lastNewSubscriberData.length);
        assertEquals(0, ((Object[]) lastNewSubscriberData[0]).length);
        epService.getEPAdministrator().destroyAllStatements();
    }

    public static class MyBeanWithoutProps {
    }
}
