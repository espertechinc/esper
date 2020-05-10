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
package com.espertech.esper.common.internal.compile.stage2;

public class FilterSpecPlanComputeFactory {
    public static FilterSpecPlanCompute make(FilterSpecPlan plan) {
        boolean hasTopControl = plan.filterConfirm != null || plan.filterNegate != null;
        boolean hasPathControl = false;
        boolean hasTripletControl = false;
        for (FilterSpecPlanPath path : plan.paths) {
            hasPathControl |= path.getPathNegate() != null;
            hasTripletControl |= path.hasTripletControl();
        }
        if (hasTripletControl) {
            return FilterSpecPlanComputeConditionalTriplets.INSTANCE;
        }
        if (hasPathControl) {
            return FilterSpecPlanComputeConditionalPath.INSTANCE;
        }
        if (hasTopControl) {
            return FilterSpecPlanComputeConditionalTopOnly.INSTANCE;
        }
        return FilterSpecPlanComputeUnconditional.INSTANCE;
    }
}
