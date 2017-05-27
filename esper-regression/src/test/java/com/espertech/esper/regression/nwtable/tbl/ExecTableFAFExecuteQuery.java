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

import com.espertech.esper.client.Configuration;
import com.espertech.esper.client.EPOnDemandQueryResult;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.bean.SupportBean_A;
import com.espertech.esper.supportregression.execution.RegressionExecution;
import com.espertech.esper.supportregression.util.IndexBackingTableInfo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

public class ExecTableFAFExecuteQuery implements RegressionExecution, IndexBackingTableInfo {
    public void configure(Configuration configuration) throws Exception {
        configuration.getEngineDefaults().getLogging().setEnableQueryPlan(true);
        configuration.addEventType("SupportBean", SupportBean.class.getName());
        configuration.addEventType("SupportBean_A", SupportBean_A.class.getName());
    }

    public void run(EPServiceProvider epService) throws Exception {
        runAssertionFAFInsert(epService);
        runAssertionFAFDelete(epService);
        runAssertionFAFUpdate(epService);
        runAssertionFAFSelect(epService);
    }

    private void runAssertionFAFInsert(EPServiceProvider epService) {
        String[] propertyNames = "p0,p1".split(",");
        EPStatement stmt = epService.getEPAdministrator().createEPL("create table MyTableINS as (p0 string, p1 int)");

        String eplInsertInto = "insert into MyTableINS (p0, p1) select 'a', 1";
        EPOnDemandQueryResult resultOne = epService.getEPRuntime().executeQuery(eplInsertInto);
        assertFAFInsertResult(resultOne, propertyNames, stmt);
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), propertyNames, new Object[][]{{"a", 1}});

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionFAFDelete(EPServiceProvider epService) {
        EPStatement stmt = epService.getEPAdministrator().createEPL("create table MyTableDEL as (p0 string primary key, thesum sum(int))");
        epService.getEPAdministrator().createEPL("into table MyTableDEL select theString, sum(intPrimitive) as thesum from SupportBean group by theString");
        for (int i = 0; i < 10; i++) {
            epService.getEPRuntime().sendEvent(new SupportBean("G" + i, i));
        }
        assertEquals(10L, getTableCount(stmt));
        epService.getEPRuntime().executeQuery("delete from MyTableDEL");
        assertEquals(0L, getTableCount(stmt));

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionFAFUpdate(EPServiceProvider epService) {
        String[] fields = "p0,p1".split(",");
        epService.getEPAdministrator().createEPL("@Name('TheTable') create table MyTableUPD as (p0 string primary key, p1 string, thesum sum(int))");
        epService.getEPAdministrator().createEPL("into table MyTableUPD select theString, sum(intPrimitive) as thesum from SupportBean group by theString");
        epService.getEPRuntime().sendEvent(new SupportBean("E1", 1));
        epService.getEPRuntime().sendEvent(new SupportBean("E2", 2));
        epService.getEPRuntime().executeQuery("update MyTableUPD set p1 = 'ABC'");
        EPAssertionUtil.assertPropsPerRowAnyOrder(epService.getEPAdministrator().getStatement("TheTable").iterator(), fields, new Object[][]{{"E1", "ABC"}, {"E2", "ABC"}});
        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionFAFSelect(EPServiceProvider epService) {
        String[] fields = "p0".split(",");
        epService.getEPAdministrator().createEPL("@Name('TheTable') create table MyTableSEL as (p0 string primary key, thesum sum(int))");
        epService.getEPAdministrator().createEPL("into table MyTableSEL select theString, sum(intPrimitive) as thesum from SupportBean group by theString");
        epService.getEPRuntime().sendEvent(new SupportBean("E1", 1));
        epService.getEPRuntime().sendEvent(new SupportBean("E2", 2));
        EPOnDemandQueryResult result = epService.getEPRuntime().executeQuery("select * from MyTableSEL");
        EPAssertionUtil.assertPropsPerRowAnyOrder(result.getArray(), fields, new Object[][]{{"E1"}, {"E2"}});
        epService.getEPAdministrator().destroyAllStatements();
    }

    private long getTableCount(EPStatement stmt) {
        return EPAssertionUtil.iteratorCount(stmt.iterator());
    }

    private void assertFAFInsertResult(EPOnDemandQueryResult resultOne, String[] propertyNames, EPStatement stmt) {
        assertEquals(0, resultOne.getArray().length);
        assertSame(resultOne.getEventType(), stmt.getEventType());
    }
}
