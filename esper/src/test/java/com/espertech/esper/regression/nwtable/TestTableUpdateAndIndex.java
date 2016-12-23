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

package com.espertech.esper.regression.nwtable;

import com.espertech.esper.client.*;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.bean.SupportBean_S0;
import com.espertech.esper.supportregression.bean.SupportBean_S1;
import com.espertech.esper.supportregression.client.SupportConfigFactory;
import com.espertech.esper.supportregression.util.SupportMessageAssertUtil;
import junit.framework.TestCase;

public class TestTableUpdateAndIndex extends TestCase {

    private EPServiceProvider epService;

    public void setUp() {
        epService = EPServiceProviderManager.getDefaultProvider(SupportConfigFactory.getConfiguration());
        epService.initialize();
        for (Class clazz : new Class[] {SupportBean.class, SupportBean_S0.class, SupportBean_S1.class}) {
            epService.getEPAdministrator().getConfiguration().addEventType(clazz);
        }
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.startTest(epService, this.getClass(), getName());}
    }

    public void tearDown() {
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.endTest();}
    }

    public void testEarlyUniqueIndexViolation() {
        EPStatement stmtCreate = epService.getEPAdministrator().createEPL("create table MyTable as (pkey0 string primary key, pkey1 int primary key, thecnt count(*))");

        epService.getEPAdministrator().createEPL("into table MyTable select count(*) as thecnt from SupportBean group by theString, intPrimitive");
        epService.getEPRuntime().sendEvent(new SupportBean("E1", 10));
        epService.getEPRuntime().sendEvent(new SupportBean("E1", 20));

        // invalid index being created
        SupportMessageAssertUtil.tryInvalid(epService, "create unique index SecIndex on MyTable(pkey0)",
                "Unexpected exception starting statement: Unique index violation, index 'SecIndex' is a unique index and key 'E1' already exists [create unique index SecIndex on MyTable(pkey0)]");

        // try fire-and-forget update of primary key to non-unique value
        try {
            epService.getEPRuntime().executeQuery("update MyTable set pkey1 = 0");
            fail();
        }
        catch (EPException ex) {
            SupportMessageAssertUtil.assertMessage(ex, "Error executing statement: Unique index violation, index 'primary-MyTable' is a unique index and key 'MultiKeyUntyped[E1, 0]' already exists [");
            // assert events are unchanged - no update actually performed
            EPAssertionUtil.assertPropsPerRowAnyOrder(stmtCreate.iterator(), "pkey0,pkey1".split(","), new Object[][]{{"E1", 10}, {"E1", 20}});
        }

        // try on-update unique index violation
        epService.getEPAdministrator().createEPL("@name('on-update') on SupportBean_S1 update MyTable set pkey1 = 0");
        try {
            epService.getEPRuntime().sendEvent(new SupportBean_S1(0));
            fail();
        }
        catch (EPException ex) {
            SupportMessageAssertUtil.assertMessage(ex.getCause(), "Unexpected exception in statement 'on-update': Unique index violation, index 'primary-MyTable' is a unique index and key 'MultiKeyUntyped[E1, 0]' already exists");
            // assert events are unchanged - no update actually performed
            EPAssertionUtil.assertPropsPerRowAnyOrder(stmtCreate.iterator(), "pkey0,pkey1".split(","), new Object[][]{{"E1", 10}, {"E1", 20}});
        }

        // disallow on-merge unique key updates
        try {
            epService.getEPAdministrator().createEPL("@name('on-merge') on SupportBean_S1 merge MyTable when matched then update set pkey1 = 0");
            fail();
        }
        catch (EPStatementException ex) {
            SupportMessageAssertUtil.assertMessage(ex.getCause(), "Validation failed in when-matched (clause 1): On-merge statements may not update unique keys of tables");
        }
    }

    public void testLateUniqueIndexViolation() {
        EPStatement stmtCreate = epService.getEPAdministrator().createEPL("create table MyTable as (" +
                "pkey0 string primary key, " +
                "pkey1 int primary key, " +
                "col0 int, " +
                "thecnt count(*))");

        epService.getEPAdministrator().createEPL("into table MyTable select count(*) as thecnt from SupportBean group by theString, intPrimitive");
        epService.getEPRuntime().sendEvent(new SupportBean("E1", 10));
        epService.getEPRuntime().sendEvent(new SupportBean("E2", 20));

        // On-merge exists before creating a unique index
        EPStatement onMerge = epService.getEPAdministrator().createEPL("@name('on-merge') on SupportBean_S1 merge MyTable " +
                "when matched then update set col0 = 0");
        try {
            epService.getEPAdministrator().createEPL("create unique index MyUniqueSecondary on MyTable (col0)");
            fail();
        }
        catch (EPStatementException ex) {
            SupportMessageAssertUtil.assertMessage(ex, "Error starting statement: Failed to validate statement 'on-merge' as a recipient of the proposed index: On-merge statements may not update unique keys of tables [");
        }
        onMerge.destroy();

        // on-update exists before creating a unique index
        EPStatement stmtUpdate = epService.getEPAdministrator().createEPL("@name('on-update') on SupportBean_S1 update MyTable set pkey1 = 0");
        epService.getEPAdministrator().createEPL("create unique index MyUniqueSecondary on MyTable (pkey1)");
        try {
            epService.getEPRuntime().sendEvent(new SupportBean_S1(0));
            fail();
        }
        catch (EPException ex) {
            SupportMessageAssertUtil.assertMessage(ex.getCause(), "Unexpected exception in statement 'on-update': Unique index violation, index 'MyUniqueSecondary' is a unique index and key '0' already exists");
            // assert events are unchanged - no update actually performed
            EPAssertionUtil.assertPropsPerRowAnyOrder(stmtCreate.iterator(), "pkey0,pkey1".split(","), new Object[][]{{"E1", 10}, {"E2", 20}});
        }

        // unregister
        stmtUpdate.destroy();
    }

    public void testFAFUpdate() {
        epService.getEPAdministrator().createEPL("create table MyTable as (pkey0 string primary key, col0 int, col1 int, thecnt count(*))");
        epService.getEPAdministrator().createEPL("create index MyIndex on MyTable(col0)");

        epService.getEPAdministrator().createEPL("into table MyTable select count(*) as thecnt from SupportBean group by theString");
        epService.getEPRuntime().sendEvent(new SupportBean("E1", 0));
        epService.getEPRuntime().sendEvent(new SupportBean("E2", 0));

        epService.getEPRuntime().executeQuery("update MyTable set col0 = 1 where pkey0='E1'");
        epService.getEPRuntime().executeQuery("update MyTable set col0 = 2 where pkey0='E2'");
        assertFAFOneRowResult("select pkey0 from MyTable where col0=1", "pkey0", new Object[]{"E1"});

        epService.getEPRuntime().executeQuery("update MyTable set col1 = 100 where pkey0='E1'");
        assertFAFOneRowResult("select pkey0 from MyTable where col1=100", "pkey0", new Object[]{"E1"});
    }

    private void assertFAFOneRowResult(String epl, String fields, Object[] objects) {
        EPOnDemandQueryResult result = epService.getEPRuntime().executeQuery(epl);
        assertEquals(1, result.getArray().length);
        EPAssertionUtil.assertProps(result.getArray()[0], fields.split(","), objects);
    }
}
