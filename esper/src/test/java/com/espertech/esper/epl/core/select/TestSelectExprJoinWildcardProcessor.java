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
import com.espertech.esper.client.EventBean;
import com.espertech.esper.core.service.StatementEventTypeRefImpl;
import com.espertech.esper.core.support.SupportEngineImportServiceFactory;
import com.espertech.esper.core.support.SupportEventAdapterService;
import com.espertech.esper.epl.core.select.SelectExprEventTypeRegistry;
import com.espertech.esper.epl.core.select.SelectExprJoinWildcardProcessorFactory;
import com.espertech.esper.epl.core.select.SelectExprProcessor;
import com.espertech.esper.epl.core.select.SelectExprProcessorForge;
import com.espertech.esper.epl.expression.core.ExprValidationException;
import com.espertech.esper.epl.table.mgmt.TableServiceImpl;
import com.espertech.esper.supportunit.bean.SupportBean;
import com.espertech.esper.supportunit.epl.SupportStreamTypeSvc3Stream;
import junit.framework.TestCase;

import java.util.Collections;

public class TestSelectExprJoinWildcardProcessor extends TestCase {
    private SelectExprProcessorForge forge;
    private SelectExprProcessor processor;

    public void setUp() throws ExprValidationException {
        SelectExprEventTypeRegistry selectExprEventTypeRegistry = new SelectExprEventTypeRegistry("abc", new StatementEventTypeRefImpl());
        SupportStreamTypeSvc3Stream supportTypes = new SupportStreamTypeSvc3Stream();

        forge = SelectExprJoinWildcardProcessorFactory.create(Collections.<Integer>emptyList(), 1, "stmtname", supportTypes.getStreamNames(), supportTypes.getEventTypes(),
                SupportEventAdapterService.getService(), null, selectExprEventTypeRegistry, null, null, new Configuration(), new TableServiceImpl(), "default", false);
        processor = forge.getSelectExprProcessor(SupportEngineImportServiceFactory.make(), false, "stmtName");
    }

    public void testProcess() {
        EventBean[] testEvents = SupportStreamTypeSvc3Stream.getSampleEvents();

        EventBean result = processor.process(testEvents, true, false, null);
        assertEquals(testEvents[0].getUnderlying(), result.get("s0"));
        assertEquals(testEvents[1].getUnderlying(), result.get("s1"));

        // Test null events, such as in an outer join
        testEvents[1] = null;
        result = processor.process(testEvents, true, false, null);
        assertEquals(testEvents[0].getUnderlying(), result.get("s0"));
        assertNull(result.get("s1"));
    }

    public void testType() {
        assertEquals(SupportBean.class, forge.getResultEventType().getPropertyType("s0"));
        assertEquals(SupportBean.class, forge.getResultEventType().getPropertyType("s1"));
    }
}
