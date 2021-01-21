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

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.RegressionFlag;
import com.espertech.esper.regressionlib.support.bean.SupportBeanComplexProps;

import java.util.EnumSet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class EventBeanEventPropertyDynamicPerformance implements RegressionExecution {
    @Override
    public EnumSet<RegressionFlag> flags() {
        return EnumSet.of(RegressionFlag.EXCLUDEWHENINSTRUMENTED, RegressionFlag.PERFORMANCE);
    }

    public void run(RegressionEnvironment env) {

        String stmtText = "@name('s0') select simpleProperty?, " +
            "indexed[1]? as indexed, " +
            "mapped('keyOne')? as mapped " +
            "from SupportBeanComplexProps";
        env.compileDeploy(stmtText).addListener("s0");

        env.assertStatement("s0", statement -> {
            EventType type = statement.getEventType();
            assertEquals(Object.class, type.getPropertyType("simpleProperty?"));
            assertEquals(Object.class, type.getPropertyType("indexed"));
            assertEquals(Object.class, type.getPropertyType("mapped"));
        });

        SupportBeanComplexProps inner = SupportBeanComplexProps.makeDefaultBean();
        env.sendEventBean(inner);
        env.assertEventNew("s0", theEvent -> {
            assertEquals(inner.getSimpleProperty(), theEvent.get("simpleProperty?"));
            assertEquals(inner.getIndexed(1), theEvent.get("indexed"));
            assertEquals(inner.getMapped("keyOne"), theEvent.get("mapped"));
        });

        long start = System.currentTimeMillis();
        for (int i = 0; i < 10000; i++) {
            env.sendEventBean(inner);
            if (i % 1000 == 0) {
                env.listenerReset("s0");
            }
        }
        long end = System.currentTimeMillis();
        long delta = end - start;
        assertTrue("delta=" + delta, delta < 1000);

        env.undeployAll();
    }
}
