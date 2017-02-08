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
package com.espertech.esper.regression.enummethod;

import com.espertech.esper.client.Configuration;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPServiceProviderManager;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.supportregression.bean.SupportBean_ST0;
import com.espertech.esper.supportregression.bean.SupportBean_ST0_Container;
import com.espertech.esper.supportregression.bean.lrreport.LocationReport;
import com.espertech.esper.supportregression.bean.lrreport.LocationReportFactory;
import com.espertech.esper.supportregression.bean.sales.PersonSales;
import com.espertech.esper.supportregression.bean.sales.Sale;
import com.espertech.esper.supportregression.client.SupportConfigFactory;
import junit.framework.TestCase;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

public class TestEnumNested extends TestCase {

    private EPServiceProvider epService;
    private SupportUpdateListener listener;

    public void setUp() {

        Configuration config = SupportConfigFactory.getConfiguration();
        config.addImport(LocationReportFactory.class);
        config.addEventType("Bean", SupportBean_ST0_Container.class);
        config.addEventType("PersonSales", PersonSales.class);
        config.addEventType("LocationReport", LocationReport.class);
        epService = EPServiceProviderManager.getDefaultProvider(config);
        epService.initialize();
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.startTest(epService, this.getClass(), getName());}
        listener = new SupportUpdateListener();
    }

    protected void tearDown() throws Exception {
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.endTest();}
        listener = null;
    }

    public void testEquivalentToMinByUncorrelated() {

        String eplFragment = "select contained.where(x => (x.p00 = contained.min(y => y.p00))) as val from Bean";
        EPStatement stmtFragment = epService.getEPAdministrator().createEPL(eplFragment);
        stmtFragment.addListener(listener);

        SupportBean_ST0_Container bean = SupportBean_ST0_Container.make2Value("E1,2", "E2,1", "E3,2");
        epService.getEPRuntime().sendEvent(bean);
        Collection<SupportBean_ST0> result = (Collection<SupportBean_ST0>) listener.assertOneGetNewAndReset().get("val");
        EPAssertionUtil.assertEqualsExactOrder(new Object[]{bean.getContained().get(1)}, result.toArray());
    }

    public void testMinByWhere() {

        String eplFragment = "select sales.where(x => x.buyer = persons.minBy(y => age)) as val from PersonSales";
        EPStatement stmtFragment = epService.getEPAdministrator().createEPL(eplFragment);
        stmtFragment.addListener(listener);

        PersonSales bean = PersonSales.make();
        epService.getEPRuntime().sendEvent(bean);

        Collection<Sale> sales = (Collection<Sale>) listener.assertOneGetNewAndReset().get("val");
        EPAssertionUtil.assertEqualsExactOrder(new Object[]{bean.getSales().get(0)}, sales.toArray());
    }

    public void testCorrelated() {

        String eplFragment = "select contained.where(x => x = (contained.firstOf(y => y.p00 = x.p00 ))) as val from Bean";
        EPStatement stmtFragment = epService.getEPAdministrator().createEPL(eplFragment);
        stmtFragment.addListener(listener);

        SupportBean_ST0_Container bean = SupportBean_ST0_Container.make2Value("E1,2", "E2,1", "E3,3");
        epService.getEPRuntime().sendEvent(bean);
        Collection<SupportBean_ST0> result = (Collection<SupportBean_ST0>) listener.assertOneGetNewAndReset().get("val");
        assertEquals(3, result.size());  // this would be 1 if the cache is invalid
    }

    public void testAnyOf() {
        epService.getEPAdministrator().getConfiguration().addEventType(ContainerEvent.class);

        // try "in" with "Set<String> multivalues"
        epService.getEPAdministrator().createEPL("select * from ContainerEvent(level1s.anyOf(x=>x.level2s.anyOf(y => 'A' in (y.multivalues))))").addListener(listener);
        runAssertionContainer();

        // try "in" with "String singlevalue"
        epService.getEPAdministrator().createEPL("select * from ContainerEvent(level1s.anyOf(x=>x.level2s.anyOf(y => y.singlevalue = 'A')))").addListener(listener);
        runAssertionContainer();
    }

    private void runAssertionContainer() {
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

    public static class ContainerEvent
    {
        private final Set<Level1Event> level1s;

        public ContainerEvent(Set<Level1Event> level1s) {
            this.level1s = level1s;
        }

        public Set<Level1Event> getLevel1s() {
            return level1s;
        }
    }

    public static class Level1Event
    {
        private final Set<Level2Event> level2s;

        public Level1Event(Set<Level2Event> level2s) {
            this.level2s = level2s;
        }

        public Set<Level2Event> getLevel2s() {
            return level2s;
        }
    }

    public static class Level2Event
    {
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
