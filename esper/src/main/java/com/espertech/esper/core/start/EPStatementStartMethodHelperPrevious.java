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

import com.espertech.esper.core.context.util.AgentInstanceViewFactoryChainContext;
import com.espertech.esper.epl.core.viewres.ViewResourceDelegateVerified;
import com.espertech.esper.epl.core.viewres.ViewResourceDelegateVerifiedStream;
import com.espertech.esper.epl.expression.prev.*;
import com.espertech.esper.view.DataWindowViewWithPrevious;
import com.espertech.esper.view.ViewFactory;
import com.espertech.esper.view.window.RandomAccessByIndexGetter;
import com.espertech.esper.view.window.RelativeAccessByEventNIndexGetter;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Starts and provides the stop method for EPL statements.
 */
public class EPStatementStartMethodHelperPrevious {
    public static Map<ExprPreviousNode, ExprPreviousEvalStrategy> compilePreviousNodeStrategies(ViewResourceDelegateVerified viewResourceDelegate, AgentInstanceViewFactoryChainContext[] contexts) {

        if (!viewResourceDelegate.isHasPrevious()) {
            return Collections.emptyMap();
        }

        Map<ExprPreviousNode, ExprPreviousEvalStrategy> strategies = new HashMap<ExprPreviousNode, ExprPreviousEvalStrategy>();

        for (int streamNum = 0; streamNum < contexts.length; streamNum++) {

            // get stream-specific info
            ViewResourceDelegateVerifiedStream delegate = viewResourceDelegate.getPerStream()[streamNum];

            // obtain getter
            handlePrevious(delegate.getPreviousRequests(), contexts[streamNum].getPreviousNodeGetter(), strategies);
        }

        return strategies;
    }

    public static DataWindowViewWithPrevious findPreviousViewFactory(List<ViewFactory> factories) {
        ViewFactory factoryFound = null;
        for (ViewFactory factory : factories) {
            if (factory instanceof DataWindowViewWithPrevious) {
                factoryFound = factory;
                break;
            }
        }
        if (factoryFound == null) {
            throw new RuntimeException("Failed to find 'previous'-handling view factory");  // was verified earlier, should not occur
        }
        return (DataWindowViewWithPrevious) factoryFound;
    }

    private static void handlePrevious(List<ExprPreviousNode> previousRequests, Object previousNodeGetter, Map<ExprPreviousNode, ExprPreviousEvalStrategy> strategies) {

        if (previousRequests.isEmpty()) {
            return;
        }

        RandomAccessByIndexGetter randomAccessGetter = null;
        RelativeAccessByEventNIndexGetter relativeAccessGetter = null;
        if (previousNodeGetter instanceof RandomAccessByIndexGetter) {
            randomAccessGetter = (RandomAccessByIndexGetter) previousNodeGetter;
        } else if (previousNodeGetter instanceof RelativeAccessByEventNIndexGetter) {
            relativeAccessGetter = (RelativeAccessByEventNIndexGetter) previousNodeGetter;
        } else {
            throw new RuntimeException("Unexpected 'previous' handler: " + previousNodeGetter);
        }

        for (ExprPreviousNode previousNode : previousRequests) {
            int streamNumber = previousNode.getStreamNumber();
            ExprPreviousNodePreviousType previousType = previousNode.getPreviousType();
            ExprPreviousEvalStrategy evaluator;

            if (previousType == ExprPreviousNodePreviousType.PREVWINDOW) {
                evaluator = new ExprPreviousEvalStrategyWindow(streamNumber, previousNode.getChildNodes()[1].getForge().getExprEvaluator(), previousNode.getResultType().getComponentType(),
                        randomAccessGetter, relativeAccessGetter);
            } else if (previousType == ExprPreviousNodePreviousType.PREVCOUNT) {
                evaluator = new ExprPreviousEvalStrategyCount(streamNumber, randomAccessGetter, relativeAccessGetter);
            } else {
                evaluator = new ExprPreviousEvalStrategyPrev(streamNumber, previousNode.getChildNodes()[0].getForge().getExprEvaluator(), previousNode.getChildNodes()[1].getForge().getExprEvaluator(),
                        randomAccessGetter, relativeAccessGetter, previousNode.isConstantIndex(), previousNode.getConstantIndexNumber(), previousType == ExprPreviousNodePreviousType.PREVTAIL);
            }

            strategies.put(previousNode, evaluator);
        }
    }
}
