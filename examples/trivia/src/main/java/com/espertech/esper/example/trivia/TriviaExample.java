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
package com.espertech.esper.example.trivia;

import com.espertech.esper.common.client.EPCompiled;
import com.espertech.esper.common.client.configuration.Configuration;
import com.espertech.esper.common.client.module.Module;
import com.espertech.esper.common.client.util.EventTypeBusModifier;
import com.espertech.esper.compiler.client.CompilerArguments;
import com.espertech.esper.compiler.client.EPCompiler;
import com.espertech.esper.compiler.client.EPCompilerProvider;
import com.espertech.esper.runtime.client.DeploymentOptions;
import com.espertech.esper.runtime.client.EPRuntime;
import com.espertech.esper.runtime.client.EPRuntimeProvider;

import java.io.InputStream;

public class TriviaExample {

    public EPRuntime setup() {

        Configuration config = new Configuration();
        config.getRuntime().getExecution().setPrioritized(true);
        config.getRuntime().getThreading().setInternalTimerEnabled(false);
        config.getCompiler().getByteCode().setBusModifierEventType(EventTypeBusModifier.BUS);
        config.getCompiler().getByteCode().setAccessModifiersPublic();

        EPRuntime runtime = EPRuntimeProvider.getDefaultRuntime(config);
        runtime.initialize();

        // Resolve "trivia.epl" file.
        InputStream inputFile = this.getClass().getClassLoader().getResourceAsStream("trivia.epl");
        if (inputFile == null) {
            inputFile = this.getClass().getClassLoader().getResourceAsStream("etc/trivia.epl");
        }
        if (inputFile == null) {
            throw new RuntimeException("Failed to find file 'trivia.epl' in classpath or relative to classpath");
        }

        try {
            // read module
            EPCompiler compiler = EPCompilerProvider.getCompiler();
            Module module = compiler.readModule(inputFile, "trivia.epl");

            CompilerArguments args = new CompilerArguments(config);
            EPCompiled compiled = compiler.compile(module, args);

            // set deployment id to 'trivia'
            runtime.getDeploymentService().deploy(compiled, new DeploymentOptions().setDeploymentId("trivia"));
        } catch (Exception e) {
            throw new RuntimeException("Error compiling and deploying EPL from 'trivia.epl': " + e.getMessage(), e);
        }

        return runtime;
    }
}

