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
package com.espertech.esper.epl.join.base;

import com.espertech.esper.epl.expression.core.ExprEvaluator;

public class JoinSetComposerDesc {
    private final JoinSetComposer joinSetComposer;
    private final ExprEvaluator postJoinFilterEvaluator;

    public JoinSetComposerDesc(JoinSetComposer joinSetComposer, ExprEvaluator postJoinFilterEvaluator) {
        this.joinSetComposer = joinSetComposer;
        this.postJoinFilterEvaluator = postJoinFilterEvaluator;
    }

    public JoinSetComposer getJoinSetComposer() {
        return joinSetComposer;
    }

    public ExprEvaluator getPostJoinFilterEvaluator() {
        return postJoinFilterEvaluator;
    }
}
