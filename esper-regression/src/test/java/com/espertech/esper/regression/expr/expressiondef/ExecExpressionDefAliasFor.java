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
package com.espertech.esper.regression.expr.expressiondef;

import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.deploy.DeploymentResult;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.client.soda.EPStatementObjectModel;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.execution.RegressionExecution;

import static com.espertech.esper.supportregression.util.SupportMessageAssertUtil.tryInvalid;
import static org.junit.Assert.*;

public class ExecExpressionDefAliasFor implements RegressionExecution {

    public void run(EPServiceProvider epService) throws Exception {
        epService.getEPAdministrator().getConfiguration().addEventType(SupportBean.class);

        runAssertionContextPartition(epService);
        runAssertionDocSamples(epService);
        runAssertionNestedAlias(epService);
        runAssertionAliasAggregation(epService);
        runAssertionGlobalAliasAndSODA(epService);
        runAssertionInvalid(epService);
    }

    private void runAssertionContextPartition(EPServiceProvider epService) throws Exception {
        String epl =
                "create expression the_expr alias for {theString='a' and intPrimitive=1};\n" +
                        "create context the_context start @now end after 10 minutes;\n" +
                        "@name('s0') context the_context select * from SupportBean(the_expr)\n";
        DeploymentResult result = epService.getEPAdministrator().getDeploymentAdmin().parseDeploy(epl);
        SupportUpdateListener listener = new SupportUpdateListener();

        epService.getEPAdministrator().getStatement("s0").addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean("a", 1));
        assertTrue(listener.getIsInvokedAndReset());

        epService.getEPRuntime().sendEvent(new SupportBean("b", 1));
        assertFalse(listener.getIsInvokedAndReset());

        epService.getEPAdministrator().getDeploymentAdmin().undeployRemove(result.getDeploymentId());
    }

    private void runAssertionDocSamples(EPServiceProvider epService) {
        epService.getEPAdministrator().createEPL("create schema SampleEvent()");
        epService.getEPAdministrator().createEPL("expression twoPI alias for {Math.PI * 2}\n" +
                "select twoPI from SampleEvent");

        epService.getEPAdministrator().createEPL("create schema EnterRoomEvent()");
        epService.getEPAdministrator().createEPL("expression countPeople alias for {count(*)} \n" +
                "select countPeople from EnterRoomEvent#time(10 seconds) having countPeople > 10");

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionNestedAlias(EPServiceProvider epService) {
        String[] fields = "c0".split(",");
        SupportUpdateListener listener = new SupportUpdateListener();

        epService.getEPAdministrator().createEPL("create expression F1 alias for {10}");
        epService.getEPAdministrator().createEPL("create expression F2 alias for {20}");
        epService.getEPAdministrator().createEPL("create expression F3 alias for {F1+F2}");
        epService.getEPAdministrator().createEPL("select F3 as c0 from SupportBean").addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 10));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{30});

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionAliasAggregation(EPServiceProvider epService) {
        String epl = "@Audit expression total alias for {sum(intPrimitive)} " +
                "select total, total+1 from SupportBean";
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        String[] fields = "total,total+1".split(",");
        for (String field : fields) {
            assertEquals(Integer.class, stmt.getEventType().getPropertyType(field));
        }

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 10));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{10, 11});

        stmt.destroy();
    }

    private void runAssertionGlobalAliasAndSODA(EPServiceProvider epService) {
        String eplDeclare = "create expression myaliastwo alias for {2}";
        EPStatementObjectModel model = epService.getEPAdministrator().compileEPL(eplDeclare);
        assertEquals(eplDeclare, model.toEPL());
        EPStatement stmtDeclare = epService.getEPAdministrator().create(model);
        assertEquals(eplDeclare, stmtDeclare.getText());

        SupportUpdateListener listener = new SupportUpdateListener();
        epService.getEPAdministrator().createEPL("create expression myalias alias for {1}");
        epService.getEPAdministrator().createEPL("select myaliastwo from SupportBean(intPrimitive = myalias)").addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 0));
        assertFalse(listener.isInvoked());

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 1));
        assertEquals(2, listener.assertOneGetNewAndReset().get("myaliastwo"));

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionInvalid(EPServiceProvider epService) {
        tryInvalid(epService, "expression total alias for {sum(xxx)} select total+1 from SupportBean",
                "Error starting statement: Failed to validate select-clause expression 'total+1': Error validating expression alias 'total': Failed to validate alias expression body expression 'sum(xxx)': Property named 'xxx' is not valid in any stream [expression total alias for {sum(xxx)} select total+1 from SupportBean]");
        tryInvalid(epService, "expression total xxx for {1} select total+1 from SupportBean",
                "For expression alias 'total' expecting 'alias' keyword but received 'xxx' [expression total xxx for {1} select total+1 from SupportBean]");
        tryInvalid(epService, "expression total(a) alias for {1} select total+1 from SupportBean",
                "For expression alias 'total' expecting no parameters but received 'a' [expression total(a) alias for {1} select total+1 from SupportBean]");
        tryInvalid(epService, "expression total alias for {a -> 1} select total+1 from SupportBean",
                "For expression alias 'total' expecting an expression without parameters but received 'a ->' [expression total alias for {a -> 1} select total+1 from SupportBean]");
        tryInvalid(epService, "expression total alias for ['some text'] select total+1 from SupportBean",
                "For expression alias 'total' expecting an expression but received a script [expression total alias for ['some text'] select total+1 from SupportBean]");
    }
}
