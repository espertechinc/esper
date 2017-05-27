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
package com.espertech.esper.regression.event.variant;

import com.espertech.esper.supportregression.execution.RegressionRunner;
import junit.framework.TestCase;

public class TestSuiteEventVariant extends TestCase {
    public void testExecEventVariantStreamAny() {
        RegressionRunner.run(new ExecEventVariantStreamAny());
    }

    public void testExecEventVariantStreamDefault() {
        RegressionRunner.run(new ExecEventVariantStreamDefault());
    }
}
