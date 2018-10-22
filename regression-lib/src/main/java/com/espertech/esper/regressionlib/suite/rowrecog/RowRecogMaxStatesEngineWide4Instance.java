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
package com.espertech.esper.regressionlib.suite.rowrecog;

import com.espertech.esper.common.client.configuration.Configuration;
import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecutionWithConfigure;
import com.espertech.esper.regressionlib.support.client.SupportConditionHandlerFactory;

import static org.junit.Assert.assertTrue;

public class RowRecogMaxStatesEngineWide4Instance implements RegressionExecutionWithConfigure {
    private SupportConditionHandlerFactory.SupportConditionHandler handler;

    public void configure(Configuration configuration) {
    }

    @Override
    public boolean enableHATest() {
        return false;
    }

    public void run(RegressionEnvironment env) {
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
        env.compileDeploy(eplOne).addListener("S1");

        String eplTwo = "@name('S2') select * from SupportBean(theString = 'B')#length(2) " +
            "match_recognize (" +
            "  partition by intPrimitive " +
            "  measures P2.intPrimitive as c0" +
            "  pattern (P1 P2) " +
            "  define " +
            "    P1 as P1.longPrimitive = 1," +
            "    P2 as P2.longPrimitive = 2" +
            ")";
        env.compileDeploy(eplTwo).addListener("S2");

        env.sendEventBean(RowRecogMaxStatesEngineWide3Instance.makeBean("A", 100, 1));
        env.sendEventBean(RowRecogMaxStatesEngineWide3Instance.makeBean("A", 200, 1));
        env.sendEventBean(RowRecogMaxStatesEngineWide3Instance.makeBean("B", 100, 1));
        env.sendEventBean(RowRecogMaxStatesEngineWide3Instance.makeBean("B", 200, 1));
        env.sendEventBean(RowRecogMaxStatesEngineWide3Instance.makeBean("B", 300, 1));
        env.sendEventBean(RowRecogMaxStatesEngineWide3Instance.makeBean("B", 400, 1));
        EPAssertionUtil.iteratorToArray(env.statement("S2").iterator());
        assertTrue(handler.getContexts().isEmpty());

        // overflow
        env.sendEventBean(RowRecogMaxStatesEngineWide3Instance.makeBean("A", 300, 1));
        RowRecogMaxStatesEngineWide3Instance.assertContextEnginePool(env, env.statement("S1"), handler.getAndResetContexts(), 4, RowRecogMaxStatesEngineWide3Instance.getExpectedCountMap(env, "S1", 2, "S2", 2));

        // terminate B
        env.sendEventBean(RowRecogMaxStatesEngineWide3Instance.makeBean("B", 400, 2));
        EPAssertionUtil.assertProps(env.listener("S2").assertOneGetNewAndReset(), fields, new Object[]{400});

        // terminate one of A
        env.sendEventBean(RowRecogMaxStatesEngineWide3Instance.makeBean("A", 100, 2));
        EPAssertionUtil.assertProps(env.listener("S1").assertOneGetNewAndReset(), fields, new Object[]{100});

        // fill up A
        env.sendEventBean(RowRecogMaxStatesEngineWide3Instance.makeBean("A", 300, 1));
        env.sendEventBean(RowRecogMaxStatesEngineWide3Instance.makeBean("A", 400, 1));
        env.sendEventBean(RowRecogMaxStatesEngineWide3Instance.makeBean("A", 500, 1));
        assertTrue(handler.getContexts().isEmpty());

        // overflow
        env.sendEventBean(RowRecogMaxStatesEngineWide3Instance.makeBean("B", 500, 1));
        RowRecogMaxStatesEngineWide3Instance.assertContextEnginePool(env, env.statement("S2"), handler.getAndResetContexts(), 4, RowRecogMaxStatesEngineWide3Instance.getExpectedCountMap(env, "S1", 4, "S2", 0));

        // destroy statement-1 freeing up all "A"
        env.undeployModuleContaining("S1");

        // any number of B doesn't trigger overflow because of data window
        env.sendEventBean(RowRecogMaxStatesEngineWide3Instance.makeBean("B", 600, 1));
        env.sendEventBean(RowRecogMaxStatesEngineWide3Instance.makeBean("B", 700, 1));
        env.sendEventBean(RowRecogMaxStatesEngineWide3Instance.makeBean("B", 800, 1));
        env.sendEventBean(RowRecogMaxStatesEngineWide3Instance.makeBean("B", 900, 1));
        assertTrue(handler.getContexts().isEmpty());
    }
}
