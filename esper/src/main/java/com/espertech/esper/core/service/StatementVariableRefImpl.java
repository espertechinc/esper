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

import com.espertech.esper.epl.expression.table.ExprTableAccessNode;
import com.espertech.esper.epl.named.NamedWindowMgmtService;
import com.espertech.esper.epl.table.mgmt.TableService;
import com.espertech.esper.epl.variable.VariableReader;
import com.espertech.esper.epl.variable.VariableService;
import com.espertech.esper.util.ManagedReadWriteLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Service for holding references between statements and their variable use.
 */
public class StatementVariableRefImpl implements StatementVariableRef {
    private static final Logger log = LoggerFactory.getLogger(StatementVariableRefImpl.class);

    private final ManagedReadWriteLock mapLock;
    private final HashMap<String, Set<String>> variableToStmt;
    private final HashMap<String, Set<String>> stmtToVariable;
    private final VariableService variableService;
    private final TableService tableService;
    private final NamedWindowMgmtService namedWindowMgmtService;
    private final Set<String> configuredVariables;

    /**
     * Ctor.
     *
     * @param variableService        variables
     * @param tableService           table service
     * @param namedWindowMgmtService named window service
     */
    public StatementVariableRefImpl(VariableService variableService, TableService tableService, NamedWindowMgmtService namedWindowMgmtService) {
        variableToStmt = new HashMap<String, Set<String>>();
        stmtToVariable = new HashMap<String, Set<String>>();
        mapLock = new ManagedReadWriteLock("StatementVariableRefImpl", false);
        this.variableService = variableService;
        this.tableService = tableService;
        this.namedWindowMgmtService = namedWindowMgmtService;

        configuredVariables = new HashSet<String>();
        for (Map.Entry<String, VariableReader> entry : variableService.getVariableReadersNonCP().entrySet()) {
            configuredVariables.add(entry.getKey());
        }
    }

    public void addConfiguredVariable(String variableName) {
        configuredVariables.add(variableName);
    }

    public void removeConfiguredVariable(String variableName) {
        configuredVariables.remove(variableName);
    }

    public void addReferences(String statementName, Set<String> variablesReferenced, ExprTableAccessNode[] tableNodes) {
        mapLock.acquireWriteLock();
        try {
            if (variablesReferenced != null) {
                for (String reference : variablesReferenced) {
                    addReference(statementName, reference);
                }
            }
            if (tableNodes != null) {
                for (ExprTableAccessNode tableNode : tableNodes) {
                    addReference(statementName, tableNode.getTableName());
                }
            }
        } finally {
            mapLock.releaseWriteLock();
        }
    }

    public void addReferences(String statementName, String variableReferenced) {
        mapLock.acquireWriteLock();
        try {
            addReference(statementName, variableReferenced);
        } finally {
            mapLock.releaseWriteLock();
        }
    }

    public void removeReferencesStatement(String statementName) {
        mapLock.acquireWriteLock();
        try {
            Set<String> variables = stmtToVariable.remove(statementName);
            if (variables != null) {
                for (String variable : variables) {
                    removeReference(statementName, variable);
                }
            }
        } finally {
            mapLock.releaseWriteLock();
        }
    }

    public void removeReferencesVariable(String name) {
        mapLock.acquireWriteLock();
        try {
            Set<String> statementNames = variableToStmt.remove(name);
            if (statementNames != null) {
                for (String statementName : statementNames) {
                    removeReference(statementName, name);
                }
            }
        } finally {
            mapLock.releaseWriteLock();
        }
    }

    public boolean isInUse(String variable) {
        mapLock.acquireReadLock();
        try {
            return variableToStmt.containsKey(variable);
        } finally {
            mapLock.releaseReadLock();
        }
    }

    public Set<String> getStatementNamesForVar(String variableName) {
        mapLock.acquireReadLock();
        try {
            Set<String> variables = variableToStmt.get(variableName);
            if (variables == null) {
                return Collections.EMPTY_SET;
            }
            return Collections.unmodifiableSet(variables);
        } finally {
            mapLock.releaseReadLock();
        }
    }

    private void addReference(String statementName, String variableName) {
        // add to variables
        Set<String> statements = variableToStmt.get(variableName);
        if (statements == null) {
            statements = new HashSet<String>();
            variableToStmt.put(variableName, statements);
        }
        statements.add(statementName);

        // add to statements
        Set<String> variables = stmtToVariable.get(statementName);
        if (variables == null) {
            variables = new HashSet<String>();
            stmtToVariable.put(statementName, variables);
        }
        variables.add(variableName);
    }

    private void removeReference(String statementName, String variableName) {
        // remove from variables
        Set<String> statements = variableToStmt.get(variableName);
        if (statements != null) {
            if (!statements.remove(statementName)) {
                log.info("Failed to find statement name '" + statementName + "' in collection");
            }

            if (statements.isEmpty()) {
                variableToStmt.remove(variableName);

                if (!configuredVariables.contains(variableName)) {
                    variableService.removeVariableIfFound(variableName);
                    tableService.removeTableIfFound(variableName);
                    namedWindowMgmtService.removeNamedWindowIfFound(variableName);
                }
            }
        }

        // remove from statements
        Set<String> variables = stmtToVariable.get(statementName);
        if (variables != null) {
            if (!variables.remove(variableName)) {
                log.info("Failed to find variable '" + variableName + "' in collection");
            }

            if (variables.isEmpty()) {
                stmtToVariable.remove(statementName);
            }
        }
    }

    /**
     * For testing, returns the mapping of variable name to statement names.
     *
     * @return mapping
     */
    protected HashMap<String, Set<String>> getVariableToStmt() {
        return variableToStmt;
    }

    /**
     * For testing, returns the mapping of statement names to variable names.
     *
     * @return mapping
     */
    protected HashMap<String, Set<String>> getStmtToVariable() {
        return stmtToVariable;
    }
}
