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
package com.espertech.esper.common.internal.epl.contained;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionNewAnonymousClass;
import com.espertech.esper.common.internal.context.aifactory.core.SAIFFInitializeSymbol;
import com.espertech.esper.common.internal.event.core.EventPropertyFragmentGetter;
import com.espertech.esper.common.internal.event.core.EventPropertyGetterSPI;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

public class ContainedEventEvalGetterForge implements ContainedEventEvalForge {

    private final EventPropertyGetterSPI getter;

    public ContainedEventEvalGetterForge(EventPropertyGetterSPI getter) {
        this.getter = getter;
    }

    public CodegenExpression make(CodegenMethodScope parent, SAIFFInitializeSymbol symbols, CodegenClassScope classScope) {
        CodegenMethod method = parent.makeChild(ContainedEventEvalGetter.class, this.getClass(), classScope);

        CodegenExpressionNewAnonymousClass anonymousClass = newAnonymousClass(method.getBlock(), EventPropertyFragmentGetter.class);
        CodegenMethod getFragment = CodegenMethod.makeParentNode(Object.class, this.getClass(), classScope).addParam(EventBean.class, "event");
        anonymousClass.addMethod("getFragment", getFragment);
        getFragment.getBlock().methodReturn(getter.eventBeanFragmentCodegen(ref("event"), getFragment, classScope));

        method.getBlock().methodReturn(newInstance(ContainedEventEvalGetter.class, anonymousClass));
        return localMethod(method);
    }
}
