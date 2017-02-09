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
package com.espertech.esper.client.deploy;

import com.espertech.esper.client.EPStatement;

import java.util.List;

/**
 * Result of a deployment operation carries a deployment id for use in undeploy and statement-level information.
 */
public class DeploymentResult {
    private final String deploymentId;
    private final List<EPStatement> statements;
    private final List<String> imports;

    /**
     * Ctor.
     *
     * @param deploymentId deployment id
     * @param statements   statements deployed and started
     * @param imports      the imports that are part of the deployment
     */
    public DeploymentResult(String deploymentId, List<EPStatement> statements, List<String> imports) {
        this.deploymentId = deploymentId;
        this.statements = statements;
        this.imports = imports;
    }

    /**
     * Returns the deployment id.
     *
     * @return id
     */
    public String getDeploymentId() {
        return deploymentId;
    }

    /**
     * Returns the statements.
     *
     * @return statements
     */
    public List<EPStatement> getStatements() {
        return statements;
    }

    /**
     * Returns a list of imports that were declared in the deployment.
     *
     * @return imports
     */
    public List<String> getImports() {
        return imports;
    }
}
