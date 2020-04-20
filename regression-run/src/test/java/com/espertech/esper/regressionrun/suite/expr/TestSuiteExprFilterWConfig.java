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

import com.espertech.esper.common.client.configuration.Configuration;
import com.espertech.esper.common.client.util.ThreadingProfile;
import com.espertech.esper.common.internal.filterspec.FilterOperator;
import com.espertech.esper.common.internal.filterspec.FilterSpecParamForge;
import com.espertech.esper.compiler.client.CompilerArguments;
import com.espertech.esper.compiler.client.EPCompileException;
import com.espertech.esper.compiler.client.EPCompilerProvider;
import com.espertech.esper.regressionlib.suite.expr.filter.ExprFilterLargeThreading;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.regressionlib.support.bean.SupportTradeEvent;
import com.espertech.esper.regressionlib.support.util.SupportFilterSpecCompileHook;
import com.espertech.esper.regressionrun.runner.RegressionRunner;
import com.espertech.esper.regressionrun.runner.RegressionSession;
import com.espertech.esper.regressionrun.runner.SupportConfigFactory;
import junit.framework.TestCase;

import static org.junit.Assert.assertEquals;

public class TestSuiteExprFilterWConfig extends TestCase {

    public void testExprFilterLargeThreading() {
        RegressionSession session = RegressionRunner.session();
        session.getConfiguration().getCommon().addEventType(SupportBean.class);
        session.getConfiguration().getCommon().addEventType(SupportTradeEvent.class);
        session.getConfiguration().getCommon().getExecution().setThreadingProfile(ThreadingProfile.LARGE);
        RegressionRunner.run(session, new ExprFilterLargeThreading());
        session.destroy();
    }

    public void testExprFilterAdvancedPlanningDisable() {
        SupportFilterSpecCompileHook.reset();
        String hook = "@Hook(type=HookType.INTERNAL_FILTERSPEC, hook='" + SupportFilterSpecCompileHook.class.getName() + "')";
        String epl = hook + "select * from SupportBean(theString = 'a' || 'b')";
        Configuration configuration = SupportConfigFactory.getConfiguration();
        configuration.getCommon().addEventType(SupportBean.class);
        configuration.getCompiler().getExecution().setFilterServiceAdvancedPlanning(false);
        try {
            EPCompilerProvider.getCompiler().compile(epl, new CompilerArguments(configuration));
        } catch (EPCompileException e) {
            throw new RuntimeException(e);
        }
        FilterSpecParamForge forge = SupportFilterSpecCompileHook.assertSingleAndReset("SupportBean");
        assertEquals(FilterOperator.BOOLEAN_EXPRESSION, forge.getFilterOperator());
    }
}
