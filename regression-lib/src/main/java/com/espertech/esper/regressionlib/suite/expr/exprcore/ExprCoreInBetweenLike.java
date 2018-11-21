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

import com.espertech.esper.common.client.EPCompiled;
import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.common.client.soda.*;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.common.internal.support.SupportBean_S1;
import com.espertech.esper.common.internal.util.SerializableObjectCopier;
import com.espertech.esper.compiler.client.CompilerArguments;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.SupportMessageAssertUtil;
import com.espertech.esper.regressionlib.support.bean.SupportBeanArrayCollMap;
import com.espertech.esper.regressionlib.support.bean.SupportBeanComplexProps;
import com.espertech.esper.runtime.client.DeploymentOptions;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import static junit.framework.TestCase.assertEquals;
import static org.junit.Assert.*;

public class ExprCoreInBetweenLike {
    public static Collection<RegressionExecution> executions() {
        ArrayList<RegressionExecution> executions = new ArrayList<>();
        executions.add(new ExprCoreInNumeric());
        executions.add(new ExprCoreInObject());
        executions.add(new ExprCoreInArraySubstitution());
        executions.add(new ExprCoreInCollectionArrayProp());
        executions.add(new ExprCoreInCollectionArrays());
        executions.add(new ExprCoreInCollectionColl());
        executions.add(new ExprCoreInCollectionMaps());
        executions.add(new ExprCoreInCollectionMixed());
        executions.add(new ExprCoreInCollectionObjectArrayProp());
        executions.add(new ExprCoreInCollectionArrayConst());
        executions.add(new ExprCoreInStringExprOM());
        executions.add(new ExprCoreInStringExpr());
        executions.add(new ExprCoreBetweenBigIntBigDecExpr());
        executions.add(new ExprCoreBetweenStringExpr());
        executions.add(new ExprCoreBetweenNumericExpr());
        executions.add(new ExprCoreInBoolExpr());
        executions.add(new ExprCoreInNumericCoercionLong());
        executions.add(new ExprCoreInNumericCoercionDouble());
        executions.add(new ExprCoreBetweenNumericCoercionLong());
        executions.add(new ExprCoreInRange());
        executions.add(new ExprCoreBetweenNumericCoercionDouble());
        executions.add(new ExprCoreInBetweenInvalid());
        return executions;
    }

    private static class ExprCoreInNumeric implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            Double[] input = new Double[]{1d, null, 1.1d, 1.0d, 1.0999999999, 2d, 4d};
            Boolean[] result = new Boolean[]{false, null, true, false, false, true, true};
            tryNumeric(env, "doubleBoxed in (1.1d, 7/3.5, 2*6/3, 0)", input, result);

            tryNumeric(env, "doubleBoxed in (7/3d, null)",
                new Double[]{2d, 7 / 3d, null},
                new Boolean[]{null, true, null});

            tryNumeric(env, "doubleBoxed in (5,5,5,5,5, -1)",
                new Double[]{5.0, 5d, 0d, null, -1d},
                new Boolean[]{true, true, false, null, true});

