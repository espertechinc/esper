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

package com.espertech.esper.regression.db;

import com.espertech.esper.client.*;
import com.espertech.esper.client.hook.*;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.support.bean.SupportBean;
import com.espertech.esper.support.client.SupportConfigFactory;
import com.espertech.esper.support.epl.SupportDatabaseService;
import junit.framework.TestCase;

import java.sql.Types;
import java.util.Properties;

public class TestDatabaseHintHook extends TestCase
{
    private EPServiceProvider epService;
    private SupportUpdateListener listener;

    public void setUp()
    {
        ConfigurationDBRef configDB = new ConfigurationDBRef();
        configDB.setDriverManagerConnection(SupportDatabaseService.DRIVER, SupportDatabaseService.FULLURL, new Properties());
        configDB.setConnectionLifecycleEnum(ConfigurationDBRef.ConnectionLifecycleEnum.RETAIN);
        configDB.setConnectionCatalog("test");
        configDB.setConnectionReadOnly(true);
        configDB.setConnectionTransactionIsolation(1);
        configDB.setConnectionAutoCommit(true);

        Configuration configuration = SupportConfigFactory.getConfiguration();
        configuration.addDatabaseReference("MyDB", configDB);

        epService = EPServiceProviderManager.getProvider("TestDatabaseJoinRetained", configuration);
        epService.initialize();
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.startTest(epService, this.getClass(), getName());}

        SupportSQLColumnTypeConversion.reset();
    }

    protected void tearDown() throws Exception {
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.endTest();}
        listener = null;
        epService.destroy();
        SupportSQLColumnTypeConversion.reset();
    }

    //@Hook(type=HookType.SQLCOL, hook="this is a sample and not used")
    public void testOutputColumnConversion() {

        epService.getEPAdministrator().getConfiguration().addVariable("myvariable", int.class, 10);

        String fields[] = new String[] {"myint"};
        String stmtText = "@Hook(type=HookType.SQLCOL, hook='" + SupportSQLColumnTypeConversion.class.getName() + "')" +
                "select * from sql:MyDB ['select myint from mytesttable where myint = ${myvariable}']";
        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        listener = new SupportUpdateListener();
        stmt.addListener(listener);

        assertEquals(Boolean.class, stmt.getEventType().getPropertyType("myint"));
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, new Object[][]{{false}});

        // assert contexts
        SQLColumnTypeContext type = SupportSQLColumnTypeConversion.getTypeContexts().get(0);
        assertEquals(Types.INTEGER, type.getColumnSqlType());
        assertEquals("MyDB", type.getDb());
        assertEquals("select myint from mytesttable where myint = ${myvariable}", type.getSql());
        assertEquals("myint", type.getColumnName());
        assertEquals(1, type.getColumnNumber());
        assertEquals(Integer.class, type.getColumnClassType());

        SQLColumnValueContext val = SupportSQLColumnTypeConversion.getValueContexts().get(0);
        assertEquals(10, val.getColumnValue());
        assertEquals("myint", val.getColumnName());
        assertEquals(1, val.getColumnNumber());

        epService.getEPRuntime().setVariableValue("myvariable", 60);    // greater 50 turns true
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, new Object[][]{{true}});
    }

    public void testInputParameterConversion() {

        epService.getEPAdministrator().getConfiguration().addVariable("myvariable", Object.class, "x10");

        String fields[] = new String[] {"myint"};
        String stmtText = "@Hook(type=HookType.SQLCOL, hook='" + SupportSQLColumnTypeConversion.class.getName() + "')" +
                "select * from sql:MyDB ['select myint from mytesttable where myint = ${myvariable}']";
        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        listener = new SupportUpdateListener();
        stmt.addListener(listener);

        epService.getEPRuntime().setVariableValue("myvariable", "x60");    // greater 50 turns true
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, new Object[][]{{true}});

        SQLInputParameterContext param = SupportSQLColumnTypeConversion.getParamContexts().get(0);
        assertEquals(1, param.getParameterNumber());
        assertEquals("x60", param.getParameterValue());
    }

    public void testOutputRowConversion() {

        epService.getEPAdministrator().getConfiguration().addVariable("myvariable", int.class, 10);

        String fields[] = "theString,intPrimitive".split(",");
        String stmtText = "@Hook(type=HookType.SQLROW, hook='" + SupportSQLOutputRowConversion.class.getName() + "')" +
                "select * from sql:MyDB ['select * from mytesttable where myint = ${myvariable}']";
        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        listener = new SupportUpdateListener();
        stmt.addListener(listener);

        assertEquals(SupportBean.class, stmt.getEventType().getUnderlyingType());
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, new Object[][]{{">10<", 99010}});

        SQLOutputRowTypeContext type = SupportSQLOutputRowConversion.getTypeContexts().get(0);
        assertEquals("MyDB", type.getDb());
        assertEquals("select * from mytesttable where myint = ${myvariable}", type.getSql());
        assertEquals(Integer.class, type.getFields().get("myint"));

        SQLOutputRowValueContext val = SupportSQLOutputRowConversion.getValueContexts().get(0);
        assertEquals(10, val.getValues().get("myint"));

        epService.getEPRuntime().setVariableValue("myvariable", 60);    // greater 50 turns true
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, new Object[][]{{">60<", 99060}});

        epService.getEPRuntime().setVariableValue("myvariable", 90);    // greater 50 turns true
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, null);
    }
}
