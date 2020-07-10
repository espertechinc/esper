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
package com.espertech.esper.common.internal.context.controller.core;

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.client.type.EPTypeClass;
import com.espertech.esper.common.client.util.StateMgmtSetting;

public class ContextDefinition {
    public final static EPTypeClass EPTYPE = new EPTypeClass(ContextDefinition.class);

    private String contextName;
    private ContextControllerFactory[] controllerFactories;
    private EventType eventTypeContextProperties;
    private StateMgmtSetting partitionIdSvcStateMgmtSettings;

    public String getContextName() {
        return contextName;
    }

    public void setContextName(String contextName) {
        this.contextName = contextName;
    }

    public ContextControllerFactory[] getControllerFactories() {
        return controllerFactories;
    }

    public void setControllerFactories(ContextControllerFactory[] controllerFactories) {
        this.controllerFactories = controllerFactories;
    }

    public void setEventTypeContextProperties(EventType eventTypeContextProperties) {
        this.eventTypeContextProperties = eventTypeContextProperties;
    }

    public EventType getEventTypeContextProperties() {
        return eventTypeContextProperties;
    }

    public StateMgmtSetting getPartitionIdSvcStateMgmtSettings() {
        return partitionIdSvcStateMgmtSettings;
    }

    public void setPartitionIdSvcStateMgmtSettings(StateMgmtSetting partitionIdSvcStateMgmtSettings) {
        this.partitionIdSvcStateMgmtSettings = partitionIdSvcStateMgmtSettings;
    }
}
