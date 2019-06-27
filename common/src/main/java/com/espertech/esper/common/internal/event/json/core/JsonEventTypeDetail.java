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
package com.espertech.esper.common.internal.event.json.core;

import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.event.json.compiletime.JsonUnderlyingField;
import com.espertech.esper.common.internal.util.CollectionUtil;

import java.util.HashMap;
import java.util.Map;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

public class JsonEventTypeDetail {
    private String underlyingClassName;
    private Class optionalUnderlyingProvided;
    private String delegateClassName;
    private String delegateFactoryClassName;
    private String serdeClassName;
    private Map<String, JsonUnderlyingField> fieldDescriptors;
    private boolean dynamic;
    private int numFieldsSupertype;

    public JsonEventTypeDetail() {
    }

    public JsonEventTypeDetail(String underlyingClassName, Class optionalUnderlyingProvided, String delegateClassName, String delegateFactoryClassName, String serdeClassName, Map<String, JsonUnderlyingField> fieldDescriptors, boolean dynamic, int numFieldsSupertype) {
        this.underlyingClassName = underlyingClassName;
        this.optionalUnderlyingProvided = optionalUnderlyingProvided;
        this.delegateClassName = delegateClassName;
        this.delegateFactoryClassName = delegateFactoryClassName;
        this.serdeClassName = serdeClassName;
        this.fieldDescriptors = fieldDescriptors;
        this.dynamic = dynamic;
        this.numFieldsSupertype = numFieldsSupertype;
    }

    public boolean isDynamic() {
        return dynamic;
    }

    public String getUnderlyingClassName() {
        return underlyingClassName;
    }

    public String getDelegateClassName() {
        return delegateClassName;
    }

    public String getDelegateFactoryClassName() {
        return delegateFactoryClassName;
    }

    public void setOptionalUnderlyingProvided(Class optionalUnderlyingProvided) {
        this.optionalUnderlyingProvided = optionalUnderlyingProvided;
    }

    public void setDelegateClassName(String delegateClassName) {
        this.delegateClassName = delegateClassName;
    }

    public String getSerdeClassName() {
        return serdeClassName;
    }

    public Map<String, JsonUnderlyingField> getFieldDescriptors() {
        return fieldDescriptors;
    }

    public void setUnderlyingClassName(String underlyingClassName) {
        this.underlyingClassName = underlyingClassName;
    }

    public void setDelegateFactoryClassName(String delegateFactoryClassName) {
        this.delegateFactoryClassName = delegateFactoryClassName;
    }

    public void setSerdeClassName(String serdeClassName) {
        this.serdeClassName = serdeClassName;
    }

    public void setFieldDescriptors(Map<String, JsonUnderlyingField> fieldDescriptors) {
        this.fieldDescriptors = fieldDescriptors;
    }

    public void setDynamic(boolean dynamic) {
        this.dynamic = dynamic;
    }

    public void setNumFieldsSupertype(int numFieldsSupertype) {
        this.numFieldsSupertype = numFieldsSupertype;
    }

    public Class getOptionalUnderlyingProvided() {
        return optionalUnderlyingProvided;
    }

    public CodegenExpression toExpression(CodegenMethodScope parent, CodegenClassScope classScope) {
        CodegenMethod method = parent.makeChild(JsonEventTypeDetail.class, JsonEventTypeDetail.class, classScope);
        method.getBlock()
            .declareVar(JsonEventTypeDetail.class, "detail", newInstance(JsonEventTypeDetail.class))
            .exprDotMethod(ref("detail"), "setUnderlyingClassName", constant(underlyingClassName))
            .exprDotMethod(ref("detail"), "setOptionalUnderlyingProvided", constant(optionalUnderlyingProvided))
            .exprDotMethod(ref("detail"), "setDelegateClassName", constant(delegateClassName))
            .exprDotMethod(ref("detail"), "setDelegateFactoryClassName", constant(delegateFactoryClassName))
            .exprDotMethod(ref("detail"), "setSerdeClassName", constant(serdeClassName))
            .exprDotMethod(ref("detail"), "setFieldDescriptors", localMethod(makeFieldDescCodegen(method, classScope)))
            .exprDotMethod(ref("detail"), "setDynamic", constant(dynamic))
            .exprDotMethod(ref("detail"), "setNumFieldsSupertype", constant(numFieldsSupertype))
            .methodReturn(ref("detail"));
        return localMethod(method);
    }

    private CodegenMethod makeFieldDescCodegen(CodegenMethodScope parent, CodegenClassScope classScope) {
        CodegenMethod method = parent.makeChild(Map.class, JsonEventTypeDetail.class, classScope);

        method.getBlock().declareVar(Map.class, "fields", newInstance(HashMap.class, constant(CollectionUtil.capacityHashMap(fieldDescriptors.size()))));
        for (Map.Entry<String, JsonUnderlyingField> entry : fieldDescriptors.entrySet()) {
            method.getBlock().exprDotMethod(ref("fields"), "put", constant(entry.getKey()), entry.getValue().toExpression());
        }
        method.getBlock().methodReturn(ref("fields"));
        return method;
    }
}
