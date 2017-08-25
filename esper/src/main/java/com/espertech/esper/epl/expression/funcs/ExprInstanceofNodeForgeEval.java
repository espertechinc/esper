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
package com.espertech.esper.epl.expression.funcs;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.codegen.base.CodegenBlock;
import com.espertech.esper.codegen.base.CodegenClassScope;
import com.espertech.esper.codegen.base.CodegenMember;
import com.espertech.esper.codegen.base.CodegenMethodScope;
import com.espertech.esper.codegen.model.expression.CodegenExpression;
import com.espertech.esper.collection.Pair;
import com.espertech.esper.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.codegen.base.CodegenMethodNode;
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.util.JavaClassHelper;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.*;

public class ExprInstanceofNodeForgeEval implements ExprEvaluator {
    private final ExprInstanceofNodeForge forge;
    private final ExprEvaluator evaluator;
    private final CopyOnWriteArrayList<Pair<Class, Boolean>> resultCache = new CopyOnWriteArrayList<Pair<Class, Boolean>>();

    public ExprInstanceofNodeForgeEval(ExprInstanceofNodeForge forge, ExprEvaluator evaluator) {
        this.forge = forge;
        this.evaluator = evaluator;
    }

    public Object evaluate(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().qExprInstanceof(forge.getForgeRenderable());
        }
        Object result = evaluator.evaluate(eventsPerStream, isNewData, exprEvaluatorContext);
        if (result == null) {
            if (InstrumentationHelper.ENABLED) {
                InstrumentationHelper.get().aExprInstanceof(false);
            }
            return false;
        }
        return instanceofCacheCheckOrAdd(forge.getClasses(), resultCache, result);
    }

    /**
     * NOTE: Code-generation-invoked method, method name and parameter order matters
     * @param classes classes
     * @param resultCache cache
     * @param result result
     * @return bool
     */
    public static Boolean instanceofCacheCheckOrAdd(Class[] classes, CopyOnWriteArrayList<Pair<Class, Boolean>> resultCache, Object result) {
        // return cached value
        for (Pair<Class, Boolean> pair : resultCache) {
            if (pair.getFirst() == result.getClass()) {
                if (InstrumentationHelper.ENABLED) {
                    InstrumentationHelper.get().aExprInstanceof(pair.getSecond());
                }
                return pair.getSecond();
            }
        }

        Boolean out = checkAddType(classes, result.getClass(), resultCache);
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().aExprInstanceof(out);
        }
        return out;
    }

    public static CodegenExpression codegen(ExprInstanceofNodeForge forge, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        CodegenMember mClasses = codegenClassScope.makeAddMember(Class[].class, forge.getClasses());
        CodegenMember mCache = codegenClassScope.makeAddMember(CopyOnWriteArrayList.class, new CopyOnWriteArrayList<Pair<Class, Boolean>>());
        CodegenMethodNode methodNode = codegenMethodScope.makeChild(Boolean.class, ExprInstanceofNodeForgeEval.class, codegenClassScope);

        CodegenBlock block = methodNode.getBlock()
                .declareVar(Object.class, "result", forge.getForgeRenderable().getChildNodes()[0].getForge().evaluateCodegen(Object.class, methodNode, exprSymbol, codegenClassScope))
                .ifRefNullReturnFalse("result");
        block.methodReturn(staticMethod(ExprInstanceofNodeForgeEval.class, "instanceofCacheCheckOrAdd", member(mClasses.getMemberId()), member(mCache.getMemberId()), ref("result")));
        return localMethod(methodNode);
    }

    // Checks type and adds to cache
    private static Boolean checkAddType(Class[] classes, Class type, CopyOnWriteArrayList<Pair<Class, Boolean>> resultCache) {

        synchronized (resultCache) {
            // check again in synchronized block
            for (Pair<Class, Boolean> pair : resultCache) {
                if (pair.getFirst() == type) {
                    return pair.getSecond();
                }
            }

            // get the types superclasses and interfaces, and their superclasses and interfaces
            Set<Class> classesToCheck = new HashSet<Class>();
            JavaClassHelper.getSuper(type, classesToCheck);
            classesToCheck.add(type);

            // check type against each class
            boolean fits = false;
            for (Class clazz : classes) {
                if (classesToCheck.contains(clazz)) {
                    fits = true;
                    break;
                }
            }

            resultCache.add(new Pair<Class, Boolean>(type, fits));
            return fits;
        }
    }
}
