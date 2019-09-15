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

import com.espertech.esper.common.client.context.ContextPartitionIdentifier;
import com.espertech.esper.common.client.context.ContextPartitionIdentifierHash;
import com.espertech.esper.common.client.util.StatementType;
import com.espertech.esper.common.internal.context.aifactory.createwindow.StatementAgentInstanceFactoryCreateNW;
import com.espertech.esper.common.internal.context.airegistry.*;
import com.espertech.esper.common.internal.context.controller.core.ContextController;
import com.espertech.esper.common.internal.context.controller.core.ContextControllerFactoryBase;
import com.espertech.esper.common.internal.context.mgr.ContextControllerStatementDesc;
import com.espertech.esper.common.internal.context.mgr.ContextManagerRealization;
import com.espertech.esper.common.internal.context.util.AgentInstanceContext;
import com.espertech.esper.common.internal.event.core.EventTypeUtility;
import com.espertech.esper.common.internal.filterspec.*;

import java.util.Map;

public class ContextControllerHashFactory extends ContextControllerFactoryBase {
    protected ContextControllerDetailHash hashSpec;

    public ContextControllerDetailHash getHashSpec() {
        return hashSpec;
    }

    public void setHashSpec(ContextControllerDetailHash hashSpec) {
        this.hashSpec = hashSpec;
    }

    public ContextController create(ContextManagerRealization contextManagerRealization) {
        return new ContextControllerHashImpl(this, contextManagerRealization);
    }

    public FilterValueSetParam[][] populateFilterAddendum(FilterSpecActivatable filterSpec, boolean forStatement, int nestingLevel, Object partitionKey, ContextControllerStatementDesc optionalStatementDesc, Map<Integer, ContextControllerStatementDesc> statements, AgentInstanceContext agentInstanceContextStatement) {
        // determine whether create-named-window
        boolean isCreateWindow = optionalStatementDesc != null && optionalStatementDesc.getLightweight().getStatementContext().getStatementInformationals().getStatementType() == StatementType.CREATE_WINDOW;
        ContextControllerDetailHashItem foundPartition = null;
        int hashCode = (Integer) partitionKey;

        if (!isCreateWindow) {
            foundPartition = findHashItemSpec(hashSpec, filterSpec);
        } else {
            StatementAgentInstanceFactoryCreateNW factory = (StatementAgentInstanceFactoryCreateNW) optionalStatementDesc.getLightweight().getStatementContext().getStatementAIFactoryProvider().getFactory();
            String declaredAsName = factory.getAsEventTypeName();
            for (ContextControllerDetailHashItem partitionItem : hashSpec.getItems()) {
                if (partitionItem.getFilterSpecActivatable().getFilterForEventType().getName().equals(declaredAsName)) {
                    foundPartition = partitionItem;
                    break;
                }
            }
        }

        if (foundPartition == null) {
            return null;
        }

        FilterValueSetParam filter = new FilterValueSetParamImpl(foundPartition.getLookupable(), FilterOperator.EQUAL, hashCode);

        FilterValueSetParam[][] addendum = new FilterValueSetParam[1][];
        addendum[0] = new FilterValueSetParam[]{filter};

        FilterValueSetParam[][] partitionFilters = foundPartition.getFilterSpecActivatable().getValueSet(null, null, agentInstanceContextStatement, agentInstanceContextStatement.getStatementContextFilterEvalEnv());
        if (partitionFilters != null) {
            addendum = FilterAddendumUtil.addAddendum(partitionFilters, filter);
        }
        return addendum;
    }

    public void populateContextProperties(Map<String, Object> props, Object allPartitionKey) {
        // nothing to populate
    }

    public StatementAIResourceRegistry allocateAgentInstanceResourceRegistry(AIRegistryRequirements registryRequirements) {
        if (hashSpec.getGranularity() <= 65536) {
            return AIRegistryUtil.allocateRegistries(registryRequirements, AIRegistryFactoryMultiPerm.INSTANCE);
        }
        return AIRegistryUtil.allocateRegistries(registryRequirements, AIRegistryFactoryMap.INSTANCE);
    }

    public ContextPartitionIdentifier getContextPartitionIdentifier(Object partitionKey) {
        return new ContextPartitionIdentifierHash((Integer) partitionKey);
    }

    private static ContextControllerDetailHashItem findHashItemSpec(ContextControllerDetailHash hashSpec, FilterSpecActivatable filterSpec) {
        ContextControllerDetailHashItem foundPartition = null;
        for (ContextControllerDetailHashItem partitionItem : hashSpec.getItems()) {
            boolean typeOrSubtype = EventTypeUtility.isTypeOrSubTypeOf(filterSpec.getFilterForEventType(), partitionItem.getFilterSpecActivatable().getFilterForEventType());
            if (typeOrSubtype) {
                foundPartition = partitionItem;
            }
        }
        return foundPartition;
    }
}
