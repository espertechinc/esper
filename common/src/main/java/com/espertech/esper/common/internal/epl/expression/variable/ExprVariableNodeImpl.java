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
import com.espertech.esper.common.client.type.EPType;
import com.espertech.esper.common.client.type.EPTypeClass;
import com.espertech.esper.common.client.type.EPTypeNull;
import com.espertech.esper.common.client.type.EPTypePremade;
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

    private EPType returnType;

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

    public EPType getEvaluationType() {
        return returnType;
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
            returnType = variableMeta.getEventType().getPropertyEPType(optSubPropName);
        } else {
            returnType = variableMeta.getType();
        }

        returnType = JavaClassHelper.getBoxedType(returnType);
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

    public CodegenExpression evaluateCodegenUninstrumented(EPTypeClass requiredType, CodegenMethodScope parent, ExprForgeCodegenSymbol symbols, CodegenClassScope classScope) {
        if (returnType == EPTypeNull.INSTANCE) {
            return constantNull();
        }
        EPTypeClass returnClass = (EPTypeClass) returnType;
        CodegenMethod methodNode = parent.makeChild(returnClass, ExprVariableNodeImpl.class, classScope);
        CodegenExpression readerExpression = getReaderExpression(variableMeta, methodNode, symbols, classScope);
        CodegenBlock block = methodNode.getBlock()
                .declareVar(VariableReader.EPTYPE, "reader", readerExpression);
        if (variableMeta.getEventType() == null) {
            block.declareVar(returnClass, "value", cast(returnClass, exprDotMethod(ref("reader"), "getValue")))
                    .methodReturn(ref("value"));
        } else {
            block.declareVar(EPTypePremade.OBJECT.getEPType(), "value", exprDotMethod(ref("reader"), "getValue"))
                    .ifRefNullReturnNull("value")
                    .declareVar(EventBean.EPTYPE, "theEvent", cast(EventBean.EPTYPE, ref("value")));
            if (optSubPropName == null) {
                block.methodReturn(cast(returnClass, exprDotUnderlying(ref("theEvent"))));
            } else {
                block.methodReturn(CodegenLegoCast.castSafeFromObjectType(returnType, optSubPropGetter.eventBeanGetCodegen(ref("theEvent"), methodNode, classScope)));
            }
        }
        return localMethod(methodNode);
    }

    public static CodegenExpression getReaderExpression(VariableMetaData variableMeta, CodegenMethod methodNode, ExprForgeCodegenSymbol symbols, CodegenClassScope classScope) {
        CodegenExpression readerExpression;
        if (variableMeta.getOptionalContextName() == null) {
            readerExpression = classScope.addOrGetFieldSharable(new VariableReaderCodegenFieldSharable(variableMeta));
        } else {
            CodegenExpressionField field = classScope.addOrGetFieldSharable(new VariableReaderPerCPCodegenFieldSharable(variableMeta));
            CodegenExpression cpid = exprDotMethod(symbols.getAddExprEvalCtx(methodNode), "getAgentInstanceId");
            readerExpression = cast(VariableReader.EPTYPE, exprDotMethod(field, "get", cpid));
        }
        return readerExpression;
    }

    public CodegenExpression codegenGetDeployTimeConstValue(CodegenClassScope classScope) {
        if (returnType == null) {
            return constantNull();
        }
        EPTypeClass returnClass = (EPTypeClass) returnType;
        CodegenExpression readerExpression = classScope.addOrGetFieldSharable(new VariableReaderCodegenFieldSharable(variableMeta));
        if (variableMeta.getEventType() == null) {
            return cast(returnClass, exprDotMethod(readerExpression, "getValue"));
        }
        CodegenExpression unpack = exprDotUnderlying(cast(EventBean.EPTYPE, exprDotMethod(readerExpression, "getValue")));
        return cast(returnClass, unpack);
    }

    public CodegenExpression evaluateCodegen(EPTypeClass requiredType, CodegenMethodScope parent, ExprForgeCodegenSymbol symbols, CodegenClassScope classScope) {
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

    public void toPrecedenceFreeEPL(StringWriter writer, ExprNodeRenderableFlags flags) {
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

    public void renderForFilterPlan(StringBuilder out) {
        out.append("variable '").append(getVariableNameWithSubProp()).append("'");
    }
}
