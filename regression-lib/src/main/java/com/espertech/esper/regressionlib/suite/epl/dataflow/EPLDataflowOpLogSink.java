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
package com.espertech.esper.regressionlib.suite.epl.dataflow;

import com.espertech.esper.common.client.dataflow.core.EPDataFlowEmitterOperator;
import com.espertech.esper.common.client.dataflow.core.EPDataFlowInstance;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.common.internal.support.SupportBean;

import static com.espertech.esper.regressionlib.framework.SupportMessageAssertUtil.tryInvalidCompile;

public class EPLDataflowOpLogSink implements RegressionExecution {
    public void run(RegressionEnvironment env) {
        runAssertion(env, null, null, null, null, null);
        runAssertion(env, "summary", true, null, null, null);
        runAssertion(env, "xml", true, null, null, null);
        runAssertion(env, "json", true, null, null, null);
        runAssertion(env, "summary", false, null, null, null);
        runAssertion(env, "summary", true, "dataflow:%df port:%p instanceId:%i title:%t event:%e", "mytitle", null);
        runAssertion(env, "xml", true, null, null, false);
        runAssertion(env, "json", true, null, "JSON_HERE", true);

        // invalid: output stream
        tryInvalidCompile(env, "create dataflow DF1 LogSink -> s1 {}",
            "Failed to obtain operator 'LogSink': LogSink operator does not provide an output stream");

        String docSmple = "@name('flow') create dataflow MyDataFlow\n" +
            "  BeaconSource -> instream {}\n" +
            "  // Output textual event to log using defaults.\n" +
            "  LogSink(instream) {}\n" +
            "  \n" +
            "  // Output JSON-formatted to console.\n" +
            "  LogSink(instream) {\n" +
            "    format : 'json',\n" +
            "    layout : '%t [%e]',\n" +
            "    log : false,\n" +
            "    linefeed : true,\n" +
            "    title : 'My Custom Title:'\n" +
            "  }";
        env.compileDeploy(docSmple);
        env.runtime().getDataFlowService().instantiate(env.deploymentId("flow"), "MyDataFlow");

        env.undeployAll();
    }

    private void runAssertion(RegressionEnvironment env, String format, Boolean log, String layout, String title, Boolean linefeed) {

        String graph = "@name('flow') create dataflow MyConsoleOut\n" +
            "Emitter -> instream<SupportBean>{name : 'e1'}\n" +
            "LogSink(instream) {\n" +
            (format == null ? "" : "  format: '" + format + "',\n") +
            (log == null ? "" : "  log: " + log + ",\n") +
            (layout == null ? "" : "  layout: '" + layout + "',\n") +
            (title == null ? "" : "  title: '" + title + "',\n") +
            (linefeed == null ? "" : "  linefeed: " + linefeed + ",\n") +
            "}";
        env.compileDeploy(graph);

        EPDataFlowInstance instance = env.runtime().getDataFlowService().instantiate(env.deploymentId("flow"), "MyConsoleOut");

        EPDataFlowEmitterOperator emitterOp = instance.startCaptive().getEmitters().get("e1");
        emitterOp.submit(new SupportBean("E1", 1));

        instance.cancel();
        env.undeployAll();
    }
}
