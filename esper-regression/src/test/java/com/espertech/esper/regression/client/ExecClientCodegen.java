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
package com.espertech.esper.regression.client;

import com.espertech.esper.client.Configuration;
import com.espertech.esper.client.ConfigurationEngineDefaults;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EventType;
import com.espertech.esper.supportregression.execution.RegressionExecution;

public class ExecClientCodegen implements RegressionExecution {
    public void configure(Configuration configuration) throws Exception {
        configuration.getEngineDefaults().getLogging().setEnableCode(true);

        ConfigurationEngineDefaults.ByteCodeGeneration codegen = configuration.getEngineDefaults().getByteCodeGeneration();
        codegen.setEnablePropertyGetter(true);
        codegen.setIncludeDebugSymbols(true);
        codegen.setIncludeComments(true);
        codegen.setEnableExpression(true);
        codegen.setEnableFallback(false);
    }

    public void run(EPServiceProvider epService) throws Exception {
        runAssertionLogGeneratedCode(epService);
    }

    private void runAssertionLogGeneratedCode(EPServiceProvider epService) {
        epService.getEPAdministrator().createEPL("create schema MyEvent10Props(p0 int, p1 int, p2 int, p3 int, p4 int, p5 int, p6 int, p7 int, p8 int, p9 int)");
        epService.getEPAdministrator().createEPL("select * from MyEvent10Props#lastevent where p0=0 or p1=1 or p2=2 or p3=3 or p4=4 or p5=5 or p6=6 or p7=7 or p8=8 or p9=9");
        epService.getEPAdministrator().createEPL("select p0=0 or p1=1 or p2=2 or p3=3 or p4=4 or p5=5 or p6=6 or p7=7 or p8=8 or p9=9 from MyEvent10Props");

        EventType eventType = epService.getEPAdministrator().getConfiguration().getEventType("MyEvent10Props");
        eventType.getGetter("p0");
    }
}
