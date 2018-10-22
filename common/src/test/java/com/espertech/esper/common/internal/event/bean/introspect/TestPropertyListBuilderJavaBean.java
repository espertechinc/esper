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
package com.espertech.esper.common.internal.event.bean.introspect;

import com.espertech.esper.common.client.configuration.common.ConfigurationCommonEventTypeBean;
import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.common.internal.event.bean.core.PropertyStem;
import com.espertech.esper.common.internal.event.core.EventPropertyType;
import com.espertech.esper.common.internal.supportunit.bean.SupportLegacyBean;
import junit.framework.TestCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedList;
import java.util.List;

public class TestPropertyListBuilderJavaBean extends TestCase {
    private PropertyListBuilderJavaBean builder;

    public void setUp() {
        ConfigurationCommonEventTypeBean config = new ConfigurationCommonEventTypeBean();

        // add 2 explicit properties, also supported
        config.addFieldProperty("x", "fieldNested");
        config.addMethodProperty("y", "readLegacyBeanVal");

        builder = new PropertyListBuilderJavaBean(config);
    }

    public void testBuildPropList() throws Exception {
        List<PropertyStem> descList = builder.assessProperties(SupportLegacyBean.class);

        List<PropertyStem> expected = new LinkedList<PropertyStem>();
        expected.add(new PropertyStem("x", SupportLegacyBean.class.getField("fieldNested"), EventPropertyType.SIMPLE));
        expected.add(new PropertyStem("y", SupportLegacyBean.class.getMethod("readLegacyBeanVal"), EventPropertyType.SIMPLE));
        EPAssertionUtil.assertEqualsAnyOrder(expected.toArray(), descList.toArray());
    }

    private final static Logger log = LoggerFactory.getLogger(TestPropertyListBuilderJavaBean.class);
}
