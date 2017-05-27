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
import com.espertech.esper.client.deploy.DeploymentResult;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.bean.SupportBean_S0;
import com.espertech.esper.supportregression.bean.SupportBean_S1;
import com.espertech.esper.supportregression.execution.RegressionExecution;

public class ExecNWTableInfraComparative implements RegressionExecution {

    public void run(EPServiceProvider epService) throws Exception {
        for (Class clazz : new Class[]{SupportBean.class, SupportBean_S0.class, SupportBean_S1.class}) {
            epService.getEPAdministrator().getConfiguration().addEventType(clazz);
        }

        String eplNamedWindow =
                "create window TotalsWindow#unique(theString) as (theString string, total int);" +
                        "insert into TotalsWindow select theString, sum(intPrimitive) as total from SupportBean group by theString;" +
                        "@Name('Listen') select p00 as c0, " +
                        "    (select total from TotalsWindow tw where tw.theString = s0.p00) as c1 from SupportBean_S0 as s0;";
        tryAssertionComparativeGroupByTopLevelSingleAgg(epService, "named window", 1000, eplNamedWindow, 1);

        String eplTable =
                "create table varTotal (key string primary key, total sum(int));\n" +
                        "into table varTotal select theString, sum(intPrimitive) as total from SupportBean group by theString;\n" +
                        "@Name('Listen') select p00 as c0, varTotal[p00].total as c1 from SupportBean_S0;\n";
        tryAssertionComparativeGroupByTopLevelSingleAgg(epService, "table", 1000, eplTable, 1);
    }

    private void tryAssertionComparativeGroupByTopLevelSingleAgg(EPServiceProvider epService, String caseName, int numEvents, String epl, int numSets) throws Exception {
        final String[] fields = "c0,c1".split(",");
        DeploymentResult deployed = epService.getEPAdministrator().getDeploymentAdmin().parseDeploy(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        epService.getEPAdministrator().getStatement("Listen").addListener(listener);

        long startLoad = System.nanoTime();
        for (int i = 0; i < numEvents; i++) {
            epService.getEPRuntime().sendEvent(new SupportBean("E" + i, i));
        }
        long deltaLoad = System.nanoTime() - startLoad;

        long startQuery = System.nanoTime();
        for (int j = 0; j < numSets; j++) {
            for (int i = 0; i < numEvents; i++) {
                String key = "E" + i;
                epService.getEPRuntime().sendEvent(new SupportBean_S0(0, key));
                EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{key, i});
            }
        }
        long deltaQuery = System.nanoTime() - startQuery;

        /** Comment-me-inn:
         System.out.println(caseName + ": Load " + deltaLoad/1000000d +
         " Query " + deltaQuery / 1000000d +
         " Total " + (deltaQuery+deltaLoad) / 1000000d );
         */
        epService.getEPAdministrator().getDeploymentAdmin().undeploy(deployed.getDeploymentId());
    }
}
