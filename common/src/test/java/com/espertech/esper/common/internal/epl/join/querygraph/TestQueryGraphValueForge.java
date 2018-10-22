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
package com.espertech.esper.common.internal.epl.join.querygraph;

import com.espertech.esper.common.internal.epl.expression.core.ExprIdentNode;
import com.espertech.esper.common.internal.epl.expression.core.ExprIdentNodeImpl;
import com.espertech.esper.common.internal.epl.expression.core.ExprNode;
import com.espertech.esper.common.internal.epl.expression.core.ExprNodeUtilityPrint;
import com.espertech.esper.common.internal.supportunit.bean.SupportABCDEEvent;
import com.espertech.esper.common.internal.supportunit.event.SupportEventTypeFactory;
import junit.framework.TestCase;

public class TestQueryGraphValueForge extends TestCase {

    public void testRangeRelOp() {

        tryAdd("b", QueryGraphRangeEnum.GREATER_OR_EQUAL, makeIdent("a"),      // read a >= b
                "c", QueryGraphRangeEnum.LESS_OR_EQUAL, makeIdent("a"),         // read a <= c
                new Object[][]{{null, "b", "c", QueryGraphRangeEnum.RANGE_CLOSED, "a"}});

        tryAdd("b", QueryGraphRangeEnum.GREATER, makeIdent("a"),      // read a > b
                "c", QueryGraphRangeEnum.LESS, makeIdent("a"),         // read a < c
                new Object[][]{{null, "b", "c", QueryGraphRangeEnum.RANGE_OPEN, "a"}});

        tryAdd("b", QueryGraphRangeEnum.GREATER_OR_EQUAL, makeIdent("a"),      // read a >= b
                "c", QueryGraphRangeEnum.LESS, makeIdent("a"),                  // read a < c
                new Object[][]{{null, "b", "c", QueryGraphRangeEnum.RANGE_HALF_OPEN, "a"}});

        tryAdd("b", QueryGraphRangeEnum.GREATER, makeIdent("a"),                       // read a > b
                "c", QueryGraphRangeEnum.LESS_OR_EQUAL, makeIdent("a"),                  // read a <= c
                new Object[][]{{null, "b", "c", QueryGraphRangeEnum.RANGE_HALF_CLOSED, "a"}});

        // sanity
        tryAdd("b", QueryGraphRangeEnum.LESS_OR_EQUAL, makeIdent("a"),                     // read a <= b
                "c", QueryGraphRangeEnum.GREATER_OR_EQUAL, makeIdent("a"),                  // read a >= c
                new Object[][]{{null, "c", "b", QueryGraphRangeEnum.RANGE_CLOSED, "a"}});
    }

    private ExprIdentNode makeIdent(String prop) {
        return new ExprIdentNodeImpl(SupportEventTypeFactory.createBeanType(SupportABCDEEvent.class), prop, 0);
    }

    private void tryAdd(String propertyKeyOne, QueryGraphRangeEnum opOne, ExprIdentNode valueOne,
                        String propertyKeyTwo, QueryGraphRangeEnum opTwo, ExprIdentNode valueTwo,
                        Object[][] expected) {

        QueryGraphValueForge value = new QueryGraphValueForge();
        value.addRelOp(makeIdent(propertyKeyOne), opOne, valueOne, true);
        value.addRelOp(makeIdent(propertyKeyTwo), opTwo, valueTwo, true);
        assertRanges(expected, value);

        value = new QueryGraphValueForge();
        value.addRelOp(makeIdent(propertyKeyTwo), opTwo, valueTwo, true);
        value.addRelOp(makeIdent(propertyKeyOne), opOne, valueOne, true);
        assertRanges(expected, value);
    }

    public void testNoDup() {

        QueryGraphValueForge value = new QueryGraphValueForge();
        value.addRelOp(makeIdent("b"), QueryGraphRangeEnum.LESS_OR_EQUAL, makeIdent("a"), false);
        value.addRelOp(makeIdent("b"), QueryGraphRangeEnum.LESS_OR_EQUAL, makeIdent("a"), false);
        assertRanges(new Object[][]{{"b", null, null, QueryGraphRangeEnum.LESS_OR_EQUAL, "a"}}, value);

        value = new QueryGraphValueForge();
        value.addRange(QueryGraphRangeEnum.RANGE_CLOSED, makeIdent("b"), makeIdent("c"), makeIdent("a"));
        value.addRange(QueryGraphRangeEnum.RANGE_CLOSED, makeIdent("b"), makeIdent("c"), makeIdent("a"));
        assertRanges(new Object[][]{{null, "b", "c", QueryGraphRangeEnum.RANGE_CLOSED, "a"}}, value);
    }

    private void assertRanges(Object[][] ranges, QueryGraphValueForge value) {
        assertEquals(ranges.length, value.getItems().size());

        int count = -1;
        for (QueryGraphValueDescForge desc : value.getItems()) {
            count++;
            QueryGraphValueEntryRangeForge r = (QueryGraphValueEntryRangeForge) desc.getEntry();

            assertEquals(ranges[count][3], r.getType());
            assertEquals(ranges[count][4], ExprNodeUtilityPrint.toExpressionStringMinPrecedenceSafe(desc.getIndexExprs()[0]));

            if (r instanceof QueryGraphValueEntryRangeRelOpForge) {
                QueryGraphValueEntryRangeRelOpForge relOp = (QueryGraphValueEntryRangeRelOpForge) r;
                assertEquals(ranges[count][0], getProp(relOp.getExpression()));
            } else {
                QueryGraphValueEntryRangeInForge rangeIn = (QueryGraphValueEntryRangeInForge) r;
                assertEquals(ranges[count][1], getProp(rangeIn.getExprStart()));
                assertEquals(ranges[count][2], getProp(rangeIn.getExprEnd()));
            }
        }
    }

    private String getProp(ExprNode node) {
        return ((ExprIdentNode) node).getUnresolvedPropertyName();
    }

}
