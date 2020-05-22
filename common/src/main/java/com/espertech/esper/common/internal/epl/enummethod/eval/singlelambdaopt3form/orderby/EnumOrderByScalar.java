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
package com.espertech.esper.common.internal.epl.enummethod.eval.singlelambdaopt3form.orderby;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenBlock;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.epl.enummethod.dot.ExprDotEvalParamLambda;
import com.espertech.esper.common.internal.epl.enummethod.eval.EnumEval;
import com.espertech.esper.common.internal.epl.enummethod.eval.singlelambdaopt3form.base.ThreeFormScalar;
import com.espertech.esper.common.internal.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluator;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.common.internal.event.arr.ObjectArrayEventBean;
import com.espertech.esper.common.internal.event.arr.ObjectArrayEventType;
import com.espertech.esper.common.internal.util.JavaClassHelper;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.TreeMap;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

public class EnumOrderByScalar extends ThreeFormScalar {

    protected final boolean descending;
    private final Class innerBoxedType;

    public EnumOrderByScalar(ExprDotEvalParamLambda lambda, ObjectArrayEventType fieldEventType, int numParameters, boolean descending) {
        super(lambda, fieldEventType, numParameters);
        this.descending = descending;
        this.innerBoxedType = JavaClassHelper.getBoxedType(innerExpression.getEvaluationType());
    }

    public EnumEval getEnumEvaluator() {
        ExprEvaluator inner = innerExpression.getExprEvaluator();
        return new EnumEval() {
            public Object evaluateEnumMethod(EventBean[] eventsLambda, Collection enumcoll, boolean isNewData, ExprEvaluatorContext context) {
                TreeMap<Comparable, Object> sort = new TreeMap<Comparable, Object>();
                boolean hasColl = false;

                ObjectArrayEventBean resultEvent = new ObjectArrayEventBean(new Object[3], fieldEventType);
                eventsLambda[getStreamNumLambda()] = resultEvent;
                Object[] props = resultEvent.getProperties();
                props[2] = enumcoll.size();
                Collection<Object> values = (Collection<Object>) enumcoll;

                int count = -1;
                for (Object next : values) {
                    count++;
                    props[1] = count;
                    props[0] = next;

                    Comparable comparable = (Comparable) inner.evaluate(eventsLambda, isNewData, context);
                    Object entry = sort.get(comparable);

                    if (entry == null) {
                        sort.put(comparable, next);
                        continue;
                    }

                    if (entry instanceof Collection) {
                        ((Collection) entry).add(next);
                        continue;
                    }

                    Deque<Object> coll = new ArrayDeque<Object>();
                    coll.add(entry);
                    coll.add(next);
                    sort.put(comparable, coll);
                    hasColl = true;
                }

                return EnumOrderByHelper.enumOrderBySortEval(sort, hasColl, descending);
            }
        };
    }

    public Class returnType() {
        return Collection.class;
    }

    public CodegenExpression returnIfEmptyOptional() {
        return null;
    }

    public void initBlock(CodegenBlock block, CodegenMethod methodNode, ExprForgeCodegenSymbol scope, CodegenClassScope codegenClassScope) {
        block.declareVar(TreeMap.class, "sort", newInstance(TreeMap.class))
            .declareVar(boolean.class, "hasColl", constantFalse());
    }

    public void forEachBlock(CodegenBlock block, CodegenMethod methodNode, ExprForgeCodegenSymbol scope, CodegenClassScope codegenClassScope) {
        EnumOrderByHelper.sortingCode(block, innerBoxedType, innerExpression, methodNode, scope, codegenClassScope);
    }

    public void returnResult(CodegenBlock block) {
        block.methodReturn(staticMethod(EnumOrderByHelper.class, "enumOrderBySortEval", ref("sort"), ref("hasColl"), constant(descending)));
    }
}
