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

import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.RegressionPath;
import com.espertech.esper.common.internal.support.SupportBean;

import java.util.ArrayList;
import java.util.List;

import static com.espertech.esper.regressionlib.framework.SupportMessageAssertUtil.tryInvalidCompile;
import static com.espertech.esper.regressionlib.support.util.SupportAdminUtil.assertStatelessStmt;
import static org.junit.Assert.assertFalse;

public class EPLDatabaseNoJoinIterate {

    public static List<RegressionExecution> executions() {
        List<RegressionExecution> execs = new ArrayList<>();
        execs.add(new EPLDatabaseExpressionPoll());
        execs.add(new EPLDatabaseVariablesPoll());
        return execs;
    }

    private static class EPLDatabaseExpressionPoll implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            env.compileDeploy("create variable boolean queryvar_bool", path);
            env.compileDeploy("create variable int queryvar_int", path);
            env.compileDeploy("create variable int lower", path);
            env.compileDeploy("create variable int upper", path);
            env.compileDeploy("on SupportBean set queryvar_int=intPrimitive, queryvar_bool=boolPrimitive, lower=intPrimitive,upper=intBoxed", path);

            // Test int and singlerow
            String stmtText = "@name('s0') select myint from sql:MyDBWithTxnIso1WithReadOnly ['select myint from mytesttable where ${queryvar_int -2} = mytesttable.mybigint']";
            env.compileDeploy(stmtText, path).addListener("s0");
            assertStatelessStmt(env, "s0", false);

            EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("s0"), new String[]{"myint"}, null);

            sendSupportBeanEvent(env, 5);
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("s0"), new String[]{"myint"}, new Object[][]{{30}});

            assertFalse(env.listener("s0").isInvoked());
            env.undeployModuleContaining("s0");

            // Test multi-parameter and multi-row
            stmtText = "@name('s0') select myint from sql:MyDBWithTxnIso1WithReadOnly ['select myint from mytesttable where mytesttable.mybigint between ${queryvar_int-2} and ${queryvar_int+2}'] order by myint";
            env.compileDeploy(stmtText, path);
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("s0"), new String[]{"myint"}, new Object[][]{{30}, {40}, {50}, {60}, {70}});
            env.undeployAll();

            // Test substitution parameters
            tryInvalidCompile(env, "@name('s0') select myint from sql:MyDBWithTxnIso1WithReadOnly ['select myint from mytesttable where mytesttable.mybigint between ${?} and ${queryvar_int+?}'] order by myint",
                "EPL substitution parameters are not allowed in SQL ${...} expressions, consider using a variable instead");
        }
    }

    private static class EPLDatabaseVariablesPoll implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            env.compileDeploy("create variable boolean queryvar_bool", path);
            env.compileDeploy("create variable int queryvar_int", path);
            env.compileDeploy("create variable int lower", path);
            env.compileDeploy("create variable int upper", path);
            env.compileDeploy("on SupportBean set queryvar_int=intPrimitive, queryvar_bool=boolPrimitive, lower=intPrimitive,upper=intBoxed", path);

            // Test int and singlerow
            String stmtText = "@name('s0') select myint from sql:MyDBWithTxnIso1WithReadOnly ['select myint from mytesttable where ${queryvar_int} = mytesttable.mybigint']";
            env.compileDeploy(stmtText, path).addListener("s0");

            EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("s0"), new String[]{"myint"}, null);

            sendSupportBeanEvent(env, 5);
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("s0"), new String[]{"myint"}, new Object[][]{{50}});

            assertFalse(env.listener("s0").isInvoked());
            env.undeployModuleContaining("s0");

            // Test boolean and multirow
            stmtText = "@name('s0') select * from sql:MyDBWithTxnIso1WithReadOnly ['select mybigint, mybool from mytesttable where ${queryvar_bool} = mytesttable.mybool and myint between ${lower} and ${upper} order by mybigint']";
            env.compileDeploy(stmtText, path).addListener("s0");

            String[] fields = new String[]{"mybigint", "mybool"};
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("s0"), fields, null);

            sendSupportBeanEvent(env, true, 10, 40);
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("s0"), fields, new Object[][]{{1L, true}, {4L, true}});

            sendSupportBeanEvent(env, false, 30, 80);
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("s0"), fields, new Object[][]{{3L, false}, {5L, false}, {6L, false}});

            sendSupportBeanEvent(env, true, 20, 30);
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("s0"), fields, null);

            sendSupportBeanEvent(env, true, 20, 60);
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("s0"), fields, new Object[][]{{4L, true}});

            env.undeployAll();
        }
    }

    private static void sendSupportBeanEvent(RegressionEnvironment env, int intPrimitive) {
        SupportBean bean = new SupportBean();
        bean.setIntPrimitive(intPrimitive);
        env.sendEventBean(bean);
    }

    private static void sendSupportBeanEvent(RegressionEnvironment env, boolean boolPrimitive, int intPrimitive, int intBoxed) {
        SupportBean bean = new SupportBean();
        bean.setBoolPrimitive(boolPrimitive);
        bean.setIntPrimitive(intPrimitive);
        bean.setIntBoxed(intBoxed);
        env.sendEventBean(bean);
    }
}
