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
package com.espertech.esper.regressionlib.suite.client.instrument;

import com.espertech.esper.common.client.EPCompiled;
import com.espertech.esper.common.client.annotation.AuditEnum;
import com.espertech.esper.common.client.dataflow.core.EPDataFlowInstance;
import com.espertech.esper.common.internal.metrics.audit.AuditContext;
import com.espertech.esper.common.internal.metrics.audit.AuditPath;
import com.espertech.esper.common.internal.support.EventRepresentationChoice;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.RegressionPath;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.regressionlib.support.bean.SupportBean_ST0;
import com.espertech.esper.regressionlib.support.bean.SupportBean_ST1;
import com.espertech.esper.regressionlib.support.client.SupportAuditCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.espertech.esper.runtime.client.EPRuntimeProvider.DEFAULT_RUNTIME_URI;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ClientInstrumentAudit {

    private static final Logger log = LoggerFactory.getLogger(ClientInstrumentAudit.class);
    private static final Logger AUDITLOG = LoggerFactory.getLogger(AuditPath.AUDIT_LOG);

    public static List<RegressionExecution> executions() {
        List<RegressionExecution> execs = new ArrayList<>();
        execs.add(new ClientInstrumentAuditDocSample());
        execs.add(new ClientInstrumentAuditAudit());
        return execs;
    }

    private static class ClientInstrumentAuditDocSample implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            EPCompiled compiled = env.compileWBusPublicType("create schema OrderEvent(price double)");
            env.deploy(compiled);
            RegressionPath path = new RegressionPath();
            path.add(compiled);

            String epl = "@Name('All-Order-Events') @Audit('stream,property') select price from OrderEvent";
            env.compileDeploy(epl, path).addListener("All-Order-Events");

            if (EventRepresentationChoice.getEngineDefault(env.getConfiguration()).isObjectArrayEvent()) {
                env.sendEventObjectArray(new Object[]{100d}, "OrderEvent");
            } else {
                env.sendEventMap(Collections.singletonMap("price", 100d), "OrderEvent");
            }

            env.undeployAll();
        }
    }

    private static class ClientInstrumentAuditAudit implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();

            // stream, and test audit callback
            SupportAuditCallback callback = new SupportAuditCallback();
            AuditPath.setAuditCallback(callback);
            AUDITLOG.info("*** Stream: ");
            env.compileDeploy("@Name('ABC') @Audit('stream') select * from SupportBean(theString = 'E1')");
            env.sendEventBean(new SupportBean("E1", 1));
            assertEquals(1, callback.getAudits().size());
            AuditContext cb = callback.getAudits().get(0);
            assertEquals("SupportBean(theString=...) inserted SupportBean[SupportBean(E1, 1)]", cb.getMessage());
            assertEquals(env.deploymentId("ABC"), cb.getDeploymentId());
            assertEquals("ABC", cb.getStatementName());
            assertEquals(DEFAULT_RUNTIME_URI, cb.getRuntimeURI());
            assertEquals(AuditEnum.STREAM, cb.getCategory());
            AuditPath.setAuditCallback(null);
            env.undeployAll();

            AUDITLOG.info("*** Insert-Into: ");
            env.compileDeploy("@Name('insert') @Audit insert into ABC select * from SupportBean");
            env.sendEventBean(new SupportBean("E1", 1));
            env.undeployAll();

            AUDITLOG.info("*** Named Window And Insert-Into: ");
            env.compileDeploy("@Name('create') @Audit create window WinOne#keepall as SupportBean", path);
            env.compileDeploy("@Name('insert') @Audit insert into WinOne select * from SupportBean", path);
            env.compileDeploy("@Name('select') @Audit select * from WinOne", path);
            env.sendEventBean(new SupportBean("E1", 1));
            env.undeployAll();
            path.clear();

            AUDITLOG.info("*** Schedule: ");
            env.advanceTime(0);
            env.compileDeploy("@Name('ABC') @Audit('schedule') select irstream * from SupportBean#time(1 sec)").addListener("ABC");
            env.sendEventBean(new SupportBean("E1", 1));
            env.listener("ABC").reset();
            log.info("Sending time");
            env.advanceTime(2000);
            assertTrue(env.listener("ABC").isInvoked());
            env.undeployAll();

            // property
            AUDITLOG.info("*** Property: ");
            env.compileDeploy("@Name('ABC') @Audit('property') select intPrimitive from SupportBean").addListener("ABC");
            env.sendEventBean(new SupportBean("E1", 50));
            assertEquals(50, env.listener("ABC").assertOneGetNewAndReset().get("intPrimitive"));
            env.undeployAll();

            // view
            AUDITLOG.info("*** View: ");
            env.compileDeploy("@Name('ABC') @Audit('view') select intPrimitive from SupportBean#lastevent").addListener("ABC");
            env.sendEventBean(new SupportBean("E1", 50));
            assertEquals(50, env.listener("ABC").assertOneGetNewAndReset().get("intPrimitive"));
            env.undeployAll();

            env.compileDeploy("@name('s0') @Audit Select * From SupportBean#groupwin(theString)#length(2)").addListener("s0");
            env.sendEventBean(new SupportBean("E1", 50));
            env.undeployAll();

            env.compileDeploy("@name('s0') @Audit Select * From SupportBean#groupwin(theString)#length(2)#unique(intPrimitive)").addListener("s0");
            env.sendEventBean(new SupportBean("E1", 50));
            env.undeployAll();

            // expression
            AUDITLOG.info("*** Expression: ");
            env.compileDeploy("@Name('ABC') @Audit('expression') select intPrimitive*100 as val0, sum(intPrimitive) as val1 from SupportBean").addListener("ABC");
            env.sendEventBean(new SupportBean("E1", 50));
            assertEquals(5000, env.listener("ABC").assertOneGetNew().get("val0"));
            assertEquals(50, env.listener("ABC").assertOneGetNewAndReset().get("val1"));
            env.undeployAll();

            // expression-detail
            AUDITLOG.info("*** Expression-Nested: ");
            env.compileDeploy("@Name('ABC') @Audit('expression-nested') select ('A'||theString)||'X' as val0 from SupportBean").addListener("ABC");
            env.sendEventBean(new SupportBean("E1", 50));
            assertEquals("AE1X", env.listener("ABC").assertOneGetNewAndReset().get("val0"));
            env.undeployAll();

            // pattern
            AUDITLOG.info("*** Pattern: ");
            env.compileDeploy("@Name('ABC') @Audit('pattern') select a.intPrimitive as val0 from pattern [a=SupportBean -> b=SupportBean_ST0]").addListener("ABC");
            env.sendEventBean(new SupportBean("E1", 1));
            env.sendEventBean(new SupportBean_ST0("E2", 2));
            assertEquals(1, env.listener("ABC").assertOneGetNewAndReset().get("val0"));
            env.undeployAll();

            // pattern-instances
            AUDITLOG.info("*** Pattern-Lifecycle: ");
            env.compileDeploy("@Name('ABC') @Audit('pattern-instances') select a.intPrimitive as val0 from pattern [every a=SupportBean -> (b=SupportBean_ST0 and not SupportBean_ST1)]").addListener("ABC");
            log.info("Sending E1");
            env.sendEventBean(new SupportBean("E1", 1));
            log.info("Sending E2");
            env.sendEventBean(new SupportBean("E2", 2));
            log.info("Sending E3");
            env.sendEventBean(new SupportBean_ST1("E3", 3));
            log.info("Destroy");
            env.undeployAll();

            // exprdef-instances
            AUDITLOG.info("*** Expression-Def: ");
            env.compileDeploy("@Name('ABC') @Audit('exprdef') " +
                "expression DEF { 1 } " +
                "expression INN {  x => x.theString }" +
                "expression OUT { x => INN(x) } " +
                "select DEF(), OUT(sb) from SupportBean sb").addListener("ABC");
            env.sendEventBean(new SupportBean("E1", 1));
            assertEquals(1, env.listener("ABC").assertOneGetNewAndReset().get("DEF()"));
            env.undeployAll();

            // data flow
            env.compileDeploy("@Audit @Name('df') create dataflow MyFlow " +
                "EventBusSource -> a<SupportBean> {filter:theString like 'I%'} " +
                "Filter(a) -> b {filter: true}" +
                "LogSink(b) {log:false}");
            EPDataFlowInstance df = env.runtime().getDataFlowService().instantiate(env.deploymentId("df"), "MyFlow");
            df.start();
            env.sendEventBean(new SupportBean("I1", 1));
            df.cancel();

            // context partitions
            env.compileDeploy("create context WhenEventArrives " +
                "initiated by SupportBean_ST0 as st0 " +
                "terminated by SupportBean_ST1(id=st0.id);\n" +
                "@Audit('ContextPartition') context WhenEventArrives select * from SupportBean;\n");
            env.sendEventBean(new SupportBean_ST0("E1", 0));
            env.sendEventBean(new SupportBean_ST1("E1", 0));
            env.undeployAll();

            // table
            AUDITLOG.info("*** Table And Insert-Into and Into-table: ");
            env.compileDeploy("@Name('create-table') @Audit create table TableOne(c0 string primary key, cnt count(*))", path);
            env.compileDeploy("@Name('into-table') @Audit into table TableOne select count(*) as cnt from SupportBean group by theString", path);
            env.compileDeploy("@Name('access-table') @Audit select TableOne[id].cnt from SupportBean_ST0", path).addListener("access-table");
            env.sendEventBean(new SupportBean("E1", 1));
            env.sendEventBean(new SupportBean_ST0("E1", 0));
            env.undeployAll();
            path.clear();

            // int-expression with endpoint-included
            env.compileDeploy("@audit select * from SupportBean#keepall where intPrimitive in (1:3)");
            env.sendEventBean(new SupportBean("E1", 1));
            env.undeployAll();
        }
    }
}
