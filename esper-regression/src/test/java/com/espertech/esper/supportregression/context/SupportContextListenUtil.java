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
package com.espertech.esper.supportregression.context;

import com.espertech.esper.client.context.*;

import java.util.function.Consumer;

import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.fail;
import static org.junit.Assert.assertEquals;

public class SupportContextListenUtil {
    public static Consumer<ContextStateEvent> eventContext(String contextName, Class type) {
        return event -> {
            assertEquals(contextName, event.getContextName());
            assertEquals(type, event.getClass());
        };
    }

    public static Consumer<ContextStateEvent> eventContextWStmt(String contextName, Class type, String statementName) {
        return event -> {
            assertEquals("default", event.getEngineURI());
            assertEquals(contextName, event.getContextName());
            assertEquals(type, event.getClass());
            if (event instanceof ContextStateEventContextStatementAdded) {
                assertEquals(statementName, ((ContextStateEventContextStatementAdded) event).getStatementName());
            }
            else if (event instanceof ContextStateEventContextStatementRemoved) {
                assertEquals(statementName, ((ContextStateEventContextStatementRemoved) event).getStatementName());
            }
            else {
                fail();
            }
        };
    }

    public static Consumer<ContextStateEvent> eventPartitionInitTerm(String contextName, Class type) {
        return event -> {
            assertEquals("default", event.getEngineURI());
            assertEquals(contextName, event.getContextName());
            assertEquals(type, event.getClass());
            ContextStateEventContextPartition partition = (ContextStateEventContextPartition) event;
            if (partition instanceof ContextStateEventContextPartitionAllocated) {
                ContextStateEventContextPartitionAllocated allocated = (ContextStateEventContextPartitionAllocated) event;
                ContextPartitionIdentifierInitiatedTerminated ident = (ContextPartitionIdentifierInitiatedTerminated) allocated.getIdentifier();
                assertNotNull(ident.getProperties().get("s0"));
            }
        };
    }
}
