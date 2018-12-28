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
package com.espertech.esper.common.internal.epl.agg.method.core;

import com.espertech.esper.common.client.hook.aggmultifunc.AggregationMultiFunctionStateKey;
import com.espertech.esper.common.internal.epl.agg.access.core.AggregationAgentForge;
import com.espertech.esper.common.internal.epl.agg.core.AggregationAccessorForge;
import com.espertech.esper.common.internal.epl.agg.core.AggregationForgeFactory;
import com.espertech.esper.common.internal.epl.agg.core.AggregationStateFactoryForge;
import com.espertech.esper.common.internal.settings.ClasspathImportService;

public abstract class AggregationForgeFactoryBase implements AggregationForgeFactory {
    public final AggregationMultiFunctionStateKey getAggregationStateKey(boolean isMatchRecognize) {
        throw new IllegalStateException("Not an access aggregation function");
    }

    public final AggregationStateFactoryForge getAggregationStateFactory(boolean isMatchRecognize) {
        throw new IllegalStateException("Not an access aggregation function");
    }

    public final AggregationAccessorForge getAccessorForge() {
        throw new IllegalStateException("Not an access aggregation function");
    }

    public final AggregationAgentForge getAggregationStateAgent(ClasspathImportService classpathImportService, String statementName) {
        throw new IllegalStateException("Not an access aggregation function");
    }

    public final boolean isAccessAggregation() {
        return false;
    }
}
