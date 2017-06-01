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
package com.espertech.esper.event.vaevent;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.EventPropertyGetter;
import com.espertech.esper.codegen.core.CodegenContext;
import com.espertech.esper.codegen.core.CodegenMember;
import com.espertech.esper.codegen.model.expression.CodegenExpression;
import com.espertech.esper.event.EventAdapterService;
import com.espertech.esper.event.EventPropertyGetterSPI;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.*;
import static com.espertech.esper.event.vaevent.VAERevisionEventPropertyGetterDeclaredGetVersioned.revisionImplementationNotProvided;

/**
 * A getter that works on POJO events residing within a Map as an event property.
 */
public class RevisionNestedPropertyGetter implements EventPropertyGetterSPI {
    private final EventPropertyGetterSPI revisionGetter;
    private final EventPropertyGetterSPI nestedGetter;
    private final EventAdapterService eventAdapterService;

    /**
     * Ctor.
     *
     * @param revisionGetter      getter for revision value
     * @param nestedGetter        getter to apply to revision value
     * @param eventAdapterService for handling object types
     */
    public RevisionNestedPropertyGetter(EventPropertyGetterSPI revisionGetter, EventPropertyGetterSPI nestedGetter, EventAdapterService eventAdapterService) {
        this.revisionGetter = revisionGetter;
        this.eventAdapterService = eventAdapterService;
        this.nestedGetter = nestedGetter;
    }

    public Object get(EventBean obj) {
        Object result = revisionGetter.get(obj);
        if (result == null) {
            return result;
        }

        // Object within the map
        EventBean theEvent = eventAdapterService.adapterForBean(result);
        return nestedGetter.get(theEvent);
    }

    private String getCodegen(CodegenContext context) {
        CodegenMember mgetter = context.makeAddMember(EventPropertyGetter.class, nestedGetter);
        CodegenMember msvc = context.makeAddMember(EventAdapterService.class, eventAdapterService);
        return context.addMethod(Object.class, EventBean.class, "obj", this.getClass())
                .declareVar(Object.class, "result", revisionGetter.codegenEventBeanGet(ref("obj"), context))
                .ifRefNullReturnNull("result")
                .declareVar(EventBean.class, "theEvent", exprDotMethod(ref(msvc.getMemberName()), "adapterForBean", ref("result")))
                .methodReturn(exprDotMethod(ref(mgetter.getMemberName()), "get", ref("theEvent")));
    }

    public boolean isExistsProperty(EventBean eventBean) {
        return true; // Property exists as the property is not dynamic (unchecked)
    }

    public Object getFragment(EventBean eventBean) {
        return null; // no fragments provided by revision events
    }

    public CodegenExpression codegenEventBeanGet(CodegenExpression beanExpression, CodegenContext context) {
        return localMethod(getCodegen(context), beanExpression);
    }

    public CodegenExpression codegenEventBeanExists(CodegenExpression beanExpression, CodegenContext context) {
        return constantTrue();
    }

    public CodegenExpression codegenEventBeanFragment(CodegenExpression beanExpression, CodegenContext context) {
        return constantNull();
    }

    public CodegenExpression codegenUnderlyingGet(CodegenExpression underlyingExpression, CodegenContext context) {
        throw revisionImplementationNotProvided();
    }

    public CodegenExpression codegenUnderlyingExists(CodegenExpression underlyingExpression, CodegenContext context) {
        throw revisionImplementationNotProvided();
    }

    public CodegenExpression codegenUnderlyingFragment(CodegenExpression underlyingExpression, CodegenContext context) {
        throw revisionImplementationNotProvided();
    }
}
