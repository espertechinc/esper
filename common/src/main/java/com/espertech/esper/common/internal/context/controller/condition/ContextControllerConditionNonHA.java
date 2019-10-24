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
package com.espertech.esper.common.internal.context.controller.condition;

import com.espertech.esper.common.client.EventBean;

import java.util.Map;

public interface ContextControllerConditionNonHA extends ContextControllerCondition {
    boolean activate(EventBean optionalTriggeringEvent, ContextControllerEndConditionMatchEventProvider endConditionMatchEventProvider, Map<String, Object> optionalTriggeringPattern);

    void deactivate();

    boolean isRunning();
}
