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
import com.espertech.esper.common.client.FragmentEventType;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenBlock;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionRef;
import com.espertech.esper.common.internal.context.compile.ContextCompileTimeDescriptor;
import com.espertech.esper.common.internal.epl.expression.codegen.CodegenLegoCast;
import com.espertech.esper.common.internal.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.common.internal.epl.streamtype.StreamTypeService;
import com.espertech.esper.common.internal.event.core.EventPropertyGetterSPI;
import com.espertech.esper.common.internal.event.core.EventTypeSPI;
import com.espertech.esper.common.internal.metrics.instrumentation.InstrumentationBuilderExpr;
import com.espertech.esper.common.internal.util.JavaClassHelper;

import java.io.StringWriter;
import java.util.Arrays;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

/**
 * Represents an stream property identifier in a filter expressiun tree.
 */
public class ExprContextPropertyNodeImpl extends ExprNodeBase implements ExprContextPropertyNode, ExprEvaluator, ExprForgeInstrumentable {
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
        EventBean props = context.getContextProperties();
        Object result = props != null ? getter.get(props) : null;
        return result;
    }

    public CodegenExpression evaluateCodegenUninstrumented(Class requiredType, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        CodegenMethod methodNode = codegenMethodScope.makeChild(getEvaluationType(), ExprContextPropertyNodeImpl.class, codegenClassScope);
        CodegenExpressionRef refExprEvalCtx = exprSymbol.getAddExprEvalCtx(methodNode);
        CodegenBlock block = methodNode.getBlock()
                .declareVar(EventBean.class, "props", exprDotMethod(refExprEvalCtx, "getContextProperties"))
                .ifRefNullReturnNull("props");
        block.methodReturn(CodegenLegoCast.castSafeFromObjectType(returnType, getter.eventBeanGetCodegen(ref("props"), methodNode, codegenClassScope)));
        return localMethod(methodNode);
    }

    public CodegenExpression evaluateCodegen(Class requiredType, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        return new InstrumentationBuilderExpr(this.getClass(), this, "ExprContextProp", requiredType, codegenMethodScope, exprSymbol, codegenClassScope).build();
    }

    public ExprForgeConstantType getForgeConstantType() {
        return ExprForgeConstantType.NONCONST;
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

    public EventPropertyGetterSPI getGetter() {
        return getter;
    }

    public boolean equalsNode(ExprNode node, boolean ignoreStreamPrefix) {
        if (this == node) return true;
        if (node == null || getClass() != node.getClass()) return false;

        ExprContextPropertyNodeImpl that = (ExprContextPropertyNodeImpl) node;
        return propertyName.equals(that.propertyName);
    }

    public ExprEnumerationForgeDesc getEnumerationForge(StreamTypeService streamTypeService, ContextCompileTimeDescriptor contextDescriptor) {
        EventTypeSPI eventType = (EventTypeSPI) contextDescriptor.getContextPropertyRegistry().getContextEventType();
        if (eventType == null) {
            return null;
        }
        FragmentEventType fragmentEventType = eventType.getFragmentType(propertyName);
        if (fragmentEventType == null || fragmentEventType.isIndexed()) {
            return null;
        }
        ExprContextPropertyNodeFragmentEnumerationForge forge = new ExprContextPropertyNodeFragmentEnumerationForge(propertyName, fragmentEventType.getFragmentType(), getter);
        return new ExprEnumerationForgeDesc(forge, true, -1);

    }
}
