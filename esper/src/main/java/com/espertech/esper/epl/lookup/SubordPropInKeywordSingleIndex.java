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
package com.espertech.esper.epl.lookup;

import com.espertech.esper.epl.expression.core.ExprNode;

import java.io.Serializable;

/**
 * Holds property information for joined properties in a lookup.
 */
public class SubordPropInKeywordSingleIndex implements Serializable {
    private static final long serialVersionUID = 5502815704969387843L;
    private final String indexedProp;
    private final Class coercionType;
    private final ExprNode[] expressions;

    public SubordPropInKeywordSingleIndex(String indexedProp, Class coercionType, ExprNode[] expressions) {
        this.indexedProp = indexedProp;
        this.coercionType = coercionType;
        this.expressions = expressions;
    }

    public String getIndexedProp() {
        return indexedProp;
    }

    public Class getCoercionType() {
        return coercionType;
    }

    public ExprNode[] getExpressions() {
        return expressions;
    }
}
