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
import com.espertech.esper.common.client.soda.EPStatementObjectModel;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.compiler.client.CompilerArguments;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.RegressionPath;
import com.espertech.esper.regressionlib.support.client.SupportPortableDeploySubstitutionParams;
import com.espertech.esper.runtime.client.DeploymentOptions;
import com.espertech.esper.runtime.client.option.StatementSubstitutionParameterOption;

import java.util.ArrayList;
import java.util.List;

import static com.espertech.esper.regressionlib.support.util.SupportAdminUtil.assertStatelessStmt;

public class EPLDatabaseNoJoinIterate {

    public static List<RegressionExecution> executions() {
        List<RegressionExecution> execs = new ArrayList<>();
        execs.add(new EPLDatabaseExpressionPoll());
        execs.add(new EPLDatabaseVariablesPoll());
        execs.add(new EPLDatabaseNullSelect());
        execs.add(new EPLDatabaseSubstitutionParameter());
        execs.add(new EPLDatabaseSQLTextParamSubquery());
        return execs;
    }

    private static class EPLDatabaseNullSelect implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') select * from sql:MyDBPlain ['select null as a from mytesttable where myint = 1']";
            env.compileDeploy(epl);

            env.assertPropsPerRowIterator("s0", new String[]{"a"}, null);

            env.undeployAll();
        }
    }

    private static class EPLDatabaseExpressionPoll implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            env.compileDeploy("@public create variable boolean queryvar_bool", path);
            env.compileDeploy("@public create variable int queryvar_int", path);
            env.compileDeploy("@public create variable int lower", path);
            env.compileDeploy("@public create variable int upper", path);
            env.compileDeploy("on SupportBean set queryvar_int=intPrimitive, queryvar_bool=boolPrimitive, lower=intPrimitive,upper=intBoxed", path);

            // Test int and singlerow
            String stmtText = "@name('s0') select myint from sql:MyDBWithTxnIso1WithReadOnly ['select myint from mytesttable where ${queryvar_int -2} = mytesttable.mybigint']";
            env.compileDeploy(stmtText, path).addListener("s0");
            assertStatelessStmt(env, "s0", false);

            env.assertPropsPerRowIteratorAnyOrder("s0", new String[]{"myint"}, null);

            sendSupportBeanEvent(env, 5);
            env.assertPropsPerRowIteratorAnyOrder("s0", new String[]{"myint"}, new Object[][]{{30}});

            env.assertListenerNotInvoked("s0");
            env.undeployModuleContaining("s0");

            // Test multi-parameter and multi-row
            stmtText = "@name('s0') select myint from sql:MyDBWithTxnIso1WithReadOnly ['select myint from mytesttable where mytesttable.mybigint between ${queryvar_int-2} and ${queryvar_int+2}'] order by myint";
            env.compileDeploy(stmtText, path);
            env.assertPropsPerRowIteratorAnyOrder("s0", new String[]{"myint"}, new Object[][]{{30}, {40}, {50}, {60}, {70}});
            env.undeployAll();
        }
    }

    private static class EPLDatabaseVariablesPoll implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            env.compileDeploy("@public create variable boolean queryvar_bool", path);
            env.compileDeploy("@public create variable int queryvar_int", path);
            env.compileDeploy("@public create variable int lower", path);
            env.compileDeploy("@public create variable int upper", path);
            env.compileDeploy("on SupportBean set queryvar_int=intPrimitive, queryvar_bool=boolPrimitive, lower=intPrimitive,upper=intBoxed", path);

            // Test int and singlerow
            String stmtText = "@name('s0') select myint from sql:MyDBWithTxnIso1WithReadOnly ['select myint from mytesttable where ${queryvar_int} = mytesttable.mybigint']";
            env.compileDeploy(stmtText, path).addListener("s0");

            env.assertPropsPerRowIteratorAnyOrder("s0", new String[]{"myint"}, null);

            sendSupportBeanEvent(env, 5);
            env.assertPropsPerRowIteratorAnyOrder("s0", new String[]{"myint"}, new Object[][]{{50}});

            env.assertListenerNotInvoked("s0");
            env.undeployModuleContaining("s0");

            // Test boolean and multirow
            stmtText = "@name('s0') select * from sql:MyDBWithTxnIso1WithReadOnly ['select mybigint, mybool from mytesttable where ${queryvar_bool} = mytesttable.mybool and myint between ${lower} and ${upper} order by mybigint']";
            env.compileDeploy(stmtText, path).addListener("s0");

            String[] fields = new String[]{"mybigint", "mybool"};
            env.assertPropsPerRowIteratorAnyOrder("s0", fields, null);

            sendSupportBeanEvent(env, true, 10, 40);
            env.assertPropsPerRowIteratorAnyOrder("s0", fields, new Object[][]{{1L, true}, {4L, true}});

            sendSupportBeanEvent(env, false, 30, 80);
            env.assertPropsPerRowIteratorAnyOrder("s0", fields, new Object[][]{{3L, false}, {5L, false}, {6L, false}});

            sendSupportBeanEvent(env, true, 20, 30);
            env.assertPropsPerRowIteratorAnyOrder("s0", fields, null);

            sendSupportBeanEvent(env, true, 20, 60);
            env.assertPropsPerRowIteratorAnyOrder("s0", fields, new Object[][]{{4L, true}});

            env.undeployAll();
        }
    }
    private static class EPLDatabaseSubstitutionParameter implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') select * from sql:MyDBPlain['select * from mytesttable where myint = ${?:myint:int}']";
            EPStatementObjectModel model = env.eplToModel(epl);
            EPCompiled compiledFromSODA = env.compile(model, new CompilerArguments().setConfiguration(env.getConfiguration()));
            EPCompiled compiled = env.compile(epl);

            assertDeploy(env, compiled, 10, "A");
            assertDeploy(env, compiledFromSODA, 50, "E");
            assertDeploy(env, compiled, 30, "C");
        }

        private void assertDeploy(RegressionEnvironment env, EPCompiled compiled, int myint, String expected) {
            StatementSubstitutionParameterOption values = new SupportPortableDeploySubstitutionParams().add("myint", myint);
            DeploymentOptions options = new DeploymentOptions().setStatementSubstitutionParameter(values);
            env.deploy(compiled, options);

            env.assertPropsPerRowIterator("s0", new String[] {"myvarchar"}, new Object[][] {{expected}});

            env.undeployAll();
        }
    }

    private static class EPLDatabaseSQLTextParamSubquery implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            env.compileDeploy("@public create window MyWindow#lastevent as SupportBean;\n" +
                    "on SupportBean merge MyWindow insert select *", path);

            String epl = "@name('s0') select * from sql:MyDBPlain['select * from mytesttable where myint = ${(select intPrimitive from MyWindow)}']";
            env.compileDeploy(epl, path);

            env.assertPropsPerRowIterator("s0", new String[] {"myvarchar"}, new Object[0][]);

            sendAssert(env, 30, "C");
            sendAssert(env, 10, "A");

            env.undeployAll();
        }

        private void sendAssert(RegressionEnvironment env, int intPrimitive, String expected) {
            env.sendEventBean(new SupportBean("", intPrimitive));
            env.assertPropsPerRowIterator("s0", new String[] {"myvarchar"}, new Object[][] {{expected}});
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
