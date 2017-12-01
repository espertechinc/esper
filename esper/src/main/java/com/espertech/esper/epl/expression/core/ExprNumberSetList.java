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
import com.espertech.esper.codegen.base.CodegenBlock;
import com.espertech.esper.codegen.base.CodegenClassScope;
import com.espertech.esper.codegen.base.CodegenMethodScope;
import com.espertech.esper.codegen.model.expression.CodegenExpression;
import com.espertech.esper.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.codegen.base.CodegenMethodNode;
import com.espertech.esper.type.*;
import com.espertech.esper.util.JavaClassHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.*;

/**
 * Expression for use within crontab to specify a list of values.
 */
public class ExprNumberSetList extends ExprNodeBase implements ExprForge, ExprEvaluator {
    private static final Logger log = LoggerFactory.getLogger(ExprNumberSetList.class);
    private final static String METHOD_HANDLEEXPRNUMBERSETLISTADD = "handleExprNumberSetListAdd";
    private final static String METHOD_HANDLEEXPRNUMBERSETLISTEMPTY = "handleExprNumberSetListEmpty";

    private transient ExprEvaluator[] evaluators;
    private static final long serialVersionUID = 4941618470342360450L;

    public ExprEvaluator getExprEvaluator() {
        return this;
    }

    public Class getEvaluationType() {
        return ListParameter.class;
    }

    public ExprForge getForge() {
        return this;
    }

    public void toPrecedenceFreeEPL(StringWriter writer) {
        String delimiter = "";

        writer.append('[');
        Iterator<ExprNode> it = Arrays.asList(this.getChildNodes()).iterator();
        do {
            ExprNode expr = it.next();
            writer.append(delimiter);
            expr.toEPL(writer, ExprPrecedenceEnum.MINIMUM);
            delimiter = ",";
        }
        while (it.hasNext());
        writer.append(']');
    }

    public ExprPrecedenceEnum getPrecedence() {
        return ExprPrecedenceEnum.UNARY;
    }

    public boolean isConstantResult() {
        for (ExprNode child : this.getChildNodes()) {
            if (!child.isConstantResult()) {
                return false;
            }
        }
        return true;
    }

    public boolean equalsNode(ExprNode node, boolean ignoreStreamPrefix) {
        return node instanceof ExprNumberSetList;
    }

    public ExprNode validate(ExprValidationContext validationContext) throws ExprValidationException {
        // all nodes must either be int, frequency or range
        evaluators = ExprNodeUtilityCore.getEvaluatorsNoCompile(this.getChildNodes());
        for (int i = 0; i < this.getChildNodes().length; i++) {
            Class type = this.getChildNodes()[i].getForge().getEvaluationType();
            if ((type == FrequencyParameter.class) || (type == RangeParameter.class)) {
                continue;
            }
            if (!(JavaClassHelper.isNumericNonFP(type))) {
                throw new ExprValidationException("Frequency operator requires an integer-type parameter");
            }
        }
        return null;
    }

    public Object evaluate(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
        List<NumberSetParameter> parameters = new ArrayList<NumberSetParameter>();
        for (ExprEvaluator child : evaluators) {
            Object value = child.evaluate(eventsPerStream, isNewData, exprEvaluatorContext);
            handleExprNumberSetListAdd(value, parameters);
        }
        handleExprNumberSetListEmpty(parameters);
        return new ListParameter(parameters);
    }

    public CodegenExpression evaluateCodegen(Class requiredType, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        CodegenMethodNode methodNode = codegenMethodScope.makeChild(ListParameter.class, ExprNumberSetList.class, codegenClassScope);
        CodegenBlock block = methodNode.getBlock()
                .declareVar(List.class, "parameters", newInstance(ArrayList.class));
        int count = -1;
        for (ExprNode node : getChildNodes()) {
            count++;
            ExprForge forge = node.getForge();
            Class evaluationType = forge.getEvaluationType();
            String refname = "value" + count;
            block.declareVar(evaluationType, refname, forge.evaluateCodegen(requiredType, methodNode, exprSymbol, codegenClassScope))
                    .staticMethod(ExprNumberSetList.class, METHOD_HANDLEEXPRNUMBERSETLISTADD, ref(refname), ref("parameters"));
        }
        block.staticMethod(ExprNumberSetList.class, METHOD_HANDLEEXPRNUMBERSETLISTEMPTY, ref("parameters"))
                .methodReturn(newInstance(ListParameter.class, ref("parameters")));
        return localMethod(methodNode);
    }

    public ExprForgeComplexityEnum getComplexity() {
        return isConstantResult() ? ExprForgeComplexityEnum.NONE : ExprForgeComplexityEnum.INTER;
    }

    public ExprNodeRenderable getForgeRenderable() {
        return this;
    }

    /**
     * NOTE: Code-generation-invoked method, method name and parameter order matters
     *
     * @param parameters params
     */
    public static void handleExprNumberSetListEmpty(List<NumberSetParameter> parameters) {
        if (parameters.isEmpty()) {
            log.warn("Empty list of values in list parameter, using upper bounds");
            parameters.add(new IntParameter(Integer.MAX_VALUE));
        }
    }

    /**
     * NOTE: Code-generation-invoked method, method name and parameter order matters
     *
     * @param value      value
     * @param parameters params
     */
    public static void handleExprNumberSetListAdd(Object value, List<NumberSetParameter> parameters) {
        if (value == null) {
            log.info("Null value returned for lower bounds value in list parameter, skipping parameter");
            return;
        }
        if ((value instanceof FrequencyParameter) || (value instanceof RangeParameter)) {
            parameters.add((NumberSetParameter) value);
            return;
        }

        int intValue = ((Number) value).intValue();
        parameters.add(new IntParameter(intValue));
    }
}
