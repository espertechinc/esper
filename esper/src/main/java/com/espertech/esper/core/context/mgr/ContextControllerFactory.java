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
import com.espertech.esper.core.context.stmt.StatementAIResourceRegistryFactory;
import com.espertech.esper.epl.expression.core.ExprValidationException;
import com.espertech.esper.epl.spec.ContextDetail;
import com.espertech.esper.epl.spec.ContextDetailPartitionItem;
import com.espertech.esper.filterspec.FilterSpecCompiled;
import com.espertech.esper.epl.expression.core.ExprFilterSpecLookupable;
import com.espertech.esper.filterspec.FilterValueSetParam;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

public interface ContextControllerFactory {

    public ContextControllerFactoryContext getFactoryContext();

    public Map<String, Object> getContextBuiltinProps();

    public boolean isSingleInstanceContext();

    public ContextDetail getContextDetail();

    public List<ContextDetailPartitionItem> getContextDetailPartitionItems();

    public StatementAIResourceRegistryFactory getStatementAIResourceRegistryFactory();

    public void validateFactory() throws ExprValidationException;

    public ContextControllerStatementCtxCache validateStatement(ContextControllerStatementBase statement) throws ExprValidationException;

    public ContextController createNoCallback(int pathId, ContextControllerLifecycleCallback callback);

    public void populateFilterAddendums(IdentityHashMap<FilterSpecCompiled, FilterValueSetParam[][]> filterAddendum, ContextControllerStatementDesc statement, Object key, int contextId);

    public ExprFilterSpecLookupable getFilterLookupable(EventType eventType);

    public ContextPartitionIdentifier keyPayloadToIdentifier(Object payload);

    public ContextStateCache getStateCache();
}
