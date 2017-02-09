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

import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.EPStatementState;
import com.espertech.esper.view.Viewable;

/**
 * Statement SPI for statements operations for state transitions and internal management.
 */
public interface EPStatementSPI extends EPStatement {
    /**
     * Returns the statement id.
     *
     * @return statement id
     */
    public int getStatementId();

    /**
     * Set the statement's isolated service name, or null to clear.
     *
     * @param serviceIsolated name of isolated service
     */
    public void setServiceIsolated(String serviceIsolated);

    /**
     * Returns the statements expression without annotations.
     *
     * @return expression
     */
    public String getExpressionNoAnnotations();

    /**
     * Returns the current set of listeners for read-only operations.
     *
     * @return listener set
     */
    public EPStatementListenerSet getListenerSet();

    /**
     * Sets the statement listeners.
     * <p>
     * Care must be taken in the use of this method as unsynchronized modification to the
     * listeners of a statement can yield problems.
     *
     * @param listeners  set
     * @param isRecovery whether we are recovering
     */
    public void setListeners(EPStatementListenerSet listeners, boolean isRecovery);

    /**
     * Set statement state.
     *
     * @param currentState        new current state
     * @param timeLastStateChange the timestamp the statement changed state
     */
    public void setCurrentState(EPStatementState currentState, long timeLastStateChange);

    /**
     * Sets the parent view.
     *
     * @param viewable is the statement viewable
     */
    public void setParentView(Viewable viewable);

    /**
     * Returns the parent view.
     *
     * @return viewable is the statement parent viewable
     */
    public Viewable getParentView();

    /**
     * Returns additional metadata about a statement.
     *
     * @return statement metadata
     */
    public StatementMetadata getStatementMetadata();

    /**
     * Returns the statement context.
     *
     * @return statement context
     */
    public StatementContext getStatementContext();

    /**
     * True if an explicit statement name has been provided, false if the statement name is system-generated.
     *
     * @return indicator if statement name exists
     */
    public boolean isNameProvided();
}
