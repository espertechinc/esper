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
package com.espertech.esper.regression.nwtable.tbl;

import com.espertech.esper.client.*;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.bean.SupportBean_S0;
import com.espertech.esper.supportregression.bean.SupportBean_S1;
import com.espertech.esper.supportregression.execution.RegressionExecution;
import com.espertech.esper.supportregression.util.SupportMessageAssertUtil;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class ExecTableUpdateAndIndex implements RegressionExecution {

    public void run(EPServiceProvider epService) throws Exception {
        for (Class clazz : new Class[]{SupportBean.class, SupportBean_S0.class, SupportBean_S1.class}) {
            epService.getEPAdministrator().getConfiguration().addEventType(clazz);
        }

        runAssertionEarlyUniqueIndexViolation(epService);
        runAssertionLateUniqueIndexViolation(epService);
        runAssertionFAFUpdate(epService);
    }

    private void runAssertionEarlyUniqueIndexViolation(EPServiceProvider epService) {
        EPStatement stmtCreate = epService.getEPAdministrator().createEPL("create table MyTableEUIV as (pkey0 string primary key, pkey1 int primary key, thecnt count(*))");

        epService.getEPAdministrator().createEPL("into table MyTableEUIV select count(*) as thecnt from SupportBean group by theString, intPrimitive");
        epService.getEPRuntime().sendEvent(new SupportBean("E1", 10));
        epService.getEPRuntime().sendEvent(new SupportBean("E1", 20));

        // invalid index being created
        SupportMessageAssertUtil.tryInvalid(epService, "create unique index SecIndex on MyTableEUIV(pkey0)",
                "Unexpected exception starting statement: Unique index violation, index 'SecIndex' is a unique index and key 'E1' already exists [create unique index SecIndex on MyTableEUIV(pkey0)]");

        // try fire-and-forget update of primary key to non-unique value
        try {
            epService.getEPRuntime().executeQuery("update MyTableEUIV set pkey1 = 0");
            fail();
        } catch (EPException ex) {
            SupportMessageAssertUtil.assertMessage(ex, "Error executing statement: Unique index violation, index 'primary-MyTableEUIV' is a unique index and key 'MultiKeyUntyped[E1, 0]' already exists [");
            // assert events are unchanged - no update actually performed
            EPAssertionUtil.assertPropsPerRowAnyOrder(stmtCreate.iterator(), "pkey0,pkey1".split(","), new Object[][]{{"E1", 10}, {"E1", 20}});
        }

        // try on-update unique index violation
        epService.getEPAdministrator().createEPL("@name('on-update') on SupportBean_S1 update MyTableEUIV set pkey1 = 0");
        try {
            epService.getEPRuntime().sendEvent(new SupportBean_S1(0));
            fail();
        } catch (EPException ex) {
            SupportMessageAssertUtil.assertMessage(ex.getCause(), "Unexpected exception in statement 'on-update': Unique index violation, index 'primary-MyTableEUIV' is a unique index and key 'MultiKeyUntyped[E1, 0]' already exists");
            // assert events are unchanged - no update actually performed
            EPAssertionUtil.assertPropsPerRowAnyOrder(stmtCreate.iterator(), "pkey0,pkey1".split(","), new Object[][]{{"E1", 10}, {"E1", 20}});
        }

        // disallow on-merge unique key updates
        try {
            epService.getEPAdministrator().createEPL("@name('on-merge') on SupportBean_S1 merge MyTableEUIV when matched then update set pkey1 = 0");
            fail();
        } catch (EPStatementException ex) {
            SupportMessageAssertUtil.assertMessage(ex.getCause(), "Validation failed in when-matched (clause 1): On-merge statements may not update unique keys of tables");
        }

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionLateUniqueIndexViolation(EPServiceProvider epService) {
        EPStatement stmtCreate = epService.getEPAdministrator().createEPL("create table MyTableLUIV as (" +
                "pkey0 string primary key, " +
                "pkey1 int primary key, " +
                "col0 int, " +
                "thecnt count(*))");

        epService.getEPAdministrator().createEPL("into table MyTableLUIV select count(*) as thecnt from SupportBean group by theString, intPrimitive");
        epService.getEPRuntime().sendEvent(new SupportBean("E1", 10));
        epService.getEPRuntime().sendEvent(new SupportBean("E2", 20));

        // On-merge exists before creating a unique index
        EPStatement onMerge = epService.getEPAdministrator().createEPL("@name('on-merge') on SupportBean_S1 merge MyTableLUIV " +
                "when matched then update set col0 = 0");
        try {
            epService.getEPAdministrator().createEPL("create unique index MyUniqueSecondary on MyTableLUIV (col0)");
            fail();
        } catch (EPStatementException ex) {
            SupportMessageAssertUtil.assertMessage(ex, "Error starting statement: Failed to validate statement 'on-merge' as a recipient of the proposed index: On-merge statements may not update unique keys of tables [");
        }
        onMerge.destroy();

        // on-update exists before creating a unique index
        EPStatement stmtUpdate = epService.getEPAdministrator().createEPL("@name('on-update') on SupportBean_S1 update MyTableLUIV set pkey1 = 0");
        epService.getEPAdministrator().createEPL("create unique index MyUniqueSecondary on MyTableLUIV (pkey1)");
        try {
            epService.getEPRuntime().sendEvent(new SupportBean_S1(0));
            fail();
        } catch (EPException ex) {
            SupportMessageAssertUtil.assertMessage(ex.getCause(), "Unexpected exception in statement 'on-update': Unique index violation, index 'MyUniqueSecondary' is a unique index and key '0' already exists");
            // assert events are unchanged - no update actually performed
            EPAssertionUtil.assertPropsPerRowAnyOrder(stmtCreate.iterator(), "pkey0,pkey1".split(","), new Object[][]{{"E1", 10}, {"E2", 20}});
        }

        // unregister
        stmtUpdate.destroy();
        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionFAFUpdate(EPServiceProvider epService) {
        epService.getEPAdministrator().createEPL("create table MyTableFAFU as (pkey0 string primary key, col0 int, col1 int, thecnt count(*))");
        epService.getEPAdministrator().createEPL("create index MyIndex on MyTableFAFU(col0)");

        epService.getEPAdministrator().createEPL("into table MyTableFAFU select count(*) as thecnt from SupportBean group by theString");
        epService.getEPRuntime().sendEvent(new SupportBean("E1", 0));
        epService.getEPRuntime().sendEvent(new SupportBean("E2", 0));

        epService.getEPRuntime().executeQuery("update MyTableFAFU set col0 = 1 where pkey0='E1'");
        epService.getEPRuntime().executeQuery("update MyTableFAFU set col0 = 2 where pkey0='E2'");
        assertFAFOneRowResult(epService, "select pkey0 from MyTableFAFU where col0=1", "pkey0", new Object[]{"E1"});

        epService.getEPRuntime().executeQuery("update MyTableFAFU set col1 = 100 where pkey0='E1'");
        assertFAFOneRowResult(epService, "select pkey0 from MyTableFAFU where col1=100", "pkey0", new Object[]{"E1"});

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void assertFAFOneRowResult(EPServiceProvider epService, String epl, String fields, Object[] objects) {
        EPOnDemandQueryResult result = epService.getEPRuntime().executeQuery(epl);
        assertEquals(1, result.getArray().length);
        EPAssertionUtil.assertProps(result.getArray()[0], fields.split(","), objects);
    }
}
