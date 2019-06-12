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
package com.espertech.esper.common.internal.epl.lookupplan;

import com.espertech.esper.common.internal.epl.expression.core.ExprNode;

/**
 * Holds property information for joined properties in a lookup.
 */
public class SubordPropInKeywordMultiIndex {
    private String[] indexedProp;
    private Class coercionType;
    private ExprNode expression;

    public SubordPropInKeywordMultiIndex(String[] indexedProp, Class coercionType, ExprNode expression) {
        this.indexedProp = indexedProp;
        this.coercionType = coercionType;
        this.expression = expression;
    }

    public String[] getIndexedProp() {
        return indexedProp;
    }

    public Class getCoercionType() {
        return coercionType;
    }

    public ExprNode getExpression() {
        return expression;
    }
}
