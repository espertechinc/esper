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
package com.espertech.esper.common.internal.context.aifactory.ontrigger.ontrigger;

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.context.util.StatementAgentInstanceLock;
import com.espertech.esper.common.internal.context.util.StatementContext;
import com.espertech.esper.common.internal.epl.namedwindow.core.NamedWindow;
import com.espertech.esper.common.internal.epl.ontrigger.InfraOnExprBaseViewFactory;
import com.espertech.esper.common.internal.epl.ontrigger.InfraOnMergeHelper;
import com.espertech.esper.common.internal.epl.ontrigger.InfraOnMergeViewFactory;
import com.espertech.esper.common.internal.epl.table.core.Table;

public class StatementAgentInstanceFactoryOnTriggerInfraMerge extends StatementAgentInstanceFactoryOnTriggerInfraBase {
    private InfraOnMergeHelper mergeHelper;

    public void setMergeHelper(InfraOnMergeHelper mergeHelper) {
        this.mergeHelper = mergeHelper;
    }

    protected boolean isSelect() {
        return false;
    }

    protected InfraOnExprBaseViewFactory setupFactory(EventType infraEventType, NamedWindow namedWindow, Table table, StatementContext statementContext) {
        return new InfraOnMergeViewFactory(infraEventType, mergeHelper);
    }

    public StatementAgentInstanceLock obtainAgentInstanceLock(StatementContext statementContext, int agentInstanceId) {
        return StatementAgentInstanceFactoryOnTriggerUtil.obtainAgentInstanceLock(this, statementContext, agentInstanceId);
    }
}
