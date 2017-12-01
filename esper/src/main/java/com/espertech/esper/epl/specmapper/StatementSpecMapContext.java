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
package com.espertech.esper.epl.specmapper;

import com.espertech.esper.client.ConfigurationInformation;
import com.espertech.esper.client.ConfigurationPlugInAggregationMultiFunction;
import com.espertech.esper.core.context.mgr.ContextManagementService;
import com.espertech.esper.core.context.util.ContextDescriptor;
import com.espertech.esper.epl.core.engineimport.EngineImportService;
import com.espertech.esper.epl.declexpr.ExprDeclaredService;
import com.espertech.esper.epl.expression.table.ExprTableAccessNode;
import com.espertech.esper.epl.named.NamedWindowMgmtService;
import com.espertech.esper.epl.spec.ExpressionDeclItem;
import com.espertech.esper.epl.spec.ExpressionScriptProvided;
import com.espertech.esper.epl.table.mgmt.TableService;
import com.espertech.esper.epl.variable.VariableService;
import com.espertech.esper.pattern.PatternNodeFactory;
import com.espertech.esper.plugin.PlugInAggregationMultiFunctionFactory;
import com.espertech.esper.util.LazyAllocatedMap;

import java.util.*;

/**
 * Context for mapping a SODA statement to a statement specification, or multiple for subqueries,
 * and obtaining certain optimization information from a statement.
 */
public class StatementSpecMapContext {
    private final EngineImportService engineImportService;
    private final VariableService variableService;
    private final ConfigurationInformation configuration;
    private final String engineURI;
    private final PatternNodeFactory patternNodeFactory;
    private final NamedWindowMgmtService namedWindowMgmtService;
    private final ContextManagementService contextManagementService;
    private final ExprDeclaredService exprDeclaredService;
    private final ContextDescriptor contextDescriptor;
    private final TableService tableService;

    private boolean hasVariables;
    private Set<String> variableNames;
    private Map<String, ExpressionDeclItem> expressionDeclarations;
    private Map<String, ExpressionScriptProvided> scripts;
    private LazyAllocatedMap<ConfigurationPlugInAggregationMultiFunction, PlugInAggregationMultiFunctionFactory> plugInAggregations = new LazyAllocatedMap<ConfigurationPlugInAggregationMultiFunction, PlugInAggregationMultiFunctionFactory>();
    private String contextName;
    private Set<ExprTableAccessNode> tableNodes = new HashSet<ExprTableAccessNode>(1);

    public StatementSpecMapContext(EngineImportService engineImportService, VariableService variableService, ConfigurationInformation configuration, String engineURI, PatternNodeFactory patternNodeFactory, NamedWindowMgmtService namedWindowMgmtService, ContextManagementService contextManagementService, ExprDeclaredService exprDeclaredService, ContextDescriptor contextDescriptor, TableService tableService) {
        this.engineImportService = engineImportService;
        this.variableService = variableService;
        this.configuration = configuration;
        this.variableNames = new HashSet<String>();
        this.engineURI = engineURI;
        this.patternNodeFactory = patternNodeFactory;
        this.namedWindowMgmtService = namedWindowMgmtService;
        this.contextManagementService = contextManagementService;
        this.exprDeclaredService = exprDeclaredService;
        this.contextDescriptor = contextDescriptor;
        this.tableService = tableService;
    }

    /**
     * Returns the engine import service.
     *
     * @return service
     */
    public EngineImportService getEngineImportService() {
        return engineImportService;
    }

    /**
     * Returns the variable service.
     *
     * @return service
     */
    public VariableService getVariableService() {
        return variableService;
    }

    /**
     * Returns true if a statement has variables.
     *
     * @return true for variables found
     */
    public boolean isHasVariables() {
        return hasVariables;
    }

    /**
     * Set to true to indicate that a statement has variables.
     *
     * @param hasVariables true for variables, false for none
     */
    public void setHasVariables(boolean hasVariables) {
        this.hasVariables = hasVariables;
    }

    /**
     * Returns the configuration.
     *
     * @return config
     */
    public ConfigurationInformation getConfiguration() {
        return configuration;
    }

    /**
     * Returns variables.
     *
     * @return variables
     */
    public Set<String> getVariableNames() {
        return variableNames;
    }

    public String getEngineURI() {
        return engineURI;
    }

    public PatternNodeFactory getPatternNodeFactory() {
        return patternNodeFactory;
    }

    public NamedWindowMgmtService getNamedWindowMgmtService() {
        return namedWindowMgmtService;
    }

    public Map<String, ExpressionDeclItem> getExpressionDeclarations() {
        if (expressionDeclarations == null) {
            return Collections.emptyMap();
        }
        return expressionDeclarations;
    }

    public void addExpressionDeclarations(ExpressionDeclItem item) {
        if (expressionDeclarations == null) {
            expressionDeclarations = new HashMap<String, ExpressionDeclItem>();
        }
        expressionDeclarations.put(item.getName(), item);
    }

    public Map<String, ExpressionScriptProvided> getScripts() {
        if (scripts == null) {
            return Collections.emptyMap();
        }
        return scripts;
    }

    public void addScript(ExpressionScriptProvided item) {
        if (scripts == null) {
            scripts = new HashMap<String, ExpressionScriptProvided>();
        }
        scripts.put(item.getName(), item);
    }

    public ContextManagementService getContextManagementService() {
        return contextManagementService;
    }

    public String getContextName() {
        return contextName;
    }

    public void setContextName(String contextName) {
        this.contextName = contextName;
    }

    public ExprDeclaredService getExprDeclaredService() {
        return exprDeclaredService;
    }

    public LazyAllocatedMap<ConfigurationPlugInAggregationMultiFunction, PlugInAggregationMultiFunctionFactory> getPlugInAggregations() {
        return plugInAggregations;
    }

    public ContextDescriptor getContextDescriptor() {
        return contextDescriptor;
    }

    public TableService getTableService() {
        return tableService;
    }

    public Set<ExprTableAccessNode> getTableExpressions() {
        return tableNodes;
    }
}
