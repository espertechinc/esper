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
package com.espertech.esper.common.internal.context.controller.category;

import com.espertech.esper.common.client.context.ContextPartitionIdentifier;
import com.espertech.esper.common.client.context.ContextPartitionIdentifierCategory;
import com.espertech.esper.common.internal.context.airegistry.AIRegistryFactoryMultiPerm;
import com.espertech.esper.common.internal.context.airegistry.AIRegistryRequirements;
import com.espertech.esper.common.internal.context.airegistry.AIRegistryUtil;
import com.espertech.esper.common.internal.context.airegistry.StatementAIResourceRegistry;
import com.espertech.esper.common.internal.context.controller.core.ContextController;
import com.espertech.esper.common.internal.context.controller.core.ContextControllerFactoryBase;
import com.espertech.esper.common.internal.context.mgr.ContextControllerStatementDesc;
import com.espertech.esper.common.internal.context.mgr.ContextManagerRealization;
import com.espertech.esper.common.internal.context.util.AgentInstanceContext;
import com.espertech.esper.common.internal.event.core.EventTypeUtility;
import com.espertech.esper.common.internal.filterspec.FilterSpecActivatable;
import com.espertech.esper.common.internal.filterspec.FilterValueSetParam;

import java.util.Map;

import static com.espertech.esper.common.internal.context.util.ContextPropertyEventType.PROP_CTX_LABEL;

public class ContextControllerCategoryFactory extends ContextControllerFactoryBase {
    private ContextControllerDetailCategory categorySpec;

    public ContextControllerDetailCategory getCategorySpec() {
        return categorySpec;
    }

    public void setCategorySpec(ContextControllerDetailCategory categorySpec) {
        this.categorySpec = categorySpec;
    }

    public ContextController create(ContextManagerRealization contextManagerRealization) {
        return new ContextControllerCategoryImpl(contextManagerRealization, this);
    }

    public FilterValueSetParam[][] populateFilterAddendum(FilterSpecActivatable filterSpec, boolean forStatement, int nestingLevel, Object partitionKey, ContextControllerStatementDesc optionalStatementDesc, Map<Integer, ContextControllerStatementDesc> statements, AgentInstanceContext agentInstanceContextStatement) {
        if (!forStatement) {
            if (!EventTypeUtility.isTypeOrSubTypeOf(filterSpec.getFilterForEventType(), categorySpec.getFilterSpecActivatable().getFilterForEventType())) {
                return null;
            }
        }

        int categoryNum = (Integer) partitionKey;
        ContextControllerDetailCategoryItem item = categorySpec.getItems()[categoryNum];
        return FilterSpecActivatable.evaluateValueSet(item.getCompiledFilterParam(), null, agentInstanceContextStatement);
    }

    public void populateContextProperties(Map<String, Object> props, Object allPartitionKey) {
        ContextControllerDetailCategoryItem item = categorySpec.getItems()[(Integer) allPartitionKey];
        props.put(PROP_CTX_LABEL, item.getName());
    }

    public StatementAIResourceRegistry allocateAgentInstanceResourceRegistry(AIRegistryRequirements registryRequirements) {
        return AIRegistryUtil.allocateRegistries(registryRequirements, AIRegistryFactoryMultiPerm.INSTANCE);
    }

    public ContextPartitionIdentifier getContextPartitionIdentifier(Object partitionKey) {
        int categoryNum = (Integer) partitionKey;
        ContextControllerDetailCategoryItem item = categorySpec.getItems()[categoryNum];
        return new ContextPartitionIdentifierCategory(item.getName());
    }
}
