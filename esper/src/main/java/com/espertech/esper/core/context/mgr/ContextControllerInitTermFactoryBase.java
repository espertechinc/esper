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
package com.espertech.esper.core.context.mgr;

import com.espertech.esper.client.EventType;
import com.espertech.esper.client.context.ContextPartitionIdentifier;
import com.espertech.esper.client.context.ContextPartitionIdentifierInitiatedTerminated;
import com.espertech.esper.core.context.stmt.*;
import com.espertech.esper.core.service.EPStatementHandle;
import com.espertech.esper.core.service.StatementContext;
import com.espertech.esper.epl.expression.core.ExprValidationException;
import com.espertech.esper.epl.spec.ContextDetailInitiatedTerminated;
import com.espertech.esper.epl.spec.ContextDetailPartitionItem;
import com.espertech.esper.filterspec.FilterSpecCompiled;
import com.espertech.esper.epl.expression.core.ExprFilterSpecLookupable;
import com.espertech.esper.filterspec.FilterValueSetParam;
import com.espertech.esper.filterspec.MatchedEventMapMeta;
import com.espertech.esper.schedule.SchedulingService;
import com.espertech.esper.schedule.TimeProvider;

import java.util.*;

public abstract class ContextControllerInitTermFactoryBase extends ContextControllerFactoryBase implements ContextControllerFactory {

    private final ContextDetailInitiatedTerminated detail;
    private Map<String, Object> contextBuiltinProps;
    private MatchedEventMapMeta matchedEventMapMeta;

    public ContextControllerInitTermFactoryBase(ContextControllerFactoryContext factoryContext, ContextDetailInitiatedTerminated detail) {
        super(factoryContext);
        this.detail = detail;
    }

    public void validateFactory() throws ExprValidationException {
        contextBuiltinProps = ContextPropertyEventType.getInitiatedTerminatedType();
        LinkedHashSet<String> allTags = new LinkedHashSet<String>();
        ContextPropertyEventType.addEndpointTypes(factoryContext.getContextName(), detail.getStart(), contextBuiltinProps, allTags);
        ContextPropertyEventType.addEndpointTypes(factoryContext.getContextName(), detail.getEnd(), contextBuiltinProps, allTags);
        matchedEventMapMeta = new MatchedEventMapMeta(allTags, false);
    }

    public Map<String, Object> getContextBuiltinProps() {
        return contextBuiltinProps;
    }

    public MatchedEventMapMeta getMatchedEventMapMeta() {
        return matchedEventMapMeta;
    }

    public ContextControllerStatementCtxCache validateStatement(ContextControllerStatementBase statement) {
        return null;
    }

    public void populateFilterAddendums(IdentityHashMap<FilterSpecCompiled, FilterValueSetParam[][]> filterAddendum, ContextControllerStatementDesc statement, Object key, int contextId) {
    }

    public ExprFilterSpecLookupable getFilterLookupable(EventType eventType) {
        return null;
    }

    public ContextDetailInitiatedTerminated getContextDetail() {
        return detail;
    }

    public List<ContextDetailPartitionItem> getContextDetailPartitionItems() {
        return Collections.emptyList();
    }

    public boolean isSingleInstanceContext() {
        return !detail.isOverlapping();
    }

    public long allocateSlot() {
        return factoryContext.getAgentInstanceContextCreate().getStatementContext().getScheduleBucket().allocateSlot();
    }

    public TimeProvider getTimeProvider() {
        return factoryContext.getAgentInstanceContextCreate().getStatementContext().getTimeProvider();
    }

    public SchedulingService getSchedulingService() {
        return factoryContext.getAgentInstanceContextCreate().getStatementContext().getSchedulingService();
    }

    public EPStatementHandle getEpStatementHandle() {
        return factoryContext.getAgentInstanceContextCreate().getStatementContext().getEpStatementHandle();
    }

    public StatementContext getStatementContext() {
        return factoryContext.getAgentInstanceContextCreate().getStatementContext();
    }

    public ContextPartitionIdentifier keyPayloadToIdentifier(Object payload) {
        ContextControllerInitTermState state = (ContextControllerInitTermState) payload;
        return new ContextPartitionIdentifierInitiatedTerminated(
                state == null ? null : state.getPatternData(),
                state == null ? 0 : state.getStartTime(),
                null);
    }

    public StatementAIResourceRegistryFactory getStatementAIResourceRegistryFactory() {
        if (getContextDetail().isOverlapping()) {
            return new StatementAIResourceRegistryFactory() {
                public StatementAIResourceRegistry make() {
                    return new StatementAIResourceRegistry(new AIRegistryAggregationMultiPerm(), new AIRegistryExprMultiPerm());
                }
            };
        } else {
            return new StatementAIResourceRegistryFactory() {
                public StatementAIResourceRegistry make() {
                    return new StatementAIResourceRegistry(new AIRegistryAggregationSingle(), new AIRegistryExprSingle());
                }
            };
        }
    }
}
