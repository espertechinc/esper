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

import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.supportregression.execution.RegressionExecution;
import com.espertech.esper.supportregression.util.SupportMessageAssertUtil;
import com.espertech.esper.supportregression.util.SupportModelHelper;
import com.espertech.esper.support.SupportEventTypeAssertionEnum;
import com.espertech.esper.support.SupportEventTypeAssertionUtil;

public class ExecNWTableInfraEventType implements RegressionExecution {
    public void run(EPServiceProvider epService) throws Exception {
        runAssertionType(epService, true);
        runAssertionType(epService, false);

        // name cannot be the same as an existing event type
        epService.getEPAdministrator().createEPL("create schema SchemaOne as (p0 string)");
        SupportMessageAssertUtil.tryInvalid(epService, "create window SchemaOne.win:keepall as SchemaOne",
                "Error starting statement: An event type or schema by name 'SchemaOne' already exists"
        );

        epService.getEPAdministrator().createEPL("create schema SchemaTwo as (p0 string)");
        SupportMessageAssertUtil.tryInvalid(epService, "create table SchemaTwo(c0 int)",
                "Error starting statement: An event type or schema by name 'SchemaTwo' already exists"
        );
    }

    private void runAssertionType(EPServiceProvider epService, boolean namedWindow) {
        String eplCreate = namedWindow ?
                "create window MyInfra#keepall as (c0 int[], c1 int[primitive])" :
                "create table MyInfra (c0 int[], c1 int[primitive])";
        EPStatement stmt = SupportModelHelper.createByCompileOrParse(epService, false, eplCreate);

        Object[][] expectedType = new Object[][]{{"c0", Integer[].class}, {"c1", int[].class}};
        SupportEventTypeAssertionUtil.assertEventTypeProperties(expectedType, stmt.getEventType(), SupportEventTypeAssertionEnum.NAME, SupportEventTypeAssertionEnum.TYPE);

        epService.getEPAdministrator().destroyAllStatements();
        epService.getEPAdministrator().getConfiguration().removeEventType("MyInfra", false);
    }
}
