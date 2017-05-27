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
package com.espertech.esper.regression.nwtable.tbl;

import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.EPStatementException;
import com.espertech.esper.client.deploy.DeploymentResult;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.bean.SupportBean_S0;
import com.espertech.esper.supportregression.bean.SupportBean_S1;
import com.espertech.esper.supportregression.execution.RegressionExecution;

import static org.junit.Assert.*;

public class ExecTableLifecycle implements RegressionExecution {
    public void run(EPServiceProvider epService) throws Exception {
        for (Class clazz : new Class[]{SupportBean.class, SupportBean_S0.class, SupportBean_S1.class}) {
            epService.getEPAdministrator().getConfiguration().addEventType(clazz);
        }

        runAssertionIntoTable(epService);

        runAssertionDependent(epService, "create index IDX on mytable (p)");
        runAssertionDependent(epService, "select * from SupportBean, mytable");
        runAssertionDependent(epService, "select * from SupportBean where exists (select * from mytable)");
        runAssertionDependent(epService, "insert into mytable select 'a' as id, 'a' as p from SupportBean");
    }

    private void runAssertionIntoTable(EPServiceProvider epService) throws Exception {
        String eplCreate = "create table abc (total count(*))";
        String eplUse = "select abc from SupportBean";
        String eplInto = "into table abc select count(*) as total from SupportBean";

        // typical select-use-destroy
        EPStatement stmtCreate = epService.getEPAdministrator().createEPL(eplCreate);
        EPStatement stmtSelect = epService.getEPAdministrator().createEPL(eplUse);
        EPStatement stmtInto = epService.getEPAdministrator().createEPL(eplInto);
        assertNotNull(epService.getEPAdministrator().getConfiguration().getEventType("table_abc__public"));
        assertNotNull(epService.getEPAdministrator().getConfiguration().getEventType("table_abc__internal"));

        stmtCreate.destroy();
        stmtSelect.destroy();
        assertFailCreate(epService, eplCreate);
        stmtInto.destroy();

        // destroy-all
        epService.getEPAdministrator().createEPL(eplCreate);
        epService.getEPAdministrator().createEPL(eplInto);
        epService.getEPAdministrator().createEPL(eplUse);
        epService.getEPAdministrator().destroyAllStatements();

        stmtCreate = epService.getEPAdministrator().createEPL(eplCreate);
        stmtCreate.destroy();

        // deploy and undeploy as module
        String module = eplCreate + ";\n" + eplUse + ";\n" + eplInto + ";\n";
        DeploymentResult deployed = epService.getEPAdministrator().getDeploymentAdmin().parseDeploy(module);
        assertNotNull(epService.getEPAdministrator().getConfiguration().getEventType("table_abc__public"));
        assertNotNull(epService.getEPAdministrator().getConfiguration().getEventType("table_abc__internal"));

        assertFailCreate(epService, eplCreate);
        epService.getEPAdministrator().getDeploymentAdmin().undeploy(deployed.getDeploymentId());
        assertNull(epService.getEPAdministrator().getConfiguration().getEventType("table_abc__public"));
        assertNull(epService.getEPAdministrator().getConfiguration().getEventType("table_abc__internal"));

        // stop and start
        EPStatement stmtCreateTwo = epService.getEPAdministrator().createEPL(eplCreate);
        stmtCreateTwo.stop();
        assertFailCreate(epService, eplCreate);
        stmtCreateTwo.start();
        assertFailCreate(epService, eplCreate);

        epService.getEPAdministrator().destroyAllStatements();
        epService.getEPAdministrator().createEPL(eplCreate);
        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionDependent(EPServiceProvider epService, String eplDependent) {
        String eplCreate = "create table mytable (id string primary key, p string)";

        // typical select-use-destroy
        EPStatement stmtCreate = epService.getEPAdministrator().createEPL(eplCreate);
        EPStatement stmtDependent = epService.getEPAdministrator().createEPL(eplDependent);

        stmtCreate.destroy();
        assertFailCreate(epService, eplCreate);
        stmtDependent.destroy();
        epService.getEPAdministrator().createEPL(eplCreate);
        epService.getEPAdministrator().destroyAllStatements();
    }

    private void assertFailCreate(EPServiceProvider epService, String create) {
        try {
            epService.getEPAdministrator().createEPL(create);
            fail();
        } catch (EPStatementException ex) {
            // expected
        }
    }
}
