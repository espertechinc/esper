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
package com.espertech.esper.common.client.hook.aggmultifunc;

/**
 * Factory for aggregation multi-function accessors.
 */
public interface AggregationMultiFunctionAccessorFactory {
    /**
     * Returns the accessor
     *
     * @param ctx contextual information
     * @return accessor
     */
    AggregationMultiFunctionAccessor newAccessor(AggregationMultiFunctionAccessorFactoryContext ctx);
}
