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

import com.espertech.esper.client.EventPropertyGetter;
import com.espertech.esper.epl.core.engineimport.EngineImportService;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.epl.expression.core.ExprFilterSpecLookupable;
import com.espertech.esper.util.SimpleNumberCoercer;

import java.lang.annotation.Annotation;

/**
 * This class represents a filter parameter containing a reference to a context property.
 */
public final class FilterSpecParamContextProp extends FilterSpecParam {
    private static final long serialVersionUID = -1651262234386299344L;
    private final String contextPropertyName;
    private transient final EventPropertyGetter getter;
    private transient final SimpleNumberCoercer numberCoercer;

    public FilterSpecParamContextProp(ExprFilterSpecLookupable lookupable, FilterOperator filterOperator, String contextPropertyName, EventPropertyGetter getter, SimpleNumberCoercer numberCoercer) {
        super(lookupable, filterOperator);
        this.contextPropertyName = contextPropertyName;
        this.getter = getter;
        this.numberCoercer = numberCoercer;
    }

    public Object getFilterValue(MatchedEventMap matchedEvents, ExprEvaluatorContext exprEvaluatorContext, EngineImportService engineImportService, Annotation[] annotations) {
        if (exprEvaluatorContext.getContextProperties() == null) {
            return null;
        }
        Object result = getter.get(exprEvaluatorContext.getContextProperties());

        if (numberCoercer == null) {
            return result;
        }
        return numberCoercer.coerceBoxed((Number) result);
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        FilterSpecParamContextProp that = (FilterSpecParamContextProp) o;

        if (!contextPropertyName.equals(that.contextPropertyName)) return false;

        return true;
    }

    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + contextPropertyName.hashCode();
        return result;
    }
}
