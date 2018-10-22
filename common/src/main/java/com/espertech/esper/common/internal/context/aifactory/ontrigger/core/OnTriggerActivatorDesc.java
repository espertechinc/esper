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
package com.espertech.esper.common.internal.context.aifactory.ontrigger.core;

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.context.activator.ViewableActivatorForge;

public class OnTriggerActivatorDesc {
    private final ViewableActivatorForge activator;
    private final String triggerEventTypeName;
    private final EventType activatorResultEventType;

    public OnTriggerActivatorDesc(ViewableActivatorForge activator, String triggerEventTypeName, EventType activatorResultEventType) {
        this.activator = activator;
        this.triggerEventTypeName = triggerEventTypeName;
        this.activatorResultEventType = activatorResultEventType;
    }

    public ViewableActivatorForge getActivator() {
        return activator;
    }

    public String getTriggerEventTypeName() {
        return triggerEventTypeName;
    }

    public EventType getActivatorResultEventType() {
        return activatorResultEventType;
    }
}
