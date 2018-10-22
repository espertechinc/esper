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
package com.espertech.esper.example.autoid;

import com.espertech.esper.common.client.EPCompiled;
import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.configuration.Configuration;
import com.espertech.esper.compiler.client.CompilerArguments;
import com.espertech.esper.compiler.client.EPCompilerProvider;
import com.espertech.esper.runtime.client.EPRuntime;
import com.espertech.esper.runtime.client.EPRuntimeProvider;
import com.espertech.esper.runtime.client.scopetest.SupportUpdateListener;
import junit.framework.TestCase;
import org.w3c.dom.Document;

import javax.xml.parsers.DocumentBuilderFactory;
import java.net.URL;

import static com.espertech.esper.example.autoid.AutoIdEPL.EPL;
import static com.espertech.esper.example.autoid.AutoIdEPL.EVENTTYPE;

public class TestRFIDTagsPerSensorStmt extends TestCase {
    private EPRuntime runtime;
    private SupportUpdateListener listener;

    public void setUp() throws Exception {
        URL url = TestRFIDTagsPerSensorStmt.class.getClassLoader().getResource("esper.examples.cfg.xml");
        Configuration config = new Configuration();
        if (url == null) {
            throw new RuntimeException("Could not load sample config file from classpath");
        }
        config.configure(url);

        EPCompiled compiled = EPCompilerProvider.getCompiler().compile(EPL, new CompilerArguments(config));
        runtime = EPRuntimeProvider.getRuntime(TestRFIDTagsPerSensorStmt.class.getSimpleName(), config);

        listener = new SupportUpdateListener();
        runtime.getDeploymentService().deploy(compiled).getStatements()[0].addListener(listener);
    }

    public void tearDown() {
        runtime.destroy();
    }

    public void testEvents() throws Exception {
        DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
        builderFactory.setNamespaceAware(true);

        Document sensor1Doc = builderFactory.newDocumentBuilder().parse(TestRFIDTagsPerSensorStmt.class.getClassLoader().getResourceAsStream("data/AutoIdSensor1.xml"));
        runtime.getEventService().sendEventXMLDOM(sensor1Doc, EVENTTYPE);
        assertReceived("urn:epc:1:4.16.36", 5);
    }

    private void assertReceived(String sensorId, double numTags) {
        assertTrue(listener.isInvoked());
        assertEquals(1, listener.getLastNewData().length);
        EventBean theEvent = listener.getLastNewData()[0];
        assertEquals(sensorId, theEvent.get("sensorId"));
        assertEquals(numTags, theEvent.get("numTagsPerSensor"));
        listener.reset();
    }
}
