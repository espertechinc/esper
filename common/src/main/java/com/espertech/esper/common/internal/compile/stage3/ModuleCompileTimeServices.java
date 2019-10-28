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
package com.espertech.esper.common.internal.compile.stage3;

import com.espertech.esper.common.client.configuration.Configuration;
import com.espertech.esper.common.internal.compile.stage1.CompilerServices;
import com.espertech.esper.common.internal.context.compile.ContextCompileTimeRegistry;
import com.espertech.esper.common.internal.context.compile.ContextCompileTimeResolver;
import com.espertech.esper.common.internal.context.module.EventTypeCompileTimeRegistry;
import com.espertech.esper.common.internal.context.module.ModuleDependenciesCompileTime;
import com.espertech.esper.common.internal.context.util.ParentClassLoader;
import com.espertech.esper.common.internal.epl.dataflow.core.DataFlowCompileTimeRegistry;
import com.espertech.esper.common.internal.epl.expression.declared.compiletime.ExprDeclaredCompileTimeRegistry;
import com.espertech.esper.common.internal.epl.expression.declared.compiletime.ExprDeclaredCompileTimeResolver;
import com.espertech.esper.common.internal.epl.historical.database.connection.DatabaseConfigServiceCompileTime;
import com.espertech.esper.common.internal.epl.index.compile.IndexCompileTimeRegistry;
import com.espertech.esper.common.internal.epl.namedwindow.compile.NamedWindowCompileTimeRegistry;
import com.espertech.esper.common.internal.epl.namedwindow.path.NamedWindowCompileTimeResolver;
import com.espertech.esper.common.internal.epl.pattern.core.PatternObjectResolutionService;
import com.espertech.esper.common.internal.epl.script.compiletime.ScriptCompileTimeRegistry;
import com.espertech.esper.common.internal.epl.script.compiletime.ScriptCompileTimeResolver;
import com.espertech.esper.common.internal.epl.table.compiletime.TableCompileTimeRegistry;
import com.espertech.esper.common.internal.epl.table.compiletime.TableCompileTimeResolver;
import com.espertech.esper.common.internal.epl.variable.compiletime.VariableCompileTimeRegistry;
import com.espertech.esper.common.internal.epl.variable.compiletime.VariableCompileTimeResolver;
import com.espertech.esper.common.internal.event.avro.EventTypeAvroHandler;
import com.espertech.esper.common.internal.event.bean.core.BeanEventTypeStemService;
import com.espertech.esper.common.internal.event.bean.service.BeanEventTypeFactoryPrivate;
import com.espertech.esper.common.internal.event.core.EventTypeCompileTimeResolver;
import com.espertech.esper.common.internal.event.eventtyperepo.EventTypeRepositoryImpl;
import com.espertech.esper.common.internal.event.xml.XMLFragmentEventTypeFactory;
import com.espertech.esper.common.internal.serde.compiletime.eventtype.SerdeEventTypeCompileTimeRegistry;
import com.espertech.esper.common.internal.serde.compiletime.resolve.SerdeCompileTimeResolver;
import com.espertech.esper.common.internal.settings.ClasspathImportServiceCompileTime;
import com.espertech.esper.common.internal.view.core.ViewResolutionService;

