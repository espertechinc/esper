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
package com.espertech.esper.regression.event.map;

import com.espertech.esper.client.Configuration;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.supportregression.execution.RegressionExecution;

public class ExecEventMapNestedConfigStatic implements RegressionExecution {
    public void configure(Configuration configuration) throws Exception {
        configuration.addEventType("NestedMap", ExecEventMapNestedConfigRuntime.getTestDefinition());
    }

    public void run(EPServiceProvider epService) throws Exception {
        ExecEventMapNestedConfigRuntime.runAssertion(epService);
    }
}
