/**************************************************************************************
 * Copyright (C) 2006-2015 EsperTech Inc. All rights reserved.                        *
 * http://www.espertech.com/esper                                                          *
 * http://www.espertech.com                                                           *
 * ---------------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the GPL license       *
 * a copy of which has been included with this distribution in the license.txt file.  *
 **************************************************************************************/
package com.espertech.esper.core.service;

import com.espertech.esper.client.ConfigurationInformation;
import com.espertech.esper.epl.core.EngineSettingsService;
import com.espertech.esper.epl.metric.MetricReportingServiceSPI;
import com.espertech.esper.epl.named.NamedWindowService;
import com.espertech.esper.epl.table.mgmt.TableExprEvaluatorContext;
import com.espertech.esper.epl.table.mgmt.TableService;
import com.espertech.esper.epl.variable.VariableService;
import com.espertech.esper.event.EventAdapterService;
import com.espertech.esper.event.vaevent.ValueAddEventService;
import com.espertech.esper.view.ViewService;

import java.net.URI;

public final class StatementContextEngineServices
{
    private final String engineURI;
    private final EventAdapterService eventAdapterService;
    private final NamedWindowService namedWindowService;
    private final VariableService variableService;
    private final TableService tableService;
    private final EngineSettingsService engineSettingsService;
    private final ValueAddEventService valueAddEventService;
    private final ConfigurationInformation configSnapshot;
    private final MetricReportingServiceSPI metricReportingService;
    private final ViewService viewService;
    private final ExceptionHandlingService exceptionHandlingService;
    private final ExpressionResultCacheService expressionResultCacheService;
    private final StatementEventTypeRef statementEventTypeRef;
    private final TableExprEvaluatorContext tableExprEvaluatorContext;
    private final ExtensionServicesContext extensionServicesContext;

    public StatementContextEngineServices(String engineURI, EventAdapterService eventAdapterService, NamedWindowService namedWindowService, VariableService variableService, TableService tableService, EngineSettingsService engineSettingsService, ValueAddEventService valueAddEventService, ConfigurationInformation configSnapshot, MetricReportingServiceSPI metricReportingService, ViewService viewService, ExceptionHandlingService exceptionHandlingService, ExpressionResultCacheService expressionResultCacheService, StatementEventTypeRef statementEventTypeRef, TableExprEvaluatorContext tableExprEvaluatorContext, ExtensionServicesContext extensionServicesContext) {
        this.engineURI = engineURI;
        this.eventAdapterService = eventAdapterService;
        this.namedWindowService = namedWindowService;
        this.variableService = variableService;
        this.tableService = tableService;
        this.engineSettingsService = engineSettingsService;
        this.valueAddEventService = valueAddEventService;
        this.configSnapshot = configSnapshot;
        this.metricReportingService = metricReportingService;
        this.viewService = viewService;
        this.exceptionHandlingService = exceptionHandlingService;
        this.expressionResultCacheService = expressionResultCacheService;
        this.statementEventTypeRef = statementEventTypeRef;
        this.tableExprEvaluatorContext = tableExprEvaluatorContext;
        this.extensionServicesContext = extensionServicesContext;
    }

    public String getEngineURI() {
        return engineURI;
    }

    public EventAdapterService getEventAdapterService() {
        return eventAdapterService;
    }

    public NamedWindowService getNamedWindowService() {
        return namedWindowService;
    }

    public VariableService getVariableService() {
        return variableService;
    }

    public URI[] getPlugInTypeResolutionURIs() {
        return engineSettingsService.getPlugInEventTypeResolutionURIs();
    }

    public ValueAddEventService getValueAddEventService() {
        return valueAddEventService;
    }

    public ConfigurationInformation getConfigSnapshot() {
        return configSnapshot;
    }

    public MetricReportingServiceSPI getMetricReportingService() {
        return metricReportingService;
    }

    public ViewService getViewService() {
        return viewService;
    }

    public ExceptionHandlingService getExceptionHandlingService() {
        return exceptionHandlingService;
    }

    public ExpressionResultCacheService getExpressionResultCacheService() {
        return expressionResultCacheService;
    }

    public StatementEventTypeRef getStatementEventTypeRef() {
        return statementEventTypeRef;
    }

    public TableService getTableService() {
        return tableService;
    }

    public TableExprEvaluatorContext getTableExprEvaluatorContext() {
        return tableExprEvaluatorContext;
    }

    public ExtensionServicesContext getExtensionServicesContext() {
        return extensionServicesContext;
    }
}
