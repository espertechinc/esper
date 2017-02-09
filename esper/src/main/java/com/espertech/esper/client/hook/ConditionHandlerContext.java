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
package com.espertech.esper.client.hook;

/**
 * Context provided to {@link ConditionHandler} implementations providing
 * engine-condition-contextual information.
 * <p>
 * Statement information pertains to the statement currently being processed when the condition occured.
 */
public class ConditionHandlerContext {
    private final String engineURI;
    private final String statementName;
    private final String epl;
    private final BaseCondition engineCondition;

    /**
     * Ctor.
     *
     * @param engineURI       engine URI
     * @param statementName   statement name
     * @param epl             statement EPL expression text
     * @param engineCondition condition reported
     */
    public ConditionHandlerContext(String engineURI, String statementName, String epl, BaseCondition engineCondition) {
        this.engineURI = engineURI;
        this.statementName = statementName;
        this.epl = epl;
        this.engineCondition = engineCondition;
    }

    /**
     * Returns the engine URI.
     *
     * @return engine URI
     */
    public String getEngineURI() {
        return engineURI;
    }

    /**
     * Returns the statement name, if provided, or the statement id assigned to the statement if no name was provided.
     *
     * @return statement name or id
     */
    public String getStatementName() {
        return statementName;
    }

    /**
     * Returns the expression text of the statement.
     *
     * @return statement.
     */
    public String getEpl() {
        return epl;
    }

    /**
     * Returns the condition reported.
     *
     * @return condition reported
     */
    public BaseCondition getEngineCondition() {
        return engineCondition;
    }
}
