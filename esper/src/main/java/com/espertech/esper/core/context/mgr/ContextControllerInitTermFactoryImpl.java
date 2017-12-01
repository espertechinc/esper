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
package com.espertech.esper.core.context.mgr;

import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.util.ExprNodeUtilityRich;
import com.espertech.esper.epl.spec.ContextDetailInitiatedTerminated;

public class ContextControllerInitTermFactoryImpl extends ContextControllerInitTermFactoryBase implements ContextControllerFactory {

    private final ContextStatePathValueBinding binding;
    private final ExprEvaluator[] distinctEvaluators;

    public ContextControllerInitTermFactoryImpl(ContextControllerFactoryContext factoryContext, ContextDetailInitiatedTerminated detail) {
        super(factoryContext, detail);
        this.binding = factoryContext.getStateCache().getBinding(detail);

        if (detail.getDistinctExpressions() != null && detail.getDistinctExpressions().length > 0) {
            distinctEvaluators = ExprNodeUtilityRich.getEvaluatorsMayCompile(detail.getDistinctExpressions(), factoryContext.getServicesContext().getEngineImportService(), ContextControllerInitTermFactoryImpl.class, false, factoryContext.getAgentInstanceContextCreate().getStatementName());
        } else {
            distinctEvaluators = null;
        }
    }

    public ExprEvaluator[] getDistinctEvaluators() {
        return distinctEvaluators;
    }

    public ContextStatePathValueBinding getBinding() {
        return binding;
    }

    public ContextController createNoCallback(int pathId, ContextControllerLifecycleCallback callback) {
        return new ContextControllerInitTerm(pathId, callback, this);
    }

    public boolean isSingleInstanceContext() {
        return !getContextDetail().isOverlapping();
    }
}
