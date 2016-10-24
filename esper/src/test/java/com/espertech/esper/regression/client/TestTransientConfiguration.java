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

package com.espertech.esper.regression.client;

import com.espertech.esper.client.*;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.support.bean.SupportBean;
import com.espertech.esper.support.client.SupportConfigFactory;
import junit.framework.TestCase;

import java.util.HashMap;

public class TestTransientConfiguration extends TestCase
{
    private final static String SERVICE_NAME = "TEST_SERVICE_NAME";
    private final static int SECRET_VALUE = 12345;

    private EPServiceProvider epService;

    public void setUp()
    {
        Configuration configuration = SupportConfigFactory.getConfiguration();
        configuration.addEventType(SupportBean.class);

        // add service (not serializable, transient configuration)
        HashMap<String, Object> transients = new HashMap<String, Object>();
        transients.put(SERVICE_NAME, new MyLocalService(SECRET_VALUE));
        configuration.setTransientConfiguration(transients);

        epService = EPServiceProviderManager.getDefaultProvider(configuration);
        epService.initialize();
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.startTest(epService, this.getClass(), getName());}
    }

    protected void tearDown() throws Exception {
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.endTest();}
    }

    public void testTransient()
    {
        EPStatement stmt = epService.getEPAdministrator().createEPL("select * from SupportBean");
        MyListener listener = new MyListener();
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean());
        assertEquals(SECRET_VALUE, listener.getSecretValue());
    }

    public static class MyLocalService {
        private final int secretValue;

        public MyLocalService(int secretValue) {
            this.secretValue = secretValue;
        }

        public int getSecretValue() {
            return secretValue;
        }
    }

    public static class MyListener implements StatementAwareUpdateListener {
        private int secretValue;

        public void update(EventBean[] newEvents, EventBean[] oldEvents, EPStatement statement, EPServiceProvider epServiceProvider) {
            MyLocalService svc = (MyLocalService) epServiceProvider.getEPAdministrator().getConfiguration().getTransientConfiguration().get(SERVICE_NAME);
            secretValue = svc.getSecretValue();
        }

        public int getSecretValue() {
            return secretValue;
        }
    }
}
