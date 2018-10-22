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

import com.espertech.esper.common.internal.collection.IntSeqKey;
import com.espertech.esper.common.internal.context.controller.condition.ContextControllerConditionNonHA;

import java.util.Collection;
import java.util.function.BiConsumer;

public interface ContextControllerInitTermSvc {
    void mgmtCreate(IntSeqKey controllerPath, Object[] parentPartitionKeys);

    Object[] mgmtGetParentPartitionKeys(IntSeqKey controllerPath);

    int mgmtUpdIncSubpath(IntSeqKey controllerPath);

    ContextControllerConditionNonHA mgmtUpdClearStartCondition(IntSeqKey controllerPath);

    void mgmtUpdSetStartCondition(IntSeqKey controllerPath, ContextControllerConditionNonHA startCondition);

    ContextControllerConditionNonHA mgmtDelete(IntSeqKey controllerPath);

    void endCreate(IntSeqKey endConditionPath, int subpathIdOrCPId, ContextControllerConditionNonHA endCondition, ContextControllerInitTermPartitionKey partitionKey);

    Collection<ContextControllerInitTermSvcEntry> endDeleteByParentPath(IntSeqKey controllerPath);

    ContextControllerInitTermSvcEntry endDelete(IntSeqKey conditionPath);

    void endVisit(IntSeqKey controllerPath, BiConsumer<ContextControllerInitTermPartitionKey, Integer> partKeyAndCPId);

    void destroy();
}
