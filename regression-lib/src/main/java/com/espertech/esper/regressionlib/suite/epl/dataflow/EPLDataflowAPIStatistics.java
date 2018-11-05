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
package com.espertech.esper.regressionlib.suite.epl.dataflow;

import com.espertech.esper.common.client.dataflow.core.EPDataFlowInstance;
import com.espertech.esper.common.client.dataflow.core.EPDataFlowInstanceOperatorStat;
import com.espertech.esper.common.client.dataflow.core.EPDataFlowInstantiationOptions;
import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.common.client.util.StatementProperty;
import com.espertech.esper.common.client.util.StatementType;
import com.espertech.esper.common.internal.epl.dataflow.util.DefaultSupportCaptureOp;
import com.espertech.esper.common.internal.epl.dataflow.util.DefaultSupportGraphOpProvider;
import com.espertech.esper.common.internal.epl.dataflow.util.DefaultSupportSourceOp;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.common.internal.support.SupportBean;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class EPLDataflowAPIStatistics implements RegressionExecution {

    public void run(RegressionEnvironment env) {
        env.compileDeploy("@name('flow') create dataflow MyGraph " +
            "DefaultSupportSourceOp -> outstream<SupportBean> {} " +
            "DefaultSupportCaptureOp(outstream) {}");
        assertEquals(StatementType.CREATE_DATAFLOW, env.statement("flow").getProperty(StatementProperty.STATEMENTTYPE));
        assertEquals("MyGraph", env.statement("flow").getProperty(StatementProperty.CREATEOBJECTNAME));

        DefaultSupportSourceOp source = new DefaultSupportSourceOp(new Object[]{new SupportBean("E1", 1), new SupportBean("E2", 2)});
        DefaultSupportCaptureOp capture = new DefaultSupportCaptureOp();
        EPDataFlowInstantiationOptions options = new EPDataFlowInstantiationOptions()
            .operatorProvider(new DefaultSupportGraphOpProvider(source, capture))
            .operatorStatistics(true)
            .cpuStatistics(true);

        EPDataFlowInstance instance = env.runtime().getDataFlowService().instantiate(env.deploymentId("flow"), "MyGraph", options);

        instance.run();

        List<EPDataFlowInstanceOperatorStat> stats = instance.getStatistics().getOperatorStatistics();
        assertEquals(2, stats.size());

        EPDataFlowInstanceOperatorStat sourceStat = stats.get(0);
        assertEquals("DefaultSupportSourceOp", sourceStat.getOperatorName());
        assertEquals(0, sourceStat.getOperatorNumber());
        assertEquals("DefaultSupportSourceOp#0() -> outstream<SupportBean>", sourceStat.getOperatorPrettyPrint());
        assertEquals(2, sourceStat.getSubmittedOverallCount());
        EPAssertionUtil.assertEqualsExactOrder(new long[]{2L}, sourceStat.getSubmittedPerPortCount());
        assertTrue(sourceStat.getTimeOverall() > 0);
        assertEquals(sourceStat.getTimeOverall(), sourceStat.getTimePerPort()[0]);

        EPDataFlowInstanceOperatorStat destStat = stats.get(1);
        assertEquals("DefaultSupportCaptureOp", destStat.getOperatorName());
        assertEquals(1, destStat.getOperatorNumber());
        assertEquals("DefaultSupportCaptureOp#1(outstream)", destStat.getOperatorPrettyPrint());
        assertEquals(0, destStat.getSubmittedOverallCount());
        assertEquals(0, destStat.getSubmittedPerPortCount().length);
        assertEquals(0, destStat.getTimeOverall());
        assertEquals(0, destStat.getTimePerPort().length);

        env.undeployAll();
    }
}
