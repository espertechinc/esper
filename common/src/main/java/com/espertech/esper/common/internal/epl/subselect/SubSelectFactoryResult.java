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
package com.espertech.esper.common.internal.epl.subselect;

import com.espertech.esper.common.internal.context.activator.ViewableActivationResult;
import com.espertech.esper.common.internal.epl.agg.core.AggregationService;
import com.espertech.esper.common.internal.epl.expression.prior.PriorEvalStrategy;
import com.espertech.esper.common.internal.epl.index.base.EventTable;
import com.espertech.esper.common.internal.epl.lookup.SubordTableLookupStrategy;
import com.espertech.esper.common.internal.view.core.Viewable;
import com.espertech.esper.common.internal.view.previous.PreviousGetterStrategy;

public class SubSelectFactoryResult {
    private final ViewableActivationResult subselectActivationResult;
    private final SubordTableLookupStrategy lookupStrategy;
    private final SubselectAggregationPreprocessorBase subselectAggregationPreprocessor;
    private final AggregationService aggregationService;
    private final PriorEvalStrategy priorStrategy;
    private final PreviousGetterStrategy previousStrategy;
    private final Viewable subselectView;
    private final EventTable[] indexes;

    public SubSelectFactoryResult(ViewableActivationResult subselectActivationResult, SubSelectStrategyRealization realization, SubordTableLookupStrategy lookupStrategy) {
        this.subselectActivationResult = subselectActivationResult;
        this.lookupStrategy = lookupStrategy;
        this.subselectAggregationPreprocessor = realization.getSubselectAggregationPreprocessor();
        this.aggregationService = realization.getAggregationService();
        this.priorStrategy = realization.getPriorStrategy();
        this.previousStrategy = realization.getPreviousStrategy();
        this.subselectView = realization.getSubselectView();
        this.indexes = realization.getIndexes();
    }

    public ViewableActivationResult getSubselectActivationResult() {
        return subselectActivationResult;
    }

    public SubordTableLookupStrategy getLookupStrategy() {
        return lookupStrategy;
    }

    public SubselectAggregationPreprocessorBase getSubselectAggregationPreprocessor() {
        return subselectAggregationPreprocessor;
    }

    public AggregationService getAggregationService() {
        return aggregationService;
    }

    public PriorEvalStrategy getPriorStrategy() {
        return priorStrategy;
    }

    public PreviousGetterStrategy getPreviousStrategy() {
        return previousStrategy;
    }

    public Viewable getSubselectView() {
        return subselectView;
    }

    public EventTable[] getIndexes() {
        return indexes;
    }
}
