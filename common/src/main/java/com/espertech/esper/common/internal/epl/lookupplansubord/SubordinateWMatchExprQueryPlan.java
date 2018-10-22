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
package com.espertech.esper.common.internal.epl.lookupplansubord;

public class SubordinateWMatchExprQueryPlan {
    private final SubordWMatchExprLookupStrategyFactory factory;
    private final SubordinateQueryIndexDesc[] indexDescs;

    public SubordinateWMatchExprQueryPlan(SubordWMatchExprLookupStrategyFactory factory, SubordinateQueryIndexDesc[] indexDescs) {
        this.factory = factory;
        this.indexDescs = indexDescs;
    }

    public SubordWMatchExprLookupStrategyFactory getFactory() {
        return factory;
    }

    public SubordinateQueryIndexDesc[] getIndexDescs() {
        return indexDescs;
    }
}
