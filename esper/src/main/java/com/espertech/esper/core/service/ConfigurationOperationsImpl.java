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

import com.espertech.esper.client.*;
import com.espertech.esper.collection.Pair;
import com.espertech.esper.core.start.EPStatementStartMethod;
import com.espertech.esper.epl.core.engineimport.EngineImportException;
import com.espertech.esper.epl.core.engineimport.EngineImportService;
import com.espertech.esper.epl.core.engineimport.EngineSettingsService;
import com.espertech.esper.epl.expression.core.ExprValidationException;
import com.espertech.esper.epl.metric.MetricReportingService;
import com.espertech.esper.epl.spec.PluggableObjectCollection;
import com.espertech.esper.epl.table.mgmt.TableService;
import com.espertech.esper.epl.util.EPLValidationUtil;
import com.espertech.esper.epl.variable.VariableExistsException;
import com.espertech.esper.epl.variable.VariableReader;
import com.espertech.esper.epl.variable.VariableService;
import com.espertech.esper.epl.variable.VariableTypeException;
import com.espertech.esper.event.EventAdapterException;
import com.espertech.esper.event.EventAdapterService;
import com.espertech.esper.event.EventTypeIdGenerator;
import com.espertech.esper.event.EventTypeUtility;
import com.espertech.esper.event.vaevent.ValueAddEventProcessor;
import com.espertech.esper.event.vaevent.ValueAddEventService;
import com.espertech.esper.event.vaevent.VariantEventType;
import com.espertech.esper.event.xml.SchemaModel;
import com.espertech.esper.event.xml.XSDSchemaMapper;
import com.espertech.esper.filter.FilterService;
import com.espertech.esper.pattern.pool.PatternSubexpressionPoolEngineSvc;
import com.espertech.esper.rowregex.MatchRecognizeStatePoolEngineSvc;
import com.espertech.esper.util.JavaClassHelper;

import java.io.Serializable;
import java.net.URI;
import java.util.*;

/**
 * Provides runtime engine configuration operations.
 */
public class ConfigurationOperationsImpl implements ConfigurationOperations {
    private final EventAdapterService eventAdapterService;
    private final EventTypeIdGenerator eventTypeIdGenerator;
    private final EngineImportService engineImportService;
    private final VariableService variableService;
    private final EngineSettingsService engineSettingsService;
    private final ValueAddEventService valueAddEventService;
    private final MetricReportingService metricReportingService;
    private final StatementEventTypeRef statementEventTypeRef;
    private final StatementVariableRef statementVariableRef;
    private final PluggableObjectCollection plugInViews;
    private final FilterService filterService;
    private final PatternSubexpressionPoolEngineSvc patternSubexpressionPoolSvc;
    private final MatchRecognizeStatePoolEngineSvc matchRecognizeStatePoolEngineSvc;
    private final TableService tableService;
    private final Map<String, Object> transientConfiguration;

    public ConfigurationOperationsImpl(EventAdapterService eventAdapterService,
                                       EventTypeIdGenerator eventTypeIdGenerator,
                                       EngineImportService engineImportService,
                                       VariableService variableService,
                                       EngineSettingsService engineSettingsService,
                                       ValueAddEventService valueAddEventService,
                                       MetricReportingService metricReportingService,
                                       StatementEventTypeRef statementEventTypeRef,
                                       StatementVariableRef statementVariableRef,
                                       PluggableObjectCollection plugInViews,
                                       FilterService filterService,
                                       PatternSubexpressionPoolEngineSvc patternSubexpressionPoolSvc,
                                       MatchRecognizeStatePoolEngineSvc matchRecognizeStatePoolEngineSvc,
                                       TableService tableService,
                                       Map<String, Object> transientConfiguration) {
        this.eventAdapterService = eventAdapterService;
        this.eventTypeIdGenerator = eventTypeIdGenerator;
        this.engineImportService = engineImportService;
        this.variableService = variableService;
        this.engineSettingsService = engineSettingsService;
        this.valueAddEventService = valueAddEventService;
        this.metricReportingService = metricReportingService;
        this.statementEventTypeRef = statementEventTypeRef;
        this.statementVariableRef = statementVariableRef;
        this.plugInViews = plugInViews;
        this.filterService = filterService;
        this.patternSubexpressionPoolSvc = patternSubexpressionPoolSvc;
        this.matchRecognizeStatePoolEngineSvc = matchRecognizeStatePoolEngineSvc;
        this.tableService = tableService;
        this.transientConfiguration = transientConfiguration;
    }

