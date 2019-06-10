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
package com.espertech.esper.common.client.configuration.runtime;

import com.espertech.esper.common.client.util.Locking;

import java.io.Serializable;

/**
 * Holds threading settings.
 */
public class ConfigurationRuntimeThreading implements Serializable {
    private static final long serialVersionUID = 6504606101119059962L;

    private boolean isListenerDispatchPreserveOrder;
    private long listenerDispatchTimeout;
    private Locking listenerDispatchLocking;

    private boolean isInsertIntoDispatchPreserveOrder;
    private long insertIntoDispatchTimeout;
    private Locking insertIntoDispatchLocking;

    private boolean isNamedWindowConsumerDispatchPreserveOrder;
    private long namedWindowConsumerDispatchTimeout;
    private Locking namedWindowConsumerDispatchLocking;

    private long internalTimerMsecResolution;
    private boolean internalTimerEnabled;

    private boolean isThreadPoolTimerExec;
    private boolean isThreadPoolInbound;
    private boolean isThreadPoolRouteExec;
    private boolean isThreadPoolOutbound;
    private int threadPoolTimerExecNumThreads;
    private int threadPoolInboundNumThreads;
    private int threadPoolRouteExecNumThreads;
    private int threadPoolOutboundNumThreads;
    private Integer threadPoolTimerExecCapacity;
    private Integer threadPoolInboundCapacity;
    private Integer threadPoolRouteExecCapacity;
    private Integer threadPoolOutboundCapacity;

    private boolean runtimeFairlock;

    /**
     * Ctor - sets up defaults.
     */
    public ConfigurationRuntimeThreading() {
        listenerDispatchTimeout = 1000;
        isListenerDispatchPreserveOrder = true;
        listenerDispatchLocking = Locking.SPIN;

        insertIntoDispatchTimeout = 100;
        isInsertIntoDispatchPreserveOrder = true;
        insertIntoDispatchLocking = Locking.SPIN;

        namedWindowConsumerDispatchTimeout = Long.MAX_VALUE;
        isNamedWindowConsumerDispatchPreserveOrder = true;
        namedWindowConsumerDispatchLocking = Locking.SPIN;

        internalTimerEnabled = true;
        internalTimerMsecResolution = 100;

        isThreadPoolInbound = false;
        isThreadPoolOutbound = false;
        isThreadPoolRouteExec = false;
        isThreadPoolTimerExec = false;

        threadPoolTimerExecNumThreads = 2;
        threadPoolInboundNumThreads = 2;
        threadPoolRouteExecNumThreads = 2;
        threadPoolOutboundNumThreads = 2;
    }

    /**
     * In multithreaded environments, this setting controls whether dispatches to listeners preserve
     * the ordering in which the statement processes events.
     *
     * @param value is true to preserve ordering, or false if not
     */
    public void setListenerDispatchPreserveOrder(boolean value) {
        isListenerDispatchPreserveOrder = value;
    }

    /**
     * In multithreaded environments, this setting controls when dispatches to listeners preserve
     * the ordering the timeout to complete any outstanding dispatches.
     *
     * @param value is the timeout in milliseconds that the runtime may spend
     *              waiting for a listener dispatch to complete before dispatching further
     *              results for the same statement to listeners for that statement
     */
    public void setListenerDispatchTimeout(long value) {
        listenerDispatchTimeout = value;
    }

    /**
     * In multithreaded environments, this setting controls whether insert-into streams preserve
     * the order of events inserted into them by one or more statements
     * such that statements that consume other statement's events behave deterministic
     *
     * @param value is true to indicate to preserve order, or false to not preserve order
     */
    public void setInsertIntoDispatchPreserveOrder(boolean value) {
        isInsertIntoDispatchPreserveOrder = value;
    }

    /**
     * Returns true to indicate preserve order for dispatch to listeners,
     * or false to indicate not to preserve order
     *
     * @return true or false
     */
    public boolean isListenerDispatchPreserveOrder() {
        return isListenerDispatchPreserveOrder;
    }

    /**
     * Returns the timeout in millisecond to wait for listener code to complete
     * before dispatching the next result, if dispatch order is preserved
     *
     * @return listener dispatch timeout
     */
    public long getListenerDispatchTimeout() {
        return listenerDispatchTimeout;
    }

