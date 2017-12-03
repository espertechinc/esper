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

import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.core.service.EPStatementSPI;
import com.espertech.esper.filter.*;
import com.espertech.esper.filterspec.FilterOperator;
import com.espertech.esper.filterspec.FilterValueSet;
import com.espertech.esper.filterspec.FilterValueSetParam;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class SupportFilterHelper {
    public static void assertFilterTwo(EPServiceProvider epService, String epl, String expressionOne, FilterOperator opOne, String expressionTwo, FilterOperator opTwo) {
        EPStatementSPI statementSPI = (EPStatementSPI) epService.getEPAdministrator().createEPL(epl);
        if (((FilterServiceSPI) statementSPI.getStatementContext().getFilterService()).isSupportsTakeApply()) {
            FilterValueSetParam[] multi = getFilterMulti(statementSPI);
            assertEquals(2, multi.length);
            assertEquals(opOne, multi[0].getFilterOperator());
            assertEquals(expressionOne, multi[0].getLookupable().getExpression());
            assertEquals(opTwo, multi[1].getFilterOperator());
            assertEquals(expressionTwo, multi[1].getLookupable().getExpression());
        }
    }

    public static FilterValueSetParam getFilterSingle(EPStatementSPI statementSPI) {
        FilterValueSetParam[] params = getFilterMulti(statementSPI);
        assertEquals(1, params.length);
        return params[0];
    }

    public static FilterValueSetParam[] getFilterMulti(EPStatementSPI statementSPI) {
        FilterServiceSPI filterServiceSPI = (FilterServiceSPI) statementSPI.getStatementContext().getFilterService();
        FilterSet set = filterServiceSPI.take(Collections.singleton(statementSPI.getStatementId()));
        assertEquals(1, set.getFilters().size());
        FilterValueSet valueSet = set.getFilters().get(0).getFilterValueSet();
        return valueSet.getParameters()[0];
    }

    public static EPStatement assertFilterMulti(EPServiceProvider epService, String epl, String eventTypeName, SupportFilterItem[][] expected) {
        EPStatementSPI statementSPI = (EPStatementSPI) epService.getEPAdministrator().createEPL(epl);
        if (!((FilterServiceSPI) statementSPI.getStatementContext().getFilterService()).isSupportsTakeApply()) {
            return statementSPI;
        }
        assertFilterMulti(statementSPI, eventTypeName, expected);
        return statementSPI;
    }

    public static void assertFilterMulti(EPStatementSPI statementSPI, String eventTypeName, SupportFilterItem[][] expected) {
        FilterServiceSPI filterServiceSPI = (FilterServiceSPI) statementSPI.getStatementContext().getFilterService();
        FilterSet set = filterServiceSPI.take(Collections.singleton(statementSPI.getStatementId()));

        FilterSetEntry filterSetEntry = null;
        for (FilterSetEntry entry : set.getFilters()) {
            if (entry.getFilterValueSet().getEventType().getName().equals(eventTypeName)) {
                if (filterSetEntry != null) {
                    fail("Multiple filters for type " + eventTypeName);
                }
                filterSetEntry = entry;
            }
        }

        FilterValueSet valueSet = filterSetEntry.getFilterValueSet();
        FilterValueSetParam[][] params = valueSet.getParameters();

        Comparator<SupportFilterItem> comparator = new Comparator<SupportFilterItem>() {
            public int compare(SupportFilterItem o1, SupportFilterItem o2) {
                if (o1.getName().equals(o2.getName())) {
                    if (o1.getOp().ordinal() > o1.getOp().ordinal()) {
                        return 1;
                    }
                    if (o1.getOp().ordinal() < o1.getOp().ordinal()) {
                        return -1;
                    }
                    return 0;
                }
                return o1.getName().compareTo(o2.getName());
            }
        };

        SupportFilterItem[][] found = new SupportFilterItem[params.length][];
        for (int i = 0; i < found.length; i++) {
            found[i] = new SupportFilterItem[params[i].length];
            for (int j = 0; j < params[i].length; j++) {
                found[i][j] = new SupportFilterItem(params[i][j].getLookupable().getExpression().toString(),
                        params[i][j].getFilterOperator());
            }
            Arrays.sort(found[i], comparator);
        }

        for (int i = 0; i < expected.length; i++) {
            Arrays.sort(expected[i], comparator);
        }

        EPAssertionUtil.assertEqualsAnyOrder(expected, found);
        filterServiceSPI.apply(set);
    }

}
