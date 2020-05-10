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

import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluator;

public class FilterSpecPlanPath {
    public final static FilterSpecPlanPath[] EMPTY_ARRAY = new FilterSpecPlanPath[0];

    private FilterSpecPlanPathTriplet[] triplets;
    private ExprEvaluator pathNegate;

    public FilterSpecPlanPath() {
    }

    public FilterSpecPlanPath(FilterSpecPlanPathTriplet[] triplets) {
        this.triplets = triplets;
    }

    public void setTriplets(FilterSpecPlanPathTriplet[] triplets) {
        this.triplets = triplets;
    }

    public ExprEvaluator getPathNegate() {
        return pathNegate;
    }

    public void setPathNegate(ExprEvaluator pathNegate) {
        this.pathNegate = pathNegate;
    }

    public FilterSpecPlanPathTriplet[] getTriplets() {
        return triplets;
    }

    public boolean hasTripletControl() {
        for (FilterSpecPlanPathTriplet triplet : triplets) {
            if (triplet.getTripletConfirm() != null) {
                return true;
            }
        }
        return false;
    }
}