public class ModuleCompileTimeServices {
    private final CompilerServices compilerServices;
    private Configuration configuration;
    private final ContextCompileTimeRegistry contextCompileTimeRegistry;
    private final ContextCompileTimeResolver contextCompileTimeResolver;
    private final BeanEventTypeStemService beanEventTypeStemService;
    private final BeanEventTypeFactoryPrivate beanEventTypeFactoryPrivate;
    private final DatabaseConfigServiceCompileTime databaseConfigServiceCompileTime;
    private ClasspathImportServiceCompileTime classpathImportServiceCompileTime;
    private final ExprDeclaredCompileTimeRegistry exprDeclaredCompileTimeRegistry;
    private final ExprDeclaredCompileTimeResolver exprDeclaredCompileTimeResolver;
    private final EventTypeAvroHandler eventTypeAvroHandler;
    private final EventTypeCompileTimeRegistry eventTypeCompileTimeRegistry;
    private final EventTypeCompileTimeResolver eventTypeCompileTimeResolver;
    private final EventTypeRepositoryImpl eventTypeRepositoryPreconfigured;
    private final boolean fireAndForget;
    private final IndexCompileTimeRegistry indexCompileTimeRegistry;
    private final ModuleDependenciesCompileTime moduleDependencies;
    private final ModuleAccessModifierService moduleVisibilityRules;
    private final NamedWindowCompileTimeResolver namedWindowCompileTimeResolver;
    private final NamedWindowCompileTimeRegistry namedWindowCompileTimeRegistry;
    private final ParentClassLoader parentClassLoader;
    private final PatternObjectResolutionService patternObjectResolutionService;
    private final ScriptCompileTimeRegistry scriptCompileTimeRegistry;
    private final ScriptCompileTimeResolver scriptCompileTimeResolver;
    private final SerdeEventTypeCompileTimeRegistry serdeEventTypeRegistry;
    private final SerdeCompileTimeResolver serdeResolver;
    private final TableCompileTimeRegistry tableCompileTimeRegistry;
    private final TableCompileTimeResolver tableCompileTimeResolver;
    private final VariableCompileTimeRegistry variableCompileTimeRegistry;
    private final VariableCompileTimeResolver variableCompileTimeResolver;
    private final ViewResolutionService viewResolutionService;
    private final XMLFragmentEventTypeFactory xmlFragmentEventTypeFactory;

    private final DataFlowCompileTimeRegistry dataFlowCompileTimeRegistry = new DataFlowCompileTimeRegistry();

    public ModuleCompileTimeServices(CompilerServices compilerServices, Configuration configuration, ContextCompileTimeRegistry contextCompileTimeRegistry, ContextCompileTimeResolver contextCompileTimeResolver, BeanEventTypeStemService beanEventTypeStemService, BeanEventTypeFactoryPrivate beanEventTypeFactoryPrivate, DatabaseConfigServiceCompileTime databaseConfigServiceCompileTime, ClasspathImportServiceCompileTime classpathImportService, ExprDeclaredCompileTimeRegistry exprDeclaredCompileTimeRegistry, ExprDeclaredCompileTimeResolver exprDeclaredCompileTimeResolver, EventTypeAvroHandler eventTypeAvroHandler, EventTypeCompileTimeRegistry eventTypeCompileTimeRegistry, EventTypeCompileTimeResolver eventTypeCompileTimeResolver, EventTypeRepositoryImpl eventTypeRepositoryPreconfigured, boolean fireAndForget, IndexCompileTimeRegistry indexCompileTimeRegistry, ModuleDependenciesCompileTime moduleDependencies, ModuleAccessModifierService moduleVisibilityRules, NamedWindowCompileTimeResolver namedWindowCompileTimeResolver, NamedWindowCompileTimeRegistry namedWindowCompileTimeRegistry, ParentClassLoader parentClassLoader, PatternObjectResolutionService patternObjectResolutionService, ScriptCompileTimeRegistry scriptCompileTimeRegistry, ScriptCompileTimeResolver scriptCompileTimeResolver, SerdeEventTypeCompileTimeRegistry serdeEventTypeRegistry, SerdeCompileTimeResolver serdeResolver, TableCompileTimeRegistry tableCompileTimeRegistry, TableCompileTimeResolver tableCompileTimeResolver, VariableCompileTimeRegistry variableCompileTimeRegistry, VariableCompileTimeResolver variableCompileTimeResolver, ViewResolutionService viewResolutionService, XMLFragmentEventTypeFactory xmlFragmentEventTypeFactory) {
        this.parentClassLoader = parentClassLoader;
        this.compilerServices = compilerServices;
        this.configuration = configuration;
        this.contextCompileTimeRegistry = contextCompileTimeRegistry;
        this.contextCompileTimeResolver = contextCompileTimeResolver;
        this.beanEventTypeStemService = beanEventTypeStemService;
        this.beanEventTypeFactoryPrivate = beanEventTypeFactoryPrivate;
        this.databaseConfigServiceCompileTime = databaseConfigServiceCompileTime;
        this.classpathImportServiceCompileTime = classpathImportService;
        this.exprDeclaredCompileTimeRegistry = exprDeclaredCompileTimeRegistry;
        this.exprDeclaredCompileTimeResolver = exprDeclaredCompileTimeResolver;
        this.eventTypeAvroHandler = eventTypeAvroHandler;
        this.eventTypeCompileTimeRegistry = eventTypeCompileTimeRegistry;
        this.eventTypeCompileTimeResolver = eventTypeCompileTimeResolver;
        this.eventTypeRepositoryPreconfigured = eventTypeRepositoryPreconfigured;
        this.fireAndForget = fireAndForget;
        this.indexCompileTimeRegistry = indexCompileTimeRegistry;
        this.moduleDependencies = moduleDependencies;
        this.moduleVisibilityRules = moduleVisibilityRules;
        this.namedWindowCompileTimeResolver = namedWindowCompileTimeResolver;
        this.namedWindowCompileTimeRegistry = namedWindowCompileTimeRegistry;
        this.patternObjectResolutionService = patternObjectResolutionService;
        this.scriptCompileTimeRegistry = scriptCompileTimeRegistry;
        this.scriptCompileTimeResolver = scriptCompileTimeResolver;
        this.serdeEventTypeRegistry = serdeEventTypeRegistry;
        this.serdeResolver = serdeResolver;
        this.tableCompileTimeRegistry = tableCompileTimeRegistry;
        this.tableCompileTimeResolver = tableCompileTimeResolver;
        this.variableCompileTimeRegistry = variableCompileTimeRegistry;
        this.variableCompileTimeResolver = variableCompileTimeResolver;
        this.viewResolutionService = viewResolutionService;
        this.xmlFragmentEventTypeFactory = xmlFragmentEventTypeFactory;
    }

