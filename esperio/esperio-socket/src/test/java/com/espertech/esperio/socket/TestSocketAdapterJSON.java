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
import com.espertech.esper.common.client.json.minimaljson.JsonObject;
import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.runtime.client.EPRuntime;
import com.espertech.esper.runtime.client.EPRuntimeProvider;
import com.espertech.esper.runtime.client.EPStatement;
import com.espertech.esper.runtime.client.scopetest.SupportUpdateListener;
import com.espertech.esperio.socket.config.ConfigurationSocketAdapter;
import com.espertech.esperio.socket.config.DataType;
import com.espertech.esperio.socket.config.SocketConfig;
import junit.framework.TestCase;

import static com.espertech.esperio.socket.SupportCompileUtil.compileDeploy;

public class TestSocketAdapterJSON extends TestCase {

    public void testSendJSON() throws Exception {
        ConfigurationSocketAdapter adapterConfig = new ConfigurationSocketAdapter();
        int port = 6801;
        String runtimeURI = "TestSocketAdapterJSON";

        SocketConfig socket = new SocketConfig();
        socket.setDataType(DataType.JSON);
        socket.setPort(port);
        adapterConfig.getSockets().put("SocketService", socket);

        EsperIOSocketAdapter adapter = new EsperIOSocketAdapter(adapterConfig, runtimeURI);

        Configuration configuration = new Configuration();
        EPRuntime runtime = EPRuntimeProvider.getRuntime(runtimeURI, configuration);

        adapter.start();

        compileDeploy(runtime, "@public @buseventtype create json schema MyEvent(stringProp string, intProp int)");
        EPStatement stmt = compileDeploy(runtime, "select * from MyEvent").getStatements()[0];
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        String[] fields = "stringProp,intProp".split(",");
        SupportSocketClientCSV client = new SupportSocketClientCSV(port);

        JsonObject json = new JsonObject().add("stringProp", "abc").add("intProp", 10);
        String message = "stream=MyEvent,json=" + json.toString() + "\n";
        System.out.println(message);
        client.send(message);
        Thread.sleep(200);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"abc", 10});

        client.close();
        adapter.destroy();
        runtime.destroy();
    }
}