    public void addEventTypeAutoName(String javaPackageName) {
        eventAdapterService.addAutoNamePackage(javaPackageName);
    }

    public void addPlugInView(String namespace, String name, String viewFactoryClass) {
        ConfigurationPlugInView configurationPlugInView = new ConfigurationPlugInView();
        configurationPlugInView.setNamespace(namespace);
        configurationPlugInView.setName(name);
        configurationPlugInView.setFactoryClassName(viewFactoryClass);
        plugInViews.addViews(Collections.singletonList(configurationPlugInView), Collections.<ConfigurationPlugInVirtualDataWindow>emptyList(), engineImportService);
    }

    public void addPlugInAggregationMultiFunction(ConfigurationPlugInAggregationMultiFunction config) throws ConfigurationException {
        try {
            engineImportService.addAggregationMultiFunction(config);
        } catch (EngineImportException e) {
            throw new ConfigurationException(e.getMessage(), e);
        }
    }

    public void addPlugInAggregationFunctionFactory(String functionName, String aggregationFactoryClassName) {
        try {
            ConfigurationPlugInAggregationFunction desc = new ConfigurationPlugInAggregationFunction(functionName, aggregationFactoryClassName);
            engineImportService.addAggregation(functionName, desc);
        } catch (EngineImportException e) {
            throw new ConfigurationException(e.getMessage(), e);
        }
    }

    public void addPlugInSingleRowFunction(String functionName, String className, String methodName) throws ConfigurationException {
        internalAddPlugInSingleRowFunction(functionName, className, methodName, ConfigurationPlugInSingleRowFunction.ValueCache.DISABLED, ConfigurationPlugInSingleRowFunction.FilterOptimizable.ENABLED, false, null);
    }

    public void addPlugInSingleRowFunction(String functionName, String className, String methodName, ConfigurationPlugInSingleRowFunction.ValueCache valueCache) throws ConfigurationException {
        internalAddPlugInSingleRowFunction(functionName, className, methodName, valueCache, ConfigurationPlugInSingleRowFunction.FilterOptimizable.ENABLED, false, null);
    }

    public void addPlugInSingleRowFunction(String functionName, String className, String methodName, ConfigurationPlugInSingleRowFunction.FilterOptimizable filterOptimizable) throws ConfigurationException {
        internalAddPlugInSingleRowFunction(functionName, className, methodName, ConfigurationPlugInSingleRowFunction.ValueCache.DISABLED, filterOptimizable, false, null);
    }

    public void addPlugInSingleRowFunction(String functionName, String className, String methodName, ConfigurationPlugInSingleRowFunction.ValueCache valueCache, ConfigurationPlugInSingleRowFunction.FilterOptimizable filterOptimizable, boolean rethrowExceptions) throws ConfigurationException {
        internalAddPlugInSingleRowFunction(functionName, className, methodName, valueCache, filterOptimizable, rethrowExceptions, null);
    }

    public void addPlugInSingleRowFunction(ConfigurationPlugInSingleRowFunction config) {
        internalAddPlugInSingleRowFunction(config.getName(), config.getFunctionClassName(), config.getFunctionMethodName(), config.getValueCache(), config.getFilterOptimizable(), config.isRethrowExceptions(), config.getEventTypeName());
    }

