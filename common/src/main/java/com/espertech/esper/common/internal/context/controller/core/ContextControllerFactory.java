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

import com.espertech.esper.common.client.context.ContextPartitionIdentifier;
import com.espertech.esper.common.internal.context.airegistry.AIRegistryRequirements;
import com.espertech.esper.common.internal.context.airegistry.StatementAIResourceRegistry;
import com.espertech.esper.common.internal.context.mgr.ContextControllerStatementDesc;
import com.espertech.esper.common.internal.context.mgr.ContextManagerRealization;
import com.espertech.esper.common.internal.context.util.AgentInstanceContext;
import com.espertech.esper.common.internal.filterspec.FilterSpecActivatable;
import com.espertech.esper.common.internal.filterspec.FilterValueSetParam;

import java.util.Map;

public interface ContextControllerFactory {
    void setFactoryEnv(ContextControllerFactoryEnv factoryEnv);

    ContextControllerFactoryEnv getFactoryEnv();

    ContextController create(ContextManagerRealization contextManagerRealization);

    FilterValueSetParam[][] populateFilterAddendum(FilterSpecActivatable filterSpec, boolean forStatement, int nestingLevel, Object partitionKey, ContextControllerStatementDesc optionalStatementDesc, Map<Integer, ContextControllerStatementDesc> statements, AgentInstanceContext agentInstanceContextStatement);

    void populateContextProperties(Map<String, Object> props, Object allPartitionKey);

    StatementAIResourceRegistry allocateAgentInstanceResourceRegistry(AIRegistryRequirements registryRequirements);

    ContextPartitionIdentifier getContextPartitionIdentifier(Object partitionKey);
}
