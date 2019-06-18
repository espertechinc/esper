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

import com.espertech.esper.common.client.json.minimaljson.JsonWriter;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenPackageScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenSymbolProviderEmpty;
import com.espertech.esper.common.internal.bytecodemodel.core.CodegenClass;
import com.espertech.esper.common.internal.bytecodemodel.core.CodegenClassMethods;
import com.espertech.esper.common.internal.bytecodemodel.core.CodegenClassType;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.bytecodemodel.util.CodegenStackGenerator;
import com.espertech.esper.common.internal.compile.stage3.StmtClassForgeable;
import com.espertech.esper.common.internal.compile.stage3.StmtClassForgeableType;
import com.espertech.esper.common.internal.event.json.parser.core.JsonDelegateBase;
import com.espertech.esper.common.internal.event.json.parser.core.JsonDelegateFactory;
import com.espertech.esper.common.internal.event.json.parser.core.JsonHandlerDelegator;
import com.espertech.esper.common.internal.event.json.parser.forge.JsonForgeDesc;
import com.espertech.esper.common.internal.event.json.write.JsonWriteForgeRefs;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;
import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.constant;

public class StmtClassForgeableJsonDelegateFactory implements StmtClassForgeable {

    private final CodegenClassType classType;
    private final String className;
    private final boolean makeWriteMethod;
    private final CodegenPackageScope packageScope;
    private final String delegateClassName;
    private final String underlyingClassName;
    private final Map<String, Object> properties;
    private final Map<String, JsonUnderlyingField> fieldDescriptors;
    private final Map<String, JsonForgeDesc> forges;

    public StmtClassForgeableJsonDelegateFactory(CodegenClassType classType, String className, boolean makeWriteMethod, CodegenPackageScope packageScope, String delegateClassName, String underlyingClassName, Map<String, Object> properties, Map<String, JsonUnderlyingField> fieldDescriptors, Map<String, JsonForgeDesc> forges) {
        this.classType = classType;
        this.className = className;
        this.packageScope = packageScope;
        this.delegateClassName = delegateClassName;
        this.underlyingClassName = underlyingClassName;
        this.properties = properties;
        this.fieldDescriptors = fieldDescriptors;
        this.forges = forges;
        this.makeWriteMethod = makeWriteMethod;
    }

    public CodegenClass forge(boolean includeDebugSymbols, boolean fireAndForget) {
        CodegenClassMethods methods = new CodegenClassMethods();
        CodegenClassScope classScope = new CodegenClassScope(includeDebugSymbols, packageScope, className);

        CodegenMethod makeMethod = CodegenMethod.makeParentNode(JsonDelegateBase.class, StmtClassForgeableJsonDelegateFactory.class, CodegenSymbolProviderEmpty.INSTANCE, classScope)
            .addParam(JsonHandlerDelegator.class, "delegator")
            .addParam(JsonDelegateBase.class, "parent");
        makeMethod.getBlock().methodReturn(newInstance(delegateClassName, ref("delegator"), ref("parent"), newInstance(underlyingClassName)));
        CodegenStackGenerator.recursiveBuildStack(makeMethod, "make", methods);

        // write-method (applicable for nested classes)
        CodegenMethod writeMethod = CodegenMethod.makeParentNode(void.class, this.getClass(), CodegenSymbolProviderEmpty.INSTANCE, classScope)
            .addParam(JsonWriter.class, "writer").addParam(Object.class, "underlying").addThrown(IOException.class);
        writeMethod.getBlock().staticMethod(className, "writeStatic", ref("writer"), ref("underlying"));
        CodegenStackGenerator.recursiveBuildStack(writeMethod, "write", methods);

        // write-static-method (applicable for nested classes)
        CodegenMethod writeStaticMethod = CodegenMethod.makeParentNode(void.class, this.getClass(), CodegenSymbolProviderEmpty.INSTANCE, classScope)
            .addParam(JsonWriter.class, "writer").addParam(Object.class, "underlying").addThrown(IOException.class);
        writeStaticMethod.setStatic(true);
        if (makeWriteMethod) {
            makeNativeWrite(writeStaticMethod, classScope);
        } else {
            writeStaticMethod.getBlock().methodThrowUnsupported(); // write method found on underlying class itself
        }
        CodegenStackGenerator.recursiveBuildStack(writeStaticMethod, "writeStatic", methods);

        CodegenClass clazz = new CodegenClass(classType, className, classScope, Collections.emptyList(), null, methods, Collections.emptyList());
        clazz.getSupers().addInterfaceImplemented(JsonDelegateFactory.class);
        return clazz;
    }

    public String getClassName() {
        return className;
    }

    public StmtClassForgeableType getForgeableType() {
        return StmtClassForgeableType.JSONDELEGATEFACTORY;
    }

    private void makeNativeWrite(CodegenMethod method, CodegenClassScope classScope) {
        method.getBlock()
            .declareVar(underlyingClassName, "und", cast(underlyingClassName, ref("underlying")))
            .ifRefNull("und")
            .exprDotMethod(ref("writer"), "writeLiteral", constant("null"))
            .blockReturnNoValue()
            .exprDotMethod(ref("writer"), "writeObjectOpen");
        boolean first = true;
        for (Map.Entry<String, Object> property : properties.entrySet()) {
            JsonUnderlyingField field = fieldDescriptors.get(property.getKey());
            JsonForgeDesc forge = forges.get(property.getKey());
            String fieldName = field.getFieldName();
            if (!first) {
                method.getBlock().exprDotMethod(ref("writer"), "writeObjectSeparator");
            }
            first = false;
            CodegenExpression write = forge.getWriteForge().codegenWrite(new JsonWriteForgeRefs(ref("writer"), ref("und." + fieldName), constant(property.getKey())), method, classScope);
            method.getBlock()
                .exprDotMethod(ref("writer"), "writeMemberName", constant(property.getKey()))
                .exprDotMethod(ref("writer"), "writeMemberSeparator")
                .expression(write);
        }
        method.getBlock()
            .exprDotMethod(ref("writer"), "writeObjectClose");
    }

}
