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
package com.espertech.esper.regression.epl.insertinto;

import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.client.soda.EPStatementObjectModel;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.bean.SupportBean_S0;
import com.espertech.esper.supportregression.execution.RegressionExecution;

import static org.junit.Assert.assertEquals;

public class ExecInsertIntoIRStreamFunc implements RegressionExecution {
    public void run(EPServiceProvider epService) throws Exception {
        epService.getEPAdministrator().getConfiguration().addEventType(SupportBean.class);
        epService.getEPAdministrator().getConfiguration().addEventType(SupportBean_S0.class);

        SupportUpdateListener listenerInsert = new SupportUpdateListener();
        SupportUpdateListener listenerSelect = new SupportUpdateListener();

        String[] fields = "c0,c1".split(",");
        String stmtTextOne = "insert irstream into MyStream " +
                "select irstream theString as c0, istream() as c1 " +
                "from SupportBean#lastevent";
        epService.getEPAdministrator().createEPL(stmtTextOne).addListener(listenerInsert);

        String stmtTextTwo = "select * from MyStream";
        epService.getEPAdministrator().createEPL(stmtTextTwo).addListener(listenerSelect);

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 0));
        EPAssertionUtil.assertProps(listenerInsert.assertOneGetNewAndReset(), fields, new Object[]{"E1", true});
        EPAssertionUtil.assertProps(listenerSelect.assertOneGetNewAndReset(), fields, new Object[]{"E1", true});

        epService.getEPRuntime().sendEvent(new SupportBean("E2", 0));
        EPAssertionUtil.assertProps(listenerInsert.assertPairGetIRAndReset(), fields, new Object[]{"E2", true}, new Object[]{"E1", false});
        EPAssertionUtil.assertPropsPerRow(listenerSelect.getAndResetDataListsFlattened(), fields, new Object[][]{{"E2", true}, {"E1", false}}, new Object[0][]);

        epService.getEPRuntime().sendEvent(new SupportBean("E3", 0));
        EPAssertionUtil.assertProps(listenerInsert.assertPairGetIRAndReset(), fields, new Object[]{"E3", true}, new Object[]{"E2", false});
        EPAssertionUtil.assertPropsPerRow(listenerSelect.getAndResetDataListsFlattened(), fields, new Object[][]{{"E3", true}, {"E2", false}}, new Object[0][]);

        // test SODA
        String eplModel = "select istream() from SupportBean";
        EPStatementObjectModel model = epService.getEPAdministrator().compileEPL(eplModel);
        assertEquals(eplModel, model.toEPL());
        EPStatement stmt = epService.getEPAdministrator().create(model);
        assertEquals(eplModel, stmt.getText());
        assertEquals(Boolean.class, stmt.getEventType().getPropertyType("istream()"));

        // test join
        epService.getEPAdministrator().destroyAllStatements();
        fields = "c0,c1,c2".split(",");
        String stmtTextJoin = "select irstream theString as c0, id as c1, istream() as c2 " +
                "from SupportBean#lastevent, SupportBean_S0#lastevent";
        epService.getEPAdministrator().createEPL(stmtTextJoin).addListener(listenerSelect);
        epService.getEPRuntime().sendEvent(new SupportBean("E1", 0));
        epService.getEPRuntime().sendEvent(new SupportBean_S0(10));
        EPAssertionUtil.assertProps(listenerSelect.assertOneGetNewAndReset(), fields, new Object[]{"E1", 10, true});

        epService.getEPRuntime().sendEvent(new SupportBean("E2", 0));
        EPAssertionUtil.assertProps(listenerSelect.getLastOldData()[0], fields, new Object[]{"E1", 10, false});
        EPAssertionUtil.assertProps(listenerSelect.getLastNewData()[0], fields, new Object[]{"E2", 10, true});
    }
}
