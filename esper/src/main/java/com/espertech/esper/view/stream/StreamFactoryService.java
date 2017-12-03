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

import com.espertech.esper.collection.Pair;
import com.espertech.esper.core.context.util.AgentInstanceContext;
import com.espertech.esper.core.context.util.EPStatementAgentInstanceHandle;
import com.espertech.esper.core.service.StatementAgentInstanceLock;
import com.espertech.esper.filter.FilterService;
import com.espertech.esper.filterspec.FilterSpecCompiled;
import com.espertech.esper.view.EventStream;

import java.lang.annotation.Annotation;

/**
 * Service on top of the filter service for reuseing filter callbacks and their associated EventStream instances.
 * Same filter specifications (equal) do not need to be added to the filter service twice and the
 * EventStream instance that is the stream of events for that filter can be reused.
 * <p>
 * We are re-using streams such that views under such streams can be reused for efficient resource use.
 */
public interface StreamFactoryService {
    /**
     * Create or reuse existing EventStream instance representing that event filter.
     * When called for some filters, should return same stream.
     *
     * @param statementId                    the statement id
     * @param filterSpec                     event filter definition
     * @param filterService                  filter service to activate filter if not already active
     * @param epStatementAgentInstanceHandle is the statements-own handle for use in registering callbacks with services
     * @param isJoin                         is indicatng whether the stream will participate in a join statement, information
     *                                       necessary for stream reuse and multithreading concerns
     * @param agentInstanceContext           expression evaluation context
     * @param hasOrderBy                     if the consumer has order-by
     * @param annotations                    annotations
     * @param filterWithSameTypeSubselect    whether filter and subselect type match
     * @param isCanIterateUnbound            whether we can iterate unbound
     * @param stateless                      whether stateless
     * @param streamNum                      stream number
     * @return event stream representing active filter
     */
    public Pair<EventStream, StatementAgentInstanceLock> createStream(final int statementId,
                                                                      FilterSpecCompiled filterSpec,
                                                                      FilterService filterService,
                                                                      EPStatementAgentInstanceHandle epStatementAgentInstanceHandle,
                                                                      boolean isJoin,
                                                                      AgentInstanceContext agentInstanceContext,
                                                                      boolean hasOrderBy,
                                                                      boolean filterWithSameTypeSubselect,
                                                                      Annotation[] annotations,
                                                                      boolean stateless,
                                                                      int streamNum,
                                                                      boolean isCanIterateUnbound);

    /**
     * Drop the event stream associated with the filter passed in.
     * Throws an exception if already dropped.
     *
     * @param filterSpec                  is the event filter definition associated with the event stream to be dropped
     * @param filterService               to be used to deactivate filter when the last event stream is dropped
     * @param isJoin                      is indicatng whether the stream will participate in a join statement, information
     *                                    necessary for stream reuse and multithreading concerns
     * @param hasOrderBy                  if the consumer has an order-by clause
     * @param filterWithSameTypeSubselect whether filter and subselect event type match
     * @param stateless                   whether stateless
     */
    public void dropStream(FilterSpecCompiled filterSpec, FilterService filterService, boolean isJoin, boolean hasOrderBy, boolean filterWithSameTypeSubselect, boolean stateless);

    /**
     * Destroy the service.
     */
    public void destroy();
}
