/*
 * *************************************************************************************
 *  Copyright (C) 2006-2015 EsperTech, Inc. All rights reserved.                       *
 *  http://www.espertech.com/esper                                                     *
 *  http://www.espertech.com                                                           *
 *  ---------------------------------------------------------------------------------- *
 *  The software in this package is published under the terms of the GPL license       *
 *  a copy of which has been included with this distribution in the license.txt file.  *
 * *************************************************************************************
 */

package com.espertech.esper.regression.nwtable;

import com.espertech.esper.client.*;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.supportregression.bean.*;
import com.espertech.esper.supportregression.client.SupportConfigFactory;
import junit.framework.TestCase;

public class TestNamedWindowIndexAddedValType extends TestCase
{
    public void testRevision()
    {
        Configuration config = SupportConfigFactory.getConfiguration();
        config.addEventType("SupportBean_S0", SupportBean_S0.class);
        config.addEventType("SupportBean_S1", SupportBean_S1.class);

        ConfigurationRevisionEventType revType = new ConfigurationRevisionEventType();
        revType.addNameBaseEventType("SupportBean_S0");
        revType.addNameDeltaEventType("SupportBean_S1");
        revType.setKeyPropertyNames(new String[] {"id"});
        revType.setPropertyRevision(ConfigurationRevisionEventType.PropertyRevision.MERGE_EXISTS);
        config.addRevisionEventType("RevType", revType);

        EPServiceProvider epService = EPServiceProviderManager.getDefaultProvider(config);
        epService.initialize();
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.startTest(epService, this.getClass(), getName());}

        // widen to long
        String stmtTextCreate = "create window MyWindowOne#keepall as select * from RevType";
        epService.getEPAdministrator().createEPL(stmtTextCreate);
        epService.getEPAdministrator().createEPL("insert into MyWindowOne select * from SupportBean_S0");
        epService.getEPAdministrator().createEPL("insert into MyWindowOne select * from SupportBean_S1");

        epService.getEPAdministrator().createEPL("create index MyWindowOneIndex1 on MyWindowOne(p10)");
        epService.getEPAdministrator().createEPL("create index MyWindowOneIndex2 on MyWindowOne(p00)");

        epService.getEPRuntime().sendEvent(new SupportBean_S0(1, "p00"));
        epService.getEPRuntime().sendEvent(new SupportBean_S1(1, "p10"));

        EPOnDemandQueryResult result = epService.getEPRuntime().executeQuery("select * from MyWindowOne where p10='1'");
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.endTest();}
    }
}