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
package com.espertech.esper.common.internal.context.aifactory.core;

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionField;
import com.espertech.esper.common.internal.epl.expression.core.ExprForge;
import com.espertech.esper.common.internal.epl.expression.core.ExprNode;
import com.espertech.esper.common.internal.epl.expression.core.ExprNodeUtilityCodegen;
import com.espertech.esper.common.internal.event.core.EventBeanManufacturer;
import com.espertech.esper.common.internal.event.core.EventBeanManufacturerForge;
import com.espertech.esper.common.internal.event.core.EventTypeUtility;
import com.espertech.esper.common.internal.util.CollectionUtil;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

public class SAIFFInitializeBuilder {
    private final Class originator;
    private final String refName;
    private final SAIFFInitializeSymbol symbols;
    private final CodegenClassScope classScope;

    private CodegenMethod method;
    private boolean closed;

    public SAIFFInitializeBuilder(Class returnType, Class originator, String refName, CodegenMethodScope parent, SAIFFInitializeSymbol symbols, CodegenClassScope classScope) {
        this.originator = originator;
        this.refName = refName;
        this.symbols = symbols;
        this.classScope = classScope;

        method = parent.makeChild(returnType, originator, classScope);
        method.getBlock().declareVar(returnType, refName, newInstance(returnType));
    }

    public SAIFFInitializeBuilder(String returnType, Class originator, String refName, CodegenMethodScope parent, SAIFFInitializeSymbol symbols, CodegenClassScope classScope) {
        this.originator = originator;
        this.refName = refName;
        this.symbols = symbols;
        this.classScope = classScope;

        method = parent.makeChild(returnType, originator, classScope);
        method.getBlock().declareVar(returnType, refName, CodegenExpressionBuilder.newInstance(returnType));
    }

    public SAIFFInitializeBuilder eventtypesMayNull(String name, EventType[] eventTypes) {
        return setValue(name, eventTypes == null ? constantNull() : EventTypeUtility.resolveTypeArrayCodegenMayNull(eventTypes, symbols.getAddInitSvc(method)));
    }

    public SAIFFInitializeBuilder eventtype(String name, EventType eventType) {
        return setValue(name, eventType == null ? constantNull() : EventTypeUtility.resolveTypeCodegen(eventType, symbols.getAddInitSvc(method)));
    }

    public SAIFFInitializeBuilder eventtypes(String name, EventType[] types) {
        return setValue(name, types == null ? constantNull() : EventTypeUtility.resolveTypeArrayCodegen(types, symbols.getAddInitSvc(method)));
    }

    public SAIFFInitializeBuilder exprnode(String name, ExprNode value) {
        return setValue(name, value == null ? constantNull() : ExprNodeUtilityCodegen.codegenEvaluator(value.getForge(), method, this.getClass(), classScope));
    }

    public SAIFFInitializeBuilder constant(String name, Object value) {
        if (value instanceof CodegenExpression) {
            throw new IllegalArgumentException("Expected a non-expression value, received " + value);
        }
        return setValue(name, value == null ? constantNull() : CodegenExpressionBuilder.constant(value));
    }

    public SAIFFInitializeBuilder method(String name, Function<CodegenMethod, CodegenExpression> expressionFunc) {
        CodegenExpression expression = expressionFunc.apply(method);
        return setValue(name, expression == null ? constantNull() : expression);
    }

    public SAIFFInitializeBuilder expression(String name, CodegenExpression expression) {
        return setValue(name, expression == null ? constantNull() : expression);
    }

    public SAIFFInitializeBuilder forges(String name, ExprForge[] evaluatorForges) {
        return setValue(name, evaluatorForges == null ? constantNull() : ExprNodeUtilityCodegen.codegenEvaluators(evaluatorForges, method, originator, classScope));
    }

    public SAIFFInitializeBuilder manufacturer(String name, EventBeanManufacturerForge forge) {
        if (forge == null) {
            return setValue(name, constantNull());
        }
        CodegenExpressionField manufacturer = classScope.addFieldUnshared(true, EventBeanManufacturer.class, forge.make(method, classScope));
        return setValue(name, manufacturer);
    }

    public SAIFFInitializeBuilder map(String name, Map<String, ?> values) {
        return setValue(name, buildMap(values));
    }

    private CodegenExpression buildMap(Map<String, ?> map) {
        if (map == null) {
            return constantNull();
        }
        if (map.isEmpty()) {
            return staticMethod(Collections.class, "emptyMap");
        }
        CodegenMethod child = method.makeChild(Map.class, originator, classScope);
        if (map.size() == 1) {
            Map.Entry<String, ?> single = map.entrySet().iterator().next();
            CodegenExpression value = buildMapValue(single.getValue(), child, classScope);
            child.getBlock().methodReturn(staticMethod(Collections.class, "singletonMap", CodegenExpressionBuilder.constant(single.getKey()), value));
        } else {
            child.getBlock().declareVar(Map.class, "map", newInstance(LinkedHashMap.class, CodegenExpressionBuilder.constant(CollectionUtil.capacityHashMap(map.size()))));
            for (Map.Entry<String, ?> entry : map.entrySet()) {
                CodegenExpression value = buildMapValue(entry.getValue(), child, classScope);
                child.getBlock().exprDotMethod(ref("map"), "put", CodegenExpressionBuilder.constant(entry.getKey()), value);
            }
            child.getBlock().methodReturn(ref("map"));
        }
        return localMethod(child);
    }

    private CodegenExpression buildMapValue(Object value, CodegenMethod method, CodegenClassScope classScope) {
        if (value instanceof Map) {
            return buildMap((Map) value);
        }
        if (value instanceof ExprNode) {
            return ExprNodeUtilityCodegen.codegenEvaluator(((ExprNode) value).getForge(), method, this.getClass(), classScope);
        }
        return CodegenExpressionBuilder.constant(value);
    }

    private SAIFFInitializeBuilder setValue(String name, CodegenExpression expression) {
        method.getBlock().exprDotMethod(ref(refName), "set" + getBeanCap(name), expression);
        return this;
    }

    private String getBeanCap(String name) {
        return name.substring(0, 1).toUpperCase(Locale.ENGLISH) + name.substring(1);
    }

    public CodegenExpression build() {
        if (closed) {
            throw new IllegalStateException("Builder already completed build");
        }
        closed = true;
        method.getBlock().methodReturn(ref(refName));
        return localMethod(method);
    }

    public CodegenMethod getMethod() {
        return method;
    }
}
