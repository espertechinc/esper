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

package com.espertech.esper.regression.nwtable;

import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import junit.framework.TestCase;
import com.espertech.esper.client.Configuration;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPServiceProviderManager;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.soda.*;
import com.espertech.esper.support.bean.SupportBean;
import com.espertech.esper.support.bean.SupportMarketDataBean;
import com.espertech.esper.support.bean.SupportBean_B;
import com.espertech.esper.support.client.SupportConfigFactory;

public class TestNamedWindowOM extends TestCase
{
    private EPServiceProvider epService;
    private SupportUpdateListener listenerWindow;
    private SupportUpdateListener listenerStmtOne;
    private SupportUpdateListener listenerOnSelect;

    public void setUp()
    {
        Configuration config = SupportConfigFactory.getConfiguration();
        epService = EPServiceProviderManager.getDefaultProvider(config);
        epService.initialize();
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.startTest(epService, this.getClass(), getName());}
        listenerWindow = new SupportUpdateListener();
        listenerStmtOne = new SupportUpdateListener();
        listenerOnSelect = new SupportUpdateListener();
    }

    protected void tearDown() throws Exception {
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.endTest();}
        listenerWindow = null;
        listenerStmtOne = null;
        listenerOnSelect = null;
    }

    public void testCompile()
    {
        String[] fields = new String[] {"key", "value"};
        String stmtTextCreate = "create window MyWindow.win:keepall() as select theString as key, longBoxed as value from " + SupportBean.class.getName();
        EPStatementObjectModel modelCreate = epService.getEPAdministrator().compileEPL(stmtTextCreate);
        EPStatement stmtCreate = epService.getEPAdministrator().create(modelCreate);
        stmtCreate.addListener(listenerWindow);
        assertEquals("create window MyWindow.win:keepall() as select theString as key, longBoxed as value from com.espertech.esper.support.bean.SupportBean", modelCreate.toEPL());

        String stmtTextOnSelect = "on " + SupportBean_B.class.getName() + " select mywin.* from MyWindow as mywin";
        EPStatementObjectModel modelOnSelect = epService.getEPAdministrator().compileEPL(stmtTextOnSelect);
        EPStatement stmtOnSelect = epService.getEPAdministrator().create(modelOnSelect);
        stmtOnSelect.addListener(listenerOnSelect);

        String stmtTextInsert = "insert into MyWindow select theString as key, longBoxed as value from " + SupportBean.class.getName();
        EPStatementObjectModel modelInsert = epService.getEPAdministrator().compileEPL(stmtTextInsert);
        EPStatement stmtInsert = epService.getEPAdministrator().create(modelInsert);

        String stmtTextSelectOne = "select irstream key, value*2 as value from MyWindow(key is not null)";
        EPStatementObjectModel modelSelect = epService.getEPAdministrator().compileEPL(stmtTextSelectOne);
        EPStatement stmtSelectOne = epService.getEPAdministrator().create(modelSelect);
        stmtSelectOne.addListener(listenerStmtOne);
        assertEquals(stmtTextSelectOne, modelSelect.toEPL());

        // send events
        sendSupportBean("E1", 10L);
        EPAssertionUtil.assertProps(listenerStmtOne.assertOneGetNewAndReset(), fields, new Object[]{"E1", 20L});
        EPAssertionUtil.assertProps(listenerWindow.assertOneGetNewAndReset(), fields, new Object[]{"E1", 10L});

        sendSupportBean("E2", 20L);
        EPAssertionUtil.assertProps(listenerStmtOne.assertOneGetNewAndReset(), fields, new Object[]{"E2", 40L});
        EPAssertionUtil.assertProps(listenerWindow.assertOneGetNewAndReset(), fields, new Object[]{"E2", 20L});

        // create delete stmt
        String stmtTextDelete = "on " + SupportMarketDataBean.class.getName() + " as s0 delete from MyWindow as s1 where s0.symbol=s1.key";
        EPStatementObjectModel modelDelete = epService.getEPAdministrator().compileEPL(stmtTextDelete);
        epService.getEPAdministrator().create(modelDelete);
        assertEquals("on com.espertech.esper.support.bean.SupportMarketDataBean as s0 delete from MyWindow as s1 where s0.symbol=s1.key", modelDelete.toEPL());

        // send delete event
        sendMarketBean("E1");
        EPAssertionUtil.assertProps(listenerStmtOne.assertOneGetOldAndReset(), fields, new Object[]{"E1", 20L});
        EPAssertionUtil.assertProps(listenerWindow.assertOneGetOldAndReset(), fields, new Object[]{"E1", 10L});

        // send delete event again, none deleted now
        sendMarketBean("E1");
        assertFalse(listenerStmtOne.isInvoked());
        assertFalse(listenerWindow.isInvoked());

        // send delete event
        sendMarketBean("E2");
        EPAssertionUtil.assertProps(listenerStmtOne.assertOneGetOldAndReset(), fields, new Object[]{"E2", 40L});
        EPAssertionUtil.assertProps(listenerWindow.assertOneGetOldAndReset(), fields, new Object[]{"E2", 20L});

        // trigger on-select on empty window
        assertFalse(listenerOnSelect.isInvoked());
        epService.getEPRuntime().sendEvent(new SupportBean_B("B1"));
        assertFalse(listenerOnSelect.isInvoked());

        sendSupportBean("E3", 30L);
        EPAssertionUtil.assertProps(listenerStmtOne.assertOneGetNewAndReset(), fields, new Object[]{"E3", 60L});
        EPAssertionUtil.assertProps(listenerWindow.assertOneGetNewAndReset(), fields, new Object[]{"E3", 30L});

        // trigger on-select on the filled window
        epService.getEPRuntime().sendEvent(new SupportBean_B("B2"));
        EPAssertionUtil.assertProps(listenerOnSelect.assertOneGetNewAndReset(), fields, new Object[]{"E3", 30L});

        stmtSelectOne.destroy();
        stmtInsert.destroy();
        stmtCreate.destroy();
    }

    public void testOM()
    {
        String[] fields = new String[] {"key", "value"};

        // create window object model
        EPStatementObjectModel model = new EPStatementObjectModel();
        model.setCreateWindow(CreateWindowClause.create("MyWindow").addView("win", "keepall"));
        model.setSelectClause(SelectClause.create()
                .addWithAsProvidedName("theString", "key")
                .addWithAsProvidedName("longBoxed", "value"));
        model.setFromClause(FromClause.create(FilterStream.create(SupportBean.class.getName())));

        EPStatement stmtCreate = epService.getEPAdministrator().create(model);
        stmtCreate.addListener(listenerWindow);

        String stmtTextCreate = "create window MyWindow.win:keepall() as select theString as key, longBoxed as value from " + SupportBean.class.getName();
        assertEquals(stmtTextCreate, model.toEPL());

        String stmtTextInsert = "insert into MyWindow select theString as key, longBoxed as value from " + SupportBean.class.getName();
        EPStatementObjectModel modelInsert = epService.getEPAdministrator().compileEPL(stmtTextInsert);
        EPStatement stmtInsert = epService.getEPAdministrator().create(modelInsert);

        // Consumer statement object model
        model = new EPStatementObjectModel();
        Expression multi = Expressions.multiply(Expressions.property("value"), Expressions.constant(2));
        model.setSelectClause(SelectClause.create().streamSelector(StreamSelector.RSTREAM_ISTREAM_BOTH)
                .add("key")
                .add(multi, "value"));
        model.setFromClause(FromClause.create(FilterStream.create("MyWindow", Expressions.isNotNull("value"))));

        EPStatement stmtSelectOne = epService.getEPAdministrator().create(model);
        stmtSelectOne.addListener(listenerStmtOne);
        String stmtTextSelectOne = "select irstream key, value*2 as value from MyWindow(value is not null)";
        assertEquals(stmtTextSelectOne, model.toEPL());

        // send events
        sendSupportBean("E1", 10L);
        EPAssertionUtil.assertProps(listenerStmtOne.assertOneGetNewAndReset(), fields, new Object[]{"E1", 20L});
        EPAssertionUtil.assertProps(listenerWindow.assertOneGetNewAndReset(), fields, new Object[]{"E1", 10L});

        sendSupportBean("E2", 20L);
        EPAssertionUtil.assertProps(listenerStmtOne.assertOneGetNewAndReset(), fields, new Object[]{"E2", 40L});
        EPAssertionUtil.assertProps(listenerWindow.assertOneGetNewAndReset(), fields, new Object[]{"E2", 20L});

        // create delete stmt
        model = new EPStatementObjectModel();
        model.setOnExpr(OnClause.createOnDelete("MyWindow", "s1"));
        model.setFromClause(FromClause.create(FilterStream.create(SupportMarketDataBean.class.getName(), "s0")));
        model.setWhereClause(Expressions.eqProperty("s0.symbol", "s1.key"));
        epService.getEPAdministrator().create(model);
        String stmtTextDelete = "on " + SupportMarketDataBean.class.getName() + " as s0 delete from MyWindow as s1 where s0.symbol=s1.key";
        assertEquals(stmtTextDelete, model.toEPL());

        // send delete event
        sendMarketBean("E1");
        EPAssertionUtil.assertProps(listenerStmtOne.assertOneGetOldAndReset(), fields, new Object[]{"E1", 20L});
        EPAssertionUtil.assertProps(listenerWindow.assertOneGetOldAndReset(), fields, new Object[]{"E1", 10L});

        // send delete event again, none deleted now
        sendMarketBean("E1");
        assertFalse(listenerStmtOne.isInvoked());
        assertFalse(listenerWindow.isInvoked());

        // send delete event
        sendMarketBean("E2");
        EPAssertionUtil.assertProps(listenerStmtOne.assertOneGetOldAndReset(), fields, new Object[]{"E2", 40L});
        EPAssertionUtil.assertProps(listenerWindow.assertOneGetOldAndReset(), fields, new Object[]{"E2", 20L});

        // On-select object model
        model = new EPStatementObjectModel();
        model.setOnExpr(OnClause.createOnSelect("MyWindow", "s1"));
        model.setWhereClause(Expressions.eqProperty("s0.id", "s1.key"));
        model.setFromClause(FromClause.create(FilterStream.create(SupportBean_B.class.getName(), "s0")));
        model.setSelectClause(SelectClause.createStreamWildcard("s1"));
        EPStatement statement = epService.getEPAdministrator().create(model);
        statement.addListener(listenerOnSelect);
        String stmtTextOnSelect = "on " + SupportBean_B.class.getName() + " as s0 select s1.* from MyWindow as s1 where s0.id=s1.key";
        assertEquals(stmtTextOnSelect, model.toEPL());

        // send some more events
        sendSupportBean("E3", 30L);
        sendSupportBean("E4", 40L);

        epService.getEPRuntime().sendEvent(new SupportBean_B("B1"));
        assertFalse(listenerOnSelect.isInvoked());

        // trigger on-select
        epService.getEPRuntime().sendEvent(new SupportBean_B("E3"));
        EPAssertionUtil.assertProps(listenerOnSelect.assertOneGetNewAndReset(), fields, new Object[]{"E3", 30L});

        stmtSelectOne.destroy();
        stmtInsert.destroy();
        stmtCreate.destroy();
    }

    public void testOMCreateTableSyntax()
    {
        String expected = "create window MyWindow.win:keepall() as (a1 string, a2 double, a3 int)";

        // create window object model
        EPStatementObjectModel model = new EPStatementObjectModel();
        CreateWindowClause clause = CreateWindowClause.create("MyWindow").addView("win", "keepall");
        clause.addColumn(new SchemaColumnDesc("a1", "string", false, false));
        clause.addColumn(new SchemaColumnDesc("a2", "double", false, false));
        clause.addColumn(new SchemaColumnDesc("a3", "int", false, false));
        model.setCreateWindow(clause);
        assertEquals(expected, model.toEPL());

        EPStatement stmtCreate = epService.getEPAdministrator().create(model);
        assertEquals(expected, stmtCreate.getText());
    }

    private SupportBean sendSupportBean(String theString, Long longBoxed)
    {
        SupportBean bean = new SupportBean();
        bean.setTheString(theString);
        bean.setLongBoxed(longBoxed);
        epService.getEPRuntime().sendEvent(bean);
        return bean;
    }

    private void sendMarketBean(String symbol)
    {
        SupportMarketDataBean bean = new SupportMarketDataBean(symbol, 0, 0l, "");
        epService.getEPRuntime().sendEvent(bean);
    }
}
