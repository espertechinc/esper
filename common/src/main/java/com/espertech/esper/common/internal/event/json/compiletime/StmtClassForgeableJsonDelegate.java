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
package com.espertech.esper.common.internal.event.json.compiletime;

import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenPackageScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenSymbolProviderEmpty;
import com.espertech.esper.common.internal.bytecodemodel.core.*;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.bytecodemodel.util.CodegenStackGenerator;
import com.espertech.esper.common.internal.compile.stage3.StmtClassForgeable;
import com.espertech.esper.common.internal.compile.stage3.StmtClassForgeableRSPFactoryProvider;
import com.espertech.esper.common.internal.compile.stage3.StmtClassForgeableType;
import com.espertech.esper.common.internal.event.json.parser.core.JsonDelegateBase;
import com.espertech.esper.common.internal.event.json.parser.core.JsonDelegateJsonGenericArray;
import com.espertech.esper.common.internal.event.json.parser.core.JsonDelegateJsonGenericObject;
import com.espertech.esper.common.internal.event.json.parser.core.JsonHandlerDelegator;
import com.espertech.esper.common.internal.event.json.parser.delegates.endvalue.JsonEndValueRefs;
import com.espertech.esper.common.internal.event.json.parser.forge.JsonDelegateRefs;
import com.espertech.esper.common.internal.event.json.parser.forge.JsonForgeDesc;

import java.util.*;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;
import static com.espertech.esper.common.internal.event.json.compiletime.StmtClassForgeableJsonUnderlying.DYNAMIC_PROP_FIELD;

public class StmtClassForgeableJsonDelegate implements StmtClassForgeable {

    private final CodegenClassType classType;
    private final String className;
    private final CodegenPackageScope packageScope;
    private final String underlyingClassName;
    private final StmtClassForgeableJsonDesc desc;

    public StmtClassForgeableJsonDelegate(CodegenClassType classType, String className, CodegenPackageScope packageScope, String underlyingClassName, StmtClassForgeableJsonDesc desc) {
        this.classType = classType;
        this.className = className;
        this.packageScope = packageScope;
        this.underlyingClassName = underlyingClassName;
        this.desc = desc;
    }

