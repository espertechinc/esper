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
import com.espertech.esper.common.client.configuration.Configuration;
import com.espertech.esper.common.client.util.NameAccessModifier;
import com.espertech.esper.compiler.client.CompilerArguments;
import com.espertech.esper.compiler.client.EPCompilerProvider;
import com.espertech.esper.runtime.client.EPDeploymentService;
import com.espertech.esper.runtime.client.EPStatement;
import com.espertech.esper.runtime.client.UpdateListener;

public class TicksPerSecondStatement {
    private EPStatement statement;

    public TicksPerSecondStatement(EPDeploymentService admin, Configuration configuration) {
        String stmt = "insert into TicksPerSecond " +
            "select feed, count(*) as cnt from MarketDataEvent#time_batch(1 sec) group by feed";
        CompilerArguments args = new CompilerArguments(configuration);
        args.getOptions().setAccessModifierEventType(env -> NameAccessModifier.PUBLIC); // export the type allowing others to refer to it
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


