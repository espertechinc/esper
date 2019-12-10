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
package com.espertech.esper.runtime.internal.kernel.service;

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.compile.stage1.spec.ExpressionDeclItem;
import com.espertech.esper.common.internal.compile.stage1.spec.ExpressionScriptProvided;
import com.espertech.esper.common.internal.context.aifactory.core.ModuleIncidentals;
import com.espertech.esper.common.internal.context.compile.ContextMetaData;
import com.espertech.esper.common.internal.context.module.ModuleIndexMeta;
import com.espertech.esper.common.internal.epl.namedwindow.path.NamedWindowMetaData;
import com.espertech.esper.common.internal.epl.script.core.NameAndParamNum;
import com.espertech.esper.common.internal.epl.table.compiletime.TableMetaData;
import com.espertech.esper.common.internal.epl.variable.compiletime.VariableMetaData;
import com.espertech.esper.common.internal.event.bean.service.BeanEventTypeFactoryPrivate;
import com.espertech.esper.common.internal.event.path.EventTypeCollectedSerde;
import com.espertech.esper.common.internal.event.path.EventTypeResolverImpl;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class DeployerModuleEPLObjects {
    private final BeanEventTypeFactoryPrivate beanEventTypeFactory;
    private final Map<String, EventType> moduleEventTypes;
    private final Map<String, NamedWindowMetaData> moduleNamedWindows;
    private final Map<String, TableMetaData> moduleTables;
    private final Set<ModuleIndexMeta> moduleIndexes;
    private final Map<String, ContextMetaData> moduleContexts;
    private final Map<String, VariableMetaData> moduleVariables;
    private final Map<String, ExpressionDeclItem> moduleExpressions;
    private final Map<NameAndParamNum, ExpressionScriptProvided> moduleScripts;
    private final List<EventTypeCollectedSerde> eventTypeSerdes;
    private final EventTypeResolverImpl eventTypeResolver;

    public DeployerModuleEPLObjects(BeanEventTypeFactoryPrivate beanEventTypeFactory, Map<String, EventType> moduleEventTypes, Map<String, NamedWindowMetaData> moduleNamedWindows, Map<String, TableMetaData> moduleTables, Set<ModuleIndexMeta> moduleIndexes, Map<String, ContextMetaData> moduleContexts, Map<String, VariableMetaData> moduleVariables, Map<String, ExpressionDeclItem> moduleExpressions, Map<NameAndParamNum, ExpressionScriptProvided> moduleScripts, List<EventTypeCollectedSerde> eventTypeSerdes, EventTypeResolverImpl eventTypeResolver) {
        this.beanEventTypeFactory = beanEventTypeFactory;
        this.moduleEventTypes = moduleEventTypes;
        this.moduleNamedWindows = moduleNamedWindows;
        this.moduleTables = moduleTables;
        this.moduleIndexes = moduleIndexes;
        this.moduleContexts = moduleContexts;
        this.moduleVariables = moduleVariables;
        this.moduleExpressions = moduleExpressions;
        this.moduleScripts = moduleScripts;
        this.eventTypeSerdes = eventTypeSerdes;
        this.eventTypeResolver = eventTypeResolver;
    }

    public BeanEventTypeFactoryPrivate getBeanEventTypeFactory() {
        return beanEventTypeFactory;
    }

    public Map<String, EventType> getModuleEventTypes() {
        return moduleEventTypes;
    }

    public Map<String, NamedWindowMetaData> getModuleNamedWindows() {
        return moduleNamedWindows;
    }

    public Map<String, TableMetaData> getModuleTables() {
        return moduleTables;
    }

    public Set<ModuleIndexMeta> getModuleIndexes() {
        return moduleIndexes;
    }

    public Map<String, ContextMetaData> getModuleContexts() {
        return moduleContexts;
    }

    public Map<String, VariableMetaData> getModuleVariables() {
        return moduleVariables;
    }

    public Map<String, ExpressionDeclItem> getModuleExpressions() {
        return moduleExpressions;
    }

    public Map<NameAndParamNum, ExpressionScriptProvided> getModuleScripts() {
        return moduleScripts;
    }

    public List<EventTypeCollectedSerde> getEventTypeSerdes() {
        return eventTypeSerdes;
    }

    public ModuleIncidentals getIncidentals() {
        return new ModuleIncidentals(moduleNamedWindows, moduleContexts, moduleVariables, moduleExpressions, moduleTables);
    }

    public EventTypeResolverImpl getEventTypeResolver() {
        return eventTypeResolver;
    }
}
