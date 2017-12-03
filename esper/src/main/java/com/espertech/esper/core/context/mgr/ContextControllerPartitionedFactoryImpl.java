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

import com.espertech.esper.epl.spec.ContextDetailPartitioned;
import com.espertech.esper.filterspec.FilterSpecCompiled;

import java.util.List;

public class ContextControllerPartitionedFactoryImpl extends ContextControllerPartitionedFactoryBase implements ContextControllerFactory {

    private final ContextStatePathValueBinding binding;

    public ContextControllerPartitionedFactoryImpl(ContextControllerFactoryContext factoryContext, ContextDetailPartitioned segmentedSpec, List<FilterSpecCompiled> filtersSpecsNestedContexts) {
        super(factoryContext, segmentedSpec, filtersSpecsNestedContexts);
        this.binding = factoryContext.getStateCache().getBinding(ContextControllerPartitionedState.class);
    }

    public ContextStatePathValueBinding getBinding() {
        return binding;
    }

    public ContextController createNoCallback(int pathId, ContextControllerLifecycleCallback callback) {
        return new ContextControllerPartitioned(pathId, callback, this);
    }
}
