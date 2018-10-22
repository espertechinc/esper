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
package com.espertech.esper.regressionrun.suite.event;

import com.espertech.esper.common.client.configuration.common.ConfigurationCommonEventTypeXMLDOM;
import com.espertech.esper.regressionrun.runner.SupportConfigFactory;
import junit.framework.TestCase;

import javax.xml.xpath.XPathConstants;

import static com.espertech.esper.regressionlib.framework.SupportMessageAssertUtil.tryInvalidConfigurationCompileAndRuntime;

public class TestSuiteEventXMLWConfig extends TestCase {

    public void testInvalidConfig() {
        ConfigurationCommonEventTypeXMLDOM desc = new ConfigurationCommonEventTypeXMLDOM();
        desc.setRootElementName("ABC");
        desc.setStartTimestampPropertyName("mystarttimestamp");
        desc.setEndTimestampPropertyName("myendtimestamp");
        desc.addXPathProperty("mystarttimestamp", "/test/prop", XPathConstants.NUMBER);

        tryInvalidConfigurationCompileAndRuntime(SupportConfigFactory.getConfiguration(),
            config -> config.getCommon().addEventType("TypeXML", desc),
            "Declared start timestamp property 'mystarttimestamp' is expected to return a Date, Calendar or long-typed value but returns 'java.lang.Double'");
    }
}
