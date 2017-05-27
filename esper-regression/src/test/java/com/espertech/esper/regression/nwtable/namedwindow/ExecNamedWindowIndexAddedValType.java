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
package com.espertech.esper.regression.nwtable.namedwindow;

import com.espertech.esper.client.Configuration;
import com.espertech.esper.client.ConfigurationRevisionEventType;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.supportregression.bean.SupportBean_S0;
import com.espertech.esper.supportregression.bean.SupportBean_S1;
import com.espertech.esper.supportregression.execution.RegressionExecution;

public class ExecNamedWindowIndexAddedValType implements RegressionExecution {
    public void configure(Configuration configuration) throws Exception {
        configuration.addEventType("SupportBean_S0", SupportBean_S0.class);
        configuration.addEventType("SupportBean_S1", SupportBean_S1.class);

        ConfigurationRevisionEventType revType = new ConfigurationRevisionEventType();
        revType.addNameBaseEventType("SupportBean_S0");
        revType.addNameDeltaEventType("SupportBean_S1");
        revType.setKeyPropertyNames(new String[]{"id"});
        revType.setPropertyRevision(ConfigurationRevisionEventType.PropertyRevision.MERGE_EXISTS);
        configuration.addRevisionEventType("RevType", revType);
    }

    public void run(EPServiceProvider epService) throws Exception {
        String stmtTextCreate = "create window MyWindowOne#keepall as select * from RevType";
        epService.getEPAdministrator().createEPL(stmtTextCreate);
        epService.getEPAdministrator().createEPL("insert into MyWindowOne select * from SupportBean_S0");
        epService.getEPAdministrator().createEPL("insert into MyWindowOne select * from SupportBean_S1");

        epService.getEPAdministrator().createEPL("create index MyWindowOneIndex1 on MyWindowOne(p10)");
        epService.getEPAdministrator().createEPL("create index MyWindowOneIndex2 on MyWindowOne(p00)");

        epService.getEPRuntime().sendEvent(new SupportBean_S0(1, "p00"));
        epService.getEPRuntime().sendEvent(new SupportBean_S1(1, "p10"));

        epService.getEPRuntime().executeQuery("select * from MyWindowOne where p10='1'");
    }
}