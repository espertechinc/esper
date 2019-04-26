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
package com.espertech.esper.common.internal.epl.index.base;

import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.context.aifactory.core.SAIFFInitializeSymbol;

import java.util.ArrayList;
import java.util.List;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

public abstract class EventTableFactoryFactoryForgeBase implements EventTableFactoryFactoryForge {
    protected final int indexedStreamNum;
    protected final Integer subqueryNum;
    protected final boolean isFireAndForget;

    protected abstract Class typeOf();

    protected abstract List<CodegenExpression> additionalParams(CodegenMethod method, SAIFFInitializeSymbol symbols, CodegenClassScope classScope);

    public EventTableFactoryFactoryForgeBase(int indexedStreamNum, Integer subqueryNum, boolean isFireAndForget) {
        this.indexedStreamNum = indexedStreamNum;
        this.subqueryNum = subqueryNum;
        this.isFireAndForget = isFireAndForget;
    }

    public CodegenExpression make(CodegenMethodScope parent, SAIFFInitializeSymbol symbols, CodegenClassScope classScope) {
        CodegenMethod method = parent.makeChild(EventTableFactoryFactory.class, this.getClass(), classScope);
        List<CodegenExpression> params = new ArrayList<>();
        params.add(constant(indexedStreamNum));
        params.add(constant(subqueryNum));
        params.add(constant(isFireAndForget));
        params.addAll(additionalParams(method, symbols, classScope));
        method.getBlock().methodReturn(newInstance(typeOf(), params.toArray(new CodegenExpression[params.size()])));
        return localMethod(method);
    }
}
