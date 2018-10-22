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

public class AuditPatternInstanceKey {
    private final String runtimeURI;
    private final int statementId;
    private final int agentInstanceId;
    private final String text;

    public AuditPatternInstanceKey(String runtimeURI, int statementId, int agentInstanceId, String text) {
        this.runtimeURI = runtimeURI;
        this.statementId = statementId;
        this.agentInstanceId = agentInstanceId;
        this.text = text;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        AuditPatternInstanceKey that = (AuditPatternInstanceKey) o;

        if (statementId != that.statementId) return false;
        if (agentInstanceId != that.agentInstanceId) return false;
        if (!runtimeURI.equals(that.runtimeURI)) return false;
        return text.equals(that.text);
    }

    public int hashCode() {
        int result = runtimeURI.hashCode();
        result = 31 * result + statementId;
        result = 31 * result + agentInstanceId;
        result = 31 * result + text.hashCode();
        return result;
    }
}
