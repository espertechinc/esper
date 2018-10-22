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

package com.espertech.esper.runtime.internal.dataflow.op.select;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.client.dataflow.annotations.DataFlowContext;
import com.espertech.esper.common.client.dataflow.util.EPDataFlowSignal;
import com.espertech.esper.common.client.dataflow.util.EPDataFlowSignalFinalMarker;
import com.espertech.esper.common.internal.collection.UniformPair;
import com.espertech.esper.common.internal.context.aifactory.select.StatementAgentInstanceFactorySelectResult;
import com.espertech.esper.common.internal.context.airegistry.AIRegistryUtil;
import com.espertech.esper.common.internal.context.util.AgentInstanceContext;
import com.espertech.esper.common.internal.context.util.AgentInstanceUtil;
import com.espertech.esper.common.internal.epl.dataflow.interfaces.*;
import com.espertech.esper.common.internal.statement.dispatch.UpdateDispatchView;
import com.espertech.esper.common.internal.view.core.ViewSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Iterator;

public class SelectOp extends ViewSupport implements DataFlowOperator, DataFlowOperatorLifecycle, UpdateDispatchView {
    private static final Logger log = LoggerFactory.getLogger(SelectOp.class);

    @DataFlowContext
    private EPDataFlowEmitter graphContext;

    private final SelectFactory factory;
    private final AgentInstanceContext agentInstanceContext;
    private final StatementAgentInstanceFactorySelectResult startResult;

    public SelectOp(SelectFactory factory, AgentInstanceContext agentInstanceContext) {
        this.factory = factory;
        this.agentInstanceContext = agentInstanceContext;

        startResult = factory.getFactorySelect().newContext(agentInstanceContext, false);
        startResult.getFinalView().setChild(this);
        AIRegistryUtil.assignFutures(factory.getResourceRegistry(), agentInstanceContext.getAgentInstanceId(), startResult.getOptionalAggegationService(), startResult.getPriorStrategies(), startResult.getPreviousGetterStrategies(), startResult.getSubselectStrategies(), startResult.getTableAccessStrategies(),
                startResult.getRowRecogPreviousStrategy());
    }

    public void open(DataFlowOpOpenContext openContext) {
    }

    public void onInput(int originatingStream, Object row) {
        if (log.isDebugEnabled()) {
            log.debug("Received row from stream " + originatingStream + " for select, row is " + row);
        }

        EventBean theEvent = factory.getAdapterFactories()[originatingStream].makeAdapter(row);

        agentInstanceContext.getAgentInstanceLock().acquireWriteLock();
        try {
            int target = factory.getOriginatingStreamToViewableStream()[originatingStream];
            startResult.getViewableActivationResults()[target].getViewable().getChild().update(new EventBean[]{theEvent}, null);
            if (startResult.getViewableActivationResults().length > 1) {
                agentInstanceContext.getEpStatementAgentInstanceHandle().getOptionalDispatchable().execute();
            }
        } finally {
            if (agentInstanceContext.getStatementContext().getEpStatementHandle().isHasTableAccess()) {
                agentInstanceContext.getStatementContext().getTableExprEvaluatorContext().releaseAcquiredLocks();
            }
            agentInstanceContext.getAgentInstanceLock().releaseWriteLock();
        }
    }

    public void onSignal(EPDataFlowSignal signal) {
        if (factory.isIterate() && signal instanceof EPDataFlowSignalFinalMarker) {
            Iterator<EventBean> it = startResult.getFinalView().iterator();
            if (it != null) {
                for (; it.hasNext(); ) {
                    EventBean event = it.next();
                    if (factory.isSubmitEventBean()) {
                        graphContext.submit(event);
                    } else {
                        graphContext.submit(event.getUnderlying());
                    }
                }
            }
        }
    }

    public void close(DataFlowOpCloseContext closeContext) {
        AgentInstanceUtil.stop(startResult.getStopCallback(), agentInstanceContext, startResult.getFinalView(), false, false);
        factory.getResourceRegistry().deassign(agentInstanceContext.getAgentInstanceId());
    }

    public void update(EventBean[] newData, EventBean[] oldData) {
        throw new UnsupportedOperationException("Not implemented");
    }

    public EventType getEventType() {
        return factory.getFactorySelect().getStatementEventType();
    }

    public Iterator<EventBean> iterator() {
        return Collections.emptyIterator();
    }

    public void newResult(UniformPair<EventBean[]> result) {
        if (result == null || result.getFirst() == null || result.getFirst().length == 0) {
            return;
        }
        for (EventBean item : result.getFirst()) {
            if (factory.isSubmitEventBean()) {
                graphContext.submit(item);
            } else {
                graphContext.submit(item.getUnderlying());
            }
        }
    }
}
