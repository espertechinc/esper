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
package com.espertech.esper.common.internal.epl.expression.dot.core;

import com.espertech.esper.common.internal.epl.expression.core.ExprForgeConstantType;
import com.espertech.esper.common.internal.epl.expression.core.ExprForgeInstrumentable;
import com.espertech.esper.common.internal.epl.join.analyze.FilterExprAnalyzerAffector;

public abstract class ExprDotNodeForge implements ExprForgeInstrumentable {
    public abstract boolean isReturnsConstantResult();

    public abstract FilterExprAnalyzerAffector getFilterExprAnalyzerAffector();

    public abstract Integer getStreamNumReferenced();

    public abstract String getRootPropertyName();

    public ExprForgeConstantType getForgeConstantType() {
        if (isReturnsConstantResult()) {
            return ExprForgeConstantType.DEPLOYCONST;
        } else {
            return ExprForgeConstantType.NONCONST;
        }
    }
}

