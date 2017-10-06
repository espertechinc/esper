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

import com.espertech.esper.client.EPOnDemandQueryResult;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.bean.SupportBean_S0;
import com.espertech.esper.supportregression.bean.SupportBean_S1;
import com.espertech.esper.supportregression.execution.RegressionExecution;
import com.espertech.esper.supportregression.util.SupportMessageAssertUtil;
import com.espertech.esper.supportregression.util.SupportModelHelper;

import java.util.Map;

public class ExecTableIntoTable implements RegressionExecution {

    public void run(EPServiceProvider epService) throws Exception {
        for (Class clazz : new Class[]{SupportBean.class, SupportBean_S0.class, SupportBean_S1.class}) {
            epService.getEPAdministrator().getConfiguration().addEventType(clazz);
        }

        runAssertionIntoTableWindowSortedFromJoin(epService);
        runAssertionBoundUnbound(epService);
    }

    private void runAssertionIntoTableWindowSortedFromJoin(EPServiceProvider epService) {
        epService.getEPAdministrator().createEPL("create table MyTable(" +
                "thewin window(*) @type('SupportBean')," +
                "thesort sorted(intPrimitive desc) @type('SupportBean')" +
                ")");

        epService.getEPAdministrator().createEPL("into table MyTable " +
                "select window(sb.*) as thewin, sorted(sb.*) as thesort " +
                "from SupportBean_S0#lastevent, SupportBean#keepall as sb");
        epService.getEPRuntime().sendEvent(new SupportBean_S0(1));

        SupportBean sb1 = new SupportBean("E1", 1);
        epService.getEPRuntime().sendEvent(sb1);
        SupportBean sb2 = new SupportBean("E2", 2);
        epService.getEPRuntime().sendEvent(sb2);

        EPOnDemandQueryResult result = epService.getEPRuntime().executeQuery("select * from MyTable");
        EPAssertionUtil.assertPropsPerRow(result.getArray(), "thewin,thesort".split(","),
                new Object[][]{{new SupportBean[]{sb1, sb2}, new SupportBean[]{sb2, sb1}}});

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionBoundUnbound(EPServiceProvider epService) {
        // Bound: max/min; Unbound: maxever/minever
        tryAssertionMinMax(epService, false);
        tryAssertionMinMax(epService, true);

        // Bound: sorted; Unbound: maxbyever/minbyever; Disallowed: minby, maxby declaration (must use sorted instead)
        // - requires declaring the same sort expression but can be against subtype of declared event type
        tryAssertionSortedMinMaxBy(epService, false);
        tryAssertionSortedMinMaxBy(epService, true);

        // Bound: window; Unbound: lastever/firstever; Disallowed: last, first
        tryAssertionLastFirstWindow(epService, false);
        tryAssertionLastFirstWindow(epService, true);
    }

    private void tryAssertionLastFirstWindow(EPServiceProvider epService, boolean soda) {
        String[] fields = "lasteveru,firsteveru,windowb".split(",");
        String eplDeclare = "create table varagg (" +
                "lasteveru lastever(*) @type('SupportBean'), " +
                "firsteveru firstever(*) @type('SupportBean'), " +
                "windowb window(*) @type('SupportBean'))";
        SupportModelHelper.createByCompileOrParse(epService, soda, eplDeclare);

        String eplIterate = "select varagg from SupportBean_S0#lastevent";
        EPStatement stmtIterate = SupportModelHelper.createByCompileOrParse(epService, soda, eplIterate);
        epService.getEPRuntime().sendEvent(new SupportBean_S0(0));

        String eplBoundInto = "into table varagg select window(*) as windowb from SupportBean#length(2)";
        SupportModelHelper.createByCompileOrParse(epService, soda, eplBoundInto);

        String eplUnboundInto = "into table varagg select lastever(*) as lasteveru, firstever(*) as firsteveru from SupportBean";
        SupportModelHelper.createByCompileOrParse(epService, soda, eplUnboundInto);

        SupportBean b1 = makeSendBean(epService, "E1", 20);
        SupportBean b2 = makeSendBean(epService, "E2", 15);
        SupportBean b3 = makeSendBean(epService, "E3", 10);
        assertResults(stmtIterate, fields, new Object[]{b3, b1, new Object[]{b2, b3}});

        SupportBean b4 = makeSendBean(epService, "E4", 5);
        assertResults(stmtIterate, fields, new Object[]{b4, b1, new Object[]{b3, b4}});

        // invalid: bound aggregation into unbound max
        SupportMessageAssertUtil.tryInvalid(epService, "into table varagg select last(*) as lasteveru from SupportBean#length(2)",
                "Error starting statement: Failed to validate select-clause expression 'last(*)': For into-table use 'window(*)' or 'window(stream.*)' instead [");
        // invalid: unbound aggregation into bound max
        SupportMessageAssertUtil.tryInvalid(epService, "into table varagg select lastever(*) as windowb from SupportBean#length(2)",
                "Error starting statement: Incompatible aggregation function for table 'varagg' column 'windowb', expecting 'window(*)' and received 'lastever(*)': Not a 'window' aggregation [");

        // valid: bound with unbound variable
        String eplBoundIntoUnbound = "into table varagg select lastever(*) as lasteveru from SupportBean#length(2)";
        SupportModelHelper.createByCompileOrParse(epService, soda, eplBoundIntoUnbound);

        epService.getEPAdministrator().destroyAllStatements();
        epService.getEPAdministrator().getConfiguration().removeEventType("table_varagg__internal", false);
        epService.getEPAdministrator().getConfiguration().removeEventType("table_varagg__public", false);
    }

    private void tryAssertionSortedMinMaxBy(EPServiceProvider epService, boolean soda) {
        String[] fields = "maxbyeveru,minbyeveru,sortedb".split(",");
        String eplDeclare = "create table varagg (" +
                "maxbyeveru maxbyever(intPrimitive) @type('SupportBean'), " +
                "minbyeveru minbyever(intPrimitive) @type('SupportBean'), " +
                "sortedb sorted(intPrimitive) @type('SupportBean'))";
        SupportModelHelper.createByCompileOrParse(epService, soda, eplDeclare);

        String eplIterate = "select varagg from SupportBean_S0#lastevent";
        EPStatement stmtIterate = SupportModelHelper.createByCompileOrParse(epService, soda, eplIterate);
        epService.getEPRuntime().sendEvent(new SupportBean_S0(0));

        String eplBoundInto = "into table varagg select sorted() as sortedb from SupportBean#length(2)";
        SupportModelHelper.createByCompileOrParse(epService, soda, eplBoundInto);

        String eplUnboundInto = "into table varagg select maxbyever() as maxbyeveru, minbyever() as minbyeveru from SupportBean";
        SupportModelHelper.createByCompileOrParse(epService, soda, eplUnboundInto);

        SupportBean b1 = makeSendBean(epService, "E1", 20);
        SupportBean b2 = makeSendBean(epService, "E2", 15);
        SupportBean b3 = makeSendBean(epService, "E3", 10);
        assertResults(stmtIterate, fields, new Object[]{b1, b3, new Object[]{b3, b2}});

        // invalid: bound aggregation into unbound max
        SupportMessageAssertUtil.tryInvalid(epService, "into table varagg select maxby(intPrimitive) as maxbyeveru from SupportBean#length(2)",
                "Error starting statement: Failed to validate select-clause expression 'maxby(intPrimitive)': When specifying into-table a sort expression cannot be provided [");
        // invalid: unbound aggregation into bound max
        SupportMessageAssertUtil.tryInvalid(epService, "into table varagg select maxbyever() as sortedb from SupportBean#length(2)",
                "Error starting statement: Incompatible aggregation function for table 'varagg' column 'sortedb', expecting 'sorted(intPrimitive)' and received 'maxbyever()': The required aggregation function name is 'sorted' and provided is 'maxbyever' [");

        // valid: bound with unbound variable
        String eplBoundIntoUnbound = "into table varagg select " +
                "maxbyever() as maxbyeveru, minbyever() as minbyeveru " +
                "from SupportBean#length(2)";
        SupportModelHelper.createByCompileOrParse(epService, soda, eplBoundIntoUnbound);

        epService.getEPAdministrator().destroyAllStatements();
        epService.getEPAdministrator().getConfiguration().removeEventType("table_varagg__internal", false);
        epService.getEPAdministrator().getConfiguration().removeEventType("table_varagg__public", false);
    }

    private void tryAssertionMinMax(EPServiceProvider epService, boolean soda) {
        String[] fields = "maxb,maxu,minb,minu".split(",");
        String eplDeclare = "create table varagg (" +
                "maxb max(int), maxu maxever(int), minb min(int), minu minever(int))";
        SupportModelHelper.createByCompileOrParse(epService, soda, eplDeclare);

        String eplIterate = "select varagg from SupportBean_S0#lastevent";
        EPStatement stmtIterate = SupportModelHelper.createByCompileOrParse(epService, soda, eplIterate);
        epService.getEPRuntime().sendEvent(new SupportBean_S0(0));

        String eplBoundInto = "into table varagg select " +
                "max(intPrimitive) as maxb, min(intPrimitive) as minb " +
                "from SupportBean#length(2)";
        SupportModelHelper.createByCompileOrParse(epService, soda, eplBoundInto);

        String eplUnboundInto = "into table varagg select " +
                "maxever(intPrimitive) as maxu, minever(intPrimitive) as minu " +
                "from SupportBean";
        SupportModelHelper.createByCompileOrParse(epService, soda, eplUnboundInto);

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 20));
        epService.getEPRuntime().sendEvent(new SupportBean("E2", 15));
        epService.getEPRuntime().sendEvent(new SupportBean("E3", 10));
        assertResults(stmtIterate, fields, new Object[]{15, 20, 10, 10});