    /**
     * Returns true to indicate preserve order for inter-statement insert-into,
     * or false to indicate not to preserve order
     *
     * @return true or false
     */
    public boolean isInsertIntoDispatchPreserveOrder() {
        return isInsertIntoDispatchPreserveOrder;
    }

    /**
     * Sets the use of internal timer.
     * <p>
     * By setting internal timer to true (the default) the runtime starts the internal timer thread
     * and relies on internal timer events to supply the time.
     * <p>
     * By setting internal timer to false the runtime does not start the internal timer thread
     * and relies on external application-supplied timer events to supply the time.
     *
     * @param internalTimerEnabled is true for internal timer enabled, or false if the application supplies timer events
     */
    public void setInternalTimerEnabled(boolean internalTimerEnabled) {
        this.internalTimerEnabled = internalTimerEnabled;
    }

    /**
     * Returns true if internal timer is enabled (the default), or false for internal timer disabled.
     *
     * @return true for internal timer enabled, false for internal timer disabled
     */
    public boolean isInternalTimerEnabled() {
        return internalTimerEnabled;
    }

    /**
     * Returns the millisecond resolution of the internal timer thread.
     *
     * @return number of milliseconds between timer processing intervals
     */
    public long getInternalTimerMsecResolution() {
        return internalTimerMsecResolution;
    }

    /**
     * Sets the length of the interval (resolution) of the timer thread.
     *
     * @param internalTimerMsecResolution is the millisecond interval length
     */
    public void setInternalTimerMsecResolution(long internalTimerMsecResolution) {
        this.internalTimerMsecResolution = internalTimerMsecResolution;
    }

    /**
     * Returns the number of milliseconds that a thread may maximally be blocking
     * to deliver statement results from a producing statement that employs insert-into
     * to a consuming statement.
     *
     * @return millisecond timeout for order-of-delivery blocking between statements
     */
    public long getInsertIntoDispatchTimeout() {
        return insertIntoDispatchTimeout;
    }

    /**
     * Sets the blocking strategy to use when multiple threads deliver results for
     * a single statement to listeners, and the guarantee of order of delivery must be maintained.
     *
     * @param listenerDispatchLocking is the blocking technique
     */
    public void setListenerDispatchLocking(Locking listenerDispatchLocking) {
        this.listenerDispatchLocking = listenerDispatchLocking;
    }

    /**
     * Sets the number of milliseconds that a thread may maximally be blocking
     * to deliver statement results from a producing statement that employs insert-into
     * to a consuming statement.
     *
     * @param msecTimeout timeout for order-of-delivery blocking between statements
     */
    public void setInsertIntoDispatchTimeout(long msecTimeout) {
        this.insertIntoDispatchTimeout = msecTimeout;
    }

    /**
     * Sets the blocking strategy to use when multiple threads deliver results for
     * a single statement to consuming statements of an insert-into, and the guarantee of order of delivery must be maintained.
     *
     * @param insertIntoDispatchLocking is the blocking technique
     */
    public void setInsertIntoDispatchLocking(Locking insertIntoDispatchLocking) {
        this.insertIntoDispatchLocking = insertIntoDispatchLocking;
    }

    /**
     * Returns the blocking strategy to use when multiple threads deliver results for
     * a single statement to listeners, and the guarantee of order of delivery must be maintained.
     *
     * @return is the blocking technique
     */
    public Locking getListenerDispatchLocking() {
        return listenerDispatchLocking;
    }

    /**
     * Returns the blocking strategy to use when multiple threads deliver results for
     * a single statement to consuming statements of an insert-into, and the guarantee of order of delivery must be maintained.
     *
     * @return is the blocking technique
     */
    public Locking getInsertIntoDispatchLocking() {
        return insertIntoDispatchLocking;
    }

    /**
     * Returns true for inbound threading enabled, the default is false for not enabled.
     *
     * @return indicator whether inbound threading is enabled
     */
    public boolean isThreadPoolInbound() {
        return isThreadPoolInbound;
    }

    /**
     * Set to true for inbound threading enabled, the default is false for not enabled.
     *
     * @param threadPoolInbound indicator whether inbound threading is enabled
     */
    public void setThreadPoolInbound(boolean threadPoolInbound) {
        isThreadPoolInbound = threadPoolInbound;
    }

