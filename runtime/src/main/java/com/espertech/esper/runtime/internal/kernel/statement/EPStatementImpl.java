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
package com.espertech.esper.runtime.internal.kernel.statement;

import com.espertech.esper.common.client.EPException;
import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.client.context.ContextPartitionSelector;
import com.espertech.esper.common.client.util.SafeIterator;
import com.espertech.esper.common.client.util.StatementProperty;
import com.espertech.esper.common.internal.context.util.StatementAgentInstanceLock;
import com.espertech.esper.common.internal.context.util.StatementContext;
import com.espertech.esper.common.internal.statement.dispatch.UpdateDispatchView;
import com.espertech.esper.common.internal.statement.resource.StatementResourceHolder;
import com.espertech.esper.common.internal.view.core.Viewable;
import com.espertech.esper.runtime.client.EPRuntime;
import com.espertech.esper.runtime.client.EPSubscriberException;
import com.espertech.esper.runtime.client.UpdateListener;
import com.espertech.esper.runtime.internal.deploymentlifesvc.StatementListenerEvent;
import com.espertech.esper.runtime.internal.kernel.service.StatementResultServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;

public class EPStatementImpl implements EPStatementSPI {
    private final static Logger log = LoggerFactory.getLogger(EPStatementImpl.class);
    private final EPStatementListenerSet statementListenerSet = new EPStatementListenerSet();

    protected final StatementContext statementContext;
    protected final UpdateDispatchView dispatchChildView;
    protected final StatementResultServiceImpl statementResultService;
    protected Viewable parentView;
    protected boolean destroyed;

    public EPStatementImpl(EPStatementFactoryArgs args) {
        this.statementContext = args.getStatementContext();
        this.dispatchChildView = args.getDispatchChildView();
        this.statementResultService = args.getStatementResultService();
        this.statementResultService.setUpdateListeners(statementListenerSet, false);
    }

    public void addListener(UpdateListener listener) {
        if (listener == null) {
            throw new IllegalArgumentException("Null listener reference supplied");
        }
        checkDestroyed();
        statementListenerSet.addListener(listener);
        statementResultService.setUpdateListeners(statementListenerSet, false);
        statementResultService.getEpServicesContext().getDeploymentLifecycleService().dispatchStatementListenerEvent(
            new StatementListenerEvent(this, StatementListenerEvent.ListenerEventType.LISTENER_ADD, listener));
    }

    public int getStatementId() {
        return statementContext.getStatementId();
    }

    public StatementContext getStatementContext() {
        return statementContext;
    }

    public String getName() {
        return statementContext.getStatementName();
    }

    public UpdateDispatchView getDispatchChildView() {
        return dispatchChildView;
    }

    public boolean isDestroyed() {
        return destroyed;
    }

    public void recoveryUpdateListeners(EPStatementListenerSet listenerSet) {
        statementListenerSet.setListeners(listenerSet);
        statementResultService.setUpdateListeners(listenerSet, true);
    }

    public EventType getEventType() {
        return dispatchChildView.getEventType();
    }

    public Annotation[] getAnnotations() {
        return statementContext.getAnnotations();
    }

    public Iterator<EventBean> iterator() {
        checkDestroyed();
        // Return null if not started
        statementContext.getVariableManagementService().setLocalVersion();
        if (destroyed || parentView == null) {
            return null;
        }
        Iterator<EventBean> theIterator;
        if (statementContext.getContextRuntimeDescriptor() != null) {
            theIterator = statementContext.getContextRuntimeDescriptor().getIteratorHandler().iterator(statementContext.getStatementId());
        } else {
            theIterator = parentView.iterator();
        }
        if (statementContext.getEpStatementHandle().isHasTableAccess()) {
            return new UnsafeIteratorWTableImpl(statementContext.getTableExprEvaluatorContext(), theIterator);
        }
        return theIterator;
    }

