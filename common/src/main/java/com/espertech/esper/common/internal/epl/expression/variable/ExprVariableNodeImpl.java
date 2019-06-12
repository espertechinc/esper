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
package com.espertech.esper.common.internal.epl.expression.variable;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.client.util.NameAccessModifier;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenBlock;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionField;
import com.espertech.esper.common.internal.epl.expression.codegen.CodegenLegoCast;
import com.espertech.esper.common.internal.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.common.internal.epl.expression.core.*;
import com.espertech.esper.common.internal.epl.streamtype.DuplicatePropertyException;
import com.espertech.esper.common.internal.epl.streamtype.PropertyNotFoundException;
import com.espertech.esper.common.internal.epl.variable.compiletime.VariableMetaData;
import com.espertech.esper.common.internal.epl.variable.compiletime.VariableReaderCodegenFieldSharable;
import com.espertech.esper.common.internal.epl.variable.compiletime.VariableReaderPerCPCodegenFieldSharable;
import com.espertech.esper.common.internal.epl.variable.core.VariableReader;
import com.espertech.esper.common.internal.event.core.EventPropertyGetterSPI;
import com.espertech.esper.common.internal.event.core.EventTypeSPI;
import com.espertech.esper.common.internal.metrics.instrumentation.InstrumentationBuilderExpr;
import com.espertech.esper.common.internal.util.JavaClassHelper;

import java.io.StringWriter;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

/**
 * Represents a variable in an expression tree.
 */
public class ExprVariableNodeImpl extends ExprNodeBase implements ExprForgeInstrumentable, ExprEvaluator, ExprVariableNode {
    private final VariableMetaData variableMeta;
    private final String optSubPropName;
    private EventPropertyGetterSPI optSubPropGetter;

    private Class variableType;

    public ExprVariableNodeImpl(VariableMetaData variableMeta, String optSubPropName) {
        this.variableMeta = variableMeta;
        this.optSubPropName = optSubPropName;
    }

    public ExprEvaluator getExprEvaluator() {
        return this;
    }

    public VariableMetaData getVariableMetadata() {
        return variableMeta;
    }

    public Class getEvaluationType() {
        return variableType;
    }

    public ExprForge getForge() {
        return this;
    }

    public ExprNodeRenderable getForgeRenderable() {
        return this;
    }

    public ExprNode validate(ExprValidationContext validationContext) throws ExprValidationException {
        // determine if any types are property agnostic; If yes, resolve to variable
        boolean hasPropertyAgnosticType = false;
        EventType[] types = validationContext.getStreamTypeService().getEventTypes();
        for (int i = 0; i < validationContext.getStreamTypeService().getEventTypes().length; i++) {
            if (types[i] instanceof EventTypeSPI) {
                hasPropertyAgnosticType |= ((EventTypeSPI) types[i]).getMetadata().isPropertyAgnostic();
            }
        }

        if (!hasPropertyAgnosticType) {
            String variableName = variableMeta.getVariableName();
            // the variable name should not overlap with a property name
            try {
                validationContext.getStreamTypeService().resolveByPropertyName(variableName, false);
                throw new ExprValidationException("The variable by name '" + variableName + "' is ambiguous to a property of the same name");
            } catch (DuplicatePropertyException e) {
                throw new ExprValidationException("The variable by name '" + variableName + "' is ambiguous to a property of the same name");
            } catch (PropertyNotFoundException e) {
                // expected
            }
        }

        String variableName = variableMeta.getVariableName();
        if (optSubPropName != null) {
            if (variableMeta.getEventType() == null) {
                throw new ExprValidationException("Property '" + optSubPropName + "' is not valid for variable '" + variableName + "'");
            }
            optSubPropGetter = ((EventTypeSPI) variableMeta.getEventType()).getGetterSPI(optSubPropName);
            if (optSubPropGetter == null) {
                throw new ExprValidationException("Property '" + optSubPropName + "' is not valid for variable '" + variableName + "'");
            }
            variableType = variableMeta.getEventType().getPropertyType(optSubPropName);
        } else {
            variableType = variableMeta.getType();
        }

        variableType = JavaClassHelper.getBoxedType(variableType);
        return null;
    }

    public String toString() {
        return "variableName=" + variableMeta.getVariableName();
    }

    public Object evaluate(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
        if (variableMeta.isCompileTimeConstant()) {
            return variableMeta.getValueWhenAvailable();
        }
        throw new IllegalStateException("Cannot evaluate at compile time");
    }