    private void internalAddPlugInSingleRowFunction(String functionName, String className, String methodName, ConfigurationPlugInSingleRowFunction.ValueCache valueCache, ConfigurationPlugInSingleRowFunction.FilterOptimizable filterOptimizable, boolean rethrowExceptions, String optionalEventTypeName) throws ConfigurationException {
        try {
            engineImportService.addSingleRow(functionName, className, methodName, valueCache, filterOptimizable, rethrowExceptions, optionalEventTypeName);
        } catch (EngineImportException e) {
            throw new ConfigurationException(e.getMessage(), e);
        }
    }

    public void addImport(String importName) {
        try {
            engineImportService.addImport(importName);
        } catch (EngineImportException e) {
            throw new ConfigurationException(e.getMessage(), e);
        }
    }

    public void addAnnotationImport(String importName) {
        try {
            engineImportService.addAnnotationImport(importName);
        } catch (EngineImportException e) {
            throw new ConfigurationException(e.getMessage(), e);
        }
    }

    public void addImport(Class importClass) {
        addImport(importClass.getName());
    }

    public boolean isEventTypeExists(String eventTypeName) {
        return eventAdapterService.getExistsTypeByName(eventTypeName) != null;
    }

    public void addEventType(String eventTypeName, String javaEventClassName) {
        checkTableExists(eventTypeName);
        try {
            eventAdapterService.addBeanType(eventTypeName, javaEventClassName, false, false, true, true);
        } catch (EventAdapterException t) {
            throw new ConfigurationException(t.getMessage(), t);
        }
    }

    public void addEventType(String eventTypeName, Class javaEventClass) {
        checkTableExists(eventTypeName);
        try {
            eventAdapterService.addBeanType(eventTypeName, javaEventClass, false, true, true);
        } catch (EventAdapterException t) {
            throw new ConfigurationException(t.getMessage(), t);
        }
    }

    public void addEventType(Class javaEventClass) {
        checkTableExists(javaEventClass.getSimpleName());
        try {
            eventAdapterService.addBeanType(javaEventClass.getSimpleName(), javaEventClass, false, true, true);
        } catch (EventAdapterException t) {
            throw new ConfigurationException(t.getMessage(), t);
        }
    }

    public void addEventType(String eventTypeName, Properties typeMap) {
        checkTableExists(eventTypeName);

        Map<String, Object> types;
        try {
            types = JavaClassHelper.getClassObjectFromPropertyTypeNames(typeMap, engineImportService.getClassForNameProvider());
        } catch (ClassNotFoundException ex) {
            throw new ConfigurationException("Unable to load class, class not found: " + ex.getMessage(), ex);
        }

        try {
            eventAdapterService.addNestableMapType(eventTypeName, types, null, false, true, true, false, false);
        } catch (EventAdapterException t) {
            throw new ConfigurationException(t.getMessage(), t);
        }
    }

    public void addEventType(String eventTypeName, Map<String, Object> typeMap) {
        checkTableExists(eventTypeName);
        try {
            Map<String, Object> compiledProperties = EventTypeUtility.compileMapTypeProperties(typeMap, eventAdapterService);
            eventAdapterService.addNestableMapType(eventTypeName, compiledProperties, null, false, true, true, false, false);
        } catch (EventAdapterException t) {
            throw new ConfigurationException(t.getMessage(), t);
        }
    }

    public void addEventType(String eventTypeName, Map<String, Object> typeMap, String[] superTypes) throws ConfigurationException {
        checkTableExists(eventTypeName);
        ConfigurationEventTypeMap optionalConfig = null;
        if ((superTypes != null) && (superTypes.length > 0)) {
            optionalConfig = new ConfigurationEventTypeMap();
            optionalConfig.getSuperTypes().addAll(Arrays.asList(superTypes));
        }

        try {
            eventAdapterService.addNestableMapType(eventTypeName, typeMap, optionalConfig, false, true, true, false, false);
        } catch (EventAdapterException t) {
            throw new ConfigurationException(t.getMessage(), t);
        }
    }

