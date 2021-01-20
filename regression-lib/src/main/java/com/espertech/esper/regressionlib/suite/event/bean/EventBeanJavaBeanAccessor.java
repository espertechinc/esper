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
import com.espertech.esper.regressionlib.support.bean.SupportLegacyBeanInt;

import static org.junit.Assert.assertEquals;

public class EventBeanJavaBeanAccessor implements RegressionExecution {

    public void run(RegressionEnvironment env) {
        String statementText = "@name('s0') select intPrimitive, explicitFInt, explicitMGetInt, explicitMReadInt " +
            " from MyLegacyTwo#length(5)";
        env.compileDeploy(statementText).addListener("s0");

        SupportLegacyBeanInt theEvent = new SupportLegacyBeanInt(10);
        env.sendEventBean(theEvent, "MyLegacyTwo");

        env.assertEventNew("s0", event -> {
            for (String name : new String[]{"intPrimitive", "explicitFInt", "explicitMGetInt", "explicitMReadInt"}) {
                assertEquals(Integer.class, event.getEventType().getPropertyType(name));
                assertEquals(10, event.get(name));
            }
        });

        env.undeployAll();
    }
}