    public CodegenExpression evaluateCodegenUninstrumented(Class requiredType, CodegenMethodScope parent, ExprForgeCodegenSymbol symbols, CodegenClassScope classScope) {
        CodegenMethod methodNode = parent.makeChild(variableType, ExprVariableNodeImpl.class, classScope);

        CodegenExpression readerExpression;
        if (variableMeta.getOptionalContextName() == null) {
            readerExpression = classScope.addOrGetFieldSharable(new VariableReaderCodegenFieldSharable(variableMeta));
        } else {
            CodegenExpressionField field = classScope.addOrGetFieldSharable(new VariableReaderPerCPCodegenFieldSharable(variableMeta));
            CodegenExpression cpid = exprDotMethod(symbols.getAddExprEvalCtx(methodNode), "getAgentInstanceId");
            readerExpression = cast(VariableReader.class, exprDotMethod(field, "get", cpid));
        }

        CodegenBlock block = methodNode.getBlock()
                .declareVar(VariableReader.class, "reader", readerExpression);
        if (variableMeta.getEventType() == null) {
            block.declareVar(variableType, "value", cast(variableType, exprDotMethod(ref("reader"), "getValue")))
                    .methodReturn(ref("value"));
        } else {
            block.declareVar(Object.class, "value", exprDotMethod(ref("reader"), "getValue"))
                    .ifRefNullReturnNull("value")
                    .declareVar(EventBean.class, "theEvent", cast(EventBean.class, ref("value")));
            if (optSubPropName == null) {
                block.methodReturn(cast(variableType, exprDotUnderlying(ref("theEvent"))));
            } else {
                block.methodReturn(CodegenLegoCast.castSafeFromObjectType(variableType, optSubPropGetter.eventBeanGetCodegen(ref("theEvent"), methodNode, classScope)));
            }
        }
        return localMethod(methodNode);
    }

    public CodegenExpression codegenGetDeployTimeConstValue(CodegenClassScope classScope) {
        CodegenExpression readerExpression = classScope.addOrGetFieldSharable(new VariableReaderCodegenFieldSharable(variableMeta));
        if (variableMeta.getEventType() == null) {
            return cast(variableType, exprDotMethod(readerExpression, "getValue"));
        }
        CodegenExpression unpack = exprDotUnderlying(cast(EventBean.class, exprDotMethod(readerExpression, "getValue")));
        return cast(variableType, unpack);
    }

    public CodegenExpression evaluateCodegen(Class requiredType, CodegenMethodScope parent, ExprForgeCodegenSymbol symbols, CodegenClassScope classScope) {
        return new InstrumentationBuilderExpr(this.getClass(), this, "ExprVariable", requiredType, parent, symbols, classScope).build();
    }

    public ExprForgeConstantType getForgeConstantType() {
        if (variableMeta.getOptionalContextName() != null) {
            return ExprForgeConstantType.NONCONST;
        }
        // for simple-value variables that are constant and created by the same module and not preconfigured we can use compile-time constant
        if (variableMeta.isConstant() && variableMeta.isCreatedByCurrentModule() && variableMeta.getVariableVisibility() != NameAccessModifier.PRECONFIGURED && variableMeta.getEventType() == null) {
            return ExprForgeConstantType.COMPILETIMECONST;
        }
        return variableMeta.isConstant() ? ExprForgeConstantType.DEPLOYCONST : ExprForgeConstantType.NONCONST;
    }

    public void toPrecedenceFreeEPL(StringWriter writer) {
        writer.append(variableMeta.getVariableName());
        if (optSubPropName != null) {
            writer.append(".");
            writer.append(optSubPropName);
        }
    }

    public ExprPrecedenceEnum getPrecedence() {
        return ExprPrecedenceEnum.UNARY;
    }

    public boolean equalsNode(ExprNode node, boolean ignoreStreamPrefix) {
        if (!(node instanceof ExprVariableNodeImpl)) {
            return false;
        }

        ExprVariableNodeImpl that = (ExprVariableNodeImpl) node;

        if (optSubPropName != null ? !optSubPropName.equals(that.optSubPropName) : that.optSubPropName != null) {
            return false;
        }
        return that.variableMeta.getVariableName().equals(this.variableMeta.getVariableName());
    }

    public String getVariableNameWithSubProp() {
        if (optSubPropName == null) {
            return variableMeta.getVariableName();
        }
        return variableMeta.getVariableName() + "." + optSubPropName;
    }
}
