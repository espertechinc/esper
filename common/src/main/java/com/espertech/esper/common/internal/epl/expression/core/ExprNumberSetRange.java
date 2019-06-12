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
import com.espertech.esper.common.internal.type.RangeParameter;
import com.espertech.esper.common.internal.util.JavaClassHelper;
import com.espertech.esper.common.internal.util.SimpleNumberCoercerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.StringWriter;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

/**
 * Expression for use within crontab to specify a range.
 * <p>
 * Differs from the between-expression since the value returned by evaluating is a cron-value object.
 */
public class ExprNumberSetRange extends ExprNodeBase implements ExprForge, ExprEvaluator {
    private static final Logger log = LoggerFactory.getLogger(ExprNumberSetRange.class);
    public final static String METHOD_HANDLENUMBERSETRANGELOWERNULL = "handleNumberSetRangeLowerNull";
    public final static String METHOD_HANDLENUMBERSETRANGEUPPERNULL = "handleNumberSetRangeUpperNull";

    private transient ExprEvaluator[] evaluators;

    public void toPrecedenceFreeEPL(StringWriter writer) {
        this.getChildNodes()[0].toEPL(writer, ExprPrecedenceEnum.MINIMUM);
        writer.append(":");
        this.getChildNodes()[1].toEPL(writer, ExprPrecedenceEnum.MINIMUM);
    }

    public ExprPrecedenceEnum getPrecedence() {
        return ExprPrecedenceEnum.UNARY;
    }

    @Override
    public ExprEvaluator getExprEvaluator() {
        return this;
    }

    public Class getEvaluationType() {
        return RangeParameter.class;
    }

    public ExprForge getForge() {
        return this;
    }

    public ExprNodeRenderable getForgeRenderable() {
        return this;
    }

    public ExprForgeConstantType getForgeConstantType() {
        return ExprForgeConstantType.values()[Math.max(this.getChildNodes()[0].getForge().getForgeConstantType().ordinal(), this.getChildNodes()[1].getForge().getForgeConstantType().ordinal())];
    }

    public boolean equalsNode(ExprNode node, boolean ignoreStreamPrefix) {
        return node instanceof ExprNumberSetRange;
    }

    public ExprNode validate(ExprValidationContext validationContext) throws ExprValidationException {
        evaluators = ExprNodeUtilityQuery.getEvaluatorsNoCompile(this.getChildNodes());
        Class typeOne = this.getChildNodes()[0].getForge().getEvaluationType();
        Class typeTwo = this.getChildNodes()[1].getForge().getEvaluationType();
        if ((!(JavaClassHelper.isNumericNonFP(typeOne))) || (!(JavaClassHelper.isNumericNonFP(typeTwo)))) {
            throw new ExprValidationException("Range operator requires integer-type parameters");
        }
        return null;
    }

    public Object evaluate(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
        Object valueLower = evaluators[0].evaluate(eventsPerStream, isNewData, exprEvaluatorContext);
        Object valueUpper = evaluators[1].evaluate(eventsPerStream, isNewData, exprEvaluatorContext);
        if (valueLower == null) {
            handleNumberSetRangeLowerNull();
            valueLower = 0;
        }
        if (valueUpper == null) {
            handleNumberSetRangeUpperNull();
            valueUpper = Integer.MAX_VALUE;
        }
        int intValueLower = ((Number) valueLower).intValue();
        int intValueUpper = ((Number) valueUpper).intValue();
        return new RangeParameter(intValueLower, intValueUpper);
    }

    /**
     * NOTE: Code-generation-invoked method, method name and parameter order matters
     */
    public static void handleNumberSetRangeLowerNull() {
        log.warn("Null value returned for lower bounds value in range parameter, using zero as lower bounds");
    }

    /**
     * NOTE: Code-generation-invoked method, method name and parameter order matters
     */
    public static void handleNumberSetRangeUpperNull() {
        log.warn("Null value returned for upper bounds value in range parameter, using max as upper bounds");
    }

    public CodegenExpression evaluateCodegen(Class requiredType, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        ExprForge valueLower = this.getChildNodes()[0].getForge();
        ExprForge valueUpper = this.getChildNodes()[1].getForge();
        CodegenMethod methodNode = codegenMethodScope.makeChild(RangeParameter.class, ExprNumberSetRange.class, codegenClassScope);
        CodegenBlock block = methodNode.getBlock()
                .declareVar(valueLower.getEvaluationType(), "valueLower", valueLower.evaluateCodegen(requiredType, methodNode, exprSymbol, codegenClassScope))
                .declareVar(valueUpper.getEvaluationType(), "valueUpper", valueUpper.evaluateCodegen(requiredType, methodNode, exprSymbol, codegenClassScope));
        if (!valueLower.getEvaluationType().isPrimitive()) {
            block.ifRefNull("valueLower")
                    .staticMethod(ExprNumberSetRange.class, METHOD_HANDLENUMBERSETRANGELOWERNULL)
                    .assignRef("valueLower", constant(0))
                    .blockEnd();

        }
        if (!valueUpper.getEvaluationType().isPrimitive()) {
            block.ifRefNull("valueUpper")
                    .staticMethod(ExprNumberSetRange.class, METHOD_HANDLENUMBERSETRANGEUPPERNULL)
                    .assignRef("valueUpper", enumValue(Integer.class, "MAX_VALUE"))
                    .blockEnd();
        }
        block.methodReturn(newInstance(RangeParameter.class,
                SimpleNumberCoercerFactory.SimpleNumberCoercerInt.codegenInt(ref("valueLower"), valueLower.getEvaluationType()),
                SimpleNumberCoercerFactory.SimpleNumberCoercerInt.codegenInt(ref("valueUpper"), valueUpper.getEvaluationType())
        ));
        return localMethod(methodNode);
    }
}
