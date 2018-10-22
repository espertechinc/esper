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
package com.espertech.esperio.regression.adapter;

import com.espertech.esper.common.client.configuration.Configuration;
import com.espertech.esper.runtime.client.EPRuntime;
import com.espertech.esper.runtime.client.EPRuntimeProvider;
import com.espertech.esperio.jms.SpringContext;
import com.espertech.esperio.jms.SpringContextLoader;
import com.espertech.esperio.support.util.SupportSerializableBean;
import junit.framework.TestCase;

import javax.jms.MapMessage;
import javax.jms.Message;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import static com.espertech.esperio.support.util.SupportCompileUtil.compileDeploy;

public class TestJMSSpringOutputAdapter extends TestCase {
    private SupportJMSReceiver jmsReceiver;

    public void setUp() {
        jmsReceiver = new SupportJMSReceiver();
    }

    public void tearDown() {
        jmsReceiver.destroy();
    }

    public void testOutputAdapter() throws Exception {
        Configuration config = new Configuration();
        config.getCommon().addEventType(SupportSerializableBean.class);
        config.getRuntime().getThreading().setInternalTimerEnabled(false);

        // define output type
        Map<String, Object> typeProps = new HashMap<String, Object>();
        typeProps.put("prop1", String.class);
        typeProps.put("prop2", String.class);
        config.getCommon().addEventType("MyOutputStream", typeProps);

        // define loader
        Properties props = new Properties();
        props.put(SpringContext.CLASSPATH_CONTEXT, "regression/jms_regression_output_spring.xml");
        config.getRuntime().addPluginLoader("MyLoader", SpringContextLoader.class.getName(), props);
        EPRuntime runtime = EPRuntimeProvider.getRuntime(this.getClass().getName() + "_testOutputAdapter", config);

        compileDeploy(runtime, "insert into MyOutputStream select theString as prop1, '>' || theString || '<' as prop2 from SupportSerializableBean");

        runtime.getEventService().sendEventBean(new SupportSerializableBean("x1"), "SupportSerializableBean");
        Message result = jmsReceiver.receiveMessage();
        assertNotNull(result);
        MapMessage mapMsg = (MapMessage) result;
        assertEquals("x1", mapMsg.getObject("prop1"));
        assertEquals(">x1<", mapMsg.getObject("prop2"));

        runtime.getEventService().sendEventBean(new SupportSerializableBean("x2"), "SupportSerializableBean");
        result = jmsReceiver.receiveMessage();
        assertNotNull(result);
        mapMsg = (MapMessage) result;
        assertEquals("x2", mapMsg.getObject("prop1"));
        assertEquals(">x2<", mapMsg.getObject("prop2"));
    }
}
