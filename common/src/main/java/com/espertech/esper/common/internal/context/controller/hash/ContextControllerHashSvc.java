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
package com.espertech.esper.common.internal.context.controller.hash;

import com.espertech.esper.common.internal.collection.IntSeqKey;
import com.espertech.esper.common.internal.context.controller.core.ContextControllerFilterEntry;

import java.util.Collection;
import java.util.function.BiConsumer;

public interface ContextControllerHashSvc {
    void mgmtCreate(IntSeqKey controllerPath, Object[] parentPartitionKeys);

    int[] mgmtGetSubpathOrCPIdsWhenPreallocate(IntSeqKey path);

    Object[] mgmtGetParentPartitionKeys(IntSeqKey controllerPath);

    ContextControllerFilterEntry[] mgmtGetFilters(IntSeqKey controllerPath);

    void mgmtSetSubpathOrCPIdsWhenPreallocate(IntSeqKey path, int[] subpathOrCPIds);

    void mgmtSetFilters(IntSeqKey controllerPath, ContextControllerFilterEntry[] filterEntries);

    boolean hashHasSeenPartition(IntSeqKey controllerPath, int value);

    void hashAddPartition(IntSeqKey controllerPath, int value, int subpathIdOrCPId);

    void hashVisit(IntSeqKey controllerPath, BiConsumer<Integer, Integer> hashAndCPId);

    int hashGetSubpathOrCPId(IntSeqKey controllerPath, int hash);

    Collection<Integer> deactivate(IntSeqKey controllerPath);

    void destroy();
}
