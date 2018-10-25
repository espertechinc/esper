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
package com.espertech.esper.regressionlib.suite.client.instrument;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.metric.StatementMetric;
import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.RegressionPath;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.regressionlib.support.util.ArrayHandlingUtil;

public class ClientInstrumentMetricsReportingNW implements RegressionExecution {

    public void run(RegressionEnvironment env) {
        RegressionPath path = new RegressionPath();
        env.advanceTime(0);
        env.compileDeploy("@Name('0') create schema StatementMetric as " + StatementMetric.class.getName());
        env.compileDeploy("@Name('A') create window MyWindow#lastevent as select * from SupportBean", path);
        env.compileDeploy("@Name('B1') insert into MyWindow select * from SupportBean", path);
        env.compileDeploy("@Name('B2') insert into MyWindow select * from SupportBean", path);
        env.compileDeploy("@Name('C') select sum(intPrimitive) from MyWindow", path);
        env.compileDeploy("@Name('D') select sum(w1.intPrimitive) from MyWindow w1, MyWindow w2", path);

        String appModuleTwo = "@Name('W') create window SupportBeanWindow#keepall as SupportBean;" +
            "" +
            "@Name('M') on SupportBean oe\n" +
            "  merge SupportBeanWindow pw\n" +
            "  where pw.theString = oe.theString\n" +
            "  when not matched \n" +
            "    then insert select *\n" +
            "  when matched and oe.intPrimitive=1\n" +
            "    then delete\n" +
            "  when matched\n" +
            "    then update set pw.intPrimitive = oe.intPrimitive";
        env.compileDeploy(appModuleTwo, path);

        env.compileDeploy("@Name('X') select * from " + StatementMetric.class.getName()).addListener("X");
        String[] fields = "statementName,numInput".split(",");

        env.sendEventBean(new SupportBean("E1", 1));
        env.advanceTime(1000);
        EventBean[] received = ArrayHandlingUtil.reorder("statementName", env.listener("X").getNewDataListFlattened());
        for (EventBean theEvent : received) {
            System.out.println(theEvent.get("statementName") + " = " + theEvent.get("numInput"));
        }
        EPAssertionUtil.assertPropsPerRow(received, fields, new Object[][]{{"A", 2L}, {"B1", 1L}, {"B2", 1L}, {"C", 2L}, {"D", 2L}, {"M", 1L}, {"W", 1L}});

        /* Comment-in for printout.
        for (int i = 0; i < received.length; i++) {
            EventBean event = received[i];
            System.out.println(event.get("statementName") + " " + event.get("wallTime") + " " + event.get("numInput"));
        }
        */

        env.undeployAll();
    }
}
