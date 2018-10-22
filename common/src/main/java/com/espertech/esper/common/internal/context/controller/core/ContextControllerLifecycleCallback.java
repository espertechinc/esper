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
package com.espertech.esper.common.internal.context.controller.core;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.context.ContextPartitionSelector;
import com.espertech.esper.common.internal.collection.IntSeqKey;
import com.espertech.esper.common.internal.context.mgr.ContextPartitionInstantiationResult;
import com.espertech.esper.common.internal.context.mgr.ContextPartitionVisitor;
import com.espertech.esper.common.internal.context.util.AgentInstance;

import java.util.List;
import java.util.Map;

public interface ContextControllerLifecycleCallback {
    ContextPartitionInstantiationResult contextPartitionInstantiate(IntSeqKey controllerPathId,
                                                                    int subpathId,
                                                                    ContextController originator,
                                                                    EventBean optionalTriggeringEvent,
                                                                    Map<String, Object> optionalTriggeringPattern, Object[] parentPartitionKeys, Object partitionKey);

    void contextPartitionTerminate(IntSeqKey controllerPath,
                                   int subpathIdOrCPId,
                                   ContextController originator,
                                   Map<String, Object> terminationProperties,
                                   boolean leaveLocksAcquired,
                                   List<AgentInstance> agentInstancesLocksHeld);

    void contextPartitionRecursiveVisit(IntSeqKey controllerPath,
                                        int subpathId,
                                        ContextController originator,
                                        ContextPartitionVisitor visitor,
                                        ContextPartitionSelector[] selectorPerLevel);
}
