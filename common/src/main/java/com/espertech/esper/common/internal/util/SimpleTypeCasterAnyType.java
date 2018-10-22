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
package com.espertech.esper.common.internal.util;

import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionField;
import com.espertech.esper.common.internal.collection.Pair;

import java.util.concurrent.CopyOnWriteArraySet;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

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
     *
     * @param object       to cast
     * @param typeToCastTo target
     * @param pairs        cache
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
            boolean passed = JavaClassHelper.isSubclassOrImplementsInterface(object.getClass(), typeToCastTo);

            if (passed) {
                pairs.add(new Pair<>(object.getClass(), true));
                return object;
            }
            pairs.add(new Pair<>(object.getClass(), false));
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
        CodegenExpressionField cache = codegenClassScope.addFieldUnshared(true, CopyOnWriteArraySet.class, newInstance(CopyOnWriteArraySet.class));
        return CodegenExpressionBuilder.cast(typeToCastTo, staticMethod(SimpleTypeCasterAnyType.class, "simpleTypeCasterCast", input,
                constant(typeToCastTo), cache));
    }
}
