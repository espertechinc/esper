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

import com.espertech.esper.client.EventType;
import com.espertech.esper.client.context.ContextPartitionDescriptor;
import com.espertech.esper.client.context.ContextPartitionSelector;
import com.espertech.esper.client.context.ContextPartitionStateListener;
import com.espertech.esper.core.context.util.ContextDescriptor;
import com.espertech.esper.epl.expression.core.ExprValidationException;
import com.espertech.esper.filter.FilterFaultHandler;
import com.espertech.esper.epl.expression.core.ExprFilterSpecLookupable;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

public interface ContextManager extends FilterFaultHandler {
    public ContextDescriptor getContextDescriptor();

    public int getNumNestingLevels();

    public ContextStateCache getContextStateCache();

    public void addStatement(ContextControllerStatementBase statement, boolean isRecoveringResilient) throws ExprValidationException;

    public void stopStatement(String statementName, int statementId);

    public void destroyStatement(String statementName, int statementId);

    public void safeDestroy();

    public ExprFilterSpecLookupable getFilterLookupable(EventType eventType);

    public ContextStatePathDescriptor extractPaths(ContextPartitionSelector contextPartitionSelector);

    public ContextStatePathDescriptor extractStopPaths(ContextPartitionSelector contextPartitionSelector);

    public ContextStatePathDescriptor extractDestroyPaths(ContextPartitionSelector contextPartitionSelector);

    public void importStartPaths(ContextControllerState state, AgentInstanceSelector agentInstanceSelector);

    public Map<Integer, ContextPartitionDescriptor> startPaths(ContextPartitionSelector contextPartitionSelector);

    public Collection<Integer> getAgentInstanceIds(ContextPartitionSelector contextPartitionSelector);

    public Map<Integer, ContextControllerStatementDesc> getStatements();

    public void addListener(ContextPartitionStateListener listener);

    public void removeListener(ContextPartitionStateListener listener);

    public Iterator<ContextPartitionStateListener> getListeners();

    public void removeListeners();

    public Map<String, Object> getContextProperties(int contextPartitionId);
}
