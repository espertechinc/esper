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
package com.espertech.esper.regressionlib.suite.infra.nwtable;

import com.espertech.esper.common.client.EPCompiled;
import com.espertech.esper.common.client.fireandforget.EPFireAndForgetPreparedQueryParameterized;
import com.espertech.esper.common.client.fireandforget.EPFireAndForgetQueryResult;
import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.common.internal.support.SupportBean_S0;
import com.espertech.esper.common.internal.support.SupportBean_S1;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.RegressionPath;
import com.espertech.esper.regressionlib.support.util.IndexBackingTableInfo;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import static com.espertech.esper.regressionlib.framework.SupportMessageAssertUtil.tryInvalidFAFCompile;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class InfraNWTableFAFSubquery implements IndexBackingTableInfo {

    public static Collection<RegressionExecution> executions() {
        ArrayList<RegressionExecution> execs = new ArrayList<>();
        execs.add(new InfraFAFSubquerySimple(true));
        execs.add(new InfraFAFSubquerySimple(false));
        execs.add(new InfraFAFSubquerySimpleJoin());
        execs.add(new InfraFAFSubqueryInsert(true));
        execs.add(new InfraFAFSubqueryInsert(false));
        execs.add(new InfraFAFSubqueryUpdateUncorrelated());
        execs.add(new InfraFAFSubqueryDeleteUncorrelated());
        execs.add(new InfraFAFSubquerySelectCorrelated());
        execs.add(new InfraFAFSubqueryUpdateCorrelatedSet());
        execs.add(new InfraFAFSubqueryUpdateCorrelatedWhere());
        execs.add(new InfraFAFSubqueryDeleteCorrelatedWhere());
        execs.add(new InfraFAFSubqueryContextBothWindows());
        execs.add(new InfraFAFSubqueryContextSelect());
        execs.add(new InfraFAFSubquerySelectWhere());
        execs.add(new InfraFAFSubquerySelectGroupBy());
        execs.add(new InfraFAFSubquerySelectIndexPerfWSubstitution(true));
        execs.add(new InfraFAFSubquerySelectIndexPerfWSubstitution(false));
        execs.add(new InfraFAFSubquerySelectIndexPerfCorrelated(true));
        execs.add(new InfraFAFSubquerySelectIndexPerfCorrelated(false));
        execs.add(new InfraFAFSubqueryInvalid());
        return execs;
    }

    public static class InfraFAFSubqueryInvalid implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            String epl = "@public create window WinSB#keepall as SupportBean;\n" +
                "create context MyContext partition by id from SupportBean_S0;\n" +
                "context MyContext create window PartitionedWinS0#keepall as SupportBean_S0;\n";
            env.compile(epl, path);

            tryInvalidFAFCompile(env, path, "select (select * from SupportBean#lastevent) from WinSB",
                "Fire-and-forget queries only allow subqueries against named windows and tables");

            tryInvalidFAFCompile(env, path, "select (select * from WinSB(theString='x')) from WinSB",
                "Failed to plan subquery number 1 querying WinSB: Subqueries in fire-and-forget queries do not allow filter expressions");

            tryInvalidFAFCompile(env, path, "select (select * from PartitionedWinS0) from WinSB",
                "Failed to plan subquery number 1 querying PartitionedWinS0: Mismatch in context specification, the context for the named window 'PartitionedWinS0' is 'MyContext' and the query specifies no context");
        }
    }

    private static class InfraFAFSubquerySelectIndexPerfCorrelated implements RegressionExecution {
        private boolean namedWindow;

        public InfraFAFSubquerySelectIndexPerfCorrelated(boolean namedWindow) {
            this.namedWindow = namedWindow;
        }

        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            String epl =
                "@public create window WinSB#keepall as SupportBean;\n" +
                    "insert into WinSB select * from SupportBean;\n";
            if (namedWindow) {
                epl += "@public create window Infra#unique(id) as (id int, value string);\n";
            } else {
                epl += "@public create table Infra(id int primary key, value string);\n";
            }
            epl += "@public create index InfraIndex on Infra(value);\n" +
                "insert into Infra select id, p00 as value from SupportBean_S0;\n";
            env.compileDeploy(epl, path);

            final int numRows = 10000;  // less than 1M
            for (int i = 0; i < numRows; i++) {
                sendSB(env, "v" + i, 0);
                sendS0(env, -1 * i, "v" + i);
            }

            long start = System.currentTimeMillis();
            String query = "select (select id from Infra as i where i.value = wsb.theString) as c0 from WinSB as wsb";
            EPFireAndForgetQueryResult result = compileExecute(env, path, query);
            long delta = System.currentTimeMillis() - start;
            assertTrue("delta is " + delta, delta < 1000);
            assertEquals(numRows, result.getArray().length);
            for (int i = 0; i < numRows; i++) {
                assertEquals(-1 * i, result.getArray()[i].get("c0"));
            }

            env.undeployAll();
        }
    }

    private static class InfraFAFSubquerySelectIndexPerfWSubstitution implements RegressionExecution {
        private boolean namedWindow;

        public InfraFAFSubquerySelectIndexPerfWSubstitution(boolean namedWindow) {
            this.namedWindow = namedWindow;
        }

        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            String epl =
                "@public create window WinSB#lastevent as SupportBean;\n" +
                    "insert into WinSB select * from SupportBean;\n";
            if (namedWindow) {
                epl += "@public create window Infra#unique(id) as (id int, value string);\n";
            } else {
                epl += "@public create table Infra(id int primary key, value string);\n";
            }
            epl += "@public create index InfraIndex on Infra(value);\n" +
                "insert into Infra select id, p00 as value from SupportBean_S0;\n";
            env.compileDeploy(epl, path);

            sendSB(env, "E1", -1);
            for (int i = 0; i < 10000; i++) {
                sendS0(env, i, "v" + i);
            }

            String query = "select (select id from Infra as i where i.value = ?:p0:string) as c0 from WinSB";
            EPCompiled compiled = env.compileFAF(query, path);
            EPFireAndForgetPreparedQueryParameterized prepared = env.runtime().getFireAndForgetService().prepareQueryWithParameters(compiled);

            long start = System.currentTimeMillis();
            for (int i = 5000; i < 6000; i++) {
                prepared.setObject("p0", "v" + i);
                EPFireAndForgetQueryResult result = env.runtime().getFireAndForgetService().executeQuery(prepared);
                assertEquals(1, result.getArray().length);
                assertEquals(i, result.getArray()[0].get("c0"));
            }
            long delta = System.currentTimeMillis() - start;
            assertTrue("delta is " + delta, delta < 1000);

            env.undeployAll();
        }
    }

    private static class InfraFAFSubquerySelectWhere implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            String epl =
                "@public create window WinS0#keepall as SupportBean_S0;\n" +
                    "@public create window WinSB#keepall as SupportBean;\n" +
                    "insert into WinS0 select * from SupportBean_S0;\n" +
                    "insert into WinSB select * from SupportBean;\n";
            env.compileDeploy(epl, path);

            String query = "select (select intPrimitive from WinSB where theString = 'x') as c0 from WinS0";
            sendS0(env, 0, null);
            assertQuerySingle(env, path, query, null);

            sendSB(env, "E1", 1);
            assertQuerySingle(env, path, query, null);

            sendSB(env, "x", 2);
            assertQuerySingle(env, path, query, 2);

            sendSB(env, "x", 3);
            assertQuerySingle(env, path, query, null);

            env.undeployAll();
        }
    }

    private static class InfraFAFSubquerySelectGroupBy implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            String epl =
                "@public create window WinS0#keepall as SupportBean_S0;\n" +
                    "@public create window WinSB#keepall as SupportBean;\n" +
                    "insert into WinS0 select * from SupportBean_S0;\n" +
                    "insert into WinSB select * from SupportBean;\n";
            env.compileDeploy(epl, path);

            String query = "select (select theString, sum(intPrimitive) as thesum from WinSB group by theString) as c0 from WinS0";
            sendS0(env, 0, null);

            sendSB(env, "E1", 10);
            sendSB(env, "E1", 11);
            Map<String, Object> result = (Map<String, Object>) runQuerySingle(env, path, query);
            assertEquals("E1", result.get("theString"));
            assertEquals(21, result.get("thesum"));

            env.undeployAll();
        }
    }

    private static class InfraFAFSubqueryContextSelect implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            String epl =
                "create context MyContext partition by id from SupportBean_S0;\n" +
                    "@public context MyContext create window WinS0#keepall as SupportBean_S0;\n" +
                    "context MyContext on SupportBean_S0 as s0 merge WinS0 insert select *;\n" +
                    "@public create window WinSB#lastevent as SupportBean;\n" +
                    "insert into WinSB select * from SupportBean;\n";
            env.compileDeploy(epl, path);

            sendS0(env, 1, "a");
            sendS0(env, 2, "b");
            sendSB(env, "E1", 1);

            String query = "context MyContext select p00, (select theString from WinSB) as theString from WinS0";
            assertQueryMultirowAnyOrder(env, path, query, "p00,theString", new Object[][]{{"a", "E1"}, {"b", "E1"}});

            env.undeployAll();
        }
    }

    private static class InfraFAFSubqueryContextBothWindows implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            String epl =
                "create context MyContext partition by id from SupportBean_S0, id from SupportBean_S1;\n" +
                    "@public context MyContext create window WinS0#keepall as SupportBean_S0;\n" +
                    "@public context MyContext create window WinS1#keepall as SupportBean_S1;\n" +
                    "context MyContext on SupportBean_S0 as s0 merge WinS0 insert select *;\n" +
                    "context MyContext on SupportBean_S1 as s1 merge WinS1 insert select *;\n";
            env.compileDeploy(epl, path);

            sendS0(env, 1, "a");
            sendS0(env, 2, "b");
            sendS0(env, 3, "c");
            sendS1(env, 1, "X");
            sendS1(env, 2, "Y");
            sendS1(env, 3, "Z");

            String query = "context MyContext select p00, (select p10 from WinS1) as p10 from WinS0";
            assertQueryMultirowAnyOrder(env, path, query, "p00,p10", new Object[][]{{"a", "X"}, {"b", "Y"}, {"c", "Z"}});

            env.undeployAll();
        }
    }

    private static class InfraFAFSubqueryDeleteCorrelatedWhere implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            String epl =
                "@public create window WinS0#keepall as SupportBean_S0;\n" +
                    "@public create window WinSB#unique(intPrimitive) as SupportBean;\n" +
                    "insert into WinS0 select * from SupportBean_S0;\n" +
                    "insert into WinSB select * from SupportBean;\n";
            env.compileDeploy(epl, path);

            sendS0(env, 1, "a");
            sendS0(env, 2, "b");
            sendS0(env, 3, "c");

            sendSB(env, "a", 0);
            sendSB(env, "b", 2);

            String update = "delete from WinS0 as wins0 where id = (select intPrimitive from WinSB winsb where winsb.theString = wins0.p00)";
            compileExecute(env, path, update);

            String query = "select * from WinS0";
            assertQueryMultirowAnyOrder(env, path, query, "id,p00", new Object[][]{{1, "a"}, {3, "c"}});

            env.undeployAll();
        }
    }

    private static class InfraFAFSubqueryUpdateCorrelatedWhere implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            String epl =
                "@public create window WinS0#keepall as SupportBean_S0;\n" +
                    "@public create window WinSB#unique(intPrimitive) as SupportBean;\n" +
                    "insert into WinS0 select * from SupportBean_S0;\n" +
                    "insert into WinSB select * from SupportBean;\n";
            env.compileDeploy(epl, path);

            sendS0(env, 1, "a");
            sendS0(env, 2, "b");
            sendS0(env, 3, "c");

            sendSB(env, "a", 0);
            sendSB(env, "b", 2);

            String update = "update WinS0 as wins0 set p00 = 'x' where id = (select intPrimitive from WinSB winsb where winsb.theString = wins0.p00)";
            compileExecute(env, path, update);

            String query = "select * from WinS0";
            assertQueryMultirowAnyOrder(env, path, query, "id,p00", new Object[][]{{1, "a"}, {2, "x"}, {3, "c"}});

            env.undeployAll();
        }
    }

    private static class InfraFAFSubqueryUpdateCorrelatedSet implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            String epl =
                "@public create window WinS0#keepall as SupportBean_S0;\n" +
                    "@public create window WinSB#unique(intPrimitive) as SupportBean;\n" +
                    "insert into WinS0 select * from SupportBean_S0;\n" +
                    "insert into WinSB select * from SupportBean;\n";
            env.compileDeploy(epl, path);

            sendS0(env, 1, "a");
            sendS0(env, 2, "b");
            sendS0(env, 3, "c");

            sendSB(env, "X", 2);
            sendSB(env, "Y", 1);
            sendSB(env, "Z", 3);

            String update = "update WinS0 as wins0 set p00 = (select theString from WinSB winsb where winsb.intPrimitive = wins0.id)";
            compileExecute(env, path, update);

            String query = "select * from WinS0";
            assertQueryMultirowAnyOrder(env, path, query, "id,p00", new Object[][]{{1, "Y"}, {2, "X"}, {3, "Z"}});

            env.undeployAll();
        }
    }

    private static class InfraFAFSubquerySelectCorrelated implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            String epl =
                "@public create window WinS0#keepall as SupportBean_S0;\n" +
                    "@public create window WinSB#unique(intPrimitive) as SupportBean;\n" +
                    "insert into WinS0 select * from SupportBean_S0;\n" +
                    "insert into WinSB select * from SupportBean;\n";
            env.compileDeploy(epl, path);

            sendS0(env, 1, "a");
            sendS0(env, 2, "b");
            sendS0(env, 3, "c");

            sendSB(env, "X", 2);
            sendSB(env, "Y", 1);
            sendSB(env, "Z", 3);

            String query = "select id, (select theString from WinSB winsb where winsb.intPrimitive = wins0.id) as theString from WinS0 as wins0";
            assertQueryMultirowAnyOrder(env, path, query, "id,theString", new Object[][]{{1, "Y"}, {2, "X"}, {3, "Z"}});

            sendSB(env, "Q", 1);
            sendSB(env, "R", 3);
            sendSB(env, "S", 2);
            assertQueryMultirowAnyOrder(env, path, query, "id,theString", new Object[][]{{1, "Q"}, {2, "S"}, {3, "R"}});

            env.undeployAll();
        }
    }

    private static class InfraFAFSubqueryDeleteUncorrelated implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            String epl =
                "@public create window Win#keepall as (key string, value int);\n" +
                    "@public create window WinSB#lastevent as SupportBean;\n" +
                    "insert into WinSB select * from SupportBean;\n";
            env.compileDeploy(epl, path);
            compileExecute(env, path, "insert into Win select 'k1' as key, 1 as value");
            compileExecute(env, path, "insert into Win select 'k2' as key, 2 as value");
            compileExecute(env, path, "insert into Win select 'k3' as key, 3 as value");

            String delete = "delete from Win where value = (select intPrimitive from WinSB)";
            String query = "select * from Win";

            assertQueryMultirowAnyOrder(env, path, query, "key,value", new Object[][]{{"k1", 1}, {"k2", 2}, {"k3", 3}});

            compileExecute(env, path, delete);
            assertQueryMultirowAnyOrder(env, path, query, "key,value", new Object[][]{{"k1", 1}, {"k2", 2}, {"k3", 3}});

            sendSB(env, "E1", 2);
            compileExecute(env, path, delete);
            assertQueryMultirowAnyOrder(env, path, query, "key,value", new Object[][]{{"k1", 1}, {"k3", 3}});

            sendSB(env, "E1", 1);
            compileExecute(env, path, delete);
            assertQueryMultirowAnyOrder(env, path, query, "key,value", new Object[][]{{"k3", 3}});

            env.undeployAll();
        }
    }

    private static class InfraFAFSubqueryUpdateUncorrelated implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            String epl =
                "@public create window Win#lastevent as (value int);\n" +
                    "@public create window WinSB#lastevent as SupportBean;\n" +
                    "insert into WinSB select * from SupportBean;\n";
            env.compileDeploy(epl, path);
            compileExecute(env, path, "insert into Win select 1 as value");

            String update = "update Win set value = (select intPrimitive from WinSB)";
            String query = "select value as c0 from Win";

            assertQuerySingle(env, path, query, 1);

            compileExecute(env, path, update);
            assertQuerySingle(env, path, query, null);

            sendSB(env, "E1", 10);
            compileExecute(env, path, update);
            assertQuerySingle(env, path, query, 10);

            sendSB(env, "E2", 20);
            compileExecute(env, path, update);
            assertQuerySingle(env, path, query, 20);

            env.undeployAll();
        }
    }

    private static class InfraFAFSubqueryInsert implements RegressionExecution {
        private boolean namedWindow;

        public InfraFAFSubqueryInsert(boolean namedWindow) {
            this.namedWindow = namedWindow;
        }

        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            String epl = "@public create window Win#keepall as (value string);\n";
            if (namedWindow) {
                epl +=
                    "@public create window InfraSB#lastevent as SupportBean;\n" +
                        "insert into InfraSB select * from SupportBean;\n";
            } else {
                epl +=
                    "@public create table InfraSB(theString string);\n" +
                        "on SupportBean as sb merge InfraSB as issb" +
                        "  when not matched then insert select theString when matched then update set issb.theString=sb.theString;\n";

            }
            env.compileDeploy(epl, path);

            String insert = "insert into Win(value) select (select theString from InfraSB)";
            String query = "select * from Win";

            compileExecute(env, path, insert);
            assertQueryMultirowAnyOrder(env, path, query, "value", new Object[][]{{null}});

            sendSB(env, "E1", 0);
            compileExecute(env, path, insert);
            assertQueryMultirowAnyOrder(env, path, query, "value", new Object[][]{{null}, {"E1"}});

            sendSB(env, "E2", 0);
            compileExecute(env, path, insert);
            assertQueryMultirowAnyOrder(env, path, query, "value", new Object[][]{{null}, {"E1"}, {"E2"}});

            env.undeployAll();
        }
    }

    private static class InfraFAFSubquerySimpleJoin implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            String epl =
                "@public create window WinSB#lastevent as SupportBean;\n" +
                    "@public create window WinS0#keepall as SupportBean_S0;\n" +
                    "@public create window WinS1#keepall as SupportBean_S1;\n" +
                    "insert into WinSB select * from SupportBean;\n" +
                    "insert into WinS0 select * from SupportBean_S0;\n" +
                    "insert into WinS1 select * from SupportBean_S1;\n";
            String query = "select (select theString from WinSB) as c0, p00, p10 from WinS0, WinS1";
            env.compileDeploy(epl, path);

            assertQueryNoRows(env, path, query, String.class);

            sendS0(env, 1, "S0_0");
            sendS1(env, 2, "S1_0");
            assertQuerySingle(env, path, query, null);

            sendSB(env, "SB_0", 0);
            assertQuerySingle(env, path, query, "SB_0");

            sendS0(env, 3, "S0_1");
            assertQueryMultirowAnyOrder(env, path, query, "c0,p00,p10", new Object[][]{{"SB_0", "S0_0", "S1_0"}, {"SB_0", "S0_1", "S1_0"}});

            env.undeployAll();

        }
    }

    private static class InfraFAFSubquerySimple implements RegressionExecution {
        boolean namedWindow;

        public InfraFAFSubquerySimple(boolean namedWindow) {
            this.namedWindow = namedWindow;
        }

        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            String epl =
                "@public create window WinSB#lastevent as SupportBean;\n" +
                    "insert into WinSB select * from SupportBean;\n";
            if (namedWindow) {
                epl +=
                    "@public create window InfraS0#lastevent as SupportBean_S0;\n" +
                        "insert into InfraS0 select * from SupportBean_S0;\n";
            } else {
                epl +=
                    "@public create table InfraS0(id int primary key, p00 string);\n" +
                        "on SupportBean_S0 as s0 merge InfraS0 as is0 where s0.id = is0.id" +
                        "  when not matched then insert select id, p00 when matched then update set is0.p00=s0.p00;\n";
            }
            String query = "select (select p00 from InfraS0) as c0 from WinSB";
            env.compileDeploy(epl, path);

            assertQueryNoRows(env, path, query, String.class);

            sendSB(env, "E1", 1);
            assertQuerySingle(env, path, query, null);

            sendS0(env, 1, "a");
            assertQuerySingle(env, path, query, "a");

            sendS0(env, 1, "b");
            assertQuerySingle(env, path, query, "b");

            env.undeployAll();
        }
    }

    private static void assertQueryNoRows(RegressionEnvironment env, RegressionPath path, String query, Class resultType) {
        EPCompiled compiled = env.compileFAF(query, path);
        EPFireAndForgetQueryResult result = env.runtime().getFireAndForgetService().executeQuery(compiled);
        assertEquals(0, result.getArray() == null ? 0 : result.getArray().length);
        assertEquals(result.getEventType().getPropertyType("c0"), resultType);
    }

    private static void assertQuerySingle(RegressionEnvironment env, RegressionPath path, String query, Object c0Expected) {
        Object result = runQuerySingle(env, path, query);
        assertEquals(c0Expected, result);
    }

    private static Object runQuerySingle(RegressionEnvironment env, RegressionPath path, String query) {
        EPFireAndForgetQueryResult result = compileExecute(env, path, query);
        assertEquals(1, result.getArray().length);
        return result.getArray()[0].get("c0");
    }

    private static void assertQueryMultirowAnyOrder(RegressionEnvironment env, RegressionPath path, String query, String fieldCSV, Object[][] expected) {
        EPFireAndForgetQueryResult result = compileExecute(env, path, query);
        EPAssertionUtil.assertPropsPerRowAnyOrder(result.getArray(), fieldCSV.split(","), expected);
    }

    private static EPFireAndForgetQueryResult compileExecute(RegressionEnvironment env, RegressionPath path, String query) {
        EPCompiled compiled = env.compileFAF(query, path);
        return env.runtime().getFireAndForgetService().executeQuery(compiled);
    }

    private static void sendS0(RegressionEnvironment env, int id, String p00) {
        env.sendEventBean(new SupportBean_S0(id, p00));
    }

    private static void sendS1(RegressionEnvironment env, int id, String p10) {
        env.sendEventBean(new SupportBean_S1(id, p10));
    }

    private static void sendSB(RegressionEnvironment env, String theString, int intPrimitive) {
        env.sendEventBean(new SupportBean(theString, intPrimitive));
    }
}
