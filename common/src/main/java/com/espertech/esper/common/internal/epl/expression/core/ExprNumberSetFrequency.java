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
package com.espertech.esper.common.internal.epl.expression.core;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenBlock;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.common.internal.type.FrequencyParameter;
import com.espertech.esper.common.internal.util.JavaClassHelper;
import com.espertech.esper.common.internal.util.SimpleNumberCoercerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.StringWriter;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

/**
 * Expression for use within crontab to specify a frequency.
 */
public class ExprNumberSetFrequency extends ExprNodeBase implements ExprForge, ExprEvaluator {
    private static final Logger log = LoggerFactory.getLogger(ExprNumberSetFrequency.class);
    private transient ExprEvaluator evaluator;

    @Override
    public ExprEvaluator getExprEvaluator() {
        return this;
    }

    public Class getEvaluationType() {
        return FrequencyParameter.class;
    }

    public ExprForge getForge() {
        return this;
    }

    public ExprNodeRenderable getForgeRenderable() {
        return this;
    }

    public void toPrecedenceFreeEPL(StringWriter writer) {
        writer.append("*/");
        this.getChildNodes()[0].toEPL(writer, ExprPrecedenceEnum.MINIMUM);
    }

    public ExprPrecedenceEnum getPrecedence() {
        return ExprPrecedenceEnum.MINIMUM;
    }

    public ExprForgeConstantType getForgeConstantType() {
        return this.getChildNodes()[0].getForge().getForgeConstantType();
    }

    public boolean equalsNode(ExprNode node, boolean ignoreStreamPrefix) {
        if (!(node instanceof ExprNumberSetFrequency)) {
            return false;
        }
        return true;
    }

    public ExprNode validate(ExprValidationContext validationContext) throws ExprValidationException {
        ExprForge forge = this.getChildNodes()[0].getForge();
        if (!(JavaClassHelper.isNumericNonFP(forge.getEvaluationType()))) {
            throw new ExprValidationException("Frequency operator requires an integer-type parameter");
        }
        evaluator = forge.getExprEvaluator();
        return null;
    }

    public Object evaluate(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
        Object value = evaluator.evaluate(eventsPerStream, isNewData, exprEvaluatorContext);
        if (value == null) {
            return handleNumberSetFreqNullValue();
        } else {
            int intValue = ((Number) value).intValue();
            return new FrequencyParameter(intValue);
        }
    }

    public CodegenExpression evaluateCodegen(Class requiredType, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        ExprForge forge = this.getChildNodes()[0].getForge();
        Class evaluationType = forge.getEvaluationType();
        CodegenMethod methodNode = codegenMethodScope.makeChild(FrequencyParameter.class, ExprNumberSetFrequency.class, codegenClassScope);
        CodegenBlock block = methodNode.getBlock()
                .declareVar(evaluationType, "value", forge.evaluateCodegen(requiredType, methodNode, exprSymbol, codegenClassScope));
        if (!evaluationType.isPrimitive()) {
            block.ifRefNull("value")
                    .blockReturn(staticMethod(ExprNumberSetFrequency.class, "handleNumberSetFreqNullValue"));
        }
        block.methodReturn(newInstance(FrequencyParameter.class, SimpleNumberCoercerFactory.SimpleNumberCoercerInt.codegenInt(ref("value"), evaluationType)));
        return localMethod(methodNode);
    }

    /**
     * NOTE: Code-generation-invoked method, method name and parameter order matters
     *
     * @return frequence params
     */
    public static FrequencyParameter handleNumberSetFreqNullValue() {
        log.warn("Null value returned for frequency parameter");
        return new FrequencyParameter(Integer.MAX_VALUE);
    }
}
