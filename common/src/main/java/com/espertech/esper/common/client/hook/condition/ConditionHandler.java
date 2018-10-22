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
package com.espertech.esper.common.client.hook.condition;

/**
 * Interface for a handler registered with a runtime instance to receive reported runtime conditions.
 */
public interface ConditionHandler {
    /**
     * Handle the runtimecondition as contained in the context object passed.
     *
     * @param context the condition information
     */
    public void handle(ConditionHandlerContext context);
}
