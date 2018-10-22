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
package com.espertech.esper.common.internal.epl.namedwindow.core;

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.client.configuration.runtime.ConfigurationRuntimeThreading;
import com.espertech.esper.common.internal.context.module.EPStatementInitServices;
import com.espertech.esper.common.internal.context.util.StatementResultService;
import com.espertech.esper.common.internal.epl.namedwindow.consume.NamedWindowDispatchService;
import com.espertech.esper.common.internal.schedule.TimeSourceService;

/**
 * This view is hooked into a named window's view chain as the last view and handles dispatching of named window
 * insert and remove stream results via {@link NamedWindowManagementService} to consuming statements.
 */
public abstract class NamedWindowTailViewBase implements NamedWindowTailView {
    protected final EventType eventType;
    protected final NamedWindowManagementService namedWindowManagementService;
    protected final NamedWindowDispatchService namedWindowDispatchService;
    protected final StatementResultService statementResultService;
    protected final boolean isPrioritized;
    protected final boolean isParentBatchWindow;
    protected final TimeSourceService timeSourceService;
    protected final ConfigurationRuntimeThreading threadingConfig;

    public NamedWindowTailViewBase(EventType eventType, boolean isParentBatchWindow, EPStatementInitServices services) {
        this.eventType = eventType;
        this.namedWindowManagementService = services.getNamedWindowManagementService();
        this.namedWindowDispatchService = services.getNamedWindowDispatchService();
        this.statementResultService = services.getStatementResultService();
        this.isPrioritized = services.getRuntimeSettingsService().getConfigurationRuntime().getExecution().isPrioritized();
        this.isParentBatchWindow = isParentBatchWindow;
        this.threadingConfig = services.getRuntimeSettingsService().getConfigurationRuntime().getThreading();
        this.timeSourceService = services.getTimeSourceService();
    }

    public boolean isParentBatchWindow() {
        return isParentBatchWindow;
    }

    public EventType getEventType() {
        return eventType;
    }

    public StatementResultService getStatementResultService() {
        return statementResultService;
    }

    public NamedWindowManagementService getNamedWindowManagementService() {
        return namedWindowManagementService;
    }

    public boolean isPrioritized() {
        return isPrioritized;
    }
}
