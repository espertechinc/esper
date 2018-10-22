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
import com.espertech.esper.common.internal.view.core.View;

/**
 * View for the on-delete statement that handles removing events from a named window.
 */
public class InfraOnMergeViewFactory extends InfraOnExprBaseViewFactory {
    private final InfraOnMergeHelper onMergeHelper;

    public InfraOnMergeViewFactory(EventType namedWindowEventType, InfraOnMergeHelper onMergeHelper) {
        super(namedWindowEventType);
        this.onMergeHelper = onMergeHelper;
    }

    public InfraOnExprBaseViewResult makeNamedWindow(SubordWMatchExprLookupStrategy lookupStrategy, NamedWindowRootViewInstance namedWindowRootViewInstance, AgentInstanceContext agentInstanceContext) {
        View view;
        if (onMergeHelper.getInsertUnmatched() != null) {
            view = new OnExprViewNamedWindowMergeInsertUnmatched(namedWindowRootViewInstance, agentInstanceContext, this);
        } else {
            view = new OnExprViewNamedWindowMerge(lookupStrategy, namedWindowRootViewInstance, agentInstanceContext, this);
        }
        return new InfraOnExprBaseViewResult(view, null);
    }

    public InfraOnExprBaseViewResult makeTable(SubordWMatchExprLookupStrategy lookupStrategy, TableInstance tableInstance, AgentInstanceContext agentInstanceContext) {
        View view;
        if (onMergeHelper.getInsertUnmatched() != null) {
            view = new OnExprViewTableMergeInsertUnmatched(tableInstance, agentInstanceContext, this);
        } else {
            view = new OnExprViewTableMerge(lookupStrategy, tableInstance, agentInstanceContext, this);
        }
        return new InfraOnExprBaseViewResult(view, null);
    }

    public InfraOnMergeHelper getOnMergeHelper() {
        return onMergeHelper;
    }
}