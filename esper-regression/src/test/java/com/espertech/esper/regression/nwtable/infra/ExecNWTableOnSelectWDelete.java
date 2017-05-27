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
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.client.soda.EPStatementObjectModel;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.bean.SupportBean_S0;
import com.espertech.esper.supportregression.execution.RegressionExecution;
import com.espertech.esper.supportregression.util.IndexBackingTableInfo;

import static org.junit.Assert.assertEquals;

public class ExecNWTableOnSelectWDelete implements RegressionExecution, IndexBackingTableInfo {
    public void run(EPServiceProvider epService) throws Exception {
        epService.getEPAdministrator().getConfiguration().addEventType("SupportBean", SupportBean.class);
        epService.getEPAdministrator().getConfiguration().addEventType("S0", SupportBean_S0.class);

        runAssertionWindowAgg(epService, true);
        runAssertionWindowAgg(epService, false);
    }

    private void runAssertionWindowAgg(EPServiceProvider epService, boolean namedWindow) {

        String[] fieldsWin = "theString,intPrimitive".split(",");
        String[] fieldsSelect = "c0".split(",");

        String eplCreate = namedWindow ?
                "create window MyInfra#keepall as SupportBean" :
                "create table MyInfra (theString string primary key, intPrimitive int primary key)";
        EPStatement stmtWin = epService.getEPAdministrator().createEPL(eplCreate);
        epService.getEPAdministrator().createEPL("insert into MyInfra select theString, intPrimitive from SupportBean");
        String eplSelectDelete = "on S0 as s0 " +
                "select and delete window(win.*).aggregate(0,(result,value) => result+value.intPrimitive) as c0 " +
                "from MyInfra as win where s0.p00=win.theString";
        EPStatement stmt = epService.getEPAdministrator().createEPL(eplSelectDelete);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 1));
        epService.getEPRuntime().sendEvent(new SupportBean("E2", 2));
        if (namedWindow) {
            EPAssertionUtil.assertPropsPerRow(stmtWin.iterator(), fieldsWin, new Object[][]{{"E1", 1}, {"E2", 2}});
        } else {
            EPAssertionUtil.assertPropsPerRowAnyOrder(stmtWin.iterator(), fieldsWin, new Object[][]{{"E1", 1}, {"E2", 2}});
        }

        // select and delete bean E1
        epService.getEPRuntime().sendEvent(new SupportBean_S0(100, "E1"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fieldsSelect, new Object[]{1});
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmtWin.iterator(), fieldsWin, new Object[][]{{"E2", 2}});

        // add some E2 events
        epService.getEPRuntime().sendEvent(new SupportBean("E2", 3));
        epService.getEPRuntime().sendEvent(new SupportBean("E2", 4));
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmtWin.iterator(), fieldsWin, new Object[][]{{"E2", 2}, {"E2", 3}, {"E2", 4}});

        // select and delete beans E2
        epService.getEPRuntime().sendEvent(new SupportBean_S0(101, "E2"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fieldsSelect, new Object[]{2 + 3 + 4});
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmtWin.iterator(), fieldsWin, new Object[0][]);

        // test SODA
        EPStatementObjectModel model = epService.getEPAdministrator().compileEPL(eplSelectDelete);
        assertEquals(eplSelectDelete, model.toEPL());
        EPStatement stmtSD = epService.getEPAdministrator().create(model);
        assertEquals(eplSelectDelete, stmtSD.getText());

        epService.getEPAdministrator().destroyAllStatements();
        epService.getEPAdministrator().getConfiguration().removeEventType("MyInfra", false);
    }
}
