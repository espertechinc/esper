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
package com.espertech.esper.core.service;

public class ExpressionResultCacheService {

    private final int declareExprCacheSize;
    private final ThreadLocal<ExpressionResultCacheServiceHolder> threadCache;

    public ExpressionResultCacheService(final int declareExprCacheSize) {
        this.declareExprCacheSize = declareExprCacheSize;
        this.threadCache = new ThreadLocal<ExpressionResultCacheServiceHolder>() {
            protected synchronized ExpressionResultCacheServiceHolder initialValue() {
                return new ExpressionResultCacheServiceHolder(declareExprCacheSize);
            }
        };
    }

    public ExpressionResultCacheForPropUnwrap getAllocateUnwrapProp() {
        return threadCache.get().getAllocateUnwrapProp();
    }

    public ExpressionResultCacheForDeclaredExprLastValue getAllocateDeclaredExprLastValue() {
        return threadCache.get().getAllocateDeclaredExprLastValue();
    }

    public ExpressionResultCacheForDeclaredExprLastColl getAllocateDeclaredExprLastColl() {
        return threadCache.get().getAllocateDeclaredExprLastColl();
    }

    public ExpressionResultCacheForEnumerationMethod getAllocateEnumerationMethod() {
        return threadCache.get().getAllocateEnumerationMethod();
    }

    public boolean isDeclaredExprCacheEnabled() {
        return declareExprCacheSize > 0;
    }
}