    public SafeIterator<EventBean> safeIterator() {
        checkDestroyed();
        // Return null if not started
        if (parentView == null) {
            return null;
        }

        if (statementContext.getContextRuntimeDescriptor() != null) {
            statementContext.getVariableManagementService().setLocalVersion();
            return statementContext.getContextRuntimeDescriptor().getIteratorHandler().safeIterator(statementContext.getStatementId());
        }

        // Set variable version and acquire the lock first
        StatementResourceHolder holder = statementContext.getStatementCPCacheService().getStatementResourceService().getResourcesUnpartitioned();
        StatementAgentInstanceLock lock = holder.getAgentInstanceContext().getAgentInstanceLock();
        lock.acquireReadLock();
        try {
            statementContext.getVariableManagementService().setLocalVersion();

            // Provide iterator - that iterator MUST be closed else the lock is not released
            if (statementContext.getEpStatementHandle().isHasTableAccess()) {
                return new SafeIteratorWTableImpl<>(lock, parentView.iterator(), statementContext.getTableExprEvaluatorContext());
            }
            return new SafeIteratorImpl<>(lock, parentView.iterator());
        } catch (RuntimeException ex) {
            lock.releaseReadLock();
            throw ex;
        }
    }

    public void removeListener(UpdateListener listener) {
        if (listener == null) {
            throw new IllegalArgumentException("Null listener reference supplied");
        }

        statementListenerSet.removeListener(listener);
        statementResultService.setUpdateListeners(statementListenerSet, true);
        statementResultService.getEpServicesContext().getDeploymentLifecycleService().dispatchStatementListenerEvent(
            new StatementListenerEvent(this, StatementListenerEvent.ListenerEventType.LISTENER_REMOVE, listener));
    }

    public void removeAllListeners() {
        statementListenerSet.removeAllListeners();
        statementResultService.setUpdateListeners(statementListenerSet, true);
        statementResultService.getEpServicesContext().getDeploymentLifecycleService().dispatchStatementListenerEvent(
            new StatementListenerEvent(this, StatementListenerEvent.ListenerEventType.LISTENER_REMOVE_ALL));
    }

    public Iterator<UpdateListener> getUpdateListeners() {
        return Arrays.asList(statementListenerSet.getListeners()).iterator();
    }

    public Viewable getParentView() {
        return parentView;
    }

    public void setParentView(Viewable viewable) {
        if (viewable == null) {
            if (parentView != null) {
                parentView.setChild(null);
            }
            parentView = null;
        } else {
            parentView = viewable;
            parentView.setChild(dispatchChildView);
        }
    }

    public void setDestroyed() {
        this.destroyed = true;
    }

    public Iterator<EventBean> iterator(ContextPartitionSelector selector) {
        checkDestroyed();
        if (statementContext.getContextRuntimeDescriptor() == null) {
            throw getUnsupportedNonContextIterator();
        }
        if (selector == null) {
            throw new IllegalArgumentException("No selector provided");
        }

        // Return null if not started
        statementContext.getVariableManagementService().setLocalVersion();
        if (parentView == null) {
            return null;
        }
        return statementContext.getContextRuntimeDescriptor().getIteratorHandler().iterator(statementContext.getStatementId(), selector);
    }

    public SafeIterator<EventBean> safeIterator(ContextPartitionSelector selector) {
        checkDestroyed();
        if (statementContext.getContextRuntimeDescriptor() == null) {
            throw getUnsupportedNonContextIterator();
        }
        if (selector == null) {
            throw new IllegalArgumentException("No selector provided");
        }

        // Return null if not started
        if (parentView == null) {
            return null;
        }

        statementContext.getVariableManagementService().setLocalVersion();
        return statementContext.getContextRuntimeDescriptor().getIteratorHandler().safeIterator(statementContext.getStatementId(), selector);
    }

    public String getDeploymentId() {
        return statementContext.getDeploymentId();
    }

    public void setSubscriber(Object subscriber) {
        checkAllowSubscriber();
        checkDestroyed();
        setSubscriber(subscriber, null);
    }

    public void setSubscriber(Object subscriber, String methodName) {
        checkAllowSubscriber();
        checkDestroyed();
        statementListenerSet.setSubscriber(subscriber, methodName);
        statementResultService.setUpdateListeners(statementListenerSet, false);
    }

