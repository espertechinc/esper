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
package com.espertech.esper.epl.core.select;

import com.espertech.esper.client.Configuration;
import com.espertech.esper.client.ConfigurationEngineDefaults;
import com.espertech.esper.core.service.StatementContext;
import com.espertech.esper.core.service.StatementEventTypeRefImpl;
import com.espertech.esper.core.service.StatementResultService;
import com.espertech.esper.core.service.StatementResultServiceImpl;
import com.espertech.esper.core.support.SupportEngineImportServiceFactory;
import com.espertech.esper.core.support.SupportEventAdapterService;
import com.espertech.esper.core.support.SupportStatementContextFactory;
import com.espertech.esper.core.thread.ThreadingServiceImpl;
import com.espertech.esper.epl.expression.core.ExprNode;
import com.espertech.esper.epl.expression.core.ExprValidationException;
import com.espertech.esper.epl.spec.SelectClauseElementCompiled;
import com.espertech.esper.epl.spec.SelectClauseElementWildcard;
import com.espertech.esper.epl.spec.SelectClauseExprCompiledSpec;
import com.espertech.esper.core.service.speccompiled.SelectClauseStreamCompiledSpec;
import com.espertech.esper.epl.table.mgmt.TableServiceImpl;
import com.espertech.esper.supportunit.epl.SupportExprNodeFactory;
import com.espertech.esper.supportunit.epl.SupportStreamTypeSvc3Stream;
import com.espertech.esper.util.CollectionUtil;
import junit.framework.TestCase;

import java.util.Collections;

public class TestSelectExprProcessorFactory extends TestCase {
    private StatementResultService statementResultService = new StatementResultServiceImpl("name", null, null, new ThreadingServiceImpl(new ConfigurationEngineDefaults.Threading()));
    private SelectExprEventTypeRegistry selectExprEventTypeRegistry = new SelectExprEventTypeRegistry("abc", new StatementEventTypeRefImpl());

    public void testGetProcessorInvalid() throws Exception {
        SelectClauseElementCompiled[] selectionList = new SelectClauseElementCompiled[2];
        ExprNode identNode = SupportExprNodeFactory.makeIdentNode("doubleBoxed", "s0");
        ExprNode mathNode = SupportExprNodeFactory.makeMathNode();
        selectionList[0] = new SelectClauseExprCompiledSpec(identNode, "result", "result", false);
        selectionList[1] = new SelectClauseExprCompiledSpec(mathNode, "result", "result", false);

        try {
            SelectExprProcessorFactory.getProcessor(Collections.<Integer>emptyList(), selectionList, false, null, null, null,
                    new SupportStreamTypeSvc3Stream(), null, null, null, null, null, null, null, null, null, null, 1, null, null, null, new Configuration(), null, null, null, null, null);
            fail();
        } catch (ExprValidationException ex) {
            // Expected
        }
    }

    public void testGetProcessorWildcard() throws Exception {
        SelectClauseElementCompiled[] selectionList = new SelectClauseElementCompiled[]{new SelectClauseElementWildcard()};
        SelectExprProcessor processor = SelectExprProcessorFactory.getProcessor(Collections.<Integer>emptyList(), selectionList, false, null, null, null,
                new SupportStreamTypeSvc3Stream(), SupportEventAdapterService.getService(), statementResultService, null, selectExprEventTypeRegistry, SupportEngineImportServiceFactory.make(), null, null, new TableServiceImpl(), null, null, 1, null, null, null, new Configuration(), null, null, null, null, null).getSelectExprProcessor(SupportEngineImportServiceFactory.make(), false, "abc");
        assertTrue(processor instanceof SelectExprResultProcessor);
    }

    public void testGetProcessorValid() throws Exception {
        SelectClauseElementCompiled[] selectionList = new SelectClauseElementCompiled[1];
        ExprNode identNode = SupportExprNodeFactory.makeIdentNode("doubleBoxed", "s0");
        selectionList[0] = new SelectClauseExprCompiledSpec(identNode, "result", null, false);
        StatementContext statementContext = SupportStatementContextFactory.makeContext();
        SelectExprProcessor processor = SelectExprProcessorFactory.getProcessor(Collections.<Integer>emptyList(), selectionList, false, null, null, null,
                new SupportStreamTypeSvc3Stream(), SupportEventAdapterService.getService(), statementResultService, null, selectExprEventTypeRegistry, statementContext.getEngineImportService(), null, null, null, null, null, 1, null, null, null, new Configuration(), null, null, null, null, null).getSelectExprProcessor(SupportEngineImportServiceFactory.make(), false, "abc");
        assertTrue(processor != null);
    }

    public void testVerifyNameUniqueness() throws Exception {
        // try valid case
        SelectClauseElementCompiled[] elements = new SelectClauseElementCompiled[4];
        elements[0] = new SelectClauseExprCompiledSpec(null, "xx", null, false);
        elements[1] = new SelectClauseExprCompiledSpec(null, "yy", null, false);
        elements[2] = new SelectClauseStreamCompiledSpec("win", null);
        elements[3] = new SelectClauseStreamCompiledSpec("s2", "abc");

        SelectExprProcessorFactory.verifyNameUniqueness(elements);

        // try invalid case
        elements = (SelectClauseElementCompiled[]) CollectionUtil.arrayExpandAddSingle(elements, new SelectClauseExprCompiledSpec(null, "yy", null, false));
        try {
            SelectExprProcessorFactory.verifyNameUniqueness(elements);
            fail();
        } catch (ExprValidationException ex) {
            // expected
        }

        // try invalid case
        elements = new SelectClauseElementCompiled[2];
        elements[0] = new SelectClauseExprCompiledSpec(null, "abc", null, false);
        elements[1] = new SelectClauseStreamCompiledSpec("s0", "abc");
        try {
            SelectExprProcessorFactory.verifyNameUniqueness(elements);
            fail();
        } catch (ExprValidationException ex) {
            // expected
        }
    }
}
