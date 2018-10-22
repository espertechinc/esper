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

import com.espertech.esper.common.internal.collection.IntSeqKey;
import com.espertech.esper.common.internal.context.controller.condition.ContextControllerConditionNonHA;
import com.espertech.esper.common.internal.context.controller.core.ContextControllerFilterEntry;

import java.util.Collection;
import java.util.List;
import java.util.function.BiConsumer;

public interface ContextControllerKeyedSvc {
    void mgmtCreate(IntSeqKey controllerPath, Object[] parentPartitionKeys);

    void mgmtSetFilters(IntSeqKey controllerPath, ContextControllerFilterEntry[] filterEntries);

    int mgmtGetIncSubpath(IntSeqKey controllerPath);

    ContextControllerFilterEntry[] mgmtGetFilters(IntSeqKey controllerPath);

    Object[] mgmtGetPartitionKeys(IntSeqKey controllerPath);

    boolean keyHasSeen(IntSeqKey controllerPath, Object key);

    void keyAdd(IntSeqKey controllerPath, Object key, int subpathIdOrCPId, ContextControllerConditionNonHA terminationCondition);

    ContextControllerKeyedSvcEntry keyRemove(IntSeqKey controllerPath, Object key);

    List<ContextControllerConditionNonHA> keyGetTermConditions(IntSeqKey controllerPath);

    int keyGetSubpathOrCPId(IntSeqKey controllerPath, Object key);

    void keyVisit(IntSeqKey controllerPath, BiConsumer<Object, Integer> keyAndSubpathOrCPId);

    Collection<Integer> deactivate(IntSeqKey controllerPath);

    void destroy();
}
