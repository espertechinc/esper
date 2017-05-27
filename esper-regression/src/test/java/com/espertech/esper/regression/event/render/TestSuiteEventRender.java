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
package com.espertech.esper.regression.event.render;

import com.espertech.esper.supportregression.execution.RegressionRunner;
import junit.framework.TestCase;

public class TestSuiteEventRender extends TestCase {
    public void testExecEventRender() {
        RegressionRunner.run(new ExecEventRender());
    }

    public void testExecEventRenderJSON() {
        RegressionRunner.run(new ExecEventRenderJSON());
    }

    public void testExecEventRenderXML() {
        RegressionRunner.run(new ExecEventRenderXML());
    }
}
