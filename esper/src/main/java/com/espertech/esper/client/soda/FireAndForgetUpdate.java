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
package com.espertech.esper.client.soda;

import java.util.ArrayList;
import java.util.List;

/**
 * Fire-and-forget (on-demand) update DML.
 */
public class FireAndForgetUpdate implements FireAndForgetClause {
    private static final long serialVersionUID = 1335566236342281539L;
    private List<Assignment> assignments = new ArrayList<Assignment>();

    /**
     * Ctor.
     */
    public FireAndForgetUpdate() {
    }

    /**
     * Returns the set-assignments.
     *
     * @return assignments
     */
    public List<Assignment> getAssignments() {
        return assignments;
    }

    /**
     * Add an assignment
     *
     * @param assignment to add
     * @return assignment
     */
    public List<Assignment> addAssignment(Assignment assignment) {
        assignments.add(assignment);
        return assignments;
    }

    /**
     * Sets the assignments.
     *
     * @param assignments to set
     */
    public void setAssignments(List<Assignment> assignments) {
        this.assignments = assignments;
    }
}
