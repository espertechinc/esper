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
package com.espertech.esper.regression.rowrecog;

import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.supportregression.bean.SupportBean_S0;
import com.espertech.esper.supportregression.bean.SupportBean_S1;
import com.espertech.esper.supportregression.execution.RegressionExecution;

public class ExecRowRecogVariantStream implements RegressionExecution {

    public void run(EPServiceProvider epService) throws Exception {
        epService.getEPAdministrator().createEPL("create schema S0 as " + SupportBean_S0.class.getName());
        epService.getEPAdministrator().createEPL("create schema S1 as " + SupportBean_S1.class.getName());
        epService.getEPAdministrator().createEPL("create variant schema MyVariantType as S0, S1");

        String[] fields = "a,b".split(",");
        String text = "select * from MyVariantType#keepall " +
                "match_recognize (" +
                "  measures A.id? as a, B.id? as b" +
                "  pattern (A B) " +
                "  define " +
                "    A as typeof(A) = 'S0'," +
                "    B as typeof(B) = 'S1'" +
                ")";

        EPStatement stmt = epService.getEPAdministrator().createEPL(text);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        epService.getEPAdministrator().createEPL("insert into MyVariantType select * from S0");
        epService.getEPAdministrator().createEPL("insert into MyVariantType select * from S1");

        epService.getEPRuntime().sendEvent(new SupportBean_S0(1, "S0"));
        epService.getEPRuntime().sendEvent(new SupportBean_S1(2, "S1"));
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields,
                new Object[][]{{1, 2}});
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields,
                new Object[][]{{1, 2}});

        String epl = "// Declare one sample type\n" +
                "create schema ST0 as (col string)\n;" +
                "// Declare second sample type\n" +
                "create schema ST1 as (col string)\n;" +
                "// Declare variant stream holding either type\n" +
                "create variant schema MyVariantStream as ST0, ST1\n;" +
                "// Populate variant stream\n" +
                "insert into MyVariantStream select * from ST0\n;" +
                "// Populate variant stream\n" +
                "insert into MyVariantStream select * from ST1\n;" +
                "// Simple pattern to match ST0 ST1 pairs\n" +
                "select * from MyVariantType#time(1 min)\n" +
                "match_recognize (\n" +
                "measures A.id? as a, B.id? as b\n" +
                "pattern (A B)\n" +
                "define\n" +
                "A as typeof(A) = 'ST0',\n" +
                "B as typeof(B) = 'ST1'\n" +
                ");";
        epService.getEPAdministrator().getDeploymentAdmin().parseDeploy(epl);
    }
}