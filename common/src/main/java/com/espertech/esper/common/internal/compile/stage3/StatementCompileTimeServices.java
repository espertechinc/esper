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
import com.espertech.esper.common.internal.compile.stage1.specmapper.StatementSpecMapEnv;
import com.espertech.esper.common.internal.context.compile.ContextCompileTimeRegistry;
import com.espertech.esper.common.internal.context.compile.ContextCompileTimeResolver;
import com.espertech.esper.common.internal.context.module.EventTypeCompileTimeRegistry;
import com.espertech.esper.common.internal.context.module.ModuleDependenciesCompileTime;
import com.espertech.esper.common.internal.epl.dataflow.core.DataFlowCompileTimeRegistry;
import com.espertech.esper.common.internal.epl.enummethod.compile.EnumMethodCallStackHelperImpl;
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
import com.espertech.esper.common.internal.event.core.EventTypeNameGeneratorStatement;
import com.espertech.esper.common.internal.event.eventtyperepo.EventTypeRepositoryImpl;
import com.espertech.esper.common.internal.event.xml.XMLFragmentEventTypeFactory;
import com.espertech.esper.common.internal.serde.compiletime.resolve.SerdeCompileTimeResolver;
import com.espertech.esper.common.internal.serde.compiletime.eventtype.SerdeEventTypeCompileTimeRegistry;
import com.espertech.esper.common.internal.settings.ClasspathImportServiceCompileTime;
import com.espertech.esper.common.internal.view.core.ViewResolutionService;

public class StatementCompileTimeServices {
    private final ModuleCompileTimeServices services;
    private final EnumMethodCallStackHelperImpl enumMethodCallStackHelper = new EnumMethodCallStackHelperImpl();
    private final EventTypeNameGeneratorStatement eventTypeNameGeneratorStatement;

    public StatementCompileTimeServices(int statementNumber, ModuleCompileTimeServices services) {
        this.services = services;
        this.eventTypeNameGeneratorStatement = new EventTypeNameGeneratorStatement(statementNumber);
    }

    public BeanEventTypeStemService getBeanEventTypeStemService() {
        return services.getBeanEventTypeStemService();
    }

    public BeanEventTypeFactoryPrivate getBeanEventTypeFactoryPrivate() {
        return services.getBeanEventTypeFactoryPrivate();
    }

    public Configuration getConfiguration() {
        return services.getConfiguration();
    }

    public ContextCompileTimeRegistry getContextCompileTimeRegistry() {
        return services.getContextCompileTimeRegistry();
    }

    public ContextCompileTimeResolver getContextCompileTimeResolver() {
        return services.getContextCompileTimeResolver();
    }

    public DatabaseConfigServiceCompileTime getDatabaseConfigServiceCompileTime() {
        return services.getDatabaseConfigServiceCompileTime();
    }

    public ClasspathImportServiceCompileTime getClasspathImportServiceCompileTime() {
        return services.getClasspathImportServiceCompileTime();
    }

    public EnumMethodCallStackHelperImpl getEnumMethodCallStackHelper() {
        return enumMethodCallStackHelper;
    }

    public ExprDeclaredCompileTimeRegistry getExprDeclaredCompileTimeRegistry() {
        return services.getExprDeclaredCompileTimeRegistry();
    }

    public ExprDeclaredCompileTimeResolver getExprDeclaredCompileTimeResolver() {
        return services.getExprDeclaredCompileTimeResolver();
    }

    public EventTypeCompileTimeRegistry getEventTypeCompileTimeRegistry() {
        return services.getEventTypeCompileTimeRegistry();
    }

    public EventTypeRepositoryImpl getEventTypeRepositoryPreconfigured() {
        return services.getEventTypeRepositoryPreconfigured();
    }

    public IndexCompileTimeRegistry getIndexCompileTimeRegistry() {
        return services.getIndexCompileTimeRegistry();
    }

    public ModuleAccessModifierService getModuleVisibilityRules() {
        return services.getModuleVisibilityRules();
    }

    public NamedWindowCompileTimeResolver getNamedWindowCompileTimeResolver() {
        return services.getNamedWindowCompileTimeResolver();
    }

    public NamedWindowCompileTimeRegistry getNamedWindowCompileTimeRegistry() {
        return services.getNamedWindowCompileTimeRegistry();
    }

    public PatternObjectResolutionService getPatternResolutionService() {
        return services.getPatternObjectResolutionService();
    }

    public TableCompileTimeResolver getTableCompileTimeResolver() {
        return services.getTableCompileTimeResolver();
    }

    public TableCompileTimeRegistry getTableCompileTimeRegistry() {
        return services.getTableCompileTimeRegistry();
    }

    public VariableCompileTimeRegistry getVariableCompileTimeRegistry() {
        return services.getVariableCompileTimeRegistry();
    }

    public VariableCompileTimeResolver getVariableCompileTimeResolver() {
        return services.getVariableCompileTimeResolver();
    }

    public ViewResolutionService getViewResolutionService() {
        return services.getViewResolutionService();
    }

    public StatementSpecMapEnv getStatementSpecMapEnv() {
        return new StatementSpecMapEnv(services.getClasspathImportServiceCompileTime(), services.getVariableCompileTimeResolver(), services.getConfiguration(),
                services.getExprDeclaredCompileTimeResolver(), services.getContextCompileTimeResolver(), services.getTableCompileTimeResolver(), services.getScriptCompileTimeResolver(), services.getCompilerServices());
    }

    public ScriptCompileTimeResolver getScriptCompileTimeResolver() {
        return services.getScriptCompileTimeResolver();
    }

    public ScriptCompileTimeRegistry getScriptCompileTimeRegistry() {
        return services.getScriptCompileTimeRegistry();
    }

    public ModuleDependenciesCompileTime getModuleDependenciesCompileTime() {
        return services.getModuleDependencies();
    }

    public EventTypeNameGeneratorStatement getEventTypeNameGeneratorStatement() {
        return eventTypeNameGeneratorStatement;
    }

    public EventTypeAvroHandler getEventTypeAvroHandler() {
        return services.getEventTypeAvroHandler();
    }

    public EventTypeCompileTimeResolver getEventTypeCompileTimeResolver() {
        return services.getEventTypeCompileTimeResolver();
    }

    public CompilerServices getCompilerServices() {
        return services.getCompilerServices();
    }

    public DataFlowCompileTimeRegistry getDataFlowCompileTimeRegistry() {
        return services.getDataFlowCompileTimeRegistry();
    }

    public boolean isInstrumented() {
        return services.isInstrumented();
    }

    public ModuleCompileTimeServices getServices() {
        return services;
    }

    public boolean isAttachPatternText() {
        return services.getConfiguration().getCompiler().getByteCode().isAttachPatternEPL();
    }

    public String getPackageName() {
        return services.getPackageName();
    }

    public ClassLoader getParentClassLoader() {
        return services.getParentClassLoader();
    }

    public SerdeEventTypeCompileTimeRegistry getSerdeEventTypeRegistry() {
        return services.getSerdeEventTypeRegistry();
    }

    public SerdeCompileTimeResolver getSerdeResolver() {
        return services.getSerdeResolver();
    }

    public XMLFragmentEventTypeFactory getXmlFragmentEventTypeFactory() {
        return services.getXmlFragmentEventTypeFactory();
    }

    public boolean isFireAndForget() {
        return services.isFireAndForget();
    }
}
