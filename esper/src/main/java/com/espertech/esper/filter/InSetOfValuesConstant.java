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
package com.espertech.esper.filter;

import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.pattern.MatchedEventMap;

/**
 * Constant value in a list of values following an in-keyword.
 */
public class InSetOfValuesConstant implements FilterSpecParamInValue {
    private Object constant;
    private static final long serialVersionUID = 575037486475447197L;

    /**
     * Ctor.
     *
     * @param constant is the constant value
     */
    public InSetOfValuesConstant(Object constant) {
        this.constant = constant;
    }

    public Object getFilterValue(MatchedEventMap matchedEvents, ExprEvaluatorContext evaluatorContext) {
        return constant;
    }

    public Class getReturnType() {
        return constant == null ? null : constant.getClass();
    }

    public boolean constant() {
        return true;
    }

    /**
     * Returns the constant value.
     *
     * @return constant
     */
    public Object getConstant() {
        return constant;
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        InSetOfValuesConstant that = (InSetOfValuesConstant) o;

        if (constant != null ? !constant.equals(that.constant) : that.constant != null) {
            return false;
        }

        return true;
    }

    public int hashCode() {
        return constant != null ? constant.hashCode() : 0;
    }
}
