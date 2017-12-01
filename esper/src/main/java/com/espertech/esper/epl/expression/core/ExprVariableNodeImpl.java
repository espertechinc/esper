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
import com.espertech.esper.client.EventType;
import com.espertech.esper.codegen.base.CodegenBlock;
import com.espertech.esper.codegen.base.CodegenClassScope;
import com.espertech.esper.codegen.base.CodegenMember;
import com.espertech.esper.codegen.base.CodegenMethodScope;
import com.espertech.esper.epl.expression.codegen.CodegenLegoCast;
import com.espertech.esper.codegen.model.expression.CodegenExpression;
import com.espertech.esper.codegen.model.expression.CodegenExpressionRef;
import com.espertech.esper.core.start.EPStatementStartMethod;
import com.espertech.esper.epl.core.streamtype.DuplicatePropertyException;
import com.espertech.esper.epl.core.streamtype.PropertyNotFoundException;
import com.espertech.esper.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.codegen.base.CodegenMethodNode;
import com.espertech.esper.epl.variable.VariableMetaData;
import com.espertech.esper.epl.variable.VariableReader;
import com.espertech.esper.event.EventPropertyGetterSPI;
import com.espertech.esper.event.EventTypeSPI;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.util.JavaClassHelper;

import java.io.StringWriter;
import java.util.Map;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.*;

/**
 * Represents a variable in an expression tree.
 */
public class ExprVariableNodeImpl extends ExprNodeBase implements ExprForge, ExprEvaluator, ExprVariableNode {
    private static final long serialVersionUID = 0L;

    private final String variableName;
    private final String optSubPropName;
    private final boolean isConstant;
    private final Object valueIfConstant;

    private Class variableType;
    private boolean isPrimitive;

    private transient EventPropertyGetterSPI eventTypeGetter;
    private transient Map<Integer, VariableReader> readersPerCp;
    private transient VariableReader readerNonCP;

    public ExprVariableNodeImpl(VariableMetaData variableMetaData, String optSubPropName) {
        if (variableMetaData == null) {
            throw new IllegalArgumentException("Variables metadata is null");
        }
        this.variableName = variableMetaData.getVariableName();
        this.optSubPropName = optSubPropName;
        this.isConstant = variableMetaData.isConstant();
        this.valueIfConstant = isConstant ? variableMetaData.getVariableStateFactory().getInitialState() : null;
    }

    public boolean isConstantValue() {
        return isConstant;
    }

    public ExprEvaluator getExprEvaluator() {
        return this;
    }

    public String getVariableName() {
        return variableName;
    }

    public Object getConstantValue(ExprEvaluatorContext context) {
        if (isConstant) {
            return valueIfConstant;
        }
        return null;
    }

    public boolean isConstantResult() {
        return isConstant;
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
            // the variable name should not overlap with a property name
            try {
                validationContext.getStreamTypeService().resolveByPropertyName(variableName, false);
                throw new ExprValidationException("The variable by name '" + variableName + "' is ambigous to a property of the same name");
            } catch (DuplicatePropertyException e) {
                throw new ExprValidationException("The variable by name '" + variableName + "' is ambigous to a property of the same name");
            } catch (PropertyNotFoundException e) {
                // expected
            }
        }

        VariableMetaData variableMetadata = validationContext.getVariableService().getVariableMetaData(variableName);
        if (variableMetadata == null) {
            throw new ExprValidationException("Failed to find variable by name '" + variableName + "'");
        }
        isPrimitive = variableMetadata.getEventType() == null;
        variableType = variableMetadata.getType();
        if (optSubPropName != null) {
            if (variableMetadata.getEventType() == null) {
                throw new ExprValidationException("Property '" + optSubPropName + "' is not valid for variable '" + variableName + "'");
            }
            eventTypeGetter = ((EventTypeSPI) variableMetadata.getEventType()).getGetterSPI(optSubPropName);
            if (eventTypeGetter == null) {
                throw new ExprValidationException("Property '" + optSubPropName + "' is not valid for variable '" + variableName + "'");
            }
            variableType = variableMetadata.getEventType().getPropertyType(optSubPropName);
        }

