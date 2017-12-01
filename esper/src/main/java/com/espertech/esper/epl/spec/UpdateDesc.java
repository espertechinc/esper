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
package com.espertech.esper.epl.spec;

import com.espertech.esper.epl.expression.core.ExprNode;

import java.io.Serializable;
import java.util.List;

/**
 * Specification for the update statement.
 */
public class UpdateDesc implements Serializable {
    private final String optionalStreamName;
    private final List<OnTriggerSetAssignment> assignments;
    private ExprNode optionalWhereClause;
    private static final long serialVersionUID = -5995788555238052741L;

    /**
     * Ctor.
     *
     * @param optionalStreamName  a stream name if provided for the update
     * @param assignments         the individual assignments made
     * @param optionalWhereClause the where-clause expression if provided
     */
    public UpdateDesc(String optionalStreamName, List<OnTriggerSetAssignment> assignments, ExprNode optionalWhereClause) {
        this.optionalStreamName = optionalStreamName;
        this.assignments = assignments;
        this.optionalWhereClause = optionalWhereClause;
    }

    /**
     * Returns a list of all assignment
     *
     * @return list of assignments
     */
    public List<OnTriggerSetAssignment> getAssignments() {
        return assignments;
    }

    /**
     * Returns the stream name if defined.
     *
     * @return stream name
     */
    public String getOptionalStreamName() {
        return optionalStreamName;
    }

    /**
     * Returns the where-clause if defined.
     *
     * @return where clause
     */
    public ExprNode getOptionalWhereClause() {
        return optionalWhereClause;
    }

    /**
     * Sets the where-clause if defined.
     *
     * @param optionalWhereClause where clause to set or null
     */
    public void setOptionalWhereClause(ExprNode optionalWhereClause) {
        this.optionalWhereClause = optionalWhereClause;
    }
}
