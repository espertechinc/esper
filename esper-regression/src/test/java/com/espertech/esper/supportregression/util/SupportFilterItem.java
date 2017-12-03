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
package com.espertech.esper.supportregression.util;

import com.espertech.esper.filterspec.FilterOperator;
import com.espertech.esper.filter.FilterSpecCompiler;

public class SupportFilterItem {
    private final String name;
    private final FilterOperator op;

    public SupportFilterItem(String name, FilterOperator op) {
        this.name = name;
        this.op = op;
    }

    public String getName() {
        return name;
    }

    public FilterOperator getOp() {
        return op;
    }

    public String toString() {
        return "FilterItem{" +
                "name='" + name + '\'' +
                ", op=" + op +
                '}';
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SupportFilterItem that = (SupportFilterItem) o;

        if (op != that.op) return false;
        if (name != null ? !name.equals(that.name) : that.name != null) return false;

        return true;
    }

    public int hashCode() {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + (op != null ? op.hashCode() : 0);
        return result;
    }

    public static SupportFilterItem getBoolExprFilterItem() {
        return new SupportFilterItem(FilterSpecCompiler.PROPERTY_NAME_BOOLEAN_EXPRESSION, FilterOperator.BOOLEAN_EXPRESSION);
    }
}
