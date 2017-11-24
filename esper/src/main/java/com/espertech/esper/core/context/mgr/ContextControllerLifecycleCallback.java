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
package com.espertech.esper.core.context.mgr;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.context.ContextPartitionIdentifier;
import com.espertech.esper.client.context.ContextPartitionState;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public interface ContextControllerLifecycleCallback {
    public ContextControllerInstanceHandle contextPartitionInstantiate(Integer optionalContextPartitionId,
                                                                       int subpath,
                                                                       Integer importSubpathId, ContextController originator, EventBean optionalTriggeringEvent,
                                                                       Map<String, Object> optionalTriggeringPattern,
                                                                       Object partitionKey,
                                                                       Map<String, Object> contextProperties,
                                                                       ContextControllerState states,
                                                                       ContextInternalFilterAddendum filterAddendum,
                                                                       boolean isRecoveringResilient,
                                                                       ContextPartitionState state,
                                                                       Supplier<ContextPartitionIdentifier> identifier);

    public void contextPartitionNavigate(ContextControllerInstanceHandle existingHandle,
                                         ContextController originator,
                                         ContextControllerState controllerState,
                                         int exportedCPOrPathId,
                                         ContextInternalFilterAddendum filterAddendum,
                                         AgentInstanceSelector agentInstanceSelector,
                                         byte[] payload,
                                         boolean isRecoveringResilient);

    public void contextPartitionTerminate(ContextControllerInstanceHandle contextNestedHandle,
                                          Map<String, Object> terminationProperties,
                                          boolean leaveLocksAcquired,
                                          List<AgentInstance> agentInstances);
}
