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

import com.espertech.esper.epl.spec.ContextDetailHash;
import com.espertech.esper.filterspec.FilterSpecCompiled;

import java.util.List;

public class ContextControllerHashFactoryImpl extends ContextControllerHashFactoryBase implements ContextControllerFactory {

    private final ContextStatePathValueBinding binding;

    public ContextControllerHashFactoryImpl(ContextControllerFactoryContext factoryContext, ContextDetailHash hashedSpec, List<FilterSpecCompiled> filtersSpecsNestedContexts) {
        super(factoryContext, hashedSpec, filtersSpecsNestedContexts);
        this.binding = factoryContext.getStateCache().getBinding(Integer.class);
    }

    public ContextController createNoCallback(int pathId, ContextControllerLifecycleCallback callback) {
        return new ContextControllerHash(pathId, callback, this);
    }

    public ContextStatePathValueBinding getBinding() {
        return binding;
    }
}