    /**
     * Returns true for timer execution threading enabled, the default is false for not enabled.
     *
     * @return indicator whether timer execution threading is enabled
     */
    public boolean isThreadPoolTimerExec() {
        return isThreadPoolTimerExec;
    }

    /**
     * Set to true for timer execution threading enabled, the default is false for not enabled.
     *
     * @param threadPoolTimerExec indicator whether timer execution threading is enabled
     */
    public void setThreadPoolTimerExec(boolean threadPoolTimerExec) {
        isThreadPoolTimerExec = threadPoolTimerExec;
    }

    /**
     * Returns true for route execution threading enabled, the default is false for not enabled.
     *
     * @return indicator whether route execution threading is enabled
     */
    public boolean isThreadPoolRouteExec() {
        return isThreadPoolRouteExec;
    }

    /**
     * Set to true for route execution threading enabled, the default is false for not enabled.
     *
     * @param threadPoolRouteExec indicator whether route execution threading is enabled
     */
    public void setThreadPoolRouteExec(boolean threadPoolRouteExec) {
        isThreadPoolRouteExec = threadPoolRouteExec;
    }

    /**
     * Returns true for outbound threading enabled, the default is false for not enabled.
     *
     * @return indicator whether outbound threading is enabled
     */
    public boolean isThreadPoolOutbound() {
        return isThreadPoolOutbound;
    }

    /**
     * Set to true for outbound threading enabled, the default is false for not enabled.
     *
     * @param threadPoolOutbound indicator whether outbound threading is enabled
     */
    public void setThreadPoolOutbound(boolean threadPoolOutbound) {
        isThreadPoolOutbound = threadPoolOutbound;
    }

    /**
     * Returns the number of thread in the inbound threading pool.
     *
     * @return number of threads
     */
    public int getThreadPoolInboundNumThreads() {
        return threadPoolInboundNumThreads;
    }

    /**
     * Sets the number of threads in the thread pool for inbound threading.
     *
     * @param num number of threads
     */
    public void setThreadPoolInboundNumThreads(int num) {
        this.threadPoolInboundNumThreads = num;
    }

    /**
     * Returns the number of thread in the outbound threading pool.
     *
     * @return number of threads
     */
    public int getThreadPoolOutboundNumThreads() {
        return threadPoolOutboundNumThreads;
    }

    /**
     * Sets the number of threads in the thread pool for outbound threading.
     *
     * @param num number of threads
     */
    public void setThreadPoolOutboundNumThreads(int num) {
        this.threadPoolOutboundNumThreads = num;
    }

    /**
     * Returns the number of thread in the route execution thread pool.
     *
     * @return number of threads
     */
    public int getThreadPoolRouteExecNumThreads() {
        return threadPoolRouteExecNumThreads;
    }

    /**
     * Sets the number of threads in the thread pool for route exec threading.
     *
     * @param num number of threads
     */
    public void setThreadPoolRouteExecNumThreads(int num) {
        this.threadPoolRouteExecNumThreads = num;
    }

    /**
     * Returns the number of thread in the timer execution threading pool.
     *
     * @return number of threads
     */
    public int getThreadPoolTimerExecNumThreads() {
        return threadPoolTimerExecNumThreads;
    }

    /**
     * Sets the number of threads in the thread pool for timer exec threading.
     *
     * @param num number of threads
     */
    public void setThreadPoolTimerExecNumThreads(int num) {
        this.threadPoolTimerExecNumThreads = num;
    }

    /**
     * Returns the capacity of the timer execution queue, or null if none defined (the unbounded case, default).
     *
     * @return capacity or null if none defined
     */
    public Integer getThreadPoolTimerExecCapacity() {
        return threadPoolTimerExecCapacity;
    }

    /**
     * Sets the capacity of the timer execution queue, or null if none defined (the unbounded case, default).
     *
     * @param capacity capacity or null if none defined
     */
    public void setThreadPoolTimerExecCapacity(Integer capacity) {
        this.threadPoolTimerExecCapacity = capacity;
    }

    /**
     * Returns the capacity of the inbound execution queue, or null if none defined (the unbounded case, default).
     *
     * @return capacity or null if none defined
     */
    public Integer getThreadPoolInboundCapacity() {
        return threadPoolInboundCapacity;
    }

