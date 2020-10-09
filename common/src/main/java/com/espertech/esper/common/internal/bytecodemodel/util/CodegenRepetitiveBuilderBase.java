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

import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.core.CodegenNamedParam;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;

import java.util.ArrayList;
import java.util.List;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.ref;

public abstract class CodegenRepetitiveBuilderBase {
    protected final CodegenMethod methodNode;
    protected final CodegenClassScope classScope;
    protected final Class provider;
    protected final List<CodegenNamedParam> params = new ArrayList<>(2);

    public abstract void build();

    public CodegenRepetitiveBuilderBase(CodegenMethod methodNode, CodegenClassScope classScope, Class provider) {
        this.methodNode = methodNode;
        this.classScope = classScope;
        this.provider = provider;
    }

    protected static int targetMethodComplexity(CodegenClassScope classScope) {
        return Math.max(1, classScope.getPackageScope().getConfig().getInternalUseOnlyMaxMethodComplexity());
    }

    protected CodegenExpression[] paramNames() {
        CodegenExpression[] names = new CodegenExpression[params.size()];
        for (int i = 0; i < params.size(); i++) {
            names[i] = ref(params.get(i).getName());
        }
        return names;
    }
}

