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
package com.espertech.esper.regression.dataflow;

import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPServiceProviderManager;
import com.espertech.esper.client.dataflow.EPDataFlowInstance;
import com.espertech.esper.client.dataflow.EPDataFlowInstanceOperatorStat;
import com.espertech.esper.client.dataflow.EPDataFlowInstantiationOptions;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.dataflow.util.DefaultSupportCaptureOp;
import com.espertech.esper.dataflow.util.DefaultSupportGraphOpProvider;
import com.espertech.esper.dataflow.util.DefaultSupportSourceOp;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.client.SupportConfigFactory;
import junit.framework.TestCase;

import java.util.List;

public class TestAPIStatistics extends TestCase {

    private EPServiceProvider epService;

    public void setUp() {
        epService = EPServiceProviderManager.getDefaultProvider(SupportConfigFactory.getConfiguration());
        epService.initialize();
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.startTest(epService, this.getClass(), getName());}
        epService.getEPAdministrator().getConfiguration().addImport(DefaultSupportCaptureOp.class.getName());
        epService.getEPAdministrator().getConfiguration().addImport(DefaultSupportSourceOp.class.getName());
    }

    public void tearDown() {
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.endTest();}
    }

    public void testOperatorStatistics() throws Exception {

        epService.getEPAdministrator().getConfiguration().addEventType(SupportBean.class);

        epService.getEPAdministrator().createEPL("create dataflow MyGraph " +
                "DefaultSupportSourceOp -> outstream<SupportBean> {} " +
                "DefaultSupportCaptureOp(outstream) {}");

        DefaultSupportSourceOp source = new DefaultSupportSourceOp(new Object[] {new SupportBean("E1", 1), new SupportBean("E2", 2) });
        DefaultSupportCaptureOp capture = new DefaultSupportCaptureOp();
        EPDataFlowInstantiationOptions options = new EPDataFlowInstantiationOptions()
                .operatorProvider(new DefaultSupportGraphOpProvider(source, capture))
                .operatorStatistics(true)
                .cpuStatistics(true);

        EPDataFlowInstance instance = epService.getEPRuntime().getDataFlowRuntime().instantiate("MyGraph", options);

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
    }
}
