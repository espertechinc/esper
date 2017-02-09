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

import java.util.Set;

/**
 * Service for maintaining references between statement name and variables.
 */
public interface StatementVariableRef {
    /**
     * Returns true if the variable is listed as in-use by any statement, or false if not
     *
     * @param variableName name
     * @return indicator whether variable is in use
     */
    public boolean isInUse(String variableName);

    /**
     * Returns the set of statement names that use a given variable.
     *
     * @param variableName name
     * @return set of statements or null if none found
     */
    public Set<String> getStatementNamesForVar(String variableName);

    /**
     * Add a reference from a statement name to a set of variables.
     *
     * @param statementName       name of statement
     * @param variablesReferenced types
     * @param tableNodes          table nodes, if any
     */
    public void addReferences(String statementName, Set<String> variablesReferenced, ExprTableAccessNode[] tableNodes);

    /**
     * Add a reference from a statement name to a single variable.
     *
     * @param statementName      name of statement
     * @param variableReferenced variable
     */
    public void addReferences(String statementName, String variableReferenced);

    /**
     * Remove all references for a given statement.
     *
     * @param statementName statement name
     */
    public void removeReferencesStatement(String statementName);

    /**
     * Remove all references for a given event type.
     *
     * @param variableName variable name
     */
    public void removeReferencesVariable(String variableName);

    /**
     * Add a preconfigured variable.
     *
     * @param variableName name
     */
    public void addConfiguredVariable(String variableName);

    /**
     * Remove a preconfigured variable.
     *
     * @param variableName var
     */
    public void removeConfiguredVariable(String variableName);
}