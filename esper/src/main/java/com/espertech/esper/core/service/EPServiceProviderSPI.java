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
package com.espertech.esper.core.service;

import com.espertech.esper.client.Configuration;
import com.espertech.esper.client.ConfigurationInformation;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.core.context.mgr.ContextManagementService;
import com.espertech.esper.core.deploy.DeploymentStateService;
import com.espertech.esper.core.thread.ThreadingService;
import com.espertech.esper.epl.core.engineimport.EngineImportService;
import com.espertech.esper.epl.metric.MetricReportingService;
import com.espertech.esper.epl.named.NamedWindowMgmtService;
import com.espertech.esper.epl.table.mgmt.TableService;
import com.espertech.esper.epl.variable.VariableService;
import com.espertech.esper.event.EventAdapterService;
import com.espertech.esper.event.vaevent.ValueAddEventService;
import com.espertech.esper.filter.FilterService;
import com.espertech.esper.schedule.SchedulingMgmtService;
import com.espertech.esper.schedule.SchedulingService;
import com.espertech.esper.schedule.TimeProvider;
import com.espertech.esper.timer.TimerService;

import javax.naming.Context;

/**
 * A service provider interface that makes available internal engine services.
 */
public interface EPServiceProviderSPI extends EPServiceProvider {
    /**
     * Returns statement management service for the engine.
     *
     * @return the StatementLifecycleSvc
     */
    public StatementLifecycleSvc getStatementLifecycleSvc();

    /**
     * Get the EventAdapterService for this engine.
     *
     * @return the EventAdapterService
     */
    public EventAdapterService getEventAdapterService();

    /**
     * Get the SchedulingService for this engine.
     *
     * @return the SchedulingService
     */
    public SchedulingService getSchedulingService();

    /**
     * Get the SchedulingMgmtService for this engine.
     *
     * @return the SchedulingMgmtService
     */
    public SchedulingMgmtService getSchedulingMgmtService();

    /**
     * Returns the filter service.
     *
     * @return filter service
     */
    public FilterService getFilterService();

    /**
     * Returns the timer service.
     *
     * @return timer service
     */
    public TimerService getTimerService();

    /**
     * Returns the named window service.
     *
     * @return named window service
     */
    public NamedWindowMgmtService getNamedWindowMgmtService();

    /**
     * Returns the table service.
     *
     * @return table service
     */
    public TableService getTableService();

    /**
     * Returns the current configuration.
     *
     * @return configuration information
     */
    public ConfigurationInformation getConfigurationInformation();

    /**
     * Returns the engine environment context for engine-external resources such as adapters.
     *
     * @return engine environment context
     */
    public Context getContext();

    /**
     * Returns the extension services context.
     *
     * @return extension services context
     */
    public EngineLevelExtensionServicesContext getExtensionServicesContext();

    /**
     * Returns metrics reporting.
     *
     * @return metrics reporting
     */
    public MetricReportingService getMetricReportingService();

    /**
     * Returns variable services.
     *
     * @return services
     */
    public VariableService getVariableService();

    /**
     * Returns value-added type service.
     *
     * @return value types
     */
    public ValueAddEventService getValueAddEventService();

    /**
     * Returns statement event type reference service.
     *
     * @return statement-type reference service
     */
    public StatementEventTypeRef getStatementEventTypeRef();

    /**
     * Returns threading service for the engine.
     *
     * @return the ThreadingService
     */
    public ThreadingService getThreadingService();

    /**
     * Returns engine environment context such as plugin loader references.
     *
     * @return environment context
     */
    public EngineEnvContext getEngineEnvContext();

    /**
     * Returns services.
     *
     * @return services
     */
    public EPServicesContext getServicesContext();

    /**
     * Returns context factory.
     *
     * @return factory
     */
    public StatementContextFactory getStatementContextFactory();

    /**
     * Returns engine imports.
     *
     * @return engine imports
     */
    public EngineImportService getEngineImportService();

    /**
     * Returns time provider.
     *
     * @return time provider
     */
    public TimeProvider getTimeProvider();

    public StatementIsolationService getStatementIsolationService();

    public DeploymentStateService getDeploymentStateService();

    public ContextManagementService getContextManagementService();

    public void setConfiguration(Configuration configuration);

    public void postInitialize();

    public void initialize(Long currentTime);
}
