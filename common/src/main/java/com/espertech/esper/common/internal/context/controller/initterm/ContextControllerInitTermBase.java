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

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.internal.collection.IntSeqKey;
import com.espertech.esper.common.internal.context.controller.condition.ContextControllerConditionCallback;
import com.espertech.esper.common.internal.context.controller.condition.ContextControllerConditionFactory;
import com.espertech.esper.common.internal.context.controller.condition.ContextControllerConditionNonHA;
import com.espertech.esper.common.internal.context.controller.condition.ContextControllerEndConditionMatchEventProvider;
import com.espertech.esper.common.internal.context.mgr.ContextManagerRealization;
import com.espertech.esper.common.internal.context.mgr.ContextPartitionInstantiationResult;
import com.espertech.esper.common.internal.context.util.AgentInstance;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

public abstract class ContextControllerInitTermBase extends ContextControllerInitTerm implements ContextControllerConditionCallback, ContextControllerEndConditionMatchEventProvider {
    protected final ContextControllerInitTermSvc initTermSvc;

    public ContextControllerInitTermBase(ContextControllerInitTermFactory factory, ContextManagerRealization realization) {
        super(factory, realization);
        initTermSvc = ContextControllerInitTermUtil.getService(factory);
    }

    public void deactivate(IntSeqKey path, boolean terminateChildContexts) {
        ContextControllerConditionNonHA initCondition = initTermSvc.mgmtDelete(path);
        if (initCondition != null) {
            if (initCondition.isRunning()) {
                initCondition.deactivate();
            }
        }

        Collection<ContextControllerInitTermSvcEntry> endConditions = initTermSvc.endDeleteByParentPath(path);
        for (ContextControllerInitTermSvcEntry entry : endConditions) {
            if (entry.getTerminationCondition().isRunning()) {
                entry.getTerminationCondition().deactivate();
            }

            if (terminateChildContexts) {
                realization.contextPartitionTerminate(path, entry.getSubpathIdOrCPId(), this, null, false, null);
            }
        }
    }

    protected void visitPartitions(IntSeqKey controllerPath, BiConsumer<ContextControllerInitTermPartitionKey, Integer> partKeyAndCPId) {
        initTermSvc.endVisit(controllerPath, partKeyAndCPId);
    }

    public void destroy() {
        initTermSvc.destroy();
    }

    List<AgentInstance> instantiateAndActivateEndCondition(IntSeqKey controllerPath, EventBean optionalTriggeringEvent, Map<String, Object> optionalTriggeringPattern, Map<String, Object> optionalPatternForInclusiveEval, ContextControllerConditionNonHA startCondition) {
        int subpathId = initTermSvc.mgmtUpdIncSubpath(controllerPath);

        IntSeqKey endConditionPath = controllerPath.addToEnd(subpathId);
        Object[] partitionKeys = initTermSvc.mgmtGetParentPartitionKeys(controllerPath);
        ContextControllerConditionNonHA endCondition = ContextControllerConditionFactory.getEndpoint(endConditionPath, partitionKeys, factory.initTermSpec.getEndCondition(), this, this, false);
        endCondition.activate(optionalTriggeringEvent, this, optionalTriggeringPattern);

        ContextControllerInitTermPartitionKey partitionKey = ContextControllerInitTermUtil.buildPartitionKey(optionalTriggeringEvent, optionalTriggeringPattern, endCondition, this);

        ContextPartitionInstantiationResult result = realization.contextPartitionInstantiate(controllerPath, subpathId, this, optionalTriggeringEvent, optionalPatternForInclusiveEval, partitionKeys, partitionKey);
        int subpathIdOrCPId = result.getSubpathOrCPId();

        initTermSvc.endCreate(endConditionPath, subpathIdOrCPId, endCondition, partitionKey);

        return result.getAgentInstances();
    }
}
