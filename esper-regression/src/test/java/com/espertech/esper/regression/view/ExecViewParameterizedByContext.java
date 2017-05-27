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
package com.espertech.esper.regression.view;

import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.deploy.DeploymentResult;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.execution.RegressionExecution;

import java.util.Arrays;

public class ExecViewParameterizedByContext implements RegressionExecution {
    public void run(EPServiceProvider epService) throws Exception {
        for (Class clazz : Arrays.asList(MyInitEventWLength.class, SupportBean.class)) {
            epService.getEPAdministrator().getConfiguration().addEventType(clazz);
        }

        runAssertionLengthWindow(epService);
        runAssertionDocSample(epService);

        epService.getEPAdministrator().createEPL("create context CtxInitToTerm initiated by MyInitEventWLength as miewl terminated after 1 year");
        runAssertionWindow(epService, "length_batch(context.miewl.intSize)");
        runAssertionWindow(epService, "time(context.miewl.intSize)");
        runAssertionWindow(epService, "ext_timed(longPrimitive, context.miewl.intSize)");
        runAssertionWindow(epService, "time_batch(context.miewl.intSize)");
        runAssertionWindow(epService, "ext_timed_batch(longPrimitive, context.miewl.intSize)");
        runAssertionWindow(epService, "time_length_batch(context.miewl.intSize, context.miewl.intSize)");
        runAssertionWindow(epService, "time_accum(context.miewl.intSize)");
        runAssertionWindow(epService, "firstlength(context.miewl.intSize)");
        runAssertionWindow(epService, "firsttime(context.miewl.intSize)");
        runAssertionWindow(epService, "sort(context.miewl.intSize, intPrimitive)");
        runAssertionWindow(epService, "rank(theString, context.miewl.intSize, theString)");
        runAssertionWindow(epService, "time_order(longPrimitive, context.miewl.intSize)");
    }

    private void runAssertionDocSample(EPServiceProvider epService) throws Exception {
        String epl = "create schema ParameterEvent(windowSize int);" +
                "create context MyContext initiated by ParameterEvent as params terminated after 1 year;" +
                "context MyContext select * from SupportBean#length(context.params.windowSize);";
        DeploymentResult deployed = epService.getEPAdministrator().getDeploymentAdmin().parseDeploy(epl);
        epService.getEPAdministrator().getDeploymentAdmin().undeployRemove(deployed.getDeploymentId());
    }

    private void runAssertionWindow(EPServiceProvider epService, String window) {
        EPStatement stmt = epService.getEPAdministrator().createEPL("context CtxInitToTerm select * from SupportBean#" + window);
        epService.getEPRuntime().sendEvent(new MyInitEventWLength("P1", 2));
        stmt.destroy();
    }

    private void runAssertionLengthWindow(EPServiceProvider epService) {
        epService.getEPAdministrator().createEPL("create context CtxInitToTerm initiated by MyInitEventWLength as miewl terminated after 1 year");
        EPStatement stmt = epService.getEPAdministrator().createEPL("context CtxInitToTerm select context.miewl.id as id, count(*) as cnt from SupportBean(theString=context.miewl.id)#length(context.miewl.intSize)");

        epService.getEPRuntime().sendEvent(new MyInitEventWLength("P1", 2));
        epService.getEPRuntime().sendEvent(new MyInitEventWLength("P2", 4));
        epService.getEPRuntime().sendEvent(new MyInitEventWLength("P3", 3));
        for (int i = 0; i < 10; i++) {
            epService.getEPRuntime().sendEvent(new SupportBean("P1", 0));
            epService.getEPRuntime().sendEvent(new SupportBean("P2", 0));
            epService.getEPRuntime().sendEvent(new SupportBean("P3", 0));
        }

        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), "id,cnt".split(","), new Object[][]{{"P1", 2L}, {"P2", 4L}, {"P3", 3L}});

        epService.getEPAdministrator().destroyAllStatements();
    }

    public static class MyInitEventWLength {
        private final String id;
        private final int intSize;

        public MyInitEventWLength(String id, int intSize) {
            this.id = id;
            this.intSize = intSize;
        }

        public String getId() {
            return id;
        }

        public int getIntSize() {
            return intSize;
        }
    }
}
