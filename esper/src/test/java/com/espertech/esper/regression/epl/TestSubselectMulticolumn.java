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
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.client.soda.EPStatementObjectModel;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.bean.SupportBean_S0;
import com.espertech.esper.supportregression.bean.SupportBean_S1;
import com.espertech.esper.supportregression.bean.SupportMarketDataBean;
import com.espertech.esper.supportregression.client.SupportConfigFactory;
import junit.framework.TestCase;

import java.util.Map;

public class TestSubselectMulticolumn extends TestCase
{
    private EPServiceProvider epService;
    private SupportUpdateListener listener;

    public void setUp()
    {
        Configuration config = SupportConfigFactory.getConfiguration();        
        config.addEventType("SupportBean", SupportBean.class);
        config.addEventType("S0", SupportBean_S0.class);
        config.addEventType("S1", SupportBean_S1.class);
        config.addEventType("MarketData", SupportMarketDataBean.class);
        epService = EPServiceProviderManager.getDefaultProvider(config);
        epService.initialize();
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.startTest(epService, this.getClass(), getName());}
        listener = new SupportUpdateListener();
    }

    protected void tearDown() throws Exception {
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.endTest();}
        listener = null;
    }

    public void testInvalid() {

        String epl = "select (select theString, sum(intPrimitive) from SupportBean#lastevent as sb) from S0";
        tryInvalid(epl, "Error starting statement: Failed to plan subquery number 1 querying SupportBean: Subquery with multi-column select requires that either all or none of the selected columns are under aggregation, unless a group-by clause is also specified [select (select theString, sum(intPrimitive) from SupportBean#lastevent as sb) from S0]");

        epl = "select (select theString, theString from SupportBean#lastevent as sb) from S0";
        tryInvalid(epl, "Error starting statement: Column 1 in subquery does not have a unique column name assigned [select (select theString, theString from SupportBean#lastevent as sb) from S0]");

        epl = "select * from S0(p00 = (select theString, theString from SupportBean#lastevent as sb))";
        tryInvalid(epl, "Failed to validate subquery number 1 querying SupportBean: Subquery multi-column select is not allowed in this context. [select * from S0(p00 = (select theString, theString from SupportBean#lastevent as sb))]");

        epl = "select exists(select sb.* as v1, intPrimitive*2 as v3 from SupportBean#lastevent as sb) as subrow from S0 as s0";
        tryInvalid(epl, "Error starting statement: Failed to plan subquery number 1 querying SupportBean: Subquery multi-column select does not allow wildcard or stream wildcard when selecting multiple columns. [select exists(select sb.* as v1, intPrimitive*2 as v3 from SupportBean#lastevent as sb) as subrow from S0 as s0]");

        epl = "select (select sb.* as v1, intPrimitive*2 as v3 from SupportBean#lastevent as sb) as subrow from S0 as s0";
        tryInvalid(epl, "Error starting statement: Failed to plan subquery number 1 querying SupportBean: Subquery multi-column select does not allow wildcard or stream wildcard when selecting multiple columns. [select (select sb.* as v1, intPrimitive*2 as v3 from SupportBean#lastevent as sb) as subrow from S0 as s0]");

        epl = "select (select *, intPrimitive from SupportBean#lastevent as sb) as subrow from S0 as s0";
        tryInvalid(epl, "Error starting statement: Failed to plan subquery number 1 querying SupportBean: Subquery multi-column select does not allow wildcard or stream wildcard when selecting multiple columns. [select (select *, intPrimitive from SupportBean#lastevent as sb) as subrow from S0 as s0]");

        epl = "select * from S0(p00 in (select theString, theString from SupportBean#lastevent as sb))";
        tryInvalid(epl, "Failed to validate subquery number 1 querying SupportBean: Subquery multi-column select is not allowed in this context. [select * from S0(p00 in (select theString, theString from SupportBean#lastevent as sb))]");
    }

    private void tryInvalid(String epl, String message) {
        try {
            epService.getEPAdministrator().createEPL(epl);
            fail();
        }
        catch (EPStatementException ex) {
            assertEquals(message, ex.getMessage());
        }
    }

    public void testColumnsUncorrelated()
    {
        String stmtText = "select " +
                "(select theString as v1, intPrimitive as v2 from SupportBean#lastevent) as subrow " +
                "from S0 as s0";
        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        stmt.addListener(listener);
        
        runAssertion(stmt);
        
        stmt.destroy();
        EPStatementObjectModel model = epService.getEPAdministrator().compileEPL(stmtText);
        assertEquals(stmtText, model.toEPL());
        stmt = epService.getEPAdministrator().create(model);
        stmt.addListener(listener);
        assertEquals(stmtText, stmt.getText());

        runAssertion(stmt);
    }

    private void runAssertion(EPStatement stmt) {

        FragmentEventType fragmentType = stmt.getEventType().getFragmentType("subrow");
        assertFalse(fragmentType.isIndexed());
        assertFalse(fragmentType.isNative());
        Object[][] rows = new Object[][] {
                {"v1", String.class},
                {"v2", int.class},
                };
        for (int i = 0; i < rows.length; i++) {
            String message = "Failed assertion for " + rows[i][0];
            EventPropertyDescriptor prop = fragmentType.getFragmentType().getPropertyDescriptors()[i];
            assertEquals(message, rows[i][0], prop.getPropertyName());
            assertEquals(message, rows[i][1], prop.getPropertyType());
        }

        String[] fields = "subrow.v1,subrow.v2".split(",");

        epService.getEPRuntime().sendEvent(new SupportBean_S0(1));
        EventBean theEvent = listener.assertOneGetNewAndReset();
        EPAssertionUtil.assertProps(theEvent, fields, new Object[]{null, null});

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 10));
        epService.getEPRuntime().sendEvent(new SupportBean_S0(2));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E1", 10});

        epService.getEPRuntime().sendEvent(new SupportBean("E2", 20));
        epService.getEPRuntime().sendEvent(new SupportBean_S0(3));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E2", 20});
    }

    public void testCorrelatedAggregation()
    {
        String stmtText = "select p00, " +
                "(select " +
                "  sum(intPrimitive) as v1, " +
                "  sum(intPrimitive + 1) as v2, " +
                "  window(intPrimitive) as v3, " +
                "  window(sb.*) as v4 " +
                "  from SupportBean#keepall sb " +
                "  where theString = s0.p00) as subrow " +
                "from S0 as s0";
        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        stmt.addListener(listener);

        Object[][] rows = new Object[][] {
                {"p00", String.class, false},
                {"subrow", Map.class, true}
                };
        for (int i = 0; i < rows.length; i++) {
            String message = "Failed assertion for " + rows[i][0];
            EventPropertyDescriptor prop = stmt.getEventType().getPropertyDescriptors()[i];
            assertEquals(message, rows[i][0], prop.getPropertyName());
            assertEquals(message, rows[i][1], prop.getPropertyType());
            assertEquals(message, rows[i][2], prop.isFragment());
        }

        FragmentEventType fragmentType = stmt.getEventType().getFragmentType("subrow");
        assertFalse(fragmentType.isIndexed());
        assertFalse(fragmentType.isNative());
        rows = new Object[][] {
                {"v1", Integer.class},
                {"v2", Integer.class},
                {"v3", int[].class},
                {"v4", SupportBean[].class},
                };
        for (int i = 0; i < rows.length; i++) {
            String message = "Failed assertion for " + rows[i][0];
            EventPropertyDescriptor prop = fragmentType.getFragmentType().getPropertyDescriptors()[i];
            assertEquals(message, rows[i][0], prop.getPropertyName());
            assertEquals(message, rows[i][1], prop.getPropertyType());
        }

        String[] fields = "p00,subrow.v1,subrow.v2".split(",");

        epService.getEPRuntime().sendEvent(new SupportBean_S0(1, "T1"));
        EventBean row = listener.assertOneGetNewAndReset();
        EPAssertionUtil.assertProps(row, fields, new Object[]{"T1", null, null});
        assertNull(row.get("subrow.v3"));
        assertNull(row.get("subrow.v4"));

        SupportBean sb1 = new SupportBean("T1", 10);
        epService.getEPRuntime().sendEvent(sb1);
        epService.getEPRuntime().sendEvent(new SupportBean_S0(2, "T1"));
        row = listener.assertOneGetNewAndReset();
        EPAssertionUtil.assertProps(row, fields, new Object[]{"T1", 10, 11});
        EPAssertionUtil.assertEqualsAnyOrder((int[]) row.get("subrow.v3"), new int[]{10});
        EPAssertionUtil.assertEqualsAnyOrder((Object[]) row.get("subrow.v4"), new Object[]{sb1});

        SupportBean sb2 = new SupportBean("T1", 20);
        epService.getEPRuntime().sendEvent(sb2);
        epService.getEPRuntime().sendEvent(new SupportBean_S0(3, "T1"));
        row = listener.assertOneGetNewAndReset();
        EPAssertionUtil.assertProps(row, fields, new Object[]{"T1", 30, 32});
        EPAssertionUtil.assertEqualsAnyOrder((int[]) row.get("subrow.v3"), new int[]{10, 20});
        EPAssertionUtil.assertEqualsAnyOrder((Object[]) row.get("subrow.v4"), new Object[]{sb1, sb2});
    }
}
