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
package com.espertech.esper.epl.lookup;

public class SubordinateQueryPlanDesc {
    private final SubordTableLookupStrategyFactory lookupStrategyFactory;
    private final SubordinateQueryIndexDesc[] indexDescs;

    public SubordinateQueryPlanDesc(SubordTableLookupStrategyFactory lookupStrategyFactory, SubordinateQueryIndexDesc[] indexDescs) {
        this.lookupStrategyFactory = lookupStrategyFactory;
        this.indexDescs = indexDescs;
    }

    public SubordTableLookupStrategyFactory getLookupStrategyFactory() {
        return lookupStrategyFactory;
    }

    public SubordinateQueryIndexDesc[] getIndexDescs() {
        return indexDescs;
    }
}
