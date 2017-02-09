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
package com.espertech.esperio.socket.config;

import junit.framework.TestCase;

import java.net.URL;

public class TestConfig extends TestCase {
    private ConfigurationSocketAdapter config;

    public void setUp() {
        config = new ConfigurationSocketAdapter();
    }

    public void testConfigureFromStream() throws Exception {
        URL url = this.getClass().getClassLoader().getResource("esperio-socket-sample-config.xml");
        ConfigurationSocketAdapterParser.doConfigure(config, url.openStream(), url.toString());
        assertFileConfig(config);
    }

    public void testEngineDefaults() {
        config = new ConfigurationSocketAdapter();
    }

    protected static void assertFileConfig(ConfigurationSocketAdapter config) throws Exception {
        assertEquals(3, config.getSockets().size());

        SocketConfig socket = config.getSockets().get("mysocketOne");
        assertEquals(7100, socket.getPort());
        assertEquals(DataType.OBJECT, socket.getDataType());
        assertNull(socket.getHostname());
        assertNull(socket.getBacklog());

        socket = config.getSockets().get("mysocketTwo");
        assertEquals(7100, socket.getPort());
        assertEquals(DataType.CSV, socket.getDataType());
        assertEquals("somehost", socket.getHostname());
        assertEquals(10, (int) socket.getBacklog());

        socket = config.getSockets().get("mysocketThree");
        assertEquals(DataType.PROPERTY_ORDERED_CSV, socket.getDataType());
        assertEquals("MyEventType", socket.getStream());
        assertEquals("field1,field2", socket.getPropertyOrder());
    }
}
