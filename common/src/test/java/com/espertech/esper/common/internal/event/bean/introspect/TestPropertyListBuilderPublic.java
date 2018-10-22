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

public class TestPropertyListBuilderPublic extends TestCase {
    private PropertyListBuilderPublic builder;

    public void setUp() {
        ConfigurationCommonEventTypeBean config = new ConfigurationCommonEventTypeBean();

        // add 2 explicit properties, also supported
        config.addFieldProperty("x", "fieldNested");
        config.addMethodProperty("y", "readLegacyBeanVal");

        builder = new PropertyListBuilderPublic(config);
    }

    public void testBuildPropList() throws Exception {
        List<PropertyStem> descList = builder.assessProperties(SupportLegacyBean.class);

        List<PropertyStem> expected = new LinkedList<PropertyStem>();
        expected.add(new PropertyStem("fieldLegacyVal", SupportLegacyBean.class.getField("fieldLegacyVal"), EventPropertyType.SIMPLE));
        expected.add(new PropertyStem("fieldStringArray", SupportLegacyBean.class.getField("fieldStringArray"), EventPropertyType.SIMPLE));
        expected.add(new PropertyStem("fieldMapped", SupportLegacyBean.class.getField("fieldMapped"), EventPropertyType.SIMPLE));
        expected.add(new PropertyStem("fieldNested", SupportLegacyBean.class.getField("fieldNested"), EventPropertyType.SIMPLE));

        expected.add(new PropertyStem("readLegacyBeanVal", SupportLegacyBean.class.getMethod("readLegacyBeanVal"), EventPropertyType.SIMPLE));
        expected.add(new PropertyStem("readStringArray", SupportLegacyBean.class.getMethod("readStringArray"), EventPropertyType.SIMPLE));
        expected.add(new PropertyStem("readStringIndexed", SupportLegacyBean.class.getMethod("readStringIndexed", new Class[]{int.class}), EventPropertyType.INDEXED));
        expected.add(new PropertyStem("readMapByKey", SupportLegacyBean.class.getMethod("readMapByKey", new Class[]{String.class}), EventPropertyType.MAPPED));
        expected.add(new PropertyStem("readMap", SupportLegacyBean.class.getMethod("readMap"), EventPropertyType.SIMPLE));
        expected.add(new PropertyStem("readLegacyNested", SupportLegacyBean.class.getMethod("readLegacyNested"), EventPropertyType.SIMPLE));

        expected.add(new PropertyStem("x", SupportLegacyBean.class.getField("fieldNested"), EventPropertyType.SIMPLE));
        expected.add(new PropertyStem("y", SupportLegacyBean.class.getMethod("readLegacyBeanVal"), EventPropertyType.SIMPLE));
        EPAssertionUtil.assertEqualsAnyOrder(expected.toArray(), descList.toArray());
    }

    private final static Logger log = LoggerFactory.getLogger(TestPropertyListBuilderPublic.class);
}
