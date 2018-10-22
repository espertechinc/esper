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
package com.espertech.esper.common.internal.metrics.audit;

import com.espertech.esper.common.client.annotation.AuditEnum;

public class AuditContext {

    private final String runtimeURI;
    private final String deploymentId;
    private final String statementName;
    private final int agentInstanceId;
    private final AuditEnum category;
    private final String message;

    public AuditContext(String runtimeURI, String deploymentId, String statementName, int agentInstanceId, AuditEnum category, String message) {
        this.runtimeURI = runtimeURI;
        this.deploymentId = deploymentId;
        this.statementName = statementName;
        this.agentInstanceId = agentInstanceId;
        this.category = category;
        this.message = message;
    }

    public String getRuntimeURI() {
        return runtimeURI;
    }

    public String getDeploymentId() {
        return deploymentId;
    }

    public String getStatementName() {
        return statementName;
    }

    public AuditEnum getCategory() {
        return category;
    }

    public int getAgentInstanceId() {
        return agentInstanceId;
    }

    public String getMessage() {
        return message;
    }

    public String format() {
        return defaultFormat(statementName, agentInstanceId, category, message);
    }

    public static String defaultFormat(String statementName, int partition, AuditEnum category, String message) {
        StringBuilder buf = new StringBuilder();
        buf.append("Statement ");
        buf.append(statementName);
        buf.append(" partition ");
        buf.append(partition);
        buf.append(" ");
        buf.append(category.getPrettyPrintText());
        buf.append(" ");
        buf.append(message);
        return buf.toString();
    }
}