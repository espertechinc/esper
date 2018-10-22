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
package com.espertech.esper.example.marketdatafeed;

import com.espertech.esper.common.client.EPCompiled;
import com.espertech.esper.common.client.EPCompilerPathable;
import com.espertech.esper.common.client.configuration.Configuration;
import com.espertech.esper.compiler.client.CompilerArguments;
import com.espertech.esper.compiler.client.EPCompilerProvider;
import com.espertech.esper.runtime.client.EPDeploymentService;
import com.espertech.esper.runtime.client.EPStatement;
import com.espertech.esper.runtime.client.UpdateListener;

public class TicksFalloffStatement {
    private EPStatement statement;

    public TicksFalloffStatement(EPDeploymentService admin, Configuration configuration, EPCompilerPathable epRuntimePath) {
        String stmt = "select feed, avg(cnt) as avgCnt, cnt as feedCnt from TicksPerSecond#time(10 sec) " +
            "group by feed " +
            "having cnt < avg(cnt) * 0.75 ";

        CompilerArguments args = new CompilerArguments(configuration);
        args.getPath().add(epRuntimePath);

        try {
            EPCompiled compiled = EPCompilerProvider.getCompiler().compile(stmt, args);
            statement = admin.deploy(compiled).getStatements()[0];
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    public void addListener(UpdateListener listener) {
        statement.addListener(listener);
    }
}