    public ModuleCompileTimeServices() {
        this.parentClassLoader = null;
        this.compilerServices = null;
        this.configuration = null;
        this.contextCompileTimeRegistry = null;
        this.contextCompileTimeResolver = null;
        this.beanEventTypeStemService = null;
        this.beanEventTypeFactoryPrivate = null;
        this.databaseConfigServiceCompileTime = null;
        this.classpathImportServiceCompileTime = null;
        this.exprDeclaredCompileTimeRegistry = null;
        this.exprDeclaredCompileTimeResolver = null;
        this.eventTypeAvroHandler = null;
        this.eventTypeCompileTimeRegistry = null;
        this.eventTypeCompileTimeResolver = null;
        this.eventTypeRepositoryPreconfigured = null;
        this.fireAndForget = false;
        this.indexCompileTimeRegistry = null;
        this.serdeEventTypeRegistry = null;
        this.serdeResolver = null;
        this.moduleDependencies = null;
        this.moduleVisibilityRules = null;
        this.namedWindowCompileTimeResolver = null;
        this.namedWindowCompileTimeRegistry = null;
        this.patternObjectResolutionService = null;
        this.scriptCompileTimeRegistry = null;
        this.scriptCompileTimeResolver = null;
        this.tableCompileTimeRegistry = null;
        this.tableCompileTimeResolver = null;
        this.variableCompileTimeRegistry = null;
        this.variableCompileTimeResolver = null;
        this.viewResolutionService = null;
        this.xmlFragmentEventTypeFactory = null;
    }

    public BeanEventTypeStemService getBeanEventTypeStemService() {
        return beanEventTypeStemService;
    }

    public BeanEventTypeFactoryPrivate getBeanEventTypeFactoryPrivate() {
        return beanEventTypeFactoryPrivate;
    }

    public CompilerServices getCompilerServices() {
        return compilerServices;
    }

    public Configuration getConfiguration() {
        return configuration;
    }

    public ContextCompileTimeRegistry getContextCompileTimeRegistry() {
        return contextCompileTimeRegistry;
    }

