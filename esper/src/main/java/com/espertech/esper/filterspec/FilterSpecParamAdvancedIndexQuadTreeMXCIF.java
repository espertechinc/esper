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
package com.espertech.esper.filterspec;

import com.espertech.esper.epl.core.engineimport.EngineImportService;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.epl.expression.core.ExprFilterSpecLookupable;
import com.espertech.esper.type.XYWHRectangle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Annotation;

public final class FilterSpecParamAdvancedIndexQuadTreeMXCIF extends FilterSpecParam {
    private static final Logger log = LoggerFactory.getLogger(FilterSpecParamAdvancedIndexQuadTreeMXCIF.class);
    private static final long serialVersionUID = 4840823619612773366L;

    private FilterSpecParamFilterForEvalDouble xEval;
    private FilterSpecParamFilterForEvalDouble yEval;
    private FilterSpecParamFilterForEvalDouble widthEval;
    private FilterSpecParamFilterForEvalDouble heightEval;

    public FilterSpecParamAdvancedIndexQuadTreeMXCIF(ExprFilterSpecLookupable lookupable, FilterOperator filterOperator, FilterSpecParamFilterForEvalDouble xEval, FilterSpecParamFilterForEvalDouble yEval, FilterSpecParamFilterForEvalDouble widthEval, FilterSpecParamFilterForEvalDouble heightEval) {
        super(lookupable, filterOperator);
        this.xEval = xEval;
        this.yEval = yEval;
        this.widthEval = widthEval;
        this.heightEval = heightEval;
    }

    public Object getFilterValue(MatchedEventMap matchedEvents, ExprEvaluatorContext exprEvaluatorContext, EngineImportService engineImportService, Annotation[] annotations) {
        Double x = xEval.getFilterValueDouble(matchedEvents, exprEvaluatorContext);
        Double y = yEval.getFilterValueDouble(matchedEvents, exprEvaluatorContext);
        Double width = widthEval.getFilterValueDouble(matchedEvents, exprEvaluatorContext);
        Double height = heightEval.getFilterValueDouble(matchedEvents, exprEvaluatorContext);
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
        return  this.xEval.equals(other.xEval) &&
                this.yEval.equals(other.yEval) &&
                this.widthEval.equals(other.widthEval) &&
                this.heightEval.equals(other.heightEval);
    }

    public int hashCode() {
        return super.hashCode();
    }
}
