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

import com.espertech.esper.epl.spec.*;
import com.espertech.esper.filterspec.FilterSpecCompiled;

import java.util.List;

public class ContextControllerFactoryFactorySvcImpl implements ContextControllerFactoryFactorySvc {

    public ContextControllerFactory make(ContextControllerFactoryContext factoryContext, ContextDetail detail, List<FilterSpecCompiled> optFiltersNested) {
        ContextControllerFactory factory;
        if (detail instanceof ContextDetailInitiatedTerminated) {
            factory = new ContextControllerInitTermFactoryImpl(factoryContext, (ContextDetailInitiatedTerminated) detail);
        } else if (detail instanceof ContextDetailPartitioned) {
            factory = new ContextControllerPartitionedFactoryImpl(factoryContext, (ContextDetailPartitioned) detail, optFiltersNested);
        } else if (detail instanceof ContextDetailCategory) {
            factory = new ContextControllerCategoryFactoryImpl(factoryContext, (ContextDetailCategory) detail, optFiltersNested);
        } else if (detail instanceof ContextDetailHash) {
            factory = new ContextControllerHashFactoryImpl(factoryContext, (ContextDetailHash) detail, optFiltersNested);
        } else {
            throw new UnsupportedOperationException("Context detail " + detail + " is not yet supported in a nested context");
        }

        return factory;
    }
}
