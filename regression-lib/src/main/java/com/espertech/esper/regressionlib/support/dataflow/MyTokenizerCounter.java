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
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.context.aifactory.core.SAIFFInitializeSymbol;
import com.espertech.esper.common.internal.epl.dataflow.interfaces.*;
import com.espertech.esper.common.internal.epl.expression.core.ExprValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.StringTokenizer;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.newInstance;

@OutputTypes({
    @OutputType(name = "line", type = int.class),
    @OutputType(name = "wordCount", type = int.class),
    @OutputType(name = "charCount", type = int.class)
})
public class MyTokenizerCounter implements DataFlowOperatorForge, DataFlowOperatorFactory, DataFlowOperator {
    private static final Logger log = LoggerFactory.getLogger(MyTokenizerCounter.class);

    @DataFlowContext
    private EPDataFlowEmitter graphContext;

    public DataFlowOpForgeInitializeResult initializeForge(DataFlowOpForgeInitializeContext context) throws ExprValidationException {
        return null;
    }

    public CodegenExpression make(CodegenMethodScope parent, SAIFFInitializeSymbol symbols, CodegenClassScope classScope) {
        return newInstance(MyTokenizerCounter.class);
    }

    public void initializeFactory(DataFlowOpFactoryInitializeContext context) {
    }

    public DataFlowOperator operator(DataFlowOpInitializeContext context) {
        return new MyTokenizerCounter();
    }

    public void onInput(String line) {
        StringTokenizer tokenizer = new StringTokenizer(line, " \t");
        int wordCount = tokenizer.countTokens();
        int charCount = 0;
        while (tokenizer.hasMoreTokens()) {
            String token = tokenizer.nextToken();
            charCount += token.length();
        }
        log.debug("Submitting stat words[" + wordCount + "] chars[" + charCount + "] for line '" + line + "'");
        graphContext.submit(new Object[]{1, wordCount, charCount});
    }
}


