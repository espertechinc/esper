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
package com.espertech.esperio.socket;

import com.espertech.esper.common.client.configuration.Configuration;
import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.runtime.client.EPRuntime;
import com.espertech.esper.runtime.client.EPRuntimeProvider;
import com.espertech.esper.runtime.client.EPStatement;
import com.espertech.esper.runtime.client.scopetest.SupportUpdateListener;
import junit.framework.TestCase;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import static com.espertech.esperio.socket.SupportCompileUtil.compileDeploy;

public class TestSocketAdapterObject extends TestCase {
    private SupportUpdateListener listener;

    public void setUp() throws Exception {
        listener = new SupportUpdateListener();
    }

    public void testSendObject() throws Exception {
        int port = 6800;
        String mapTypeName = "MyMapType";

        String esperIOConfig = "<esperio-socket-configuration>\n" +
            "<socket name=\"service1\" port=\"" + port + "\" data=\"object\"/>" +
            "</esperio-socket-configuration>";

        Configuration configuration = new Configuration();
        configuration.getRuntime().addPluginLoader("EsperIOSocketAdapter", EsperIOSocketAdapterPlugin.class.getName(), new Properties(), esperIOConfig);
        configuration.getCommon().addEventType("SupportBean", SupportBean.class);
        configuration.getCommon().addEventType(mapTypeName, getMapType());

        EPRuntime runtime = EPRuntimeProvider.getRuntime("SocketAdapterTest", configuration);

        EPStatement stmt = compileDeploy(runtime, "select * from SupportBean").getStatements()[0];
        stmt.addListener(listener);

        stmt = compileDeploy(runtime, "select * from " + mapTypeName).getStatements()[0];
        stmt.addListener(listener);

        SupportSocketUtil.sendSingleObject(port, new SupportBean("E1", 10));
        String[] fields = "stringProp,intProp".split(",");
        Thread.sleep(200);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E1", 10});

        SupportSocketClientObject client = new SupportSocketClientObject(port);
        client.send(new SupportBean("E2", 20));
        Thread.sleep(200);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E2", 20});

        client.send(new SupportBean("E3", 30));
        Thread.sleep(200);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E3", 30});

        client.close();

        SupportSocketClientObject clientOne = new SupportSocketClientObject(port);
        SupportSocketClientObject clientTwo = new SupportSocketClientObject(port);

        clientTwo.send(new SupportBean("E4", 40));
        Thread.sleep(200);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E4", 40});

        clientOne.send(new SupportBean("E5", 50));
        Thread.sleep(200);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E5", 50});

        clientOne.send(getMapEvent(mapTypeName, "E6", 60));
        Thread.sleep(200);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E6", 60});

        clientOne.close();
        clientTwo.close();
        runtime.destroy();
    }

    private Map<String, Object> getMapType() {
        Map<String, Object> type = new HashMap<String, Object>();
        type.put("stringProp", String.class);
        type.put("intProp", int.class);
        return type;
    }

    private Map<String, Object> getMapEvent(String typeName, String s, int v) {
        Map<String, Object> data = new HashMap<String, Object>();
        data.put("stream", typeName);
        data.put("stringProp", s);
        data.put("intProp", v);
        return data;
    }
}