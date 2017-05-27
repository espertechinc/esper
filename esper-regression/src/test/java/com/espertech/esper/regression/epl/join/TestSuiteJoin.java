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
package com.espertech.esper.regression.epl.join;

import com.espertech.esper.supportregression.execution.RegressionRunner;
import junit.framework.TestCase;

public class TestSuiteJoin extends TestCase {
    public void testExecJoin20Stream() {
        RegressionRunner.run(new ExecJoin20Stream());
    }

    public void testExecJoinCoercion() {
        RegressionRunner.run(new ExecJoinCoercion());
    }

    public void testExecJoinDerivedValueViews() {
        RegressionRunner.run(new ExecJoinDerivedValueViews());
    }

    public void testExecJoinEventRepresentation() {
        RegressionRunner.run(new ExecJoinEventRepresentation());
    }

    public void testExecJoinInheritAndInterface() {
        RegressionRunner.run(new ExecJoinInheritAndInterface());
    }

    public void testExecJoinMultiKeyAndRange() {
        RegressionRunner.run(new ExecJoinMultiKeyAndRange());
    }

    public void testExecJoinNoTableName() {
        RegressionRunner.run(new ExecJoinNoTableName());
    }

    public void testExecJoinNoWhereClause() {
        RegressionRunner.run(new ExecJoinNoWhereClause());
    }

    public void testExecJoinPropertyAccess() {
        RegressionRunner.run(new ExecJoinPropertyAccess());
    }

    public void testExecJoinSingleOp3Stream() {
        RegressionRunner.run(new ExecJoinSingleOp3Stream());
    }

    public void testExecJoinStartStop() {
        RegressionRunner.run(new ExecJoinStartStop());
    }

    public void testExecJoinUniqueIndex() {
        RegressionRunner.run(new ExecJoinUniqueIndex());
    }

    public void testExecOuterFullJoin3Stream() {
        RegressionRunner.run(new ExecOuterFullJoin3Stream());
    }

    public void testExecOuterInnerJoin3Stream() {
        RegressionRunner.run(new ExecOuterInnerJoin3Stream());
    }

    public void testExecOuterInnerJoin4Stream() {
        RegressionRunner.run(new ExecOuterInnerJoin4Stream());
    }

    public void testExecOuterJoin2Stream() {
        RegressionRunner.run(new ExecOuterJoin2Stream());
    }

    public void testExecOuterJoin6Stream() {
        RegressionRunner.run(new ExecOuterJoin6Stream());
    }

    public void testExecOuterJoin7Stream() {
        RegressionRunner.run(new ExecOuterJoin7Stream());
    }

    public void testExecOuterJoinCart4Stream() {
        RegressionRunner.run(new ExecOuterJoinCart4Stream());
    }

    public void testExecOuterJoinCart5Stream() {
        RegressionRunner.run(new ExecOuterJoinCart5Stream());
    }

    public void testExecOuterJoinChain4Stream() {
        RegressionRunner.run(new ExecOuterJoinChain4Stream());
    }

    public void testExecOuterJoinUnidirectional() {
        RegressionRunner.run(new ExecOuterJoinUnidirectional());
    }

    public void testExecOuterJoinVarA3Stream() {
        RegressionRunner.run(new ExecOuterJoinVarA3Stream());
    }

    public void testExecOuterJoinVarB3Stream() {
        RegressionRunner.run(new ExecOuterJoinVarB3Stream());
    }

    public void testExecOuterJoinVarC3Stream() {
        RegressionRunner.run(new ExecOuterJoinVarC3Stream());
    }

    public void testExecOuterJoinLeftWWhere() {
        RegressionRunner.run(new ExecOuterJoinLeftWWhere());
    }

    public void testExecJoinSelectClause() {
        RegressionRunner.run(new ExecJoinSelectClause());
    }

    public void testExecJoinPatterns() {
        RegressionRunner.run(new ExecJoinPatterns());
    }

    public void testExecJoinUnidirectionalStream() {
        RegressionRunner.run(new ExecJoinUnidirectionalStream());
    }

    public void testExecJoin2StreamAndPropertyPerformance() {
        RegressionRunner.run(new ExecJoin2StreamAndPropertyPerformance());
    }

    public void testExecJoin2StreamExprPerformance() {
        RegressionRunner.run(new ExecJoin2StreamExprPerformance());
    }

    public void testExecJoin2StreamInKeywordPerformance() {
        RegressionRunner.run(new ExecJoin2StreamInKeywordPerformance());
    }

    public void testExecJoin2StreamRangePerformance() {
        RegressionRunner.run(new ExecJoin2StreamRangePerformance());
    }

    public void testExecJoin2StreamSimplePerformance() {
        RegressionRunner.run(new ExecJoin2StreamSimplePerformance());
    }

    public void testExecJoin2StreamSimpleCoercionPerformance() {
        RegressionRunner.run(new ExecJoin2StreamSimpleCoercionPerformance());
    }

    public void testExecJoin3StreamAndPropertyPerformance() {
        RegressionRunner.run(new ExecJoin3StreamAndPropertyPerformance());
    }

    public void testExecJoin3StreamCoercionPerformance() {
        RegressionRunner.run(new ExecJoin3StreamCoercionPerformance());
    }

    public void testExecJoin3StreamInKeywordPerformance() {
        RegressionRunner.run(new ExecJoin3StreamInKeywordPerformance());
    }

    public void testExecJoin3StreamOuterJoinCoercionPerformance() {
        RegressionRunner.run(new ExecJoin3StreamOuterJoinCoercionPerformance());
    }

    public void testExecJoin3StreamRangePerformance() {
        RegressionRunner.run(new ExecJoin3StreamRangePerformance());
    }

    public void testExecJoin5StreamPerformance() {
        RegressionRunner.run(new ExecJoin5StreamPerformance());
    }
}
