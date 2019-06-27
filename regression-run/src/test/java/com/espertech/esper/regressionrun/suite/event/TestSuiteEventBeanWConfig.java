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

import com.espertech.esper.common.client.configuration.common.ConfigurationCommonEventTypeBean;
import com.espertech.esper.common.client.util.AccessorStyle;
import com.espertech.esper.common.client.util.PropertyResolutionStyle;
import com.espertech.esper.regressionlib.suite.event.bean.*;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.regressionlib.support.bean.SupportBeanComplexProps;
import com.espertech.esper.regressionlib.support.bean.SupportBeanDupProperty;
import com.espertech.esper.regressionlib.support.bean.SupportLegacyBean;
import com.espertech.esper.regressionrun.runner.RegressionRunner;
import com.espertech.esper.regressionrun.runner.RegressionSession;
import junit.framework.TestCase;

public class TestSuiteEventBeanWConfig extends TestCase {

    public void testEventBeanPropertyResolutionAccessorStyleGlobalPublic() {
        RegressionSession session = RegressionRunner.session();
        session.getConfiguration().getCommon().getEventMeta().setDefaultAccessorStyle(AccessorStyle.PUBLIC);
        session.getConfiguration().getCommon().addEventType(SupportLegacyBean.class);
        RegressionRunner.run(session, new EventBeanPropertyResolutionAccessorStyleGlobalPublic());
        session.destroy();
    }

    public void testEventBeanPropertyResolutionCaseDistinctInsensitive() {
        RegressionSession session = RegressionRunner.session();
        session.getConfiguration().getCommon().getEventMeta().setClassPropertyResolutionStyle(PropertyResolutionStyle.DISTINCT_CASE_INSENSITIVE);
        session.getConfiguration().getCommon().addEventType(SupportBeanDupProperty.class);
        RegressionRunner.run(session, new EventBeanPropertyResolutionCaseDistinctInsensitive());
        session.destroy();
    }

    public void testEventBeanPropertyResolutionCaseInsensitive() {
        RegressionSession session = RegressionRunner.session();
        session.getConfiguration().getCommon().getEventMeta().setClassPropertyResolutionStyle(PropertyResolutionStyle.CASE_INSENSITIVE);
        session.getConfiguration().getCommon().addEventType(SupportBeanDupProperty.class);
        session.getConfiguration().getCommon().addEventType(SupportBeanComplexProps.class);
        RegressionRunner.run(session, new EventBeanPropertyResolutionCaseInsensitive());
        session.destroy();
    }

    public void testEventBeanPropertyResolutionCaseInsensitiveEngineDefault() {
        RegressionSession session = RegressionRunner.session();
        session.getConfiguration().getCommon().getEventMeta().setClassPropertyResolutionStyle(PropertyResolutionStyle.CASE_INSENSITIVE);
        session.getConfiguration().getCommon().addEventType("BeanWCIED", SupportBean.class);
        RegressionRunner.run(session, new EventBeanPropertyResolutionCaseInsensitiveEngineDefault());
        session.destroy();
    }

    public void testEventBeanPublicFields() {
        RegressionSession session = RegressionRunner.session();

        ConfigurationCommonEventTypeBean legacyDef = new ConfigurationCommonEventTypeBean();
        legacyDef.setAccessorStyle(AccessorStyle.PUBLIC);
        session.getConfiguration().getCommon().addEventType("MyLegacyNestedEvent", SupportLegacyBean.LegacyNested.class.getName(), legacyDef);

        ConfigurationCommonEventTypeBean anotherLegacyEvent = new ConfigurationCommonEventTypeBean();
        anotherLegacyEvent.setAccessorStyle(AccessorStyle.PUBLIC);
        anotherLegacyEvent.addFieldProperty("explicitFSimple", "fieldLegacyVal");
        anotherLegacyEvent.addFieldProperty("explicitFIndexed", "fieldStringArray");
        anotherLegacyEvent.addFieldProperty("explicitFNested", "fieldNested");
        anotherLegacyEvent.addMethodProperty("explicitMSimple", "readLegacyBeanVal");
        anotherLegacyEvent.addMethodProperty("explicitMArray", "readStringArray");
        anotherLegacyEvent.addMethodProperty("explicitMIndexed", "readStringIndexed");
        anotherLegacyEvent.addMethodProperty("explicitMMapped", "readMapByKey");
        session.getConfiguration().getCommon().addEventType("AnotherLegacyEvent", SupportLegacyBean.class.getName(), anotherLegacyEvent);

        RegressionRunner.run(session, new EventBeanPublicAccessors());
        session.destroy();
    }

    public void testEventBeanPropertyResolutionCaseInsensitiveConfigureType() {
        RegressionSession session = RegressionRunner.session();

        ConfigurationCommonEventTypeBean beanWithCaseInsensitive = new ConfigurationCommonEventTypeBean();
        beanWithCaseInsensitive.setPropertyResolutionStyle(PropertyResolutionStyle.CASE_INSENSITIVE);
        session.getConfiguration().getCommon().addEventType("BeanWithCaseInsensitive", SupportBean.class.getName(), beanWithCaseInsensitive);

        RegressionRunner.run(session, new EventBeanPropertyResolutionCaseInsensitiveConfigureType());
        session.destroy();
    }

    public void testEventBeanExplicitOnly() {
        RegressionSession session = RegressionRunner.session();

        ConfigurationCommonEventTypeBean legacyDef = new ConfigurationCommonEventTypeBean();
        legacyDef.setAccessorStyle(AccessorStyle.EXPLICIT);
        legacyDef.addFieldProperty("explicitFNested", "fieldNested");
        legacyDef.addMethodProperty("explicitMNested", "readLegacyNested");
        session.getConfiguration().getCommon().addEventType("MyLegacyEvent", SupportLegacyBean.class.getName(), legacyDef);

        legacyDef = new ConfigurationCommonEventTypeBean();
        legacyDef.setAccessorStyle(AccessorStyle.EXPLICIT);
        legacyDef.addFieldProperty("fieldNestedClassValue", "fieldNestedValue");
        legacyDef.addMethodProperty("readNestedClassValue", "readNestedValue");
        session.getConfiguration().getCommon().addEventType("MyLegacyNestedEvent", SupportLegacyBean.LegacyNested.class.getName(), legacyDef);

        ConfigurationCommonEventTypeBean mySupportBean = new ConfigurationCommonEventTypeBean();
        mySupportBean.setAccessorStyle(AccessorStyle.EXPLICIT);
        session.getConfiguration().getCommon().addEventType("MySupportBean", SupportBean.class.getName(), mySupportBean);

        RegressionRunner.run(session, new EventBeanExplicitOnly());
        session.destroy();
    }
}
