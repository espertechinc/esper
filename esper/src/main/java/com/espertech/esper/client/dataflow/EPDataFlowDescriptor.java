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
package com.espertech.esper.client.dataflow;

import com.espertech.esper.client.EPStatementState;

/**
 * Data flow descriptor.
 */
public class EPDataFlowDescriptor {
    private final String dataFlowName;
    private final EPStatementState statementState;
    private final String statementName;

    /**
     * Ctor.
     *
     * @param dataFlowName   data flow name
     * @param statementState statement state
     * @param statementName  statement name
     */
    public EPDataFlowDescriptor(String dataFlowName, EPStatementState statementState, String statementName) {
        this.dataFlowName = dataFlowName;
        this.statementState = statementState;
        this.statementName = statementName;
    }

    /**
     * Returns the data flow name.
     *
     * @return name
     */
    public String getDataFlowName() {
        return dataFlowName;
    }

    /**
     * Returns the statement state.
     *
     * @return statement state
     */
    public EPStatementState getStatementState() {
        return statementState;
    }

    /**
     * Returns the statement name.
     *
     * @return statement name.
     */
    public String getStatementName() {
        return statementName;
    }
}
