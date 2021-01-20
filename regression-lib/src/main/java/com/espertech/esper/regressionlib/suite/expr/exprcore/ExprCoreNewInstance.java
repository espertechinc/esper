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
package com.espertech.esper.regressionlib.suite.expr.exprcore;

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.client.type.EPTypeClassParameterized;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.common.internal.support.SupportEventTypeAssertionEnum;
import com.espertech.esper.common.internal.support.SupportEventTypeAssertionUtil;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.support.bean.SupportObjectCtor;
import com.espertech.esper.regressionlib.support.expreval.SupportEvalBuilder;

import java.util.*;

import static com.espertech.esper.common.client.scopetest.EPAssertionUtil.assertProps;
import static org.junit.Assert.*;

public class ExprCoreNewInstance {

    public static Collection<RegressionExecution> executions() {
        List<RegressionExecution> executions = new ArrayList<>();
        executions.add(new ExecCoreNewInstanceKeyword(true));
        executions.add(new ExecCoreNewInstanceKeyword(false));
        executions.add(new ExecCoreNewInstanceStreamAlias());
        executions.add(new ExecCoreNewInstanceGeneric(false));
        executions.add(new ExecCoreNewInstanceGeneric(true));
        executions.add(new ExecCoreNewInstanceInvalid());
        executions.add(new ExecCoreNewInstanceArraySized(false));
        executions.add(new ExecCoreNewInstanceArraySized(true));
        executions.add(new ExecCoreNewInstanceArrayInitOneDim(false));
        executions.add(new ExecCoreNewInstanceArrayInitOneDim(true));
        executions.add(new ExecCoreNewInstanceArrayInitTwoDim(false));
        executions.add(new ExecCoreNewInstanceArrayInitTwoDim(true));
        executions.add(new ExecCoreNewInstanceArrayInvalid());
        return executions;
    }

    private static class ExecCoreNewInstanceArrayInitTwoDim implements RegressionExecution {
        boolean soda;

        public ExecCoreNewInstanceArrayInitTwoDim(boolean soda) {
            this.soda = soda;
        }

        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') select " +
                "new char[][] {} as c0, " +
                "new double[][] {{1}} as c1, " +
                "new int[][] {{1},{intPrimitive,10}} as c2, " +
                "new float[][] {{},{1},{2.0f}} as c3, " +
                "new long[][] {{1L,Long.MAX_VALUE,-1L}} as c4, " +
                "new String[][] {} as c5, " +
                "new String[][] {{},{},{\"x\"},{}} as c6, " +
                "new String[][] {{\"x\",\"y\"},{\"z\"}} as c7, " +
                "new Integer[][] {{intPrimitive,intPrimitive+1},{intPrimitive+2,intPrimitive+3}} as c8, " +
                "new java.util.Calendar[][] {} as c9, " +
                "new Object[][] {{}} as c10, " +
                "new Object[][] {{1}} as c11, " +
                "new Object[][] {{\"x\"},{1},{10L}} as c12 " +
                "from SupportBean";
            env.compileDeploy(soda, epl).addListener("s0");

            env.assertStatement("s0", statement -> {
                EventType out = statement.getEventType();
                assertEquals(char[][].class, out.getPropertyType("c0"));
                assertEquals(double[][].class, out.getPropertyType("c1"));
                assertEquals(int[][].class, out.getPropertyType("c2"));
                assertEquals(float[][].class, out.getPropertyType("c3"));
                assertEquals(long[][].class, out.getPropertyType("c4"));
                assertEquals(String[][].class, out.getPropertyType("c5"));
                assertEquals(String[][].class, out.getPropertyType("c6"));
                assertEquals(String[][].class, out.getPropertyType("c7"));
                assertEquals(Integer[][].class, out.getPropertyType("c8"));
                assertEquals(Calendar[][].class, out.getPropertyType("c9"));
                assertEquals(Object[][].class, out.getPropertyType("c10"));
                assertEquals(Object[][].class, out.getPropertyType("c11"));
                assertEquals(Object[][].class, out.getPropertyType("c12"));
            });

            env.sendEventBean(new SupportBean("E1", 2));
            env.assertEventNew("s0", event -> {
                assertProps(event, "c0,c1,c2,c3,c4,c5,c6,c7,c8,c9,c10,c11,c12".split(","),
                    new Object[]{new char[][]{}, new double[][]{{1}}, new int[][]{{1}, {2, 10}},
                        new float[][]{{}, {1}, {2.0f}}, new long[][]{{1L, Long.MAX_VALUE, -1L}}, new String[][]{},
                        new String[][]{{}, {}, {"x"}, {}}, new String[][]{{"x", "y"}, {"z"}}, new Integer[][]{{2, 2 + 1}, {2 + 2, 2 + 3}},
                        new java.util.Calendar[][]{}, new Object[][]{{}}, new Object[][]{{1}},
                        new Object[][]{{"x"}, {1}, {10L}}
                    });
            });

