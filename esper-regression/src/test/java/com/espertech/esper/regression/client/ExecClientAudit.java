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
package com.espertech.esper.regression.client;

import com.espertech.esper.client.Configuration;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.annotation.AuditEnum;
import com.espertech.esper.client.dataflow.EPDataFlowInstance;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.client.time.CurrentTimeEvent;
import com.espertech.esper.core.service.EPServiceProviderSPI;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.bean.SupportBean_ST0;
import com.espertech.esper.supportregression.bean.SupportBean_ST1;
import com.espertech.esper.supportregression.client.SupportAuditCallback;
import com.espertech.esper.supportregression.execution.RegressionExecution;
import com.espertech.esper.util.AuditContext;
import com.espertech.esper.util.AuditPath;
import com.espertech.esper.support.EventRepresentationChoice;
import com.espertech.esper.util.EPServiceProviderName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ExecClientAudit implements RegressionExecution {

    private static final Logger log = LoggerFactory.getLogger(ExecClientAudit.class);
    private static final Logger AUDITLOG = LoggerFactory.getLogger(AuditPath.AUDIT_LOG);

    public void configure(Configuration configuration) throws Exception {
        configuration.addEventType("SupportBean", SupportBean.class);
        configuration.addEventType("SupportBean_ST0", SupportBean_ST0.class);
        configuration.addEventType("SupportBean_ST1", SupportBean_ST1.class);
        configuration.getEngineDefaults().getLogging().setAuditPattern("[%u] [%s] [%c] %m");
    }

    public void run(EPServiceProvider epService) throws Exception {
        runAssertionDocSample(epService);
        runAssertionAudit(epService);
    }

    private void runAssertionDocSample(EPServiceProvider epService) {
        EPStatement stmt = epService.getEPAdministrator().createEPL("create schema OrderEvent(price double)");

        String epl = "@Name('All-Order-Events') @Audit('stream,property') select price from OrderEvent";
        SupportUpdateListener listener = new SupportUpdateListener();
        epService.getEPAdministrator().createEPL(epl).addListener(listener);

        if (EventRepresentationChoice.getEngineDefault(epService).isObjectArrayEvent()) {
            epService.getEPRuntime().sendEvent(new Object[]{100d}, "OrderEvent");
        } else {
            epService.getEPRuntime().sendEvent(Collections.singletonMap("price", 100d), "OrderEvent");
        }

        stmt.destroy();
    }

    private void runAssertionAudit(EPServiceProvider epService) throws Exception {
        SupportUpdateListener listener = new SupportUpdateListener();

        // stream, and test audit callback
        SupportAuditCallback callback = new SupportAuditCallback();
        AuditPath.setAuditCallback(callback);
        AUDITLOG.info("*** Stream: ");
        EPStatement stmtInput = epService.getEPAdministrator().createEPL("@Name('ABC') @Audit('stream') select * from SupportBean(theString = 'E1')");
        epService.getEPRuntime().sendEvent(new SupportBean("E1", 1));
        assertEquals(1, callback.getAudits().size());
        AuditContext cb = callback.getAudits().get(0);
        assertEquals("SupportBean(theString=...) inserted SupportBean[SupportBean(E1, 1)]", cb.getMessage());
        assertEquals("ABC", cb.getStatementName());
        assertEquals(EPServiceProviderName.DEFAULT_ENGINE_URI, cb.getEngineURI());
        assertEquals(AuditEnum.STREAM, cb.getCategory());
        AuditPath.setAuditCallback(null);
        stmtInput.destroy();

        AUDITLOG.info("*** Named Window And Insert-Into: ");
        EPStatement stmtNW = epService.getEPAdministrator().createEPL("@Name('create') @Audit create window WinOne#keepall as SupportBean");
        EPStatement stmtInsertNW = epService.getEPAdministrator().createEPL("@Name('insert') @Audit insert into WinOne select * from SupportBean");
        EPStatement stmtConsumeNW = epService.getEPAdministrator().createEPL("@Name('select') @Audit select * from WinOne");
        epService.getEPRuntime().sendEvent(new SupportBean("E1", 1));
        stmtNW.destroy();
        stmtInsertNW.destroy();
        stmtConsumeNW.destroy();

        AUDITLOG.info("*** Insert-Into: ");
        EPStatement stmtInsertInto = epService.getEPAdministrator().createEPL("@Name('insert') @Audit insert into ABC select * from SupportBean");
        epService.getEPRuntime().sendEvent(new SupportBean("E1", 1));
        stmtInsertInto.destroy();

        AUDITLOG.info("*** Schedule: ");
        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(0));
        EPStatement stmtSchedule = epService.getEPAdministrator().createEPL("@Name('ABC') @Audit('schedule') select irstream * from SupportBean#time(1 sec)");
        stmtSchedule.addListener(listener);
        epService.getEPRuntime().sendEvent(new SupportBean("E1", 1));
        listener.reset();
        log.info("Sending time");
        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(2000));
        assertTrue(listener.isInvoked());
        listener.reset();
        stmtSchedule.destroy();

        // exprdef-instances
        AUDITLOG.info("*** Expression-Def: ");
        EPStatement stmtExprDef = epService.getEPAdministrator().createEPL("@Name('ABC') @Audit('exprdef') " +
                "expression DEF { 1 } " +
                "expression INN {  x => x.theString }" +
                "expression OUT { x => INN(x) } " +
                "select DEF(), OUT(sb) from SupportBean sb");
        stmtExprDef.addListener(listener);
        epService.getEPRuntime().sendEvent(new SupportBean("E1", 1));
        assertEquals(1, listener.assertOneGetNewAndReset().get("DEF()"));
        stmtExprDef.destroy();

        // pattern-instances
        AUDITLOG.info("*** Pattern-Lifecycle: ");
        EPStatement stmtPatternLife = epService.getEPAdministrator().createEPL("@Name('ABC') @Audit('pattern-instances') select a.intPrimitive as val0 from pattern [every a=SupportBean -> (b=SupportBean_ST0 and not SupportBean_ST1)]");
        stmtPatternLife.addListener(listener);
        log.info("Sending E1");
        epService.getEPRuntime().sendEvent(new SupportBean("E1", 1));
        log.info("Sending E2");
        epService.getEPRuntime().sendEvent(new SupportBean("E2", 2));
        log.info("Sending E3");
        epService.getEPRuntime().sendEvent(new SupportBean_ST1("E3", 3));
        stmtPatternLife.destroy();

        // pattern
        AUDITLOG.info("*** Pattern: ");
        EPStatement stmtPattern = epService.getEPAdministrator().createEPL("@Name('ABC') @Audit('pattern') select a.intPrimitive as val0 from pattern [a=SupportBean -> b=SupportBean_ST0]");
        stmtPattern.addListener(listener);
        epService.getEPRuntime().sendEvent(new SupportBean("E1", 1));
        epService.getEPRuntime().sendEvent(new SupportBean_ST0("E2", 2));
        assertEquals(1, listener.assertOneGetNewAndReset().get("val0"));
        stmtPattern.destroy();

        // view
        AUDITLOG.info("*** View: ");
        EPStatement stmtView = epService.getEPAdministrator().createEPL("@Name('ABC') @Audit('view') select intPrimitive from SupportBean#lastevent");
        stmtView.addListener(listener);
        epService.getEPRuntime().sendEvent(new SupportBean("E1", 50));
        assertEquals(50, listener.assertOneGetNewAndReset().get("intPrimitive"));
        stmtView.destroy();

        EPStatement stmtGroupedView = epService.getEPAdministrator().createEPL("@Audit Select * From SupportBean#groupwin(theString)#length(2)");
        stmtGroupedView.addListener(listener);
        epService.getEPRuntime().sendEvent(new SupportBean("E1", 50));
        listener.reset();
        stmtGroupedView.destroy();

        EPStatement stmtGroupedWIntersectionView = epService.getEPAdministrator().createEPL("@Audit Select * From SupportBean#groupwin(theString)#length(2)#unique(intPrimitive)");
        stmtGroupedWIntersectionView.addListener(listener);
        epService.getEPRuntime().sendEvent(new SupportBean("E1", 50));
        listener.reset();
        stmtGroupedWIntersectionView.destroy();

        // expression
        AUDITLOG.info("*** Expression: ");
        EPStatement stmtExpr = epService.getEPAdministrator().createEPL("@Name('ABC') @Audit('expression') select intPrimitive*100 as val0, sum(intPrimitive) as val1 from SupportBean");
        stmtExpr.addListener(listener);
        epService.getEPRuntime().sendEvent(new SupportBean("E1", 50));
        assertEquals(5000, listener.assertOneGetNew().get("val0"));
        assertEquals(50, listener.assertOneGetNewAndReset().get("val1"));
        stmtExpr.destroy();

        // expression-detail
        AUDITLOG.info("*** Expression-Nested: ");
        EPStatement stmtExprNested = epService.getEPAdministrator().createEPL("@Name('ABC') @Audit('expression-nested') select ('A'||theString)||'X' as val0 from SupportBean");
        stmtExprNested.addListener(listener);
        epService.getEPRuntime().sendEvent(new SupportBean("E1", 50));
        assertEquals("AE1X", listener.assertOneGetNewAndReset().get("val0"));
        stmtExprNested.destroy();

        // property
        AUDITLOG.info("*** Property: ");
        EPStatement stmtProp = epService.getEPAdministrator().createEPL("@Name('ABC') @Audit('property') select intPrimitive from SupportBean");
        stmtProp.addListener(listener);
        epService.getEPRuntime().sendEvent(new SupportBean("E1", 50));
        assertEquals(50, listener.assertOneGetNewAndReset().get("intPrimitive"));
        stmtProp.destroy();

        // with aggregation
        epService.getEPAdministrator().createEPL("@Audit @Name ('create') create window MyWindow#keepall as SupportBean");
        String eplWithAgg = "@Audit @Name('S0') on SupportBean as sel select count(*) from MyWindow as win having count(*)=3 order by win.intPrimitive";
        EPStatement stmtWithAgg = epService.getEPAdministrator().createEPL(eplWithAgg);
        stmtWithAgg.destroy();

        // data flow
        EPStatement stmtDataflow = epService.getEPAdministrator().createEPL("@Audit @Name('df') create dataflow MyFlow " +
                "EventBusSource -> a<SupportBean> {filter:theString like 'I%'} " +
                "Filter(a) -> b {filter: true}" +
                "LogSink(b) {log:false}");
        EPDataFlowInstance df = epService.getEPRuntime().getDataFlowRuntime().instantiate("MyFlow");
        df.start();
        epService.getEPRuntime().sendEvent(new SupportBean("I1", 1));
        df.cancel();

        // context partitions
        epService.getEPAdministrator().createEPL("create context WhenEventArrives " +
                "initiated by SupportBean_ST0 as st0 " +
                "terminated by SupportBean_ST1(id=st0.id)");
        epService.getEPAdministrator().createEPL("@Audit('ContextPartition') context WhenEventArrives select * from SupportBean");
        epService.getEPRuntime().sendEvent(new SupportBean_ST0("E1", 0));
        epService.getEPRuntime().sendEvent(new SupportBean_ST1("E1", 0));
        stmtDataflow.destroy();

        // table
        AUDITLOG.info("*** Table And Insert-Into and Into-table: ");
        EPStatement stmtTable = epService.getEPAdministrator().createEPL("@Name('create-table') @Audit create table TableOne(c0 string primary key, cnt count(*))");
        EPStatement stmtIntoTable = epService.getEPAdministrator().createEPL("@Name('into-table') @Audit into table TableOne select count(*) as cnt from SupportBean group by theString");
        EPStatement stmtAccessTable = epService.getEPAdministrator().createEPL("@Name('access-table') @Audit select TableOne[id].cnt from SupportBean_ST0");
        stmtAccessTable.addListener(listener);
        epService.getEPRuntime().sendEvent(new SupportBean("E1", 1));
        epService.getEPRuntime().sendEvent(new SupportBean_ST0("E1", 0));
        stmtTable.destroy();
        stmtIntoTable.destroy();
        stmtAccessTable.destroy();

        // int-expression with endpoint-included
        epService.getEPAdministrator().createEPL("@audit select * from SupportBean#keepall where intPrimitive in (1:3)");
        epService.getEPRuntime().sendEvent(new SupportBean("E1", 1));
    }
}
