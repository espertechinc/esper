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
package com.espertech.esper.common.internal.filterspec;

public class FilterSharedBoolExprRepositoryImpl implements FilterSharedBoolExprRepository {
    public final static FilterSharedBoolExprRepositoryImpl INSTANCE = new FilterSharedBoolExprRepositoryImpl();

    private FilterSharedBoolExprRepositoryImpl() {
    }

    public void registerBoolExpr(int statementId, FilterSpecParamExprNode node) {
    }

    public FilterSpecParamExprNode getFilterBoolExpr(int statementId, int filterBoolExprNum) {
        throw new UnsupportedOperationException("Not provided by this implementation");
    }

    public void removeStatement(int statementId) {
    }
}
