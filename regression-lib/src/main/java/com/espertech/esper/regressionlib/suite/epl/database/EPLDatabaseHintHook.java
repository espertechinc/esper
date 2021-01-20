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
package com.espertech.esper.regressionlib.suite.epl.database;

import com.espertech.esper.common.client.hook.type.*;
import com.espertech.esper.common.client.type.EPTypePremade;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.support.util.SupportSQLColumnTypeConversion;
import com.espertech.esper.regressionlib.support.util.SupportSQLOutputRowConversion;
import org.junit.Assert;

import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

public class EPLDatabaseHintHook {

    public static List<RegressionExecution> executions() {
        List<RegressionExecution> execs = new ArrayList<>();
        execs.add(new EPLDatabaseOutputColumnConversion());
        execs.add(new EPLDatabaseInputParameterConversion());
        execs.add(new EPLDatabaseOutputRowConversion());
        return execs;
    }

    //@Hook(type=HookType.SQLCOL, hook="this is a sample and not used")
    private static class EPLDatabaseOutputColumnConversion implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            SupportSQLColumnTypeConversion.reset();

            String[] fields = new String[]{"myint"};
            String stmtText = "@name('s0') @Hook(type=HookType.SQLCOL, hook='" + SupportSQLColumnTypeConversion.class.getName() + "')" +
                "select * from sql:MyDBWithTxnIso1WithReadOnly ['select myint from mytesttable where myint = ${myvariableOCC}']";
            env.compileDeploy(stmtText);

            env.assertStatement("s0", statement ->  Assert.assertEquals(Boolean.class, statement.getEventType().getPropertyType("myint")));
            env.assertPropsPerRowIteratorAnyOrder("s0", fields, new Object[][]{{false}});

            // assert contexts
            env.assertThat(() -> {
                SQLColumnTypeContext type = SupportSQLColumnTypeConversion.getTypeContexts().get(0);
                Assert.assertEquals(Types.INTEGER, type.getColumnSqlType());
                Assert.assertEquals("MyDBWithTxnIso1WithReadOnly", type.getDb());
                Assert.assertEquals("select myint from mytesttable where myint = ${myvariableOCC}", type.getSql());
                Assert.assertEquals("myint", type.getColumnName());
                Assert.assertEquals(1, type.getColumnNumber());
                Assert.assertEquals(EPTypePremade.INTEGERBOXED.getEPType(), type.getColumnClassType());

                SQLColumnValueContext val = SupportSQLColumnTypeConversion.getValueContexts().get(0);
                Assert.assertEquals(10, val.getColumnValue());
                Assert.assertEquals("myint", val.getColumnName());
                Assert.assertEquals(1, val.getColumnNumber());
            });

            env.runtimeSetVariable(null, "myvariableOCC", 60);    // greater 50 turns true
            env.assertPropsPerRowIteratorAnyOrder("s0", fields, new Object[][]{{true}});

            env.undeployAll();
        }
    }

    private static class EPLDatabaseInputParameterConversion implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            SupportSQLColumnTypeConversion.reset();

            String[] fields = new String[]{"myint"};
            String stmtText = "@name('s0') @Hook(type=HookType.SQLCOL, hook='" + SupportSQLColumnTypeConversion.class.getName() + "')" +
                "select * from sql:MyDBWithTxnIso1WithReadOnly ['select myint from mytesttable where myint = ${myvariableIPC}']";
            env.compileDeploy(stmtText);

            env.runtimeSetVariable(null, "myvariableIPC", "x60");    // greater 50 turns true
            env.assertPropsPerRowIteratorAnyOrder("s0", fields, new Object[][]{{true}});

            env.assertThat(() -> {
                SQLInputParameterContext param = SupportSQLColumnTypeConversion.getParamContexts().get(0);
                Assert.assertEquals(1, param.getParameterNumber());
                Assert.assertEquals("x60", param.getParameterValue());
            });

            env.undeployAll();
        }
    }

    private static class EPLDatabaseOutputRowConversion implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            SupportSQLColumnTypeConversion.reset();

            String[] fields = "theString,intPrimitive".split(",");
            String stmtText = "@name('s0') @Hook(type=HookType.SQLROW, hook='" + SupportSQLOutputRowConversion.class.getName() + "')" +
                "select * from sql:MyDBWithTxnIso1WithReadOnly ['select * from mytesttable where myint = ${myvariableORC}']";
            env.compileDeploy(stmtText);

            env.assertStatement("s0", statement -> Assert.assertEquals(SupportBean.class, statement.getEventType().getUnderlyingType()));
            env.assertPropsPerRowIteratorAnyOrder("s0", fields, new Object[][]{{">10<", 99010}});

            env.assertThat(() -> {
                SQLOutputRowTypeContext type = SupportSQLOutputRowConversion.getTypeContexts().get(0);
                Assert.assertEquals("MyDBWithTxnIso1WithReadOnly", type.getDb());
                Assert.assertEquals("select * from mytesttable where myint = ${myvariableORC}", type.getSql());
                Assert.assertEquals(EPTypePremade.INTEGERBOXED.getEPType(), type.getFields().get("myint"));

                SQLOutputRowValueContext val = SupportSQLOutputRowConversion.getValueContexts().get(0);
                Assert.assertEquals(10, val.getValues().get("myint"));
            });

            env.runtimeSetVariable(null, "myvariableORC", 60);    // greater 50 turns true
            env.assertPropsPerRowIteratorAnyOrder("s0", fields, new Object[][]{{">60<", 99060}});

            env.runtimeSetVariable(null, "myvariableORC", 90);    // greater 50 turns true
            env.assertPropsPerRowIteratorAnyOrder("s0", fields, null);

            env.undeployAll();
        }
    }
}
