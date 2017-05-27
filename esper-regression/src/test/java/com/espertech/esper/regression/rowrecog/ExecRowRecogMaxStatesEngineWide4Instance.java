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
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.bean.SupportBeanConstants;
import com.espertech.esper.supportregression.bean.SupportBean_S0;
import com.espertech.esper.supportregression.bean.SupportBean_S1;
import com.espertech.esper.supportregression.client.SupportConditionHandlerFactory;
import com.espertech.esper.supportregression.execution.RegressionExecution;

import static org.junit.Assert.assertTrue;

public class ExecRowRecogMaxStatesEngineWide4Instance implements RegressionExecution, SupportBeanConstants {
    private SupportConditionHandlerFactory.SupportConditionHandler handler;

    public void configure(Configuration configuration) throws Exception {
        configuration.addEventType(SupportBean.class);
        configuration.addEventType(SupportBean_S0.class);
        configuration.addEventType(SupportBean_S1.class);
        configuration.getEngineDefaults().getConditionHandling().addClass(SupportConditionHandlerFactory.class);
        configuration.getEngineDefaults().getMatchRecognize().setMaxStates(4L);
        configuration.getEngineDefaults().getMatchRecognize().setMaxStatesPreventStart(true);
        configuration.getEngineDefaults().getLogging().setEnableExecutionDebug(true);
    }

    public void run(EPServiceProvider epService) throws Exception {
        handler = SupportConditionHandlerFactory.getLastHandler();
        String[] fields = "c0".split(",");

        String eplOne = "@name('S1') select * from SupportBean(theString = 'A') " +
                "match_recognize (" +
                "  partition by intPrimitive " +
                "  measures P2.intPrimitive as c0" +
                "  pattern (P1 P2) " +
                "  define " +
                "    P1 as P1.longPrimitive = 1," +
                "    P2 as P2.longPrimitive = 2" +
                ")";
        EPStatement stmtOne = epService.getEPAdministrator().createEPL(eplOne);
        SupportUpdateListener listenerOne = new SupportUpdateListener();
        stmtOne.addListener(listenerOne);

        String eplTwo = "@name('S2') select * from SupportBean(theString = 'B')#length(2) " +
                "match_recognize (" +
                "  partition by intPrimitive " +
                "  measures P2.intPrimitive as c0" +
                "  pattern (P1 P2) " +
                "  define " +
                "    P1 as P1.longPrimitive = 1," +
                "    P2 as P2.longPrimitive = 2" +
                ")";
        EPStatement stmtTwo = epService.getEPAdministrator().createEPL(eplTwo);
        SupportUpdateListener listenerTwo = new SupportUpdateListener();
        stmtTwo.addListener(listenerTwo);

        epService.getEPRuntime().sendEvent(ExecRowRecogMaxStatesEngineWide3Instance.makeBean("A", 100, 1));
        epService.getEPRuntime().sendEvent(ExecRowRecogMaxStatesEngineWide3Instance.makeBean("A", 200, 1));
        epService.getEPRuntime().sendEvent(ExecRowRecogMaxStatesEngineWide3Instance.makeBean("B", 100, 1));
        epService.getEPRuntime().sendEvent(ExecRowRecogMaxStatesEngineWide3Instance.makeBean("B", 200, 1));
        epService.getEPRuntime().sendEvent(ExecRowRecogMaxStatesEngineWide3Instance.makeBean("B", 300, 1));
        epService.getEPRuntime().sendEvent(ExecRowRecogMaxStatesEngineWide3Instance.makeBean("B", 400, 1));
        EPAssertionUtil.iteratorToArray(stmtTwo.iterator());
        assertTrue(handler.getContexts().isEmpty());

        // overflow
        epService.getEPRuntime().sendEvent(ExecRowRecogMaxStatesEngineWide3Instance.makeBean("A", 300, 1));
        ExecRowRecogMaxStatesEngineWide3Instance.assertContextEnginePool(epService, stmtOne, handler.getAndResetContexts(), 4, ExecRowRecogMaxStatesEngineWide3Instance.getExpectedCountMap("S1", 2, "S2", 2));

        // terminate B
        epService.getEPRuntime().sendEvent(ExecRowRecogMaxStatesEngineWide3Instance.makeBean("B", 400, 2));
        EPAssertionUtil.assertProps(listenerTwo.assertOneGetNewAndReset(), fields, new Object[]{400});

        // terminate one of A
        epService.getEPRuntime().sendEvent(ExecRowRecogMaxStatesEngineWide3Instance.makeBean("A", 100, 2));
        EPAssertionUtil.assertProps(listenerOne.assertOneGetNewAndReset(), fields, new Object[]{100});

        // fill up A
        epService.getEPRuntime().sendEvent(ExecRowRecogMaxStatesEngineWide3Instance.makeBean("A", 300, 1));
        epService.getEPRuntime().sendEvent(ExecRowRecogMaxStatesEngineWide3Instance.makeBean("A", 400, 1));
        epService.getEPRuntime().sendEvent(ExecRowRecogMaxStatesEngineWide3Instance.makeBean("A", 500, 1));
        assertTrue(handler.getContexts().isEmpty());

        // overflow
        epService.getEPRuntime().sendEvent(ExecRowRecogMaxStatesEngineWide3Instance.makeBean("B", 500, 1));
        ExecRowRecogMaxStatesEngineWide3Instance.assertContextEnginePool(epService, stmtTwo, handler.getAndResetContexts(), 4, ExecRowRecogMaxStatesEngineWide3Instance.getExpectedCountMap("S1", 4, "S2", 0));

        // destroy statement-1 freeing up all "A"
        stmtOne.destroy();

        // any number of B doesn't trigger overflow because of data window
        epService.getEPRuntime().sendEvent(ExecRowRecogMaxStatesEngineWide3Instance.makeBean("B", 600, 1));
        epService.getEPRuntime().sendEvent(ExecRowRecogMaxStatesEngineWide3Instance.makeBean("B", 700, 1));
        epService.getEPRuntime().sendEvent(ExecRowRecogMaxStatesEngineWide3Instance.makeBean("B", 800, 1));
        epService.getEPRuntime().sendEvent(ExecRowRecogMaxStatesEngineWide3Instance.makeBean("B", 900, 1));
        assertTrue(handler.getContexts().isEmpty());
    }
}
