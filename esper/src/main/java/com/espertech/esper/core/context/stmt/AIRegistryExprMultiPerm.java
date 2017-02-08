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
package com.espertech.esper.core.context.stmt;

public class AIRegistryExprMultiPerm extends AIRegistryExprBase {

    public AIRegistryExprMultiPerm() {
    }

    public AIRegistrySubselect allocateAIRegistrySubselect() {
        return new AIRegistrySubselectMultiPerm();
    }

    public AIRegistryPrevious allocateAIRegistryPrevious() {
        return new AIRegistryPreviousMultiPerm();
    }

    public AIRegistryPrior allocateAIRegistryPrior() {
        return new AIRegistryPriorMultiPerm();
    }

    public AIRegistryAggregation allocateAIRegistrySubselectAggregation() {
        return new AIRegistryAggregationMultiPerm();
    }

    public AIRegistryMatchRecognizePrevious allocateAIRegistryMatchRecognizePrevious() {
        return new AIRegistryMatchRecognizePreviousMultiPerm();
    }

    public AIRegistryTableAccess allocateAIRegistryTableAccess() {
        return new AIRegistryTableAccessMultiPerm();
    }
}
