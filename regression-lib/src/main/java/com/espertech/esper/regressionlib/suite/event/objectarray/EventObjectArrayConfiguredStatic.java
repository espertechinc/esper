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
package com.espertech.esper.regressionlib.suite.event.objectarray;

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.common.internal.support.SupportBean;

import java.util.Collections;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class EventObjectArrayConfiguredStatic implements RegressionExecution {

    public void run(RegressionEnvironment env) {
        EventType eventType = env.runtime().getEventTypeService().getEventTypePreconfigured("MyOAType");
        assertEquals(Object[].class, eventType.getUnderlyingType());
        assertEquals(String.class, eventType.getPropertyType("theString"));
        assertEquals(Map.class, eventType.getPropertyType("map"));
        assertEquals(SupportBean.class, eventType.getPropertyType("bean"));

        env.compileDeploy("@name('s0') select bean, theString, map('key'), bean.theString from MyOAType");
        env.addListener("s0");

        assertEquals(Object[].class, env.statement("s0").getEventType().getUnderlyingType());

        SupportBean bean = new SupportBean("E1", 1);
        env.sendEventObjectArray(new Object[]{bean, "abc", Collections.singletonMap("key", "value")}, "MyOAType");
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNew(), "bean,theString,map('key'),bean.theString".split(","), new Object[]{bean, "abc", "value", "E1"});

        env.undeployAll();
    }
}
