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
package com.espertech.esper.regressionlib.suite.client.stage;

import com.espertech.esper.common.client.EPCompiled;
import com.espertech.esper.common.client.EPException;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.RegressionPath;
import com.espertech.esper.regressionlib.framework.SupportMessageAssertUtil;

import java.util.ArrayList;
import java.util.List;

import static com.espertech.esper.regressionlib.framework.SupportMessageAssertUtil.tryInvalidDeploy;
import static com.espertech.esper.regressionlib.support.stage.SupportStageUtil.stageIt;
import static com.espertech.esper.regressionlib.support.stage.SupportStageUtil.unstageIt;
import static org.junit.Assert.fail;

public class ClientStageObjectResolution {

    private final static String EPL_NAMED_WINDOW = "@public create window MyWindow#keepall as SupportBean;\n";
    private final static String EPL_CONTEXT = "@public create context MyContext initiated by SupportBean_S0;\n";
    private final static String EPL_VARIABLE = "@public create variable int MyVariable;\n";
    private final static String EPL_EVENT_TYPE = "@public create schema MyEvent();\n";
    private final static String EPL_TABLE = "@public create table MyTable(k string);\n";
    private final static String EPL_EXPRESSION = "@public create expression MyExpression {1};\n";
    private final static String EPL_SCRIPT = "@public create expression MyScript(params)[ ];\n";

    private final static String EPL_OBJECTS = "@name('eplobjects') " +
        EPL_NAMED_WINDOW +
        EPL_CONTEXT +
        EPL_VARIABLE +
        EPL_EVENT_TYPE +
        EPL_TABLE +
        EPL_EXPRESSION +
        EPL_SCRIPT;

    public static List<RegressionExecution> executions() {
        List<RegressionExecution> execs = new ArrayList<>();
        execs.add(new ClientStageObjectResolutionAfterStaging());
        execs.add(new ClientStageObjectAlreadyExists());
        return execs;
    }

    private static class ClientStageObjectAlreadyExists implements ClientStageRegressionExecution {
        public void run(RegressionEnvironment env) {
            EPCompiled compiled = env.compile(EPL_OBJECTS);
            env.deploy(compiled);

            String idCreate = env.deploymentId("eplobjects");
            env.stageService().getStage("S1");
            stageIt(env, "S1", idCreate);

            tryInvalidDeploy(env, env.compile(EPL_NAMED_WINDOW),
                "A precondition is not satisfied: named window by name 'MyWindow' is already defined by stage 'S1'");
            tryInvalidDeploy(env, env.compile(EPL_CONTEXT),
                "A precondition is not satisfied: context by name 'MyContext' is already defined by stage 'S1'");
            tryInvalidDeploy(env, env.compile(EPL_VARIABLE),
                "A precondition is not satisfied: variable by name 'MyVariable' is already defined by stage 'S1'");
            tryInvalidDeploy(env, env.compile(EPL_EVENT_TYPE),
                "A precondition is not satisfied: event type by name 'MyEvent' is already defined by stage 'S1'");
            tryInvalidDeploy(env, env.compile(EPL_TABLE),
                "A precondition is not satisfied: event type by name 'table_internal_MyTable' is already defined by stage 'S1'");
            tryInvalidDeploy(env, env.compile(EPL_EXPRESSION),
                "A precondition is not satisfied: expression by name 'MyExpression' is already defined by stage 'S1'");
            tryInvalidDeploy(env, env.compile(EPL_SCRIPT),
                "A precondition is not satisfied: script by name 'MyScript (1 parameters)' is already defined by stage 'S1'");

            unstageIt(env, "S1", idCreate);

            env.undeployAll();
        }
    }

    private static class ClientStageObjectResolutionAfterStaging implements ClientStageRegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            env.compileDeploy(EPL_OBJECTS, path);

            String idCreate = env.deploymentId("eplobjects");
            env.stageService().getStage("S1");
            stageIt(env, "S1", idCreate);

            String eplNamedWindow = "select * from MyWindow";
            tryInvalidDeploy(env, path, eplNamedWindow, "A precondition is not satisfied: Required dependency named window 'MyWindow' cannot be found");
            tryInvalidFAF(env, path, eplNamedWindow, "Failed to resolve path named window 'MyWindow'");

            String eplContext = "context MyContext select count(*) from SupportBean";
            tryInvalidDeploy(env, path, eplContext, "A precondition is not satisfied: Required dependency context 'MyContext' cannot be found");

            String eplVariable = "select MyVariable from SupportBean";
            tryInvalidDeploy(env, path, eplVariable, "A precondition is not satisfied: Required dependency variable 'MyVariable' cannot be found");

            String eplEventType = "select * from MyEvent";
            tryInvalidDeploy(env, path, eplEventType, "A precondition is not satisfied: Required dependency event type 'MyEvent' cannot be found");

            String eplTable = "select MyTable from SupportBean";
            tryInvalidDeploy(env, path, eplTable, "A precondition is not satisfied: Required dependency table 'MyTable' cannot be found");

            String eplExpression = "select MyExpression from SupportBean";
            tryInvalidDeploy(env, path, eplExpression, "A precondition is not satisfied: Required dependency declared-expression 'MyExpression' cannot be found");

            String eplScript = "select MyScript(theString) from SupportBean";
            tryInvalidDeploy(env, path, eplScript, "A precondition is not satisfied: Required dependency script 'MyScript' cannot be found");

            unstageIt(env, "S1", idCreate);

            env.compileDeploy(eplNamedWindow, path);
            env.compileExecuteFAF(eplNamedWindow, path);
            env.compileDeploy(eplContext, path);

            env.undeployAll();
        }

    }

    private static void tryInvalidFAF(RegressionEnvironment env, RegressionPath path, String query, String expected) {
        EPCompiled compiled = env.compileFAF(query, path);
        try {
            env.runtime().getFireAndForgetService().executeQuery(compiled);
            fail();
        } catch (EPException ex) {
            SupportMessageAssertUtil.assertMessage(ex.getMessage(), expected);
        }
    }
}
