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
import com.espertech.esper.common.client.fireandforget.EPFireAndForgetQueryResult;
import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.compiler.client.CompilerArguments;
import com.espertech.esper.compiler.client.EPCompileException;
import com.espertech.esper.compiler.client.EPCompilerProvider;
import com.espertech.esper.compiler.client.option.ModuleUsesContext;
import com.espertech.esper.compiler.client.option.ModuleUsesOption;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.support.util.IndexBackingTableInfo;

import java.util.*;

public class InfraNWTableFAFResolve implements IndexBackingTableInfo {

    public static Collection<RegressionExecution> executions() {
        ArrayList<RegressionExecution> execs = new ArrayList<>();

        execs.add(new InfraSelectWildcard(true));
        execs.add(new InfraSelectWildcard(false));

        return execs;
    }

    private static class InfraSelectWildcard implements RegressionExecution {
        private final boolean namedWindow;

        private InfraSelectWildcard(boolean namedWindow) {
            this.namedWindow = namedWindow;
        }

        public void run(RegressionEnvironment env) {
            setupInfra(env, namedWindow);

            EPCompiled insertA = compileRuntimePath(env, "A", "insert into MyInfra(c0, c1) values ('A1', 10)");
            EPCompiled insertB = compileRuntimePath(env, "B", "insert into MyInfra(c2, c3) values (20, 'B1')");

            env.runtime().getFireAndForgetService().executeQuery(insertA);
            env.runtime().getFireAndForgetService().executeQuery(insertB);

            EPCompiled selectA = compileRuntimePath(env, "A", "select * from MyInfra");
            EPCompiled selectB = compileRuntimePath(env, "B", "select * from MyInfra");

            EPFireAndForgetQueryResult resultA = env.runtime().getFireAndForgetService().executeQuery(selectA);
            EPAssertionUtil.assertPropsPerRow(resultA.iterator(), "c0,c1".split(","), new Object[][]{{"A1", 10}});

            EPFireAndForgetQueryResult resultB = env.runtime().getFireAndForgetService().executeQuery(selectB);
            EPAssertionUtil.assertPropsPerRow(resultB.iterator(), "c2,c3".split(","), new Object[][]{{20, "B1"}});

            env.undeployAll();
        }
    }

    private static EPCompiled compileRuntimePath(RegressionEnvironment env, String moduleName, String query) {
        try {
            CompilerArguments args = new CompilerArguments();
            args.getOptions().setModuleUses(new ModuleUsesOption() {
                public Set<String> getValue(ModuleUsesContext env) {
                    return new HashSet<>(Arrays.asList(moduleName));
                }
            });
            args.getPath().add(env.runtime().getRuntimePath());
            return EPCompilerProvider.getCompiler().compileQuery(query, args);
        } catch (EPCompileException ex) {
            throw new RuntimeException(ex);
        }
    }

    private static void setupInfra(RegressionEnvironment env, boolean namedWindow) {
        String eplCreate = namedWindow ?
            "module A; @Name('TheInfra') @protected create window MyInfra#keepall as (c0 string, c1 int)" :
            "module A; @Name('TheInfra') @protected create table MyInfra as (c0 string primary key, c1 int primary key)";
        env.compileDeploy(eplCreate);

        eplCreate = namedWindow ?
            "module B; @Name('TheInfra') @protected create window MyInfra#keepall as (c2 int, c3 string)" :
            "module B; @Name('TheInfra') @protected create table MyInfra as (c2 int primary key, c3 string primary key)";
        env.compileDeploy(eplCreate);
    }
}
