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
package com.espertech.esper.common.internal.view.groupwin;

import com.espertech.esper.common.internal.context.util.AgentInstanceStopCallback;
import com.espertech.esper.common.internal.context.util.AgentInstanceStopServices;
import com.espertech.esper.common.internal.view.core.AgentInstanceViewFactoryChainContext;
import com.espertech.esper.common.internal.view.core.View;
import com.espertech.esper.common.internal.view.core.ViewFactory;

public class GroupByViewUtil {
    protected static View makeSubView(GroupByView view, Object groupKey) {
        AgentInstanceViewFactoryChainContext agentInstanceContext = view.getAgentInstanceContext();
        MergeView mergeView = view.getMergeView();

        ViewFactory[] factories = view.getViewFactory().getGroupeds();
        View first = factories[0].makeView(agentInstanceContext);
        first.setParent(view);
        View currentParent = first;
        for (int i = 1; i < factories.length; i++) {
            View next = factories[i].makeView(agentInstanceContext);
            next.setParent(currentParent);
            currentParent.setChild(next);
            currentParent = next;
        }

        if (view.getViewFactory().isAddingProperties()) {
            AddPropertyValueOptionalView adder = new AddPropertyValueOptionalView(view.getViewFactory(), agentInstanceContext, groupKey);
            currentParent.setChild(adder);
            adder.setParent(currentParent);

            adder.setChild(mergeView);
            mergeView.addParentView(adder);
        } else {
            currentParent.setChild(mergeView);
            mergeView.addParentView(currentParent);
        }

        return first;
    }

    public static void removeSubview(View view, AgentInstanceStopServices services) {
        view.setParent(null);
        if (view instanceof AgentInstanceStopCallback) {
            ((AgentInstanceStopCallback) view).stop(services);
        }
        recursiveChildRemove(view, services);
    }

    private static void recursiveChildRemove(View view, AgentInstanceStopServices services) {
        View child = view.getChild();
        if (child == null) {
            return;
        }
        if (child instanceof MergeView) {
            MergeView mergeView = (MergeView) child;
            mergeView.removeParentView(view);
        } else {
            if (child instanceof AgentInstanceStopCallback) {
                ((AgentInstanceStopCallback) child).stop(services);
            }
            recursiveChildRemove(child, services);
        }
    }
}
