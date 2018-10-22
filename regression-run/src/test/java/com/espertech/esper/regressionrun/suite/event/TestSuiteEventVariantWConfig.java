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

import com.espertech.esper.common.client.configuration.common.ConfigurationCommonVariantStream;
import com.espertech.esper.regressionrun.runner.SupportConfigFactory;
import junit.framework.TestCase;

import static com.espertech.esper.regressionlib.framework.SupportMessageAssertUtil.tryInvalidConfigurationCompiler;

public class TestSuiteEventVariantWConfig extends TestCase {
    public void testInvalidConfig() {
        ConfigurationCommonVariantStream config = new ConfigurationCommonVariantStream();
        tryInvalidVarstream(config, "Failed compiler startup: Invalid variant stream configuration, no event type name has been added and default type variance requires at least one type, for name 'ABC'");

        config.addEventTypeName("dummy");
        tryInvalidVarstream(config, "Failed compiler startup: Event type by name 'dummy' could not be found for use in variant stream configuration by name 'ABC'");
    }

    private void tryInvalidVarstream(ConfigurationCommonVariantStream config, String expected) {
        tryInvalidConfigurationCompiler(SupportConfigFactory.getConfiguration(), configuration -> configuration.getCommon().addVariantStream("ABC", config), expected);
    }
}