    public void addEventType(String eventTypeName, Map<String, Object> typeMap, ConfigurationEventTypeMap mapConfig) throws ConfigurationException {
        checkTableExists(eventTypeName);
        try {
            eventAdapterService.addNestableMapType(eventTypeName, typeMap, mapConfig, false, true, true, false, false);
        } catch (EventAdapterException t) {
            throw new ConfigurationException(t.getMessage(), t);
        }
    }

    public void addEventType(String eventTypeName, String[] propertyNames, Object[] propertyTypes) throws ConfigurationException {
        addEventType(eventTypeName, propertyNames, propertyTypes, null);
    }

    public void addEventType(String eventTypeName, String[] propertyNames, Object[] propertyTypes, ConfigurationEventTypeObjectArray optionalConfiguration) throws ConfigurationException {
        checkTableExists(eventTypeName);
        try {
            LinkedHashMap<String, Object> propertyTypesMap = EventTypeUtility.validateObjectArrayDef(propertyNames, propertyTypes);
            Map<String, Object> compiledProperties = EventTypeUtility.compileMapTypeProperties(propertyTypesMap, eventAdapterService);
            eventAdapterService.addNestableObjectArrayType(eventTypeName, compiledProperties, optionalConfiguration, false, true, true, false, false, false, null);
        } catch (EventAdapterException t) {
            throw new ConfigurationException(t.getMessage(), t);
        }
    }

    public void addEventType(String eventTypeName, ConfigurationEventTypeXMLDOM xmlDOMEventTypeDesc) {
        checkTableExists(eventTypeName);
        SchemaModel schemaModel = null;

        if ((xmlDOMEventTypeDesc.getSchemaResource() != null) || (xmlDOMEventTypeDesc.getSchemaText() != null)) {
            try {
                schemaModel = XSDSchemaMapper.loadAndMap(xmlDOMEventTypeDesc.getSchemaResource(), xmlDOMEventTypeDesc.getSchemaText(), engineImportService);
            } catch (Exception ex) {
                throw new ConfigurationException(ex.getMessage(), ex);
            }
        }

        try {
            eventAdapterService.addXMLDOMType(eventTypeName, xmlDOMEventTypeDesc, schemaModel, false);
        } catch (EventAdapterException t) {
            throw new ConfigurationException(t.getMessage(), t);
        }
    }

    public void addVariable(String variableName, Class type, Object initializationValue) throws ConfigurationException {
        addVariable(variableName, type.getName(), initializationValue, false);
    }

    public void addVariable(String variableName, String eventTypeName, Object initializationValue) throws ConfigurationException {
        addVariable(variableName, eventTypeName, initializationValue, false);
    }

    public void addVariable(String variableName, String type, Object initializationValue, boolean constant) throws ConfigurationException {
        try {
            Pair<String, Boolean> arrayType = JavaClassHelper.isGetArrayType(type);
            variableService.createNewVariable(null, variableName, arrayType.getFirst(), constant, arrayType.getSecond(), false, initializationValue, engineImportService);
            variableService.allocateVariableState(variableName, EPStatementStartMethod.DEFAULT_AGENT_INSTANCE_ID, null, false);
            statementVariableRef.addConfiguredVariable(variableName);
        } catch (VariableExistsException e) {
            throw new ConfigurationException("Error creating variable: " + e.getMessage(), e);
        } catch (VariableTypeException e) {
            throw new ConfigurationException("Error creating variable: " + e.getMessage(), e);
        }
    }

    public void addPlugInEventType(String eventTypeName, URI[] resolutionURIs, Serializable initializer) {
        try {
            eventAdapterService.addPlugInEventType(eventTypeName, resolutionURIs, initializer);
        } catch (EventAdapterException e) {
            throw new ConfigurationException("Error adding plug-in event type: " + e.getMessage(), e);
        }
    }

    public void setPlugInEventTypeResolutionURIs(URI[] urisToResolveName) {
        engineSettingsService.setPlugInEventTypeResolutionURIs(urisToResolveName);
    }

