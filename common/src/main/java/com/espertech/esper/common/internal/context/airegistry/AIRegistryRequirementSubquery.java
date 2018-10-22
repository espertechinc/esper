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
package com.espertech.esper.common.internal.context.airegistry;

import com.espertech.esper.common.internal.epl.lookup.LookupStrategyDesc;

public class AIRegistryRequirementSubquery {
    private final boolean hasAggregation;
    private final boolean hasPrior;
    private final boolean hasPrev;
    private final LookupStrategyDesc lookupStrategyDesc;

    public AIRegistryRequirementSubquery(boolean hasAggregation, boolean hasPrior, boolean hasPrev, LookupStrategyDesc lookupStrategyDesc) {
        this.hasAggregation = hasAggregation;
        this.hasPrior = hasPrior;
        this.hasPrev = hasPrev;
        this.lookupStrategyDesc = lookupStrategyDesc;
    }

    public boolean isHasAggregation() {
        return hasAggregation;
    }

    public boolean isHasPrior() {
        return hasPrior;
    }

    public boolean isHasPrev() {
        return hasPrev;
    }

    public LookupStrategyDesc getLookupStrategyDesc() {
        return lookupStrategyDesc;
    }
}
