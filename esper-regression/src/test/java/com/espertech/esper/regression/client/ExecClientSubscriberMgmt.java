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
package com.espertech.esper.regression.client;

import com.espertech.esper.client.Configuration;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.supportregression.bean.*;
import com.espertech.esper.supportregression.execution.RegressionExecution;
import com.espertech.esper.supportregression.subscriber.SupportSubscriberRowByRowSpecificNStmt;
import com.espertech.esper.supportregression.subscriber.SupportSubscriberRowByRowSpecificWStmt;
import com.espertech.esper.supportregression.util.SupportStmtAwareUpdateListener;
import com.espertech.esper.support.EventRepresentationChoice;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class ExecClientSubscriberMgmt implements RegressionExecution {
    private final static String[] FIELDS = "theString,intPrimitive".split(",");

    public void configure(Configuration configuration) throws Exception {
        String pkg = SupportBean.class.getPackage().getName();
        configuration.addEventTypeAutoName(pkg);
    }

    public void run(EPServiceProvider epService) throws Exception {
        runAssertionStartStopStatement(epService);
        runAssertionVariables(epService);
        runAssertionNamedWindow(epService);
        runAssertionSimpleSelectUpdateOnly(epService);
    }

    private void runAssertionStartStopStatement(EPServiceProvider epService) {
        SubscriberInterface subscriber = new SubscriberInterface();
        EPStatement stmt = epService.getEPAdministrator().createEPL("select * from SupportMarkerInterface");
        stmt.setSubscriber(subscriber);

        SupportBean_A a1 = new SupportBean_A("A1");
        epService.getEPRuntime().sendEvent(a1);
        EPAssertionUtil.assertEqualsExactOrder(new Object[]{a1}, subscriber.getAndResetIndicate().toArray());

        SupportBean_B b1 = new SupportBean_B("B1");
        epService.getEPRuntime().sendEvent(b1);
        EPAssertionUtil.assertEqualsExactOrder(new Object[]{b1}, subscriber.getAndResetIndicate().toArray());

        stmt.stop();

        SupportBean_C c1 = new SupportBean_C("C1");
        epService.getEPRuntime().sendEvent(c1);
        assertEquals(0, subscriber.getAndResetIndicate().size());

        stmt.start();

        SupportBean_D d1 = new SupportBean_D("D1");
        epService.getEPRuntime().sendEvent(d1);
        EPAssertionUtil.assertEqualsExactOrder(new Object[]{d1}, subscriber.getAndResetIndicate().toArray());

        stmt.destroy();
    }

    private void runAssertionVariables(EPServiceProvider epService) {
        String[] fields = "myvar".split(",");
        SubscriberMap subscriberCreateVariable = new SubscriberMap();
        String stmtTextCreate = "create variable string myvar = 'abc'";
        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtTextCreate);
        stmt.setSubscriber(subscriberCreateVariable);

        SubscriberMap subscriberSetVariable = new SubscriberMap();
        String stmtTextSet = "on SupportBean set myvar = theString";
        stmt = epService.getEPAdministrator().createEPL(stmtTextSet);
        stmt.setSubscriber(subscriberSetVariable);

        epService.getEPRuntime().sendEvent(new SupportBean("def", 1));
        EPAssertionUtil.assertPropsMap(subscriberCreateVariable.getAndResetIndicate().get(0), fields, new Object[]{"def"});
        EPAssertionUtil.assertPropsMap(subscriberSetVariable.getAndResetIndicate().get(0), fields, new Object[]{"def"});

        stmt.destroy();
    }

    private void runAssertionNamedWindow(EPServiceProvider epService) {
        tryAssertionNamedWindow(epService, EventRepresentationChoice.MAP);
    }

    private void tryAssertionNamedWindow(EPServiceProvider epService, EventRepresentationChoice eventRepresentationEnum) {
        String[] fields = "key,value".split(",");
        SubscriberMap subscriberNamedWindow = new SubscriberMap();
        String stmtTextCreate = eventRepresentationEnum.getAnnotationText() + " create window MyWindow#keepall as select theString as key, intPrimitive as value from SupportBean";
        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtTextCreate);
        stmt.setSubscriber(subscriberNamedWindow);

        SubscriberFields subscriberInsertInto = new SubscriberFields();
        String stmtTextInsertInto = "insert into MyWindow select theString as key, intPrimitive as value from SupportBean";
        stmt = epService.getEPAdministrator().createEPL(stmtTextInsertInto);
        stmt.setSubscriber(subscriberInsertInto);

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 1));
        EPAssertionUtil.assertPropsMap(subscriberNamedWindow.getAndResetIndicate().get(0), fields, new Object[]{"E1", 1});
        EPAssertionUtil.assertEqualsExactOrder(new Object[][]{{"E1", 1}}, subscriberInsertInto.getAndResetIndicate());

        // test on-delete
        SubscriberMap subscriberDelete = new SubscriberMap();
        String stmtTextDelete = "on SupportMarketDataBean s0 delete from MyWindow s1 where s0.symbol = s1.key";
        stmt = epService.getEPAdministrator().createEPL(stmtTextDelete);
        stmt.setSubscriber(subscriberDelete);

        epService.getEPRuntime().sendEvent(new SupportMarketDataBean("E1", 0, 1L, ""));
        EPAssertionUtil.assertPropsMap(subscriberDelete.getAndResetIndicate().get(0), fields, new Object[]{"E1", 1});

        // test on-select
        SubscriberMap subscriberSelect = new SubscriberMap();
        String stmtTextSelect = "on SupportMarketDataBean s0 select key, value from MyWindow s1";
        stmt = epService.getEPAdministrator().createEPL(stmtTextSelect);
        stmt.setSubscriber(subscriberSelect);

        epService.getEPRuntime().sendEvent(new SupportBean("E2", 2));
        epService.getEPRuntime().sendEvent(new SupportMarketDataBean("M1", 0, 1L, ""));
        EPAssertionUtil.assertPropsMap(subscriberSelect.getAndResetIndicate().get(0), fields, new Object[]{"E2", 2});

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionSimpleSelectUpdateOnly(EPServiceProvider epService) {
        SupportSubscriberRowByRowSpecificNStmt subscriber = new SupportSubscriberRowByRowSpecificNStmt();
        EPStatement stmt = epService.getEPAdministrator().createEPL("select theString, intPrimitive from " + SupportBean.class.getName() + "#lastevent");
        stmt.setSubscriber(subscriber);

        // get statement, attach listener
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        // send event
        epService.getEPRuntime().sendEvent(new SupportBean("E1", 100));
        subscriber.assertOneReceivedAndReset(stmt, new Object[]{"E1", 100});
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), FIELDS, new Object[][]{{"E1", 100}});
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), FIELDS, new Object[]{"E1", 100});

        // remove listener
        stmt.removeAllListeners();

        // send event
        epService.getEPRuntime().sendEvent(new SupportBean("E2", 200));
        subscriber.assertOneReceivedAndReset(stmt, new Object[]{"E2", 200});
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), FIELDS, new Object[][]{{"E2", 200}});
        assertFalse(listener.isInvoked());

        // add listener
        SupportStmtAwareUpdateListener stmtAwareListener = new SupportStmtAwareUpdateListener();
        stmt.addListener(stmtAwareListener);

        // send event
        epService.getEPRuntime().sendEvent(new SupportBean("E3", 300));
        subscriber.assertOneReceivedAndReset(stmt, new Object[]{"E3", 300});
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), FIELDS, new Object[][]{{"E3", 300}});
        EPAssertionUtil.assertProps(stmtAwareListener.assertOneGetNewAndReset(), FIELDS, new Object[]{"E3", 300});

        // subscriber with EPStatement in the footprint
        stmt.removeAllListeners();
        SupportSubscriberRowByRowSpecificWStmt subsWithStatement = new SupportSubscriberRowByRowSpecificWStmt();
        stmt.setSubscriber(subsWithStatement);
        epService.getEPRuntime().sendEvent(new SupportBean("E10", 999));
        subsWithStatement.assertOneReceivedAndReset(stmt, new Object[]{"E10", 999});

        stmt.destroy();
    }

    public class SubscriberFields {
        private ArrayList<Object[]> indicate = new ArrayList<>();

        public void update(String key, int value) {
            indicate.add(new Object[]{key, value});
        }

        List<Object[]> getAndResetIndicate() {
            List<Object[]> result = indicate;
            indicate = new ArrayList<>();
            return result;
        }
    }

    public class SubscriberInterface {
        private ArrayList<SupportMarkerInterface> indicate = new ArrayList<>();

        public void update(SupportMarkerInterface impl) {
            indicate.add(impl);
        }

        List<SupportMarkerInterface> getAndResetIndicate() {
            List<SupportMarkerInterface> result = indicate;
            indicate = new ArrayList<>();
            return result;
        }
    }

    public class SubscriberMap {
        private ArrayList<Map> indicate = new ArrayList<>();

        public void update(Map row) {
            indicate.add(row);
        }

        List<Map> getAndResetIndicate() {
            List<Map> result = indicate;
            indicate = new ArrayList<>();
            return result;
        }
    }
}
