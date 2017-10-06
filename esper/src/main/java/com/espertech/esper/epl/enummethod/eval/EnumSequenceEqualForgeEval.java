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
import com.espertech.esper.codegen.base.CodegenClassScope;
import com.espertech.esper.codegen.base.CodegenMethodScope;
import com.espertech.esper.codegen.model.expression.CodegenExpression;
import com.espertech.esper.epl.enummethod.codegen.EnumForgeCodegenParams;
import com.espertech.esper.epl.enummethod.codegen.EnumForgeCodegenNames;
import com.espertech.esper.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.codegen.base.CodegenMethodNode;
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Iterator;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.localMethod;
import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.staticMethod;

public class EnumSequenceEqualForgeEval implements EnumEval {

    private static final Logger log = LoggerFactory.getLogger(EnumSequenceEqualForgeEval.class);

    private final ExprEvaluator innerExpression;

    public EnumSequenceEqualForgeEval(ExprEvaluator innerExpression) {
        this.innerExpression = innerExpression;
    }

    public Object evaluateEnumMethod(EventBean[] eventsLambda, Collection enumcoll, boolean isNewData, ExprEvaluatorContext context) {
        Object otherObj = innerExpression.evaluate(eventsLambda, isNewData, context);
        return enumSequenceEqualsCompare(enumcoll, otherObj);
    }

    public static CodegenExpression codegen(EnumSequenceEqualForge forge, EnumForgeCodegenParams args, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        ExprForgeCodegenSymbol scope = new ExprForgeCodegenSymbol(false, null);
        CodegenMethodNode methodNode = codegenMethodScope.makeChildWithScope(boolean.class, EnumSequenceEqualForgeEval.class, scope, codegenClassScope).addParam(EnumForgeCodegenNames.PARAMS);

        methodNode.getBlock().methodReturn(staticMethod(EnumSequenceEqualForgeEval.class, "enumSequenceEqualsCompare", EnumForgeCodegenNames.REF_ENUMCOLL, forge.innerExpression.evaluateCodegen(Object.class, methodNode, scope, codegenClassScope)));
        return localMethod(methodNode, args.getEps(), args.getEnumcoll(), args.getIsNewData(), args.getExprCtx());
    }

    public static boolean enumSequenceEqualsCompare(Collection enumcoll, Object otherObj) {
        if (otherObj == null) {
            return false;
        }
        if (!(otherObj instanceof Collection)) {
            if (otherObj.getClass().isArray()) {
                if (enumcoll.size() != Array.getLength(otherObj)) {
                    return false;
                }

                if (enumcoll.isEmpty()) {
                    return true;
                }

                Iterator oneit = enumcoll.iterator();
                for (int i = 0; i < enumcoll.size(); i++) {
                    Object first = oneit.next();
                    Object second = Array.get(otherObj, i);

                    if (first == null) {
                        if (second != null) {
                            return false;
                        }
                        continue;
                    }
                    if (second == null) {
                        return false;
                    }

                    if (!first.equals(second)) {
                        return false;
                    }
                }

                return true;
            } else {
                log.warn("Enumeration method 'sequenceEqual' expected a Collection-type return value from its parameter but received '" + otherObj.getClass() + "'");
                return false;
            }
        }

        Collection other = (Collection) otherObj;
        if (enumcoll.size() != other.size()) {
            return false;
        }

        if (enumcoll.isEmpty()) {
            return true;
        }

        Iterator oneit = enumcoll.iterator();
        Iterator twoit = other.iterator();
        for (int i = 0; i < enumcoll.size(); i++) {
            Object first = oneit.next();
            Object second = twoit.next();

            if (first == null) {
                if (second != null) {
                    return false;
                }
                continue;
            }
            if (second == null) {
                return false;
            }

            if (!first.equals(second)) {
                return false;
            }
        }

        return true;
    }
}
