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

import com.espertech.esper.client.context.EPContextPartitionAdmin;
import com.espertech.esper.client.deploy.EPDeploymentAdmin;
import com.espertech.esper.client.soda.EPStatementObjectModel;

/**
 * Administrative interface to the event stream processing engine. Includes methods to create patterns and EPL statements.
 */
public interface EPAdministrator {
    /**
     * Returns deployment administrative services.
     *
     * @return deployment administration
     */
    public EPDeploymentAdmin getDeploymentAdmin();

    /**
     * Create and starts an event pattern statement for the expressing string passed.
     * <p>
     * The engine assigns a unique name to the statement.
     *
     * @param onExpression must follow the documented syntax for pattern statements
     * @return EPStatement to poll data from or to add listeners to
     * @throws EPException when the expression was not valid
     */
    public EPStatement createPattern(String onExpression) throws EPException;

    /**
     * Creates and starts an EPL statement.
     * <p>
     * The engine assigns a unique name to the statement. The returned statement is in started state.
     *
     * @param eplStatement is the query language statement
     * @return EPStatement to poll data from or to add listeners to
     * @throws EPException when the expression was not valid
     */
    public EPStatement createEPL(String eplStatement) throws EPException;

    /**
     * Create and starts an event pattern statement for the expressing string passed and assign the name passed.
     * <p>
     * The statement name is optimally a unique name. If a statement of the same name
     * has already been created, the engine assigns a postfix to create a unique statement name.
     *
     * @param onExpression  must follow the documented syntax for pattern statements
     * @param statementName is the name to assign to the statement for use in managing the statement
     * @return EPStatement to poll data from or to add listeners to
     * @throws EPException when the expression was not valid
     */
    public EPStatement createPattern(String onExpression, String statementName) throws EPException;

    /**
     * Create and starts an event pattern statement for the expressing string passed and assign the name passed.
     * <p>
     * The statement name is optimally a unique name. If a statement of the same name
     * has already been created, the engine assigns a postfix to create a unique statement name.
     * <p>
     * Accepts an application defined user data object associated with the statement. The <em>user
     * object</em> is a single, unnamed field that is stored with every statement.
     * Applications may put arbitrary objects in this field or a null value.
     *
     * @param onExpression  must follow the documented syntax for pattern statements
     * @param statementName is the name to assign to the statement for use in managing the statement
     * @param userObject    is the application-defined user object
     * @return EPStatement to poll data from or to add listeners to
     * @throws EPException when the expression was not valid
     */
    public EPStatement createPattern(String onExpression, String statementName, Object userObject) throws EPException;

    /**
     * Create and starts an event pattern statement for the expressing string passed and assign the name passed.
     * <p>
     * Accepts an application defined user data object associated with the statement. The <em>user
     * object</em> is a single, unnamed field that is stored with every statement.
     * Applications may put arbitrary objects in this field or a null value.
     *
     * @param onExpression must follow the documented syntax for pattern statements
     * @param userObject   is the application-defined user object
     * @return EPStatement to poll data from or to add listeners to
     * @throws EPException when the expression was not valid
     */
    public EPStatement createPattern(String onExpression, Object userObject) throws EPException;

    /**
     * Create and starts an EPL statement.
     * <p>
     * The statement name is optimally a unique name. If a statement of the same name
     * has already been created, the engine assigns a postfix to create a unique statement name.
     *
     * @param eplStatement  is the query language statement
     * @param statementName is the name to assign to the statement for use in managing the statement
     * @return EPStatement to poll data from or to add listeners to
     * @throws EPException when the expression was not valid
     */
    public EPStatement createEPL(String eplStatement, String statementName) throws EPException;

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
     * @param statementName is the name to assign to the statement for use in managing the statement
     * @param userObject    is the application-defined user object
     * @return EPStatement to poll data from or to add listeners to
     * @throws EPException when the expression was not valid
     */
    public EPStatement createEPL(String eplStatement, String statementName, Object userObject) throws EPException;

    /**
     * Create and starts an EPL statement.
     * <p>
     * Accepts an application defined user data object associated with the statement. The <em>user
     * object</em> is a single, unnamed field that is stored with every statement.
     * Applications may put arbitrary objects in this field or a null value.
     *
     * @param eplStatement is the query language statement
     * @param userObject   is the application-defined user object
     * @return EPStatement to poll data from or to add listeners to
     * @throws EPException when the expression was not valid
     */
    public EPStatement createEPL(String eplStatement, Object userObject) throws EPException;

    /**
     * Creates and starts an EPL statement.
     * <p>
     * The statement name is optimally a unique name. If a statement of the same name
     * has already been created, the engine assigns a postfix to create a unique statement name.
     *
     * @param sodaStatement is the statement object model
     * @param statementName is the name to assign to the statement for use in managing the statement
     * @return EPStatement to poll data from or to add listeners to
     * @throws EPException when the expression was not valid
     */
    public EPStatement create(EPStatementObjectModel sodaStatement, String statementName) throws EPException;

