/*
 * *************************************************************************************
 *  Copyright (C) 2006-2015 EsperTech, Inc. All rights reserved.                       *
 *  http://www.espertech.com/esper                                                     *
 *  http://www.espertech.com                                                           *
 *  ---------------------------------------------------------------------------------- *
 *  The software in this package is published under the terms of the GPL license       *
 *  a copy of which has been included with this distribution in the license.txt file.  *
 * *************************************************************************************
 */

package com.espertech.esper.epl.agg;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.collection.MultiKeyUntyped;
import com.espertech.esper.epl.agg.service.AggSvcGroupByNoAccessImpl;
import com.espertech.esper.epl.core.MethodResolutionService;
import com.espertech.esper.epl.core.MethodResolutionServiceImpl;
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.support.epl.SupportAggregatorFactory;
import com.espertech.esper.support.epl.SupportExprNode;
import com.espertech.esper.core.support.SupportStatementContextFactory;
import junit.framework.TestCase;

public class TestAggregationServiceGroupByImpl extends TestCase
{
    private AggSvcGroupByNoAccessImpl service;
    private MultiKeyUntyped groupOneKey;
    private MultiKeyUntyped groupTwoKey;
    private MethodResolutionService methodResolutionService;

    public void setUp()
    {
        SupportAggregatorFactory aggregators[] = new SupportAggregatorFactory[2];
        for (int i = 0; i < aggregators.length; i++)
        {
            aggregators[i] = new SupportAggregatorFactory();
        }
        ExprEvaluator evaluators[] = new ExprEvaluator[] { new SupportExprNode(5).getExprEvaluator(), new SupportExprNode(2).getExprEvaluator() };
        methodResolutionService = new MethodResolutionServiceImpl(null, null);

        service = new AggSvcGroupByNoAccessImpl(evaluators, aggregators, null, methodResolutionService);

        groupOneKey = new MultiKeyUntyped(new Object[] {"x", "y1"});
        groupTwoKey = new MultiKeyUntyped(new Object[] {"x", "y2"});
    }

    public void testGetValue()
    {
        ExprEvaluatorContext exprEvaluatorContext = SupportStatementContextFactory.makeEvaluatorContext();
        // apply 3 rows to group key 1, all aggregators evaluated their sub-expressions(constants 5 and 2)
        service.applyEnter(new EventBean[1], groupOneKey, exprEvaluatorContext);
        service.applyEnter(new EventBean[1], groupOneKey, exprEvaluatorContext);
        service.applyEnter(new EventBean[1], groupTwoKey, exprEvaluatorContext);

        service.setCurrentAccess(groupOneKey, -1, null);
        assertEquals(10, service.getValue(0, -1, null, true, null));
        assertEquals(4, service.getValue(1, -1, null, true, null));
        service.setCurrentAccess(groupTwoKey, -1, null);
        assertEquals(5, service.getValue(0, -1, null, true, null));
        assertEquals(2, service.getValue(1, -1, null, true, null));

        service.applyLeave(new EventBean[1], groupTwoKey, exprEvaluatorContext);
        service.applyLeave(new EventBean[1], groupTwoKey, exprEvaluatorContext);
        service.applyLeave(new EventBean[1], groupTwoKey, exprEvaluatorContext);
        service.applyLeave(new EventBean[1], groupOneKey, exprEvaluatorContext);

        service.setCurrentAccess(groupOneKey, -1, null);
        assertEquals(10 - 5, service.getValue(0, -1, null, true, null));
        assertEquals(4 - 2, service.getValue(1, -1, null, true, null));
        service.setCurrentAccess(groupTwoKey, -1, null);
        assertEquals(5 - 15, service.getValue(0, -1, null, true, null));
        assertEquals(2 - 6, service.getValue(1, -1, null, true, null));
    }
}
