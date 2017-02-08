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

public class FireAndForgetSpecUpdate extends FireAndForgetSpec {
    private static final long serialVersionUID = -2633119130798557349L;
    private final List<OnTriggerSetAssignment> assignments;

    public FireAndForgetSpecUpdate(List<OnTriggerSetAssignment> assignments) {
        this.assignments = assignments;
    }

    public List<OnTriggerSetAssignment> getAssignments() {
        return assignments;
    }
}
