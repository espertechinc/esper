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
package com.espertech.esper.regressionlib.support.filter;

import java.util.List;

public class FilterTestCaseSingleField {
    private final String filterExpr;
    private final String fieldName;
    private final Object[] values;
    private final boolean[] isInvoked;

    public FilterTestCaseSingleField(String filterExpr, String fieldName, Object[] values, boolean[] isInvoked) {
        this.filterExpr = filterExpr;
        this.fieldName = fieldName;
        this.values = values;
        this.isInvoked = isInvoked;
    }

    public String getFilterExpr() {
        return filterExpr;
    }

    public String getFieldName() {
        return fieldName;
    }

    public Object[] getValues() {
        return values;
    }

    public boolean[] getIsInvoked() {
        return isInvoked;
    }

    public static void addCase(List<FilterTestCaseSingleField> testCases, String filterExpr, String fieldName, Object[] values, boolean[] isInvoked) {
        testCases.add(new FilterTestCaseSingleField(filterExpr, fieldName, values, isInvoked));
    }
}
