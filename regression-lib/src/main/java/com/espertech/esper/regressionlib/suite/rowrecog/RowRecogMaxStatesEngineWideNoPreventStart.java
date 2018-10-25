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
import com.espertech.esper.common.client.hook.condition.ConditionHandlerFactoryContext;
import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecutionWithConfigure;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.regressionlib.support.client.SupportConditionHandlerFactory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class RowRecogMaxStatesEngineWideNoPreventStart implements RegressionExecutionWithConfigure {
    private SupportConditionHandlerFactory.SupportConditionHandler handler;

    @Override
    public boolean enableHATest() {
        return false;
    }

    public void configure(Configuration configuration) {

    }

    public void run(RegressionEnvironment env) {
        ConditionHandlerFactoryContext conditionHandlerFactoryContext = SupportConditionHandlerFactory.getFactoryContexts().get(0);
        assertEquals(conditionHandlerFactoryContext.getRuntimeURI(), env.runtimeURI());
        handler = SupportConditionHandlerFactory.getLastHandler();

        String[] fields = "c0".split(",");

        String epl = "@name('s0') select * from SupportBean " +
            "match_recognize (" +
            "  partition by theString " +
            "  measures P1.theString as c0" +
            "  pattern (P1 P2) " +
            "  define " +
            "    P1 as P1.intPrimitive = 1," +
            "    P2 as P2.intPrimitive = 2" +
            ")";

        env.compileDeploy(epl).addListener("s0");

        env.sendEventBean(new SupportBean("A", 1));
        env.sendEventBean(new SupportBean("B", 1));
        env.sendEventBean(new SupportBean("C", 1));
        assertTrue(handler.getContexts().isEmpty());

        // overflow
        env.sendEventBean(new SupportBean("D", 1));
        RowRecogMaxStatesEngineWide3Instance.assertContextEnginePool(env, env.statement("s0"), handler.getAndResetContexts(), 3, RowRecogMaxStatesEngineWide3Instance.getExpectedCountMap(env, "s0", 3));
        env.sendEventBean(new SupportBean("E", 1));
        RowRecogMaxStatesEngineWide3Instance.assertContextEnginePool(env, env.statement("s0"), handler.getAndResetContexts(), 3, RowRecogMaxStatesEngineWide3Instance.getExpectedCountMap(env, "s0", 4));

        env.sendEventBean(new SupportBean("D", 2));    // D gone
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"D"});

        env.sendEventBean(new SupportBean("A", 2));    // A gone
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"A"});

        env.sendEventBean(new SupportBean("C", 2));    // C gone
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"C"});

        env.sendEventBean(new SupportBean("F", 1));
        assertTrue(handler.getContexts().isEmpty());

        env.sendEventBean(new SupportBean("G", 1));
        RowRecogMaxStatesEngineWide3Instance.assertContextEnginePool(env, env.statement("s0"), handler.getAndResetContexts(), 3, RowRecogMaxStatesEngineWide3Instance.getExpectedCountMap(env, "s0", 3));

        env.undeployAll();
    }
}
