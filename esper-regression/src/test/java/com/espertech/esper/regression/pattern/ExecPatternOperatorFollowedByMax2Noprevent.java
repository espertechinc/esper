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
package com.espertech.esper.regression.pattern;

import com.espertech.esper.client.Configuration;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.bean.SupportBeanConstants;
import com.espertech.esper.supportregression.bean.SupportBean_A;
import com.espertech.esper.supportregression.bean.SupportBean_B;
import com.espertech.esper.supportregression.client.SupportConditionHandlerFactory;
import com.espertech.esper.supportregression.client.SupportConfigFactory;
import com.espertech.esper.supportregression.execution.RegressionExecution;

import static com.espertech.esper.regression.pattern.ExecPatternOperatorFollowedByMax4Prevent.assertContextEnginePool;
import static com.espertech.esper.regression.pattern.ExecPatternOperatorFollowedByMax4Prevent.getExpectedCountMap;

public class ExecPatternOperatorFollowedByMax2Noprevent implements RegressionExecution, SupportBeanConstants {
    public void configure(Configuration configuration) throws Exception {
        configuration.addEventType("SupportBean", SupportBean.class);
        configuration.addEventType("SupportBean_A", SupportBean_A.class);
        configuration.addEventType("SupportBean_B", SupportBean_B.class);
        configuration.getEngineDefaults().getConditionHandling().addClass(SupportConditionHandlerFactory.class);
        configuration.getEngineDefaults().getPatterns().setMaxSubexpressions(2L);
        configuration.getEngineDefaults().getPatterns().setMaxSubexpressionPreventStart(false);
        configuration.getEngineDefaults().getLogging().setEnableExecutionDebug(true);
    }

    public void run(EPServiceProvider epService) throws Exception {
        if (SupportConfigFactory.skipTest(ExecPatternOperatorFollowedByMax2Noprevent.class)) {
            return;
        }
        SupportConditionHandlerFactory.SupportConditionHandler handler = SupportConditionHandlerFactory.getLastHandler();

        String expression = "@Name('A') select a.id as a, b.id as b from pattern [every a=SupportBean_A -> b=SupportBean_B]";
        EPStatement stmt = epService.getEPAdministrator().createEPL(expression);

        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean_A("A1"));
        epService.getEPRuntime().sendEvent(new SupportBean_A("A2"));

        handler.getContexts().clear();
        epService.getEPRuntime().sendEvent(new SupportBean_A("A3"));
        assertContextEnginePool(epService, stmt, handler.getContexts(), 2, getExpectedCountMap("A", 2));

        handler.getContexts().clear();
        epService.getEPRuntime().sendEvent(new SupportBean_A("A4"));
        assertContextEnginePool(epService, stmt, handler.getContexts(), 2, getExpectedCountMap("A", 3));

        String[] fields = new String[]{"a", "b"};
        epService.getEPRuntime().sendEvent(new SupportBean_B("B1"));
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields, new Object[][]{{"A1", "B1"}, {"A2", "B1"}, {"A3", "B1"}, {"A4", "B1"}});

        // set new max
        epService.getEPAdministrator().getConfiguration().setPatternMaxSubexpressions(1L);

        epService.getEPRuntime().sendEvent(new SupportBean_A("A5"));

        handler.getContexts().clear();
        epService.getEPRuntime().sendEvent(new SupportBean_A("A6"));
        assertContextEnginePool(epService, stmt, handler.getContexts(), 1, getExpectedCountMap("A", 1));

        stmt.destroy();
    }
}
