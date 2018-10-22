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
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.support.autoname.two.MyAutoNameEvent;

import java.util.ArrayList;
import java.util.List;

import static com.espertech.esper.regressionlib.framework.SupportMessageAssertUtil.tryInvalidCompile;
import static org.junit.Assert.assertEquals;

public class ClientCompileEventTypeAutoName {

    public static List<RegressionExecution> executions() {
        List<RegressionExecution> execs = new ArrayList<>();
        execs.add(new ClientCompileAutoNameResolve());
        execs.add(new ClientCompileAutoNameAmbiguous());
        return execs;
    }

    public static class ClientCompileAutoNameResolve implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "create schema MANE as MyAutoNameEvent;\n" +
                "@name('s0') select p0 from MANE;\n";
            EPCompiled compiled = env.compileWBusPublicType(epl);
            env.deploy(compiled).addListener("s0");

            env.sendEventBean(new MyAutoNameEvent("test"), "MANE");
            assertEquals("test", env.listener("s0").assertOneGetNewAndReset().get("p0"));

            env.undeployAll();
        }
    }

    public static class ClientCompileAutoNameAmbiguous implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            tryInvalidCompile(env, "create schema SupportAmbigousEventType as SupportAmbigousEventType",
                "Failed to resolve name 'SupportAmbigousEventType', the class was ambigously found both in package 'com.espertech.esper.regressionlib.support.autoname.one' and in package 'com.espertech.esper.regressionlib.support.autoname.two'");
        }
    }
}
