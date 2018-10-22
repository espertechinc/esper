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
package com.espertech.esper.regressionlib.support.context;

import com.espertech.esper.common.client.context.*;

import java.util.function.Consumer;

import static junit.framework.TestCase.fail;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class SupportContextListenUtil {
    public static Consumer<ContextStateEvent> eventContext(String deploymentId, String contextName, Class type) {
        return event -> {
            assertEquals(deploymentId, event.getContextDeploymentId());
            assertEquals(contextName, event.getContextName());
            assertEquals(type, event.getClass());
        };
    }

    public static Consumer<ContextStateEvent> eventContextWStmt(String contextDeploymentId, String contextName, Class type, String statementDeploymentId, String statementName) {
        return event -> {
            assertEquals("default", event.getRuntimeURI());
            assertEquals(contextDeploymentId, event.getContextDeploymentId());
            assertEquals(contextName, event.getContextName());
            assertEquals(type, event.getClass());
            if (event instanceof ContextStateEventContextStatementAdded) {
                ContextStateEventContextStatementAdded added = (ContextStateEventContextStatementAdded) event;
                assertEquals(statementDeploymentId, added.getStatementDeploymentId());
                assertEquals(statementName, added.getStatementName());
            } else if (event instanceof ContextStateEventContextStatementRemoved) {
                ContextStateEventContextStatementRemoved removed = (ContextStateEventContextStatementRemoved) event;
                assertEquals(statementDeploymentId, removed.getStatementDeploymentId());
                assertEquals(statementName, removed.getStatementName());
            } else {
                fail();
            }
        };
    }

    public static Consumer<ContextStateEvent> eventPartitionInitTerm(String contextDeploymentId, String contextName, Class type) {
        return event -> {
            assertEquals("default", event.getRuntimeURI());
            assertEquals(contextDeploymentId, event.getContextDeploymentId());
            assertEquals(contextName, event.getContextName());
            assertEquals(type, event.getClass());
            ContextStateEventContextPartition partition = (ContextStateEventContextPartition) event;
            if (partition instanceof ContextStateEventContextPartitionAllocated) {
                ContextStateEventContextPartitionAllocated allocated = (ContextStateEventContextPartitionAllocated) event;
                if (allocated.getIdentifier() instanceof ContextPartitionIdentifierInitiatedTerminated) {
                    ContextPartitionIdentifierInitiatedTerminated ident = (ContextPartitionIdentifierInitiatedTerminated) allocated.getIdentifier();
                    assertNotNull(ident.getProperties().get("s0"));
                } else if (allocated.getIdentifier() instanceof ContextPartitionIdentifierNested) {
                    ContextPartitionIdentifierNested nested = (ContextPartitionIdentifierNested) allocated.getIdentifier();
                    ContextPartitionIdentifierInitiatedTerminated ident = (ContextPartitionIdentifierInitiatedTerminated) nested.getIdentifiers()[1];
                    assertNotNull(ident.getProperties().get("s0"));
                }
            }
        };
    }
}
