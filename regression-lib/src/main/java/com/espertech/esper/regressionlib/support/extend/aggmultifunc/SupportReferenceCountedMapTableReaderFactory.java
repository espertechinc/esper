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

import com.espertech.esper.common.client.hook.aggmultifunc.AggregationMultiFunctionTableReader;
import com.espertech.esper.common.client.hook.aggmultifunc.AggregationMultiFunctionTableReaderFactory;
import com.espertech.esper.common.client.hook.aggmultifunc.AggregationMultiFunctionTableReaderFactoryContext;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluator;

public class SupportReferenceCountedMapTableReaderFactory implements AggregationMultiFunctionTableReaderFactory {
    private ExprEvaluator eval;

    public AggregationMultiFunctionTableReader newReader(AggregationMultiFunctionTableReaderFactoryContext context) {
        return new SupportReferenceCountedMapTableReader(this);
    }

    public ExprEvaluator getEval() {
        return eval;
    }

    public void setEval(ExprEvaluator eval) {
        this.eval = eval;
    }
}
