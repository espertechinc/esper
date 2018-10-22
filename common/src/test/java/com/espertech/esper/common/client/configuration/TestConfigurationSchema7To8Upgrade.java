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

import junit.framework.TestCase;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

public class TestConfigurationSchema7To8Upgrade extends TestCase {
    private final String FILE_PREFIX = "regression/esper_version_7_old_configuration_file_";
    private final String FILE_ONE = FILE_PREFIX + "one.xml";
    private final String FILE_TWO = FILE_PREFIX + "two.xml";
    private final String FILE_THREE = FILE_PREFIX + "three.xml";

    public void testIt() {
        runAssertion(FILE_ONE);
        runAssertion(FILE_TWO);
        runAssertion(FILE_THREE);
    }

    private void runAssertion(String file) {
        URL url = this.getClass().getClassLoader().getResource(file);
        if (url == null) {
            fail("Failed to find " + FILE_ONE);
        }

        InputStream stream = null;
        try {
            stream = url.openStream();
            String result = ConfigurationSchema7To8Upgrade.upgrade(stream, file);
            System.out.println(result);
        } catch (Throwable t) {
            throw new RuntimeException(t);
        } finally {
            if (stream != null) {
                try {
                    stream.close();
                } catch (IOException e) {
                    fail(e.getMessage());
                }
            }
        }
    }
}
