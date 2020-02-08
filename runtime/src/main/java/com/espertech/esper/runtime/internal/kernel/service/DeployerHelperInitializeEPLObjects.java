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
import com.espertech.esper.common.internal.collection.PathRegistry;
import com.espertech.esper.common.internal.compile.stage1.spec.ExpressionDeclItem;
import com.espertech.esper.common.internal.compile.stage1.spec.ExpressionScriptProvided;
import com.espertech.esper.common.internal.context.compile.ContextCollector;
import com.espertech.esper.common.internal.context.compile.ContextCollectorImpl;
import com.espertech.esper.common.internal.context.compile.ContextMetaData;
import com.espertech.esper.common.internal.context.module.*;
import com.espertech.esper.common.internal.epl.classprovided.core.ClassProvided;
import com.espertech.esper.common.internal.epl.classprovided.core.ClassProvidedCollectorRuntime;
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
import com.espertech.esper.runtime.client.EPDeployPreconditionException;
import com.espertech.esper.runtime.client.EPStageService;
import com.espertech.esper.runtime.client.util.EPObjectType;
import com.espertech.esper.runtime.internal.kernel.stage.EPStageImpl;
import com.espertech.esper.runtime.internal.kernel.stage.EPStageServiceSPI;
import com.espertech.esper.runtime.internal.kernel.stage.StageSpecificServices;

import java.util.*;
import java.util.function.Function;

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

        // initialize module class-provided create-class
        Map<String, ClassProvided> moduleClasses = new HashMap<>();
        ClassProvidedCollectorRuntime classProvidedCollectorRuntime = new ClassProvidedCollectorRuntime(moduleClasses);
        try {
            provider.getModuleProvider().initializeClassProvided(new EPModuleClassProvidedInitServicesImpl(classProvidedCollectorRuntime));
        } catch (Throwable e) {
            throw new EPException(e);
        }
        for (Map.Entry<String, ClassProvided> moduleClass : moduleClasses.entrySet()) {
            moduleClass.getValue().loadClasses(provider.getClassLoader());
        }

        return new DeployerModuleEPLObjects(beanEventTypeFactory, moduleEventTypes, moduleNamedWindows, moduleTables, moduleIndexes, moduleContexts, moduleVariables, moduleExpressions, moduleScripts, moduleClasses, eventTypeCollector.getSerdes(), eventTypeResolver);
    }

    public static void validateStagedEPLObjects(DeployerModuleEPLObjects moduleEPLObjects, String moduleName, int rolloutItemNumber, EPStageService stageService) throws EPDeployPreconditionException {
        EPStageServiceSPI spi = (EPStageServiceSPI) stageService;
        if (spi.isEmpty()) {
            return;
        }
        for (Map.Entry<String, ContextMetaData> entry : moduleEPLObjects.getModuleContexts().entrySet()) {
            checkAlreadyDefinedByStage(spi, EPObjectType.CONTEXT, svc -> svc.getContextPathRegistry(), entry.getKey(), moduleName, rolloutItemNumber);
        }
        for (Map.Entry<String, NamedWindowMetaData> entry : moduleEPLObjects.getModuleNamedWindows().entrySet()) {
            checkAlreadyDefinedByStage(spi, EPObjectType.NAMEDWINDOW, svc -> svc.getNamedWindowPathRegistry(), entry.getKey(), moduleName, rolloutItemNumber);
        }
        for (Map.Entry<String, VariableMetaData> entry : moduleEPLObjects.getModuleVariables().entrySet()) {
            checkAlreadyDefinedByStage(spi, EPObjectType.VARIABLE, svc -> svc.getVariablePathRegistry(), entry.getKey(), moduleName, rolloutItemNumber);
        }
        for (Map.Entry<String, EventType> entry : moduleEPLObjects.getModuleEventTypes().entrySet()) {
            checkAlreadyDefinedByStage(spi, EPObjectType.EVENTTYPE, svc -> svc.getEventTypePathRegistry(), entry.getKey(), moduleName, rolloutItemNumber);
        }
        for (Map.Entry<String, TableMetaData> entry : moduleEPLObjects.getModuleTables().entrySet()) {
            checkAlreadyDefinedByStage(spi, EPObjectType.TABLE, svc -> svc.getTablePathRegistry(), entry.getKey(), moduleName, rolloutItemNumber);
        }
        for (Map.Entry<String, ExpressionDeclItem> entry : moduleEPLObjects.getModuleExpressions().entrySet()) {
            checkAlreadyDefinedByStage(spi, EPObjectType.EXPRESSION, svc -> svc.getExprDeclaredPathRegistry(), entry.getKey(), moduleName, rolloutItemNumber);
        }
        for (Map.Entry<NameAndParamNum, ExpressionScriptProvided> entry : moduleEPLObjects.getModuleScripts().entrySet()) {
            checkAlreadyDefinedByStage(spi, EPObjectType.SCRIPT, svc -> svc.getScriptPathRegistry(), entry.getKey(), moduleName, rolloutItemNumber);
        }
        for (Map.Entry<String, ClassProvided> entry : moduleEPLObjects.getModuleClasses().entrySet()) {
            checkAlreadyDefinedByStage(spi, EPObjectType.CLASSPROVIDED, svc -> svc.getClassProvidedPathRegistry(), entry.getKey(), moduleName, rolloutItemNumber);
        }
    }

    private static <K, E> void checkAlreadyDefinedByStage(EPStageServiceSPI spi, EPObjectType objectType, Function<StageSpecificServices, PathRegistry<K, E>> registryFunc, K objectKey, String moduleName, int rolloutItemNumber)
        throws EPDeployPreconditionException {
        for (Map.Entry<String, EPStageImpl> entry : spi.getStages().entrySet()) {
            PathRegistry<K, E> registry = registryFunc.apply(entry.getValue().getStageSpecificServices());
            if (registry.getWithModule(objectKey, moduleName) != null) {
                throw new EPDeployPreconditionException(objectType.getPrettyName() + " by name '" + objectKey + "' is already defined by stage '" + entry.getKey() + "'", rolloutItemNumber);
            }
        }
    }
}
