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
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.execution.RegressionExecution;

public class ExecInsertIntoWrapper implements RegressionExecution {
    public void run(EPServiceProvider epService) throws Exception {
        epService.getEPAdministrator().getConfiguration().addEventType(SupportBean.class);
        epService.getEPAdministrator().getConfiguration().addEventType(MyEventContainsSupportBean.class);

        runAssertionWrappedBean(epService);
    }

    private void runAssertionWrappedBean(EPServiceProvider epService) {
        SupportUpdateListener listenerOne = new SupportUpdateListener();
        EPStatement stmtOne = epService.getEPAdministrator().createEPL("insert into WrappedBean select *, intPrimitive as p0 from SupportBean");
        stmtOne.addListener(listenerOne);

        SupportUpdateListener listenerTwo = new SupportUpdateListener();
        EPStatement stmtTwo = epService.getEPAdministrator().createEPL("insert into WrappedBean select sb from MyEventContainsSupportBean sb");
        stmtTwo.addListener(listenerTwo);

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 1));
        EPAssertionUtil.assertProps(listenerOne.assertOneGetNewAndReset(), "theString,intPrimitive,p0".split(","), new Object[] {"E1", 1, 1});

        epService.getEPRuntime().sendEvent(new MyEventContainsSupportBean(new SupportBean("E2", 2)));
        EPAssertionUtil.assertProps(listenerTwo.assertOneGetNewAndReset(), "theString,intPrimitive,p0".split(","), new Object[] {"E2", 2, null});

        epService.getEPAdministrator().destroyAllStatements();
    }

    public final static class MyEventContainsSupportBean {
        private final SupportBean sb;

        public MyEventContainsSupportBean(SupportBean sb) {
            this.sb = sb;
        }

        public SupportBean getSb() {
            return sb;
        }
    }
}
