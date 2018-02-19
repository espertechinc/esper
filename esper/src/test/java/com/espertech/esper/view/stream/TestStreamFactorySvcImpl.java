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
package com.espertech.esper.view.stream;

import com.espertech.esper.client.EventType;
import com.espertech.esper.core.context.util.AgentInstanceContext;
import com.espertech.esper.core.context.util.EPStatementAgentInstanceHandle;
import com.espertech.esper.core.service.EPStatementHandle;
import com.espertech.esper.core.service.StatementAgentInstanceRWLockImpl;
import com.espertech.esper.core.service.StatementType;
import com.espertech.esper.core.service.multimatch.MultiMatchHandlerFactoryImpl;
import com.espertech.esper.core.support.SupportStatementContextFactory;
import com.espertech.esper.filterspec.FilterOperator;
import com.espertech.esper.filterspec.FilterSpecCompiled;
import com.espertech.esper.supportunit.bean.SupportBean;
import com.espertech.esper.supportunit.event.SupportEventTypeFactory;
import com.espertech.esper.supportunit.filter.SupportFilterServiceImpl;
import com.espertech.esper.supportunit.filter.SupportFilterSpecBuilder;
import com.espertech.esper.view.EventStream;
import junit.framework.TestCase;

public class TestStreamFactorySvcImpl extends TestCase {
    private StreamFactoryService streamFactoryService;
    private SupportFilterServiceImpl supportFilterService;

    private FilterSpecCompiled[] filterSpecs;
    private EventStream[] streams;
    private EPStatementHandle handle = new EPStatementHandle(1, "name", "text", StatementType.SELECT, "text", false, null, 1, false, false, new MultiMatchHandlerFactoryImpl().getDefaultHandler());
    private EPStatementAgentInstanceHandle agentHandle = new EPStatementAgentInstanceHandle(handle, new StatementAgentInstanceRWLockImpl(false), -1, null, null);
    private AgentInstanceContext agentInstanceContext = SupportStatementContextFactory.makeAgentInstanceContext();

    public void setUp() {
        supportFilterService = new SupportFilterServiceImpl();
        streamFactoryService = new StreamFactorySvcImpl("default", true);
        EventType eventType = SupportEventTypeFactory.createBeanType(SupportBean.class);

        filterSpecs = new FilterSpecCompiled[3];
        filterSpecs[0] = SupportFilterSpecBuilder.build(eventType, new Object[]{"string", FilterOperator.EQUAL, "a"});
        filterSpecs[1] = SupportFilterSpecBuilder.build(eventType, new Object[]{"string", FilterOperator.EQUAL, "a"});
        filterSpecs[2] = SupportFilterSpecBuilder.build(eventType, new Object[]{"string", FilterOperator.EQUAL, "b"});
    }

    public void testInvalidJoin() {
        streams = new EventStream[3];
        streams[0] = streamFactoryService.createStream(1, filterSpecs[0], supportFilterService, agentHandle, true, agentInstanceContext, false, false, null, false, 0, false).getFirst();

        try {
            // try to reuse the same filter spec object, should fail
            streamFactoryService.createStream(1, filterSpecs[0], supportFilterService, agentHandle, true, null, false, false, null, false, 0, false);
            fail();
        } catch (IllegalStateException ex) {
            // expected
        }
    }

    public void testCreateJoin() {
        streams = new EventStream[3];
        streams[0] = streamFactoryService.createStream(1, filterSpecs[0], supportFilterService, agentHandle, true, agentInstanceContext, false, false, null, false, 0, false).getFirst();
        streams[1] = streamFactoryService.createStream(1, filterSpecs[1], supportFilterService, agentHandle, true, agentInstanceContext, false, false, null, false, 0, false).getFirst();
        streams[2] = streamFactoryService.createStream(1, filterSpecs[2], supportFilterService, agentHandle, true, agentInstanceContext, false, false, null, false, 0, false).getFirst();

        // Streams are reused
        assertNotSame(streams[0], streams[1]);
        assertNotSame(streams[0], streams[2]);
        assertNotSame(streams[1], streams[2]);

        // Type is ok
        assertEquals(SupportBean.class, streams[0].getEventType().getUnderlyingType());

        // 2 filters are active now
        assertEquals(3, supportFilterService.getAdded().size());
    }

