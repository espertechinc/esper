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
package com.espertech.esper.regressionlib.suite.resultset.outputlimit;

import com.espertech.esper.common.client.util.DateTime;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.common.internal.support.SupportBean_S0;
import com.espertech.esper.regressionlib.support.bean.SupportScheduleSimpleEvent;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ResultSetOutputLimitParameterizedByContext implements RegressionExecution {

    public void run(RegressionEnvironment env) {
        env.advanceTime(DateTime.parseDefaultMSec("2002-05-01T09:00:00.000"));
        String epl = "@name('ctx') create context MyCtx start SupportScheduleSimpleEvent as sse;\n" +
            "@name('s0') context MyCtx\n" +
            "select count(*) as c \n" +
            "from SupportBean_S0\n" +
            "output last at(context.sse.atminute, context.sse.athour, *, *, *, *) and when terminated\n";
        env.compileDeploy(epl).addListener("s0");

        env.sendEventBean(new SupportScheduleSimpleEvent(10, 15));
        env.sendEventBean(new SupportBean_S0(0));

        env.advanceTime(DateTime.parseDefaultMSec("2002-05-01T10:14:59.000"));
        assertFalse(env.listener("s0").getAndClearIsInvoked());

        env.advanceTime(DateTime.parseDefaultMSec("2002-05-01T10:15:00.000"));
        assertTrue(env.listener("s0").getAndClearIsInvoked());

        env.undeployAll();
    }

}