    public Object getSubscriber() {
        return statementListenerSet.getSubscriber();
    }

    public Object getProperty(StatementProperty field) {
        if (field == StatementProperty.STATEMENTTYPE) {
            return statementContext.getStatementType();
        }
        if (field == StatementProperty.CONTEXTNAME) {
            return statementContext.getContextRuntimeDescriptor() == null ? null : statementContext.getContextRuntimeDescriptor().getContextName();
        }
        if (field == StatementProperty.CONTEXTDEPLOYMENTID) {
            return statementContext.getContextRuntimeDescriptor() == null ? null : statementContext.getContextRuntimeDescriptor().getContextDeploymentId();
        }
        return statementContext.getStatementInformationals().getProperties().get(field);
    }

    public void addListenerWithReplay(UpdateListener listener) {
        if (listener == null) {
            throw new IllegalArgumentException("Null listener reference supplied");
        }
        checkDestroyed();
        if (statementContext.getStatementInformationals().getOptionalContextName() != null) {
            throw new EPException("Operation is not available for use with contexts");
        }

        EPRuntime runtime = (EPRuntime) statementContext.getRuntime();
        StatementResourceHolder holder = statementContext.getStatementCPCacheService().getStatementResourceService().getResourcesUnpartitioned();
        StatementAgentInstanceLock lock = holder.getAgentInstanceContext().getAgentInstanceLock();
        lock.acquireReadLock();

        try {
            // Add listener - listener not receiving events from this statement, as the statement is locked
            statementListenerSet.addListener(listener);
            this.statementResultService.setUpdateListeners(statementListenerSet, false);

            Iterator<EventBean> it = iterator();
            if (it == null) {
                try {
                    listener.update(null, null, this, runtime);
                } catch (Throwable t) {
                    String message = "Unexpected exception invoking listener update method for replay on listener class '" + listener.getClass().getSimpleName() +
                            "' : " + t.getClass().getSimpleName() + " : " + t.getMessage();
                    log.error(message, t);
                }
                return;
            }

            ArrayList<EventBean> events = new ArrayList<EventBean>();
            for (; it.hasNext(); ) {
                events.add(it.next());
            }

            if (events.isEmpty()) {
                try {
                    listener.update(null, null, this, runtime);
                } catch (Throwable t) {
                    String message = "Unexpected exception invoking listener update method for replay on listener class '" + listener.getClass().getSimpleName() +
                            "' : " + t.getClass().getSimpleName() + " : " + t.getMessage();
                    log.error(message, t);
                }
            } else {
                EventBean[] iteratorResult = events.toArray(new EventBean[events.size()]);
                try {
                    listener.update(iteratorResult, null, this, runtime);
                } catch (Throwable t) {
                    String message = "Unexpected exception invoking listener update method for replay on listener class '" + listener.getClass().getSimpleName() +
                            "' : " + t.getClass().getSimpleName() + " : " + t.getMessage();
                    log.error(message, t);
                }
            }
        } finally {
            if (statementContext.getEpStatementHandle().isHasTableAccess()) {
                statementContext.getTableExprEvaluatorContext().releaseAcquiredLocks();
            }
            lock.releaseReadLock();
        }
    }

    public Object getUserObjectCompileTime() {
        return statementContext.getStatementInformationals().getUserObjectCompileTime();
    }

    public Object getUserObjectRuntime() {
        return statementContext.getUserObjectRuntime();
    }

    private UnsupportedOperationException getUnsupportedNonContextIterator() {
        return new UnsupportedOperationException("Iterator with context selector is only supported for statements under context");
    }

    protected void checkDestroyed() {
        if (destroyed) {
            throw new IllegalStateException("Statement has already been undeployed");
        }
    }

    private void checkAllowSubscriber() throws EPSubscriberException {
        if (!statementContext.getStatementInformationals().isAllowSubscriber()) {
            throw new EPSubscriberException("Setting a subscriber is not allowed for the statement, the statement has been compiled with allowSubscriber=false");
        }
    }
}
