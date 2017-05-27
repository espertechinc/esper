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
package com.espertech.esper.regression.epl.contained;

import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.deploy.DeploymentResult;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.bean.SupportBeanArrayCollMap;
import com.espertech.esper.supportregression.execution.RegressionExecution;

import static org.junit.Assert.assertEquals;

public class ExecContainedEventArray implements RegressionExecution {
    public void run(EPServiceProvider epService) throws Exception {
        runAssertion(epService);
        runDocSample(epService);
    }

    private void runDocSample(EPServiceProvider epService) throws Exception {
        epService.getEPAdministrator().getDeploymentAdmin().parseDeploy(
                "create schema IdContainer(id int);" +
                        "create schema MyEvent(ids int[]);" +
                        "select * from MyEvent[ids@type(IdContainer)];");

        epService.getEPAdministrator().getDeploymentAdmin().parseDeploy(
                "create window MyWindow#keepall (id int);" +
                        "on MyEvent[ids@type(IdContainer)] as my_ids \n" +
                        "delete from MyWindow my_window \n" +
                        "where my_ids.id = my_window.id;");
    }

    private void runAssertion(EPServiceProvider epService) throws Exception {
        epService.getEPAdministrator().getConfiguration().addEventType(SupportBean.class);
        epService.getEPAdministrator().getConfiguration().addEventType(SupportBeanArrayCollMap.class);

        String epl = "create objectarray schema DeleteId(id int);" +
                "create window MyWindow#keepall as SupportBean;" +
                "insert into MyWindow select * from SupportBean;" +
                "on SupportBeanArrayCollMap[intArr@type(DeleteId)] delete from MyWindow where intPrimitive = id";
        DeploymentResult deployed = epService.getEPAdministrator().getDeploymentAdmin().parseDeploy(epl);

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 1));
        epService.getEPRuntime().sendEvent(new SupportBean("E2", 2));

        assertCount(epService, 2);
        epService.getEPRuntime().sendEvent(new SupportBeanArrayCollMap(new int[]{1, 2}));
        assertCount(epService, 0);

        epService.getEPAdministrator().getDeploymentAdmin().undeploy(deployed.getDeploymentId());
    }

    private void assertCount(EPServiceProvider epService, long i) {
        assertEquals(i, epService.getEPRuntime().executeQuery("select count(*) as c0 from MyWindow").getArray()[0].get("c0"));
    }
}
