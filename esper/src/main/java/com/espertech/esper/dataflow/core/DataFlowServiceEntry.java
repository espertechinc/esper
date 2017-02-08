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
package com.espertech.esper.dataflow.core;

import com.espertech.esper.client.EPStatementState;

public class DataFlowServiceEntry {
    private final DataFlowStmtDesc dataFlowDesc;
    private EPStatementState state;

    public DataFlowServiceEntry(DataFlowStmtDesc dataFlowDesc, EPStatementState state) {
        this.dataFlowDesc = dataFlowDesc;
        this.state = state;
    }

    public DataFlowStmtDesc getDataFlowDesc() {
        return dataFlowDesc;
    }

    public EPStatementState getState() {
        return state;
    }

    public void setState(EPStatementState state) {
        this.state = state;
    }
}
