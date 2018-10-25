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

import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.common.client.soda.EPStatementObjectModel;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.SupportMessageAssertUtil;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.regressionlib.support.bean.SupportBeanArrayCollMap;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import static org.junit.Assert.assertEquals;

public class ExprCoreAnyAllSome {

    public static Collection<RegressionExecution> executions() {
        ArrayList<RegressionExecution> executions = new ArrayList<>();
        executions.add(new ExprCoreAnyAllSomeEqualsAll());
        executions.add(new ExprCoreEqualsAllArray());
        executions.add(new ExprCoreEqualsAny());
        executions.add(new ExprCoreEqualsAnyBigInt());
        executions.add(new ExprCoreEqualsAnyArray());
        executions.add(new ExprCoreRelationalOpAllArray());
        executions.add(new ExprCoreRelationalOpNullOrNoRows());
        executions.add(new ExprCoreRelationalOpAnyArray());
        executions.add(new ExprCoreRelationalOpAll());
        executions.add(new ExprCoreRelationalOpAny());
        executions.add(new ExprCoreEqualsInNullOrNoRows());
        executions.add(new ExprCoreAnyAllSomeInvalid());
        return executions;
    }

    private static class ExprCoreAnyAllSomeEqualsAll implements RegressionExecution {

        public void run(RegressionEnvironment env) {
            String[] fields = "eq,neq,sqlneq,nneq".split(",");
            String epl = "@name('s0') select " +
                "intPrimitive=all(1,intBoxed) as eq, " +
                "intPrimitive!=all(1,intBoxed) as neq, " +
                "intPrimitive<>all(1,intBoxed) as sqlneq, " +
                "not intPrimitive=all(1,intBoxed) as nneq " +
                "from SupportBean(theString like \"E%\")";
            env.compileDeploy(epl).addListener("s0");

            // in the format intPrimitive, intBoxed
            int[][] testdata = {
                {1, 1},
                {1, 2},
                {2, 2},
                {2, 1},
            };

            Object[][] result = {
                {true, false, false, false}, // 1, 1
                {false, false, false, true}, // 1, 2
                {false, false, false, true}, // 2, 2
                {false, true, true, true}    // 2, 1
            };

            for (int i = 0; i < testdata.length; i++) {
                SupportBean bean = new SupportBean("E", testdata[i][0]);
                bean.setIntBoxed(testdata[i][1]);
                env.sendEventBean(bean);
                //System.out.println("line " + i);
                EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, result[i]);
            }

            env.undeployAll();

            // test OM
            EPStatementObjectModel model = env.eplToModel(epl);
            assertEquals(epl.replace("<>", "!="), model.toEPL());
            env.compileDeploy(model).addListener("s0");

            for (int i = 0; i < testdata.length; i++) {
                SupportBean bean = new SupportBean("E", testdata[i][0]);
                bean.setIntBoxed(testdata[i][1]);
                env.sendEventBean(bean);
                EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, result[i]);
            }

