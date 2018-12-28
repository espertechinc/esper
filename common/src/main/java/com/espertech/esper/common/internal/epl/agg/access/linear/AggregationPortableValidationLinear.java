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
package com.espertech.esper.common.internal.epl.agg.access.linear;

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.client.hook.aggmultifunc.AggregationMultiFunctionMethodDesc;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.context.aifactory.core.ModuleTableInitializeSymbol;
import com.espertech.esper.common.internal.epl.agg.core.AggregationForgeFactory;
import com.espertech.esper.common.internal.epl.agg.core.AggregationPortableValidation;
import com.espertech.esper.common.internal.epl.agg.core.AggregationValidationUtil;
import com.espertech.esper.common.internal.epl.expression.agg.accessagg.ExprAggMultiFunctionUtil;
import com.espertech.esper.common.internal.epl.expression.core.*;
import com.espertech.esper.common.internal.epl.streamtype.StreamTypeServiceImpl;
import com.espertech.esper.common.internal.epl.table.compiletime.TableCompileTimeUtil;
import com.espertech.esper.common.internal.event.core.EventTypeUtility;
import com.espertech.esper.common.internal.util.JavaClassHelper;

import java.util.List;
import java.util.Locale;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

public class AggregationPortableValidationLinear implements AggregationPortableValidation {
    private EventType containedEventType;

    public AggregationPortableValidationLinear() {
    }

    public AggregationPortableValidationLinear(EventType containedEventType) {
        this.containedEventType = containedEventType;
    }

    public void setContainedEventType(EventType containedEventType) {
        this.containedEventType = containedEventType;
    }

    public EventType getContainedEventType() {
        return containedEventType;
    }

    public void validateIntoTableCompatible(String tableExpression, AggregationPortableValidation intoTableAgg, String intoExpression, AggregationForgeFactory factory) throws ExprValidationException {
        AggregationValidationUtil.validateAggregationType(this, tableExpression, intoTableAgg, intoExpression);
        AggregationPortableValidationLinear other = (AggregationPortableValidationLinear) intoTableAgg;
        AggregationValidationUtil.validateEventType(this.containedEventType, other.getContainedEventType());
    }

    public CodegenExpression make(CodegenMethodScope parent, ModuleTableInitializeSymbol symbols, CodegenClassScope classScope) {
        CodegenMethod method = parent.makeChild(AggregationPortableValidationLinear.class, this.getClass(), classScope);
        method.getBlock()
            .declareVar(AggregationPortableValidationLinear.class, "v", newInstance(AggregationPortableValidationLinear.class))
            .exprDotMethod(ref("v"), "setContainedEventType", EventTypeUtility.resolveTypeCodegen(containedEventType, symbols.getAddInitSvc(method)))
            .methodReturn(ref("v"));
        return localMethod(method);
    }

    public boolean isAggregationMethod(String name, ExprNode[] parameters, ExprValidationContext validationContext) {
        name = name.toLowerCase(Locale.ENGLISH);
        return AggregationAccessorLinearType.fromString(name) != null || name.equals("countevents") || name.equals("listreference");
    }

    public AggregationMultiFunctionMethodDesc validateAggregationMethod(ExprValidationContext validationContext, String aggMethodName, ExprNode[] params) throws ExprValidationException {
        aggMethodName = aggMethodName.toLowerCase(Locale.ENGLISH);
        if (aggMethodName.equals("countevents") || aggMethodName.equals("listreference")) {
            if (params.length > 0) {
                throw new ExprValidationException("Invalid number of parameters");
            }
            Class provider = AggregationMethodLinearCount.class;
            Class result = Integer.class;
            if (aggMethodName.equals("listreference")) {
                provider = AggregationMethodLinearListReference.class;
                result = List.class;
            }
            return new AggregationMultiFunctionMethodDesc(new AggregationMethodLinearNoParamForge(provider, result), null, null, null);
        }
        AggregationAccessorLinearType methodType = AggregationAccessorLinearType.fromString(aggMethodName);
        if (methodType == AggregationAccessorLinearType.FIRST || methodType == AggregationAccessorLinearType.LAST) {
            return handleMethodFirstLast(params, methodType, validationContext);
        } else {
            return handleMethodWindow(params, validationContext);
        }
    }

