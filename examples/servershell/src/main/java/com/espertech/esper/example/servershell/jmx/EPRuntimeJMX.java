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
package com.espertech.esper.example.servershell.jmx;

import com.espertech.esper.common.client.EPCompiled;
import com.espertech.esper.compiler.client.CompilerArguments;
import com.espertech.esper.compiler.client.EPCompilerProvider;
import com.espertech.esper.runtime.client.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EPRuntimeJMX implements EPRuntimeJMXMBean {
    private final static Logger log = LoggerFactory.getLogger(EPRuntimeJMX.class);
    private EPRuntime runtime;

    public EPRuntimeJMX(EPRuntime runtime) {
        if (runtime == null) {
            throw new IllegalArgumentException("No runtime instance supplied");
        }
        this.runtime = runtime;
    }

    public void createEPL(String expression, String statementName) {
        log.info("Via Java Management JMX proxy: Creating statement '" + expression + "' named '" + statementName + "'");
        compileDeploy(expression, statementName);
    }

    public void createEPL(String expression, String statementName, UpdateListener listener) {
        log.info("Via Java Management JMX proxy: Creating statement '" + expression + "' named '" + statementName + "'");
        EPStatement stmt = compileDeploy(expression, statementName);
        stmt.addListener(listener);
    }

    public void destroy(String statementName) {
        log.info("Via Java Management JMX proxy: Destroying statement '" + statementName + "'");
        for (String deploymentId : runtime.getDeploymentService().getDeployments()) {
            EPDeployment deployment = runtime.getDeploymentService().getDeployment(deploymentId);
            for (EPStatement stmt : deployment.getStatements()) {
                if (statementName.equals(stmt.getName())) {
                    try {
                        runtime.getDeploymentService().undeploy(deploymentId);
                    } catch (EPUndeployException e) {
                        log.warn("Failed to undeploy: " + e.getMessage());
                    }
                }
            }
        }
    }

    private EPStatement compileDeploy(String expression, String statementName) {
        try {
            CompilerArguments args = new CompilerArguments(runtime.getRuntimePath());
            EPCompiled compiled = EPCompilerProvider.getCompiler().compile(expression, args);

            DeploymentOptions options = new DeploymentOptions();
            options.setStatementNameRuntime(env -> statementName);
            return runtime.getDeploymentService().deploy(compiled, options).getStatements()[0];
        } catch (Exception ex) {
            throw new RuntimeException("Failed to compile and deploy: " + ex.getMessage(), ex);
        }
    }
}
