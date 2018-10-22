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
package com.espertech.esper.regressionlib.suite.event.map;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;

import java.util.Map;

public class EventMapNestedEscapeDot implements RegressionExecution {

    public void run(RegressionEnvironment env) {
        String statementText = "@name('s0') select a\\.b, a\\.b\\.c, nes\\., nes\\.nes2.x\\.y from DotMap";
        env.compileDeploy(statementText).addListener("s0");

        Map<String, Object> data = EventMapCore.makeMap(new Object[][]{
            {"a.b", 10},
            {"a.b.c", 20},
            {"nes.", 30},
            {"nes.nes2", EventMapCore.makeMap(new Object[][]{{"x.y", 40}})}
        });
        env.sendEventMap(data, "DotMap");

        String[] fields = "a.b,a.b.c,nes.,nes.nes2.x.y".split(",");
        EventBean received = env.listener("s0").assertOneGetNewAndReset();
        EPAssertionUtil.assertProps(received, fields, new Object[]{10, 20, 30, 40});

        env.undeployAll();
    }
}
