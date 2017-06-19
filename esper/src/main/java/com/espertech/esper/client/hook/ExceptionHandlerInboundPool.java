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
 * Extends the {@link ExceptionHandler} with a method to, for the inbound-pool threading configuration,
 * handle exceptions that are not associated to a specific statement i.e. sharable-filter processing exceptions.
 */
public interface ExceptionHandlerInboundPool extends ExceptionHandler {

    /**
     * For use with inbound-thread-pool only, when the engine evaluates events as shared filters
     * and not associated to any statements, the engine passes the exception to this method.
     * @param context the exception information
     */
    public void handleInboundPoolUnassociated(ExceptionHandlerContextUnassociated context);
}
