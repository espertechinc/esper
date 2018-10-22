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
package com.espertech.esper.common.internal.epl.util;

import com.espertech.esper.common.internal.epl.expression.core.ExprValidationException;
import com.espertech.esper.common.internal.epl.expression.prev.ExprPreviousNode;
import com.espertech.esper.common.internal.epl.expression.prior.ExprPriorNode;
import com.espertech.esper.common.internal.view.access.ViewResourceDelegateDesc;
import com.espertech.esper.common.internal.view.access.ViewResourceDelegateExpr;
import com.espertech.esper.common.internal.view.core.DataWindowViewForgeWithPrevious;
import com.espertech.esper.common.internal.view.core.ViewFactoryForge;
import com.espertech.esper.common.internal.view.groupwin.GroupByViewFactoryForge;

import java.util.*;

public class ViewResourceVerifyHelper {
    public static ViewResourceDelegateDesc[] verifyPreviousAndPriorRequirements(List<ViewFactoryForge>[] unmaterializedViewChain, ViewResourceDelegateExpr delegate)
            throws ExprValidationException {

        int numStreams = unmaterializedViewChain.length;
        ViewResourceDelegateDesc[] perStream = new ViewResourceDelegateDesc[numStreams];

        // verify "previous"
        boolean[] previousPerStream = new boolean[numStreams];
        for (ExprPreviousNode previousNode : delegate.getPreviousRequests()) {
            int stream = previousNode.getStreamNumber();
            List<ViewFactoryForge> forges = unmaterializedViewChain[stream];

            boolean pass = inspectViewFactoriesForPrevious(forges);
            if (!pass) {
                throw new ExprValidationException("Previous function requires a single data window view onto the stream");
            }

            boolean found = findDataWindow(forges);
            if (!found) {
                throw new ExprValidationException("Required data window not found for the 'prev' function, specify a data window for which previous events are retained");
            }

            previousPerStream[stream] = true;
        }

        // determine 'prior' indexes
        SortedMap<Integer, List<ExprPriorNode>>[] priorPerStream = new SortedMap[numStreams];
        for (ExprPriorNode priorNode : delegate.getPriorRequests()) {
            int stream = priorNode.getStreamNumber();

            if (priorPerStream[stream] == null) {
                priorPerStream[stream] = new TreeMap<Integer, List<ExprPriorNode>>();
            }

            TreeMap<Integer, List<ExprPriorNode>> treemap = (TreeMap<Integer, List<ExprPriorNode>>) priorPerStream[stream];
            List<ExprPriorNode> callbackList = treemap.get(priorNode.getConstantIndexNumber());
            if (callbackList == null) {
                callbackList = new LinkedList<ExprPriorNode>();
                treemap.put(priorNode.getConstantIndexNumber(), callbackList);
            }
            callbackList.add(priorNode);
        }
        // when a given stream has multiple 'prior' nodes, assign a relative index
        for (int i = 0; i < numStreams; i++) {
            if (priorPerStream[i] != null) {
                int relativeIndex = 0;
                for (Map.Entry<Integer, List<ExprPriorNode>> entry : priorPerStream[i].entrySet()) {
                    for (ExprPriorNode node : entry.getValue()) {
                        node.setRelativeIndex(relativeIndex);
                    }
                    relativeIndex++;
                }
            }
        }


        // build per-stream info
        for (int i = 0; i < numStreams; i++) {
            if (priorPerStream[i] == null) {
                priorPerStream[i] = new TreeMap<>();
            }
            perStream[i] = new ViewResourceDelegateDesc(previousPerStream[i], new TreeSet<>(priorPerStream[i].keySet()));
        }

        return perStream;
    }

    private static boolean findDataWindow(List<ViewFactoryForge> forges) {
        for (ViewFactoryForge forge : forges) {
            if (forge instanceof DataWindowViewForgeWithPrevious) {
                return true;
            }
            if (forge instanceof GroupByViewFactoryForge) {
                GroupByViewFactoryForge group = (GroupByViewFactoryForge) forge;
                return findDataWindow(group.getGroupeds());
            }
        }
        return false;
    }

    private static boolean inspectViewFactoriesForPrevious(List<ViewFactoryForge> viewFactories) {
        // We allow the capability only if
        //  - 1 view
        //  - 2 views and the first view is a group-by (for window-per-group access)
        if (viewFactories.size() == 1) {
            return true;
        }
        if (viewFactories.size() == 2) {
            if (viewFactories.get(0) instanceof GroupByViewFactoryForge) {
                return true;
            }
            return false;
        }
        return true;
    }
}
