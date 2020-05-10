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
import com.espertech.esper.common.internal.filterspec.FilterSpecParam;

public class FilterSpecPlanPathTriplet {
    private FilterSpecParam param;
    private ExprEvaluator tripletConfirm;

    public FilterSpecPlanPathTriplet() {
    }

    public FilterSpecPlanPathTriplet(FilterSpecParam param) {
        this.param = param;
    }

    public FilterSpecParam getParam() {
        return param;
    }

    public void setParam(FilterSpecParam param) {
        this.param = param;
    }

    public ExprEvaluator getTripletConfirm() {
        return tripletConfirm;
    }

    public void setTripletConfirm(ExprEvaluator tripletConfirm) {
        this.tripletConfirm = tripletConfirm;
    }
}
