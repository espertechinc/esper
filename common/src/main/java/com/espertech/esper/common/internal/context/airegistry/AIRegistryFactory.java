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

public interface AIRegistryFactory {
    AIRegistryPriorEvalStrategy makePrior();

    AIRegistryPreviousGetterStrategy makePrevious();

    AIRegistrySubselectLookup makeSubqueryLookup(LookupStrategyDesc lookupStrategyDesc);

    AIRegistryAggregation makeAggregation();

    AIRegistryTableAccess makeTableAccess();

    AIRegistryRowRecogPreviousStrategy makeRowRecogPreviousStrategy();
}
