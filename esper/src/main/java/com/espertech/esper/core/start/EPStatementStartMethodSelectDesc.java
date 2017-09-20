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
package com.espertech.esper.core.start;

import com.espertech.esper.core.context.factory.StatementAgentInstanceFactorySelect;
import com.espertech.esper.core.context.subselect.SubSelectStrategyCollection;
import com.espertech.esper.epl.core.resultset.core.ResultSetProcessorFactoryDesc;
import com.espertech.esper.epl.core.viewres.ViewResourceDelegateUnverified;

/**
 * Starts and provides the stop method for EPL statements.
 */
public class EPStatementStartMethodSelectDesc {
    private final StatementAgentInstanceFactorySelect statementAgentInstanceFactorySelect;
    private final SubSelectStrategyCollection subSelectStrategyCollection;
    private final ViewResourceDelegateUnverified viewResourceDelegateUnverified;
    private final ResultSetProcessorFactoryDesc resultSetProcessorPrototypeDesc;
    private final EPStatementStopMethod stopMethod;
    private final EPStatementDestroyCallbackList destroyCallbacks;

    public EPStatementStartMethodSelectDesc(StatementAgentInstanceFactorySelect statementAgentInstanceFactorySelect, SubSelectStrategyCollection subSelectStrategyCollection, ViewResourceDelegateUnverified viewResourceDelegateUnverified, ResultSetProcessorFactoryDesc resultSetProcessorPrototypeDesc, EPStatementStopMethod stopMethod, EPStatementDestroyCallbackList destroyCallbacks) {
        this.statementAgentInstanceFactorySelect = statementAgentInstanceFactorySelect;
        this.subSelectStrategyCollection = subSelectStrategyCollection;
        this.viewResourceDelegateUnverified = viewResourceDelegateUnverified;
        this.resultSetProcessorPrototypeDesc = resultSetProcessorPrototypeDesc;
        this.stopMethod = stopMethod;
        this.destroyCallbacks = destroyCallbacks;
    }

    public StatementAgentInstanceFactorySelect getStatementAgentInstanceFactorySelect() {
        return statementAgentInstanceFactorySelect;
    }

    public SubSelectStrategyCollection getSubSelectStrategyCollection() {
        return subSelectStrategyCollection;
    }

    public ViewResourceDelegateUnverified getViewResourceDelegateUnverified() {
        return viewResourceDelegateUnverified;
    }

    public ResultSetProcessorFactoryDesc getResultSetProcessorPrototypeDesc() {
        return resultSetProcessorPrototypeDesc;
    }

    public EPStatementStopMethod getStopMethod() {
        return stopMethod;
    }

    public EPStatementDestroyCallbackList getDestroyCallbacks() {
        return destroyCallbacks;
    }
}
