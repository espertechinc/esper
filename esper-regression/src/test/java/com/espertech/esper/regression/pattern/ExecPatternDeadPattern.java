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
package com.espertech.esper.regression.pattern;

import com.espertech.esper.client.Configuration;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.supportregression.bean.SupportBean_A;
import com.espertech.esper.supportregression.bean.SupportBean_B;
import com.espertech.esper.supportregression.bean.SupportBean_C;
import com.espertech.esper.supportregression.execution.RegressionExecution;

import static org.junit.Assert.assertTrue;

public class ExecPatternDeadPattern implements RegressionExecution {
    public void configure(Configuration configuration) throws Exception {
        configuration.addEventType("A", SupportBean_A.class.getName());
        configuration.addEventType("B", SupportBean_B.class.getName());
        configuration.addEventType("C", SupportBean_C.class.getName());
    }

    public void run(EPServiceProvider epService) throws Exception {
        String pattern = "(A() -> B()) and not C()";
        // Adjust to 20000 to better test the limit
        for (int i = 0; i < 1000; i++) {
            epService.getEPAdministrator().createPattern(pattern);
        }

        epService.getEPRuntime().sendEvent(new SupportBean_C("C1"));

        long startTime = System.currentTimeMillis();
        epService.getEPRuntime().sendEvent(new SupportBean_A("A1"));
        long delta = System.currentTimeMillis() - startTime;
        assertTrue("performance: delta=" + delta, delta < 20);
    }
}
