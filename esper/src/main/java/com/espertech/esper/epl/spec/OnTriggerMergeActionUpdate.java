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

import java.util.List;

/**
 * Specification for the merge statement update-part.
 */
public class OnTriggerMergeActionUpdate extends OnTriggerMergeAction {
    private static final long serialVersionUID = 726673263717907039L;

    private List<OnTriggerSetAssignment> assignments;

    public OnTriggerMergeActionUpdate(ExprNode optionalMatchCond, List<OnTriggerSetAssignment> assignments) {
        super(optionalMatchCond);
        this.assignments = assignments;
    }

    public List<OnTriggerSetAssignment> getAssignments() {
        return assignments;
    }
}

