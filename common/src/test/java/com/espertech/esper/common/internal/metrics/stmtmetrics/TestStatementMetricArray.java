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
package com.espertech.esper.common.internal.metrics.stmtmetrics;

import com.espertech.esper.common.client.metric.StatementMetric;
import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.common.internal.util.DeploymentIdNamePair;
import junit.framework.TestCase;

public class TestStatementMetricArray extends TestCase {
    public void testFlowReportActive() {
        StatementMetricArray rep = new StatementMetricArray("uri", "name", 3, false);

        DeploymentIdNamePair d001 = new DeploymentIdNamePair("A", "001");
        DeploymentIdNamePair d002 = new DeploymentIdNamePair("A", "002");
        DeploymentIdNamePair d003 = new DeploymentIdNamePair("A", "003");
        DeploymentIdNamePair d004 = new DeploymentIdNamePair("A", "004");
        DeploymentIdNamePair d005 = new DeploymentIdNamePair("A", "005");
        DeploymentIdNamePair d006 = new DeploymentIdNamePair("A", "006");
        DeploymentIdNamePair d007 = new DeploymentIdNamePair("A", "007");
        DeploymentIdNamePair d008 = new DeploymentIdNamePair("A", "008");
        DeploymentIdNamePair d009 = new DeploymentIdNamePair("A", "009");

        assertEquals(0, rep.sizeLastElement());

        assertEquals(0, rep.addStatementGetIndex(d001));
        assertEquals(1, rep.sizeLastElement());

        assertEquals(1, rep.addStatementGetIndex(d002));
        assertEquals(2, rep.addStatementGetIndex(d003));
        assertEquals(3, rep.sizeLastElement());

        rep.removeStatement(d002);

        assertEquals(3, rep.addStatementGetIndex(d004));
        assertEquals(4, rep.addStatementGetIndex(d005));

        rep.removeStatement(d005);
        assertEquals(5, rep.addStatementGetIndex(d006));

        StatementMetric metrics[] = new StatementMetric[6];
        for (int i = 0; i < 6; i++) {
            metrics[i] = rep.getAddMetric(i);
        }

        StatementMetric flushed[] = rep.flushMetrics();
        EPAssertionUtil.assertSameExactOrder(metrics, flushed);

        assertEquals(1, rep.addStatementGetIndex(d007));
        assertEquals(4, rep.addStatementGetIndex(d008));

        rep.removeStatement(d001);
        rep.removeStatement(d003);
        rep.removeStatement(d004);
        rep.removeStatement(d006);
        rep.removeStatement(d007);
        assertEquals(6, rep.sizeLastElement());
        rep.removeStatement(d008);
        assertEquals(6, rep.sizeLastElement());

        flushed = rep.flushMetrics();
        assertEquals(6, flushed.length);
        assertEquals(0, rep.sizeLastElement());

        flushed = rep.flushMetrics();
        assertNull(flushed);
        assertEquals(0, rep.sizeLastElement());

        assertEquals(0, rep.addStatementGetIndex(d009));
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

        assertEquals(0, rep.addStatementGetIndex(new DeploymentIdNamePair("A", "001")));
        assertEquals(1, rep.addStatementGetIndex(new DeploymentIdNamePair("A", "002")));
        assertEquals(2, rep.addStatementGetIndex(new DeploymentIdNamePair("A", "003")));
        rep.removeStatement(new DeploymentIdNamePair("A", "002"));

        StatementMetric[] flushed = rep.flushMetrics();
        for (int i = 0; i < 3; i++) {
            assertNotNull(flushed[i]);
        }
    }
}
