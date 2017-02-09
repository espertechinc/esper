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
package com.espertech.esper.epl.agg;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.epl.agg.service.AggSvcGroupAllNoAccessImpl;
import com.espertech.esper.epl.agg.service.AggregationMethodFactory;
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.supportunit.epl.SupportAggregator;
import com.espertech.esper.supportunit.epl.SupportAggregatorFactory;
import com.espertech.esper.supportunit.epl.SupportExprNode;
import junit.framework.TestCase;

public class TestAggregationServiceGroupAllImpl extends TestCase {
    private AggSvcGroupAllNoAccessImpl service;

    public void setUp() {
        SupportAggregator aggregators[] = new SupportAggregator[2];
        for (int i = 0; i < aggregators.length; i++) {
            aggregators[i] = new SupportAggregator();
        }

        ExprEvaluator[] evaluators = new ExprEvaluator[]{new SupportExprNode(5).getExprEvaluator(), new SupportExprNode(2).getExprEvaluator()};

        service = new AggSvcGroupAllNoAccessImpl(evaluators, aggregators, new AggregationMethodFactory[]{
                new SupportAggregatorFactory(), new SupportAggregatorFactory()
        });
    }

    public void testApplyEnter() {
        // apply two rows, all aggregators evaluated their sub-expressions(constants 5 and 2) twice
        service.applyEnter(new EventBean[1], null, null);
        service.applyEnter(new EventBean[1], null, null);
        assertEquals(10, service.getValue(0, -1, null, true, null));
        assertEquals(4, service.getValue(1, -1, null, true, null));
    }

    public void testApplyLeave() {
        // apply 3 rows, all aggregators evaluated their sub-expressions(constants 5 and 2)
        service.applyLeave(new EventBean[1], null, null);
        service.applyLeave(new EventBean[1], null, null);
        service.applyLeave(new EventBean[1], null, null);
        assertEquals(-15, service.getValue(0, -1, null, true, null));
        assertEquals(-6, service.getValue(1, -1, null, true, null));
    }

    private static EventBean[][] makeEvents(int countRows) {
        EventBean[][] result = new EventBean[countRows][0];
        for (int i = 0; i < countRows; i++) {
            result[i] = new EventBean[0];
        }
        return result;
    }
}