    public void addRevisionEventType(String revisioneventTypeName, ConfigurationRevisionEventType revisionEventTypeConfig) {
        checkTableExists(revisioneventTypeName);
        valueAddEventService.addRevisionEventType(revisioneventTypeName, revisionEventTypeConfig, eventAdapterService);
    }

    public void addVariantStream(String varianteventTypeName, ConfigurationVariantStream variantStreamConfig) {
        checkTableExists(varianteventTypeName);
        valueAddEventService.addVariantStream(varianteventTypeName, variantStreamConfig, eventAdapterService, eventTypeIdGenerator);
    }

    public void updateMapEventType(String mapeventTypeName, Map<String, Object> typeMap) throws ConfigurationException {
        try {
            eventAdapterService.updateMapEventType(mapeventTypeName, typeMap);
        } catch (EventAdapterException e) {
            throw new ConfigurationException("Error updating Map event type: " + e.getMessage(), e);
        }
    }

    public void updateObjectArrayEventType(String objectArrayEventTypeName, String[] propertyNames, Object[] propertyTypes) throws ConfigurationException {
        try {
            LinkedHashMap<String, Object> typeMap = EventTypeUtility.validateObjectArrayDef(propertyNames, propertyTypes);
            eventAdapterService.updateObjectArrayEventType(objectArrayEventTypeName, typeMap);
        } catch (EventAdapterException e) {
            throw new ConfigurationException("Error updating Object-array event type: " + e.getMessage(), e);
        }
    }

    public void replaceXMLEventType(String xmlEventTypeName, ConfigurationEventTypeXMLDOM config) throws ConfigurationException {
        SchemaModel schemaModel = null;
        if (config.getSchemaResource() != null || config.getSchemaText() != null) {
            try {
                schemaModel = XSDSchemaMapper.loadAndMap(config.getSchemaResource(), config.getSchemaText(), engineImportService);
            } catch (Exception ex) {
                throw new ConfigurationException(ex.getMessage(), ex);
            }
        }

        try {
            eventAdapterService.replaceXMLEventType(xmlEventTypeName, config, schemaModel);
        } catch (EventAdapterException e) {
            throw new ConfigurationException("Error updating XML event type: " + e.getMessage(), e);
        }
    }

    public void setMetricsReportingInterval(String stmtGroupName, long newInterval) {
        try {
            metricReportingService.setMetricsReportingInterval(stmtGroupName, newInterval);
        } catch (RuntimeException e) {
            throw new ConfigurationException("Error updating interval for metric reporting: " + e.getMessage(), e);
        }
    }


    public void setMetricsReportingStmtEnabled(String statementName) {
        try {
            metricReportingService.setMetricsReportingStmtEnabled(statementName);
        } catch (RuntimeException e) {
            throw new ConfigurationException("Error enabling metric reporting for statement: " + e.getMessage(), e);
        }
    }

    public void setMetricsReportingStmtDisabled(String statementName) {
        try {
            metricReportingService.setMetricsReportingStmtDisabled(statementName);
        } catch (RuntimeException e) {
            throw new ConfigurationException("Error enabling metric reporting for statement: " + e.getMessage(), e);
        }
    }

    public void setMetricsReportingEnabled() {
        try {
            metricReportingService.setMetricsReportingEnabled();
        } catch (RuntimeException e) {
            throw new ConfigurationException("Error enabling metric reporting: " + e.getMessage(), e);
        }
    }

    public void setMetricsReportingDisabled() {
        try {
            metricReportingService.setMetricsReportingDisabled();
        } catch (RuntimeException e) {
            throw new ConfigurationException("Error enabling metric reporting: " + e.getMessage(), e);
        }
    }

    public boolean isVariantStreamExists(String name) {
        ValueAddEventProcessor processor = valueAddEventService.getValueAddProcessor(name);
        if (processor == null) {
            return false;
        }
        return processor.getValueAddEventType() instanceof VariantEventType;
    }

