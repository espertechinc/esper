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
package com.espertech.esper.regressionrun.suite.epl;

import com.espertech.esper.common.client.configuration.Configuration;
import com.espertech.esper.common.client.configuration.common.ConfigurationCommon;
import com.espertech.esper.common.client.configuration.common.ConfigurationCommonMethodRef;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.common.internal.support.SupportBean_S0;
import com.espertech.esper.common.internal.support.SupportBean_S1;
import com.espertech.esper.common.internal.support.SupportBean_S2;
import com.espertech.esper.regressionlib.suite.epl.fromclausemethod.*;
import com.espertech.esper.regressionlib.support.bean.*;
import com.espertech.esper.regressionlib.support.epl.SupportJoinMethods;
import com.espertech.esper.regressionlib.support.epl.SupportStaticMethodInvocations;
import com.espertech.esper.regressionrun.runner.RegressionRunner;
import com.espertech.esper.regressionrun.runner.RegressionSession;
import junit.framework.TestCase;

public class TestSuiteEPLFromClauseMethodWConfig extends TestCase {

    public void testEPLFromClauseMethodCacheExpiry() {
        RegressionSession session = RegressionRunner.session();

        ConfigurationCommonMethodRef methodConfig = new ConfigurationCommonMethodRef();
        methodConfig.setExpiryTimeCache(1, 10);
        session.getConfiguration().getCommon().addMethodRef(SupportStaticMethodInvocations.class.getName(), methodConfig);
        session.getConfiguration().getCommon().addImport(SupportStaticMethodInvocations.class.getPackage().getName() + ".*");
        session.getConfiguration().getCommon().addEventType(SupportBean.class);

        RegressionRunner.run(session, new EPLFromClauseMethodCacheExpiry());

        session.destroy();
    }

    public void testEPLFromClauseMethodCacheLRU() {
        RegressionSession session = RegressionRunner.session();

        ConfigurationCommonMethodRef methodConfig = new ConfigurationCommonMethodRef();
        methodConfig.setLRUCache(3);
        session.getConfiguration().getCommon().addMethodRef(SupportStaticMethodInvocations.class.getName(), methodConfig);
        session.getConfiguration().getCommon().addImport(SupportStaticMethodInvocations.class.getPackage().getName() + ".*");
        session.getConfiguration().getCommon().addEventType(SupportBean.class);

        RegressionRunner.run(session, new EPLFromClauseMethodCacheLRU());

        session.destroy();
    }

    public void testEPLFromClauseMethodJoinPerformance() {
        RegressionSession session = RegressionRunner.session();

        ConfigurationCommonMethodRef configMethod = new ConfigurationCommonMethodRef();
        configMethod.setLRUCache(10);
        session.getConfiguration().getCommon().addMethodRef(SupportJoinMethods.class.getName(), configMethod);
        session.getConfiguration().getCommon().addEventType(SupportBeanInt.class);
        session.getConfiguration().getCommon().addImport(SupportJoinMethods.class);

        RegressionRunner.run(session, EPLFromClauseMethodJoinPerformance.executions());
        session.destroy();
    }

    public void testEPLFromClauseMethodVariable() {
        RegressionSession session = RegressionRunner.session();

        Configuration configuration = session.getConfiguration();
        configuration.getCommon().addMethodRef(EPLFromClauseMethodVariable.MyStaticService.class, new ConfigurationCommonMethodRef());

        configuration.getCommon().addImport(EPLFromClauseMethodVariable.MyStaticService.class);
        configuration.getCommon().addImport(EPLFromClauseMethodVariable.MyNonConstantServiceVariableFactory.class);
        configuration.getCommon().addImport(EPLFromClauseMethodVariable.MyNonConstantServiceVariable.class);

        ConfigurationCommon common = configuration.getCommon();
        common.addVariable("MyConstantServiceVariable", EPLFromClauseMethodVariable.MyConstantServiceVariable.class, new EPLFromClauseMethodVariable.MyConstantServiceVariable());
        common.addVariable("MyNonConstantServiceVariable", EPLFromClauseMethodVariable.MyNonConstantServiceVariable.class, new EPLFromClauseMethodVariable.MyNonConstantServiceVariable("postfix"));
        common.addVariable("MyNullMap", EPLFromClauseMethodVariable.MyMethodHandlerMap.class, null);
        common.addVariable("MyMethodHandlerMap", EPLFromClauseMethodVariable.MyMethodHandlerMap.class, new EPLFromClauseMethodVariable.MyMethodHandlerMap("a", "b"));
        common.addVariable("MyMethodHandlerOA", EPLFromClauseMethodVariable.MyMethodHandlerOA.class, new EPLFromClauseMethodVariable.MyMethodHandlerOA("a", "b"));

        configuration.getCommon().getLogging().setEnableQueryPlan(true);
        configuration.getCommon().addEventType(SupportBean.class);
        configuration.getCommon().addEventType(SupportBean_S0.class);
        configuration.getCommon().addEventType(SupportBean_S1.class);
        configuration.getCommon().addEventType(SupportBean_S2.class);

        RegressionRunner.run(session, EPLFromClauseMethodVariable.executions());

        session.destroy();
    }

    public void testEPLFromClauseMethodMultikeyWArray() {
        RegressionSession session = RegressionRunner.session();

        ConfigurationCommonMethodRef methodConfig = new ConfigurationCommonMethodRef();
        methodConfig.setExpiryTimeCache(1, 10);
        session.getConfiguration().getCommon().addMethodRef(EPLFromClauseMethodMultikeyWArray.SupportJoinResultIsArray.class.getName(), methodConfig);
        session.getConfiguration().getCommon().addEventType(SupportEventWithManyArray.class);
        session.getConfiguration().getCommon().getLogging().setEnableQueryPlan(true);

        RegressionRunner.run(session, EPLFromClauseMethodMultikeyWArray.executions());

        session.destroy();
    }
}
