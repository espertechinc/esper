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
package com.espertech.esper.common.internal.filterspec;

import com.espertech.esper.common.internal.bytecodemodel.core.CodegenNamedParam;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionRef;
import com.espertech.esper.common.internal.context.util.StatementContextFilterEvalEnv;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.common.internal.epl.expression.core.ExprFilterSpecLookupable;

import java.util.Collection;
import java.util.List;

import static com.espertech.esper.common.internal.epl.expression.codegen.ExprForgeCodegenNames.REF_EXPREVALCONTEXT;

/**
 * This class represents one filter parameter in an {@link FilterSpecActivatable} filter specification.
 * <p> Each filerting parameter has an attribute name and operator type.
 */
public abstract class FilterSpecParam {
    public final static CodegenExpressionRef REF_MATCHEDEVENTMAP = new CodegenExpressionRef("matchedEvents");
    public final static CodegenExpressionRef REF_STMTCTXFILTEREVALENV = new CodegenExpressionRef("stmtCtxFilterEnv");
    public final static List<CodegenNamedParam> GET_FILTER_VALUE_FP = CodegenNamedParam.from(
            MatchedEventMap.class, REF_MATCHEDEVENTMAP.getRef(),
            ExprEvaluatorContext.class, REF_EXPREVALCONTEXT.getRef(),
            StatementContextFilterEvalEnv.class, REF_STMTCTXFILTEREVALENV.getRef());
    public final static CodegenExpression[] GET_FILTER_VALUE_REFS = new CodegenExpressionRef[]{REF_MATCHEDEVENTMAP, REF_EXPREVALCONTEXT, REF_STMTCTXFILTEREVALENV};

    public final static FilterSpecParam[] EMPTY_PARAM_ARRAY = new FilterSpecParam[0];
    public final static FilterValueSetParam[] EMPTY_VALUE_ARRAY = new FilterValueSetParam[0];

    public abstract Object getFilterValue(MatchedEventMap matchedEvents, ExprEvaluatorContext exprEvaluatorContext, StatementContextFilterEvalEnv filterEvalEnv);

    protected final ExprFilterSpecLookupable lookupable;
    private final FilterOperator filterOperator;

    public FilterSpecParam(ExprFilterSpecLookupable lookupable, FilterOperator filterOperator) {
        this.lookupable = lookupable;
        this.filterOperator = filterOperator;
    }

    public ExprFilterSpecLookupable getLookupable() {
        return lookupable;
    }

    public FilterOperator getFilterOperator() {
        return filterOperator;
    }

    public String toString() {
        return "FilterSpecParam" +
                " lookupable=" + lookupable +
                " filterOp=" + filterOperator;
    }

    public static FilterSpecParam[] toArray(Collection<FilterSpecParam> coll) {
        if (coll.isEmpty()) {
            return EMPTY_PARAM_ARRAY;
        }
        return coll.toArray(new FilterSpecParam[coll.size()]);
    }
}
