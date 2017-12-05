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

import com.espertech.esper.client.EPException;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.soda.EPStatementObjectModel;
import com.espertech.esper.core.service.speccompiled.StatementSpecCompiled;
import com.espertech.esper.epl.spec.StatementSpecRaw;

import java.util.Map;

/**
 * Handles statement management.
 */
public interface StatementLifecycleSvc extends StatementLifecycleStmtContextResolver {
    /**
     * Initialized the service before use.
     */
    public void init();

    /**
     * Add an observer to be called back when statement-state or listener/subscriber changes are registered.
     * <p>
     * The observers list is backed by a Set.
     *
     * @param observer to add
     */
    public void addObserver(StatementLifecycleObserver observer);

    /**
     * Remove an observer to be called back when statement-state or listener/subscriber changes are registered.
     *
     * @param observer to remove
     */
    public void removeObserver(StatementLifecycleObserver observer);

    /**
     * Dispatch event to observers.
     *
     * @param theEvent to dispatch
     */
    public void dispatchStatementLifecycleEvent(StatementLifecycleEvent theEvent);

    /**
     * Create and start the statement.
     *
     * @param statementSpec         is the statement definition in bean object form, raw unvalidated and unoptimized.
     * @param expression            is the expression text
     * @param isPattern             is an indicator on whether this is a pattern statement and thus the iterator must return the last result,
     *                              versus for non-pattern statements the iterator returns view content.
     * @param optStatementName      is an optional statement name, null if none was supplied
     * @param userObject            the application define user object associated to each statement, if supplied
     * @param isolationUnitServices isolated service services
     * @param optionalStatementId   the statement id
     * @param optionalModel         model
     * @return started statement
     */
    public EPStatement createAndStart(StatementSpecRaw statementSpec, String expression, boolean isPattern, String optStatementName, Object userObject, EPIsolationUnitServices isolationUnitServices, Integer optionalStatementId, EPStatementObjectModel optionalModel);

    /**
     * Start statement by statement id.
     *
     * @param statementId of the statement to start.
     */
    public void start(int statementId);

    /**
     * Stop statement by statement id.
     *
     * @param statementId of the statement to stop.
     */
    public void stop(int statementId);

    /**
     * Destroy statement by statement id.
     *
     * @param statementId statementId of the statement to destroy
     */
    public void destroy(int statementId);

    /**
     * Returns the statement by the given name, or null if no such statement exists.
     *
     * @param name is the statement name
     * @return statement for the given name, or null if no such statement existed
     */
    public EPStatement getStatementByName(String name);

    /**
     * Returns an array of statement names. If no statement has been created, an empty array is returned.
     * <p>
     * Only returns started and stopped statements.
     *
     * @return statement names
     */
    public String[] getStatementNames();

    /**
     * Starts all stopped statements. First statement to fail supplies the exception.
     *
     * @throws EPException to indicate a start error.
     */
    public void startAllStatements() throws EPException;

    /**
     * Stops all started statements. First statement to fail supplies the exception.
     *
     * @throws EPException to indicate a start error.
     */
    public void stopAllStatements() throws EPException;

    /**
     * Destroys all started statements. First statement to fail supplies the exception.
     *
     * @throws EPException to indicate a start error.
     */
    public void destroyAllStatements() throws EPException;

    /**
     * Statements indicate that listeners have been added through this method.
     *
     * @param stmt       is the statement for which listeners were added
     * @param listeners  is the set of listeners after adding the new listener
     * @param isRecovery indicator whether recovering
     */
    public void updatedListeners(EPStatement stmt, EPStatementListenerSet listeners, boolean isRecovery);

    /**
     * Destroy the service.
     */
    public void destroy();

    public String getStatementNameById(int statementId);

    public Map<String, EPStatement> getStmtNameToStmt();

    public EPStatementSPI getStatementById(int statementId);

    StatementSpecCompiled getStatementSpec(int statementId);
}
