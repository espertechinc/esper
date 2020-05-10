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
package com.espertech.esper.common.internal.compile.stage2;

import com.espertech.esper.common.internal.context.util.StatementContextFilterEvalEnv;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluator;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.common.internal.epl.pattern.core.MatchedEventConvertor;
import com.espertech.esper.common.internal.filterspec.FilterValueSetParam;
import com.espertech.esper.common.internal.filterspec.MatchedEventMap;

public class FilterSpecPlan {
    public final static FilterSpecPlan EMPTY_PLAN;
    static {
        EMPTY_PLAN = new FilterSpecPlan(FilterSpecPlanPath.EMPTY_ARRAY, null, null);
        EMPTY_PLAN.initialize();
    }

    protected FilterSpecPlanPath[] paths;
    protected ExprEvaluator filterConfirm;
    protected ExprEvaluator filterNegate;
    protected MatchedEventConvertor convertor;
    private FilterSpecPlanCompute compute;

    public FilterSpecPlan() {
    }

    public FilterSpecPlan(FilterSpecPlanPath[] paths, ExprEvaluator filterConfirm, ExprEvaluator controlNegate) {
        this.paths = paths;
        this.filterConfirm = filterConfirm;
        this.filterNegate = controlNegate;
    }

    public FilterSpecPlanPath[] getPaths() {
        return paths;
    }

    public void setPaths(FilterSpecPlanPath[] paths) {
        this.paths = paths;
    }

    public ExprEvaluator getFilterConfirm() {
        return filterConfirm;
    }

    public void setFilterConfirm(ExprEvaluator filterConfirm) {
        this.filterConfirm = filterConfirm;
    }

    public MatchedEventConvertor getConvertor() {
        return convertor;
    }

    public void setConvertor(MatchedEventConvertor convertor) {
        this.convertor = convertor;
    }

    public ExprEvaluator getFilterNegate() {
        return filterNegate;
    }

    public void setFilterNegate(ExprEvaluator filterNegate) {
        this.filterNegate = filterNegate;
    }

    public void initialize() {
        compute = FilterSpecPlanComputeFactory.make(this);
    }

    public FilterValueSetParam[][] evaluateValueSet(MatchedEventMap matchedEvents, ExprEvaluatorContext exprEvaluatorContext, StatementContextFilterEvalEnv filterEvalEnv) {
        return compute.compute(this, matchedEvents, exprEvaluatorContext, filterEvalEnv);
    }
}
