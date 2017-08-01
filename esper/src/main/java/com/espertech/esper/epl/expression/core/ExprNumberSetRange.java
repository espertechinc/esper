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
package com.espertech.esper.epl.expression.core;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.codegen.core.CodegenBlock;
import com.espertech.esper.codegen.core.CodegenContext;
import com.espertech.esper.codegen.model.expression.CodegenExpression;
import com.espertech.esper.codegen.model.method.CodegenParamSetExprPremade;
import com.espertech.esper.type.RangeParameter;
import com.espertech.esper.util.JavaClassHelper;
import com.espertech.esper.util.SimpleNumberCoercerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.StringWriter;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.*;

/**
 * Expression for use within crontab to specify a range.
 * <p>
 * Differs from the between-expression since the value returned by evaluating is a cron-value object.
 */
public class ExprNumberSetRange extends ExprNodeBase implements ExprForge, ExprEvaluator {
    private static final Logger log = LoggerFactory.getLogger(ExprNumberSetRange.class);
    private transient ExprEvaluator[] evaluators;
    private static final long serialVersionUID = -3777415170380735662L;

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

    public boolean isConstantResult() {
        return this.getChildNodes()[0].isConstantResult() && this.getChildNodes()[1].isConstantResult();
    }

    public boolean equalsNode(ExprNode node, boolean ignoreStreamPrefix) {
        return node instanceof ExprNumberSetRange;
    }

    public ExprNode validate(ExprValidationContext validationContext) throws ExprValidationException {
        evaluators = ExprNodeUtility.getEvaluatorsNoCompile(this.getChildNodes());
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

    public CodegenExpression evaluateCodegen(CodegenParamSetExprPremade params, CodegenContext context) {
        ExprForge valueLower = this.getChildNodes()[0].getForge();
        ExprForge valueUpper = this.getChildNodes()[1].getForge();
        CodegenBlock block = context.addMethod(RangeParameter.class, ExprNumberSetRange.class).add(params).begin()
                .declareVar(valueLower.getEvaluationType(), "valueLower", valueLower.evaluateCodegen(params, context))
                .declareVar(valueUpper.getEvaluationType(), "valueUpper", valueUpper.evaluateCodegen(params, context));
        if (!valueLower.getEvaluationType().isPrimitive()) {
            block.ifRefNull("valueLower")
                    .expression(staticMethod(ExprNumberSetRange.class, "handleNumberSetRangeLowerNull"))
                    .assignRef("valueLower", constant(0))
                    .blockEnd();

        }
        if (!valueUpper.getEvaluationType().isPrimitive()) {
            block.ifRefNull("valueUpper")
                    .expression(staticMethod(ExprNumberSetRange.class, "handleNumberSetRangeUpperNull"))
                    .assignRef("valueUpper", enumValue(Integer.class, "MAX_VALUE"))
                    .blockEnd();
        }
        String method = block.methodReturn(newInstance(RangeParameter.class,
                SimpleNumberCoercerFactory.SimpleNumberCoercerInt.codegenInt(ref("valueLower"), valueLower.getEvaluationType()),
                SimpleNumberCoercerFactory.SimpleNumberCoercerInt.codegenInt(ref("valueUpper"), valueUpper.getEvaluationType())
        ));
        return localMethodBuild(method).passAll(params).call();
    }

    public ExprForgeComplexityEnum getComplexity() {
        return isConstantResult() ? ExprForgeComplexityEnum.NONE : ExprForgeComplexityEnum.INTER;
    }
}
