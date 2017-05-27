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
import com.espertech.esper.supportregression.bean.lrreport.LocationReport;
import com.espertech.esper.supportregression.bean.lrreport.LocationReportFactory;
import com.espertech.esper.supportregression.bean.sales.PersonSales;
import com.espertech.esper.supportregression.bean.sales.Sale;
import com.espertech.esper.supportregression.execution.RegressionExecution;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import static org.junit.Assert.*;

public class ExecEnumNested implements RegressionExecution {

    public void configure(Configuration configuration) throws Exception {
        configuration.addImport(LocationReportFactory.class);
        configuration.addEventType("Bean", SupportBean_ST0_Container.class);
        configuration.addEventType("PersonSales", PersonSales.class);
        configuration.addEventType("LocationReport", LocationReport.class);
    }

    public void run(EPServiceProvider epService) throws Exception {
        runAssertionEquivalentToMinByUncorrelated(epService);
        runAssertionMinByWhere(epService);
        runAssertionCorrelated(epService);
        runAssertionAnyOf(epService);
    }

    private void runAssertionEquivalentToMinByUncorrelated(EPServiceProvider epService) {

        String eplFragment = "select contained.where(x => (x.p00 = contained.min(y => y.p00))) as val from Bean";
        EPStatement stmtFragment = epService.getEPAdministrator().createEPL(eplFragment);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmtFragment.addListener(listener);

        SupportBean_ST0_Container bean = SupportBean_ST0_Container.make2Value("E1,2", "E2,1", "E3,2");
        epService.getEPRuntime().sendEvent(bean);
        Collection<SupportBean_ST0> result = (Collection<SupportBean_ST0>) listener.assertOneGetNewAndReset().get("val");
        EPAssertionUtil.assertEqualsExactOrder(new Object[]{bean.getContained().get(1)}, result.toArray());
    }

    private void runAssertionMinByWhere(EPServiceProvider epService) {

        String eplFragment = "select sales.where(x => x.buyer = persons.minBy(y => age)) as val from PersonSales";
        EPStatement stmtFragment = epService.getEPAdministrator().createEPL(eplFragment);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmtFragment.addListener(listener);

        PersonSales bean = PersonSales.make();
        epService.getEPRuntime().sendEvent(bean);

        Collection<Sale> sales = (Collection<Sale>) listener.assertOneGetNewAndReset().get("val");
        EPAssertionUtil.assertEqualsExactOrder(new Object[]{bean.getSales().get(0)}, sales.toArray());
    }

    private void runAssertionCorrelated(EPServiceProvider epService) {

        String eplFragment = "select contained.where(x => x = (contained.firstOf(y => y.p00 = x.p00 ))) as val from Bean";
        EPStatement stmtFragment = epService.getEPAdministrator().createEPL(eplFragment);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmtFragment.addListener(listener);

        SupportBean_ST0_Container bean = SupportBean_ST0_Container.make2Value("E1,2", "E2,1", "E3,3");
        epService.getEPRuntime().sendEvent(bean);
        Collection<SupportBean_ST0> result = (Collection<SupportBean_ST0>) listener.assertOneGetNewAndReset().get("val");
        assertEquals(3, result.size());  // this would be 1 if the cache is invalid
    }

    private void runAssertionAnyOf(EPServiceProvider epService) {
        epService.getEPAdministrator().getConfiguration().addEventType(ContainerEvent.class);
        SupportUpdateListener listener = new SupportUpdateListener();

        // try "in" with "Set<String> multivalues"
        epService.getEPAdministrator().createEPL("select * from ContainerEvent(level1s.anyOf(x=>x.level2s.anyOf(y => 'A' in (y.multivalues))))").addListener(listener);
        tryAssertionAnyOf(epService, listener);
        epService.getEPAdministrator().destroyAllStatements();

        // try "in" with "String singlevalue"
        epService.getEPAdministrator().createEPL("select * from ContainerEvent(level1s.anyOf(x=>x.level2s.anyOf(y => y.singlevalue = 'A')))").addListener(listener);
        tryAssertionAnyOf(epService, listener);
        epService.getEPAdministrator().destroyAllStatements();
    }

    private void tryAssertionAnyOf(EPServiceProvider epService, SupportUpdateListener listener) {
        epService.getEPRuntime().sendEvent(makeContainerEvent("A"));
        assertTrue(listener.getAndClearIsInvoked());

        epService.getEPRuntime().sendEvent(makeContainerEvent("B"));
        assertFalse(listener.getAndClearIsInvoked());
    }

    private ContainerEvent makeContainerEvent(String value) {
        Set<Level1Event> level1s = new LinkedHashSet<Level1Event>();
        level1s.add(new Level1Event(Collections.singleton(new Level2Event(Collections.singleton("X1"), "X1"))));
        level1s.add(new Level1Event(Collections.singleton(new Level2Event(Collections.singleton(value), value))));
        level1s.add(new Level1Event(Collections.singleton(new Level2Event(Collections.singleton("X2"), "X2"))));
        return new ContainerEvent(level1s);
    }

    public static class ContainerEvent {
        private final Set<Level1Event> level1s;

        public ContainerEvent(Set<Level1Event> level1s) {
            this.level1s = level1s;
        }

        public Set<Level1Event> getLevel1s() {
            return level1s;
        }
    }

    public static class Level1Event {
        private final Set<Level2Event> level2s;

        public Level1Event(Set<Level2Event> level2s) {
            this.level2s = level2s;
        }

        public Set<Level2Event> getLevel2s() {
            return level2s;
        }
    }

    public static class Level2Event {
        private final Set<String> multivalues;
        private final String singlevalue;

        public Level2Event(Set<String> multivalues, String singlevalue) {
            this.multivalues = multivalues;
            this.singlevalue = singlevalue;
        }

        public Set<String> getMultivalues() {
            return multivalues;
        }

        public String getSinglevalue() {
            return singlevalue;
        }
    }
}
