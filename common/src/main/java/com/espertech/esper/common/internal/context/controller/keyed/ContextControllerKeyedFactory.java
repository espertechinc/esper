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

import com.espertech.esper.common.client.context.ContextPartitionIdentifier;
import com.espertech.esper.common.client.context.ContextPartitionIdentifierPartitioned;
import com.espertech.esper.common.client.util.MultiKey;
import com.espertech.esper.common.internal.context.airegistry.*;
import com.espertech.esper.common.internal.context.controller.core.ContextController;
import com.espertech.esper.common.internal.context.controller.core.ContextControllerFactoryBase;
import com.espertech.esper.common.internal.context.mgr.ContextControllerStatementDesc;
import com.espertech.esper.common.internal.context.mgr.ContextManagerRealization;
import com.espertech.esper.common.internal.context.util.AgentInstanceContext;
import com.espertech.esper.common.internal.context.util.ContextPropertyEventType;
import com.espertech.esper.common.internal.event.core.EventTypeUtility;
import com.espertech.esper.common.internal.filterspec.FilterSpecActivatable;
import com.espertech.esper.common.internal.filterspec.FilterValueSetParam;

import java.util.List;
import java.util.Map;

public class ContextControllerKeyedFactory extends ContextControllerFactoryBase {
    protected ContextControllerDetailKeyed keyedSpec;

    public ContextControllerDetailKeyed getKeyedSpec() {
        return keyedSpec;
    }

    public void setKeyedSpec(ContextControllerDetailKeyed keyedSpec) {
        this.keyedSpec = keyedSpec;
    }

    public ContextController create(ContextManagerRealization contextManagerRealization) {
        return new ContextControllerKeyedImpl(this, contextManagerRealization);
    }

    public FilterValueSetParam[][] populateFilterAddendum(FilterSpecActivatable filterSpec, boolean forStatement, int nestingLevel, Object partitionKey, ContextControllerStatementDesc optionalStatementDesc, Map<Integer, ContextControllerStatementDesc> statements, AgentInstanceContext agentInstanceContextStatement) {
        if (!forStatement) {
            boolean found = false;
            List<FilterSpecActivatable> filters = keyedSpec.getFilterSpecActivatables();
            for (FilterSpecActivatable def : filters) {
                if (EventTypeUtility.isTypeOrSubTypeOf(filterSpec.getFilterForEventType(), def.getFilterForEventType())) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                return null;
            }
        }

        boolean includePartitionKey = forStatement || nestingLevel != factoryContext.getNestingLevel();
        Object getterKey = getGetterKey(partitionKey);
        return ContextControllerKeyedUtil.getAddendumFilters(getterKey, filterSpec, keyedSpec, includePartitionKey, optionalStatementDesc, statements, agentInstanceContextStatement);
    }

    public void populateContextProperties(Map<String, Object> props, Object partitionKey) {
        if (!keyedSpec.isHasAsName()) {
            populateContextPropertiesAddKeyInfo(props, partitionKey);
            return;
        }

        ContextControllerKeyedPartitionKeyWInit info = (ContextControllerKeyedPartitionKeyWInit) partitionKey;
        populateContextPropertiesAddKeyInfo(props, info.getGetterKey());
        if (info.getOptionalInitAsName() != null) {
            props.put(info.getOptionalInitAsName(), info.getOptionalInitBean());
        }
    }

    public StatementAIResourceRegistry allocateAgentInstanceResourceRegistry(AIRegistryRequirements registryRequirements) {
        if (keyedSpec.getOptionalTermination() != null) {
            return AIRegistryUtil.allocateRegistries(registryRequirements, AIRegistryFactoryMap.INSTANCE);
        }
        return AIRegistryUtil.allocateRegistries(registryRequirements, AIRegistryFactoryMultiPerm.INSTANCE);
    }

    public ContextPartitionIdentifier getContextPartitionIdentifier(Object partitionKey) {
        Object getterKey = getGetterKey(partitionKey);
        if (getterKey instanceof Object[]) {
            return new ContextPartitionIdentifierPartitioned((Object[]) getterKey);
        }
        return new ContextPartitionIdentifierPartitioned(new Object[]{getterKey});
    }

    public Object getGetterKey(Object partitionKey) {
        if (keyedSpec.isHasAsName()) {
            ContextControllerKeyedPartitionKeyWInit info = (ContextControllerKeyedPartitionKeyWInit) partitionKey;
            return info.getGetterKey();
        }
        return partitionKey;
    }

    private void populateContextPropertiesAddKeyInfo(Map<String, Object> props, Object getterKey) {
        if (getterKey instanceof MultiKey) {
            MultiKey values = (MultiKey) getterKey;
            for (int i = 0; i < values.getNumKeys(); i++) {
                String propertyName = ContextPropertyEventType.PROP_CTX_KEY_PREFIX + (i + 1);
                props.put(propertyName, values.getKey(i));
            }
        } else {
            props.put(ContextPropertyEventType.PROP_CTX_KEY_PREFIX_SINGLE, getterKey);
        }
    }
}
