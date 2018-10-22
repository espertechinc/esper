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
package com.espertech.esper.regressionlib.support.extend.aggmultifunc;

import com.espertech.esper.common.client.hook.aggmultifunc.*;
import com.espertech.esper.common.internal.epl.expression.core.ExprNode;

public class SupportReferenceCountedMapForge implements AggregationMultiFunctionForge {
    private final static AggregationMultiFunctionStateKey SHARED_STATE_KEY = new AggregationMultiFunctionStateKey() {
    };

    public void addAggregationFunction(AggregationMultiFunctionDeclarationContext declarationContext) {
    }

    public AggregationMultiFunctionHandler validateGetHandler(AggregationMultiFunctionValidationContext validationContext) {
        if (validationContext.getFunctionName().equals("referenceCountedMap")) {
            return new SupportReferenceCountedMapRCMFunctionHandler(SHARED_STATE_KEY, validationContext.getParameterExpressions());
        }
        if (validationContext.getFunctionName().equals("referenceCountLookup")) {
            ExprNode eval = validationContext.getParameterExpressions()[0];
            return new SupportReferenceCountedMapRCLFunctionHandler(eval);
        }
        throw new IllegalArgumentException("Unexpected function name '" + validationContext.getFunctionName());
    }

}
