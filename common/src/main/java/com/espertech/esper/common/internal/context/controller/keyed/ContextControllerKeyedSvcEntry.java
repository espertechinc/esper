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
package com.espertech.esper.common.internal.context.controller.keyed;

import com.espertech.esper.common.internal.context.controller.condition.ContextControllerConditionNonHA;

public class ContextControllerKeyedSvcEntry {
    private final int subpathOrCPId;
    private final ContextControllerConditionNonHA terminationCondition;

    public ContextControllerKeyedSvcEntry(int subpathOrCPId, ContextControllerConditionNonHA terminationCondition) {
        this.subpathOrCPId = subpathOrCPId;
        this.terminationCondition = terminationCondition;
    }

    public int getSubpathOrCPId() {
        return subpathOrCPId;
    }

    public ContextControllerConditionNonHA getTerminationCondition() {
        return terminationCondition;
    }
}