    public CodegenClass forge(boolean includeDebugSymbols, boolean fireAndForget) {
        CodegenClassScope classScope = new CodegenClassScope(includeDebugSymbols, packageScope, className);

        // make members
        List<CodegenTypedParam> members = new ArrayList<>(2);
        members.add(new CodegenTypedParam(underlyingClassName, "bean"));

        // make ctor
        CodegenTypedParam delegatorParam = new CodegenTypedParam(JsonHandlerDelegator.class, "delegator", false, false);
        CodegenTypedParam parentParam = new CodegenTypedParam(JsonDelegateBase.class, "parent", false, false);
        CodegenTypedParam beanParam = new CodegenTypedParam(underlyingClassName, "bean", false, false);
        List<CodegenTypedParam> ctorParams = Arrays.asList(delegatorParam, parentParam, beanParam);
        CodegenCtor ctor = new CodegenCtor(StmtClassForgeableRSPFactoryProvider.class, classScope, ctorParams);
        if (desc.getOptionalSupertype() != null) {
            ctor.getBlock().superCtor(ref("delegator"), ref("parent"), ref("bean"));
        } else {
            ctor.getBlock().superCtor(ref("delegator"), ref("parent"));
        }
        ctor.getBlock().assignRef(ref("this.bean"), ref("bean"));

        // startObject
        CodegenMethod startObjectMethod = CodegenMethod.makeParentNode(JsonDelegateBase.class, this.getClass(), CodegenSymbolProviderEmpty.INSTANCE, classScope)
            .addParam(String.class, "name");
        if (desc.getOptionalSupertype() != null) {
            startObjectMethod.getBlock()
                .declareVar(JsonDelegateBase.class, "delegate", exprDotMethod(ref("super"), "startObject", ref("name")))
                .ifCondition(notEqualsNull(ref("delegate"))).blockReturn(ref("delegate"));
        }
        for (String property : desc.getPropertiesThisType().keySet()) {
            JsonForgeDesc forge = desc.getForges().get(property);
            if (forge.getOptionalStartObjectForge() != null) {
                startObjectMethod.getBlock()
                    .ifCondition(exprDotMethod(ref("name"), "equals", constant(property)))
                    .blockReturn(forge.getOptionalStartObjectForge().newDelegate(JsonDelegateRefs.INSTANCE, startObjectMethod, classScope));
            }
        }
        CodegenExpression resultStartObject = desc.isDynamic() ? newInstance(JsonDelegateJsonGenericObject.class, JsonDelegateRefs.INSTANCE.getBaseHandler(), JsonDelegateRefs.INSTANCE.getThis()) : constantNull();
        startObjectMethod.getBlock().methodReturn(resultStartObject);

        // startArray
        CodegenMethod startArrayMethod = CodegenMethod.makeParentNode(JsonDelegateBase.class, this.getClass(), CodegenSymbolProviderEmpty.INSTANCE, classScope)
            .addParam(String.class, "name");
        if (desc.getOptionalSupertype() != null) {
            startArrayMethod.getBlock()
                .declareVar(JsonDelegateBase.class, "delegate", exprDotMethod(ref("super"), "startArray", ref("name")))
                .ifCondition(notEqualsNull(ref("delegate"))).blockReturn(ref("delegate"));
        }
        for (String property : desc.getPropertiesThisType().keySet()) {
            JsonForgeDesc forge = desc.getForges().get(property);
            if (forge.getOptionalStartArrayForge() != null) {
                startArrayMethod.getBlock()
                    .ifCondition(exprDotMethod(ref("name"), "equals", constant(property)))
                    .blockReturn(forge.getOptionalStartArrayForge().newDelegate(JsonDelegateRefs.INSTANCE, startArrayMethod, classScope));
            }
        }

        CodegenExpression resultStartArray = desc.isDynamic() ? newInstance(JsonDelegateJsonGenericArray.class, JsonDelegateRefs.INSTANCE.getBaseHandler(), JsonDelegateRefs.INSTANCE.getThis()) : constantNull();
        startArrayMethod.getBlock().methodReturn(resultStartArray);

        // endObjectValue
        CodegenMethod endObjectValueMethod = CodegenMethod.makeParentNode(boolean.class, this.getClass(), CodegenSymbolProviderEmpty.INSTANCE, classScope)
            .addParam(String.class, "name");
        if (desc.getOptionalSupertype() != null) {
            endObjectValueMethod.getBlock()
                .declareVar(boolean.class, "handled", exprDotMethod(ref("super"), "endObjectValue", ref("name")))
                .ifCondition(ref("handled")).blockReturn(constantTrue());
        }
        for (Map.Entry<String, Object> propertyPair : desc.getPropertiesThisType().entrySet()) {
            if (propertyPair.getValue() == null) { // no assignment for null values
                continue;
            }
            String fieldName = desc.getFieldDescriptorsInclSupertype().get(propertyPair.getKey()).getFieldName();
            JsonForgeDesc forge = desc.getForges().get(propertyPair.getKey());
            CodegenExpression value = forge.getEndValueForge().captureValue(JsonEndValueRefs.INSTANCE, endObjectValueMethod, classScope);
            endObjectValueMethod.getBlock()
                .ifCondition(exprDotMethod(ref("name"), "equals", constant(propertyPair.getKey())))
                .assignRef(ref("bean." + fieldName), value)
                .blockReturn(constantTrue());
        }
        if (desc.isDynamic()) {
            endObjectValueMethod.getBlock().exprDotMethod(ref("this"), "addGeneralJson", ref("bean." + DYNAMIC_PROP_FIELD), ref("name"));
        }
        endObjectValueMethod.getBlock().methodReturn(constantFalse());

        // make get-bean method
        CodegenMethod getResultMethod = CodegenMethod.makeParentNode(underlyingClassName, this.getClass(), CodegenSymbolProviderEmpty.INSTANCE, classScope);
        getResultMethod.getBlock().methodReturn(ref("bean"));

        // walk methods
        CodegenClassMethods methods = new CodegenClassMethods();
        CodegenStackGenerator.recursiveBuildStack(startObjectMethod, "startObject", methods);
        CodegenStackGenerator.recursiveBuildStack(startArrayMethod, "startArray", methods);
        CodegenStackGenerator.recursiveBuildStack(endObjectValueMethod, "endObjectValue", methods);
        CodegenStackGenerator.recursiveBuildStack(getResultMethod, "getResult", methods);

        CodegenClass clazz = new CodegenClass(classType, className, classScope, members, ctor, methods, Collections.emptyList());
        if (desc.getOptionalSupertype() == null) {
            clazz.getSupers().setClassExtended(JsonDelegateBase.class);
        } else {
            clazz.getSupers().setClassExtended(desc.getOptionalSupertype().getDetail().getDelegateClassName());
        }
        return clazz;
    }

    public String getClassName() {
        return className;
    }

    public StmtClassForgeableType getForgeableType() {
        return StmtClassForgeableType.JSONDELEGATE;
    }
}
