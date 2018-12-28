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
package com.espertech.esper.common.internal.epl.agg.access.sorted;

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.client.hook.aggmultifunc.AggregationMultiFunctionMethodDesc;
import com.espertech.esper.common.client.util.HashableMultiKey;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.context.aifactory.core.ModuleTableInitializeSymbol;
import com.espertech.esper.common.internal.epl.agg.core.AggregationForgeFactory;
import com.espertech.esper.common.internal.epl.agg.core.AggregationMethodForge;
import com.espertech.esper.common.internal.epl.agg.core.AggregationPortableValidation;
import com.espertech.esper.common.internal.epl.agg.core.AggregationValidationUtil;
import com.espertech.esper.common.internal.epl.expression.core.*;
import com.espertech.esper.common.internal.epl.methodbase.*;
import com.espertech.esper.common.internal.event.core.EventTypeUtility;
import com.espertech.esper.common.internal.util.JavaClassHelper;

import java.util.Arrays;
import java.util.Locale;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

public class AggregationPortableValidationSorted implements AggregationPortableValidation {
    private String aggFuncName;
    private EventType containedEventType;
    private Class[] optionalCriteriaTypes;

    public AggregationPortableValidationSorted() {
    }

    public AggregationPortableValidationSorted(String aggFuncName, EventType containedEventType, Class[] optionalCriteriaTypes) {
        this.aggFuncName = aggFuncName;
        this.containedEventType = containedEventType;
        this.optionalCriteriaTypes = optionalCriteriaTypes;
    }

    public void setAggFuncName(String aggFuncName) {
        this.aggFuncName = aggFuncName;
    }

    public void setContainedEventType(EventType containedEventType) {
        this.containedEventType = containedEventType;
    }

    public void setOptionalCriteriaTypes(Class[] optionalCriteriaTypes) {
        this.optionalCriteriaTypes = optionalCriteriaTypes;
    }

    public void validateIntoTableCompatible(String tableExpression, AggregationPortableValidation intoTableAgg, String intoExpression, AggregationForgeFactory factory) throws ExprValidationException {
        AggregationValidationUtil.validateAggregationType(this, tableExpression, intoTableAgg, intoExpression);
        AggregationPortableValidationSorted other = (AggregationPortableValidationSorted) intoTableAgg;
        AggregationValidationUtil.validateEventType(this.containedEventType, other.containedEventType);
        AggregationValidationUtil.validateAggFuncName(aggFuncName, other.aggFuncName);
    }

    public CodegenExpression make(CodegenMethodScope parent, ModuleTableInitializeSymbol symbols, CodegenClassScope classScope) {
        CodegenMethod method = parent.makeChild(AggregationPortableValidationSorted.class, this.getClass(), classScope);
        method.getBlock()
            .declareVar(AggregationPortableValidationSorted.class, "v", newInstance(AggregationPortableValidationSorted.class))
            .exprDotMethod(ref("v"), "setAggFuncName", constant(aggFuncName))
            .exprDotMethod(ref("v"), "setContainedEventType", EventTypeUtility.resolveTypeCodegen(containedEventType, symbols.getAddInitSvc(method)))
            .exprDotMethod(ref("v"), "setOptionalCriteriaTypes", constant(optionalCriteriaTypes))
            .methodReturn(ref("v"));
        return localMethod(method);
    }

    public boolean isAggregationMethod(String nameMixed, ExprNode[] parameters, ExprValidationContext validationContext) {
        String name = nameMixed.toLowerCase(Locale.ENGLISH);
        if (name.equals("maxby") || name.equals("minby")) {
            return parameters.length == 0;
        }
        AggregationMethodSortedEnum methodEnum = AggregationMethodSortedEnum.fromString(nameMixed);
        return name.equals("sorted") || methodEnum != null;
    }

    public AggregationMultiFunctionMethodDesc validateAggregationMethod(ExprValidationContext validationContext, String aggMethodName, ExprNode[] params) throws ExprValidationException {
        String name = aggMethodName.toLowerCase(Locale.ENGLISH);
        Class componentType = containedEventType.getUnderlyingType();
        if (name.equals("maxby") || name.equals("minby")) {
            AggregationMethodSortedMinMaxByForge forge = new AggregationMethodSortedMinMaxByForge(componentType, name.equals("maxby"));
            return new AggregationMultiFunctionMethodDesc(forge, null, null, containedEventType);
        } else if (name.equals("sorted")) {
            AggregationMethodSortedWindowForge forge = new AggregationMethodSortedWindowForge(JavaClassHelper.getArrayType(componentType));
            return new AggregationMultiFunctionMethodDesc(forge, containedEventType, null, null);
        }

        // validate all parameters
        for (int i = 0; i < params.length; i++) {
            params[i] = ExprNodeUtilityValidate.getValidatedSubtree(ExprNodeOrigin.AGGPARAM, params[i], validationContext);
        }

        // determine method
        AggregationMethodSortedEnum methodEnum = AggregationMethodSortedEnum.fromString(aggMethodName);

        // validate footprint
        DotMethodFPProvided footprintProvided = DotMethodUtil.getProvidedFootprint(Arrays.asList(params));
        DotMethodFP[] footprints = methodEnum.getFootprint().getFp();
        DotMethodUtil.validateParametersDetermineFootprint(footprints, DotMethodTypeEnum.AGGMETHOD, aggMethodName, footprintProvided, DotMethodInputTypeMatcher.DEFAULT_ALL);

        Class keyType = optionalCriteriaTypes == null ? Comparable.class : (optionalCriteriaTypes.length == 1 ? optionalCriteriaTypes[0] : HashableMultiKey.class);
        Class resultType = methodEnum.getResultType(containedEventType.getUnderlyingType(), keyType);

        AggregationMethodForge forge;
        if (methodEnum.getFootprint() == AggregationMethodSortedFootprintEnum.SUBMAP) {
            validateKeyType(aggMethodName, 0, keyType, params[0]);
            validateKeyType(aggMethodName, 2, keyType, params[2]);
            forge = new AggregationMethodSortedSubmapForge(params[0], params[1], params[2], params[3], componentType, methodEnum, resultType);
        } else if (methodEnum.getFootprint() == AggregationMethodSortedFootprintEnum.KEYONLY) {
            validateKeyType(aggMethodName, 0, keyType, params[0]);
            forge = new AggregationMethodSortedKeyedForge(params[0], componentType, methodEnum, resultType);
        } else {
            forge = new AggregationMethodSortedNoParamForge(componentType, methodEnum, resultType);
        }

        EventType eventTypeCollection = methodEnum.isReturnsCollectionOfEvents() ? containedEventType : null;
        EventType eventTypeSingle = methodEnum.isReturnsSingleEvent() ? containedEventType : null;
        return new AggregationMultiFunctionMethodDesc(forge, eventTypeCollection, null, eventTypeSingle);
    }

    private void validateKeyType(String aggMethodName, int parameterNumber, Class keyType, ExprNode validated) throws ExprValidationException {
        Class keyBoxed = JavaClassHelper.getBoxedType(keyType);
        Class providedBoxed = JavaClassHelper.getBoxedType(validated.getForge().getEvaluationType());
        if (keyBoxed != providedBoxed) {
            throw new ExprValidationException("Method '" + aggMethodName + "' for parameter " + parameterNumber + " requires a key of type '" + JavaClassHelper.getClassNameFullyQualPretty(keyBoxed) + "' but receives '" + JavaClassHelper.getClassNameFullyQualPretty(providedBoxed) + "'");
        }
    }
}
