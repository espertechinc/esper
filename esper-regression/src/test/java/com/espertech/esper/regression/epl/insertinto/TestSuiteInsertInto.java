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
package com.espertech.esper.regression.epl.insertinto;

import com.espertech.esper.supportregression.execution.RegressionRunner;
import junit.framework.TestCase;

public class TestSuiteInsertInto extends TestCase {
    public void testExecInsertInto() {
        RegressionRunner.run(new ExecInsertInto());
    }

    public void testExecInsertIntoEmptyPropType() {
        RegressionRunner.run(new ExecInsertIntoEmptyPropType());
    }

    public void testExecInsertIntoIRStreamFunc() {
        RegressionRunner.run(new ExecInsertIntoIRStreamFunc());
    }

    public void testExecInsertIntoPopulateCreateStream() {
        RegressionRunner.run(new ExecInsertIntoPopulateCreateStream());
    }

    public void testExecInsertIntoPopulateCreateStreamAvro() {
        RegressionRunner.run(new ExecInsertIntoPopulateCreateStreamAvro());
    }

    public void testExecInsertIntoPopulateEventTypeColumn() {
        RegressionRunner.run(new ExecInsertIntoPopulateEventTypeColumn());
    }

    public void testExecInsertIntoPopulateSingleColByMethodCall() {
        RegressionRunner.run(new ExecInsertIntoPopulateSingleColByMethodCall());
    }

    public void testExecInsertIntoPopulateUnderlying() {
        RegressionRunner.run(new ExecInsertIntoPopulateUnderlying());
    }

    public void testExecInsertIntoPopulateUndStreamSelect() {
        RegressionRunner.run(new ExecInsertIntoPopulateUndStreamSelect());
    }

    public void testExecInsertIntoTransposePattern() {
        RegressionRunner.run(new ExecInsertIntoTransposePattern());
    }

    public void testExecInsertIntoTransposeStream() {
        RegressionRunner.run(new ExecInsertIntoTransposeStream());
    }

    public void testExecInsertIntoFromPattern() {
        RegressionRunner.run(new ExecInsertIntoFromPattern());
    }

    public void testExecInsertIntoWrapper() {
        RegressionRunner.run(new ExecInsertIntoWrapper());
    }
}
