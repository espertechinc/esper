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

import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.context.aifactory.core.ModuleTableInitializeSymbol;
import com.espertech.esper.common.internal.epl.agg.core.AggregationForgeFactory;
import com.espertech.esper.common.internal.epl.agg.core.AggregationPortableValidation;
import com.espertech.esper.common.internal.epl.agg.core.AggregationValidationUtil;
import com.espertech.esper.common.internal.epl.approx.countminsketch.CountMinSketchAggType;
import com.espertech.esper.common.internal.epl.expression.core.ExprValidationException;
import com.espertech.esper.common.internal.util.JavaClassHelper;

import java.util.Arrays;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

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
            if (aggType == CountMinSketchAggType.FREQ || aggType == CountMinSketchAggType.ADD) {
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
}
