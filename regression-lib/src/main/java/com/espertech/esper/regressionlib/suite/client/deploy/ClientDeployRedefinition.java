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
package com.espertech.esper.regressionlib.suite.client.deploy;

import com.espertech.esper.common.client.EPCompiled;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.support.filter.SupportFilterHelper;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class ClientDeployRedefinition {

    public static List<RegressionExecution> executions() {
        List<RegressionExecution> execs = new ArrayList<>();
        execs.add(new ClientDeployRedefinitionCreateSchemaNamedWindowInsert());
        execs.add(new ClientDeployRedefinitionNamedWindow());
        execs.add(new ClientDeployRedefinitionInsertInto());
        execs.add(new ClientDeployRedefinitionVariables());
        return execs;
    }

    private static class ClientDeployRedefinitionCreateSchemaNamedWindowInsert implements RegressionExecution {
        public void run(RegressionEnvironment env) {

            String text = "module test.test1;\n" +
                "create schema MyTypeOne(col1 string, col2 int);" +
                "create window MyWindowOne#keepall as select * from MyTypeOne;" +
                "insert into MyWindowOne select * from MyTypeOne;";
            env.compileDeploy(text).undeployAll();
            env.compileDeploy(text).undeployAll();
            text = "module test.test1;\n" +
                "create schema MyTypeOne(col1 string, col2 int, col3 long);" +
                "create window MyWindowOne#keepall as select * from MyTypeOne;" +
                "insert into MyWindowOne select * from MyTypeOne;";
            env.compileDeploy(text).undeployAll();
            assertEquals(0, SupportFilterHelper.getFilterCountApprox(env));

            // test on-merge
            String moduleString =
                "@Name('S0') create window MyWindow#unique(intPrimitive) as SupportBean;\n" +
                    "@Name('S1') on MyWindow insert into SecondStream select *;\n" +
                    "@Name('S2') on SecondStream merge MyWindow when matched then insert into ThirdStream select * then delete\n";
            EPCompiled compiled = env.compile(moduleString);
            env.deploy(compiled).undeployAll().deploy(compiled).undeployAll();

            // test table
            String moduleTableOne = "create table MyTable(c0 string, c1 string)";
            env.compileDeploy(moduleTableOne).undeployAll();
            String moduleTableTwo = "create table MyTable(c0 string, c1 string, c2 string)";
            env.compileDeploy(moduleTableTwo).undeployAll();
        }
    }

    private static class ClientDeployRedefinitionNamedWindow implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            env.compileDeploy("create window MyWindow#time(30) as (col1 int, col2 string)");
            env.compileDeploy("create window MyWindow#time(30) as (col1 short, col2 long)");
            env.undeployAll();
        }
    }

    private static class ClientDeployRedefinitionInsertInto implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            env.compileDeploy("create schema MySchema (col1 int, col2 string);"
                + "insert into MyStream select * from MySchema;");
            env.compileDeploy("create schema MySchema (col1 short, col2 long);"
                + "insert into MyStream select * from MySchema;");
            env.undeployAll();
        }
    }

    private static class ClientDeployRedefinitionVariables implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            env.compileDeploy("create variable int MyVar;"
                + "create schema MySchema (col1 short, col2 long);"
                + "select MyVar from MySchema;");
            env.compileDeploy("create variable string MyVar;"
                + "create schema MySchema (col1 short, col2 long);"
                + "select MyVar from MySchema;");
            env.undeployAll();
        }
    }
}

