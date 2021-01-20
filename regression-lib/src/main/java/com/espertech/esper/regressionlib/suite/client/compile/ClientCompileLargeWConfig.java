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
package com.espertech.esper.regressionlib.suite.client.compile;

import com.espertech.esper.common.client.EPCompiled;
import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.client.fireandforget.EPFireAndForgetPreparedQueryParameterized;
import com.espertech.esper.common.client.fireandforget.EPFireAndForgetQueryResult;
import com.espertech.esper.common.internal.support.EventRepresentationChoice;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.common.internal.support.SupportBean_S0;
import com.espertech.esper.common.internal.support.SupportBean_S1;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.RegressionFlag;
import com.espertech.esper.regressionlib.framework.RegressionPath;
import com.espertech.esper.runtime.client.DeploymentOptions;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.function.BiConsumer;

import static org.junit.Assert.*;

public class ClientCompileLargeWConfig {

    public static List<RegressionExecution> executions() {
        List<RegressionExecution> execs = new ArrayList<>();
        execs.add(new ClientCompileLargeTableAggregationReset(1000));
        execs.add(new ClientCompileLargeTableAggregationEnterLeaveMethod(1000));
        execs.add(new ClientCompileLargeTableAggregationEnterLeaveAccessAgg(1000));
        execs.add(new ClientCompileLargeAggregation(5000));
        execs.add(new ClientCompileLargeAggregationAccess(1000));
        execs.add(new ClientCompileLargeSubstitutionParams(5000));
        execs.add(new ClientCompileLargeSubstitutionParamsFAF(5000));
        execs.add(new ClientCompileLargeSelectCol(EventRepresentationChoice.MAP, 5000));
        execs.add(new ClientCompileLargeSelectCol(EventRepresentationChoice.OBJECTARRAY, 5000));
        execs.add(new ClientCompileLargeCreateSchemaAndInsert(EventRepresentationChoice.MAP, 5000, false));
        execs.add(new ClientCompileLargeCreateSchemaAndInsert(EventRepresentationChoice.MAP, 5000, true));
        execs.add(new ClientCompileLargeCreateSchemaAndInsert(EventRepresentationChoice.OBJECTARRAY, 5000, false));
        return execs;
    }

    public static class ClientCompileLargeCreateSchemaAndInsert implements RegressionExecution {
        private final EventRepresentationChoice representation;
        private final int numColumns;
        private final boolean widening;

        public ClientCompileLargeCreateSchemaAndInsert(EventRepresentationChoice representation, int numColumns, boolean widening) {
            this.representation = representation;
            this.numColumns = numColumns;
            this.widening = widening;
        }

        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            env.advanceTime(1000000);
            StringWriter eplSchema = new StringWriter();
            eplSchema.append("@name('schema') @public @buseventtype create ").append(representation.getName()).append(" schema MyEvent (");
            String delimiter = "";
            for (int i = 0; i < numColumns; i++) {
                // create-schema goes back from pN-1 to p0
                eplSchema.append(delimiter).append("p").append(Integer.toString(numColumns - i - 1)).append(" long");
                delimiter = ",";
            }
            eplSchema.append(");\n");
            env.compileDeploy(eplSchema.toString(), path);

            env.assertStatement("schema", statement -> {
                EventType eventType = statement.getEventType();
                for (int i = 0; i < numColumns; i++) {
                    assertEquals(Long.class, eventType.getPropertyType("p" + i));
                }
            });

            StringWriter eplInsert = new StringWriter();
            eplInsert.append("insert into MyEvent select ");
            delimiter = "";
            String adder = widening ? "1000000" : "current_timestamp()";
            for (int i = 0; i < numColumns; i++) {
                eplInsert.append(delimiter).append(Integer.toString(i)).append("+").append(adder).append(" as p").append(Integer.toString(i));
                delimiter = ",";
            }
            eplInsert.append(" from SupportBean;\n");
            env.compileDeploy(eplInsert.toString(), path);

            env.compileDeploy("@name('s0') select * from MyEvent", path).addListener("s0");
            env.sendEventBean(new SupportBean());
            env.assertEventNew("s0", event -> {
                for (int i = 0; i < numColumns; i++) {
                    assertEquals(i + 1000000L, event.get("p" + i));
                }
            });

