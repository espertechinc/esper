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
package com.espertech.esper.client;

import junit.framework.TestCase;

import java.io.File;
import java.net.URL;

public class TestConfiguration extends TestCase {
    protected static final String ESPER_TEST_CONFIG = "regression/esper.test.readconfig.cfg.xml";

    private Configuration config;

    public void setUp() {
        config = new Configuration();
        config.getEngineDefaults().getLogging().setEnableExecutionDebug(true);
    }

    public void testString() throws Exception {
        config.configure(ESPER_TEST_CONFIG);
        TestConfigurationParser.assertFileConfig(config);
    }

    public void testURL() throws Exception {
        URL url = this.getClass().getClassLoader().getResource(ESPER_TEST_CONFIG);
        config.configure(url);
        TestConfigurationParser.assertFileConfig(config);
    }

    public void testFile() throws Exception {
        URL url = this.getClass().getClassLoader().getResource(ESPER_TEST_CONFIG);
        File file = new File(url.toURI());
        config.configure(file);
        TestConfigurationParser.assertFileConfig(config);
    }

    public void testAddeventTypeName() {
        config.addEventType("AEventType", "BClassName");

        assertTrue(config.isEventTypeExists("AEventType"));
        assertEquals(1, config.getEventTypeNames().size());
        assertEquals("BClassName", config.getEventTypeNames().get("AEventType"));
        assertDefaultConfig();
    }

    private void assertDefaultConfig() {
        assertEquals(6, config.getImports().size());
        assertEquals("java.lang.*", config.getImports().get(0));
        assertEquals("java.math.*", config.getImports().get(1));
        assertEquals("java.text.*", config.getImports().get(2));
        assertEquals("java.util.*", config.getImports().get(3));
        assertEquals("com.espertech.esper.client.annotation.*", config.getImports().get(4));
        assertEquals("com.espertech.esper.dataflow.ops.*", config.getImports().get(5));
    }
}
