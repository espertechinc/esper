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

import com.espertech.esper.common.internal.context.util.StatementAgentInstanceLock;
import com.espertech.esper.common.internal.context.util.StatementContext;
import com.espertech.esper.common.internal.epl.namedwindow.core.NamedWindow;
import com.espertech.esper.common.internal.epl.namedwindow.core.NamedWindowInstance;
import com.espertech.esper.common.internal.epl.table.core.Table;
import com.espertech.esper.common.internal.epl.table.core.TableInstance;

public class StatementAgentInstanceFactoryOnTriggerUtil {
    public static StatementAgentInstanceLock obtainAgentInstanceLock(StatementAgentInstanceFactoryOnTriggerInfraBase base, StatementContext statementContext, int agentInstanceId) {
        NamedWindow namedWindow = base.getNamedWindow();
        if (namedWindow != null) {
            NamedWindowInstance instance;
            if (agentInstanceId == -1) {
                instance = namedWindow.getNamedWindowInstanceNoContext();
            } else {
                instance = namedWindow.getNamedWindowInstance(agentInstanceId);
            }
            return instance.getRootViewInstance().getAgentInstanceContext().getAgentInstanceLock();
        }

        Table table = base.getTable();
        TableInstance instance;
        if (agentInstanceId == -1) {
            instance = table.getTableInstanceNoContext();
        } else {
            instance = table.getTableInstance(agentInstanceId);
        }
        return instance.getAgentInstanceContext().getAgentInstanceLock();
    }
}