            env.undeployAll();
        }
    }

    private static class ExprCoreEqualsAllArray implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = "e,ne".split(",");
            String epl = "@name('s0') select " +
                "longBoxed = all ({1, 1}, intArr, longCol) as e, " +
                "longBoxed != all ({1, 1}, intArr, longCol) as ne " +
                "from SupportBeanArrayCollMap";
            env.compileDeploy(epl).addListener("s0");

            SupportBeanArrayCollMap arrayBean = new SupportBeanArrayCollMap(new int[]{1, 1});
            arrayBean.setLongCol(Arrays.asList(1L, 1L));
            arrayBean.setLongBoxed(1L);
            env.sendEventBean(arrayBean);

            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{true, false});

            arrayBean.setIntArr(new int[]{1, 1, 0});
            env.sendEventBean(arrayBean);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{false, false});

            arrayBean.setLongBoxed(2L);
            env.sendEventBean(arrayBean);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{false, true});

            env.undeployAll();
        }
    }

    private static class ExprCoreEqualsAny implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = "eq,neq,sqlneq,nneq".split(",");
            String epl = "@name('s0') select " +
                "intPrimitive = any (1, intBoxed) as eq, " +
                "intPrimitive != any (1, intBoxed) as neq, " +
                "intPrimitive <> any (1, intBoxed) as sqlneq, " +
                "not intPrimitive = any (1, intBoxed) as nneq " +
                " from SupportBean(theString like 'E%')";
            env.compileDeploy(epl).addListener("s0");

            // in the format intPrimitive, intBoxed
            int[][] testdata = {
                {1, 1},
                {1, 2},
                {2, 2},
                {2, 1},
            };

            Object[][] result = {
                {true, false, false, false}, // 1, 1
                {true, true, true, false}, // 1, 2
                {true, true, true, false}, // 2, 2
                {false, true, true, true} // 2, 1
            };

            for (int i = 0; i < testdata.length; i++) {
                SupportBean bean = new SupportBean("E", testdata[i][0]);
                bean.setIntBoxed(testdata[i][1]);
                env.sendEventBean(bean);
                //System.out.println("line " + i);
                EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, result[i]);
            }

            env.undeployAll();
        }
    }

    private static class ExprCoreEqualsAnyBigInt implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = "c0,c1,c2,c3".split(",");
            String epl = "@name('s0') select " +
                "bigInteger = any (null, 1) as c0," +
                "bigInteger = any (2, 3) as c1," +
                "bigDecimal = any (null, 1) as c2," +
                "bigDecimal = any (2, 3) as c3" +
                " from SupportBean";
            env.compileDeploy(epl).addListener("s0");

            SupportBean bean = new SupportBean();
            bean.setBigInteger(BigInteger.valueOf(1));
            bean.setBigDecimal(new BigDecimal(1d));
            env.sendEventBean(bean);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{true, false, true, false});

            env.undeployAll();
        }
    }

    private static class ExprCoreEqualsAnyArray implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = "e,ne".split(",");
            String epl = "@name('s0') select " +
                "longBoxed = any ({1, 1}, intArr, longCol) as e, " +
                "longBoxed != any ({1, 1}, intArr, longCol) as ne " +
                "from SupportBeanArrayCollMap";
            env.compileDeploy(epl).addListener("s0");

            SupportBeanArrayCollMap arrayBean = new SupportBeanArrayCollMap(new int[]{1, 1});
            arrayBean.setLongCol(Arrays.asList(1L, 1L));
            arrayBean.setLongBoxed(1L);
            env.sendEventBean(arrayBean);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{true, false});

            arrayBean.setIntArr(new int[]{1, 1, 0});
            env.sendEventBean(arrayBean);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{true, true});

            arrayBean.setLongBoxed(2L);
            env.sendEventBean(arrayBean);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{false, true});

            env.undeployAll();
        }
    }

    private static class ExprCoreRelationalOpAllArray implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = "g,ge".split(",");
            String epl = "@name('s0') select " +
                "longBoxed>all({1,2},intArr,intCol) as g, " +
                "longBoxed>=all({1,2},intArr,intCol) as ge " +
                "from SupportBeanArrayCollMap";
            env.compileDeploy(epl).addListener("s0");

            SupportBeanArrayCollMap arrayBean = new SupportBeanArrayCollMap(new int[]{1, 2});
            arrayBean.setIntCol(Arrays.asList(1, 2));
            arrayBean.setLongBoxed(3L);
            env.sendEventBean(arrayBean);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{true, true});

            arrayBean.setLongBoxed(2L);
            env.sendEventBean(arrayBean);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{false, true});

            arrayBean = new SupportBeanArrayCollMap(new int[]{1, 3});
            arrayBean.setIntCol(Arrays.asList(1, 2));
            arrayBean.setLongBoxed(3L);
            env.sendEventBean(arrayBean);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{false, true});

            arrayBean = new SupportBeanArrayCollMap(new int[]{1, 2});
            arrayBean.setIntCol(Arrays.asList(1, 3));
            arrayBean.setLongBoxed(3L);
            env.sendEventBean(arrayBean);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{false, true});

            env.undeployAll();

            // test OM
            EPStatementObjectModel model = env.eplToModel(epl);
            assertEquals(epl.replace("<>", "!="), model.toEPL());
            env.compileDeploy(model).addListener("s0");

            arrayBean = new SupportBeanArrayCollMap(new int[]{1, 2});
            arrayBean.setIntCol(Arrays.asList(1, 2));
            arrayBean.setLongBoxed(3L);
            env.sendEventBean(arrayBean);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{true, true});

            env.undeployAll();
        }
    }

    private static class ExprCoreRelationalOpNullOrNoRows implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            // test array
            String[] fields = "vall,vany".split(",");
            String epl;

            epl = "@name('s0') select " +
                "intBoxed >= all ({doubleBoxed, longBoxed}) as vall, " +
                "intBoxed >= any ({doubleBoxed, longBoxed}) as vany " +
                " from SupportBean(theString like 'E%')";
            env.compileDeploy(epl).addListener("s0");

            sendEvent(env, "E3", null, null, null);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{null, null});
            sendEvent(env, "E4", 1, null, null);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{null, null});

            sendEvent(env, "E5", null, 1d, null);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{null, null});
            sendEvent(env, "E6", 1, 1d, null);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{null, true});
            sendEvent(env, "E7", 0, 1d, null);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{false, false});

            env.undeployAll();

            // test fields
            fields = "vall,vany".split(",");
            epl = "@name('s0') select " +
                "intBoxed >= all (doubleBoxed, longBoxed) as vall, " +
                "intBoxed >= any (doubleBoxed, longBoxed) as vany " +
                " from SupportBean(theString like 'E%')";
            env.compileDeployAddListenerMile(epl, "s0", 1);

            sendEvent(env, "E3", null, null, null);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{null, null});
            sendEvent(env, "E4", 1, null, null);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{null, null});

            sendEvent(env, "E5", null, 1d, null);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{null, null});
            sendEvent(env, "E6", 1, 1d, null);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{null, true});
            sendEvent(env, "E7", 0, 1d, null);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{false, false});

            env.undeployAll();
        }
    }

    private static class ExprCoreRelationalOpAnyArray implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = "g,ge".split(",");
            String epl = "@name('s0') select " +
                "longBoxed > any ({1, 2}, intArr, intCol) as g, " +
                "longBoxed >= any ({1, 2}, intArr, intCol) as ge " +
                "from SupportBeanArrayCollMap";
            env.compileDeploy(epl).addListener("s0");

            SupportBeanArrayCollMap arrayBean = new SupportBeanArrayCollMap(new int[]{1, 2});
            arrayBean.setIntCol(Arrays.asList(1, 2));
            arrayBean.setLongBoxed(1L);
            env.sendEventBean(arrayBean);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{false, true});

            arrayBean.setLongBoxed(2L);
            env.sendEventBean(arrayBean);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{true, true});

            arrayBean = new SupportBeanArrayCollMap(new int[]{2, 2});
            arrayBean.setIntCol(Arrays.asList(2, 1));
            arrayBean.setLongBoxed(1L);
            env.sendEventBean(arrayBean);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{false, true});

            arrayBean = new SupportBeanArrayCollMap(new int[]{1, 1});
            arrayBean.setIntCol(Arrays.asList(1, 1));
            arrayBean.setLongBoxed(0L);
            env.sendEventBean(arrayBean);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{false, false});

            env.undeployAll();
        }
    }

    private static class ExprCoreRelationalOpAll implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = "g,ge,l,le".split(",");
            String epl = "@name('s0') select " +
                "intPrimitive > all (1, 3, 4) as g, " +
                "intPrimitive >= all (1, 3, 4) as ge, " +
                "intPrimitive < all (1, 3, 4) as l, " +
                "intPrimitive <= all (1, 3, 4) as le " +
                " from SupportBean(theString like 'E%')";
            env.compileDeploy(epl).addListener("s0");

            Object[][] result = {
                {false, false, true, true},
                {false, false, false, true},
                {false, false, false, false},
                {false, false, false, false},
                {false, true, false, false},
                {true, true, false, false}
            };

            for (int i = 0; i < 6; i++) {
                env.sendEventBean(new SupportBean("E1", i));
                EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, result[i]);
            }

            env.undeployAll();
        }
    }

    private static class ExprCoreRelationalOpAny implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = "g,ge,l,le".split(",");
            String epl = "@name('s0') select " +
                "intPrimitive > any (1, 3, 4) as g, " +
                "intPrimitive >= some (1, 3, 4) as ge, " +
                "intPrimitive < any (1, 3, 4) as l, " +
                "intPrimitive <= some (1, 3, 4) as le " +
                " from SupportBean(theString like 'E%')";
            env.compileDeploy(epl).addListener("s0");

            Object[][] result = {
                {false, false, true, true},
                {false, true, true, true},
                {true, true, true, true},
                {true, true, true, true},
                {true, true, false, true},
                {true, true, false, false}
            };

            for (int i = 0; i < 6; i++) {
                env.sendEventBean(new SupportBean("E1", i));
                EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, result[i]);
            }

            env.undeployAll();
        }
    }

    private static class ExprCoreEqualsInNullOrNoRows implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            // test fixed array case
            String[] fields;
            String epl;

            fields = "eall,eany,neall,neany,isin".split(",");
            epl = "@name('s0') select " +
                "intBoxed = all ({doubleBoxed, longBoxed}) as eall, " +
                "intBoxed = any ({doubleBoxed, longBoxed}) as eany, " +
                "intBoxed != all ({doubleBoxed, longBoxed}) as neall, " +
                "intBoxed != any ({doubleBoxed, longBoxed}) as neany, " +
                "intBoxed in ({doubleBoxed, longBoxed}) as isin " +
                " from SupportBean";
            env.compileDeploy(epl).addListener("s0");

            sendEvent(env, "E3", null, null, null);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{null, null, null, null, null});
            sendEvent(env, "E4", 1, null, null);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{null, null, null, null, null});

            sendEvent(env, "E5", null, null, 1L);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{null, null, null, null, null});
            sendEvent(env, "E6", 1, null, 1L);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{null, true, false, null, true});
            sendEvent(env, "E7", 0, null, 1L);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{false, null, null, true, null});

            env.undeployAll();

            // test non-array case
            fields = "eall,eany,neall,neany,isin".split(",");
            epl = "@name('s0') select " +
                "intBoxed = all (doubleBoxed, longBoxed) as eall, " +
                "intBoxed = any (doubleBoxed, longBoxed) as eany, " +
                "intBoxed != all (doubleBoxed, longBoxed) as neall, " +
                "intBoxed != any (doubleBoxed, longBoxed) as neany, " +
                "intBoxed in (doubleBoxed, longBoxed) as isin " +
                " from SupportBean";
            env.compileDeployAddListenerMile(epl, "s0", 1);

            sendEvent(env, "E3", null, null, null);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{null, null, null, null, null});
            sendEvent(env, "E4", 1, null, null);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{null, null, null, null, null});

            sendEvent(env, "E5", null, null, 1L);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{null, null, null, null, null});
            sendEvent(env, "E6", 1, null, 1L);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{null, true, false, null, true});
            sendEvent(env, "E7", 0, null, 1L);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{false, null, null, true, null});

            env.undeployAll();
        }
    }

    private static class ExprCoreAnyAllSomeInvalid implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            SupportMessageAssertUtil.tryInvalidCompile(env, "select intArr = all (1, 2, 3) as r1 from SupportBeanArrayCollMap",
                "Failed to validate select-clause expression 'intArr=all(1,2,3)': Collection or array comparison is not allowed for the IN, ANY, SOME or ALL keywords");
            SupportMessageAssertUtil.tryInvalidCompile(env, "select intArr > all (1, 2, 3) as r1 from SupportBeanArrayCollMap",
                "Failed to validate select-clause expression 'intArr>all(1,2,3)': Collection or array comparison is not allowed for the IN, ANY, SOME or ALL keywords");
        }
    }

    private static void sendEvent(RegressionEnvironment env, String theString, Integer intBoxed, Double doubleBoxed, Long longBoxed) {
        SupportBean bean = new SupportBean(theString, -1);
        bean.setIntBoxed(intBoxed);
        bean.setDoubleBoxed(doubleBoxed);
        bean.setLongBoxed(longBoxed);
        env.sendEventBean(bean);
    }
}
