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

import com.espertech.esper.common.internal.epl.lookup.SubordTableLookupStrategyFactory;

public class SubordinateQueryPlanDesc {
    private SubordTableLookupStrategyFactory lookupStrategyFactory;
    private SubordinateQueryIndexDesc[] indexDescs;

    public void setLookupStrategyFactory(SubordTableLookupStrategyFactory lookupStrategyFactory) {
        this.lookupStrategyFactory = lookupStrategyFactory;
    }

    public void setIndexDescs(SubordinateQueryIndexDesc[] indexDescs) {
        this.indexDescs = indexDescs;
    }

    public SubordTableLookupStrategyFactory getLookupStrategyFactory() {
        return lookupStrategyFactory;
    }

    public SubordinateQueryIndexDesc[] getIndexDescs() {
        return indexDescs;
    }
}
