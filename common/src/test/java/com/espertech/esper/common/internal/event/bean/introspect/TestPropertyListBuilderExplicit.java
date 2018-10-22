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

import com.espertech.esper.common.client.configuration.ConfigurationException;
import com.espertech.esper.common.client.configuration.common.ConfigurationCommonEventTypeBean;
import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.common.internal.event.bean.core.PropertyStem;
import com.espertech.esper.common.internal.event.core.EventPropertyType;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.common.internal.supportunit.bean.SupportLegacyBean;
import junit.framework.TestCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedList;
import java.util.List;

public class TestPropertyListBuilderExplicit extends TestCase {
    private PropertyListBuilderExplicit builder;

    public void setUp() {
        ConfigurationCommonEventTypeBean config = new ConfigurationCommonEventTypeBean();
        config.addFieldProperty("f_legVal", "fieldLegacyVal");
        config.addFieldProperty("f_strArr", "fieldStringArray");
        config.addFieldProperty("f_strMap", "fieldMapped");
        config.addFieldProperty("f_legNested", "fieldNested");

        config.addMethodProperty("m_legVal", "readLegacyBeanVal");
        config.addMethodProperty("m_strArr", "readStringArray");
        config.addMethodProperty("m_strInd", "readStringIndexed");
        config.addMethodProperty("m_strMapKeyed", "readMapByKey");
        config.addMethodProperty("m_strMap", "readMap");
        config.addMethodProperty("m_legNested", "readLegacyNested");

        builder = new PropertyListBuilderExplicit(config);
    }

    public void testBuildPropList() throws Exception {
        List<PropertyStem> descList = builder.assessProperties(SupportLegacyBean.class);

        List<PropertyStem> expected = new LinkedList<PropertyStem>();
        expected.add(new PropertyStem("f_legVal", SupportLegacyBean.class.getField("fieldLegacyVal"), EventPropertyType.SIMPLE));
        expected.add(new PropertyStem("f_strArr", SupportLegacyBean.class.getField("fieldStringArray"), EventPropertyType.SIMPLE));
        expected.add(new PropertyStem("f_strMap", SupportLegacyBean.class.getField("fieldMapped"), EventPropertyType.SIMPLE));
        expected.add(new PropertyStem("f_legNested", SupportLegacyBean.class.getField("fieldNested"), EventPropertyType.SIMPLE));

        expected.add(new PropertyStem("m_legVal", SupportLegacyBean.class.getMethod("readLegacyBeanVal"), EventPropertyType.SIMPLE));
        expected.add(new PropertyStem("m_strArr", SupportLegacyBean.class.getMethod("readStringArray"), EventPropertyType.SIMPLE));
        expected.add(new PropertyStem("m_strInd", SupportLegacyBean.class.getMethod("readStringIndexed", new Class[]{int.class}), EventPropertyType.INDEXED));
        expected.add(new PropertyStem("m_strMapKeyed", SupportLegacyBean.class.getMethod("readMapByKey", new Class[]{String.class}), EventPropertyType.MAPPED));
        expected.add(new PropertyStem("m_strMap", SupportLegacyBean.class.getMethod("readMap"), EventPropertyType.SIMPLE));
        expected.add(new PropertyStem("m_legNested", SupportLegacyBean.class.getMethod("readLegacyNested"), EventPropertyType.SIMPLE));

        EPAssertionUtil.assertEqualsAnyOrder(expected.toArray(), descList.toArray());
    }

    public void testInvalid() {
        tryInvalidField("x", SupportBean.class);
        tryInvalidField("intPrimitive", SupportBean.class);

        tryInvalidMethod("x", SupportBean.class);
        tryInvalidMethod("intPrimitive", SupportBean.class);
    }

    private void tryInvalidMethod(String methodName, Class clazz) {
        ConfigurationCommonEventTypeBean config = new ConfigurationCommonEventTypeBean();
        config.addMethodProperty("name", methodName);
        builder = new PropertyListBuilderExplicit(config);

        try {
            builder.assessProperties(clazz);
        } catch (ConfigurationException ex) {
            // expected
            log.debug(ex.getMessage());
        }
    }

    private void tryInvalidField(String fieldName, Class clazz) {
        ConfigurationCommonEventTypeBean config = new ConfigurationCommonEventTypeBean();
        config.addFieldProperty("name", fieldName);
        builder = new PropertyListBuilderExplicit(config);

        try {
            builder.assessProperties(clazz);
        } catch (ConfigurationException ex) {
            // expected
            log.debug(ex.getMessage());
        }
    }

    private final static Logger log = LoggerFactory.getLogger(TestPropertyListBuilderExplicit.class);
}
