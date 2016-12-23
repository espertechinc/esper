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

package com.espertech.esper.regression.epl;

import com.espertech.esper.client.*;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportSubscriber;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.bean.SupportBean_S0;
import com.espertech.esper.supportregression.bean.SupportBean_S1;
import com.espertech.esper.supportregression.client.SupportConfigFactory;
import com.espertech.esper.supportregression.util.SupportModelHelper;
import junit.framework.TestCase;

import java.util.Map;

/**
 * Test for populating an empty type:
 * - an empty insert-into property list is allowed, i.e. "insert into EmptySchema()"
 * - an empty select-clause is not allowed, i.e. "select from xxx" fails
 * - we require "select null from" (unnamed null column) for populating an empty type
 */
public class TestInsertIntoEmptyPropType extends TestCase
{
    private EPServiceProvider epService;
    private SupportUpdateListener listener;
    
    public void setUp()
    {
        Configuration config = SupportConfigFactory.getConfiguration();
        epService = EPServiceProviderManager.getDefaultProvider(config);
        epService.initialize();
        epService.getEPAdministrator().getConfiguration().addEventType(SupportBean.class);
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.startTest(epService, this.getClass(), getName());}
        listener = new SupportUpdateListener();
    }

    protected void tearDown() throws Exception {
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.endTest();}
        listener = null;
    }

    public void testNamedWindowModelAfter() {
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
    }

    public void testCreateSchemaInsertInto() {
        runAssertionInsertMap(true);
        runAssertionInsertMap(false);
        runAssertionInsertOA();
        runAssertionInsertBean();
    }

    private void runAssertionInsertBean() {
        epService.getEPAdministrator().createEPL("create schema MyBeanWithoutProps as " + MyBeanWithoutProps.class.getName());
        epService.getEPAdministrator().createEPL("insert into MyBeanWithoutProps select null from SupportBean");

        EPStatement stmt = epService.getEPAdministrator().createEPL("select * from MyBeanWithoutProps");
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean());
        assertTrue(listener.assertOneGetNewAndReset().getUnderlying() instanceof MyBeanWithoutProps);

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionInsertMap(boolean soda) {
        SupportModelHelper.createByCompileOrParse(epService, soda, "create map schema EmptyMapSchema as ()");
        epService.getEPAdministrator().createEPL("insert into EmptyMapSchema() select null from SupportBean");

        EPStatement stmt = epService.getEPAdministrator().createEPL("select * from EmptyMapSchema");
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean());
        EventBean event = listener.assertOneGetNewAndReset();
        assertTrue(((Map) event.getUnderlying()).isEmpty());
        assertEquals(0, event.getEventType().getPropertyDescriptors().length);

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionInsertOA() {
        epService.getEPAdministrator().createEPL("create objectarray schema EmptyOASchema()");
        epService.getEPAdministrator().createEPL("insert into EmptyOASchema select null from SupportBean");

        SupportSubscriber supportSubscriber = new SupportSubscriber();
        EPStatement stmt = epService.getEPAdministrator().createEPL("select * from EmptyOASchema");
        stmt.addListener(listener);
        stmt.setSubscriber(supportSubscriber);

        epService.getEPRuntime().sendEvent(new SupportBean());
        assertEquals(0, ((Object[]) listener.assertOneGetNewAndReset().getUnderlying()).length);

        Object[] lastNewSubscriberData = supportSubscriber.getLastNewData();
        assertEquals(1, lastNewSubscriberData.length);
        assertEquals(0, ((Object[]) lastNewSubscriberData[0]).length);
        epService.getEPAdministrator().destroyAllStatements();
    }

    private static class MyBeanWithoutProps {
    }
}