    public void testDropJoin() {
        streams = new EventStream[3];
        streams[0] = streamFactoryService.createStream(1, filterSpecs[0], supportFilterService, agentHandle, true, agentInstanceContext, false, false, null, false, 0, false).getFirst();
        streams[1] = streamFactoryService.createStream(2, filterSpecs[1], supportFilterService, agentHandle, true, agentInstanceContext, false, false, null, false, 0, false).getFirst();
        streams[2] = streamFactoryService.createStream(3, filterSpecs[2], supportFilterService, agentHandle, true, agentInstanceContext, false, false, null, false, 0, false).getFirst();

        streamFactoryService.dropStream(filterSpecs[0], supportFilterService, true, false, false, false);
        streamFactoryService.dropStream(filterSpecs[1], supportFilterService, true, false, false, false);
        assertEquals(2, supportFilterService.getRemoved().size());

        // Filter removed
        streamFactoryService.dropStream(filterSpecs[2], supportFilterService, true, false, false, false);
        assertEquals(3, supportFilterService.getRemoved().size());

        // Something already removed
        try {
            streamFactoryService.dropStream(filterSpecs[2], supportFilterService, true, false, false, false);
            TestCase.fail();
        } catch (IllegalStateException ex) {
            // Expected
        }
    }

    public void testCreateNoJoin() {
        EPStatementHandle stmtHande = new EPStatementHandle(1, "id", null, StatementType.SELECT, "text", false, null, 1, false, false, new MultiMatchHandlerFactoryImpl().getDefaultHandler());
        EPStatementAgentInstanceHandle stmtAgentHandle = new EPStatementAgentInstanceHandle(stmtHande, new StatementAgentInstanceRWLockImpl(false), -1, null, null);

        streams = new EventStream[4];
        streams[0] = streamFactoryService.createStream(1, filterSpecs[0], supportFilterService, stmtAgentHandle, false, agentInstanceContext, false, false, null, false, 0, false).getFirst();
        streams[1] = streamFactoryService.createStream(2, filterSpecs[0], supportFilterService, stmtAgentHandle, false, agentInstanceContext, false, false, null, false, 0, false).getFirst();
        streams[2] = streamFactoryService.createStream(3, filterSpecs[1], supportFilterService, stmtAgentHandle, false, agentInstanceContext, false, false, null, false, 0, false).getFirst();
        streams[3] = streamFactoryService.createStream(4, filterSpecs[2], supportFilterService, stmtAgentHandle, false, agentInstanceContext, false, false, null, false, 0, false).getFirst();

        // Streams are reused
        assertSame(streams[0], streams[1]);
        assertSame(streams[0], streams[2]);
        assertNotSame(streams[0], streams[3]);

        // Type is ok
        assertEquals(SupportBean.class, streams[0].getEventType().getUnderlyingType());

        // 2 filters are active now
        assertEquals(2, supportFilterService.getAdded().size());
    }

    public void testDropNoJoin() {
        EPStatementHandle stmtHande = new EPStatementHandle(1, "id", null, StatementType.SELECT, "text", false, null, 1, false, false, new MultiMatchHandlerFactoryImpl().getDefaultHandler());
        EPStatementAgentInstanceHandle stmtAgentHandle = new EPStatementAgentInstanceHandle(stmtHande, new StatementAgentInstanceRWLockImpl(false), -1, null, null);
        streams = new EventStream[4];
        streams[0] = streamFactoryService.createStream(1, filterSpecs[0], supportFilterService, stmtAgentHandle, false, agentInstanceContext, false, false, null, false, 0, false).getFirst();
        streams[1] = streamFactoryService.createStream(2, filterSpecs[0], supportFilterService, stmtAgentHandle, false, agentInstanceContext, false, false, null, false, 0, false).getFirst();
        streams[2] = streamFactoryService.createStream(3, filterSpecs[1], supportFilterService, stmtAgentHandle, false, agentInstanceContext, false, false, null, false, 0, false).getFirst();
        streams[3] = streamFactoryService.createStream(4, filterSpecs[2], supportFilterService, stmtAgentHandle, false, agentInstanceContext, false, false, null, false, 0, false).getFirst();

        streamFactoryService.dropStream(filterSpecs[0], supportFilterService, false, false, false, false);
        streamFactoryService.dropStream(filterSpecs[1], supportFilterService, false, false, false, false);
        assertEquals(0, supportFilterService.getRemoved().size());

        // Filter removed
        streamFactoryService.dropStream(filterSpecs[0], supportFilterService, false, false, false, false);
        assertEquals(1, supportFilterService.getRemoved().size());

        streamFactoryService.dropStream(filterSpecs[2], supportFilterService, false, false, false, false);
        assertEquals(2, supportFilterService.getRemoved().size());

        // Something already removed
        try {
            streamFactoryService.dropStream(filterSpecs[2], supportFilterService, false, false, false, false);
            TestCase.fail();
        } catch (IllegalStateException ex) {
            // Expected
        }
    }
}
