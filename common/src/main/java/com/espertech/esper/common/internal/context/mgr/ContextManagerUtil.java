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
package com.espertech.esper.common.internal.context.mgr;

import com.espertech.esper.common.internal.context.controller.core.ContextControllerFactory;
import com.espertech.esper.common.internal.context.controller.core.ContextDefinition;
import com.espertech.esper.common.internal.context.util.AgentInstance;
import com.espertech.esper.common.internal.context.util.AgentInstanceContext;
import com.espertech.esper.common.internal.context.util.ContextPropertyEventType;
import com.espertech.esper.common.internal.context.util.StatementContext;
import com.espertech.esper.common.internal.event.core.MappedEventBean;
import com.espertech.esper.common.internal.filterspec.FilterAddendumUtil;
import com.espertech.esper.common.internal.filterspec.FilterSpecActivatable;
import com.espertech.esper.common.internal.filterspec.FilterValueSetParam;
import com.espertech.esper.common.internal.statement.resource.StatementResourceHolder;
import com.espertech.esper.common.internal.util.CollectionUtil;

import java.util.*;
import java.util.function.Function;

public class ContextManagerUtil {

    public static List<AgentInstance> getAgentInstances(ContextControllerStatementDesc statement, Collection<Integer> agentInstanceIds) {
        StatementContext statementContext = statement.getLightweight().getStatementContext();
        List<AgentInstance> instances = new ArrayList<AgentInstance>();
        for (int id : agentInstanceIds) {
            AgentInstance agentInstance = getAgentInstance(statementContext, id);
            instances.add(agentInstance);
        }
        return instances;
    }

    public static AgentInstance getAgentInstance(StatementContext statementContext, int agentInstanceId) {
        StatementResourceHolder holder = statementContext.getStatementCPCacheService().makeOrGetEntryCanNull(agentInstanceId, statementContext);
        return new AgentInstance(holder.getAgentInstanceStopCallback(), holder.getAgentInstanceContext(), holder.getFinalView());
    }

    public static List<AgentInstance> getAgentInstancesFiltered(ContextControllerStatementDesc statement, Collection<Integer> agentInstanceIds, Function<AgentInstance, Boolean> filter) {
        StatementContext statementContext = statement.getLightweight().getStatementContext();
        List<AgentInstance> instances = new ArrayList<AgentInstance>();
        for (int id : agentInstanceIds) {
            AgentInstance agentInstance = getAgentInstance(statementContext, id);
            if (filter.apply(agentInstance)) {
                instances.add(agentInstance);
            }
        }
        return instances;
    }

    public static IdentityHashMap<FilterSpecActivatable, FilterValueSetParam[][]> computeAddendumForStatement(ContextControllerStatementDesc statementDesc,
                                                                                                              Map<Integer, ContextControllerStatementDesc> statements,
                                                                                                              ContextControllerFactory[] controllerFactories,
                                                                                                              Object[] allPartitionKeys,
                                                                                                              AgentInstanceContext agentInstanceContextCreate) {
        Map<Integer, FilterSpecActivatable> filters = statementDesc.getLightweight().getStatementContext().getFilterSpecActivatables();
        IdentityHashMap<FilterSpecActivatable, FilterValueSetParam[][]> map = new IdentityHashMap<>(CollectionUtil.capacityHashMap(filters.size()));
        for (Map.Entry<Integer, FilterSpecActivatable> filter : filters.entrySet()) {
            FilterValueSetParam[][] addendum = computeAddendum(allPartitionKeys, filter.getValue(), true, statementDesc, controllerFactories, statements, agentInstanceContextCreate);
            if (addendum != null && addendum.length > 0) {
                map.put(filter.getValue(), addendum);
            }
        }
        return map;
    }

    public static FilterValueSetParam[][] computeAddendumNonStmt(Object[] partitionKeys, FilterSpecActivatable filterCallback, ContextManagerRealization realization) {
        return computeAddendum(partitionKeys, filterCallback, false, null, realization.getContextManager().getContextDefinition().getControllerFactories(), realization.getContextManager().getStatements(), realization.getAgentInstanceContextCreate());
    }

    private static FilterValueSetParam[][] computeAddendum(Object[] parentPartitionKeys, FilterSpecActivatable filterCallback, boolean forStatement, ContextControllerStatementDesc optionalStatementDesc, ContextControllerFactory[] controllerFactories, Map<Integer, ContextControllerStatementDesc> statements, AgentInstanceContext agentInstanceContextCreate) {
        FilterValueSetParam[][] result = new FilterValueSetParam[0][];
        for (int i = 0; i < parentPartitionKeys.length; i++) {
            FilterValueSetParam[][] addendumForController = controllerFactories[i].populateFilterAddendum(filterCallback, forStatement, i + 1, parentPartitionKeys[i], optionalStatementDesc, statements, agentInstanceContextCreate);
            result = FilterAddendumUtil.multiplyAddendum(result, addendumForController);
        }
        return result;
    }

    public static MappedEventBean buildContextProperties(int agentInstanceId, Object[] allPartitionKeys, ContextDefinition contextDefinition, StatementContext statementContextCreate) {
        Map<String, Object> props = buildContextPropertiesMap(agentInstanceId, allPartitionKeys, contextDefinition);
        return statementContextCreate.getEventBeanTypedEventFactory().adapterForTypedMap(props, contextDefinition.getEventTypeContextProperties());
    }

    private static Map<String, Object> buildContextPropertiesMap(int agentInstanceId, Object[] allPartitionKeys, ContextDefinition contextDefinition) {
        Map<String, Object> props = new HashMap<>();
        props.put(ContextPropertyEventType.PROP_CTX_NAME, contextDefinition.getContextName());
        props.put(ContextPropertyEventType.PROP_CTX_ID, agentInstanceId);

        ContextControllerFactory[] controllerFactories = contextDefinition.getControllerFactories();

        if (controllerFactories.length == 1) {
            controllerFactories[0].populateContextProperties(props, allPartitionKeys[0]);
            return props;
        }

        for (int level = 0; level < controllerFactories.length; level++) {
            String nestedContextName = controllerFactories[level].getFactoryEnv().getContextName();
            Map<String, Object> nestedProps = new HashMap<>();
            nestedProps.put(ContextPropertyEventType.PROP_CTX_NAME, nestedContextName);
            if (level == controllerFactories.length - 1) {
                nestedProps.put(ContextPropertyEventType.PROP_CTX_ID, agentInstanceId);
            }
            controllerFactories[level].populateContextProperties(nestedProps, allPartitionKeys[level]);
            props.put(nestedContextName, nestedProps);
        }

        return props;
    }
}
