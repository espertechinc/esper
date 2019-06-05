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

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.configuration.Configuration;
import com.espertech.esper.common.client.json.minimaljson.Json;
import com.espertech.esper.common.client.json.minimaljson.JsonObject;
import com.espertech.esper.runtime.client.EPRuntime;
import com.espertech.esper.runtime.client.EPRuntimeProvider;
import com.espertech.esper.runtime.client.EPStatement;
import com.espertech.esper.runtime.client.plugin.PluginLoader;
import com.espertech.esper.runtime.client.scopetest.SupportUpdateListener;
import com.espertech.esper.runtime.client.util.InputAdapter;
import com.espertech.esper.runtime.internal.kernel.service.EPRuntimeSPI;
import com.espertech.esperio.jms.SpringContext;
import com.espertech.esperio.jms.SpringContextLoader;
import com.espertech.esperio.support.util.SupportSerializableBean;
import junit.framework.TestCase;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import static com.espertech.esperio.support.util.SupportCompileUtil.compileDeploy;

public class TestJMSSpringInputAdapter extends TestCase {
    private SupportJMSSender jmsSender;

    public void setUp() {
        jmsSender = new SupportJMSSender();

    }

    public void tearDown() {
        jmsSender.destroy();
    }

    public void testSerializable() throws Exception {
        Configuration config = getCommonConfig();
        config.getCommon().addEventType(SupportSerializableBean.class);
        EPRuntime runtime = EPRuntimeProvider.getRuntime(this.getClass().getName() + "_testSerializable", config);

        EPStatement statement = compileDeploy(runtime, "select * from SupportSerializableBean").getStatements()[0];
        SupportUpdateListener listener = new SupportUpdateListener();
        statement.addListener(listener);

        jmsSender.sendSerializable(new SupportSerializableBean("x1"));
        Thread.sleep(200);
        assertEquals("x1", listener.assertOneGetNewAndReset().get("theString"));

        jmsSender.sendSerializable(new SupportSerializableBean("x2"));
        Thread.sleep(200);
        assertEquals("x2", listener.assertOneGetNewAndReset().get("theString"));

        EPRuntimeSPI spi = (EPRuntimeSPI) runtime;
        PluginLoader loader = (PluginLoader) spi.getContext().lookup("plugin-loader/MyLoader");
        loader.destroy();
    }

    public void testMap() throws Exception {
        Configuration config = getCommonConfig();

        // define type
        Map<String, Object> typeProps = new HashMap<String, Object>();
        typeProps.put("prop1", String.class);
        typeProps.put("prop2", int.class);
        config.getCommon().addEventType("MyMapType", typeProps);

        EPRuntime runtime = EPRuntimeProvider.getRuntime(this.getClass().getName() + "_testMap", config);

        EPStatement statement = compileDeploy(runtime, "select * from MyMapType").getStatements()[0];
        SupportUpdateListener listener = new SupportUpdateListener();
        statement.addListener(listener);

        jmsSender.sendMap(makeMap("MyMapType", "IBM", 100));
        Thread.sleep(500);
        EventBean received = listener.assertOneGetNewAndReset();
        assertEquals("IBM", received.get("prop1"));
        assertEquals(100, received.get("prop2"));

        // test some invalid types
        jmsSender.sendMap(makeMap(null, "IBM", 100));
        jmsSender.sendMap(makeMap("xxx", "IBM", 100));

        jmsSender.sendMap(makeMap("MyMapType", "CSCO", 200));
        Thread.sleep(200);
        received = listener.assertOneGetNewAndReset();
        assertEquals("CSCO", received.get("prop1"));
        assertEquals(200, received.get("prop2"));
    }

    public void testJSON() throws Exception {
        Configuration config = getCommonConfig();
        EPRuntime runtime = EPRuntimeProvider.getRuntime(this.getClass().getName() + "_testJson", config);
        compileDeploy(runtime, "@public @buseventtype create json schema MyJsonType(prop1 string, prop2 int)");

        EPStatement statement = compileDeploy(runtime, "select * from MyJsonType").getStatements()[0];
        SupportUpdateListener listener = new SupportUpdateListener();
        statement.addListener(listener);

        jmsSender.sendJson("MyJsonType", makeJson("IBM", 100));
        Thread.sleep(500);
        EventBean received = listener.assertOneGetNewAndReset();
        assertEquals("IBM", received.get("prop1"));
        assertEquals(100, received.get("prop2"));
    }

    private Map<String, Object> makeMap(String type, String prop1, int prop2) {
        Map<String, Object> props = new HashMap<String, Object>();
        props.put("prop1", prop1);
        props.put("prop2", prop2);
        props.put(InputAdapter.ESPERIO_MAP_EVENT_TYPE, type);
        return props;
    }

    private String makeJson(String prop1, int prop2) {
        return new JsonObject().add("prop1", prop1).add("prop2", prop2).toString();
    }

    private Configuration getCommonConfig() {
        Configuration config = new Configuration();
        Properties props = new Properties();
        props.put(SpringContext.CLASSPATH_CONTEXT, "regression/jms_regression_input_spring.xml");
        config.getRuntime().addPluginLoader("MyLoader", SpringContextLoader.class.getName(), props);
        return config;
    }
}
