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
package com.espertech.esper.regressionlib.support.dataflow;

import com.espertech.esper.common.client.dataflow.annotations.DataFlowContext;
import com.espertech.esper.common.client.dataflow.annotations.OutputType;
import com.espertech.esper.common.client.dataflow.annotations.OutputTypes;
import com.espertech.esper.common.client.dataflow.util.EPDataFlowSignal;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.context.aifactory.core.SAIFFInitializeSymbol;
import com.espertech.esper.common.internal.epl.dataflow.interfaces.*;
import com.espertech.esper.common.internal.epl.expression.core.ExprValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.newInstance;

@OutputTypes(value = {
    @OutputType(name = "stats", type = MyWordCountStats.class)
})
public class MyWordCountAggregator implements DataFlowOperatorForge, DataFlowOperatorFactory, DataFlowOperator {
    private static final Logger log = LoggerFactory.getLogger(MyWordCountAggregator.class);

    @DataFlowContext
    private EPDataFlowEmitter graphContext;

    private final MyWordCountStats aggregate = new MyWordCountStats();

    public DataFlowOpForgeInitializeResult initializeForge(DataFlowOpForgeInitializeContext context) throws ExprValidationException {
        return null;
    }

    public CodegenExpression make(CodegenMethodScope parent, SAIFFInitializeSymbol symbols, CodegenClassScope classScope) {
        return newInstance(MyWordCountAggregator.class);
    }

    public void initializeFactory(DataFlowOpFactoryInitializeContext context) {
    }

    public DataFlowOperator operator(DataFlowOpInitializeContext context) {
        return new MyWordCountAggregator();
    }

    public void onInput(int lines, int words, int chars) {
        aggregate.add(lines, words, chars);
        log.debug("Aggregated: " + aggregate);
    }

    public void onSignal(EPDataFlowSignal signal) {
        log.debug("Received punctuation, submitting totals: " + aggregate);
        graphContext.submit(aggregate);
    }
}
