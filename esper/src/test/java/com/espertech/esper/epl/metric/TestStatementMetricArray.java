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
package com.espertech.esper.epl.metric;

import com.espertech.esper.client.metric.StatementMetric;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import junit.framework.TestCase;

public class TestStatementMetricArray extends TestCase {
    public void testFlowReportActive() {
        StatementMetricArray rep = new StatementMetricArray("uri", "name", 3, false);

        assertEquals(0, rep.sizeLastElement());

        assertEquals(0, rep.addStatementGetIndex("001"));
        assertEquals(1, rep.sizeLastElement());

        assertEquals(1, rep.addStatementGetIndex("002"));
        assertEquals(2, rep.addStatementGetIndex("003"));
        assertEquals(3, rep.sizeLastElement());

        rep.removeStatement("002");

        assertEquals(3, rep.addStatementGetIndex("004"));
        assertEquals(4, rep.addStatementGetIndex("005"));

        rep.removeStatement("005");
        assertEquals(5, rep.addStatementGetIndex("006"));

        StatementMetric metrics[] = new StatementMetric[6];
        for (int i = 0; i < 6; i++) {
            metrics[i] = rep.getAddMetric(i);
        }

        StatementMetric flushed[] = rep.flushMetrics();
        EPAssertionUtil.assertSameExactOrder(metrics, flushed);

        assertEquals(1, rep.addStatementGetIndex("007"));
        assertEquals(4, rep.addStatementGetIndex("008"));

        rep.removeStatement("001");
        rep.removeStatement("003");
        rep.removeStatement("004");
        rep.removeStatement("006");
        rep.removeStatement("007");
        assertEquals(6, rep.sizeLastElement());
        rep.removeStatement("008");
        assertEquals(6, rep.sizeLastElement());

        flushed = rep.flushMetrics();
        assertEquals(6, flushed.length);
        assertEquals(0, rep.sizeLastElement());

        flushed = rep.flushMetrics();
        assertNull(flushed);
        assertEquals(0, rep.sizeLastElement());

        assertEquals(0, rep.addStatementGetIndex("009"));
        assertEquals(1, rep.sizeLastElement());

        flushed = rep.flushMetrics();
        assertEquals(6, flushed.length);
        for (int i = 0; i < flushed.length; i++) {
            assertNull(flushed[i]);
        }
        assertEquals(1, rep.sizeLastElement());
    }

    public void testFlowReportInactive() {
        StatementMetricArray rep = new StatementMetricArray("uri", "name", 3, true);

        assertEquals(0, rep.addStatementGetIndex("001"));
        assertEquals(1, rep.addStatementGetIndex("002"));
        assertEquals(2, rep.addStatementGetIndex("003"));
        rep.removeStatement("002");

        StatementMetric[] flushed = rep.flushMetrics();
        for (int i = 0; i < 3; i++) {
            assertNotNull(flushed[i]);
        }
    }
}
