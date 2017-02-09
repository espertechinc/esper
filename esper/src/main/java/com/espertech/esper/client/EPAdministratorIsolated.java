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
package com.espertech.esper.client;

/**
 * Administrative interfae
 */
public interface EPAdministratorIsolated {
    /**
     * Create and starts an EPL statement.
     * <p>
     * The statement name is optimally a unique name. If a statement of the same name
     * has already been created, the engine assigns a postfix to create a unique statement name.
     * <p>
     * Accepts an application defined user data object associated with the statement. The <em>user
     * object</em> is a single, unnamed field that is stored with every statement.
     * Applications may put arbitrary objects in this field or a null value.
     *
     * @param eplStatement  is the query language statement
     * @param userObject    is the application-defined user object, or null if none provided
     * @param statementName is the statement name or null if not provided or provided via annotation instead
     * @return EPStatement to poll data from or to add listeners to, or null if provided via annotation
     * @throws com.espertech.esper.client.EPException when the expression was not valid
     */
    public EPStatement createEPL(String eplStatement, String statementName, Object userObject) throws EPException;

    /**
     * Returns the statement names of all started and stopped statements.
     * <p>
     * This excludes the name of destroyed statements.
     *
     * @return statement names
     */
    public String[] getStatementNames();

    /**
     * Add a statement to the isolated service.
     *
     * @param statement to add
     * @throws EPServiceIsolationException if the statement cannot be isolated, typically because it already is isolated
     */
    public void addStatement(EPStatement statement) throws EPServiceIsolationException;

    /**
     * Remove a statement from the isolated service. This does not change engine state.
     *
     * @param statement to remove
     * @throws EPServiceIsolationException if the statement was not isolated herein
     */
    public void removeStatement(EPStatement statement) throws EPServiceIsolationException;

    /**
     * Add statements to the isolated service.
     *
     * @param statements to add
     * @throws EPServiceIsolationException if the statement cannot be isolated, typically because it already is isolated
     */
    public void addStatement(EPStatement[] statements) throws EPServiceIsolationException;

    /**
     * Remove statements from the isolated service. This does not change engine state.
     *
     * @param statements to remove
     * @throws EPServiceIsolationException if the statement was not isolated herein
     */
    public void removeStatement(EPStatement[] statements) throws EPServiceIsolationException;
}
