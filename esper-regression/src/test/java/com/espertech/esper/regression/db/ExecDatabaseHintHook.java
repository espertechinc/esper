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
package com.espertech.esper.regression.db;

import com.espertech.esper.client.Configuration;
import com.espertech.esper.client.ConfigurationDBRef;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.hook.*;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.db.SupportSQLColumnTypeConversion;
import com.espertech.esper.supportregression.db.SupportSQLOutputRowConversion;
import com.espertech.esper.supportregression.epl.SupportDatabaseService;
import com.espertech.esper.supportregression.execution.RegressionExecution;

import java.sql.Types;
import java.util.Properties;

import static org.junit.Assert.assertEquals;

public class ExecDatabaseHintHook implements RegressionExecution {
    public void configure(Configuration configuration) throws Exception {
        ConfigurationDBRef configDB = new ConfigurationDBRef();
        configDB.setDriverManagerConnection(SupportDatabaseService.DRIVER, SupportDatabaseService.FULLURL, new Properties());
        configDB.setConnectionLifecycleEnum(ConfigurationDBRef.ConnectionLifecycleEnum.RETAIN);
        configDB.setConnectionCatalog("test");
        configDB.setConnectionReadOnly(true);
        configDB.setConnectionTransactionIsolation(1);
        configDB.setConnectionAutoCommit(true);
        configuration.addDatabaseReference("MyDB", configDB);
    }

    public void run(EPServiceProvider epService) throws Exception {
        runAssertionOutputColumnConversion(epService);
        runAssertionInputParameterConversion(epService);
        runAssertionOutputRowConversion(epService);
    }

    //@Hook(type=HookType.SQLCOL, hook="this is a sample and not used")
    private void runAssertionOutputColumnConversion(EPServiceProvider epService) {
        SupportSQLColumnTypeConversion.reset();
        epService.getEPAdministrator().getConfiguration().addVariable("myvariableOCC", int.class, 10);

        String[] fields = new String[]{"myint"};
        String stmtText = "@Hook(type=HookType.SQLCOL, hook='" + SupportSQLColumnTypeConversion.class.getName() + "')" +
                "select * from sql:MyDB ['select myint from mytesttable where myint = ${myvariableOCC}']";
        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        assertEquals(Boolean.class, stmt.getEventType().getPropertyType("myint"));
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, new Object[][]{{false}});

        // assert contexts
        SQLColumnTypeContext type = SupportSQLColumnTypeConversion.getTypeContexts().get(0);
        assertEquals(Types.INTEGER, type.getColumnSqlType());
        assertEquals("MyDB", type.getDb());
        assertEquals("select myint from mytesttable where myint = ${myvariableOCC}", type.getSql());
        assertEquals("myint", type.getColumnName());
        assertEquals(1, type.getColumnNumber());
        assertEquals(Integer.class, type.getColumnClassType());

        SQLColumnValueContext val = SupportSQLColumnTypeConversion.getValueContexts().get(0);
        assertEquals(10, val.getColumnValue());
        assertEquals("myint", val.getColumnName());
        assertEquals(1, val.getColumnNumber());

        epService.getEPRuntime().setVariableValue("myvariableOCC", 60);    // greater 50 turns true
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, new Object[][]{{true}});

        stmt.destroy();
    }

    private void runAssertionInputParameterConversion(EPServiceProvider epService) {
        SupportSQLColumnTypeConversion.reset();
        epService.getEPAdministrator().getConfiguration().addVariable("myvariableIPC", Object.class, "x10");

        String[] fields = new String[]{"myint"};
        String stmtText = "@Hook(type=HookType.SQLCOL, hook='" + SupportSQLColumnTypeConversion.class.getName() + "')" +
                "select * from sql:MyDB ['select myint from mytesttable where myint = ${myvariableIPC}']";
        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        epService.getEPRuntime().setVariableValue("myvariableIPC", "x60");    // greater 50 turns true
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, new Object[][]{{true}});

        SQLInputParameterContext param = SupportSQLColumnTypeConversion.getParamContexts().get(0);
        assertEquals(1, param.getParameterNumber());
        assertEquals("x60", param.getParameterValue());

        stmt.destroy();
    }

    private void runAssertionOutputRowConversion(EPServiceProvider epService) {
        SupportSQLColumnTypeConversion.reset();
        epService.getEPAdministrator().getConfiguration().addVariable("myvariableORC", int.class, 10);

        String[] fields = "theString,intPrimitive".split(",");
        String stmtText = "@Hook(type=HookType.SQLROW, hook='" + SupportSQLOutputRowConversion.class.getName() + "')" +
                "select * from sql:MyDB ['select * from mytesttable where myint = ${myvariableORC}']";
        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        assertEquals(SupportBean.class, stmt.getEventType().getUnderlyingType());
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, new Object[][]{{">10<", 99010}});

        SQLOutputRowTypeContext type = SupportSQLOutputRowConversion.getTypeContexts().get(0);
        assertEquals("MyDB", type.getDb());
        assertEquals("select * from mytesttable where myint = ${myvariableORC}", type.getSql());
        assertEquals(Integer.class, type.getFields().get("myint"));

        SQLOutputRowValueContext val = SupportSQLOutputRowConversion.getValueContexts().get(0);
        assertEquals(10, val.getValues().get("myint"));

        epService.getEPRuntime().setVariableValue("myvariableORC", 60);    // greater 50 turns true
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, new Object[][]{{">60<", 99060}});

        epService.getEPRuntime().setVariableValue("myvariableORC", 90);    // greater 50 turns true
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, null);

        stmt.destroy();
    }
}