            tryNumeric(env, "doubleBoxed not in (1.1d, 7/3.5, 2*6/3, 0)",
                new Double[]{1d, null, 1.1d, 1.0d, 1.0999999999, 2d, 4d},
                new Boolean[]{true, null, false, true, true, false, false});
        }
    }

    private static class ExprCoreInObject implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('stmt1') select s0.anyObject in (objectArr) as value from SupportBeanArrayCollMap s0";

            env.compileDeploy(epl).addListener("stmt1");

            SupportBean_S1 s1 = new SupportBean_S1(100);
            SupportBeanArrayCollMap arrayBean = new SupportBeanArrayCollMap(s1);
            arrayBean.setObjectArr(new Object[]{null, "a", false, s1});
            env.sendEventBean(arrayBean);
            assertEquals(true, env.listener("stmt1").assertOneGetNewAndReset().get("value"));

            arrayBean.setAnyObject(null);
            env.sendEventBean(arrayBean);
            assertNull(env.listener("stmt1").assertOneGetNewAndReset().get("value"));

            env.undeployAll();
        }
    }

    private static class ExprCoreInArraySubstitution implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String stmtText = "@name('s0') select intPrimitive in (?::int[primitive]) as result from SupportBean";
            EPCompiled compiled = env.compile(stmtText);
            env.deploy(compiled, new DeploymentOptions().setStatementSubstitutionParameter(prepared -> prepared.setObject(1, new int[]{10, 20, 30})));
            env.addListener("s0");

            env.sendEventBean(new SupportBean("E1", 10), SupportBean.class.getSimpleName());
            assertTrue((Boolean) env.listener("s0").assertOneGetNewAndReset().get("result"));

            env.sendEventBean(new SupportBean("E2", 9), SupportBean.class.getSimpleName());
            assertFalse((Boolean) env.listener("s0").assertOneGetNewAndReset().get("result"));

            env.undeployAll();
        }
    }

    private static class ExprCoreInCollectionArrayProp implements RegressionExecution {
        public void run(RegressionEnvironment env) {

            String epl = "@name('s0') select 10 in (arrayProperty) as result from SupportBeanComplexProps";
            env.compileDeploy(epl).addListener("s0");
            assertEquals(Boolean.class, env.statement("s0").getEventType().getPropertyType("result"));

            epl = "@name('s1') select 5 in (arrayProperty) as result from SupportBeanComplexProps";
            env.compileDeploy(epl).addListener("s1");
            env.milestone(0);

            env.sendEventBean(SupportBeanComplexProps.makeDefaultBean());
            assertEquals(true, env.listener("s0").assertOneGetNewAndReset().get("result"));
            assertEquals(false, env.listener("s1").assertOneGetNewAndReset().get("result"));

            env.undeployAll();
        }
    }

    private static class ExprCoreInCollectionArrays implements RegressionExecution {
        public void run(RegressionEnvironment env) {

            String epl = "@name('s0') select 1 in (intArr, longArr) as resOne, 1 not in (intArr, longArr) as resTwo from SupportBeanArrayCollMap";
            env.compileDeploy(epl).addListener("s0");

            String[] fields = "resOne, resTwo".split(",");
            sendArrayCollMap(env, new SupportBeanArrayCollMap(new int[]{10, 20, 30}));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{false, true});
            sendArrayCollMap(env, new SupportBeanArrayCollMap(new int[]{10, 1, 30}));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{true, false});
            sendArrayCollMap(env, new SupportBeanArrayCollMap(new int[]{30}, new Long[]{20L, 1L}));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{true, false});
            sendArrayCollMap(env, new SupportBeanArrayCollMap(new int[]{}, new Long[]{null, 1L}));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{true, false});
            sendArrayCollMap(env, new SupportBeanArrayCollMap(null, new Long[]{1L, 100L}));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{true, false});
            sendArrayCollMap(env, new SupportBeanArrayCollMap(null, new Long[]{0L, 100L}));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{false, true});

            env.undeployAll();
        }
    }

    private static class ExprCoreInCollectionColl implements RegressionExecution {
        public void run(RegressionEnvironment env) {

            String[] fields = "resOne, resTwo".split(",");
            String epl = "@name('s0') select 1 in (intCol, longCol) as resOne, 1 not in (longCol, intCol) as resTwo from SupportBeanArrayCollMap";
            env.compileDeploy(epl).addListener("s0");

            sendArrayCollMap(env, new SupportBeanArrayCollMap(true, new int[]{10, 20, 30}, null));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{false, true});
            sendArrayCollMap(env, new SupportBeanArrayCollMap(true, new int[]{10, 20, 1}, null));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{true, false});
            sendArrayCollMap(env, new SupportBeanArrayCollMap(true, new int[]{30}, new Long[]{20L, 1L}));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{false, true});
            sendArrayCollMap(env, new SupportBeanArrayCollMap(true, new int[]{}, new Long[]{null, 1L}));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{false, true});
            sendArrayCollMap(env, new SupportBeanArrayCollMap(true, null, new Long[]{1L, 100L}));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{false, true});

            env.undeployAll();
        }
    }

    private static class ExprCoreInCollectionMaps implements RegressionExecution {
        @Override
        public boolean excludeWhenInstrumented() {
            return true;
        }

        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') select 1 in (longMap, intMap) as resOne, 1 not in (longMap, intMap) as resTwo from SupportBeanArrayCollMap";
            env.compileDeploy(epl).addListener("s0");

            String[] fields = "resOne, resTwo".split(",");
            sendArrayCollMap(env, new SupportBeanArrayCollMap(false, new int[]{10, 20, 30}, null));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{false, true});
            sendArrayCollMap(env, new SupportBeanArrayCollMap(false, new int[]{10, 20, 1}, null));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{true, false});
            sendArrayCollMap(env, new SupportBeanArrayCollMap(false, new int[]{30}, new Long[]{20L, 1L}));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{false, true});
            sendArrayCollMap(env, new SupportBeanArrayCollMap(false, new int[]{}, new Long[]{null, 1L}));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{false, true});
            sendArrayCollMap(env, new SupportBeanArrayCollMap(false, null, new Long[]{1L, 100L}));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{false, true});

            env.undeployAll();
        }
    }

    private static class ExprCoreInCollectionMixed implements RegressionExecution {
        public void run(RegressionEnvironment env) {

            String epl = "@name('s0') select 1 in (longBoxed, intArr, longMap, intCol) as resOne, 1 not in (longBoxed, intArr, longMap, intCol) as resTwo from SupportBeanArrayCollMap";
            env.compileDeploy(epl).addListener("s0");

            String[] fields = "resOne, resTwo".split(",");
            sendArrayCollMap(env, new SupportBeanArrayCollMap(1L, new int[0], new Long[0], new int[0]));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{true, false});
            sendArrayCollMap(env, new SupportBeanArrayCollMap(2L, null, new Long[0], new int[0]));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{false, true});

            sendArrayCollMap(env, new SupportBeanArrayCollMap(null, null, null, new int[]{3, 4, 5, 6, 7, 7, 7, 8, 8, 8, 1}));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{true, false});

            sendArrayCollMap(env, new SupportBeanArrayCollMap(-1L, null, new Long[]{1L}, new int[]{3, 4, 5, 6, 7, 7, 7, 8, 8}));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{false, true});
            sendArrayCollMap(env, new SupportBeanArrayCollMap(-1L, new int[]{1}, null, new int[]{}));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{true, false});

            env.undeployAll();
        }
    }

    private static class ExprCoreInCollectionObjectArrayProp implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') select 1 in (objectArr) as resOne, 2 in (objectArr) as resTwo from SupportBeanArrayCollMap";
            env.compileDeploy(epl).addListener("s0");
            String[] fields = "resOne, resTwo".split(",");

            sendArrayCollMap(env, new SupportBeanArrayCollMap(new Object[]{}));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{false, false});
            sendArrayCollMap(env, new SupportBeanArrayCollMap(new Object[]{1, 2}));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{true, true});
            sendArrayCollMap(env, new SupportBeanArrayCollMap(new Object[]{1d, 2L}));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{false, false});
            sendArrayCollMap(env, new SupportBeanArrayCollMap(new Object[]{null, 2}));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{null, true});

            env.undeployAll();
        }
    }

    private static class ExprCoreInCollectionArrayConst implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') select 1 in ({1,2,3}) as resOne, 2 in ({0, 1}) as resTwo from SupportBeanArrayCollMap";
            env.compileDeploy(epl).addListener("s0");
            String[] fields = "resOne, resTwo".split(",");

            sendArrayCollMap(env, new SupportBeanArrayCollMap(new Object[]{}));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{true, false});

            env.undeployAll();
        }
    }

    private static class ExprCoreInStringExprOM implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String caseExpr = "@name('s0') select theString in (\"a\",\"b\",\"c\") as result from " + SupportBean.class.getSimpleName();
            EPStatementObjectModel model = new EPStatementObjectModel();
            model.setAnnotations(Collections.singletonList(AnnotationPart.nameAnnotation("s0")));
            model.setSelectClause(SelectClause.create().add(Expressions.in("theString", "a", "b", "c"), "result"));
            model.setFromClause(FromClause.create(FilterStream.create(SupportBean.class.getSimpleName())));

            tryString(env, model, caseExpr,
                new String[]{"0", "a", "b", "c", "d", null},
                new Boolean[]{false, true, true, true, false, null});

            model = new EPStatementObjectModel();
            model.setAnnotations(Collections.singletonList(AnnotationPart.nameAnnotation("s0")));
            model.setSelectClause(SelectClause.create().add(Expressions.notIn("theString", "a", "b", "c"), "result"));
            model.setFromClause(FromClause.create(FilterStream.create(SupportBean.class.getSimpleName())));
            SerializableObjectCopier.copyMayFail(model);

            tryString(env, "theString not in ('a', 'b', 'c')",
                new String[]{"0", "a", "b", "c", "d", null},
                new Boolean[]{true, false, false, false, true, null});
        }
    }

    private static class ExprCoreInStringExpr implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            tryString(env, "theString in ('a', 'b', 'c')",
                new String[]{"0", "a", "b", "c", "d", null},
                new Boolean[]{false, true, true, true, false, null});

            tryString(env, "theString in ('a')",
                new String[]{"0", "a", "b", "c", "d", null},
                new Boolean[]{false, true, false, false, false, null});

            tryString(env, "theString in ('a', 'b')",
                new String[]{"0", "b", "a", "c", "d", null},
                new Boolean[]{false, true, true, false, false, null});

            tryString(env, "theString in ('a', null)",
                new String[]{"0", "b", "a", "c", "d", null},
                new Boolean[]{null, null, true, null, null, null});

            tryString(env, "theString in (null)",
                new String[]{"0", null, "b"},
                new Boolean[]{null, null, null});

            tryString(env, "theString not in ('a', 'b', 'c')",
                new String[]{"0", "a", "b", "c", "d", null},
                new Boolean[]{true, false, false, false, true, null});

            tryString(env, "theString not in (null)",
                new String[]{"0", null, "b"},
                new Boolean[]{null, null, null});
        }
    }

    private static class ExprCoreBetweenBigIntBigDecExpr implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = "c0,c1,c2,c3".split(",");
            String epl = "@name('s0') select " +
                "intPrimitive between BigInteger.valueOf(1) and BigInteger.valueOf(3) as c0," +
                "intPrimitive between BigDecimal.valueOf(1) and BigDecimal.valueOf(3) as c1," +
                "intPrimitive in (BigInteger.valueOf(1):BigInteger.valueOf(3)) as c2," +
                "intPrimitive in (BigDecimal.valueOf(1):BigDecimal.valueOf(3)) as c3" +
                " from SupportBean";
            env.compileDeploy(epl).addListener("s0");

            env.sendEventBean(new SupportBean("E0", 0));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{false, false, false, false});

            env.sendEventBean(new SupportBean("E1", 1));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{true, true, false, false});

            env.sendEventBean(new SupportBean("E2", 2));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{true, true, true, true});

            env.sendEventBean(new SupportBean("E3", 3));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{true, true, false, false});

            env.sendEventBean(new SupportBean("E4", 4));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{false, false, false, false});

            env.undeployAll();
        }
    }

    private static class ExprCoreBetweenStringExpr implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] input;
            Boolean[] result;

            input = new String[]{"0", "a1", "a10", "c", "d", null, "a0", "b9", "b90"};
            result = new Boolean[]{false, true, true, false, false, false, true, true, false};
            tryString(env, "theString between 'a0' and 'b9'", input, result);
            tryString(env, "theString between 'b9' and 'a0'", input, result);

            tryString(env, "theString between null and 'b9'",
                new String[]{"0", null, "a0", "b9"},
                new Boolean[]{false, false, false, false});

            tryString(env, "theString between null and null",
                new String[]{"0", null, "a0", "b9"},
                new Boolean[]{false, false, false, false});

            tryString(env, "theString between 'a0' and null",
                new String[]{"0", null, "a0", "b9"},
                new Boolean[]{false, false, false, false});

            input = new String[]{"0", "a1", "a10", "c", "d", null, "a0", "b9", "b90"};
            result = new Boolean[]{true, false, false, true, true, false, false, false, true};
            tryString(env, "theString not between 'a0' and 'b9'", input, result);
            tryString(env, "theString not between 'b9' and 'a0'", input, result);
        }
    }

    private static class ExprCoreBetweenNumericExpr implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            Double[] input = new Double[]{1d, null, 1.1d, 2d, 1.0999999999, 2d, 4d, 15d, 15.00001d};
            Boolean[] result = new Boolean[]{false, false, true, true, false, true, true, true, false};
            tryNumeric(env, "doubleBoxed between 1.1 and 15", input, result);
            tryNumeric(env, "doubleBoxed between 15 and 1.1", input, result);

            tryNumeric(env, "doubleBoxed between null and 15",
                new Double[]{1d, null, 1.1d},
                new Boolean[]{false, false, false});

            tryNumeric(env, "doubleBoxed between 15 and null",
                new Double[]{1d, null, 1.1d},
                new Boolean[]{false, false, false});

            tryNumeric(env, "doubleBoxed between null and null",
                new Double[]{1d, null, 1.1d},
                new Boolean[]{false, false, false});

            input = new Double[]{1d, null, 1.1d, 2d, 1.0999999999, 2d, 4d, 15d, 15.00001d};
            result = new Boolean[]{true, false, false, false, true, false, false, false, true};
            tryNumeric(env, "doubleBoxed not between 1.1 and 15", input, result);
            tryNumeric(env, "doubleBoxed not between 15 and 1.1", input, result);

            tryNumeric(env, "doubleBoxed not between 15 and null",
                new Double[]{1d, null, 1.1d},
                new Boolean[]{false, false, false});
        }
    }

    private static class ExprCoreInBoolExpr implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            tryInBoolean(env, "boolBoxed in (true, true)",
                new Boolean[]{true, false},
                new boolean[]{true, false});

            tryInBoolean(env, "boolBoxed in (1>2, 2=3, 4<=2)",
                new Boolean[]{true, false},
                new boolean[]{false, true});

            tryInBoolean(env, "boolBoxed not in (1>2, 2=3, 4<=2)",
                new Boolean[]{true, false},
                new boolean[]{true, false});
        }
    }

    private static class ExprCoreInNumericCoercionLong implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') select intPrimitive in (shortBoxed, intBoxed, longBoxed) as result from " + SupportBean.class.getSimpleName();

            env.compileDeploy(epl).addListener("s0");
            assertEquals(Boolean.class, env.statement("s0").getEventType().getPropertyType("result"));

            sendAndAssert(env, 1, 2, 3, 4L, false);
            sendAndAssert(env, 1, 1, 3, 4L, true);
            sendAndAssert(env, 1, 3, 1, 4L, true);
            sendAndAssert(env, 1, 3, 7, 1L, true);
            sendAndAssert(env, 1, 3, 7, null, null);
            sendAndAssert(env, 1, 1, null, null, true);
            sendAndAssert(env, 1, 0, null, 1L, true);

            env.undeployAll();
        }
    }

    private static class ExprCoreInNumericCoercionDouble implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') select intBoxed in (floatBoxed, doublePrimitive, longBoxed) as result from " + SupportBean.class.getSimpleName();
            env.compileDeploy(epl).addListener("s0");

            assertEquals(Boolean.class, env.statement("s0").getEventType().getPropertyType("result"));

            sendAndAssert(env, 1, 2f, 3d, 4L, false);
            sendAndAssert(env, 1, 1f, 3d, 4L, true);
            sendAndAssert(env, 1, 1.1f, 1.0d, 4L, true);
            sendAndAssert(env, 1, 1.1f, 1.2d, 1L, true);
            sendAndAssert(env, 1, null, 1.2d, 1L, true);
            sendAndAssert(env, null, null, 1.2d, 1L, null);
            sendAndAssert(env, null, 11f, 1.2d, 1L, null);

            env.undeployAll();
        }
    }

    private static class ExprCoreBetweenNumericCoercionLong implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') select intPrimitive between shortBoxed and longBoxed as result from " + SupportBean.class.getSimpleName();

            env.compileDeploy(epl).addListener("s0");
            assertEquals(Boolean.class, env.statement("s0").getEventType().getPropertyType("result"));

            sendAndAssert(env, 1, 2, 3L, false);
            sendAndAssert(env, 2, 2, 3L, true);
            sendAndAssert(env, 3, 2, 3L, true);
            sendAndAssert(env, 4, 2, 3L, false);
            sendAndAssert(env, 5, 10, 1L, true);
            sendAndAssert(env, 1, 10, 1L, true);
            sendAndAssert(env, 10, 10, 1L, true);
            sendAndAssert(env, 11, 10, 1L, false);

            env.undeployAll();
        }
    }

    private static class ExprCoreInRange implements RegressionExecution {
        public void run(RegressionEnvironment env) {

            String[] fields = "ro,rc,rho,rhc,nro,nrc,nrho,nrhc".split(",");
            String eplOne = "@name('s0') select intPrimitive in (2:4) as ro, intPrimitive in [2:4] as rc, intPrimitive in [2:4) as rho, intPrimitive in (2:4] as rhc, " +
                "intPrimitive not in (2:4) as nro, intPrimitive not in [2:4] as nrc, intPrimitive not in [2:4) as nrho, intPrimitive not in (2:4] as nrhc " +
                "from SupportBean";
            env.compileDeploy(eplOne).addListener("s0");

            env.sendEventBean(new SupportBean("E1", 1));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{false, false, false, false, true, true, true, true});

            env.sendEventBean(new SupportBean("E1", 2));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{false, true, true, false, true, false, false, true});

            env.sendEventBean(new SupportBean("E1", 3));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{true, true, true, true, false, false, false, false});

            env.sendEventBean(new SupportBean("E1", 4));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{false, true, false, true, true, false, true, false});

            env.sendEventBean(new SupportBean("E1", 5));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{false, false, false, false, true, true, true, true});

            env.undeployAll();

            EPStatementObjectModel model = env.eplToModel(eplOne);
            String epl = model.toEPL();
            epl = epl.replace("intPrimitive between 2 and 4 as rc", "intPrimitive in [2:4] as rc");
            epl = epl.replace("intPrimitive not between 2 and 4 as nrc", "intPrimitive not in [2:4] as nrc");
            assertEquals(eplOne, epl);

            // test range reversed
            String eplTwo = "@name('s1') select intPrimitive between 4 and 2 as r1, intPrimitive in [4:2] as r2 from SupportBean";
            env.compileDeployAddListenerMile(eplTwo, "s1", 1);

            fields = "r1,r2".split(",");
            env.sendEventBean(new SupportBean("E1", 3));
            EPAssertionUtil.assertProps(env.listener("s1").assertOneGetNewAndReset(), fields, new Object[]{true, true});

            env.undeployAll();

            // test string type;
            fields = "ro".split(",");
            String eplThree = "@name('s2') select theString in ('a':'d') as ro from SupportBean";
            env.compileDeployAddListenerMile(eplThree, "s2", 2);

            env.sendEventBean(new SupportBean("a", 5));
            EPAssertionUtil.assertProps(env.listener("s2").assertOneGetNewAndReset(), fields, new Object[]{false});

            env.sendEventBean(new SupportBean("b", 5));
            EPAssertionUtil.assertProps(env.listener("s2").assertOneGetNewAndReset(), fields, new Object[]{true});

            env.sendEventBean(new SupportBean("c", 5));
            EPAssertionUtil.assertProps(env.listener("s2").assertOneGetNewAndReset(), fields, new Object[]{true});

            env.sendEventBean(new SupportBean("d", 5));
            EPAssertionUtil.assertProps(env.listener("s2").assertOneGetNewAndReset(), fields, new Object[]{false});

            env.undeployAll();
        }
    }

    private static class ExprCoreBetweenNumericCoercionDouble implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') select intBoxed between floatBoxed and doublePrimitive as result from " + SupportBean.class.getSimpleName();
            env.compileDeploy(epl).addListener("s0");

            assertEquals(Boolean.class, env.statement("s0").getEventType().getPropertyType("result"));

            sendAndAssert(env, 1, 2f, 3d, false);
            sendAndAssert(env, 2, 2f, 3d, true);
            sendAndAssert(env, 3, 2f, 3d, true);
            sendAndAssert(env, 4, 2f, 3d, false);
            sendAndAssert(env, null, 2f, 3d, false);
            sendAndAssert(env, null, null, 3d, false);
            sendAndAssert(env, 1, 3f, 2d, false);
            sendAndAssert(env, 2, 3f, 2d, true);
            sendAndAssert(env, 3, 3f, 2d, true);
            sendAndAssert(env, 4, 3f, 2d, false);
            sendAndAssert(env, null, 3f, 2d, false);
            sendAndAssert(env, null, null, 2d, false);

            env.undeployAll();
        }
    }

    private static class ExprCoreInBetweenInvalid implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "select intArr in (1, 2, 3) as r1 from SupportBeanArrayCollMap";
            SupportMessageAssertUtil.tryInvalidCompile(env, epl,
                "Failed to validate select-clause expression 'intArr in (1,2,3)': Collection or array comparison is not allowed for the IN, ANY, SOME or ALL keywords");
        }
    }

    private static void sendAndAssert(RegressionEnvironment env, Integer intBoxed, Float floatBoxed, double doublePrimitive, Boolean result) {
        SupportBean bean = new SupportBean();
        bean.setIntBoxed(intBoxed);
        bean.setFloatBoxed(floatBoxed);
        bean.setDoublePrimitive(doublePrimitive);

        env.sendEventBean(bean);

        EventBean theEvent = env.listener("s0").assertOneGetNewAndReset();
        assertEquals(result, theEvent.get("result"));
    }

    private static void sendAndAssert(RegressionEnvironment env, int intPrimitive, int shortBoxed, Integer intBoxed, Long longBoxed, Boolean result) {
        SupportBean bean = new SupportBean();
        bean.setIntPrimitive(1);
        bean.setShortBoxed((short) shortBoxed);
        bean.setIntBoxed(intBoxed);
        bean.setLongBoxed(longBoxed);

        env.sendEventBean(bean);

        EventBean theEvent = env.listener("s0").assertOneGetNewAndReset();
        assertEquals(result, theEvent.get("result"));
    }

    private static void sendAndAssert(RegressionEnvironment env, int intPrimitive, int shortBoxed, Long longBoxed, Boolean result) {
        SupportBean bean = new SupportBean();
        bean.setIntPrimitive(intPrimitive);
        bean.setShortBoxed((short) shortBoxed);
        bean.setLongBoxed(longBoxed);

        env.sendEventBean(bean);

        EventBean theEvent = env.listener("s0").assertOneGetNewAndReset();
        assertEquals(result, theEvent.get("result"));
    }

    private static void sendAndAssert(RegressionEnvironment env, Integer intBoxed, Float floatBoxed, double doublePrimitve, Long longBoxed, Boolean result) {
        SupportBean bean = new SupportBean();
        bean.setIntBoxed(intBoxed);
        bean.setFloatBoxed(floatBoxed);
        bean.setDoublePrimitive(doublePrimitve);
        bean.setLongBoxed(longBoxed);

        env.sendEventBean(bean);

        EventBean theEvent = env.listener("s0").assertOneGetNewAndReset();
        assertEquals(result, theEvent.get("result"));
    }

    private static void tryInBoolean(RegressionEnvironment env, String expr, Boolean[] input, boolean[] result) {
        String epl = "@name('s0') select " + expr + " as result from " + SupportBean.class.getSimpleName();
        env.compileDeploy(epl).addListener("s0");
        assertEquals(Boolean.class, env.statement("s0").getEventType().getPropertyType("result"));

        for (int i = 0; i < input.length; i++) {
            sendSupportBeanEvent(env, input[i]);
            EventBean theEvent = env.listener("s0").assertOneGetNewAndReset();
            assertEquals("Wrong result for " + input[i], result[i], theEvent.get("result"));
        }
        env.undeployAll();
    }

    private static void tryString(RegressionEnvironment env, String expression, String[] input, Boolean[] result) {
        String epl = "@name('s0') select " + expression + " as result from " + SupportBean.class.getSimpleName();
        env.compileDeploy(epl).addListener("s0");

        assertEquals(Boolean.class, env.statement("s0").getEventType().getPropertyType("result"));

        for (int i = 0; i < input.length; i++) {
            sendSupportBeanEvent(env, input[i]);
            EventBean theEvent = env.listener("s0").assertOneGetNewAndReset();
            assertEquals("Wrong result for " + input[i], result[i], theEvent.get("result"));
        }
        env.undeployAll();
    }

    private static void tryString(RegressionEnvironment env, EPStatementObjectModel model, String epl, String[] input, Boolean[] result) {
        EPCompiled compiled = env.compile(model, new CompilerArguments(env.getConfiguration()));
        assertEquals(epl, model.toEPL());

        EPStatementObjectModel objectmodel = env.eplToModel(epl);
        objectmodel = SerializableObjectCopier.copyMayFail(objectmodel);
        assertEquals(epl, objectmodel.toEPL());

        env.deploy(compiled).addListener("s0");

        assertEquals(Boolean.class, env.statement("s0").getEventType().getPropertyType("result"));

        for (int i = 0; i < input.length; i++) {
            sendSupportBeanEvent(env, input[i]);
            EventBean theEvent = env.listener("s0").assertOneGetNewAndReset();
            assertEquals("Wrong result for " + input[i], result[i], theEvent.get("result"));
        }
        env.undeployAll();
    }

    private static void tryNumeric(RegressionEnvironment env, String expr, Double[] input, Boolean[] result) {
        String epl = "@name('s0') select " + expr + " as result from SupportBean";
        env.compileDeploy(epl).addListener("s0");

        assertEquals(Boolean.class, env.statement("s0").getEventType().getPropertyType("result"));

        for (int i = 0; i < input.length; i++) {
            sendSupportBeanEvent(env, input[i]);
            EventBean theEvent = env.listener("s0").assertOneGetNewAndReset();
            assertEquals("Wrong result for " + input[i], result[i], theEvent.get("result"));
        }

        env.undeployAll();
    }

    private static void sendArrayCollMap(RegressionEnvironment env, SupportBeanArrayCollMap event) {
        env.sendEventBean(event);
    }

    private static void sendSupportBeanEvent(RegressionEnvironment env, Double doubleBoxed) {
        SupportBean theEvent = new SupportBean();
        theEvent.setDoubleBoxed(doubleBoxed);
        env.sendEventBean(theEvent);
    }

    private static void sendSupportBeanEvent(RegressionEnvironment env, String theString) {
        SupportBean theEvent = new SupportBean();
        theEvent.setTheString(theString);
        env.sendEventBean(theEvent);
    }

    private static void sendSupportBeanEvent(RegressionEnvironment env, boolean boolBoxed) {
        SupportBean theEvent = new SupportBean();
        theEvent.setBoolBoxed(boolBoxed);
        env.sendEventBean(theEvent);
    }
}
