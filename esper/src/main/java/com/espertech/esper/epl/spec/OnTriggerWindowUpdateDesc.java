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

import java.util.List;

/**
 * Specification for the on-select and on-delete (no split-stream) statement.
 */
public class OnTriggerWindowUpdateDesc extends OnTriggerWindowDesc {
    private static final long serialVersionUID = 3796573624109335943L;

    private List<OnTriggerSetAssignment> assignments;

    /**
     * Ctor.
     *
     * @param windowName     the window name
     * @param optionalAsName the optional name
     * @param assignments    set-assignments
     */
    public OnTriggerWindowUpdateDesc(String windowName, String optionalAsName, List<OnTriggerSetAssignment> assignments) {
        super(windowName, optionalAsName, OnTriggerType.ON_UPDATE, false);
        this.assignments = assignments;
    }

    /**
     * Returns assignments.
     *
     * @return assignments
     */
    public List<OnTriggerSetAssignment> getAssignments() {
        return assignments;
    }
}