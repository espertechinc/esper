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
package com.espertech.esper.common.internal.bytecodemodel.util;

import com.espertech.esper.common.client.type.EPTypeClass;
import com.espertech.esper.common.client.type.EPTypePremade;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.core.CodegenNamedParam;

import java.util.Collection;
import java.util.Iterator;

public class CodegenRepetitiveValueBuilder<V> extends CodegenRepetitiveBuilderBase {
    private final Collection<V> values;
    private ConsumerByValue<V> consumer;

    public CodegenRepetitiveValueBuilder(Collection<V> values, CodegenMethod methodNode, CodegenClassScope classScope, Class provider) {
        super(methodNode, classScope, provider);
        this.values = values;
    }

    public CodegenRepetitiveValueBuilder<V> addParam(EPTypeClass type, String name) {
        params.add(new CodegenNamedParam(type, name));
        return this;
    }

    public CodegenRepetitiveValueBuilder<V> setConsumer(ConsumerByValue<V> consumer) {
        this.consumer = consumer;
        return this;
    }

    public void build() {
        int complexity = targetMethodComplexity(classScope);
        if (values.size() < complexity) {
            int index = 0;
            for (V value : values) {
                consumer.accept(value, index++, methodNode);
            }
            return;
        }

        int count = 0;
        Iterator<V> it = values.iterator();
        while (count < values.size()) {
            int remaining = values.size() - count;
            int target = Math.min(remaining, complexity);
            CodegenMethod child = methodNode.makeChild(EPTypePremade.VOID.getEPType(), provider, classScope).addParam(params);
            methodNode.getBlock().localMethod(child, paramNames());
            for (int i = 0; i < target; i++) {
                V value = it.next();
                consumer.accept(value, count, child);
                count++;
            }
        }
    }

    @FunctionalInterface
    public interface ConsumerByValue<V> {
        void accept(V value, int index, CodegenMethod leafMethod);
    }
}

