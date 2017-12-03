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

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.util.Collection;

/**
 * This class represents one filter parameter in an {@link FilterSpecCompiled} filter specification.
 * <p> Each filerting parameter has an attribute name and operator type.
 */
public abstract class FilterSpecParam implements Serializable {
    public final static FilterSpecParam[] EMPTY_PARAM_ARRAY = new FilterSpecParam[0];

    /**
     * The property name of the filter parameter.
     */
    protected final ExprFilterSpecLookupable lookupable;

    private final FilterOperator filterOperator;
    private static final long serialVersionUID = -677137265660114030L;

    FilterSpecParam(ExprFilterSpecLookupable lookupable, FilterOperator filterOperator) {
        this.lookupable = lookupable;
        this.filterOperator = filterOperator;
    }

    /**
     * Return the filter parameter constant to filter for.
     * @param matchedEvents        is the prior results that can be used to determine filter parameters
     * @param exprEvaluatorContext context
     * @param engineImportService
     * @param annotations @return filter parameter constant's value
     */
    public abstract Object getFilterValue(MatchedEventMap matchedEvents, ExprEvaluatorContext exprEvaluatorContext, EngineImportService engineImportService, Annotation[] annotations);

    public ExprFilterSpecLookupable getLookupable() {
        return lookupable;
    }

    /**
     * Returns the filter operator type.
     *
     * @return filter operator type
     */
    public FilterOperator getFilterOperator() {
        return filterOperator;
    }


    public String toString() {
        return "FilterSpecParam" +
                " lookupable=" + lookupable +
                " filterOp=" + filterOperator;
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (!(obj instanceof FilterSpecParam)) {
            return false;
        }

        FilterSpecParam other = (FilterSpecParam) obj;
        if (!(this.lookupable.equals(other.lookupable))) {
            return false;
        }
        if (this.filterOperator != other.filterOperator) {
            return false;
        }
        return true;
    }

    public int hashCode() {
        int result;
        result = lookupable.hashCode();
        result = 31 * result + filterOperator.hashCode();
        return result;
    }

    public static FilterSpecParam[] toArray(Collection<FilterSpecParam> coll) {
        if (coll.isEmpty()) {
            return EMPTY_PARAM_ARRAY;
        }
        return coll.toArray(new FilterSpecParam[coll.size()]);
    }
}
