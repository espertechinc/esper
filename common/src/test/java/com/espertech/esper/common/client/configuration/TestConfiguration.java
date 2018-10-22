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
package com.espertech.esper.common.client.configuration;

import com.espertech.esper.common.client.configuration.common.ConfigurationCommon;
import junit.framework.TestCase;

import java.io.File;
import java.net.URL;

public class TestConfiguration extends TestCase {
    protected static final String ESPER_TEST_CONFIG = "regression/esper.test.readconfig.cfg.xml";

    private Configuration config;

    public void setUp() {
        config = new Configuration();
        config.getRuntime().getLogging().setEnableExecutionDebug(true);
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
        ConfigurationCommon common = config.getCommon();
        common.addEventType("AEventType", "BClassName");

        assertTrue(common.isEventTypeExists("AEventType"));
        assertEquals(1, common.getEventTypeNames().size());
        assertEquals("BClassName", common.getEventTypeNames().get("AEventType"));
        assertDefaultConfig();
    }

    private void assertDefaultConfig() {
        ConfigurationCommon common = config.getCommon();
        assertEquals(6, common.getImports().size());
        assertEquals("java.lang.*", common.getImports().get(0));
        assertEquals("java.math.*", common.getImports().get(1));
        assertEquals("java.text.*", common.getImports().get(2));
        assertEquals("java.util.*", common.getImports().get(3));
        assertEquals("com.espertech.esper.common.client.annotation.*", common.getImports().get(4));
        assertEquals("com.espertech.esper.common.internal.epl.dataflow.ops.*", common.getImports().get(5));
    }
}
