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
package com.espertech.esper.regression.nwtable;

import com.espertech.esper.client.Configuration;
import com.espertech.esper.client.EPServiceProviderManager;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.core.service.EPServiceProviderSPI;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.supportregression.client.SupportConfigFactory;
import com.espertech.esper.supportregression.util.SupportMessageAssertUtil;
import com.espertech.esper.supportregression.util.SupportModelHelper;
import com.espertech.esper.util.support.SupportEventTypeAssertionEnum;
import com.espertech.esper.util.support.SupportEventTypeAssertionUtil;
import junit.framework.TestCase;

public class TestInfraEventType extends TestCase
{
    private EPServiceProviderSPI epService;

    public void setUp()
    {
        Configuration config = SupportConfigFactory.getConfiguration();
        epService = (EPServiceProviderSPI) EPServiceProviderManager.getDefaultProvider(config);
        epService.initialize();
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.startTest(epService, this.getClass(), getName());}
    }
    
    protected void tearDown() throws Exception {
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.endTest();}
    }

    public void testEventType() {
        runAssertionType(true);
        runAssertionType(false);

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
    
    private void runAssertionType(boolean namedWindow) {
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
