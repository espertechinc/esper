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

import com.espertech.esper.epl.expression.core.ExprValidationException;

public class ContextControllerFactoryServiceImpl implements ContextControllerFactoryService {

    private final static ContextStateCache CACHE_NO_SAVE = new ContextStateCacheNoSave();

    public final static ContextControllerFactoryServiceImpl DEFAULT_FACTORY = new ContextControllerFactoryServiceImpl(CACHE_NO_SAVE);

    private final ContextStateCache cache;

    public ContextControllerFactoryServiceImpl(ContextStateCache cache) {
        this.cache = cache;
    }

    public ContextControllerFactory[] getFactory(ContextControllerFactoryServiceContext serviceContext) throws ExprValidationException {
        return ContextControllerFactoryHelper.getFactory(serviceContext, cache);
    }

    public ContextPartitionIdManager allocatePartitionIdMgr(String contextName, int contextStmtId) {
        return new ContextPartitionIdManagerImpl();
    }
}
