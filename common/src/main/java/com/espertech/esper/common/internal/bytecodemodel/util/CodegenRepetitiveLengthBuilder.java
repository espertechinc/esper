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

public class CodegenRepetitiveLengthBuilder extends CodegenRepetitiveBuilderBase {
    private final int length;
    private ConsumerByLength consumer;

    public CodegenRepetitiveLengthBuilder(int length, CodegenMethod methodNode, CodegenClassScope classScope, Class provider) {
        super(methodNode, classScope, provider);
        this.length = length;
    }

    public CodegenRepetitiveLengthBuilder addParam(EPTypeClass type, String name) {
        params.add(new CodegenNamedParam(type, name));
        return this;
    }

    public CodegenRepetitiveLengthBuilder setConsumer(ConsumerByLength consumer) {
        this.consumer = consumer;
        return this;
    }

    public void build() {
        int complexity = targetMethodComplexity(classScope);
        if (length < complexity) {
            for (int i = 0; i < length; i++) {
                consumer.accept(i, methodNode);
            }
            return;
        }

        int count = 0;
        while (count < length) {
            int remaining = length - count;
            int target = Math.min(remaining, complexity);
            CodegenMethod child = methodNode.makeChild(EPTypePremade.VOID.getEPType(), provider, classScope).addParam(params);
            methodNode.getBlock().localMethod(child, paramNames());

            for (int i = 0; i < target; i++) {
                consumer.accept(count, child);
                count++;
            }
        }
    }

    @FunctionalInterface
    public interface ConsumerByLength {
        void accept(int index, CodegenMethod leafMethod);
    }
}

