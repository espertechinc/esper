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
 * Interface for a handler registered with an engine instance to receive reported engine conditions.
 */
public interface ConditionHandler {
    /**
     * Handle the engine condition as contained in the context object passed.
     *
     * @param context the condition information
     */
    public void handle(ConditionHandlerContext context);
}
