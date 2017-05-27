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
package com.espertech.esper.regression.event.plugin;

import com.espertech.esper.supportregression.execution.RegressionRunner;
import junit.framework.TestCase;

public class TestSuiteEventPlugIn extends TestCase {
    public void testExecEventPlugInConfigStaticTypeResolution() {
        RegressionRunner.run(new ExecEventPlugInConfigStaticTypeResolution());
    }

    public void testExecEventPlugInConfigRuntimeTypeResolution() {
        RegressionRunner.run(new ExecEventPlugInConfigRuntimeTypeResolution());
    }

    public void testExecEventPlugInInvalid() {
        RegressionRunner.run(new ExecEventPlugInInvalid());
    }

    public void testExecEventPlugInContextContent() {
        RegressionRunner.run(new ExecEventPlugInContextContent());
    }

    public void testExecEventPlugInRuntimeConfigDynamicTypeResolution() {
        RegressionRunner.run(new ExecEventPlugInRuntimeConfigDynamicTypeResolution());
    }

    public void testExecEventPlugInStaticConfigDynamicTypeResolution() {
        RegressionRunner.run(new ExecEventPlugInStaticConfigDynamicTypeResolution());
    }
}