        epService.getEPRuntime().sendEvent(new SupportBean("E4", 5));
        assertResults(stmtIterate, fields, new Object[]{10, 20, 5, 5});

        epService.getEPRuntime().sendEvent(new SupportBean("E5", 25));
        assertResults(stmtIterate, fields, new Object[]{25, 25, 5, 5});

        // invalid: unbound aggregation into bound max
        SupportMessageAssertUtil.tryInvalid(epService, "into table varagg select max(intPrimitive) as maxb from SupportBean",
                "Error starting statement: Incompatible aggregation function for table 'varagg' column 'maxb', expecting 'max(int)' and received 'max(intPrimitive)': The aggregation declares use with data windows and provided is unbound [");

        // valid: bound with unbound variable
        String eplBoundIntoUnbound = "into table varagg select " +
                "maxever(intPrimitive) as maxu, minever(intPrimitive) as minu " +
                "from SupportBean#length(2)";
        SupportModelHelper.createByCompileOrParse(epService, soda, eplBoundIntoUnbound);

        epService.getEPAdministrator().destroyAllStatements();
        epService.getEPAdministrator().getConfiguration().removeEventType("table_varagg__internal", false);
        epService.getEPAdministrator().getConfiguration().removeEventType("table_varagg__public", false);
    }

    private void assertResults(EPStatement stmt, String[] fields, Object[] values) {
        EventBean event = stmt.iterator().next();
        Map map = (Map) event.get("varagg");
        EPAssertionUtil.assertPropsMap(map, fields, values);
    }

    private SupportBean makeSendBean(EPServiceProvider epService, String theString, int intPrimitive) {
        SupportBean bean = new SupportBean(theString, intPrimitive);
        epService.getEPRuntime().sendEvent(bean);
        return bean;
    }
}
