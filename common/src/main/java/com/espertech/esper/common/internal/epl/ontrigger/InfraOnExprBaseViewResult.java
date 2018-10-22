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
package com.espertech.esper.common.internal.epl.ontrigger;

import com.espertech.esper.common.internal.epl.agg.core.AggregationService;
import com.espertech.esper.common.internal.view.core.View;

/**
 * View for the on-delete statement that handles removing events from a named window.
 */
public class InfraOnExprBaseViewResult {
    private final View view;
    private final AggregationService optionalAggregationService;

    public InfraOnExprBaseViewResult(View view, AggregationService optionalAggregationService) {
        this.view = view;
        this.optionalAggregationService = optionalAggregationService;
    }

    public View getView() {
        return view;
    }

    public AggregationService getOptionalAggregationService() {
        return optionalAggregationService;
    }
}
