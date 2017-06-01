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
package com.espertech.esper.regression.event.avro;

import com.espertech.esper.supportregression.execution.RegressionRunner;
import junit.framework.TestCase;

public class TestSuiteEventAvro extends TestCase {
    public void testExecEventAvroSampleConfigDocOutputSchema() {
        RegressionRunner.run(new ExecEventAvroSampleConfigDocOutputSchema());
    }

    public void testExecEventAvroJsonWithSchema() {
        RegressionRunner.run(new ExecEventAvroJsonWithSchema());
    }

    public void testExecEventAvroHook() {
        RegressionRunner.run(new ExecEventAvroHook());
    }

    public void testExecAvroEventBean() {
        RegressionRunner.run(new ExecAvroEventBean());
    }
}
