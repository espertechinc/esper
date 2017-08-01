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
package com.espertech.esper.regression.event.infra;

import com.espertech.esper.supportregression.execution.RegressionRunner;
import junit.framework.TestCase;

public class TestSuiteEventInfra extends TestCase {
    public void testExecEventInfraPropertyUnderlyingSimple() {
        RegressionRunner.run(new ExecEventInfraPropertyUnderlyingSimple());
    }

    public void testExecEventInfraPropertyMappedIndexed() {
        RegressionRunner.run(new ExecEventInfraPropertyMappedIndexed());
    }

    public void testExecEventInfraPropertyDynamicNonSimple() {
        RegressionRunner.run(new ExecEventInfraPropertyDynamicNonSimple());
    }

    public void testExecEventInfraPropertyDynamicSimple() {
        RegressionRunner.run(new ExecEventInfraPropertyDynamicSimple());
    }

    public void testExecEventInfraPropertyNestedSimple() {
        RegressionRunner.run(new ExecEventInfraPropertyNestedSimple());
    }

    public void testExecEventInfraPropertyNestedIndexed() {
        RegressionRunner.run(new ExecEventInfraPropertyNestedIndexed());
    }

    public void testExecEventInfraPropertyNestedDynamic() {
        RegressionRunner.run(new ExecEventInfraPropertyNestedDynamic());
    }

    public void testExecEventInfraPropertyNestedDynamicRootedSimple() {
        RegressionRunner.run(new ExecEventInfraPropertyNestedDynamicRootedSimple());
    }

    public void testExecEventInfraPropertyNestedDynamicDeep() {
        RegressionRunner.run(new ExecEventInfraPropertyNestedDynamicDeep());
    }

    public void testExecEventInfraPropertyNestedDynamicRootedNonSimple() {
        RegressionRunner.run(new ExecEventInfraPropertyNestedDynamicRootedNonSimple());
    }

    public void testExecEventInfraEventRenderer() {
        RegressionRunner.run(new ExecEventInfraEventRenderer());
    }

    public void testExecEventInfraEventSender() {
        RegressionRunner.run(new ExecEventInfraEventSender());
    }

    public void testExecEventInfraSuperType() {
        RegressionRunner.run(new ExecEventInfraSuperType());
    }

    public void testExecEventInfraStaticConfiguration() {
        RegressionRunner.run(new ExecEventInfraStaticConfiguration());
    }

    public void testExecEventInfraPropertyAccessPerformance() {
        RegressionRunner.run(new ExecEventInfraPropertyAccessPerformance());
    }

    public void testExecEventInfraPropertyIndexedKeyExpr() {
        RegressionRunner.run(new ExecEventInfraPropertyIndexedKeyExpr());
    }
}
