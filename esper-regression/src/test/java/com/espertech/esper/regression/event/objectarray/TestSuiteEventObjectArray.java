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
package com.espertech.esper.regression.event.objectarray;

import com.espertech.esper.supportregression.execution.RegressionRunner;
import junit.framework.TestCase;

public class TestSuiteEventObjectArray extends TestCase {
    public void testExecEventObjectArray() {
        RegressionRunner.run(new ExecEventObjectArray());
    }

    public void testExecEventObjectArrayNestedMap() {
        RegressionRunner.run(new ExecEventObjectArrayNestedMap());
    }

    public void testExecEventObjectArrayInheritanceConfigInit() {
        RegressionRunner.run(new ExecEventObjectArrayInheritanceConfigInit());
    }

    public void testExecEventObjectArrayInheritanceConfigRuntime() {
        RegressionRunner.run(new ExecEventObjectArrayInheritanceConfigRuntime());
    }

    public void testExecEventObjectArrayConfiguredStatic() {
        RegressionRunner.run(new ExecEventObjectArrayConfiguredStatic());
    }

    public void testExecEventObjectArrayTypeUpdate() {
        RegressionRunner.run(new ExecEventObjectArrayTypeUpdate());
    }

    public void testExecEventObjectArrayEventNestedPojo() {
        RegressionRunner.run(new ExecEventObjectArrayEventNestedPojo());
    }

    public void testExecEventObjectArrayEventNested() {
        RegressionRunner.run(new ExecEventObjectArrayEventNested());
    }
}
