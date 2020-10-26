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
package com.espertech.esper.common.internal.epl.agg.method.core;

import com.espertech.esper.common.internal.epl.agg.method.plugin.AggregationForgeFactoryPlugin;

public class AggregatorMethodFactoryContext {
    private final AggregationForgeFactoryPlugin factory;

    public AggregatorMethodFactoryContext(AggregationForgeFactoryPlugin factory) {
        this.factory = factory;
    }

    public AggregationForgeFactoryPlugin getFactory() {
        return factory;
    }
}
