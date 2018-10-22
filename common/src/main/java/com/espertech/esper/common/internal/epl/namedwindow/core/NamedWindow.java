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
package com.espertech.esper.common.internal.epl.namedwindow.core;

import com.espertech.esper.common.internal.context.util.AgentInstanceContext;
import com.espertech.esper.common.internal.context.util.StatementContext;
import com.espertech.esper.common.internal.epl.expression.core.ExprValidationException;
import com.espertech.esper.common.internal.epl.join.lookup.IndexMultiKey;
import com.espertech.esper.common.internal.epl.join.queryplan.QueryPlanIndexItem;
import com.espertech.esper.common.internal.epl.lookupplansubord.EventTableIndexMetadata;

public interface NamedWindow {
    String getName();

    NamedWindowRootView getRootView();

    NamedWindowTailView getTailView();

    NamedWindowInstance getNamedWindowInstance(AgentInstanceContext agentInstanceContext);

    NamedWindowInstance getNamedWindowInstance(int cpid);

    NamedWindowInstance getNamedWindowInstanceNoContext();

    EventTableIndexMetadata getEventTableIndexMetadata();

    void removeAllInstanceIndexes(IndexMultiKey index);

    void removeIndexReferencesStmtMayRemoveIndex(IndexMultiKey imk, String referringDeploymentId, String referringStatementName);

    void validateAddIndex(String deploymentId, String statementName, String indexName, String indexModuleName, QueryPlanIndexItem explicitIndexDesc, IndexMultiKey indexMultiKey) throws ExprValidationException;

    void setStatementContext(StatementContext statementContext);

    StatementContext getStatementContext();
}
