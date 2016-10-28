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

package com.espertech.esper.regression.view;

import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPServiceProviderManager;
import com.espertech.esper.client.deploy.DeploymentResult;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.support.bean.SupportBean;
import com.espertech.esper.support.bean.SupportBeanArrayCollMap;
import com.espertech.esper.support.client.SupportConfigFactory;
import junit.framework.TestCase;

public class TestContainedEventArray extends TestCase
{
    private EPServiceProvider epService;
    private SupportUpdateListener listener;

    public void setUp()
    {
        epService = EPServiceProviderManager.getDefaultProvider(SupportConfigFactory.getConfiguration());
        epService.initialize();
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.startTest(epService, this.getClass(), getName());}
        listener = new SupportUpdateListener();
    }

    protected void tearDown() throws Exception {
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.endTest();}
        listener = null;
    }

    public void testContainedEventArray() throws Exception {
        runAssertion();
        runDocSample();
    }

    private void runDocSample() throws Exception {
        epService.getEPAdministrator().getDeploymentAdmin().parseDeploy(
                "create schema IdContainer(id int);" +
                "create schema MyEvent(ids int[]);" +
                "select * from MyEvent[ids@type(IdContainer)];");

        epService.getEPAdministrator().getDeploymentAdmin().parseDeploy(
                "create window MyWindow#keepall() (id int);" +
                "on MyEvent[ids@type(IdContainer)] as my_ids \n" +
                "delete from MyWindow my_window \n" +
                "where my_ids.id = my_window.id;");
    }

    private void runAssertion() throws Exception {
        epService.getEPAdministrator().getConfiguration().addEventType(SupportBean.class);
        epService.getEPAdministrator().getConfiguration().addEventType(SupportBeanArrayCollMap.class);

        String epl = "create objectarray schema DeleteId(id int);" +
                     "create window MyWindow#keepall() as SupportBean;" +
                     "insert into MyWindow select * from SupportBean;" +
                     "on SupportBeanArrayCollMap[intArr@type(DeleteId)] delete from MyWindow where intPrimitive = id";
        DeploymentResult deployed = epService.getEPAdministrator().getDeploymentAdmin().parseDeploy(epl);

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 1));
        epService.getEPRuntime().sendEvent(new SupportBean("E2", 2));

        assertCount(2);
        epService.getEPRuntime().sendEvent(new SupportBeanArrayCollMap(new int[] {1, 2}));
        assertCount(0);

        epService.getEPAdministrator().getDeploymentAdmin().undeploy(deployed.getDeploymentId());
    }

    private void assertCount(long i) {
        assertEquals(i, epService.getEPRuntime().executeQuery("select count(*) as c0 from MyWindow").getArray()[0].get("c0"));
    }
}
