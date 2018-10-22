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
package com.espertech.esper.common.internal.epl.agg.core;

import com.espertech.esper.common.internal.epl.expression.core.ExprForge;

public class AggregationRowStateForgeDesc {
    private final ExprForge[][] methodForges;
    private final AggregationForgeFactory[] optionalMethodFactories;
    private final AggregationStateFactoryForge[] accessFactoriesForges;
    private final AggregationAccessorSlotPairForge[] accessAccessorsForges;
    private final AggregationUseFlags useFlags;

    public AggregationRowStateForgeDesc(AggregationForgeFactory[] optionalMethodFactories, ExprForge[][] methodForges, AggregationStateFactoryForge[] accessFactoriesForges, AggregationAccessorSlotPairForge[] accessAccessorsForges, AggregationUseFlags useFlags) {
        this.methodForges = methodForges;
        this.optionalMethodFactories = optionalMethodFactories;
        this.accessAccessorsForges = accessAccessorsForges;
        this.accessFactoriesForges = accessFactoriesForges;
        this.useFlags = useFlags;
    }

    public ExprForge[][] getMethodForges() {
        return methodForges;
    }

    public AggregationForgeFactory[] getOptionalMethodFactories() {
        return optionalMethodFactories;
    }

    public AggregationAccessorSlotPairForge[] getAccessAccessorsForges() {
        return accessAccessorsForges;
    }

    public AggregationStateFactoryForge[] getAccessFactoriesForges() {
        return accessFactoriesForges;
    }

    public AggregationUseFlags getUseFlags() {
        return useFlags;
    }

    public int getNumMethods() {
        return methodForges == null ? 0 : methodForges.length;
    }

    public int getNumAccess() {
        return accessAccessorsForges == null ? 0 : accessAccessorsForges.length;
    }
}
