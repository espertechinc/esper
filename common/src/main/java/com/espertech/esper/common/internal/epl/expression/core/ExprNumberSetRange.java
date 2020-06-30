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
import com.espertech.esper.common.client.type.EPType;
import com.espertech.esper.common.client.type.EPTypeClass;
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

    public void toPrecedenceFreeEPL(StringWriter writer, ExprNodeRenderableFlags flags) {
        this.getChildNodes()[0].toEPL(writer, ExprPrecedenceEnum.MINIMUM, flags);
        writer.append(":");
        this.getChildNodes()[1].toEPL(writer, ExprPrecedenceEnum.MINIMUM, flags);
    }

    public ExprPrecedenceEnum getPrecedence() {
        return ExprPrecedenceEnum.UNARY;
    }

    @Override
    public ExprEvaluator getExprEvaluator() {
        return this;
    }

    public EPTypeClass getEvaluationType() {
        return RangeParameter.EPTYPE;
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
        EPType typeOne = this.getChildNodes()[0].getForge().getEvaluationType();
        EPType typeTwo = this.getChildNodes()[1].getForge().getEvaluationType();
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

    public CodegenExpression evaluateCodegen(EPTypeClass requiredType, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        ExprForge lowerValue = this.getChildNodes()[0].getForge();
        ExprForge upperValue = this.getChildNodes()[1].getForge();
        EPTypeClass lowerType = (EPTypeClass) lowerValue.getEvaluationType();
        EPTypeClass upperType = (EPTypeClass) upperValue.getEvaluationType();
        CodegenMethod methodNode = codegenMethodScope.makeChild(RangeParameter.EPTYPE, ExprNumberSetRange.class, codegenClassScope);
        CodegenBlock block = methodNode.getBlock()
                .declareVar(lowerType, "valueLower", lowerValue.evaluateCodegen(requiredType, methodNode, exprSymbol, codegenClassScope))
                .declareVar(upperType, "valueUpper", upperValue.evaluateCodegen(requiredType, methodNode, exprSymbol, codegenClassScope));
        if (!lowerType.getType().isPrimitive()) {
            block.ifRefNull("valueLower")
                    .staticMethod(ExprNumberSetRange.class, METHOD_HANDLENUMBERSETRANGELOWERNULL)
                    .assignRef("valueLower", constant(0))
                    .blockEnd();

        }
        if (!upperType.getType().isPrimitive()) {
            block.ifRefNull("valueUpper")
                    .staticMethod(ExprNumberSetRange.class, METHOD_HANDLENUMBERSETRANGEUPPERNULL)
                    .assignRef("valueUpper", enumValue(Integer.class, "MAX_VALUE"))
                    .blockEnd();
        }
        block.methodReturn(newInstance(RangeParameter.EPTYPE,
                SimpleNumberCoercerFactory.SimpleNumberCoercerInt.codegenInt(ref("valueLower"), lowerType),
                SimpleNumberCoercerFactory.SimpleNumberCoercerInt.codegenInt(ref("valueUpper"), upperType)
        ));
        return localMethod(methodNode);
    }
}
