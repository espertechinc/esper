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
package com.espertech.esper.view.std;

import com.espertech.esper.client.EPException;
import com.espertech.esper.core.context.util.AgentInstanceViewFactoryChainContext;
import com.espertech.esper.epl.expression.core.ExprNodeUtilityCore;
import com.espertech.esper.view.GroupableView;
import com.espertech.esper.view.View;

public class GroupByViewUtil {
    /**
     * Instantiate subviews for the given group view and the given key value to group-by.
     * Makes shallow copies of each child view and its subviews up to the merge point.
     * Sets up merge data views for merging the group-by key value back in.
     *
     * @param groupView            is the parent view for which to copy subviews for
     * @param groupByValues        is the key values to group-by
     * @param agentInstanceContext is the view services that sub-views may need
     * @return a single view or a list of views that are copies of the original list, with copied children, with
     * data merge views added to the copied child leaf views.
     */
    public static View makeSubView(GroupByView groupView, Object groupByValues, AgentInstanceViewFactoryChainContext agentInstanceContext) {
        if (!groupView.hasViews()) {
            String message = "Unexpected empty list of child nodes for group view";
            throw new EPException(message);
        }
        return copyChildView(groupView, groupByValues, agentInstanceContext, groupView.getViews()[0]);
    }

    private static View copyChildView(GroupByView groupView, Object groupByValues, AgentInstanceViewFactoryChainContext agentInstanceContext, View originalChildView) {
        if (originalChildView instanceof MergeView) {
            String message = "Unexpected merge view as child of group-by view";
            throw new EPException(message);
        }

        GroupableView cloneableView = (GroupableView) originalChildView;

        // Copy child node
        View copyChildView = cloneableView.getViewFactory().makeView(agentInstanceContext);
        copyChildView.setParent(groupView);

        // Make the sub views for child copying from the original to the child
        copySubViews(groupView, groupByValues, originalChildView, copyChildView, agentInstanceContext);

        return copyChildView;
    }

    private static void copySubViews(GroupByView groupByView, Object groupByValues, View originalView, View copyView, AgentInstanceViewFactoryChainContext agentInstanceContext) {
        for (View subView : originalView.getViews()) {
            // Determine if view is our merge view
            if (subView instanceof MergeViewMarker) {
                MergeViewMarker mergeView = (MergeViewMarker) subView;
                if (ExprNodeUtilityCore.deepEquals(mergeView.getGroupFieldNames(), groupByView.getViewFactory().getCriteriaExpressions(), false)) {
                    if (mergeView.getEventType() != copyView.getEventType()) {
                        // We found our merge view - install a new data merge view on top of it
                        AddPropertyValueOptionalView addPropertyView = new AddPropertyValueOptionalView(agentInstanceContext, groupByView.getViewFactory().getPropertyNames(), groupByValues, mergeView.getEventType());

                        // Add to the copied parent subview the view merge data view
                        copyView.addView(addPropertyView);

                        // Add to the new merge data view the actual single merge view instance that clients may attached to
                        addPropertyView.addView(mergeView);

                        // Add a parent view to the single merge view instance
                        mergeView.addParentView(addPropertyView);
                    } else {
                        // Add to the copied parent subview the view merge data view
                        copyView.addView(mergeView);

                        // Add a parent view to the single merge view instance
                        mergeView.addParentView(copyView);
                    }

                    continue;
                }
            }

            GroupableView cloneableView = (GroupableView) subView;
            View copiedChild = cloneableView.getViewFactory().makeView(agentInstanceContext);
            copyView.addView(copiedChild);

            // Make the sub views for child
            copySubViews(groupByView, groupByValues, subView, copiedChild, agentInstanceContext);
        }
    }
}