    private AggregationMultiFunctionMethodDesc handleMethodWindow(ExprNode[] childNodes, ExprValidationContext validationContext)
        throws ExprValidationException {

        if (childNodes.length == 0 || (childNodes.length == 1 && childNodes[0] instanceof ExprWildcard)) {
            Class componentType = getContainedEventType().getUnderlyingType();
            AggregationMethodLinearWindowForge forge = new AggregationMethodLinearWindowForge(JavaClassHelper.getArrayType(componentType), null);
            return new AggregationMultiFunctionMethodDesc(forge, getContainedEventType(), null, null);
        }
        if (childNodes.length == 1) {
            // Expressions apply to events held, thereby validate in terms of event value expressions
            ExprNode paramNode = childNodes[0];
            StreamTypeServiceImpl streams = TableCompileTimeUtil.streamTypeFromTableColumn(getContainedEventType());
            ExprValidationContext localValidationContext = new ExprValidationContext(streams, validationContext);
            paramNode = ExprNodeUtilityValidate.getValidatedSubtree(ExprNodeOrigin.AGGPARAM, paramNode, localValidationContext);
            Class paramNodeType = JavaClassHelper.getBoxedType(paramNode.getForge().getEvaluationType());
            AggregationMethodLinearWindowForge forge = new AggregationMethodLinearWindowForge(JavaClassHelper.getArrayType(paramNodeType), paramNode);
            return new AggregationMultiFunctionMethodDesc(forge, null, paramNodeType, null);
        }
        throw new ExprValidationException("Invalid number of parameters");
    }

    private AggregationMultiFunctionMethodDesc handleMethodFirstLast(ExprNode[] childNodes, AggregationAccessorLinearType methodType, ExprValidationContext validationContext)
        throws ExprValidationException {
        Class underlyingType = getContainedEventType().getUnderlyingType();
        if (childNodes.length == 0) {
            AggregationMethodLinearFirstLastForge forge = new AggregationMethodLinearFirstLastForge(underlyingType, methodType, null);
            return new AggregationMultiFunctionMethodDesc(forge, null, null, getContainedEventType());
        }
        if (childNodes.length == 1) {
            if (childNodes[0] instanceof ExprWildcard) {
                AggregationMethodLinearFirstLastForge forge = new AggregationMethodLinearFirstLastForge(underlyingType, methodType, null);
                return new AggregationMultiFunctionMethodDesc(forge, null, null, getContainedEventType());
            }
            if (childNodes[0] instanceof ExprStreamUnderlyingNode) {
                throw new ExprValidationException("Stream-wildcard is not allowed for table column access");
            }
            // Expressions apply to events held, thereby validate in terms of event value expressions
            ExprNode paramNode = childNodes[0];
            StreamTypeServiceImpl streams = TableCompileTimeUtil.streamTypeFromTableColumn(getContainedEventType());
            ExprValidationContext localValidationContext = new ExprValidationContext(streams, validationContext);
            paramNode = ExprNodeUtilityValidate.getValidatedSubtree(ExprNodeOrigin.AGGPARAM, paramNode, localValidationContext);
            AggregationMethodLinearFirstLastForge forge = new AggregationMethodLinearFirstLastForge(paramNode.getForge().getEvaluationType(), methodType, paramNode);
            return new AggregationMultiFunctionMethodDesc(forge, null, null, null);
        }
        if (childNodes.length == 2) {
            Integer constant = null;
            ExprNode indexEvalNode = childNodes[1];
            Class indexEvalType = indexEvalNode.getForge().getEvaluationType();
            if (indexEvalType != Integer.class && indexEvalType != int.class) {
                throw new ExprValidationException(getErrorPrefix(methodType) + " requires a constant index expression that returns an integer value");
            }

            ExprNode indexExpr;
            if (indexEvalNode.getForge().getForgeConstantType() == ExprForgeConstantType.COMPILETIMECONST) {
                constant = (Integer) indexEvalNode.getForge().getExprEvaluator().evaluate(null, true, null);
                indexExpr = null;
            } else {
                indexExpr = indexEvalNode;
            }
            AggregationMethodLinearFirstLastIndexForge forge = new AggregationMethodLinearFirstLastIndexForge(underlyingType, methodType, constant, indexExpr);
            return new AggregationMultiFunctionMethodDesc(forge, null, null, getContainedEventType());
        }
        throw new ExprValidationException("Invalid number of parameters");
    }

    private static String getErrorPrefix(AggregationAccessorLinearType stateType) {
        return ExprAggMultiFunctionUtil.getErrorPrefix(stateType.toString().toLowerCase(Locale.ENGLISH));
    }
}
