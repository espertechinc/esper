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

import com.espertech.esper.common.client.configuration.Configuration;
import com.espertech.esper.common.client.configuration.common.ConfigurationCommonEventTypeMap;
import com.espertech.esper.common.internal.support.SupportJavaVersionUtil;
import com.espertech.esper.regressionlib.framework.SupportMessageAssertUtil;
import com.espertech.esper.regressionlib.suite.event.map.EventMapCore;
import com.espertech.esper.regressionlib.suite.event.map.EventMapInheritanceRuntime;
import com.espertech.esper.regressionlib.suite.event.map.EventMapNestedConfigRuntime;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.regressionrun.runner.RegressionRunner;
import com.espertech.esper.regressionrun.runner.RegressionSession;
import com.espertech.esper.regressionrun.runner.SupportConfigFactory;
import junit.framework.TestCase;

import java.util.Collections;
import java.util.Map;
import java.util.function.Consumer;

public class TestSuiteEventMapWConfig extends TestCase {

    public void testEventMapNestedConfigRuntime() {
        RegressionSession session = RegressionRunner.session();
        RegressionRunner.run(session, new EventMapNestedConfigRuntime());
        session.destroy();
    }

    public void testEventMapInheritanceRuntime() {
        RegressionSession session = RegressionRunner.session();
        RegressionRunner.run(session, new EventMapInheritanceRuntime());
        session.destroy();
    }

    public void testInvalidConfig() {
        // supertype not found
        tryInvalidConfigure(config -> {
            ConfigurationCommonEventTypeMap map = new ConfigurationCommonEventTypeMap();
            map.setSuperTypes(Collections.singleton("NONE"));
            config.getCommon().addEventType("InvalidMap", Collections.emptyMap(), map);
        }, "Supertype by name 'NONE' could not be found");

        // invalid property
        tryInvalidConfigure(config -> {
            config.getCommon().addEventType("InvalidMap", Collections.singletonMap("key", "XXX"));
        }, "Nestable type configuration encountered an unexpected property type name 'XXX' for property 'key', expected java.lang.Class or java.util.Map or the name of a previously-declared event type");

        // invalid key
        final Map<String, Object> invalid = EventMapCore.makeMap(new Object[][]{{new Integer(5), null}});
        tryInvalidConfigure(config -> {
            config.getCommon().addEventType("InvalidMap", invalid);
        }, SupportJavaVersionUtil.getCastMessage(Integer.class, String.class));

        final Map<String, Object> invalidTwo = EventMapCore.makeMap(new Object[][]{{"abc", new SupportBean()}});
        tryInvalidConfigure(config -> {
            config.getCommon().addEventType("InvalidMap", invalidTwo);
        }, "Nestable type configuration encountered an unexpected property type name 'SupportBean(null, 0)' for property 'abc', expected java.lang.Class or java.util.Map or the name of a previously-declared event type");
    }

    private void tryInvalidConfigure(Consumer<Configuration> configurer, String expected) {
        SupportMessageAssertUtil.tryInvalidConfigurationCompileAndRuntime(SupportConfigFactory.getConfiguration(), configurer, expected);
    }
}
