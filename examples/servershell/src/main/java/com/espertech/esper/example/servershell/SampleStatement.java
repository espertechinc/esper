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
package com.espertech.esper.example.servershell;

import com.espertech.esper.common.client.EPCompiled;
import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.compiler.client.CompilerArguments;
import com.espertech.esper.compiler.client.EPCompilerProvider;
import com.espertech.esper.runtime.client.EPRuntime;
import com.espertech.esper.runtime.client.EPStatement;
import com.espertech.esper.runtime.client.UpdateListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SampleStatement {
    private final static Logger log = LoggerFactory.getLogger(SampleStatement.class);

    public static void createStatement(EPRuntime runtime) {

        String epl = "select istream ipAddress, avg(duration) from SampleEvent#time(10 sec) group by ipAddress output snapshot every 2 seconds order by ipAddress asc";
        EPStatement statement;
        try {
            CompilerArguments args = new CompilerArguments(runtime.getRuntimePath());
            EPCompiled compiled = EPCompilerProvider.getCompiler().compile(epl, args);
            statement = runtime.getDeploymentService().deploy(compiled).getStatements()[0];
        } catch (Exception ex) {
            throw new RuntimeException("Failed to compile and deploy: " + ex.getMessage(), ex);
        }

        statement.addListener(new UpdateListener() {
            public void update(EventBean[] newEvents, EventBean[] oldEvents, EPStatement statement, EPRuntime runtime) {
                if (newEvents == null) {
                    return;
                }

                for (int i = 0; i < newEvents.length; i++) {
                    if (log.isInfoEnabled()) {
                        log.info("IPAddress: " + newEvents[i].get("ipAddress") +
                            " Avg Duration: " + newEvents[i].get("avg(duration)"));
                    }
                }
            }
        });

    }
}
