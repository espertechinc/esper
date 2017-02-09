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
package com.espertech.esper.client.hook;

/**
 * Factory for engine condition handler instance(s).
 * <p>
 * Receives CEP engine contextual information and should return an implementation of the
 * {@link ConditionHandler} interface.
 */
public interface ConditionHandlerFactory {

    /**
     * Returns an exception handler instances, or null if the factory decided not to contribute an exception handler.
     *
     * @param context contains the engine URI
     * @return exception handler
     */
    public ConditionHandler getHandler(ConditionHandlerFactoryContext context);
}
