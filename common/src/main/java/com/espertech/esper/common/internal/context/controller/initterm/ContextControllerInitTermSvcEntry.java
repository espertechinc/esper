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
package com.espertech.esper.common.internal.context.controller.initterm;

import com.espertech.esper.common.internal.context.controller.condition.ContextControllerConditionNonHA;

public class ContextControllerInitTermSvcEntry {
    private final int subpathIdOrCPId;
    private final ContextControllerConditionNonHA terminationCondition;
    private final ContextControllerInitTermPartitionKey partitionKey;

    public ContextControllerInitTermSvcEntry(int subpathIdOrCPId, ContextControllerConditionNonHA terminationCondition, ContextControllerInitTermPartitionKey partitionKey) {
        this.subpathIdOrCPId = subpathIdOrCPId;
        this.terminationCondition = terminationCondition;
        this.partitionKey = partitionKey;
    }

    public int getSubpathIdOrCPId() {
        return subpathIdOrCPId;
    }

    public ContextControllerConditionNonHA getTerminationCondition() {
        return terminationCondition;
    }

    public ContextControllerInitTermPartitionKey getPartitionKey() {
        return partitionKey;
    }
}
