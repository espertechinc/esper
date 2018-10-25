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
package com.espertech.esper.regressionlib.suite.epl.join;

import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.regressionlib.support.bean.SupportSimpleBeanOne;
import com.espertech.esper.regressionlib.support.bean.SupportSimpleBeanTwo;
import com.espertech.esper.regressionlib.support.util.IndexAssertionEventSend;
import com.espertech.esper.regressionlib.support.util.IndexBackingTableInfo;
import com.espertech.esper.regressionlib.support.util.SupportQueryPlanIndexHook;

import java.util.concurrent.atomic.AtomicInteger;

public class EPLJoinUniqueIndex implements RegressionExecution, IndexBackingTableInfo {

    public void run(RegressionEnvironment env) {
        AtomicInteger milestone = new AtomicInteger();

        // test no where clause with unique on multiple props, exact specification of where-clause
        IndexAssertionEventSend assertSendEvents = new IndexAssertionEventSend() {
            public void run() {
                String[] fields = "ssb1.s1,ssb2.s2".split(",");
                env.sendEventBean(new SupportSimpleBeanTwo("E1", 1, 3, 10));
                env.sendEventBean(new SupportSimpleBeanTwo("E2", 1, 2, 0));
                env.sendEventBean(new SupportSimpleBeanTwo("E3", 1, 3, 9));
                env.sendEventBean(new SupportSimpleBeanOne("EX", 1, 3, 9));
                EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"EX", "E3"});
            }
        };

        CaseEnum[] testCases = CaseEnum.values();
        for (CaseEnum caseEnum : testCases) {
            runAssertion(env, milestone, caseEnum, "d2,i2", "where ssb2.i2 = ssb1.i1 and ssb2.d2 = ssb1.d1", true, assertSendEvents);
            runAssertion(env, milestone, caseEnum, "d2,i2", "where ssb2.d2 = ssb1.d1 and ssb2.i2 = ssb1.i1", true, assertSendEvents);
            runAssertion(env, milestone, caseEnum, "d2,i2", "where ssb2.l2 = ssb1.l1 and ssb2.d2 = ssb1.d1 and ssb2.i2 = ssb1.i1", true, assertSendEvents);
            runAssertion(env, milestone, caseEnum, "d2,i2", "where ssb2.l2 = ssb1.l1 and ssb2.i2 = ssb1.i1", false, assertSendEvents);
            runAssertion(env, milestone, caseEnum, "d2,i2", "where ssb2.d2 = ssb1.d1", false, assertSendEvents);
            runAssertion(env, milestone, caseEnum, "d2,i2", "where ssb2.i2 = ssb1.i1 and ssb2.d2 = ssb1.d1 and ssb2.l2 between 1 and 1000", true, assertSendEvents);
            runAssertion(env, milestone, caseEnum, "d2,i2", "where ssb2.d2 = ssb1.d1 and ssb2.l2 between 1 and 1000", false, assertSendEvents);
            runAssertion(env, milestone, caseEnum, "i2,d2,l2", "where ssb2.l2 = ssb1.l1 and ssb2.d2 = ssb1.d1", false, assertSendEvents);
            runAssertion(env, milestone, caseEnum, "i2,d2,l2", "where ssb2.l2 = ssb1.l1 and ssb2.i2 = ssb1.i1 and ssb2.d2 = ssb1.d1", true, assertSendEvents);
            runAssertion(env, milestone, caseEnum, "d2,l2,i2", "where ssb2.l2 = ssb1.l1 and ssb2.i2 = ssb1.i1 and ssb2.d2 = ssb1.d1", true, assertSendEvents);
            runAssertion(env, milestone, caseEnum, "d2,l2,i2", "where ssb2.l2 = ssb1.l1 and ssb2.i2 = ssb1.i1 and ssb2.d2 = ssb1.d1 and ssb2.s2 between 'E3' and 'E4'", true, assertSendEvents);
            runAssertion(env, milestone, caseEnum, "l2", "where ssb2.l2 = ssb1.l1", true, assertSendEvents);
            runAssertion(env, milestone, caseEnum, "l2", "where ssb2.l2 = ssb1.l1 and ssb1.i1 between 1 and 20", true, assertSendEvents);
        }
    }

    private void runAssertion(RegressionEnvironment env, AtomicInteger milestone, CaseEnum caseEnum, String uniqueFields, String whereClause, boolean unique, IndexAssertionEventSend assertion) {
        SupportQueryPlanIndexHook.reset();
        String eplUnique = INDEX_CALLBACK_HOOK +
            "@name('s0') select * from ";

        if (caseEnum == CaseEnum.UNIDIRECTIONAL || caseEnum == CaseEnum.UNIDIRECTIONAL_3STREAM) {
            eplUnique += "SupportSimpleBeanOne as ssb1 unidirectional ";
        } else {
            eplUnique += "SupportSimpleBeanOne#lastevent as ssb1 ";
        }
        eplUnique += ", SupportSimpleBeanTwo#unique(" + uniqueFields + ") as ssb2 ";
        if (caseEnum == CaseEnum.UNIDIRECTIONAL_3STREAM || caseEnum == CaseEnum.MULTIDIRECTIONAL_3STREAM) {
            eplUnique += ", SupportBean#lastevent ";
        }
        eplUnique += whereClause;

        env.compileDeployAddListenerMile(eplUnique, "s0", milestone.getAndIncrement());

        SupportQueryPlanIndexHook.assertJoinOneStreamAndReset(unique);

        env.sendEventBean(new SupportBean("JOINEVENT", 1));
        assertion.run();

        env.undeployAll();
    }

    private static enum CaseEnum {
        UNIDIRECTIONAL,
        MULTIDIRECTIONAL,
        UNIDIRECTIONAL_3STREAM,
        MULTIDIRECTIONAL_3STREAM
    }

}
