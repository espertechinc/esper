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
import com.espertech.esper.client.EventPropertyGetter;
import com.espertech.esper.codegen.base.CodegenBlock;
import com.espertech.esper.codegen.base.CodegenClassScope;
import com.espertech.esper.codegen.base.CodegenMethodScope;
import com.espertech.esper.epl.expression.codegen.CodegenLegoCast;
import com.espertech.esper.codegen.model.expression.CodegenExpression;
import com.espertech.esper.codegen.model.expression.CodegenExpressionRef;
import com.espertech.esper.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.codegen.base.CodegenMethodNode;
import com.espertech.esper.event.EventPropertyGetterSPI;
import com.espertech.esper.event.EventTypeSPI;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.util.JavaClassHelper;

import java.io.StringWriter;
import java.util.Arrays;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.*;

/**
 * Represents an stream property identifier in a filter expressiun tree.
 */
public class ExprContextPropertyNodeImpl extends ExprNodeBase implements ExprContextPropertyNode, ExprEvaluator, ExprForge {
    private static final long serialVersionUID = 2816977190089087618L;
    private final String propertyName;
    private Class returnType;
    private transient EventPropertyGetterSPI getter;

    public ExprContextPropertyNodeImpl(String propertyName) {
        this.propertyName = propertyName;
    }

    public ExprEvaluator getExprEvaluator() {
        return this;
    }

    public String getPropertyName() {
        return propertyName;
    }

    public Class getEvaluationType() {
        return returnType;
    }

    public ExprForge getForge() {
        return this;
    }

    public ExprNodeRenderable getForgeRenderable() {
        return this;
    }

    public ExprNode validate(ExprValidationContext validationContext) throws ExprValidationException {
        if (validationContext.getContextDescriptor() == null) {
            throw new ExprValidationException("Context property '" + propertyName + "' cannot be used in the expression as provided");
        }
        EventTypeSPI eventType = (EventTypeSPI) validationContext.getContextDescriptor().getContextPropertyRegistry().getContextEventType();
        if (eventType == null) {
            throw new ExprValidationException("Context property '" + propertyName + "' cannot be used in the expression as provided");
        }
        getter = eventType.getGetterSPI(propertyName);
        if (getter == null) {
            throw new ExprValidationException("Context property '" + propertyName + "' is not a known property, known properties are " + Arrays.toString(eventType.getPropertyNames()));
        }
        returnType = JavaClassHelper.getBoxedType(eventType.getPropertyType(propertyName));
        return null;
    }

    public boolean isConstantResult() {
        return false;
    }

    public Object evaluate(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().qExprContextProp(this);
        }
        EventBean props = context.getContextProperties();
        Object result = props != null ? getter.get(props) : null;
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().aExprContextProp(result);
        }
        return result;
    }

    public CodegenExpression evaluateCodegen(Class requiredType, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        CodegenMethodNode methodNode = codegenMethodScope.makeChild(getEvaluationType(), ExprContextPropertyNodeImpl.class, codegenClassScope);
        CodegenExpressionRef refExprEvalCtx = exprSymbol.getAddExprEvalCtx(methodNode);
        CodegenBlock block = methodNode.getBlock()
                .declareVar(EventBean.class, "props", exprDotMethod(refExprEvalCtx, "getContextProperties"))
                .ifRefNullReturnNull("props");
        block.methodReturn(CodegenLegoCast.castSafeFromObjectType(returnType, getter.eventBeanGetCodegen(ref("props"), methodNode, codegenClassScope)));
        return localMethod(methodNode);
    }

    public ExprForgeComplexityEnum getComplexity() {
        return ExprForgeComplexityEnum.SINGLE;
    }

    public Class getType() {
        return returnType;
    }

    public void toPrecedenceFreeEPL(StringWriter writer) {
        writer.append(propertyName);
    }

    public ExprPrecedenceEnum getPrecedence() {
        return ExprPrecedenceEnum.UNARY;
    }

    public EventPropertyGetter getGetter() {
        return getter;
    }

    public boolean equalsNode(ExprNode node, boolean ignoreStreamPrefix) {
        if (this == node) return true;
        if (node == null || getClass() != node.getClass()) return false;

        ExprContextPropertyNodeImpl that = (ExprContextPropertyNodeImpl) node;
        return propertyName.equals(that.propertyName);
    }
}
