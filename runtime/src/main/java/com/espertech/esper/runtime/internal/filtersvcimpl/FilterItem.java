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

import com.espertech.esper.common.internal.filterspec.FilterOperator;

import static com.espertech.esper.common.internal.compile.stage2.FilterSpecCompilerIndexPlanner.PROPERTY_NAME_BOOLEAN_EXPRESSION;

public class FilterItem {
    private final String name;
    private final FilterOperator op;
    private final Object optionalValue;
    private final Object index;

    public FilterItem(String name, FilterOperator op, Object optionalValue, Object index) {
        this.name = name;
        this.op = op;
        this.optionalValue = optionalValue;
        this.index = index;
    }

    public FilterItem(String name, FilterOperator op) {
        this(name, op, null, null);
    }

    public FilterItem(String name, FilterOperator op, Object index) {
        this(name, op, null, index);
    }

    public String getName() {
        return name;
    }

    public FilterOperator getOp() {
        return op;
    }

    public Object getOptionalValue() {
        return optionalValue;
    }

    public Object getIndex() {
        return index;
    }

    public String toString() {
        return "FilterItem{" +
            "name='" + name + '\'' +
            ", op=" + op +
            ", optionalValue=" + optionalValue +
            ", index=" + index +
            '}';
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        FilterItem that = (FilterItem) o;

        if (op != that.op) return false;
        if (name != null ? !name.equals(that.name) : that.name != null) return false;

        return true;
    }

    public int hashCode() {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + (op != null ? op.hashCode() : 0);
        return result;
    }

    public static FilterItem getBoolExprFilterItem() {
        return new FilterItem(PROPERTY_NAME_BOOLEAN_EXPRESSION, FilterOperator.BOOLEAN_EXPRESSION, null);
    }
}
