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

import java.lang.annotation.Annotation;

/**
 * This class represents a single, constant value filter parameter in an {@link FilterSpecCompiled} filter specification.
 */
public final class FilterSpecParamConstant extends FilterSpecParam {
    private final Object filterConstant;
    private static final long serialVersionUID = 5732440503234468449L;

    /**
     * Constructor.
     *
     * @param lookupable     is the lookupable
     * @param filterOperator is the type of compare
     * @param filterConstant contains the value to match against the event's property value
     * @throws IllegalArgumentException if an operator was supplied that does not take a single constant value
     */
    public FilterSpecParamConstant(ExprFilterSpecLookupable lookupable, FilterOperator filterOperator, Object filterConstant)
            throws IllegalArgumentException {
        super(lookupable, filterOperator);
        this.filterConstant = filterConstant;

        if (filterOperator.isRangeOperator()) {
            throw new IllegalArgumentException("Illegal filter operator " + filterOperator + " supplied to " +
                    "constant filter parameter");
        }
    }

    public Object getFilterValue(MatchedEventMap matchedEvents, ExprEvaluatorContext exprEvaluatorContext, EngineImportService engineImportService, Annotation[] annotations) {
        return filterConstant;
    }

    /**
     * Returns the constant value.
     *
     * @return constant value
     */
    public Object getFilterConstant() {
        return filterConstant;
    }

    public final String toString() {
        return super.toString() + " filterConstant=" + filterConstant;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        FilterSpecParamConstant that = (FilterSpecParamConstant) o;

        if (filterConstant != null ? !filterConstant.equals(that.filterConstant) : that.filterConstant != null)
            return false;

        return true;
    }

    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (filterConstant != null ? filterConstant.hashCode() : 0);
        return result;
    }
}
