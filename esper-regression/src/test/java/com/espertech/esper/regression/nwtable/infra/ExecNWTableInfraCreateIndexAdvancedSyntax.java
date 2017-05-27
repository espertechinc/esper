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
package com.espertech.esper.regression.nwtable.infra;

import com.espertech.esper.client.Configuration;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.soda.EPStatementObjectModel;
import com.espertech.esper.supportregression.bean.SupportSpatialPoint;
import com.espertech.esper.supportregression.execution.RegressionExecution;
import com.espertech.esper.supportregression.util.SupportMessageAssertUtil;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;

public class ExecNWTableInfraCreateIndexAdvancedSyntax implements RegressionExecution {
    public void configure(Configuration configuration) throws Exception {
        configuration.getEngineDefaults().getLogging().setEnableQueryPlan(true);
    }

    public void run(EPServiceProvider epService) throws Exception {
        for (Class clazz : Arrays.asList(SupportSpatialPoint.class)) {
            epService.getEPAdministrator().getConfiguration().addEventType(clazz);
        }

        assertCompileSODA(epService, "create index MyIndex on MyWindow((x,y) dummy_name(\"a\",10101))");
        assertCompileSODA(epService, "create index MyIndex on MyWindow(x dummy_name)");
        assertCompileSODA(epService, "create index MyIndex on MyWindow((x,y,z) dummy_name)");
        assertCompileSODA(epService, "create index MyIndex on MyWindow(x dummy_name, (y,z) dummy_name_2(\"a\"), p dummyname3)");

        epService.getEPAdministrator().createEPL("create window MyWindow#keepall as SupportSpatialPoint");
        SupportMessageAssertUtil.tryInvalid(epService, "create index MyIndex on MyWindow(())",
                "Error starting statement: Invalid empty list of index expressions");

        SupportMessageAssertUtil.tryInvalid(epService, "create index MyIndex on MyWindow(intPrimitive+1)",
                "Error starting statement: Invalid index expression 'intPrimitive+1'");

        SupportMessageAssertUtil.tryInvalid(epService, "create index MyIndex on MyWindow((x, y))",
                "Error starting statement: Invalid multiple index expressions");

        SupportMessageAssertUtil.tryInvalid(epService, "create index MyIndex on MyWindow(x.y)",
                "Error starting statement: Invalid index expression 'x.y'");

        SupportMessageAssertUtil.tryInvalid(epService, "create index MyIndex on MyWindow(id xxxx)",
                "Error starting statement: Unrecognized advanced-type index 'xxxx'");
    }

    private void assertCompileSODA(EPServiceProvider epService, String epl) {
        EPStatementObjectModel model = epService.getEPAdministrator().compileEPL(epl);
        assertEquals(epl, model.toEPL());
    }
}
