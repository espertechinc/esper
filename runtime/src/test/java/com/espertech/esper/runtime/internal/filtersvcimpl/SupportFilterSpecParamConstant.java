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
package com.espertech.esper.runtime.internal.filtersvcimpl;

import com.espertech.esper.common.internal.context.util.StatementContextFilterEvalEnv;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.common.internal.epl.expression.core.ExprFilterSpecLookupable;
import com.espertech.esper.common.internal.filterspec.FilterOperator;
import com.espertech.esper.common.internal.filterspec.FilterSpecParam;
import com.espertech.esper.common.internal.filterspec.MatchedEventMap;

public class SupportFilterSpecParamConstant extends FilterSpecParam {
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
    public SupportFilterSpecParamConstant(ExprFilterSpecLookupable lookupable, FilterOperator filterOperator, Object filterConstant)
            throws IllegalArgumentException {
        super(lookupable, filterOperator);
        this.filterConstant = filterConstant;

        if (filterOperator.isRangeOperator()) {
            throw new IllegalArgumentException("Illegal filter operator " + filterOperator + " supplied to " +
                    "constant filter parameter");
        }
    }

    public Object getFilterValue(MatchedEventMap matchedEvents, ExprEvaluatorContext exprEvaluatorContext, StatementContextFilterEvalEnv filterEvalEnv) {
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

        SupportFilterSpecParamConstant that = (SupportFilterSpecParamConstant) o;

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
