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
package com.espertech.esper.common.internal.epl.prior;

import com.espertech.esper.common.internal.collection.ViewUpdatedCollection;
import com.espertech.esper.common.internal.epl.expression.prior.ExprPriorEvalStrategyRandomAccess;
import com.espertech.esper.common.internal.epl.expression.prior.ExprPriorEvalStrategyRelativeAccess;
import com.espertech.esper.common.internal.epl.expression.prior.PriorEvalStrategy;
import com.espertech.esper.common.internal.view.access.RandomAccessByIndex;
import com.espertech.esper.common.internal.view.access.RelativeAccessByEventNIndex;
import com.espertech.esper.common.internal.view.core.AgentInstanceViewFactoryChainContext;
import com.espertech.esper.common.internal.view.core.ViewFactory;
import com.espertech.esper.common.internal.view.prior.PriorEventViewFactory;

public class PriorHelper {
    public static PriorEventViewFactory findPriorViewFactory(ViewFactory[] factories) {
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

    public static PriorEvalStrategy toStrategy(AgentInstanceViewFactoryChainContext viewFactoryChainContext) {
        ViewUpdatedCollection priorViewUpdatedCollection = viewFactoryChainContext.getPriorViewUpdatedCollection();
        if (priorViewUpdatedCollection instanceof RandomAccessByIndex) {
            return new ExprPriorEvalStrategyRandomAccess((RandomAccessByIndex) priorViewUpdatedCollection);
        }
        if (priorViewUpdatedCollection instanceof RelativeAccessByEventNIndex) {
            return new ExprPriorEvalStrategyRelativeAccess((RelativeAccessByEventNIndex) priorViewUpdatedCollection);
        }
        return null;
    }
}