    /**
     * Sets the capacity of the inbound queue, or null if none defined (the unbounded case, default).
     *
     * @param capacity capacity or null if none defined
     */
    public void setThreadPoolInboundCapacity(Integer capacity) {
        this.threadPoolInboundCapacity = capacity;
    }

    /**
     * Returns the capacity of the route execution queue, or null if none defined (the unbounded case, default).
     *
     * @return capacity or null if none defined
     */
    public Integer getThreadPoolRouteExecCapacity() {
        return threadPoolRouteExecCapacity;
    }

    /**
     * Sets the capacity of the route execution queue, or null if none defined (the unbounded case, default).
     *
     * @param capacity capacity or null if none defined
     */
    public void setThreadPoolRouteExecCapacity(Integer capacity) {
        this.threadPoolRouteExecCapacity = capacity;
    }

    /**
     * Returns the capacity of the outbound queue, or null if none defined (the unbounded case, default).
     *
     * @return capacity or null if none defined
     */
    public Integer getThreadPoolOutboundCapacity() {
        return threadPoolOutboundCapacity;
    }

    /**
     * Sets the capacity of the outbound queue, or null if none defined (the unbounded case, default).
     *
     * @param capacity capacity or null if none defined
     */
    public void setThreadPoolOutboundCapacity(Integer capacity) {
        this.threadPoolOutboundCapacity = capacity;
    }

    /**
     * Returns true if the runtime-level lock is configured as a fair lock (default is false).
     * <p>
     * This lock coordinates
     * event processing threads (threads that send events) with threads that
     * perform administrative functions (threads that start or destroy statements, for example).
     *
     * @return true for fair lock
     */
    public boolean isRuntimeFairlock() {
        return runtimeFairlock;
    }

    /**
     * Set to true to configured the runtime-level lock as a fair lock (default is false).
     * <p>
     * This lock coordinates
     * event processing threads (threads that send events) with threads that
     * perform administrative functions (threads that start or destroy statements, for example).
     *
     * @param runtimeFairlock true for fair lock
     */
    public void setRuntimeFairlock(boolean runtimeFairlock) {
        this.runtimeFairlock = runtimeFairlock;
    }

    /**
     * In multithreaded environments, this setting controls whether named window dispatches to named window consumers preserve
     * the order of events inserted and removed such that statements that consume a named windows delta stream
     * behave deterministic (true by default).
     *
     * @return flag
     */
    public boolean isNamedWindowConsumerDispatchPreserveOrder() {
        return isNamedWindowConsumerDispatchPreserveOrder;
    }

    /**
     * In multithreaded environments, this setting controls whether named window dispatches to named window consumers preserve
     * the order of events inserted and removed such that statements that consume a named windows delta stream
     * behave deterministic (true by default).
     *
     * @param isNamedWindowConsumerDispatchPreserveOrder flag
     */
    public void setNamedWindowConsumerDispatchPreserveOrder(boolean isNamedWindowConsumerDispatchPreserveOrder) {
        this.isNamedWindowConsumerDispatchPreserveOrder = isNamedWindowConsumerDispatchPreserveOrder;
    }

    /**
     * Returns the timeout millisecond value for named window dispatches to named window consumers.
     *
     * @return timeout milliseconds
     */
    public long getNamedWindowConsumerDispatchTimeout() {
        return namedWindowConsumerDispatchTimeout;
    }

    /**
     * Sets the timeout millisecond value for named window dispatches to named window consumers.
     *
     * @param namedWindowConsumerDispatchTimeout timeout milliseconds
     */
    public void setNamedWindowConsumerDispatchTimeout(long namedWindowConsumerDispatchTimeout) {
        this.namedWindowConsumerDispatchTimeout = namedWindowConsumerDispatchTimeout;
    }

    /**
     * Returns the locking strategy value for named window dispatches to named window consumers (default is spin).
     *
     * @return strategy
     */
    public Locking getNamedWindowConsumerDispatchLocking() {
        return namedWindowConsumerDispatchLocking;
    }

    /**
     * Sets the locking strategy value for named window dispatches to named window consumers (default is spin).
     *
     * @param namedWindowConsumerDispatchLocking strategy
     */
    public void setNamedWindowConsumerDispatchLocking(Locking namedWindowConsumerDispatchLocking) {
        this.namedWindowConsumerDispatchLocking = namedWindowConsumerDispatchLocking;
    }

}
