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

import com.espertech.esper.common.client.context.ContextPartitionIdentifier;
import com.espertech.esper.common.client.context.ContextPartitionIdentifierInitiatedTerminated;
import com.espertech.esper.common.internal.context.airegistry.*;
import com.espertech.esper.common.internal.context.controller.condition.ContextConditionDescriptorFilter;
import com.espertech.esper.common.internal.context.controller.condition.ContextConditionDescriptorPattern;
import com.espertech.esper.common.internal.context.controller.core.ContextController;
import com.espertech.esper.common.internal.context.controller.core.ContextControllerFactoryBase;
import com.espertech.esper.common.internal.context.mgr.ContextControllerStatementDesc;
import com.espertech.esper.common.internal.context.mgr.ContextManagerRealization;
import com.espertech.esper.common.internal.context.util.AgentInstanceContext;
import com.espertech.esper.common.internal.context.util.ContextPropertyEventType;
import com.espertech.esper.common.internal.filterspec.FilterSpecActivatable;
import com.espertech.esper.common.internal.filterspec.FilterValueSetParam;

import java.util.Collections;
import java.util.Map;

public class ContextControllerInitTermFactory extends ContextControllerFactoryBase {
    protected ContextControllerDetailInitiatedTerminated initTermSpec;

    public ContextControllerDetailInitiatedTerminated getInitTermSpec() {
        return initTermSpec;
    }

    public void setInitTermSpec(ContextControllerDetailInitiatedTerminated initTermSpec) {
        this.initTermSpec = initTermSpec;
    }

    public ContextController create(ContextManagerRealization contextManagerRealization) {
        if (initTermSpec.isOverlapping()) {
            return new ContextControllerInitTermOverlap(this, contextManagerRealization);
        }
        return new ContextControllerInitTermNonOverlap(this, contextManagerRealization);
    }

    public FilterValueSetParam[][] populateFilterAddendum(FilterSpecActivatable filterSpec, boolean forStatement, int nestingLevel, Object partitionKey, ContextControllerStatementDesc optionalStatementDesc, Map<Integer, ContextControllerStatementDesc> statements, AgentInstanceContext agentInstanceContextStatement) {
        // none
        return null;
    }

    public void populateContextProperties(Map<String, Object> props, Object partitionKey) {
        ContextControllerInitTermPartitionKey key = (ContextControllerInitTermPartitionKey) partitionKey;
        props.put(ContextPropertyEventType.PROP_CTX_STARTTIME, key.getStartTime());
        props.put(ContextPropertyEventType.PROP_CTX_ENDTIME, key.getExpectedEndTime());

        if (initTermSpec.getStartCondition() instanceof ContextConditionDescriptorFilter) {
            ContextConditionDescriptorFilter filter = (ContextConditionDescriptorFilter) initTermSpec.getStartCondition();
            if (filter.getOptionalFilterAsName() != null) {
                props.put(filter.getOptionalFilterAsName(), key.getTriggeringEvent());
            }
        }

        if (initTermSpec.getStartCondition() instanceof ContextConditionDescriptorPattern) {
            Map<String, Object> pattern = key.getTriggeringPattern();
            if (pattern != null) {
                props.putAll(pattern);
            }
        }
    }

    public StatementAIResourceRegistry allocateAgentInstanceResourceRegistry(AIRegistryRequirements registryRequirements) {
        if (initTermSpec.isOverlapping()) {
            return AIRegistryUtil.allocateRegistries(registryRequirements, AIRegistryFactoryMap.INSTANCE);
        }
        return AIRegistryUtil.allocateRegistries(registryRequirements, AIRegistryFactorySingle.INSTANCE);
    }

    public ContextPartitionIdentifier getContextPartitionIdentifier(Object partitionKey) {
        ContextControllerInitTermPartitionKey key = (ContextControllerInitTermPartitionKey) partitionKey;
        ContextPartitionIdentifierInitiatedTerminated ident = new ContextPartitionIdentifierInitiatedTerminated();
        ident.setStartTime(key.getStartTime());
        ident.setEndTime(key.getExpectedEndTime());

        if (initTermSpec.getStartCondition() instanceof ContextConditionDescriptorFilter) {
            ContextConditionDescriptorFilter filter = (ContextConditionDescriptorFilter) initTermSpec.getStartCondition();
            if (filter.getOptionalFilterAsName() != null) {
                ident.setProperties(Collections.singletonMap(filter.getOptionalFilterAsName(), key.getTriggeringEvent()));
            }
        }

        return ident;
    }
}
