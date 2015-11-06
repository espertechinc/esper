/*
 * *************************************************************************************
 *  Copyright (C) 2006-2015 EsperTech, Inc. All rights reserved.                       *
 *  http://www.espertech.com/esper                                                     *
 *  http://www.espertech.com                                                           *
 *  ---------------------------------------------------------------------------------- *
 *  The software in this package is published under the terms of the GPL license       *
 *  a copy of which has been included with this distribution in the license.txt file.  *
 * *************************************************************************************
 */

package com.espertech.esper.filter;

import com.espertech.esper.client.ConfigurationInformation;
import com.espertech.esper.client.EventType;
import com.espertech.esper.collection.Pair;
import com.espertech.esper.core.context.util.ContextDescriptor;
import com.espertech.esper.epl.core.MethodResolutionService;
import com.espertech.esper.epl.core.StreamTypeService;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.epl.table.mgmt.TableService;
import com.espertech.esper.epl.variable.VariableService;
import com.espertech.esper.event.EventAdapterService;
import com.espertech.esper.schedule.TimeProvider;

import java.lang.annotation.Annotation;
import java.util.LinkedHashMap;

public class FilterSpecCompilerArgs {

    public final LinkedHashMap<String, Pair<EventType, String>> taggedEventTypes;
    public final LinkedHashMap<String, Pair<EventType, String>> arrayEventTypes;
    public final ExprEvaluatorContext exprEvaluatorContext;
    public final String statementName;
    public final String statementId;
    public final StreamTypeService streamTypeService;
    public final MethodResolutionService methodResolutionService;
    public final TimeProvider timeProvider;
    public final VariableService variableService;
    public final TableService tableService;
    public final EventAdapterService eventAdapterService;
    public final FilterBooleanExpressionFactory filterBooleanExpressionFactory;
    public final Annotation[] annotations;
    public final ContextDescriptor contextDescriptor;
    public final ConfigurationInformation configurationInformation;

    public FilterSpecCompilerArgs(LinkedHashMap<String, Pair<EventType, String>> taggedEventTypes, LinkedHashMap<String, Pair<EventType, String>> arrayEventTypes, ExprEvaluatorContext exprEvaluatorContext, String statementName, String statementId, StreamTypeService streamTypeService, MethodResolutionService methodResolutionService, TimeProvider timeProvider, VariableService variableService, TableService tableService, EventAdapterService eventAdapterService, FilterBooleanExpressionFactory filterBooleanExpressionFactory, Annotation[] annotations, ContextDescriptor contextDescriptor, ConfigurationInformation configurationInformation) {
        this.taggedEventTypes = taggedEventTypes;
        this.arrayEventTypes = arrayEventTypes;
        this.exprEvaluatorContext = exprEvaluatorContext;
        this.statementName = statementName;
        this.statementId = statementId;
        this.streamTypeService = streamTypeService;
        this.methodResolutionService = methodResolutionService;
        this.timeProvider = timeProvider;
        this.variableService = variableService;
        this.tableService = tableService;
        this.eventAdapterService = eventAdapterService;
        this.filterBooleanExpressionFactory = filterBooleanExpressionFactory;
        this.annotations = annotations;
        this.contextDescriptor = contextDescriptor;
        this.configurationInformation = configurationInformation;
    }
}
