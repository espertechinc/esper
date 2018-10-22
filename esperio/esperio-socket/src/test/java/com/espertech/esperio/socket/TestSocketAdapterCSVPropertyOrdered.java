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
import com.espertech.esperio.socket.config.ConfigurationSocketAdapter;
import com.espertech.esperio.socket.config.DataType;
import com.espertech.esperio.socket.config.SocketConfig;
import junit.framework.TestCase;

import static com.espertech.esperio.socket.SupportCompileUtil.compileDeploy;

public class TestSocketAdapterCSVPropertyOrdered extends TestCase {
    public static String newline = System.getProperty("line.separator");
    private SupportUpdateListener listener;

    public void setUp() throws Exception {
        listener = new SupportUpdateListener();
    }

    public void testSendCSV() throws Exception {
        ConfigurationSocketAdapter adapterConfig = new ConfigurationSocketAdapter();
        int port = 6801;
        String runtimeURI = this.getClass().getSimpleName();

        SocketConfig socket = new SocketConfig();
        socket.setDataType(DataType.PROPERTY_ORDERED_CSV);
        socket.setPort(port);
        socket.setStream("SupportBean");
        socket.setPropertyOrder("stringProp,intProp");
        adapterConfig.getSockets().put("SocketService", socket);

        EsperIOSocketAdapter adapter = new EsperIOSocketAdapter(adapterConfig, runtimeURI);

        Configuration configuration = new Configuration();
        configuration.getCommon().addEventType("SupportBean", SupportBean.class);
        EPRuntime runtime = EPRuntimeProvider.getRuntime(runtimeURI, configuration);

        adapter.start();

        EPStatement stmt = compileDeploy(runtime, "select * from SupportBean").getStatements()[0];
        stmt.addListener(listener);

        String[] fields = "stringProp,intProp".split(",");
        SupportSocketClientCSV client = new SupportSocketClientCSV(port);

        client.send("E1,20" + newline);
        Thread.sleep(200);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E1", 20});

        client.close();
        adapter.destroy();
        runtime.destroy();
    }
}