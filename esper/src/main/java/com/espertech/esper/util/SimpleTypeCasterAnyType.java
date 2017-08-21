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
package com.espertech.esper.util;

import com.espertech.esper.codegen.base.CodegenClassScope;
import com.espertech.esper.codegen.base.CodegenMember;
import com.espertech.esper.codegen.base.CodegenMethodScope;
import com.espertech.esper.codegen.model.expression.CodegenExpression;
import com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder;
import com.espertech.esper.collection.Pair;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.staticMethod;

/**
 * Cast implementation for non-numeric values that caches allowed casts assuming there is a small set of casts allowed.
 */
public class SimpleTypeCasterAnyType implements SimpleTypeCaster {
    private final Class typeToCastTo;
    private CopyOnWriteArraySet<Pair<Class, Boolean>> pairs = new CopyOnWriteArraySet<Pair<Class, Boolean>>();

    /**
     * Ctor.
     *
     * @param typeToCastTo is the target type
     */
    public SimpleTypeCasterAnyType(Class typeToCastTo) {
        this.typeToCastTo = typeToCastTo;
    }

    public boolean isNumericCast() {
        return false;
    }

    /**
     * NOTE: Code-generation-invoked method, method name and parameter order matters
     * @param object to cast
     * @param typeToCastTo target
     * @param pairs cache
     * @return null or object
     */
    public static Object simpleTypeCasterCast(Object object, Class typeToCastTo, CopyOnWriteArraySet<Pair<Class, Boolean>> pairs) {
        if (object.getClass() == typeToCastTo) {
            return object;
        }

        // check cache to see if this is cast-able
        for (Pair<Class, Boolean> pair : pairs) {
            if (pair.getFirst() == typeToCastTo) {
                if (!pair.getSecond()) {
                    return null;
                }
                return object;
            }
        }

        // Not found in cache, add to cache;
        synchronized (pairs) {
            // search cache once more
            for (Pair<Class, Boolean> pair : pairs) {
                if (pair.getFirst() == typeToCastTo) {
                    if (!pair.getSecond()) {
                        return null;
                    }
                    return object;
                }
            }

            // Determine if any of the super-types and interfaces that the object implements or extends
            // is the same as any of the target types
            Set<Class> classesToCheck = new HashSet<Class>();
            JavaClassHelper.getSuper(object.getClass(), classesToCheck);

            if (classesToCheck.contains(typeToCastTo)) {
                pairs.add(new Pair<Class, Boolean>(object.getClass(), true));
                return object;
            }
            pairs.add(new Pair<Class, Boolean>(object.getClass(), false));
            return null;
        }
    }

    public Object cast(Object object) {
        return simpleTypeCasterCast(object, typeToCastTo, pairs);
    }

    public CodegenExpression codegen(CodegenExpression input, Class inputType, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        if (JavaClassHelper.isSubclassOrImplementsInterface(inputType, typeToCastTo)) {
            return input;
        }
        CodegenMember target = codegenClassScope.makeAddMember(Class.class, typeToCastTo);
        CodegenMember cache = codegenClassScope.makeAddMember(CopyOnWriteArraySet.class, pairs);
        return CodegenExpressionBuilder.cast(typeToCastTo, staticMethod(SimpleTypeCasterAnyType.class, "simpleTypeCasterCast", input, CodegenExpressionBuilder.member(target.getMemberId()), CodegenExpressionBuilder.member(cache.getMemberId())));
    }
}
