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
package com.espertech.esper.regression.rowrecog;

import com.espertech.esper.client.Configuration;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.hook.ConditionHandlerFactoryContext;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.bean.SupportBeanConstants;
import com.espertech.esper.supportregression.bean.SupportBean_S0;
import com.espertech.esper.supportregression.bean.SupportBean_S1;
import com.espertech.esper.supportregression.client.SupportConditionHandlerFactory;
import com.espertech.esper.supportregression.execution.RegressionExecution;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ExecRowRecogMaxStatesEngineWideNoPreventStart implements RegressionExecution, SupportBeanConstants {
    private SupportConditionHandlerFactory.SupportConditionHandler handler;

    public void configure(Configuration configuration) throws Exception {
        configuration.addEventType(SupportBean.class);
        configuration.addEventType(SupportBean_S0.class);
        configuration.addEventType(SupportBean_S1.class);
        configuration.getEngineDefaults().getConditionHandling().addClass(SupportConditionHandlerFactory.class);
        configuration.getEngineDefaults().getMatchRecognize().setMaxStates(3L);
        configuration.getEngineDefaults().getMatchRecognize().setMaxStatesPreventStart(false);
        configuration.getEngineDefaults().getLogging().setEnableExecutionDebug(true);
    }

    public void run(EPServiceProvider epService) throws Exception {
        ConditionHandlerFactoryContext conditionHandlerFactoryContext = SupportConditionHandlerFactory.getFactoryContexts().get(0);
        assertEquals(conditionHandlerFactoryContext.getEngineURI(), epService.getURI());
        handler = SupportConditionHandlerFactory.getLastHandler();

        String[] fields = "c0".split(",");

        String epl = "@name('S1') select * from SupportBean " +
                "match_recognize (" +
                "  partition by theString " +
                "  measures P1.theString as c0" +
                "  pattern (P1 P2) " +
                "  define " +
                "    P1 as P1.intPrimitive = 1," +
                "    P2 as P2.intPrimitive = 2" +
                ")";

        SupportUpdateListener listener = new SupportUpdateListener();
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean("A", 1));
        epService.getEPRuntime().sendEvent(new SupportBean("B", 1));
        epService.getEPRuntime().sendEvent(new SupportBean("C", 1));
        assertTrue(handler.getContexts().isEmpty());

        // overflow
        epService.getEPRuntime().sendEvent(new SupportBean("D", 1));
        ExecRowRecogMaxStatesEngineWide3Instance.assertContextEnginePool(epService, stmt, handler.getAndResetContexts(), 3, ExecRowRecogMaxStatesEngineWide3Instance.getExpectedCountMap("S1", 3));
        epService.getEPRuntime().sendEvent(new SupportBean("E", 1));
        ExecRowRecogMaxStatesEngineWide3Instance.assertContextEnginePool(epService, stmt, handler.getAndResetContexts(), 3, ExecRowRecogMaxStatesEngineWide3Instance.getExpectedCountMap("S1", 4));

        epService.getEPRuntime().sendEvent(new SupportBean("D", 2));    // D gone
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"D"});

        epService.getEPRuntime().sendEvent(new SupportBean("A", 2));    // A gone
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"A"});

        epService.getEPRuntime().sendEvent(new SupportBean("C", 2));    // C gone
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"C"});

        epService.getEPRuntime().sendEvent(new SupportBean("F", 1));
        assertTrue(handler.getContexts().isEmpty());

        epService.getEPRuntime().sendEvent(new SupportBean("G", 1));
        ExecRowRecogMaxStatesEngineWide3Instance.assertContextEnginePool(epService, stmt, handler.getAndResetContexts(), 3, ExecRowRecogMaxStatesEngineWide3Instance.getExpectedCountMap("S1", 3));

        epService.getEPAdministrator().getConfiguration().setMatchRecognizeMaxStates(4L);

        epService.getEPRuntime().sendEvent(new SupportBean("G", 1));
        assertTrue(handler.getContexts().isEmpty());

        epService.getEPRuntime().sendEvent(new SupportBean("H", 1));
        ExecRowRecogMaxStatesEngineWide3Instance.assertContextEnginePool(epService, stmt, handler.getAndResetContexts(), 4, ExecRowRecogMaxStatesEngineWide3Instance.getExpectedCountMap("S1", 4));

        epService.getEPAdministrator().getConfiguration().setMatchRecognizeMaxStates(null);

        epService.getEPRuntime().sendEvent(new SupportBean("I", 1));
        assertTrue(handler.getContexts().isEmpty());
    }
}
