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
package com.espertech.esper.common.internal.epl.dataflow.interfaces;

import com.espertech.esper.common.internal.context.util.StatementContext;

public class DataFlowOpFactoryInitializeContext {

    private final String dataFlowName;
    private final int operatorNumber;
    private final StatementContext statementContext;

    public DataFlowOpFactoryInitializeContext(String dataFlowName, int operatorNumber, StatementContext statementContext) {
        this.dataFlowName = dataFlowName;
        this.operatorNumber = operatorNumber;
        this.statementContext = statementContext;
    }

    public StatementContext getStatementContext() {
        return statementContext;
    }

    public int getOperatorNumber() {
        return operatorNumber;
    }

    public String getDataFlowName() {
        return dataFlowName;
    }
}
