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
package com.espertech.esperio.http;

import com.espertech.esper.common.client.configuration.Configuration;
import com.espertech.esper.runtime.client.EPRuntime;
import com.espertech.esper.runtime.client.EPRuntimeProvider;
import com.espertech.esper.runtime.client.EPStatement;
import com.espertech.esper.runtime.client.scopetest.SupportUpdateListener;
import com.espertech.esperio.http.config.ConfigurationHTTPAdapter;
import com.espertech.esperio.http.config.Request;
import junit.framework.TestCase;

import static com.espertech.esperio.http.SupportCompileUtil.compileDeploy;

public class TestHTTPAdapterOutput extends TestCase {
    private final static String RUNTIME_URI = "TestHTTPAdapterOutput";

    private SupportUpdateListener listener;

    public void setUp() throws Exception {
        listener = new SupportUpdateListener();
    }

    public void testRequest() throws Exception {
        ConfigurationHTTPAdapter adapterConfig = new ConfigurationHTTPAdapter();

        Request requestOne = new Request();
        requestOne.setStream("SupportBean");
        requestOne.setUri("http://localhost:8078/root");
        adapterConfig.getRequests().add(requestOne);

        Request requestTwo = new Request();
        requestTwo.setStream("SupportBean");
        requestTwo.setUri("http://localhost:8077/root/${stream}/${stringProp}/${intProp}");
        adapterConfig.getRequests().add(requestTwo);

        EsperIOHTTPAdapter adapter = new EsperIOHTTPAdapter(adapterConfig, RUNTIME_URI);

        Configuration configuration = new Configuration();
        configuration.getCommon().addEventType("SupportBean", SupportBean.class);
        configuration.getCommon().addEventType("SupportBeanTwo", SupportBeanTwo.class);
        EPRuntime runtime = EPRuntimeProvider.getRuntime(RUNTIME_URI, configuration);

        adapter.start();

        EPStatement stmt = compileDeploy(runtime, "insert into SupportBean select id as stringProp, 5 as intProp from SupportBeanTwo").getStatements()[0];
        stmt.addListener(listener);

        SupportHTTPServer server8078 = new SupportHTTPServer(8078);
        server8078.start();

        runtime.getEventService().sendEventBean(new SupportBeanTwo("E1"), "SupportBeanTwo");
        assertEquals(1, SupportHTTPServerReqestHandler.getTargets().size());
        assertEquals("/root?stream=SupportBean&intProp=5&stringProp=E1", SupportHTTPServerReqestHandler.getAndResetTargets().get(0));

        runtime.getEventService().sendEventBean(new SupportBeanTwo("E3"), "SupportBeanTwo");
        assertEquals("/root?stream=SupportBean&intProp=5&stringProp=E3", SupportHTTPServerReqestHandler.getAndResetTargets().get(0));

        server8078.stop();
        Thread.sleep(5000);

        SupportHTTPServer server8077 = new SupportHTTPServer(8077);
        server8077.start();

        runtime.getEventService().sendEventBean(new SupportBeanTwo("E2"), "SupportBeanTwo");
        assertEquals(1, SupportHTTPServerReqestHandler.getTargets().size());
        assertEquals("/root/SupportBean/E2/5", SupportHTTPServerReqestHandler.getAndResetTargets().get(0));

        runtime.getEventService().sendEventBean(new SupportBeanTwo("E4"), "SupportBeanTwo");
        assertEquals("/root/SupportBean/E4/5", SupportHTTPServerReqestHandler.getAndResetTargets().get(0));

        server8077.stop();

        adapter.destroy();
        runtime.destroy();
    }
}