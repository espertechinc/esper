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

import com.espertech.esper.common.internal.context.util.StatementContextFilterEvalEnv;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.common.internal.epl.expression.core.ExprFilterSpecLookupable;
import com.espertech.esper.common.internal.type.XYPoint;

public final class FilterSpecParamAdvancedIndexQuadTreePointRegion extends FilterSpecParam {

    private FilterSpecParamFilterForEvalDouble xEval;
    private FilterSpecParamFilterForEvalDouble yEval;

    public FilterSpecParamAdvancedIndexQuadTreePointRegion(ExprFilterSpecLookupable lookupable, FilterOperator filterOperator) {
        super(lookupable, filterOperator);
    }

    public Object getFilterValue(MatchedEventMap matchedEvents, ExprEvaluatorContext exprEvaluatorContext, StatementContextFilterEvalEnv filterEvalEnv) {
        Double x = xEval.getFilterValueDouble(matchedEvents, exprEvaluatorContext, filterEvalEnv);
        Double y = yEval.getFilterValueDouble(matchedEvents, exprEvaluatorContext, filterEvalEnv);
        return new XYPoint(x, y);
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (!(obj instanceof FilterSpecParamAdvancedIndexQuadTreePointRegion)) {
            return false;
        }

        FilterSpecParamAdvancedIndexQuadTreePointRegion other = (FilterSpecParamAdvancedIndexQuadTreePointRegion) obj;
        if (!super.equals(other)) {
            return false;
        }
        return this.xEval.equals(other.xEval) &&
                (this.yEval.equals(other.yEval));
    }

    public int hashCode() {
        return super.hashCode();
    }

    public void setxEval(FilterSpecParamFilterForEvalDouble xEval) {
        this.xEval = xEval;
    }

    public void setyEval(FilterSpecParamFilterForEvalDouble yEval) {
        this.yEval = yEval;
    }
}
