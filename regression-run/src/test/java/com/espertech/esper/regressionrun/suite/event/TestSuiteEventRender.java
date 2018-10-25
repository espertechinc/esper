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
package com.espertech.esper.regressionrun.suite.event;

import com.espertech.esper.common.client.configuration.Configuration;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.common.internal.support.SupportBean_S0;
import com.espertech.esper.regressionlib.suite.event.render.EventRender;
import com.espertech.esper.regressionlib.suite.event.render.EventRenderJSON;
import com.espertech.esper.regressionlib.suite.event.render.EventRenderXML;
import com.espertech.esper.regressionlib.support.bean.*;
import com.espertech.esper.regressionrun.runner.RegressionRunner;
import com.espertech.esper.regressionrun.runner.RegressionSession;
import junit.framework.TestCase;

import java.util.LinkedHashMap;
import java.util.Map;

public class TestSuiteEventRender extends TestCase {
    private RegressionSession session;

    public void setUp() {
        session = RegressionRunner.session();
        configure(session.getConfiguration());
    }

    public void tearDown() {
        session.destroy();
        session = null;
    }

    public void testEventRender() {
        RegressionRunner.run(session, EventRender.executions());
    }

    public void testEventRenderJSON() {
        RegressionRunner.run(session, EventRenderJSON.executions());
    }

    public void testEventRenderXML() {
        RegressionRunner.run(session, EventRenderXML.executions());
    }

    private static void configure(Configuration configuration) {
        for (Class clazz : new Class[]{SupportBean.class, EventRender.MyRendererEvent.class,
            SupportBeanRendererOne.class, SupportBeanRendererThree.class,
            EventRenderJSON.EmptyMapEvent.class}) {
            configuration.getCommon().addEventType(clazz);
        }

        String[] props = {"p0", "p1", "p2", "p3", "p4"};
        Object[] types = {String.class, int.class, SupportBean_S0.class, long.class, Double.class};
        configuration.getCommon().addEventType("MyObjectArrayType", props, types);

        Map<String, Object> outerMap = new LinkedHashMap<String, Object>();
        outerMap.put("intarr", int[].class);
        outerMap.put("innersimple", "InnerMap");
        outerMap.put("innerarray", "InnerMap[]");
        outerMap.put("prop0", SupportBean_A.class);

        Map<String, Object> innerMap = new LinkedHashMap<String, Object>();
        innerMap.put("stringarr", String[].class);
        innerMap.put("prop1", String.class);

        configuration.getCommon().addEventType("InnerMap", innerMap);
        configuration.getCommon().addEventType("OuterMap", outerMap);

        configuration.getCompiler().getViewResources().setIterableUnbound(true);
    }
}