            env.undeployAll();
        }

        public String name() {
            return this.getClass().getSimpleName() + "{" +
                "representation=" + representation +
                ", numColumns=" + numColumns +
                ", widening=" + widening +
                '}';
        }

        public EnumSet<RegressionFlag> flags() {
            return EnumSet.of(RegressionFlag.COMPILEROPS);
        }
    }

    public static class ClientCompileLargeSubstitutionParams implements RegressionExecution {
        private final int numColumns;

        public ClientCompileLargeSubstitutionParams(int numColumns) {
            this.numColumns = numColumns;
        }

        public void run(RegressionEnvironment env) {
            StringWriter epl = new StringWriter();
            epl.append("@name('s0') select ");
            String delimiter = "";
            for (int i = 0; i < numColumns; i++) {
                epl.append(delimiter).append("?:p").append(Integer.toString(i)).append(":string as c").append(Integer.toString(i));
                delimiter = ",";
            }
            epl.append(" from SupportBean;\n");
            EPCompiled compiled = env.compile(epl.toString());

            DeploymentOptions options = new DeploymentOptions();
            options.setStatementSubstitutionParameter(ctx -> {
                for (int i = 0; i < numColumns; i++) {
                    ctx.setObject("p" + i, "v" + i);
                }
            });
            env.deploy(compiled, options).addListener("s0");

            env.sendEventBean(new SupportBean());
            env.assertEventNew("s0", event -> {
                for (int i = 0; i < numColumns; i++) {
                    assertEquals("v" + i, event.get("c" + i));
                }
            });

            env.undeployAll();
        }

        public EnumSet<RegressionFlag> flags() {
            return EnumSet.of(RegressionFlag.COMPILEROPS);
        }
    }

    public static class ClientCompileLargeSubstitutionParamsFAF implements RegressionExecution {
        private final int numColumns;

        public ClientCompileLargeSubstitutionParamsFAF(int numColumns) {
            this.numColumns = numColumns;
        }

        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            String eplNamedWindow = "@public create window MyWindow#lastevent (p0 string);\n";
            env.compileDeploy(eplNamedWindow, path);
            env.compileExecuteFAF("insert into MyWindow select 'x' as p0", path);

            StringWriter eplFAF = new StringWriter();
            eplFAF.append("select ");
            String delimiter = "";
            for (int i = 0; i < numColumns; i++) {
                eplFAF.append(delimiter).append("p0 || ?:p").append(Integer.toString(i)).append(":string as c").append(Integer.toString(i));
                delimiter = ",";
            }
            eplFAF.append(" from MyWindow");

            EPCompiled compiled = env.compileFAF(eplFAF.toString(), path);
            EPFireAndForgetPreparedQueryParameterized prepared = env.runtime().getFireAndForgetService().prepareQueryWithParameters(compiled);
            for (int i = 0; i < numColumns; i++) {
                prepared.setObject("p" + i, "v" + i);
            }
            EPFireAndForgetQueryResult result = env.runtime().getFireAndForgetService().executeQuery(prepared);
            EventBean event = result.getArray()[0];

            for (int i = 0; i < numColumns; i++) {
                assertEquals("xv" + i, event.get("c" + i));
            }

            env.undeployAll();
        }

        public EnumSet<RegressionFlag> flags() {
            return EnumSet.of(RegressionFlag.COMPILEROPS);
        }
    }

    public static class ClientCompileLargeAggregationAccess implements RegressionExecution {
        private final int numColumns;

        public ClientCompileLargeAggregationAccess(int numColumns) {
            this.numColumns = numColumns;
        }

        public void run(RegressionEnvironment env) {
            StringWriter epl = new StringWriter();
            epl.append("@name('s0') select ");
            String delimiter = "";
            for (int i = 0; i < numColumns; i++) {
                epl.append(delimiter).append("sorted(intPrimitive + ").append(Integer.toString(i)).append(").firstEvent() as c").append(Integer.toString(i));
                delimiter = ",";
                epl.append(delimiter).append("sorted(intPrimitive + ").append(Integer.toString(i)).append(").selectFrom(v => v) as d").append(Integer.toString(i));
            }
            epl.append(" from SupportBean#keepall");
            env.compileDeploy(epl.toString()).addListener("s0");

            SupportBean sbOne = new SupportBean("E1", 10);
            env.sendEventBean(sbOne);
            env.assertEventNew("s0", event -> {
                for (int i = 0; i < numColumns; i++) {
                    assertEquals(sbOne, event.get("c" + i));
                }
            });

            env.undeployAll();
        }

        public EnumSet<RegressionFlag> flags() {
            return EnumSet.of(RegressionFlag.COMPILEROPS);
        }
    }

    public static class ClientCompileLargeAggregation implements RegressionExecution {
        private final int numColumns;

        public ClientCompileLargeAggregation(int numColumns) {
            this.numColumns = numColumns;
        }

        public void run(RegressionEnvironment env) {
            StringWriter epl = new StringWriter();
            epl.append("@name('s0') select ");
            String delimiter = "";
            for (int i = 0; i < numColumns; i++) {
                epl.append(delimiter).append("sum(intPrimitive + ").append(Integer.toString(i)).append(") as c").append(Integer.toString(i));
                delimiter = ",";
            }
            epl.append(" from SupportBean#lastevent");
            env.compileDeploy(epl.toString()).addListener("s0");

            sendBeanAssert(env, 10);
            sendBeanAssert(env, 50);

            env.undeployAll();
        }

        public EnumSet<RegressionFlag> flags() {
            return EnumSet.of(RegressionFlag.COMPILEROPS);
        }

        private void sendBeanAssert(RegressionEnvironment env, int intPrimitive) {
            env.sendEventBean(new SupportBean("x", intPrimitive));
            env.assertEventNew("s0", event -> {
                for (int i = 0; i < numColumns; i++) {
                    assertTrue(event.getEventType().isProperty("c" + i));
                    assertEquals(intPrimitive + i, event.get("c" + i));
                }
            });
        }
    }

    public static class ClientCompileLargeTableAggregationEnterLeaveAccessAgg implements RegressionExecution {
        private final int numColumns;

        public ClientCompileLargeTableAggregationEnterLeaveAccessAgg(int numColumns) {
            this.numColumns = numColumns;
        }

        public void run(RegressionEnvironment env) {
            StringWriter epl = setupTable(numColumns, "SupportBean#length(1)", "window(*) @type(SupportBean)", (writer, index) -> writer.append("window(*)"));
            env.compileDeploy(epl.toString());

            SupportBean sbOne = new SupportBean("E0", 0);
            env.sendEventBean(sbOne);
            assertTableWindow(env, numColumns, sbOne);

            SupportBean sbTwo = new SupportBean("E1", 1);
            env.sendEventBean(sbTwo);
            assertTableWindow(env, numColumns, sbTwo);

            env.undeployAll();
        }

        public EnumSet<RegressionFlag> flags() {
            return EnumSet.of(RegressionFlag.COMPILEROPS);
        }
    }

    public static class ClientCompileLargeTableAggregationEnterLeaveMethod implements RegressionExecution {
        private final int numColumns;

        public ClientCompileLargeTableAggregationEnterLeaveMethod(int numColumns) {
            this.numColumns = numColumns;
        }

        public void run(RegressionEnvironment env) {
            StringWriter epl = setupTable(numColumns, "SupportBean#length(1)", "sum(int)", (writer, index) -> writer.append("sum(intPrimitive + ").append(Integer.toString(index)).append(")"));
            env.compileDeploy(epl.toString());

            env.sendEventBean(new SupportBean("E0", 20));
            assertTableSum(env, numColumns, 20);

            env.sendEventBean(new SupportBean("E1", 30));
            assertTableSum(env, numColumns, 30);

            env.undeployAll();
        }

        public EnumSet<RegressionFlag> flags() {
            return EnumSet.of(RegressionFlag.COMPILEROPS);
        }
    }

    public static class ClientCompileLargeTableAggregationReset implements RegressionExecution {
        private final int numColumns;

        public ClientCompileLargeTableAggregationReset(int numColumns) {
            this.numColumns = numColumns;
        }

        public void run(RegressionEnvironment env) {
            StringWriter epl = setupTable(numColumns, "SupportBean", "sum(int)", (writer, index) -> writer.append("sum(intPrimitive + ").append(Integer.toString(index)).append(")"));

            epl.append("on SupportBean_S0 merge MyTable when matched then update set ");
            String delimiter = "";
            for (int i = 0; i < numColumns; i++) {
                epl.append(delimiter).append("c").append(Integer.toString(i)).append(".reset()");
                delimiter = ",";
            }
            epl.append(";\n");

            epl.append("on SupportBean_S1 merge MyTable as mt when matched then update set mt.reset();\n");
            env.compileDeploy(epl.toString());

            env.sendEventBean(new SupportBean("E0", 2));
            assertTableSum(env, numColumns, 2);

            env.sendEventBean(new SupportBean_S0(0));
            assertTableReset(env, numColumns);

            env.sendEventBean(new SupportBean("E1", 3));
            assertTableSum(env, numColumns, 3);

            env.sendEventBean(new SupportBean_S1(0));
            assertTableReset(env, numColumns);

            env.undeployAll();
        }

        public EnumSet<RegressionFlag> flags() {
            return EnumSet.of(RegressionFlag.COMPILEROPS);
        }
    }

    public static class ClientCompileLargeSelectCol implements RegressionExecution {
        private final EventRepresentationChoice representation;
        private final int numColumns;

        public ClientCompileLargeSelectCol(EventRepresentationChoice representation, int numColumns) {
            this.representation = representation;
            this.numColumns = numColumns;
        }

        public void run(RegressionEnvironment env) {
            StringWriter epl = new StringWriter();
            epl.append(representation.getAnnotationText()).append("@name('s0') select ");
            String delimiter = "";
            for (int i = 0; i < numColumns; i++) {
                epl.append(delimiter).append("theString||'").append(Integer.toString(i)).append("' as c").append(Integer.toString(i));
                delimiter = ",";
            }
            epl.append(" from SupportBean");
            env.compileDeploy(epl.toString()).addListener("s0");

            env.sendEventBean(new SupportBean("x", 0));
            env.assertEventNew("s0", event -> {
                for (int i = 0; i < numColumns; i++) {
                    assertTrue(event.getEventType().isProperty("c" + i));
                    assertEquals("x" + i, event.get("c" + i));
                }
            });

            env.undeployAll();
        }

        public String name() {
            return this.getClass().getSimpleName() + "{" +
                    "representation=" + representation +
                    '}';
        }

        public EnumSet<RegressionFlag> flags() {
            return EnumSet.of(RegressionFlag.COMPILEROPS);
        }
    }

    private static StringWriter setupTable(int numColumns, String selector, String tableColType, BiConsumer<StringWriter, Integer> intoTable) {
        StringWriter epl = new StringWriter();
        epl.append("@name('table') create table MyTable(");
        String delimiter = "";
        for (int i = 0; i < numColumns; i++) {
            epl.append(delimiter).append("c").append(Integer.toString(i)).append(" ").append(tableColType);
            delimiter = ",";
        }
        epl.append(");\n");

        epl.append("into table MyTable select ");
        delimiter = "";
        for (int i = 0; i < numColumns; i++) {
            epl.append(delimiter);
            intoTable.accept(epl, i);
            epl.append(" as c").append(Integer.toString(i));
            delimiter = ",";
        }
        epl.append(" from ").append(selector).append(";\n");
        return epl;
    }

    private static void assertTableSum(RegressionEnvironment env, int numColumns, int intPrimitive) {
        env.assertIterator("table", iterator -> {
            EventBean result = iterator.next();
            for (int i = 0; i < numColumns; i++) {
                assertTrue(result.getEventType().isProperty("c" + i));
                assertEquals(intPrimitive + i, result.get("c" + i));
            }
        });
    }

    private static void assertTableReset(RegressionEnvironment env, int numColumns) {
        env.assertIterator("table", iterator -> {
            EventBean result = iterator.next();
            for (int i = 0; i < numColumns; i++) {
                assertNull(result.get("c" + i));
            }
        });
    }

    private static void assertTableWindow(RegressionEnvironment env, int numColumns, SupportBean sb) {
        env.assertIterator("table", iterator -> {
            EventBean result = iterator.next();
            for (int i = 0; i < numColumns; i++) {
                SupportBean[] beans = (SupportBean[]) result.get("c" + i);
                assertEquals(1, beans.length);
                assertEquals(beans[0], sb);
            }
        });
    }
}
