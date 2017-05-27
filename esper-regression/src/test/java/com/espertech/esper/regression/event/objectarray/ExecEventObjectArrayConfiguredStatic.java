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
package com.espertech.esper.regression.event.objectarray;

import com.espertech.esper.client.Configuration;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.EventType;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.client.util.EventUnderlyingType;
import com.espertech.esper.regression.event.map.TestSuiteEventMap;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.execution.RegressionExecution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class ExecEventObjectArrayConfiguredStatic implements RegressionExecution {
    public void configure(Configuration configuration) throws Exception {
        configuration.getEngineDefaults().getEventMeta().setDefaultEventRepresentation(EventUnderlyingType.OBJECTARRAY);
        configuration.addEventType("MyOAType", "bean,theString,map".split(","), new Object[]{SupportBean.class.getName(), "string", "java.util.Map"});
    }

    public void run(EPServiceProvider epService) throws Exception {
        EventType eventType = epService.getEPAdministrator().getConfiguration().getEventType("MyOAType");
        assertEquals(Object[].class, eventType.getUnderlyingType());
        assertEquals(String.class, eventType.getPropertyType("theString"));
        assertEquals(Map.class, eventType.getPropertyType("map"));
        assertEquals(SupportBean.class, eventType.getPropertyType("bean"));

        EPStatement stmt = epService.getEPAdministrator().createEPL("select bean, theString, map('key'), bean.theString from MyOAType");
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);
        assertEquals(Object[].class, stmt.getEventType().getUnderlyingType());

        SupportBean bean = new SupportBean("E1", 1);
        epService.getEPRuntime().sendEvent(new Object[]{bean, "abc", Collections.singletonMap("key", "value")}, "MyOAType");
        EPAssertionUtil.assertProps(listener.assertOneGetNew(), "bean,theString,map('key'),bean.theString".split(","), new Object[]{bean, "abc", "value", "E1"});
    }

    private final static Logger log = LoggerFactory.getLogger(TestSuiteEventMap.class);
}
