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
package com.espertech.esper.regression.epl.contained;

import com.espertech.esper.supportregression.execution.RegressionRunner;
import junit.framework.TestCase;

public class TestSuiteContained extends TestCase {
    public void testExecContainedEventArray() {
        RegressionRunner.run(new ExecContainedEventArray());
    }

    public void testExecContainedEventExample() {
        RegressionRunner.run(new ExecContainedEventExample());
    }

    public void testExecContainedEventNested() {
        RegressionRunner.run(new ExecContainedEventNested());
    }

    public void testExecContainedEventSimple() {
        RegressionRunner.run(new ExecContainedEventSimple());
    }

    public void testExecContainedEventSplitExpr() {
        RegressionRunner.run(new ExecContainedEventSplitExpr());
    }
}
