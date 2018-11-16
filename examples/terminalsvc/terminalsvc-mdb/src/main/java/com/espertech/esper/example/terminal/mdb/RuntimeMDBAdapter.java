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
package com.espertech.esper.example.terminal.mdb;

import com.espertech.esper.common.client.EPCompiled;
import com.espertech.esper.common.client.configuration.Configuration;
import com.espertech.esper.compiler.client.CompilerArguments;
import com.espertech.esper.compiler.client.EPCompilerProvider;
import com.espertech.esper.example.terminal.common.*;
import com.espertech.esper.runtime.client.EPDeployment;
import com.espertech.esper.runtime.client.EPRuntime;
import com.espertech.esper.runtime.client.EPRuntimeProvider;
import com.espertech.esper.runtime.client.EPStatement;

public class RuntimeMDBAdapter {
    private final EPRuntime runtime;

    public RuntimeMDBAdapter(OutboundSender outboundSender) {
        Configuration config = new Configuration();
        config.getCommon().addEventType("Checkin", Checkin.class);
        config.getCommon().addEventType("Cancelled", Cancelled.class);
        config.getCommon().addEventType("Completed", Completed.class);
        config.getCommon().addEventType("Status", Status.class);
        config.getCommon().addEventType("LowPaper", LowPaper.class);
        config.getCommon().addEventType("OutOfOrder", OutOfOrder.class);
        config.getCommon().addEventType("BaseTerminalEvent", BaseTerminalEvent.class);

        // Get runtime instance - same runtime instance for all MDB instances
        runtime = EPRuntimeProvider.getDefaultRuntime(config);
        runtime.initialize(); // since this test is running as part of a larger test suite, reset to make sure its a clean runtime
        System.out.println(TerminalMDB.class.getName() + "::instance this=" + this.toString() + " runtime=" + runtime.toString());

        EPStatement statement = null;
        String stmt = null;

        stmt = "select a.term.id as terminal from pattern [ every a=Checkin -> " +
            "      ( OutOfOrder(term.id=a.term.id) and not (Cancelled(term.id=a.term.id) or Completed(term.id=a.term.id)) )]";
        statement = compileDeploy(stmt);
        statement.addListener(new CheckinProblemListener(outboundSender));

        stmt = "select * from BaseTerminalEvent where type = 'LowPaper' or type = 'OutOfOrder'";
        statement = compileDeploy(stmt);
        statement.addListener(new TerminalEventListener(outboundSender));

        stmt = "select '1' as terminal, 'terminal is offline' as text from pattern [ every timer:interval(60 seconds) -> (timer:interval(65 seconds) and not Status(term.id = 'T1')) ] output first every 5 minutes";
        statement = compileDeploy(stmt);
        statement.addListener(new TerminalStatusListener(outboundSender));

        stmt = "insert into CountPerType " +
            "select type, count(*) as countPerType " +
            "from BaseTerminalEvent#time(10 min) " +
            "group by type " +
            "output all every 1 minutes";
        statement = compileDeploy(stmt);
        statement.addListener(new CountPerTypeListener(outboundSender));
    }

    public void sendEvent(Object theEvent) {
        synchronized (runtime) {
            runtime.getEventService().sendEventBean(theEvent, theEvent.getClass().getSimpleName());
        }
    }

    public EPRuntime getRuntime() {
        return runtime;
    }

    private EPStatement compileDeploy(String epl) {
        try {
            CompilerArguments args = new CompilerArguments();
            args.getPath().add(runtime.getRuntimePath());

            EPCompiled compiled = EPCompilerProvider.getCompiler().compile(epl, args);
            EPDeployment deployment = runtime.getDeploymentService().deploy(compiled);
            return deployment.getStatements()[0];
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }
}
