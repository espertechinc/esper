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

import com.espertech.esper.common.client.EPException;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.common.internal.support.SupportBean;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class EventObjectArrayNestedMap implements RegressionExecution {

    public void run(RegressionEnvironment env) {
        assertEquals(Object[].class, env.runtime().getEventTypeService().getEventTypePreconfigured("MyMapNestedObjectArray").getUnderlyingType());
        env.compileDeploy("@name('s0') select lev0name.lev1name.sb.theString as val from MyMapNestedObjectArray").addListener("s0");

        Map<String, Object> lev2data = new HashMap<String, Object>();
        lev2data.put("sb", new SupportBean("E1", 0));
        Map<String, Object> lev1data = new HashMap<String, Object>();
        lev1data.put("lev1name", lev2data);

        env.sendEventObjectArray(new Object[]{lev1data}, "MyMapNestedObjectArray");
        assertEquals("E1", env.listener("s0").assertOneGetNewAndReset().get("val"));

        try {
            env.sendEventMap(new HashMap(), "MyMapNestedObjectArray");
            fail();
        } catch (EPException ex) {
            assertEquals("Event type named 'MyMapNestedObjectArray' has not been defined or is not a Map-type event type, the name 'MyMapNestedObjectArray' refers to a java.lang.Object(Array) event type", ex.getMessage());
        }

        env.undeployAll();
    }
}
