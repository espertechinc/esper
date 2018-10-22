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
package com.espertech.esper.common.client.hook.expr;

/**
 * Provides expression evaluation context information in an expression.
 */
public class EPLExpressionEvaluationContext {
    private final String statementName;
    private final int contextPartitionId;
    private final String runtimeURI;
    private final Object statementUserObject;

    /**
     * Ctor - for runtime use and not for client use.
     *
     * @param statementName       the statement name
     * @param contextPartitionId  the context partition id
     * @param runtimeURI          the runtime URI
     * @param statementUserObject the statement user object or null if none was assigned
     */
    public EPLExpressionEvaluationContext(String statementName, int contextPartitionId, String runtimeURI, Object statementUserObject) {
        this.statementName = statementName;
        this.contextPartitionId = contextPartitionId;
        this.runtimeURI = runtimeURI;
        this.statementUserObject = statementUserObject;
    }

    /**
     * Returns the runtime URI
     *
     * @return runtime URI
     */
    public String getRuntimeURI() {
        return runtimeURI;
    }

    /**
     * Returns the statement name.
     *
     * @return statement name
     */
    public String getStatementName() {
        return statementName;
    }

    /**
     * Returns the context partition id
     *
     * @return context partition id
     */
    public int getContextPartitionId() {
        return contextPartitionId;
    }

    /**
     * Returns the statement user object
     *
     * @return statement user object, can be null if unassigned
     */
    public Object getStatementUserObject() {
        return statementUserObject;
    }
}
