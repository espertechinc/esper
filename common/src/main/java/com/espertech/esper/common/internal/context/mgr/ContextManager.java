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

import com.espertech.esper.common.client.context.ContextPartitionCollection;
import com.espertech.esper.common.client.context.ContextPartitionIdentifier;
import com.espertech.esper.common.client.context.ContextPartitionSelector;
import com.espertech.esper.common.client.context.ContextPartitionStateListener;
import com.espertech.esper.common.internal.context.airegistry.AIRegistryRequirements;
import com.espertech.esper.common.internal.context.airegistry.StatementAIResourceRegistry;
import com.espertech.esper.common.internal.context.util.*;
import com.espertech.esper.common.internal.event.core.MappedEventBean;
import com.espertech.esper.common.client.serde.DataInputOutputSerde;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

public interface ContextManager extends FilterFaultHandler {
    void setStatementContext(StatementContext statementContext);

    void addStatement(ContextControllerStatementDesc statement, boolean recovery);

    void stopStatement(ContextControllerStatementDesc statement);

    int countStatements(Function<StatementContext, Boolean> filter);

    Map<Integer, ContextControllerStatementDesc> getStatements();

    int getNumNestingLevels();

    ContextAgentInstanceInfo getContextAgentInstanceInfo(StatementContext statementContextOfStatement, int agentInstanceId);

    ContextManagerRealization getRealization();

    ContextRuntimeDescriptor getContextRuntimeDescriptor();

    StatementAIResourceRegistry allocateAgentInstanceResourceRegistry(AIRegistryRequirements registryRequirements);

    DataInputOutputSerde[] getContextPartitionKeySerdes();

    ContextManagerRealization allocateNewRealization(AgentInstanceContext agentInstanceContext);

    Map<String, Object> getContextPartitions(int contextPartitionId);

    MappedEventBean getContextPropertiesEvent(int contextPartitionId);

    ContextPartitionIdentifier getContextIdentifier(int agentInstanceId);

    ContextPartitionCollection getContextPartitions(ContextPartitionSelector selector);

    Set<Integer> getContextPartitionIds(ContextPartitionSelector selector);

    long getContextPartitionCount();

    void addListener(ContextPartitionStateListener listener);

    void removeListener(ContextPartitionStateListener listener);

    Iterator<ContextPartitionStateListener> getListeners();

    void removeListeners();

    void destroyContext();

    void clearCaches();
}
