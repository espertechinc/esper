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

import com.espertech.esper.client.EPException;
import com.espertech.esper.collection.ViewUpdatedCollection;
import com.espertech.esper.core.context.util.AgentInstanceViewFactoryChainContext;
import com.espertech.esper.core.service.StatementContext;
import com.espertech.esper.epl.core.viewres.ViewResourceDelegateVerified;
import com.espertech.esper.epl.expression.core.ExprConstantNodeImpl;
import com.espertech.esper.epl.expression.core.ExprNode;
import com.espertech.esper.epl.expression.prior.ExprPriorEvalStrategy;
import com.espertech.esper.epl.expression.prior.ExprPriorEvalStrategyRandomAccess;
import com.espertech.esper.epl.expression.prior.ExprPriorEvalStrategyRelativeAccess;
import com.espertech.esper.epl.expression.prior.ExprPriorNode;
import com.espertech.esper.view.*;
import com.espertech.esper.view.internal.PriorEventViewFactory;
import com.espertech.esper.view.internal.PriorEventViewRelAccess;
import com.espertech.esper.view.window.RandomAccessByIndex;
import com.espertech.esper.view.window.RelativeAccessByEventNIndex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Starts and provides the stop method for EPL statements.
 */
public class EPStatementStartMethodHelperPrior {
    private static final Logger log = LoggerFactory.getLogger(EPStatementStartMethodHelperPrior.class);

    public static PriorEventViewFactory findPriorViewFactory(List<ViewFactory> factories) {
        ViewFactory factoryFound = null;
        for (ViewFactory factory : factories) {
            if (factory instanceof PriorEventViewFactory) {
                factoryFound = factory;
                break;
            }
        }
        if (factoryFound == null) {
            throw new RuntimeException("Failed to find 'prior'-handling view factory");  // was verified earlier, should not occur
        }
        return (PriorEventViewFactory) factoryFound;
    }

    public static PriorEventViewFactory getPriorEventViewFactory(StatementContext statementContext, int streamNum, boolean unboundStream, boolean isSubquery, int subqueryNumber) {
        try {
            String namespace = ViewEnum.PRIOR_EVENT_VIEW.getNamespace();
            String name = ViewEnum.PRIOR_EVENT_VIEW.getName();
            ViewFactory factory = statementContext.getViewResolutionService().create(namespace, name);

            ViewFactoryContext context = new ViewFactoryContext(statementContext, streamNum, namespace, name, isSubquery, subqueryNumber, false);
            factory.setViewParameters(context, Arrays.asList((ExprNode) new ExprConstantNodeImpl(unboundStream)));

            return (PriorEventViewFactory) factory;
        } catch (ViewProcessingException ex) {
            String text = "Exception creating prior event view factory";
            throw new EPException(text, ex);
        } catch (ViewParameterException ex) {
            String text = "Exception creating prior event view factory";
            throw new EPException(text, ex);
        }
    }

    public static Map<ExprPriorNode, ExprPriorEvalStrategy> compilePriorNodeStrategies(ViewResourceDelegateVerified viewResourceDelegate, AgentInstanceViewFactoryChainContext[] viewFactoryChainContexts) {

        if (!viewResourceDelegate.isHasPrior()) {
            return Collections.emptyMap();
        }

        Map<ExprPriorNode, ExprPriorEvalStrategy> strategies = new HashMap<ExprPriorNode, ExprPriorEvalStrategy>();

        for (int streamNum = 0; streamNum < viewResourceDelegate.getPerStream().length; streamNum++) {
            ViewUpdatedCollection viewUpdatedCollection = viewFactoryChainContexts[streamNum].getPriorViewUpdatedCollection();
            SortedMap<Integer, List<ExprPriorNode>> callbacksPerIndex = viewResourceDelegate.getPerStream()[streamNum].getPriorRequests();
            handlePrior(viewUpdatedCollection, callbacksPerIndex, strategies);
        }

        return strategies;
    }

    private static void handlePrior(ViewUpdatedCollection viewUpdatedCollection, SortedMap<Integer, List<ExprPriorNode>> callbacksPerIndex, Map<ExprPriorNode, ExprPriorEvalStrategy> strategies) {

        // Since an expression such as "prior(2, price), prior(8, price)" translates
        // into {2, 8} the relative index is {0, 1}.
        // Map the expression-supplied index to a relative viewUpdatedCollection-known index via wrapper
        int relativeIndex = 0;
        for (int reqIndex : callbacksPerIndex.keySet()) {
            List<ExprPriorNode> priorNodes = callbacksPerIndex.get(reqIndex);
            for (ExprPriorNode callback : priorNodes) {
                ExprPriorEvalStrategy strategy;
                if (viewUpdatedCollection instanceof RelativeAccessByEventNIndex) {
                    RelativeAccessByEventNIndex relativeAccess = (RelativeAccessByEventNIndex) viewUpdatedCollection;
                    PriorEventViewRelAccess impl = new PriorEventViewRelAccess(relativeAccess, relativeIndex);
                    strategy = new ExprPriorEvalStrategyRelativeAccess(impl);
                } else {
                    if (viewUpdatedCollection instanceof RandomAccessByIndex) {
                        strategy = new ExprPriorEvalStrategyRandomAccess((RandomAccessByIndex) viewUpdatedCollection);
                    } else {
                        strategy = new ExprPriorEvalStrategyRelativeAccess((RelativeAccessByEventNIndex) viewUpdatedCollection);
                    }
                }

                strategies.put(callback, strategy);
            }
            relativeIndex++;
        }
    }
}
