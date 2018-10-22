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
package com.espertech.esper.common.internal.epl.ontrigger;

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.context.util.AgentInstanceContext;
import com.espertech.esper.common.internal.epl.lookupplansubord.SubordWMatchExprLookupStrategy;
import com.espertech.esper.common.internal.epl.namedwindow.core.NamedWindowRootViewInstance;
import com.espertech.esper.common.internal.epl.table.core.TableInstance;

/**
 * View for the on-delete statement that handles removing events from a named window.
 */
public class InfraOnDeleteViewFactory extends InfraOnExprBaseViewFactory {

    public InfraOnDeleteViewFactory(EventType infaEventType) {
        super(infaEventType);
    }

    public InfraOnExprBaseViewResult makeNamedWindow(SubordWMatchExprLookupStrategy lookupStrategy, NamedWindowRootViewInstance namedWindowRootViewInstance, AgentInstanceContext agentInstanceContext) {
        return new InfraOnExprBaseViewResult(new OnExprViewNamedWindowDelete(lookupStrategy, namedWindowRootViewInstance, agentInstanceContext), null);
    }

    public InfraOnExprBaseViewResult makeTable(SubordWMatchExprLookupStrategy lookupStrategy, TableInstance tableInstance, AgentInstanceContext agentInstanceContext) {
        return new InfraOnExprBaseViewResult(new OnExprViewTableDelete(lookupStrategy, tableInstance, agentInstanceContext), null);
    }
}
