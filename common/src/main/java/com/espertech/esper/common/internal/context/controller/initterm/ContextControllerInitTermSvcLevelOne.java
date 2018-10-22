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
import com.espertech.esper.common.internal.collection.IntSeqKeyOne;
import com.espertech.esper.common.internal.context.controller.condition.ContextControllerConditionNonHA;

import java.util.*;
import java.util.function.BiConsumer;

public class ContextControllerInitTermSvcLevelOne implements ContextControllerInitTermSvc {
    private final static Object[] EMPTY_PARENT_PARTITION_KEYS = new Object[0];

    private int currentSubpath;
    private ContextControllerConditionNonHA startCondition;

    private Map<Integer, ContextControllerInitTermSvcEntry> endConditions = new HashMap<>();

    public void mgmtCreate(IntSeqKey controllerPath, Object[] parentPartitionKeys) {
        // non-nested we do no care
    }

    public Object[] mgmtGetParentPartitionKeys(IntSeqKey controllerPath) {
        return EMPTY_PARENT_PARTITION_KEYS;
    }

    public ContextControllerConditionNonHA mgmtDelete(IntSeqKey controllerPath) {
        ContextControllerConditionNonHA tmp = startCondition;
        startCondition = null;
        return tmp;
    }

    public ContextControllerConditionNonHA mgmtUpdClearStartCondition(IntSeqKey controllerPath) {
        ContextControllerConditionNonHA tmp = startCondition;
        startCondition = null;
        return tmp;
    }

    public void mgmtUpdSetStartCondition(IntSeqKey controllerPath, ContextControllerConditionNonHA startCondition) {
        this.startCondition = startCondition;
    }

    public int mgmtUpdIncSubpath(IntSeqKey controllerPath) {
        return currentSubpath++;
    }

    public void endCreate(IntSeqKey endConditionPath, int subpathIdOrCPId, ContextControllerConditionNonHA endCondition, ContextControllerInitTermPartitionKey partitionKey) {
        endConditions.put(((IntSeqKeyOne) endConditionPath).getOne(), new ContextControllerInitTermSvcEntry(subpathIdOrCPId, endCondition, partitionKey));
    }

    public ContextControllerInitTermSvcEntry endDelete(IntSeqKey conditionPath) {
        return endConditions.remove(((IntSeqKeyOne) conditionPath).getOne());
    }

    public Collection<ContextControllerInitTermSvcEntry> endDeleteByParentPath(IntSeqKey controllerPath) {
        List<ContextControllerInitTermSvcEntry> entries = new ArrayList<>(endConditions.values());
        endConditions.clear();
        return entries;
    }

    public void endVisit(IntSeqKey controllerPath, BiConsumer<ContextControllerInitTermPartitionKey, Integer> partKeyAndCPId) {
        for (Map.Entry<Integer, ContextControllerInitTermSvcEntry> entry : endConditions.entrySet()) {
            partKeyAndCPId.accept(entry.getValue().getPartitionKey(), entry.getValue().getSubpathIdOrCPId());
        }
    }

    public void destroy() {
        currentSubpath = 0;
        startCondition = null;
        endConditions = null;
    }
}