            env.undeployAll();
        }

        public String name() {
            return this.getClass().getSimpleName() + "{" +
                "soda=" + soda +
                '}';
        }
    }

    private static class ExecCoreNewInstanceArrayInitOneDim implements RegressionExecution {
        boolean soda;

        public ExecCoreNewInstanceArrayInitOneDim(boolean soda) {
            this.soda = soda;
        }

        public void run(RegressionEnvironment env) {
            String[] fields = "c0,c1,c2,c3,c4,c5,c6,c7,c8,c9,c10,c11,c12".split(",");
            SupportEvalBuilder builder = new SupportEvalBuilder("SupportBean")
                .expression(fields[0], "new char[] {}")
                .expression(fields[1], "new double[] {1}")
                .expression(fields[2], "new int[] {1,intPrimitive,10}")
                .expression(fields[3], "new float[] {1,2.0f}")
                .expression(fields[4], "new long[] {1L,Long.MAX_VALUE,-1L}")
                .expression(fields[5], "new String[] {}")
                .expression(fields[6], "new String[] {\"x\"}")
                .expression(fields[7], "new String[] {\"x\",\"y\"}")
                .expression(fields[8], "new Integer[] {intPrimitive,intPrimitive+1,intPrimitive+2,intPrimitive+3}")
                .expression(fields[9], "new java.util.Calendar[] {}")
                .expression(fields[10], "new Object[] {}")
                .expression(fields[11], "new Object[] {1}")
                .expression(fields[12], "new Object[] {\"x\",1,10L}");

            builder.statementConsumer(stmt -> {
                EventType out = stmt.getEventType();
                assertEquals(char[].class, out.getPropertyType("c0"));
                assertEquals(double[].class, out.getPropertyType("c1"));
                assertEquals(int[].class, out.getPropertyType("c2"));
                assertEquals(float[].class, out.getPropertyType("c3"));
                assertEquals(long[].class, out.getPropertyType("c4"));
                assertEquals(String[].class, out.getPropertyType("c5"));
                assertEquals(String[].class, out.getPropertyType("c6"));
                assertEquals(String[].class, out.getPropertyType("c7"));
                assertEquals(Integer[].class, out.getPropertyType("c8"));
                assertEquals(Calendar[].class, out.getPropertyType("c9"));
                assertEquals(Object[].class, out.getPropertyType("c10"));
                assertEquals(Object[].class, out.getPropertyType("c11"));
                assertEquals(Object[].class, out.getPropertyType("c12"));
            });

            builder.assertion(new SupportBean("E1", 2)).expect(fields, new char[0], new double[]{1}, new int[]{1, 2, 10},
                new float[]{1, 2}, new long[]{1, Long.MAX_VALUE, -1}, new String[0],
                new String[]{"x"}, new String[]{"x", "y"}, new Integer[]{2, 3, 4, 5},
                new Calendar[0], new Object[0], new Object[]{1}, new Object[]{"x", 1, 10L});

            builder.run(env, soda);
            env.undeployAll();
        }

        public String name() {
            return this.getClass().getSimpleName() + "{" +
                "soda=" + soda +
                '}';
        }
    }

    private static class ExecCoreNewInstanceGeneric implements RegressionExecution {
        boolean soda;

        public ExecCoreNewInstanceGeneric(boolean soda) {
            this.soda = soda;
        }

        public void run(RegressionEnvironment env) {
            String[] fields = "c0,c1,c2,c3,c4,c5".split(",");
            SupportEvalBuilder builder = new SupportEvalBuilder("SupportBean")
                .expression(fields[0], "new ArrayList<String>()")
                .expression(fields[1], "new HashMap<String,Integer>()")
                .expression(fields[2], "new ArrayList<String>(20)")
                .expression(fields[3], "new ArrayList<String>[5]")
                .expression(fields[4], "new ArrayList<String>[] {new ArrayList<String>(),new ArrayList<String>()}")
                .expression(fields[5], "new ArrayList<String[][]>[2][]");

            builder.statementConsumer(stmt -> {
                EventType out = stmt.getEventType();
                assertEquals(EPTypeClassParameterized.from(ArrayList.class, String.class), out.getPropertyEPType("c0"));
                assertEquals(EPTypeClassParameterized.from(HashMap.class, String.class, Integer.class), out.getPropertyEPType("c1"));
                assertEquals(EPTypeClassParameterized.from(ArrayList.class, String.class), out.getPropertyEPType("c2"));
                assertEquals(EPTypeClassParameterized.from(ArrayList[].class, String.class), out.getPropertyEPType("c3"));
                assertEquals(EPTypeClassParameterized.from(ArrayList[].class, String.class), out.getPropertyEPType("c4"));
                assertEquals(EPTypeClassParameterized.from(ArrayList[][].class, String[][].class), out.getPropertyEPType("c5"));
            });

            builder.assertion(new SupportBean("E1", 2))
                .verify("c0", value -> assertTrue(value instanceof ArrayList))
                .verify("c1", value -> assertTrue(value instanceof HashMap))
                .verify("c2", value -> assertTrue(value instanceof ArrayList))
                .verify("c3", value -> {
                    ArrayList[] array = (ArrayList[]) value;
                    assertEquals(5, array.length);
                })
                .verify("c4", value -> {
                    ArrayList[] array = (ArrayList[]) value;
                    assertEquals(2, array.length);
                    for (int i = 0; i < 2; i++) {
                        assertTrue(array[i] instanceof ArrayList);
                    }
                })
                .verify("c5", value -> {
                    ArrayList[][] array = (ArrayList[][]) value;
                    assertEquals(2, array.length);
                });

            builder.run(env, soda);
            env.undeployAll();
        }

        public String name() {
            return this.getClass().getSimpleName() + "{" +
                "soda=" + soda +
                '}';
        }
    }

    private static class ExecCoreNewInstanceArraySized implements RegressionExecution {
        boolean soda;

        public ExecCoreNewInstanceArraySized(boolean soda) {
            this.soda = soda;
        }

        public void run(RegressionEnvironment env) {
            String[] fields = "new double[1],c1,c2,new double[1][2],c4".split(",");
            SupportEvalBuilder builder = new SupportEvalBuilder("SupportBean")
                .expression(fields[0], "new double[1]")
                .expression(fields[1], "new Integer[2*2]")
                .expression(fields[2], "new java.util.Calendar[intPrimitive]")
                .expression(fields[3], "new double[1][2]")
                .expression(fields[4], "new java.util.Calendar[intPrimitive][intPrimitive]");

            builder.statementConsumer(stmt -> {
                EventType out = stmt.getEventType();
                assertEquals(double[].class, out.getPropertyType("new double[1]"));
                assertEquals(Integer[].class, out.getPropertyType("c1"));
                assertEquals(Calendar[].class, out.getPropertyType("c2"));
                assertEquals(double[][].class, out.getPropertyType("new double[1][2]"));
                assertEquals(Calendar[][].class, out.getPropertyType("c4"));
            });

            builder.assertion(new SupportBean("E1", 2)).expect(fields, new double[1], new Integer[4], new Calendar[2], new double[1][2], new Calendar[2][2]);

            builder.run(env, soda);
            env.undeployAll();
        }

        public String name() {
            return this.getClass().getSimpleName() + "{" +
                "soda=" + soda +
                '}';
        }
    }

    private static class ExecCoreNewInstanceArrayInvalid implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            // Dimension-provided
            //
            env.tryInvalidCompile("select new double[] from SupportBean",
                "Incorrect syntax near 'from' (a reserved keyword) expecting a left curly bracket '{' but found 'from' at line 1 column 20");

            env.tryInvalidCompile("select new double[1, 2, 3] from SupportBean",
                "Incorrect syntax near ',' expecting a right angle bracket ']'");

            env.tryInvalidCompile("select new double['a'] from SupportBean",
                "Failed to validate select-clause expression 'new double[\"a\"]': New-keyword with an array-type result requires an Integer-typed dimension but received type 'String'");
            env.tryInvalidCompile("select new double[1]['a'] from SupportBean", "skip");

            // Initializers-provided
            //
            env.tryInvalidCompile("select new double[] {null} from SupportBean",
                "Failed to validate select-clause expression 'new double[] {null}': Array element type mismatch: Expecting type double but received null");

            env.tryInvalidCompile("select new String[] {1} from SupportBean",
                "Failed to validate select-clause expression 'new String[] {1}': Array element type mismatch: Expecting type String but received type int");

            env.tryInvalidCompile("select new String[] {intPrimitive} from SupportBean",
                "Failed to validate select-clause expression 'new String[] {intPrimitive}': Array element type mismatch: Expecting type String but received type Integer");

            env.tryInvalidCompile("select new String[][] {intPrimitive} from SupportBean",
                "Failed to validate select-clause expression 'new String[] {intPrimitive}': Two-dimensional array element does not allow element expression 'intPrimitive'");

            env.tryInvalidCompile("select new String[][] {{intPrimitive}} from SupportBean",
                "Failed to validate select-clause expression 'new String[] {{intPrimitive}}': Array element type mismatch: Expecting type String but received type Integer");

            env.tryInvalidCompile("select new String[] {{'x'}} from SupportBean",
                "Failed to validate select-clause expression 'new String[] {{\"x\"}}': Array element type mismatch: Expecting type String but received type String[]");

            // Runtime null handling
            //
            String eplNullDimension = "@name('s0') select new double[intBoxed] from SupportBean";
            env.compileDeploy(eplNullDimension).addListener("s0");
            try {
                env.sendEventBean(new SupportBean());
                fail();
            } catch (RuntimeException ex) {
                // expected, rethrown
                assertTrue(ex.getMessage().contains("new-array received a null value for dimension"));
            }
            env.undeployAll();

            String eplNullValuePrimitiveArray = "@name('s0') select new double[] {intBoxed} from SupportBean";
            env.compileDeploy(eplNullValuePrimitiveArray).addListener("s0");
            try {
                env.sendEventBean(new SupportBean());
                fail();
            } catch (RuntimeException ex) {
                // expected, rethrown
                assertTrue(ex.getMessage().contains("new-array received a null value"));
            }
            env.undeployAll();
        }
    }

    private static class ExecCoreNewInstanceInvalid implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            // try variable
            env.compileDeploy("create constant variable java.util.concurrent.atomic.AtomicInteger cnt = new java.util.concurrent.atomic.AtomicInteger(1)");

            // try shallow invalid cases
            env.tryInvalidCompile("select new Dummy() from SupportBean",
                "Failed to validate select-clause expression 'new Dummy()': Failed to resolve type parameter 'Dummy'");

            env.tryInvalidCompile("select new SupportPrivateCtor() from SupportBean",
                "Failed to validate select-clause expression 'new SupportPrivateCtor()': Failed to find a suitable constructor for class ");

            env.undeployAll();
        }
    }

    private static class ExecCoreNewInstanceStreamAlias implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = "c0".split(",");
            SupportEvalBuilder builder = new SupportEvalBuilder("SupportBean", "sb")
                .expressions(fields, "new SupportObjectCtor(sb)");

            SupportBean sb = new SupportBean();
            builder.assertion(sb).verify("c0", result -> assertSame(sb, ((SupportObjectCtor) result).getObject()));

            builder.run(env);
            env.undeployAll();
        }
    }

    private static class ExecCoreNewInstanceKeyword implements RegressionExecution {
        private final boolean soda;

        public ExecCoreNewInstanceKeyword(boolean soda) {
            this.soda = soda;
        }

        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') select " +
                "new SupportBean(\"A\",intPrimitive) as c0, " +
                "new SupportBean(\"B\",intPrimitive+10), " +
                "new SupportBean() as c2, " +
                "new SupportBean(\"ABC\",0).getTheString() as c3 " +
                "from SupportBean";
            env.compileDeploy(soda, epl).addListener("s0");
            Object[][] expectedAggType = new Object[][]{{"c0", SupportBean.class}, {"new SupportBean(\"B\",intPrimitive+10)", SupportBean.class}};
            env.assertStatement("s0", statement -> SupportEventTypeAssertionUtil.assertEventTypeProperties(expectedAggType, statement.getEventType(), SupportEventTypeAssertionEnum.NAME, SupportEventTypeAssertionEnum.TYPE));

            env.sendEventBean(new SupportBean("E1", 10));
            env.assertEventNew("s0", event -> {
                assertSupportBean(event.get("c0"), new Object[]{"A", 10});
                assertSupportBean(((Map) event.getUnderlying()).get("new SupportBean(\"B\",intPrimitive+10)"), new Object[]{"B", 20});
                assertSupportBean(event.get("c2"), new Object[]{null, 0});
                assertEquals("ABC", event.get("c3"));
            });

            env.undeployAll();
        }

        public String name() {
            return this.getClass().getSimpleName() + "{" +
                "soda=" + soda +
                '}';
        }

        private void assertSupportBean(Object bean, Object[] objects) {
            SupportBean b = (SupportBean) bean;
            assertEquals(objects[0], b.getTheString());
            assertEquals(objects[1], b.getIntPrimitive());
        }
    }
}
