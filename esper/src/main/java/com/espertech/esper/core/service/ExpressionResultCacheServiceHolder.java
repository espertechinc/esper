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

public class ExpressionResultCacheServiceHolder {

    private final int declareExprCacheSize;

    private ExpressionResultCacheForPropUnwrap propUnwrap;
    private ExpressionResultCacheForDeclaredExprLastValue declaredExprLastValue;
    private ExpressionResultCacheForDeclaredExprLastColl declaredExprLastColl;
    private ExpressionResultCacheForEnumerationMethod enumerationMethod;

    public ExpressionResultCacheServiceHolder(int declareExprCacheSize) {
        this.declareExprCacheSize = declareExprCacheSize;
    }

    public ExpressionResultCacheForPropUnwrap getAllocateUnwrapProp() {
        if (propUnwrap == null) {
            propUnwrap = new ExpressionResultCacheForPropUnwrapImpl();
        }
        return propUnwrap;
    }

    public ExpressionResultCacheForDeclaredExprLastValue getAllocateDeclaredExprLastValue() {
        if (declaredExprLastValue == null) {
            if (declareExprCacheSize < 1) {
                declaredExprLastValue = new ExpressionResultCacheForDeclaredExprLastValueNone();
            } else if (declareExprCacheSize < 2) {
                declaredExprLastValue = new ExpressionResultCacheForDeclaredExprLastValueSingle();
            } else {
                declaredExprLastValue = new ExpressionResultCacheForDeclaredExprLastValueMulti(declareExprCacheSize);
            }
        }
        return declaredExprLastValue;
    }

    public ExpressionResultCacheForDeclaredExprLastColl getAllocateDeclaredExprLastColl() {
        if (declaredExprLastColl == null) {
            declaredExprLastColl = new ExpressionResultCacheForDeclaredExprLastCollImpl();
        }
        return declaredExprLastColl;
    }

    public ExpressionResultCacheForEnumerationMethod getAllocateEnumerationMethod() {
        if (enumerationMethod == null) {
            enumerationMethod = new ExpressionResultCacheForEnumerationMethodImpl();
        }
        return enumerationMethod;
    }
}
