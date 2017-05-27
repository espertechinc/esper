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
package com.espertech.esper.regression.epl.fromclausemethod;

import com.espertech.esper.client.Configuration;
import com.espertech.esper.client.ConfigurationMethodRef;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.epl.SupportStaticMethodInvocations;
import com.espertech.esper.supportregression.execution.RegressionExecution;

import static org.junit.Assert.assertEquals;

public class ExecFromClauseMethodCacheLRU implements RegressionExecution {
    public void configure(Configuration configuration) throws Exception {
        ConfigurationMethodRef methodConfig = new ConfigurationMethodRef();
        methodConfig.setLRUCache(3);
        configuration.addMethodRef(SupportStaticMethodInvocations.class.getName(), methodConfig);
        configuration.addImport(SupportStaticMethodInvocations.class.getPackage().getName() + ".*");
    }

    public void run(EPServiceProvider epService) throws Exception {

        SupportUpdateListener listener = new SupportUpdateListener();

        String joinStatement = "select id, p00, theString from " +
                SupportBean.class.getName() + "()#length(100) as s1, " +
                " method:SupportStaticMethodInvocations.fetchObjectLog(theString, intPrimitive)";
        EPStatement stmt = epService.getEPAdministrator().createEPL(joinStatement);
        stmt.addListener(listener);

        // set sleep off
        SupportStaticMethodInvocations.getInvocationSizeReset();

        // The LRU cache caches per same keys
        String[] fields = new String[]{"id", "p00", "theString"};
        sendBeanEvent(epService, "E1", 1);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{1, "|E1|", "E1"});

        sendBeanEvent(epService, "E2", 2);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{2, "|E2|", "E2"});

        sendBeanEvent(epService, "E3", 3);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{3, "|E3|", "E3"});
        assertEquals(3, SupportStaticMethodInvocations.getInvocationSizeReset());

        // should be cached
        sendBeanEvent(epService, "E3", 3);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{3, "|E3|", "E3"});
        assertEquals(0, SupportStaticMethodInvocations.getInvocationSizeReset());

        // should not be cached
        sendBeanEvent(epService, "E4", 4);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{4, "|E4|", "E4"});
        assertEquals(1, SupportStaticMethodInvocations.getInvocationSizeReset());

        // should be cached
        sendBeanEvent(epService, "E2", 2);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{2, "|E2|", "E2"});
        assertEquals(0, SupportStaticMethodInvocations.getInvocationSizeReset());

        // should not be cached
        sendBeanEvent(epService, "E1", 1);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{1, "|E1|", "E1"});
        assertEquals(1, SupportStaticMethodInvocations.getInvocationSizeReset());
    }

    private void sendBeanEvent(EPServiceProvider epService, String theString, int intPrimitive) {
        SupportBean bean = new SupportBean();
        bean.setTheString(theString);
        bean.setIntPrimitive(intPrimitive);
        epService.getEPRuntime().sendEvent(bean);
    }
}
