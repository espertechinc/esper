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

import com.espertech.esper.common.client.EPException;
import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.compile.stage1.spec.ExpressionDeclItem;
import com.espertech.esper.common.internal.compile.stage1.spec.ExpressionScriptProvided;
import com.espertech.esper.common.internal.context.compile.ContextCollector;
import com.espertech.esper.common.internal.context.compile.ContextCollectorImpl;
import com.espertech.esper.common.internal.context.compile.ContextMetaData;
import com.espertech.esper.common.internal.context.module.*;
import com.espertech.esper.common.internal.epl.expression.declared.runtime.ExprDeclaredCollectorRuntime;
import com.espertech.esper.common.internal.epl.index.base.IndexCollectorRuntime;
import com.espertech.esper.common.internal.epl.namedwindow.path.NamedWindowCollector;
import com.espertech.esper.common.internal.epl.namedwindow.path.NamedWindowCollectorImpl;
import com.espertech.esper.common.internal.epl.namedwindow.path.NamedWindowMetaData;
import com.espertech.esper.common.internal.epl.script.core.NameAndParamNum;
import com.espertech.esper.common.internal.epl.script.core.ScriptCollectorRuntime;
import com.espertech.esper.common.internal.epl.table.compiletime.TableMetaData;
import com.espertech.esper.common.internal.epl.table.core.TableCollectorImpl;
import com.espertech.esper.common.internal.epl.variable.compiletime.VariableMetaData;
import com.espertech.esper.common.internal.epl.variable.core.VariableCollector;
import com.espertech.esper.common.internal.epl.variable.core.VariableCollectorImpl;
import com.espertech.esper.common.internal.event.bean.service.BeanEventTypeFactoryPrivate;
import com.espertech.esper.common.internal.event.core.EventBeanTypedEventFactoryRuntime;
import com.espertech.esper.common.internal.event.json.compiletime.JsonEventTypeUtility;
import com.espertech.esper.common.internal.event.path.EventTypeCollectorImpl;
import com.espertech.esper.common.internal.event.path.EventTypeResolverImpl;

import java.util.*;

public class DeployerHelperInitializeEPLObjects {
    public static DeployerModuleEPLObjects initializeEPLObjects(ModuleProviderCLPair provider, String deploymentId, EPServicesContext services) {
        // keep protected types
        BeanEventTypeFactoryPrivate beanEventTypeFactory = new BeanEventTypeFactoryPrivate(new EventBeanTypedEventFactoryRuntime(services.getEventTypeAvroHandler()), services.getEventTypeFactory(), services.getBeanEventTypeStemService());

        // initialize module event types
        Map<String, EventType> moduleEventTypes = new LinkedHashMap<>();
        EventTypeResolverImpl eventTypeResolver = new EventTypeResolverImpl(moduleEventTypes, services.getEventTypePathRegistry(), services.getEventTypeRepositoryBus(), services.getBeanEventTypeFactoryPrivate(), services.getEventSerdeFactory());
        EventTypeCollectorImpl eventTypeCollector = new EventTypeCollectorImpl(moduleEventTypes, beanEventTypeFactory, provider.getClassLoader(), services.getEventTypeFactory(), services.getBeanEventTypeStemService(), eventTypeResolver, services.getXmlFragmentEventTypeFactory(), services.getEventTypeAvroHandler(), services.getEventBeanTypedEventFactory(), services.getClasspathImportServiceRuntime());
        try {
            provider.getModuleProvider().initializeEventTypes(new EPModuleEventTypeInitServicesImpl(eventTypeCollector, eventTypeResolver));
        } catch (Throwable e) {
            throw new EPException(e);
        }
        JsonEventTypeUtility.addJsonUnderlyingClass(moduleEventTypes, services.getClassLoaderParent(), deploymentId);

        // initialize module named windows
        Map<String, NamedWindowMetaData> moduleNamedWindows = new HashMap<>();
        NamedWindowCollector namedWindowCollector = new NamedWindowCollectorImpl(moduleNamedWindows);
        try {
            provider.getModuleProvider().initializeNamedWindows(new EPModuleNamedWindowInitServicesImpl(namedWindowCollector, eventTypeResolver));
        } catch (Throwable e) {
            throw new EPException(e);
        }

        // initialize module tables
        Map<String, TableMetaData> moduleTables = new HashMap<>();
        TableCollectorImpl tableCollector = new TableCollectorImpl(moduleTables);
        try {
            provider.getModuleProvider().initializeTables(new EPModuleTableInitServicesImpl(tableCollector, eventTypeResolver));
        } catch (Throwable e) {
            throw new EPException(e);
        }

        // initialize create-index indexes
        Set<ModuleIndexMeta> moduleIndexes = new HashSet<>();
        IndexCollectorRuntime indexCollector = new IndexCollectorRuntime(moduleIndexes);
        try {
            provider.getModuleProvider().initializeIndexes(new EPModuleIndexInitServicesImpl(indexCollector));
        } catch (Throwable e) {
            throw new EPException(e);
        }

        // initialize module contexts
        Map<String, ContextMetaData> moduleContexts = new HashMap<>();
        ContextCollector contextCollector = new ContextCollectorImpl(moduleContexts);
        try {
            provider.getModuleProvider().initializeContexts(new EPModuleContextInitServicesImpl(contextCollector, eventTypeResolver));
        } catch (Throwable e) {
            throw new EPException(e);
        }

        // initialize module variables
        Map<String, VariableMetaData> moduleVariables = new HashMap<>();
        VariableCollector variableCollector = new VariableCollectorImpl(moduleVariables);
        try {
            provider.getModuleProvider().initializeVariables(new EPModuleVariableInitServicesImpl(variableCollector, eventTypeResolver));
        } catch (Throwable e) {
            throw new EPException(e);
        }

        // initialize module expressions
        Map<String, ExpressionDeclItem> moduleExpressions = new HashMap<>();
        ExprDeclaredCollectorRuntime exprDeclaredCollector = new ExprDeclaredCollectorRuntime(moduleExpressions);
        try {
            provider.getModuleProvider().initializeExprDeclareds(new EPModuleExprDeclaredInitServicesImpl(exprDeclaredCollector));
        } catch (Throwable e) {
            throw new EPException(e);
        }

        // initialize module scripts
        Map<NameAndParamNum, ExpressionScriptProvided> moduleScripts = new HashMap<>();
        ScriptCollectorRuntime scriptCollectorRuntime = new ScriptCollectorRuntime(moduleScripts);
        try {
            provider.getModuleProvider().initializeScripts(new EPModuleScriptInitServicesImpl(scriptCollectorRuntime));
        } catch (Throwable e) {
            throw new EPException(e);
        }

        return new DeployerModuleEPLObjects(beanEventTypeFactory, moduleEventTypes, moduleNamedWindows, moduleTables, moduleIndexes, moduleContexts, moduleVariables, moduleExpressions, moduleScripts, eventTypeCollector.getSerdes(), eventTypeResolver);
    }
}
