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
package com.espertech.esper.regressionrun.suite.expr;

import com.espertech.esper.common.client.configuration.common.ConfigurationCommonEventTypeBean;
import com.espertech.esper.regressionlib.suite.expr.datetime.ExprDTResolution;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.regressionlib.support.bean.SupportDateTime;
import com.espertech.esper.regressionrun.runner.RegressionRunner;
import com.espertech.esper.regressionrun.runner.RegressionSession;
import com.espertech.esper.regressionrun.runner.SupportConfigFactory;
import junit.framework.TestCase;

import java.util.concurrent.TimeUnit;

import static com.espertech.esper.regressionlib.framework.SupportMessageAssertUtil.tryInvalidConfigurationCompileAndRuntime;
import static com.espertech.esper.regressionrun.suite.expr.TestSuiteExprDateTime.addIdStsEtsEvent;

public class TestSuiteExprDateTimeWConfig extends TestCase {

    public void testExprDTMicrosecondResolution() {
        RegressionSession session = RegressionRunner.session();
        session.getConfiguration().getCommon().addEventType(SupportDateTime.class);
        session.getConfiguration().getCommon().getTimeSource().setTimeUnit(TimeUnit.MICROSECONDS);
        addIdStsEtsEvent(session.getConfiguration());
        RegressionRunner.run(session, ExprDTResolution.executions(true));
        session.destroy();
    }

    public void testInvalidConfigure() {
        ConfigurationCommonEventTypeBean configBean = new ConfigurationCommonEventTypeBean();

        configBean.setStartTimestampPropertyName(null);
        configBean.setEndTimestampPropertyName("caldate");
        tryInvalidConfig(SupportDateTime.class, configBean, "Declared end timestamp property requires that a start timestamp property is also declared");

        configBean.setStartTimestampPropertyName("xyz");
        configBean.setEndTimestampPropertyName(null);
        tryInvalidConfig(SupportBean.class, configBean, "Declared start timestamp property name 'xyz' was not found");

        configBean.setStartTimestampPropertyName("longPrimitive");
        configBean.setEndTimestampPropertyName("xyz");
        tryInvalidConfig(SupportBean.class, configBean, "Declared end timestamp property name 'xyz' was not found");

        configBean.setEndTimestampPropertyName(null);
        configBean.setStartTimestampPropertyName("theString");
        tryInvalidConfig(SupportBean.class, configBean, "Declared start timestamp property 'theString' is expected to return a Date, Calendar or long-typed value but returns 'java.lang.String'");

        configBean.setStartTimestampPropertyName("longPrimitive");
        configBean.setEndTimestampPropertyName("theString");
        tryInvalidConfig(SupportBean.class, configBean, "Declared end timestamp property 'theString' is expected to return a Date, Calendar or long-typed value but returns 'java.lang.String'");

        configBean.setStartTimestampPropertyName("longdate");
        configBean.setEndTimestampPropertyName("caldate");
        tryInvalidConfig(SupportDateTime.class, configBean, "Declared end timestamp property 'caldate' is expected to have the same property type as the start-timestamp property 'longdate'");
    }

    private void tryInvalidConfig(Class beanEventClass, ConfigurationCommonEventTypeBean configBean, String expected) {
        tryInvalidConfigurationCompileAndRuntime(SupportConfigFactory.getConfiguration(),
            config -> config.getCommon().addEventType(beanEventClass.getName(), beanEventClass.getName(), configBean),
            expected);

    }
}