        readersPerCp = validationContext.getVariableService().getReadersPerCP(variableName);
        if (variableMetadata.getContextPartitionName() == null) {
            readerNonCP = readersPerCp.get(EPStatementStartMethod.DEFAULT_AGENT_INSTANCE_ID);
        }
        variableType = JavaClassHelper.getBoxedType(variableType);
        return null;
    }

    public Class getConstantType() {
        return variableType;
    }

    public String toString() {
        return "variableName=" + variableName;
    }

    public Object evaluate(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
        VariableReader reader;
        if (readerNonCP != null) {
            reader = readerNonCP;
        } else {
            reader = readersPerCp.get(exprEvaluatorContext.getAgentInstanceId());
        }

        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().qExprVariable(this);
        }
        Object value = reader.getValue();
        if (isPrimitive || value == null) {
            if (InstrumentationHelper.ENABLED) {
                InstrumentationHelper.get().aExprVariable(value);
            }
            return value;
        }

        EventBean theEvent = (EventBean) value;
        if (optSubPropName == null) {
            if (InstrumentationHelper.ENABLED) {
                InstrumentationHelper.get().aExprVariable(theEvent.getUnderlying());
            }
            return theEvent.getUnderlying();
        }
        Object result = eventTypeGetter.get(theEvent);
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().aExprVariable(result);
        }
        return result;
    }

    public CodegenExpression evaluateCodegen(Class requiredType, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        CodegenMethodNode methodNode = codegenMethodScope.makeChild(variableType, ExprVariableNodeImpl.class, codegenClassScope);
        CodegenExpressionRef refExprEvalCtx = exprSymbol.getAddExprEvalCtx(methodNode);

        CodegenExpression readerExpression;
        if (readerNonCP != null) {
            CodegenMember memberVariableReader = codegenClassScope.makeAddMember(VariableReader.class, readerNonCP);
            readerExpression = member(memberVariableReader.getMemberId());
        } else {
            CodegenMember memberReadersPerCp = codegenClassScope.makeAddMember(Map.class, readersPerCp);
            readerExpression = cast(VariableReader.class, exprDotMethod(member(memberReadersPerCp.getMemberId()), "get", exprDotMethod(refExprEvalCtx, "getAgentInstanceId")));
        }

        CodegenBlock block = methodNode.getBlock()
                .declareVar(VariableReader.class, "reader", readerExpression);
        if (isPrimitive) {
            block.declareVar(variableType, "value", cast(variableType, exprDotMethod(ref("reader"), "getValue")))
                    .methodReturn(ref("value"));
        } else {
            block.declareVar(Object.class, "value", exprDotMethod(ref("reader"), "getValue"))
                    .ifRefNullReturnNull("value")
                    .declareVar(EventBean.class, "theEvent", cast(EventBean.class, ref("value")));
            if (optSubPropName == null) {
                block.methodReturn(cast(variableType, exprDotUnderlying(ref("theEvent"))));
            } else {
                block.methodReturn(CodegenLegoCast.castSafeFromObjectType(variableType, eventTypeGetter.eventBeanGetCodegen(ref("theEvent"), methodNode, codegenClassScope)));
            }
        }
        return localMethod(methodNode);
    }

    public ExprForgeComplexityEnum getComplexity() {
        return ExprForgeComplexityEnum.SINGLE;
    }

    public void toPrecedenceFreeEPL(StringWriter writer) {
        writer.append(variableName);
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
        return that.variableName.equals(this.variableName);
    }

    public String getVariableNameWithSubProp() {
        if (optSubPropName == null) {
            return variableName;
        }
        return variableName + "." + optSubPropName;
    }
}