    public ContextCompileTimeResolver getContextCompileTimeResolver() {
        return contextCompileTimeResolver;
    }

    public DatabaseConfigServiceCompileTime getDatabaseConfigServiceCompileTime() {
        return databaseConfigServiceCompileTime;
    }

    public ClasspathImportServiceCompileTime getClasspathImportServiceCompileTime() {
        return classpathImportServiceCompileTime;
    }

    public ExprDeclaredCompileTimeRegistry getExprDeclaredCompileTimeRegistry() {
        return exprDeclaredCompileTimeRegistry;
    }

    public ExprDeclaredCompileTimeResolver getExprDeclaredCompileTimeResolver() {
        return exprDeclaredCompileTimeResolver;
    }

    public EventTypeAvroHandler getEventTypeAvroHandler() {
        return eventTypeAvroHandler;
    }

    public EventTypeCompileTimeRegistry getEventTypeCompileTimeRegistry() {
        return eventTypeCompileTimeRegistry;
    }

    public EventTypeRepositoryImpl getEventTypeRepositoryPreconfigured() {
        return eventTypeRepositoryPreconfigured;
    }

    public boolean isFireAndForget() {
        return fireAndForget;
    }

    public IndexCompileTimeRegistry getIndexCompileTimeRegistry() {
        return indexCompileTimeRegistry;
    }

    public ModuleDependenciesCompileTime getModuleDependencies() {
        return moduleDependencies;
    }

    public ModuleAccessModifierService getModuleVisibilityRules() {
        return moduleVisibilityRules;
    }

    public NamedWindowCompileTimeResolver getNamedWindowCompileTimeResolver() {
        return namedWindowCompileTimeResolver;
    }

    public NamedWindowCompileTimeRegistry getNamedWindowCompileTimeRegistry() {
        return namedWindowCompileTimeRegistry;
    }

    public String getPackageName() {
        return "generated";
    }

    public ParentClassLoader getParentClassLoader() {
        return parentClassLoader;
    }

    public PatternObjectResolutionService getPatternObjectResolutionService() {
        return patternObjectResolutionService;
    }

    public ScriptCompileTimeRegistry getScriptCompileTimeRegistry() {
        return scriptCompileTimeRegistry;
    }

    public ScriptCompileTimeResolver getScriptCompileTimeResolver() {
        return scriptCompileTimeResolver;
    }

    public SerdeEventTypeCompileTimeRegistry getSerdeEventTypeRegistry() {
        return serdeEventTypeRegistry;
    }

    public SerdeCompileTimeResolver getSerdeResolver() {
        return serdeResolver;
    }

    public TableCompileTimeRegistry getTableCompileTimeRegistry() {
        return tableCompileTimeRegistry;
    }

    public TableCompileTimeResolver getTableCompileTimeResolver() {
        return tableCompileTimeResolver;
    }

    public VariableCompileTimeRegistry getVariableCompileTimeRegistry() {
        return variableCompileTimeRegistry;
    }

    public VariableCompileTimeResolver getVariableCompileTimeResolver() {
        return variableCompileTimeResolver;
    }

    public ViewResolutionService getViewResolutionService() {
        return viewResolutionService;
    }

    public XMLFragmentEventTypeFactory getXmlFragmentEventTypeFactory() {
        return xmlFragmentEventTypeFactory;
    }

    public EventTypeCompileTimeResolver getEventTypeCompileTimeResolver() {
        return eventTypeCompileTimeResolver;
    }

    public DataFlowCompileTimeRegistry getDataFlowCompileTimeRegistry() {
        return dataFlowCompileTimeRegistry;
    }

    public boolean isInstrumented() {
        return configuration.getCompiler().getByteCode().isInstrumented();
    }

    public void setClasspathImportServiceCompileTime(ClasspathImportServiceCompileTime classpathImportServiceCompileTime) {
        this.classpathImportServiceCompileTime = classpathImportServiceCompileTime;
    }

    public void setConfiguration(Configuration configuration) {
        this.configuration = configuration;
    }
}
