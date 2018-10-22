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
import com.espertech.esper.common.internal.type.XYWHRectangle;

public final class FilterSpecParamAdvancedIndexQuadTreeMXCIF extends FilterSpecParam {
    private FilterSpecParamFilterForEvalDouble xEval;
    private FilterSpecParamFilterForEvalDouble yEval;
    private FilterSpecParamFilterForEvalDouble widthEval;
    private FilterSpecParamFilterForEvalDouble heightEval;

    public FilterSpecParamAdvancedIndexQuadTreeMXCIF(ExprFilterSpecLookupable lookupable, FilterOperator filterOperator) {
        super(lookupable, filterOperator);
    }

    public Object getFilterValue(MatchedEventMap matchedEvents, ExprEvaluatorContext exprEvaluatorContext, StatementContextFilterEvalEnv filterEvalEnv) {
        Double x = xEval.getFilterValueDouble(matchedEvents, exprEvaluatorContext, filterEvalEnv);
        Double y = yEval.getFilterValueDouble(matchedEvents, exprEvaluatorContext, filterEvalEnv);
        Double width = widthEval.getFilterValueDouble(matchedEvents, exprEvaluatorContext, filterEvalEnv);
        Double height = heightEval.getFilterValueDouble(matchedEvents, exprEvaluatorContext, filterEvalEnv);
        return new XYWHRectangle(x, y, width, height);
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (!(obj instanceof FilterSpecParamAdvancedIndexQuadTreeMXCIF)) {
            return false;
        }

        FilterSpecParamAdvancedIndexQuadTreeMXCIF other = (FilterSpecParamAdvancedIndexQuadTreeMXCIF) obj;
        if (!super.equals(other)) {
            return false;
        }
        return this.xEval.equals(other.xEval) &&
                this.yEval.equals(other.yEval) &&
                this.widthEval.equals(other.widthEval) &&
                this.heightEval.equals(other.heightEval);
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

    public void setWidthEval(FilterSpecParamFilterForEvalDouble widthEval) {
        this.widthEval = widthEval;
    }

    public void setHeightEval(FilterSpecParamFilterForEvalDouble heightEval) {
        this.heightEval = heightEval;
    }
}
