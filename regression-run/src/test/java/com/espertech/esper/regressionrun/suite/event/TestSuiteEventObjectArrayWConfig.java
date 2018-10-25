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
import com.espertech.esper.common.client.configuration.ConfigurationException;
import com.espertech.esper.common.client.configuration.common.ConfigurationCommonEventTypeObjectArray;
import com.espertech.esper.common.client.util.EventUnderlyingType;
import com.espertech.esper.regressionlib.suite.event.objectarray.EventObjectArrayConfiguredStatic;
import com.espertech.esper.regressionlib.suite.event.objectarray.EventObjectArrayInheritanceConfigRuntime;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.regressionrun.runner.RegressionRunner;
import com.espertech.esper.regressionrun.runner.RegressionSession;
import com.espertech.esper.regressionrun.runner.SupportConfigFactory;
import junit.framework.TestCase;

import java.util.Arrays;
import java.util.HashSet;

public class TestSuiteEventObjectArrayWConfig extends TestCase {

    public void testEventObjectArrayConfiguredStatic() {
        RegressionSession session = RegressionRunner.session();
        session.getConfiguration().getCommon().getEventMeta().setDefaultEventRepresentation(EventUnderlyingType.OBJECTARRAY);
        session.getConfiguration().getCommon().addEventType("MyOAType", "bean,theString,map".split(","), new Object[]{SupportBean.class.getName(), "string", "java.util.Map"});
        RegressionRunner.run(session, new EventObjectArrayConfiguredStatic());
        session.destroy();
    }

    public void testEventObjectArrayInheritanceConfigRuntime() {
        RegressionSession session = RegressionRunner.session();
        RegressionRunner.run(session, new EventObjectArrayInheritanceConfigRuntime());
        session.destroy();
    }

    public void testInvalidConfig() {
        // invalid multiple supertypes
        ConfigurationCommonEventTypeObjectArray invalidOAConfig = new ConfigurationCommonEventTypeObjectArray();
        invalidOAConfig.setSuperTypes(new HashSet<>(Arrays.asList("A", "B")));
        String[] invalidOANames = new String[]{"p00"};
        Object[] invalidOATypes = new Object[]{int.class};
        try {
            Configuration configuration = SupportConfigFactory.getConfiguration();
            configuration.getCommon().addEventType("MyInvalidEventTwo", invalidOANames, invalidOATypes, invalidOAConfig);
            fail();
        } catch (ConfigurationException ex) {
            assertEquals("Object-array event types only allow a single supertype", ex.getMessage());
        }

        // mismatched property number
        try {
            Configuration configuration = SupportConfigFactory.getConfiguration();
            configuration.getCommon().addEventType("MyInvalidEvent", new String[]{"p00"}, new Object[]{int.class, String.class});
            fail();
        } catch (ConfigurationException ex) {
            assertEquals("Number of property names and property types do not match, found 1 property names and 2 property types", ex.getMessage());
        }
    }
}
