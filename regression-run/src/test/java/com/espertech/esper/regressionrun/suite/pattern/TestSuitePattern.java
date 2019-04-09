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
package com.espertech.esper.regressionrun.suite.pattern;

import com.espertech.esper.common.client.configuration.Configuration;
import com.espertech.esper.common.client.configuration.common.ConfigurationCommon;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.common.internal.support.SupportBean_S0;
import com.espertech.esper.regressionlib.suite.pattern.*;
import com.espertech.esper.regressionlib.support.bean.*;
import com.espertech.esper.regressionlib.support.client.SupportConditionHandlerFactory;
import com.espertech.esper.regressionlib.support.epl.SupportStaticMethodLib;
import com.espertech.esper.regressionrun.runner.RegressionRunner;
import com.espertech.esper.regressionrun.runner.RegressionSession;
import junit.framework.TestCase;

public class TestSuitePattern extends TestCase {
    private RegressionSession session;

    public void setUp() {
        session = RegressionRunner.session();
        configure(session.getConfiguration());
    }

    public void tearDown() {
        session.destroy();
        session = null;
    }

    public void testPatternOperatorAnd() {
        RegressionRunner.run(session, PatternOperatorAnd.executions());
    }

    public void testPatternOperatorOr() {
        RegressionRunner.run(session, PatternOperatorOr.executions());
    }

    public void testPatternOperatorNot() {
        RegressionRunner.run(session, PatternOperatorNot.executions());
    }

    public void testPatternObserverTimerInterval() {
        RegressionRunner.run(session, PatternObserverTimerInterval.executions());
    }

    public void testPatternGuardTimerWithin() {
        RegressionRunner.run(session, PatternGuardTimerWithin.executions());
    }

    public void testPatternOperatorFollowedBy() {
        RegressionRunner.run(session, PatternOperatorFollowedBy.executions());
    }

    public void testPatternOperatorEvery() {
        RegressionRunner.run(session, PatternOperatorEvery.executions());
    }

    public void testPatternOperatorMatchUntil() {
        RegressionRunner.run(session, PatternOperatorMatchUntil.executions());
    }

    public void testPatternOperatorEveryDistinct() {
        RegressionRunner.run(session, PatternOperatorEveryDistinct.executions());
    }

    public void testPatternObserverTimerAt() {
        RegressionRunner.run(session, PatternObserverTimerAt.executions());
    }

    public void testPatternObserverTimerSchedule() {
        RegressionRunner.run(session, PatternObserverTimerSchedule.executions());
    }

    public void testPatternGuardWhile() {
        RegressionRunner.run(session, PatternGuardWhile.executions());
    }

    public void testPatternGuardTimerWithinOrMax() {
        RegressionRunner.run(session, new PatternGuardTimerWithinOrMax());
    }

    public void testPatternUseResult() {
        RegressionRunner.run(session, PatternUseResult.executions());
    }

    public void testPatternOperatorOperatorMix() {
        RegressionRunner.run(session, new PatternOperatorOperatorMix());
    }

    public void testPatternComplexPropertyAccess() {
        RegressionRunner.run(session, PatternComplexPropertyAccess.executions());
    }

    public void testPatternOperatorFollowedByMax() {
        RegressionRunner.run(session, PatternOperatorFollowedByMax.executions());
    }

    public void testPatternCompositeSelect() {
        RegressionRunner.run(session, PatternCompositeSelect.executions());
    }

    public void testPatternConsumingFilter() {
        RegressionRunner.run(session, PatternConsumingFilter.executions());
    }

    public void testPatternConsumingPattern() {
        RegressionRunner.run(session, PatternConsumingPattern.executions());
    }

    public void testPatternDeadPattern() {
        RegressionRunner.run(session, new PatternDeadPattern());
    }

    public void testPatternInvalid() {
        RegressionRunner.run(session, PatternInvalid.executions());
    }

    public void testPatternStartLoop() {
        RegressionRunner.run(session, new PatternStartLoop());
    }

    public void testPatternMicrosecondResolution() {
        RegressionRunner.run(session, new PatternMicrosecondResolution(false));
    }

    public void testPatternStartStop() {
        RegressionRunner.run(session, PatternStartStop.executions());
    }

    public void testPatternSuperAndInterfaces() {
        RegressionRunner.run(session, new PatternSuperAndInterfaces());
    }

    public void testPatternRepeatRouteEvent() {
        RegressionRunner.run(session, PatternRepeatRouteEvent.executions());
    }

    public void testPatternExpressionText() {
        RegressionRunner.run(session, new PatternExpressionText());
    }

    private static void configure(Configuration configuration) {
        for (Class clazz : new Class[]{SupportBean_A.class, SupportBean_B.class, SupportBean_C.class, SupportBean_D.class, SupportBean_E.class, SupportBean_F.class, SupportBean_G.class,
            SupportBean.class, SupportCallEvent.class, SupportRFIDEvent.class, SupportBean_N.class, SupportBean_S0.class,
            SupportIdEventA.class, SupportIdEventB.class, SupportIdEventC.class, SupportIdEventD.class,
            SupportMarketDataBean.class, SupportTradeEvent.class, SupportBeanComplexProps.class, SupportBeanCombinedProps.class, ISupportC.class,
            ISupportBaseAB.class, ISupportA.class, ISupportB.class, ISupportD.class, ISupportBaseD.class, ISupportBaseDBase.class, ISupportAImplSuperG.class,
            ISupportAImplSuperGImplPlus.class, SupportOverrideBase.class, SupportOverrideOne.class, SupportOverrideOneA.class, SupportOverrideOneB.class,
            ISupportCImpl.class, ISupportABCImpl.class, ISupportAImpl.class, ISupportBImpl.class, ISupportDImpl.class, ISupportBCImpl.class,
            ISupportBaseABImpl.class, ISupportAImplSuperGImpl.class, SupportEventWithIntArray.class}) {
            configuration.getCommon().addEventType(clazz.getSimpleName(), clazz);
        }

        for (String name : "computeISO8601String,getThe1980Calendar,getThe1980Date,getThe1980Long,getTheSeconds,getThe1980LocalDateTime,getThe1980ZonedDateTime".split(",")) {
            configuration.getCompiler().addPlugInSingleRowFunction(name, PatternObserverTimerSchedule.class.getName(), name);
        }

        ConfigurationCommon common = configuration.getCommon();
        common.addVariable("lower", int.class, null);
        common.addVariable("upper", int.class, null);
        common.addVariable("VMIN", int.class, 0);
        common.addVariable("VHOUR", int.class, 8);
        common.addVariable("D", double.class, 1);
        common.addVariable("H", double.class, 2);
        common.addVariable("M", double.class, 3);
        common.addVariable("S", double.class, 4);
        common.addVariable("MS", double.class, 5);

        configuration.getRuntime().getConditionHandling().addClass(SupportConditionHandlerFactory.class);

        configuration.getCommon().addImport(SupportStaticMethodLib.class);

        configuration.getCompiler().getByteCode().setAttachEPL(true);
    }
}