    /**
     * Creates and starts an EPL statement.
     * <p>
     * The statement name is optimally a unique name. If a statement of the same name
     * has already been created, the engine assigns a postfix to create a unique statement name.
     * <p>
     * Accepts an application defined user data object associated with the statement. The <em>user
     * object</em> is a single, unnamed field that is stored with every statement.
     * Applications may put arbitrary objects in this field or a null value.
     *
     * @param sodaStatement is the statement object model
     * @param statementName is the name to assign to the statement for use in managing the statement
     * @param userObject    is the application-defined user object
     * @return EPStatement to poll data from or to add listeners to
     * @throws EPException when the expression was not valid
     */
    public EPStatement create(EPStatementObjectModel sodaStatement, String statementName, Object userObject) throws EPException;

    /**
     * Creates and starts an EPL statement.
     *
     * @param sodaStatement is the statement object model
     * @return EPStatement to poll data from or to add listeners to
     * @throws EPException when the expression was not valid
     */
    public EPStatement create(EPStatementObjectModel sodaStatement) throws EPException;

    /**
     * Compiles a given EPL into an object model representation of the query.
     *
     * @param eplExpression is the statement text to compile
     * @return object model of statement
     * @throws EPException indicates compilation errors.
     */
    public EPStatementObjectModel compileEPL(String eplExpression) throws EPException;

    /**
     * Prepares a statement for the given EPL, which can include substitution parameters marked via question mark '?'.
     *
     * @param eplExpression is the statement text to prepare
     * @return prepared statement
     * @throws EPException indicates compilation errors.
     */
    public EPPreparedStatement prepareEPL(String eplExpression) throws EPException;

    /**
     * Prepares a statement for the given pattern, which can include substitution parameters marked via question mark '?'.
     *
     * @param patternExpression is the statement text to prepare
     * @return prepared statement
     * @throws EPException indicates compilation errors.
     */
    public EPPreparedStatement preparePattern(String patternExpression) throws EPException;

    /**
     * Creates and starts a prepared statement.
     * <p>
     * The statement name is optimally a unique name. If a statement of the same name
     * has already been created, the engine assigns a postfix to create a unique statement name.
     *
     * @param prepared      is the prepared statement for which all substitution values have been provided
     * @param statementName is the name to assign to the statement for use in managing the statement
     * @return EPStatement to poll data from or to add listeners to
     * @throws EPException when the prepared statement was not valid
     */
    public EPStatement create(EPPreparedStatement prepared, String statementName) throws EPException;

    /**
     * Creates and starts a prepared statement.
     * <p>
     * The statement name is optimally a unique name. If a statement of the same name
     * has already been created, the engine assigns a postfix to create a unique statement name.
     * <p>
     * Accepts an application defined user data object associated with the statement. The <em>user
     * object</em> is a single, unnamed field that is stored with every statement.
     * Applications may put arbitrary objects in this field or a null value.
     *
     * @param prepared      is the prepared statement for which all substitution values have been provided
     * @param statementName is the name to assign to the statement for use in managing the statement
     * @param userObject    is the application-defined user object
     * @return EPStatement to poll data from or to add listeners to
     * @throws EPException when the prepared statement was not valid
     */
    public EPStatement create(EPPreparedStatement prepared, String statementName, Object userObject) throws EPException;

    /**
     * Creates and starts a prepared statement.
     *
     * @param prepared is the prepared statement for which all substitution values have been provided
     * @return EPStatement to poll data from or to add listeners to
     * @throws EPException when the expression was not valid
     */
    public EPStatement create(EPPreparedStatement prepared) throws EPException;

    /**
     * Returns the statement by the given statement name. Returns null if a statement of that name has not
     * been created, or if the statement by that name has been destroyed.
     *
     * @param name is the statement name to return the statement for
     * @return statement for the given name, or null if no such started or stopped statement exists
     */
    public EPStatement getStatement(String name);

    /**
     * Returns the statement names of all started and stopped statements.
     * <p>
     * This excludes the name of destroyed statements.
     *
     * @return statement names
     */
    public String[] getStatementNames();

    /**
     * Starts all statements that are in stopped state. Statements in started state
     * are not affected by this method.
     *
     * @throws EPException when an error occured starting statements.
     */
    public void startAllStatements() throws EPException;

    /**
     * Stops all statements that are in started state. Statements in stopped state are not affected by this method.
     *
     * @throws EPException when an error occured stopping statements
     */
    public void stopAllStatements() throws EPException;

    /**
     * Stops and destroys all statements.
     *
     * @throws EPException when an error occured stopping or destroying statements
     */
    public void destroyAllStatements() throws EPException;

    /**
     * Returns configuration operations for runtime engine configuration.
     *
     * @return runtime engine configuration operations
     */
    public ConfigurationOperations getConfiguration();

    /**
     * Returns the administrative interface for context partitions.
     *
     * @return context partition administrative interface
     */
    public EPContextPartitionAdmin getContextPartitionAdmin();
}
