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
package com.espertech.esper.regression.event.map;

import com.espertech.esper.client.Configuration;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.supportregression.execution.RegressionExecution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class ExecEventMapNestedEscapeDot implements RegressionExecution {
    public void configure(Configuration configuration) throws Exception {
        Map<String, Object> definition = ExecEventMap.makeMap(new Object[][]{
                {"a.b", int.class},
                {"a.b.c", int.class},
                {"nes.", int.class},
                {"nes.nes2", ExecEventMap.makeMap(new Object[][]{{"x.y", int.class}})}
        });
        configuration.addEventType("DotMap", definition);
    }

    public void run(EPServiceProvider epService) throws Exception {
        String statementText = "select a\\.b, a\\.b\\.c, nes\\., nes\\.nes2.x\\.y from DotMap";
        EPStatement statement = epService.getEPAdministrator().createEPL(statementText);
        SupportUpdateListener listener = new SupportUpdateListener();
        statement.addListener(listener);

        Map<String, Object> data = ExecEventMap.makeMap(new Object[][]{
                {"a.b", 10},
                {"a.b.c", 20},
                {"nes.", 30},
                {"nes.nes2", ExecEventMap.makeMap(new Object[][]{{"x.y", 40}})}
        });
        epService.getEPRuntime().sendEvent(data, "DotMap");

        String[] fields = "a.b,a.b.c,nes.,nes.nes2.x.y".split(",");
        EventBean received = listener.assertOneGetNewAndReset();
        EPAssertionUtil.assertProps(received, fields, new Object[]{10, 20, 30, 40});
    }

    private final static Logger log = LoggerFactory.getLogger(TestSuiteEventMap.class);
}
