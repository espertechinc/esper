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
package com.espertech.esper.regressionlib.suite.event.bean;

import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.RegressionFlag;
import com.espertech.esper.regressionlib.support.bean.SupportBeanCombinedProps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.EnumSet;

import static org.junit.Assert.assertTrue;

public class EventBeanPropertyAccessPerformance implements RegressionExecution {
    @Override
    public EnumSet<RegressionFlag> flags() {
        return EnumSet.of(RegressionFlag.EXCLUDEWHENINSTRUMENTED, RegressionFlag.PERFORMANCE);
    }

    public void run(RegressionEnvironment env) {
        String methodName = ".testPerfPropertyAccess";

        String joinStatement = "@name('s0') select * from " +
            "SupportBeanCombinedProps#length(1)" +
            " where indexed[0].mapped('a').value = 'dummy'";
        env.compileDeploy(joinStatement).addListener("s0");

        // Send events for each stream
        SupportBeanCombinedProps theEvent = SupportBeanCombinedProps.makeDefaultBean();
        log.info(methodName + " Sending events");

        long startTime = System.currentTimeMillis();
        for (int i = 0; i < 10000; i++) {
            sendEvent(env, theEvent);
        }
        log.info(methodName + " Done sending events");

        long endTime = System.currentTimeMillis();
        log.info(methodName + " delta=" + (endTime - startTime));

        // Stays at 250, below 500ms
        assertTrue((endTime - startTime) < 1000);

        env.undeployAll();
    }

    private void sendEvent(RegressionEnvironment env, Object theEvent) {
        env.sendEventBean(theEvent);
    }

    private static final Logger log = LoggerFactory.getLogger(EventBeanPropertyAccessPerformance.class);
}
