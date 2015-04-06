/*
 * *************************************************************************************
 *  Copyright (C) 2006-2015 EsperTech, Inc. All rights reserved.                       *
 *  http://www.espertech.com/esper                                                     *
 *  http://www.espertech.com                                                           *
 *  ---------------------------------------------------------------------------------- *
 *  The software in this package is published under the terms of the GPL license       *
 *  a copy of which has been included with this distribution in the license.txt file.  *
 * *************************************************************************************
 */

package com.espertech.esper.core.context.factory;

import com.espertech.esper.epl.core.ResultSetProcessorFactoryDesc;
import com.espertech.esper.epl.expression.core.ExprNode;

public class StatementAgentInstanceFactoryOnTriggerSplitDesc {

    private final ResultSetProcessorFactoryDesc[] processorFactories;
    private final ExprNode[] whereClauses;
    private final boolean[] namedWindowInsert;

    public StatementAgentInstanceFactoryOnTriggerSplitDesc(ResultSetProcessorFactoryDesc[] processors, ExprNode[] whereClauses, boolean[] namedWindowInsert) {
        this.processorFactories = processors;
        this.whereClauses = whereClauses;
        this.namedWindowInsert = namedWindowInsert;
    }

    public ResultSetProcessorFactoryDesc[] getProcessorFactories() {
        return processorFactories;
    }

    public ExprNode[] getWhereClauses() {
        return whereClauses;
    }

    public boolean[] getNamedWindowInsert() {
        return namedWindowInsert;
    }
}
