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
package com.espertech.esper.regression.epl.join;

import com.espertech.esper.client.Configuration;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.bean.SupportSimpleBeanOne;
import com.espertech.esper.supportregression.bean.SupportSimpleBeanTwo;
import com.espertech.esper.supportregression.epl.SupportQueryPlanIndexHook;
import com.espertech.esper.supportregression.execution.RegressionExecution;
import com.espertech.esper.supportregression.util.IndexAssertionEventSend;
import com.espertech.esper.supportregression.util.IndexBackingTableInfo;

public class ExecJoinUniqueIndex implements RegressionExecution, IndexBackingTableInfo {
    public void configure(Configuration configuration) throws Exception {
        configuration.getEngineDefaults().getLogging().setEnableQueryPlan(true);
    }

    public void run(EPServiceProvider epService) throws Exception {
        epService.getEPAdministrator().getConfiguration().addEventType("SSB1", SupportSimpleBeanOne.class);
        epService.getEPAdministrator().getConfiguration().addEventType("SSB2", SupportSimpleBeanTwo.class);
        epService.getEPAdministrator().getConfiguration().addEventType(SupportBean.class);
        SupportUpdateListener listener = new SupportUpdateListener();

        // test no where clause with unique on multiple props, exact specification of where-clause
        IndexAssertionEventSend assertSendEvents = new IndexAssertionEventSend() {
            public void run() {
                String[] fields = "ssb1.s1,ssb2.s2".split(",");
                epService.getEPRuntime().sendEvent(new SupportSimpleBeanTwo("E1", 1, 3, 10));
                epService.getEPRuntime().sendEvent(new SupportSimpleBeanTwo("E2", 1, 2, 0));
                epService.getEPRuntime().sendEvent(new SupportSimpleBeanTwo("E3", 1, 3, 9));
                epService.getEPRuntime().sendEvent(new SupportSimpleBeanOne("EX", 1, 3, 9));
                EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"EX", "E3"});
            }
        };

        CaseEnum[] testCases = CaseEnum.values();
        for (CaseEnum caseEnum : testCases) {
            runAssertion(epService, listener, caseEnum, "d2,i2", "where ssb2.i2 = ssb1.i1 and ssb2.d2 = ssb1.d1", true, assertSendEvents);
            runAssertion(epService, listener, caseEnum, "d2,i2", "where ssb2.d2 = ssb1.d1 and ssb2.i2 = ssb1.i1", true, assertSendEvents);
            runAssertion(epService, listener, caseEnum, "d2,i2", "where ssb2.l2 = ssb1.l1 and ssb2.d2 = ssb1.d1 and ssb2.i2 = ssb1.i1", true, assertSendEvents);
            runAssertion(epService, listener, caseEnum, "d2,i2", "where ssb2.l2 = ssb1.l1 and ssb2.i2 = ssb1.i1", false, assertSendEvents);
            runAssertion(epService, listener, caseEnum, "d2,i2", "where ssb2.d2 = ssb1.d1", false, assertSendEvents);
            runAssertion(epService, listener, caseEnum, "d2,i2", "where ssb2.i2 = ssb1.i1 and ssb2.d2 = ssb1.d1 and ssb2.l2 between 1 and 1000", true, assertSendEvents);
            runAssertion(epService, listener, caseEnum, "d2,i2", "where ssb2.d2 = ssb1.d1 and ssb2.l2 between 1 and 1000", false, assertSendEvents);
            runAssertion(epService, listener, caseEnum, "i2,d2,l2", "where ssb2.l2 = ssb1.l1 and ssb2.d2 = ssb1.d1", false, assertSendEvents);
            runAssertion(epService, listener, caseEnum, "i2,d2,l2", "where ssb2.l2 = ssb1.l1 and ssb2.i2 = ssb1.i1 and ssb2.d2 = ssb1.d1", true, assertSendEvents);
            runAssertion(epService, listener, caseEnum, "d2,l2,i2", "where ssb2.l2 = ssb1.l1 and ssb2.i2 = ssb1.i1 and ssb2.d2 = ssb1.d1", true, assertSendEvents);
            runAssertion(epService, listener, caseEnum, "d2,l2,i2", "where ssb2.l2 = ssb1.l1 and ssb2.i2 = ssb1.i1 and ssb2.d2 = ssb1.d1 and ssb2.s2 between 'E3' and 'E4'", true, assertSendEvents);
            runAssertion(epService, listener, caseEnum, "l2", "where ssb2.l2 = ssb1.l1", true, assertSendEvents);
            runAssertion(epService, listener, caseEnum, "l2", "where ssb2.l2 = ssb1.l1 and ssb1.i1 between 1 and 20", true, assertSendEvents);
        }
    }

    private void runAssertion(EPServiceProvider epService, SupportUpdateListener listener, CaseEnum caseEnum, String uniqueFields, String whereClause, boolean unique, IndexAssertionEventSend assertion) {
        SupportQueryPlanIndexHook.reset();
        String eplUnique = INDEX_CALLBACK_HOOK +
                "select * from ";

        if (caseEnum == CaseEnum.UNIDIRECTIONAL || caseEnum == CaseEnum.UNIDIRECTIONAL_3STREAM) {
            eplUnique += "SSB1 as ssb1 unidirectional ";
        } else {
            eplUnique += "SSB1#lastevent as ssb1 ";
        }
        eplUnique += ", SSB2#unique(" + uniqueFields + ") as ssb2 ";
        if (caseEnum == CaseEnum.UNIDIRECTIONAL_3STREAM || caseEnum == CaseEnum.MULTIDIRECTIONAL_3STREAM) {
            eplUnique += ", SupportBean#lastevent ";
        }
        eplUnique += whereClause;

        EPStatement stmtUnique = epService.getEPAdministrator().createEPL(eplUnique);
        stmtUnique.addListener(listener);

        SupportQueryPlanIndexHook.assertJoinOneStreamAndReset(unique);

        epService.getEPRuntime().sendEvent(new SupportBean("JOINEVENT", 1));
        assertion.run();

        stmtUnique.destroy();
    }

    private static enum CaseEnum {
        UNIDIRECTIONAL,
        MULTIDIRECTIONAL,
        UNIDIRECTIONAL_3STREAM,
        MULTIDIRECTIONAL_3STREAM
    }

}