    public boolean removeEventType(String name, boolean force) throws ConfigurationException {
        if (!force) {
            Set<String> statements = statementEventTypeRef.getStatementNamesForType(name);
            if ((statements != null) && (!statements.isEmpty())) {
                throw new ConfigurationException("Event type '" + name + "' is in use by one or more statements");
            }
        }

        EventType type = eventAdapterService.getExistsTypeByName(name);
        if (type == null) {
            return false;
        }

        eventAdapterService.removeType(name);
        statementEventTypeRef.removeReferencesType(name);
        filterService.removeType(type);
        return true;
    }

    public boolean removeVariable(String name, boolean force) throws ConfigurationException {
        if (!force) {
            Set<String> statements = statementVariableRef.getStatementNamesForVar(name);
            if ((statements != null) && (!statements.isEmpty())) {
                throw new ConfigurationException("Variable '" + name + "' is in use by one or more statements");
            }
        }

        VariableReader reader = variableService.getReader(name, EPStatementStartMethod.DEFAULT_AGENT_INSTANCE_ID);
        if (reader == null) {
            return false;
        }

        variableService.removeVariableIfFound(name);
        statementVariableRef.removeReferencesVariable(name);
        statementVariableRef.removeConfiguredVariable(name);
        return true;
    }

    public Set<String> getEventTypeNameUsedBy(String name) {
        Set<String> statements = statementEventTypeRef.getStatementNamesForType(name);
        if ((statements == null) || (statements.isEmpty())) {
            return Collections.emptySet();
        }
        return Collections.unmodifiableSet(statements);
    }

    public Set<String> getVariableNameUsedBy(String variableName) {
        Set<String> statements = statementVariableRef.getStatementNamesForVar(variableName);
        if ((statements == null) || (statements.isEmpty())) {
            return Collections.emptySet();
        }
        return Collections.unmodifiableSet(statements);
    }

    public EventType getEventType(String eventTypeName) {
        return eventAdapterService.getExistsTypeByName(eventTypeName);
    }

    public EventType[] getEventTypes() {
        return eventAdapterService.getAllTypes();
    }

    public void addEventType(String eventTypeName, String eventClass, ConfigurationEventTypeLegacy legacyEventTypeDesc) {
        checkTableExists(eventTypeName);
        try {
            Map<String, ConfigurationEventTypeLegacy> map = new HashMap<String, ConfigurationEventTypeLegacy>();
            map.put(eventClass, legacyEventTypeDesc);
            eventAdapterService.setClassLegacyConfigs(map);
            eventAdapterService.addBeanType(eventTypeName, eventClass, false, false, false, true);
        } catch (EventAdapterException ex) {
            throw new ConfigurationException("Failed to add legacy event type definition for type '" + eventTypeName + "': " + ex.getMessage(), ex);
        }
    }

    private void checkTableExists(String eventTypeName) {
        try {
            EPLValidationUtil.validateTableExists(tableService, eventTypeName);
        } catch (ExprValidationException ex) {
            throw new ConfigurationException(ex.getMessage(), ex);
        }
    }

    public void setPatternMaxSubexpressions(Long maxSubexpressions) {
        if (patternSubexpressionPoolSvc != null) {
            patternSubexpressionPoolSvc.setPatternMaxSubexpressions(maxSubexpressions);
        }
    }

    public void setMatchRecognizeMaxStates(Long maxStates) {
        if (matchRecognizeStatePoolEngineSvc != null) {
            matchRecognizeStatePoolEngineSvc.setMatchRecognizeMaxStates(maxStates);
        }
    }

    public Map<String, Object> getTransientConfiguration() {
        return transientConfiguration;
    }

    public void addEventTypeAvro(String eventTypeName, ConfigurationEventTypeAvro avro) {
        checkTableExists(eventTypeName);
        try {
            eventAdapterService.addAvroType(eventTypeName, avro, false, true, true, false, false);
        } catch (EventAdapterException t) {
            throw new ConfigurationException(t.getMessage(), t);
        }
    }
}
