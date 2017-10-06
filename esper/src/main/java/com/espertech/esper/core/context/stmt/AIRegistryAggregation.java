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

import com.espertech.esper.epl.agg.service.common.AggregationService;

public interface AIRegistryAggregation extends AggregationService {
    public void assignService(int serviceId, AggregationService aggregationService);

    public void deassignService(int serviceId);

    public int getInstanceCount();
}
