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
package com.espertech.esper.event.bean;

import com.espertech.esper.client.ConfigurationEventTypeLegacy;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.event.EventPropertyType;
import com.espertech.esper.supportunit.bean.SupportLegacyBean;
import junit.framework.TestCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedList;
import java.util.List;

public class TestPropertyListBuilderPublic extends TestCase {
    private PropertyListBuilderPublic builder;

    public void setUp() {
        ConfigurationEventTypeLegacy config = new ConfigurationEventTypeLegacy();

        // add 2 explicit properties, also supported
        config.addFieldProperty("x", "fieldNested");
        config.addMethodProperty("y", "readLegacyBeanVal");

        builder = new PropertyListBuilderPublic(config);
    }

    public void testBuildPropList() throws Exception {
        List<InternalEventPropDescriptor> descList = builder.assessProperties(SupportLegacyBean.class);

        List<InternalEventPropDescriptor> expected = new LinkedList<InternalEventPropDescriptor>();
        expected.add(new InternalEventPropDescriptor("fieldLegacyVal", SupportLegacyBean.class.getField("fieldLegacyVal"), EventPropertyType.SIMPLE));
        expected.add(new InternalEventPropDescriptor("fieldStringArray", SupportLegacyBean.class.getField("fieldStringArray"), EventPropertyType.SIMPLE));
        expected.add(new InternalEventPropDescriptor("fieldMapped", SupportLegacyBean.class.getField("fieldMapped"), EventPropertyType.SIMPLE));
        expected.add(new InternalEventPropDescriptor("fieldNested", SupportLegacyBean.class.getField("fieldNested"), EventPropertyType.SIMPLE));

        expected.add(new InternalEventPropDescriptor("readLegacyBeanVal", SupportLegacyBean.class.getMethod("readLegacyBeanVal"), EventPropertyType.SIMPLE));
        expected.add(new InternalEventPropDescriptor("readStringArray", SupportLegacyBean.class.getMethod("readStringArray"), EventPropertyType.SIMPLE));
        expected.add(new InternalEventPropDescriptor("readStringIndexed", SupportLegacyBean.class.getMethod("readStringIndexed", new Class[]{int.class}), EventPropertyType.INDEXED));
        expected.add(new InternalEventPropDescriptor("readMapByKey", SupportLegacyBean.class.getMethod("readMapByKey", new Class[]{String.class}), EventPropertyType.MAPPED));
        expected.add(new InternalEventPropDescriptor("readMap", SupportLegacyBean.class.getMethod("readMap"), EventPropertyType.SIMPLE));
        expected.add(new InternalEventPropDescriptor("readLegacyNested", SupportLegacyBean.class.getMethod("readLegacyNested"), EventPropertyType.SIMPLE));

        expected.add(new InternalEventPropDescriptor("x", SupportLegacyBean.class.getField("fieldNested"), EventPropertyType.SIMPLE));
        expected.add(new InternalEventPropDescriptor("y", SupportLegacyBean.class.getMethod("readLegacyBeanVal"), EventPropertyType.SIMPLE));
        EPAssertionUtil.assertEqualsAnyOrder(expected.toArray(), descList.toArray());
    }

    private final static Logger log = LoggerFactory.getLogger(TestPropertyListBuilderPublic.class);
}
