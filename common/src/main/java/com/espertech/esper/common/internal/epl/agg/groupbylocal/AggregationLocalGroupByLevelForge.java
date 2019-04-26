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
package com.espertech.esper.common.internal.epl.agg.groupbylocal;

import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder;
import com.espertech.esper.common.internal.compile.multikey.MultiKeyClassRef;
import com.espertech.esper.common.internal.epl.agg.core.AggregationForgeFactory;
import com.espertech.esper.common.internal.epl.agg.core.AggregationStateFactoryForge;
import com.espertech.esper.common.internal.epl.expression.core.ExprForge;
import com.espertech.esper.common.internal.epl.expression.core.ExprNode;
import com.espertech.esper.common.internal.epl.expression.core.ExprNodeUtilityQuery;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

public class AggregationLocalGroupByLevelForge {

    private final ExprForge[][] methodForges;
    private final AggregationForgeFactory[] methodFactories;
    private final AggregationStateFactoryForge[] accessStateForges;
    private final ExprNode[] partitionForges;
    private final MultiKeyClassRef partitionMKClasses;
    private final boolean isDefaultLevel;

    public AggregationLocalGroupByLevelForge(ExprForge[][] methodForges, AggregationForgeFactory[] methodFactories, AggregationStateFactoryForge[] accessStateForges, ExprNode[] partitionForges, MultiKeyClassRef partitionMKClasses, boolean defaultLevel) {
        this.methodForges = methodForges;
        this.methodFactories = methodFactories;
        this.accessStateForges = accessStateForges;
        this.partitionForges = partitionForges;
        this.partitionMKClasses = partitionMKClasses;
        isDefaultLevel = defaultLevel;
    }

    public ExprForge[][] getMethodForges() {
        return methodForges;
    }

    public AggregationForgeFactory[] getMethodFactories() {
        return methodFactories;
    }

    public AggregationStateFactoryForge[] getAccessStateForges() {
        return accessStateForges;
    }

    public ExprNode[] getPartitionForges() {
        return partitionForges;
    }

    public boolean isDefaultLevel() {
        return isDefaultLevel;
    }

    public MultiKeyClassRef getPartitionMKClasses() {
        return partitionMKClasses;
    }

    public CodegenExpression toExpression(String rowFactory, String rowSerde, CodegenExpression groupKeyEval, CodegenMethod method, CodegenClassScope classScope) {
        return newInstance(AggregationLocalGroupByLevel.class,
                CodegenExpressionBuilder.newInstance(rowFactory, ref("this")),
                CodegenExpressionBuilder.newInstance(rowSerde, ref("this")),
                constant(ExprNodeUtilityQuery.getExprResultTypes(partitionForges)),
                groupKeyEval,
                constant(isDefaultLevel),
                partitionMKClasses.getExprMKSerde(method, classScope));
    }
}
