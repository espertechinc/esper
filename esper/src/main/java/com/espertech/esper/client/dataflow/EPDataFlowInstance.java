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
package com.espertech.esper.client.dataflow;

import java.util.Map;

/**
 * Data flow instanve.
 */
public interface EPDataFlowInstance {
    /**
     * Returns the data flow name.
     *
     * @return name
     */
    public String getDataFlowName();

    /**
     * Returns the state.
     *
     * @return state
     */
    public EPDataFlowState getState();

    /**
     * Blocking execution of the data flow instance.
     *
     * @throws IllegalStateException           thrown to indicate that the state is not instantiated.
     * @throws EPDataFlowExecutionException    thrown when an execution exception occurs
     * @throws EPDataFlowCancellationException throw to indicate the data flow was cancelled.
     */
    public void run() throws IllegalStateException, EPDataFlowExecutionException, EPDataFlowCancellationException;

    /**
     * Non-Blocking execution of the data flow instance.
     *
     * @throws IllegalStateException thrown to indicate that the state is not instantiated.
     */
    public void start() throws IllegalStateException;

    /**
     * Captive execution of the data flow instance.
     *
     * @return runnables and emitters
     */
    public EPDataFlowInstanceCaptive startCaptive();

    /**
     * Join an executing data flow instance.
     *
     * @throws IllegalStateException thrown if it cannot be joined
     * @throws InterruptedException  thrown if interrupted
     */
    public void join() throws IllegalStateException, InterruptedException;

    /**
     * Cancel execution.
     */
    public void cancel();

    /**
     * Get data flow instance statistics, required instantiation with statistics option, use {@link EPDataFlowInstantiationOptions} to turn on stats.
     *
     * @return stats
     */
    public EPDataFlowInstanceStatistics getStatistics();

    /**
     * Returns the user object associated, if any. Use {@link EPDataFlowInstantiationOptions} to associate.
     *
     * @return user object
     */
    public Object getUserObject();

    /**
     * Returns the instance id associated, if any. Use {@link EPDataFlowInstantiationOptions} to associate.
     *
     * @return instance if
     */
    public String getInstanceId();

    /**
     * Returns runtime parameters provided at instantiation time, or null if none have been provided.
     *
     * @return runtime parameters
     */
    public Map<String, Object> getParameters();
}
