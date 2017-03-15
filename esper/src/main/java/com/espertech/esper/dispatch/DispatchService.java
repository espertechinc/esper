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
package com.espertech.esper.dispatch;

/**
 * Service for dispatching internally (for operators/views processing results of prior operators/views) and
 * externally (dispatch events to UpdateListener implementations).
 * The service accepts Dispatchable implementations to its internal and external lists.
 * When a client invokes dispatch the implementation first invokes all internal Dispatchable
 * instances then all external Dispatchable instances. Dispatchables are invoked
 * in the same order they are added. Any dispatchable added twice is dispatched once.
 * <p>Note: Each execution thread owns its own dispatch queue.</p>
 * <p>
 * Note: Dispatchs could result in further call to the dispatch service. This is because listener code
 * that is invoked as a result of a dispatch may create patterns that fireStatementStopped as soon as they are started
 * resulting in further dispatches within the same thread. Thus the implementation class must be careful
 * with the use of iterators to avoid ConcurrentModificationException errors.
 * </p>
 */
public interface DispatchService {
    /**
     * Add a Dispatchable implementation.
     *
     * @param dispatchable to execute later
     */
    public void addExternal(Dispatchable dispatchable);

    /**
     * Execute all Dispatchable implementations added to the service since the last invocation of this method.
     */
    public void dispatch();

}
