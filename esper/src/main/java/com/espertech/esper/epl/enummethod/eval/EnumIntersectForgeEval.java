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
package com.espertech.esper.epl.enummethod.eval;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.codegen.core.CodegenBlock;
import com.espertech.esper.codegen.core.CodegenContext;
import com.espertech.esper.codegen.model.expression.CodegenExpression;
import com.espertech.esper.codegen.model.method.CodegenParamSetEnumMethodNonPremade;
import com.espertech.esper.codegen.model.method.CodegenParamSetEnumMethodPremade;
import com.espertech.esper.codegen.model.method.CodegenParamSetExprPremade;
import com.espertech.esper.epl.expression.core.ExprEnumerationEval;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;

import java.util.ArrayList;
import java.util.Collection;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.*;

public class EnumIntersectForgeEval implements EnumEval {

    private final EnumIntersectForge forge;
    private final ExprEnumerationEval evaluator;

    public EnumIntersectForgeEval(EnumIntersectForge forge, ExprEnumerationEval evaluator) {
        this.forge = forge;
        this.evaluator = evaluator;
    }

    public Object evaluateEnumMethod(EventBean[] eventsLambda, Collection enumcoll, boolean isNewData, ExprEvaluatorContext context) {
        Collection other;
        if (forge.scalar) {
            other = evaluator.evaluateGetROCollectionScalar(eventsLambda, isNewData, context);
        } else {
            other = evaluator.evaluateGetROCollectionEvents(eventsLambda, isNewData, context);
        }

        return enumIntersectForgeEvalSet(other, enumcoll, forge.scalar);
    }

    public static CodegenExpression codegen(EnumIntersectForge forge, CodegenParamSetEnumMethodNonPremade args, CodegenContext context) {
        CodegenParamSetEnumMethodPremade premade = CodegenParamSetEnumMethodPremade.INSTANCE;
        CodegenBlock block = context.addMethod(Collection.class, EnumIntersectForgeEval.class).add(premade).begin();
        if (forge.scalar) {
            block.declareVar(Collection.class, "other", forge.evaluatorForge.evaluateGetROCollectionScalarCodegen(CodegenParamSetExprPremade.INSTANCE, context));
        } else {
            block.declareVar(Collection.class, "other", forge.evaluatorForge.evaluateGetROCollectionEventsCodegen(CodegenParamSetExprPremade.INSTANCE, context));
        }
        String method = block.methodReturn(staticMethod(EnumIntersectForgeEval.class, "enumIntersectForgeEvalSet", ref("other"), premade.enumcoll(), constant(forge.scalar)));
        return localMethodBuild(method).passAll(args).call();
    }

    /**
     * NOTE: Code-generation-invoked method, method name and parameter order matters
     *
     * @param other    other
     * @param enumcoll coll
     * @param scalar   indicator
     * @return intersection
     */
    public static Collection enumIntersectForgeEvalSet(Collection other, Collection enumcoll, boolean scalar) {
        if (other == null || other.isEmpty() || enumcoll.isEmpty()) {
            return enumcoll;
        }

        if (scalar) {
            ArrayList<Object> result = new ArrayList<Object>(enumcoll);
            result.retainAll(other);
            return result;
        }

        Collection<EventBean> targetEvents = (Collection<EventBean>) enumcoll;
        Collection<EventBean> sourceEvents = (Collection<EventBean>) other;
        ArrayList<EventBean> result = new ArrayList<EventBean>();

        // we compare event underlying
        for (EventBean targetEvent : targetEvents) {
            if (targetEvent == null) {
                result.add(null);
                continue;
            }

            boolean found = false;
            for (EventBean sourceEvent : sourceEvents) {
                if (targetEvent == sourceEvent) {
                    found = true;
                    break;
                }
                if (sourceEvent == null) {
                    continue;
                }
                if (targetEvent.getUnderlying().equals(sourceEvent.getUnderlying())) {
                    found = true;
                    break;
                }
            }

            if (found) {
                result.add(targetEvent);
            }
        }
        return result;
    }
}
