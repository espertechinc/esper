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

import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import junit.framework.TestCase;
import com.espertech.esper.client.*;
import com.espertech.esper.support.bean.SupportBean;
import com.espertech.esper.support.bean.SupportMarketDataBean;
import com.espertech.esper.support.client.SupportConfigFactory;

public class TestSubscriberInvalid extends TestCase
{
    private EPServiceProvider epService;
    private EPAdministrator epAdmin;

    public void setUp()
    {
        Configuration config = SupportConfigFactory.getConfiguration();
        String pkg = SupportBean.class.getPackage().getName();
        config.addEventTypeAutoName(pkg);
        epService = EPServiceProviderManager.getDefaultProvider(config);
        epService.initialize();
        epAdmin = epService.getEPAdministrator();
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.startTest(epService, this.getClass(), getName());}
    }

    public void tearDown() {
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.endTest();}
        epAdmin = null;
    }

    public void testBindWildcardJoin()
    {
        EPStatement stmt = epAdmin.createEPL("select * from SupportBean");
        tryInvalid(this, stmt, "Subscriber object does not provide a public method by name 'update'");
        tryInvalid(new DummySubscriberEmptyUpd(), stmt, "No suitable subscriber method named 'update' found, expecting a method that takes 1 parameter of type SupportBean");
        tryInvalid(new DummySubscriberMultipleUpdate(), stmt, "No suitable subscriber method named 'update' found, expecting a method that takes 1 parameter of type SupportBean");
        tryInvalid(new DummySubscriberUpdate(), stmt, "Subscriber method named 'update' for parameter number 1 is not assignable, expecting type 'SupportBean' but found type 'SupportMarketDataBean'");
        tryInvalid(new DummySubscriberPrivateUpd(), stmt, "Subscriber object does not provide a public method by name 'update'");
    }

    public void testInvocationTargetEx()
    {
        // smoke test, need to consider log file; test for ESPER-331 
        EPStatement stmt = epAdmin.createEPL("select * from SupportMarketDataBean");
        stmt.setSubscriber(new DummySubscriberException());
        stmt.addListener(new UpdateListener() {
            public void update(EventBean[] newEvents, EventBean[] oldEvents)
            {
                throw new RuntimeException("test exception 1");
            }
        });
        stmt.addListener(new StatementAwareUpdateListener()
        {
            public void update(EventBean[] newEvents, EventBean[] oldEvents, EPStatement statement, EPServiceProvider epServiceProvider)
            {
                throw new RuntimeException("test exception 2");
            }
        });
        stmt.addListenerWithReplay(new UpdateListener()
        {
            public void update(EventBean[] newEvents, EventBean[] oldEvents)
            {
                throw new RuntimeException("test exception 3");
            }
        });

        // no exception expected
        epService.getEPRuntime().sendEvent(new SupportMarketDataBean("IBM", 0, 0L, ""));
    }

    private void tryInvalid(Object subscriber, EPStatement stmt, String message)
    {
        try
        {
            stmt.setSubscriber(subscriber);
            fail();
        }
        catch (EPSubscriberException ex)
        {
            assertEquals(message, ex.getMessage());
        }
    }

    public class DummySubscriberException
    {
        public void update(SupportMarketDataBean bean) {
            throw new RuntimeException("DummySubscriberException-generated");
        }
    }

    public class DummySubscriberEmptyUpd
    {
        public void update() {}
    }

    public class DummySubscriberPrivateUpd
    {
        private void update(SupportBean bean) {}
    }

    public class DummySubscriberUpdate
    {
        public void update(SupportMarketDataBean dummy) {}
    }

    public class DummySubscriberMultipleUpdate
    {
        public void update(long x) {}
        public void update(int x) {}
    }
}
