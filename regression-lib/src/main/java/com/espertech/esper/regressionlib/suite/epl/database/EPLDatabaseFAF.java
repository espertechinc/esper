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

import com.espertech.esper.common.client.EPCompiled;
import com.espertech.esper.common.client.EPException;
import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.fireandforget.EPFireAndForgetPreparedQuery;
import com.espertech.esper.common.client.fireandforget.EPFireAndForgetPreparedQueryParameterized;
import com.espertech.esper.common.client.fireandforget.EPFireAndForgetQueryResult;
import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.common.client.soda.EPStatementObjectModel;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.RegressionExecutionFAFOnly;
import com.espertech.esper.regressionlib.framework.RegressionPath;
import com.espertech.esper.regressionlib.support.util.SupportSQLColumnTypeConversion;
import com.espertech.esper.regressionlib.support.util.SupportSQLOutputRowConversion;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class EPLDatabaseFAF {

    public static List<RegressionExecution> executions() {
        List<RegressionExecution> execs = new ArrayList<>();
        execs.add(new EPLDatabaseFAFSimple());
        execs.add(new EPLDatabaseFAFHook());
        execs.add(new EPLDatabaseFAFPrepareExecutePerformance());
        execs.add(new EPLDatabaseFAFSubstitutionParam());
        execs.add(new EPLDatabaseFAFDistinct());
        execs.add(new EPLDatabaseFAFWhereClause());
        execs.add(new EPLDatabaseFAFVariable());
        execs.add(new EPLDatabaseFAFSODA());
        execs.add(new EPLDatabaseFAFSQLTextParamSubquery());
        execs.add(new EPLDatabaseFAFInvalid());
        return execs;
    }

    private static class EPLDatabaseFAFSimple extends RegressionExecutionFAFOnly {
        public void run(RegressionEnvironment env) {
            EPFireAndForgetQueryResult result = env.compileExecuteFAF("select * from sql:MyDBPlain[\"select myint from mytesttable where myint between 5 and 15\"]");
            assertSingleRowResult(result, "myint", 10);
        }
    }

    private static class EPLDatabaseFAFHook extends RegressionExecutionFAFOnly {
        public void run(RegressionEnvironment env) {
            String queryColummTypeConversion = "@name('s0') @Hook(type=HookType.SQLCOL, hook='" + SupportSQLColumnTypeConversion.class.getName() + "')" +
                    "select * from sql:MyDBPooled ['select myint as myintTurnedBoolean from mytesttable where myint = 50']";
            EPFireAndForgetQueryResult resultColType = env.compileExecuteFAF(queryColummTypeConversion);
            assertSingleRowResult(resultColType, "myintTurnedBoolean", true);

            String queryRowConversion = "@name('s0') @Hook(type=HookType.SQLROW, hook='" + SupportSQLOutputRowConversion.class.getName() + "')" +
                    "select * from sql:MyDBPooled ['select * from mytesttable where myint = 10']";
            env.compileDeploy(queryRowConversion);
            EPFireAndForgetQueryResult resultRowConv = env.compileExecuteFAF(queryRowConversion);
            EPAssertionUtil.assertPropsPerRow(resultRowConv.getArray(), new String[]{"theString", "intPrimitive"}, new Object[][]{{">10<", 99010}});
        }
    }

    private static class EPLDatabaseFAFPrepareExecutePerformance extends RegressionExecutionFAFOnly {
        public void run(RegressionEnvironment env) {
            EPCompiled compiled = env.compileFAF("select * from sql:MyDBPooled ['select * from mytesttable where myint = 10']", new RegressionPath());

            long start = System.currentTimeMillis();
            EPFireAndForgetPreparedQuery prepared = env.runtime().getFireAndForgetService().prepareQuery(compiled);
            try {
                for (int i = 0; i < 1000; i++) {
                    EPFireAndForgetQueryResult result = prepared.execute();
                    assertEquals(1, result.getArray().length);
                }
            } finally {
                prepared.close();
            }
            long delta = System.currentTimeMillis() - start;
            assertTrue("delta=" + delta, delta < 2000);
        }
    }

    private static class EPLDatabaseFAFSubstitutionParam extends RegressionExecutionFAFOnly {
        public void run(RegressionEnvironment env) {

            String epl = "select myvarchar as c0, ?:selectValue:int as c1 from sql:MyDBPooled ['select myvarchar from mytesttable where myint = ${?:filterValue:int}']";
            EPCompiled compiled = env.compileFAF(epl, new RegressionPath());
            EPFireAndForgetPreparedQueryParameterized parameterized = env.runtime().getFireAndForgetService().prepareQueryWithParameters(compiled);

            assertQuery(env, parameterized, 1, 10, "A");
            assertQuery(env, parameterized, 2, 60, "F");

            parameterized.close();
        }

        private void assertQuery(RegressionEnvironment env, EPFireAndForgetPreparedQueryParameterized parameterized, int selectValue, int filterValue, String expected) {
            parameterized.setObject("selectValue", selectValue);
            parameterized.setObject("filterValue", filterValue);
            EventBean row = env.runtime().getFireAndForgetService().executeQuery(parameterized).getArray()[0];
            EPAssertionUtil.assertProps(row, "c0,c1".split(","), new Object[] {expected, selectValue});
        }
    }

    private static class EPLDatabaseFAFDistinct extends RegressionExecutionFAFOnly {
        public void run(RegressionEnvironment env) {
            String sql = "select myint from mytesttable where myint = 10 union all select myint from mytesttable where myint = 10";
            String epl = "select distinct myint from sql:MyDBPooled ['" + sql + "']";

            EventBean[] out = env.compileExecuteFAF(epl).getArray();
            EPAssertionUtil.assertPropsPerRow(out, new String[] {"myint"}, new Object[][] {{10}});
        }
    }

    private static class EPLDatabaseFAFWhereClause extends RegressionExecutionFAFOnly {
        public void run(RegressionEnvironment env) {
            String epl = "select * from sql:MyDBPooled ['select myint, myvarchar from mytesttable'] where myvarchar in ('A', 'E')";
            EventBean[] out = env.compileExecuteFAF(epl).getArray();
            EPAssertionUtil.assertPropsPerRow(out, "myint,myvarchar".split(","), new Object[][] {{10, "A"}, {50, "E"}});
        }
    }

    private static class EPLDatabaseFAFVariable extends RegressionExecutionFAFOnly {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            env.compileDeploy("@public create variable int myvar = 20;\n" +
                    "on SupportBean set myvar = intPrimitive;\n", path);

            String epl = "@name('s0') select * from sql:MyDBPooled ['select * from mytesttable where myint = ${myvar}']";
            EPCompiled compiled = env.compileFAF(epl, path);

            EPFireAndForgetPreparedQuery prepared = env.runtime().getFireAndForgetService().prepareQuery(compiled);
            assertSingleRow(prepared.execute(), "B");

            env.sendEventBean(new SupportBean(null, 50));
            assertSingleRow(prepared.execute(), "E");

            env.sendEventBean(new SupportBean(null, 30));
            assertSingleRow(prepared.execute(), "C");

            prepared.close();
        }
    }

    private static class EPLDatabaseFAFSODA extends RegressionExecutionFAFOnly {
        public void run(RegressionEnvironment env) {
            String epl = "select col as c0 from sql:MyDBPooled[\"select myvarchar as col from mytesttable where myint between 20 and 30\"]";
            EPStatementObjectModel model = env.eplToModel(epl);
            assertEquals(epl, model.toEPL());
            EventBean[] rows = env.compileExecuteFAF(model, new RegressionPath()).getArray();
            EPAssertionUtil.assertPropsPerRow(rows, new String[]{"c0"}, new Object[][]{{"B"}, {"C"}});
        }
    }

    private static class EPLDatabaseFAFSQLTextParamSubquery implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            env.compileDeploy("@public create window MyWindow#lastevent as SupportBean;\n" +
                    "on SupportBean merge MyWindow insert select *", path);

            String epl = "select * from sql:MyDBPlain['select * from mytesttable where myint = ${(select intPrimitive from MyWindow)}']";
            EPCompiled compiled = env.compileFAF(epl, path);

            EPFireAndForgetPreparedQuery prepared = env.runtime().getFireAndForgetService().prepareQuery(compiled);

            sendAssert(env, prepared, 30, "C");
            sendAssert(env, prepared, 10, "A");

            env.undeployAll();
            prepared.close();
        }

        private void sendAssert(RegressionEnvironment env, EPFireAndForgetPreparedQuery prepared, int intPrimitive, String expected) {
            env.sendEventBean(new SupportBean("", intPrimitive));
            EventBean[] result = prepared.execute().getArray();
            EPAssertionUtil.assertPropsPerRow(result, new String[] {"myvarchar"}, new Object[][] {{expected}});
        }
    }


    private static class EPLDatabaseFAFInvalid extends RegressionExecutionFAFOnly {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            env.compileDeploy("@public create context MyContext partition by theString from SupportBean", path);

            // invalid join
            String eplJoin = "select * from sql:MyDBPooled['select * from mytesttable'],sql:MyDBPooled['select * from mytesttable']";
            env.tryInvalidCompileFAF(path, eplJoin, "Join between SQL query results in fire-and-forget is not supported");

            // invalid join
            String eplContext = "context MyContext select * from sql:MyDBPooled['select * from mytesttable']";
            env.tryInvalidCompileFAF(path, eplContext, "Context specification for SQL queries in fire-and-forget is not supported");

            // invalid SQL
            String eplInvalidSQL = "select * from sql:MyDBPooled['select *']";
            env.tryInvalidCompileFAF(path, eplInvalidSQL, "Error in statement 'select *', failed to obtain result metadata, consider turning off metadata interrogation via configuration, please check the statement, reason: No tables used");

            // closed called before execute
            String eplSimple = "select * from sql:MyDBPooled['select * from mytesttable']";
            EPCompiled compiled = env.compileFAF(eplSimple, path);
            EPFireAndForgetPreparedQuery prepared = env.runtime().getFireAndForgetService().prepareQuery(compiled);
            prepared.close();
            try {
                prepared.execute();
                fail();
            } catch (EPException ex) {
                assertEquals("Prepared fire-and-forget query is already closed", ex.getMessage());
            }
        }
    }

    private static void assertSingleRow(EPFireAndForgetQueryResult result, String expected) {
        assertSingleRowResult(result, "myvarchar", expected);
    }

    private static void assertSingleRowResult(EPFireAndForgetQueryResult result, String columnName, Object expected) {
        EPAssertionUtil.assertPropsPerRow(result.getArray(), new String[]{columnName}, new Object[][]{{expected}});
        assertEquals(expected.getClass(), result.getArray()[0].getEventType().getPropertyType(columnName));
    }
}

