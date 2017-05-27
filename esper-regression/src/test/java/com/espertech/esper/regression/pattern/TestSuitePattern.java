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

import com.espertech.esper.supportregression.execution.RegressionRunner;
import junit.framework.TestCase;

public class TestSuitePattern extends TestCase {
    public void testExecPatternOperatorAnd() {
        RegressionRunner.run(new ExecPatternOperatorAnd());
    }

    public void testExecPatternOperatorNot() {
        RegressionRunner.run(new ExecPatternOperatorNot());
    }

    public void testExecPatternOperatorOr() {
        RegressionRunner.run(new ExecPatternOperatorOr());
    }

    public void testExecPatternOperatorOperatorMix() {
        RegressionRunner.run(new ExecPatternOperatorOperatorMix());
    }

    public void testExecPatternOperatorFollowedBy() {
        RegressionRunner.run(new ExecPatternOperatorFollowedBy());
    }

    public void testExecPatternOperatorFollowedByMax() {
        RegressionRunner.run(new ExecPatternOperatorFollowedByMax());
    }

    public void testExecPatternOperatorEvery() {
        RegressionRunner.run(new ExecPatternOperatorEvery());
    }

    public void testExecPatternOperatorEveryDistinct() {
        RegressionRunner.run(new ExecPatternOperatorEveryDistinct());
    }

    public void testExecPatternOperatorMatchUntilExpr() {
        RegressionRunner.run(new ExecPatternOperatorMatchUntilExpr());
    }

    public void testExecPatternOperatorFollowedByMax4Prevent() {
        RegressionRunner.run(new ExecPatternOperatorFollowedByMax4Prevent());
    }

    public void testExecPatternOperatorFollowedByMax2Prevent() {
        RegressionRunner.run(new ExecPatternOperatorFollowedByMax2Prevent());
    }

    public void testExecPatternOperatorFollowedByMax2Noprevent() {
        RegressionRunner.run(new ExecPatternOperatorFollowedByMax2Noprevent());
    }

    public void testExecPatternComplexPropertyAccess() {
        RegressionRunner.run(new ExecPatternComplexPropertyAccess());
    }

    public void testExecPatternCompositeSelect() {
        RegressionRunner.run(new ExecPatternCompositeSelect());
    }

    public void testExecPatternConsumingFilter() {
        RegressionRunner.run(new ExecPatternConsumingFilter());
    }

    public void testExecPatternConsumingPattern() {
        RegressionRunner.run(new ExecPatternConsumingPattern());
    }

    public void testExecPatternCronParameter() {
        RegressionRunner.run(new ExecPatternCronParameter());
    }

    public void testExecPatternDeadPattern() {
        RegressionRunner.run(new ExecPatternDeadPattern());
    }

    public void testExecPatternInvalid() {
        RegressionRunner.run(new ExecPatternInvalid());
    }

    public void testExecPatternExpressionText() {
        RegressionRunner.run(new ExecPatternExpressionText());
    }

    public void testExecPatternMicrosecondResolution() {
        RegressionRunner.run(new ExecPatternMicrosecondResolution());
    }

    public void testExecPatternStartLoop() {
        RegressionRunner.run(new ExecPatternStartLoop());
    }

    public void testExecPatternStartStop() {
        RegressionRunner.run(new ExecPatternStartStop());
    }

    public void testExecPatternRepeatRouteEvent() {
        RegressionRunner.run(new ExecPatternRepeatRouteEvent());
    }

    public void testExecPatternSuperAndInterfaces() {
        RegressionRunner.run(new ExecPatternSuperAndInterfaces());
    }

    public void testExecPatternObserverTimerAt() {
        RegressionRunner.run(new ExecPatternObserverTimerAt());
    }

    public void testExecPatternObserverTimerInterval() {
        RegressionRunner.run(new ExecPatternObserverTimerInterval());
    }

    public void testExecPatternObserverTimerSchedule() {
        RegressionRunner.run(new ExecPatternObserverTimerSchedule());
    }

    public void testExecPatternObserverTimerScheduleTimeZoneEST() {
        RegressionRunner.run(new ExecPatternObserverTimerScheduleTimeZoneEST());
    }

    public void testExecPatternGuardTimerWithin() {
        RegressionRunner.run(new ExecPatternGuardTimerWithin());
    }

    public void testExecPatternGuardTimerWithinOrMax() {
        RegressionRunner.run(new ExecPatternGuardTimerWithinOrMax());
    }

    public void testExecPatternGuardWhile() {
        RegressionRunner.run(new ExecPatternGuardWhile());
    }

    public void testExecPatternUseResult() {
        RegressionRunner.run(new ExecPatternUseResult());
    }

}
