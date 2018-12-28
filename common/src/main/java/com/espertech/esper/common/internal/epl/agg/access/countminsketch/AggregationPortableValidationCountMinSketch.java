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
package com.espertech.esper.common.internal.epl.agg.access.countminsketch;

import com.espertech.esper.common.client.hook.aggmultifunc.AggregationMultiFunctionMethodDesc;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.context.aifactory.core.ModuleTableInitializeSymbol;
import com.espertech.esper.common.internal.epl.agg.core.AggregationForgeFactory;
import com.espertech.esper.common.internal.epl.agg.core.AggregationPortableValidation;
import com.espertech.esper.common.internal.epl.agg.core.AggregationMethodForge;
import com.espertech.esper.common.internal.epl.agg.core.AggregationValidationUtil;
import com.espertech.esper.common.internal.epl.approx.countminsketch.CountMinSketchAggMethod;
import com.espertech.esper.common.internal.epl.approx.countminsketch.CountMinSketchAggType;
import com.espertech.esper.common.internal.epl.expression.core.*;
import com.espertech.esper.common.internal.util.JavaClassHelper;

import java.util.Arrays;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;
import static com.espertech.esper.common.internal.epl.expression.agg.accessagg.ExprAggMultiFunctionCountMinSketchNode.MSG_NAME;

public class AggregationPortableValidationCountMinSketch implements AggregationPortableValidation {

    private Class[] acceptableValueTypes;

    public AggregationPortableValidationCountMinSketch() {
    }

    public AggregationPortableValidationCountMinSketch(Class[] acceptableValueTypes) {
        this.acceptableValueTypes = acceptableValueTypes;
    }

    public void setAcceptableValueTypes(Class[] acceptableValueTypes) {
        this.acceptableValueTypes = acceptableValueTypes;
    }

    public void validateIntoTableCompatible(String tableExpression, AggregationPortableValidation intoTableAgg, String intoExpression, AggregationForgeFactory factory) throws ExprValidationException {
        AggregationValidationUtil.validateAggregationType(this, tableExpression, intoTableAgg, intoExpression);

        if (factory instanceof AggregationForgeFactoryAccessCountMinSketchAdd) {
            AggregationForgeFactoryAccessCountMinSketchAdd add = (AggregationForgeFactoryAccessCountMinSketchAdd) factory;
            CountMinSketchAggType aggType = add.getParent().getAggType();
            if (aggType == CountMinSketchAggType.ADD) {
                Class clazz = add.getAddOrFrequencyEvaluatorReturnType();
                boolean foundMatch = false;
                for (Class allowed : acceptableValueTypes) {
                    if (JavaClassHelper.isSubclassOrImplementsInterface(clazz, allowed)) {
                        foundMatch = true;
                    }
                }
                if (!foundMatch) {
                    throw new ExprValidationException("Mismatching parameter return type, expected any of " + Arrays.toString(acceptableValueTypes) + " but received " + JavaClassHelper.getClassNameFullyQualPretty(clazz));
                }
            }
        }
    }

    public CodegenExpression make(CodegenMethodScope parent, ModuleTableInitializeSymbol symbols, CodegenClassScope classScope) {
        CodegenMethod method = parent.makeChild(AggregationPortableValidationCountMinSketch.class, this.getClass(), classScope);
        method.getBlock()
                .declareVar(AggregationPortableValidationCountMinSketch.class, "v", newInstance(AggregationPortableValidationCountMinSketch.class))
                .exprDotMethod(ref("v"), "setAcceptableValueTypes", constant(acceptableValueTypes))
                .methodReturn(ref("v"));
        return localMethod(method);
    }

    public boolean isAggregationMethod(String name, ExprNode[] parameters, ExprValidationContext validationContext) {
        return CountMinSketchAggMethod.fromNameMayMatch(name) != null;
    }

    public AggregationMultiFunctionMethodDesc validateAggregationMethod(ExprValidationContext validationContext, String aggMethodName, ExprNode[] params) throws ExprValidationException {
        CountMinSketchAggMethod aggMethod = CountMinSketchAggMethod.fromNameMayMatch(aggMethodName);
        AggregationMethodForge forge;
        if (aggMethod == CountMinSketchAggMethod.FREQ) {
            if (params.length == 0 || params.length > 1) {
                throw new ExprValidationException(getMessagePrefix(aggMethod) + "requires a single parameter expression");
            }
            ExprNodeUtilityValidate.getValidatedSubtree(ExprNodeOrigin.AGGPARAM, params, validationContext);
            ExprNode frequencyEval = params[0];
            forge = new AgregationMethodCountMinSketchFreqForge(frequencyEval);
        } else {
            if (params.length != 0) {
                throw new ExprValidationException(getMessagePrefix(aggMethod) + "requires a no parameter expressions");
            }
            forge = new AgregationMethodCountMinSketchTopKForge();
        }
        return new AggregationMultiFunctionMethodDesc(forge, null, null, null);
    }

    private String getMessagePrefix(CountMinSketchAggMethod aggType) {
        return MSG_NAME + " aggregation function '" + aggType.getMethodName() + "' ";
    }
}
