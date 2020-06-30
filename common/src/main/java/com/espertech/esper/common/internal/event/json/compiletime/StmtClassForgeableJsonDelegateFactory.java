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

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.client.json.minimaljson.JsonWriter;
import com.espertech.esper.common.client.type.EPTypeClass;
import com.espertech.esper.common.client.type.EPTypeNull;
import com.espertech.esper.common.client.type.EPTypePremade;
import com.espertech.esper.common.internal.bytecodemodel.base.*;
import com.espertech.esper.common.internal.bytecodemodel.core.CodegenClass;
import com.espertech.esper.common.internal.bytecodemodel.core.CodegenClassMethods;
import com.espertech.esper.common.internal.bytecodemodel.core.CodegenClassType;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.bytecodemodel.model.statement.CodegenStatementSwitch;
import com.espertech.esper.common.internal.bytecodemodel.util.CodegenStackGenerator;
import com.espertech.esper.common.internal.compile.stage3.StmtClassForgeable;
import com.espertech.esper.common.internal.compile.stage3.StmtClassForgeableType;
import com.espertech.esper.common.internal.event.core.TypeBeanOrUnderlying;
import com.espertech.esper.common.internal.event.json.core.JsonEventObjectBase;
import com.espertech.esper.common.internal.event.json.core.JsonEventType;
import com.espertech.esper.common.internal.event.json.parser.core.JsonDelegateBase;
import com.espertech.esper.common.internal.event.json.parser.core.JsonDelegateFactory;
import com.espertech.esper.common.internal.event.json.parser.core.JsonHandlerDelegator;
import com.espertech.esper.common.internal.event.json.parser.forge.JsonForgeDesc;
import com.espertech.esper.common.internal.event.json.write.JsonWriteForgeRefs;
import com.espertech.esper.common.internal.util.JavaClassHelper;

import java.util.Collections;
import java.util.Map;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;
import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionRelational.CodegenRelational.LT;
import static com.espertech.esper.common.internal.event.json.compiletime.StmtClassForgeableJsonUtil.getCasesNumberNtoM;
import static com.espertech.esper.common.internal.event.json.compiletime.StmtClassForgeableJsonUtil.makeNoSuchElementDefault;

public class StmtClassForgeableJsonDelegateFactory implements StmtClassForgeable {

    private final CodegenClassType classType;
    private final String className;
    private final boolean makeWriteMethod;
    private final CodegenPackageScope packageScope;
    private final String delegateClassName;
    private final String underlyingClassName;
    private final StmtClassForgeableJsonDesc desc;

    public StmtClassForgeableJsonDelegateFactory(CodegenClassType classType, String className, boolean makeWriteMethod, CodegenPackageScope packageScope, String delegateClassName, String underlyingClassName, StmtClassForgeableJsonDesc desc) {
        this.classType = classType;
        this.className = className;
        this.makeWriteMethod = makeWriteMethod;
        this.packageScope = packageScope;
        this.delegateClassName = delegateClassName;
        this.underlyingClassName = underlyingClassName;
        this.desc = desc;
    }

    public CodegenClass forge(boolean includeDebugSymbols, boolean fireAndForget) {
        CodegenClassMethods methods = new CodegenClassMethods();
        CodegenClassScope classScope = new CodegenClassScope(includeDebugSymbols, packageScope, className);

        CodegenMethod makeMethod = CodegenMethod.makeParentNode(JsonDelegateBase.EPTYPE, StmtClassForgeableJsonDelegateFactory.class, CodegenSymbolProviderEmpty.INSTANCE, classScope)
            .addParam(JsonHandlerDelegator.EPTYPE, "delegator")
            .addParam(JsonDelegateBase.EPTYPE, "parent");
        makeMethod.getBlock().methodReturn(newInstance(delegateClassName, ref("delegator"), ref("parent"), newInstance(underlyingClassName)));
        CodegenStackGenerator.recursiveBuildStack(makeMethod, "make", methods);

        // write-method (applicable for nested classes)
        CodegenMethod writeMethod = CodegenMethod.makeParentNode(EPTypePremade.VOID.getEPType(), this.getClass(), CodegenSymbolProviderEmpty.INSTANCE, classScope)
            .addParam(JsonWriter.EPTYPE, "writer").addParam(EPTypePremade.OBJECT.getEPType(), "underlying").addThrown(EPTypePremade.IOEXCEPTION.getEPType());
        writeMethod.getBlock().staticMethod(className, "writeStatic", ref("writer"), ref("underlying"));
        CodegenStackGenerator.recursiveBuildStack(writeMethod, "write", methods);

        // write-static-method (applicable for nested classes)
        CodegenMethod writeStaticMethod = CodegenMethod.makeParentNode(EPTypePremade.VOID.getEPType(), this.getClass(), CodegenSymbolProviderEmpty.INSTANCE, classScope)
            .addParam(JsonWriter.EPTYPE, "writer").addParam(EPTypePremade.OBJECT.getEPType(), "underlying").addThrown(EPTypePremade.IOEXCEPTION.getEPType());
        writeStaticMethod.setStatic(true);
        if (makeWriteMethod) {
            makeNativeWrite(writeStaticMethod, classScope);
        } else {
            writeStaticMethod.getBlock().methodThrowUnsupported(); // write method found on underlying class itself
        }
        CodegenStackGenerator.recursiveBuildStack(writeStaticMethod, "writeStatic", methods);

        // copy-method
        CodegenMethod copyMethod = CodegenMethod.makeParentNode(EPTypePremade.OBJECT.getEPType(), this.getClass(), CodegenSymbolProviderEmpty.INSTANCE, classScope)
            .addParam(EPTypePremade.OBJECT.getEPType(), "und");
        makeCopy(copyMethod, classScope);
        CodegenStackGenerator.recursiveBuildStack(copyMethod, "copy", methods);

        // get-value-method
        CodegenMethod getValueMethod = CodegenMethod.makeParentNode(EPTypePremade.OBJECT.getEPType(), this.getClass(), CodegenSymbolProviderEmpty.INSTANCE, classScope)
            .addParam(EPTypePremade.INTEGERPRIMITIVE.getEPType(), "num").addParam(EPTypePremade.OBJECT.getEPType(), "und");
        makeGetValue(getValueMethod, classScope);
        CodegenStackGenerator.recursiveBuildStack(getValueMethod, "getValue", methods);

        // set-value-method
        CodegenMethod setValueMethod = CodegenMethod.makeParentNode(EPTypePremade.VOID.getEPType(), this.getClass(), CodegenSymbolProviderEmpty.INSTANCE, classScope)
            .addParam(EPTypePremade.INTEGERPRIMITIVE.getEPType(), "num").addParam(EPTypePremade.OBJECT.getEPType(), "value").addParam(EPTypePremade.OBJECT.getEPType(), "und");
        makeSetValue(setValueMethod, classScope);
        CodegenStackGenerator.recursiveBuildStack(setValueMethod, "setValue", methods);

        // newUnderlying-method
        CodegenMethod newUnderlyingMethod = CodegenMethod.makeParentNode(EPTypePremade.OBJECT.getEPType(), this.getClass(), CodegenSymbolProviderEmpty.INSTANCE, classScope);
        makeNewUnderlyingMethod(newUnderlyingMethod);
        CodegenStackGenerator.recursiveBuildStack(newUnderlyingMethod, "newUnderlying", methods);

        CodegenClass clazz = new CodegenClass(classType, className, classScope, Collections.emptyList(), null, methods, Collections.emptyList());
        clazz.getSupers().addInterfaceImplemented(JsonDelegateFactory.EPTYPE);
        return clazz;
    }

    private void makeNewUnderlyingMethod(CodegenMethod method) {
        // we know this underlying class has a default constructor otherwise it is not json and deep-class eligible
        method.getBlock().methodReturn(newInstance(underlyingClassName));
    }

    private void makeGetValue(CodegenMethod method, CodegenClassScope classScope) {
        if (desc.getNumFieldsSupertype() > 0) {
            method.getBlock()
                .ifCondition(relational(ref("num"), LT, constant(desc.getNumFieldsSupertype())))
                .blockReturn(exprDotMethod(cast(JsonEventObjectBase.EPTYPE, ref("und")), "getNativeValue", ref("num")));
        }

        method.getBlock()
            .declareVar(underlyingClassName, "src", cast(underlyingClassName, ref("und")));
        CodegenExpression[] cases = getCasesNumberNtoM(desc);
        CodegenStatementSwitch switchStmt = method.getBlock().switchBlockExpressions(ref("num"), cases, true, false);
        makeNoSuchElementDefault(switchStmt, ref("num"));
        int index = 0;
        for (Map.Entry<String, Object> property : desc.getPropertiesThisType().entrySet()) {
            JsonUnderlyingField field = desc.getFieldDescriptorsInclSupertype().get(property.getKey());
            switchStmt.getBlocks()[index].blockReturn(ref("src." + field.getFieldName()));
            index++;
        }
    }

    private void makeCopy(CodegenMethod method, CodegenClassScope classScope) {
        method.getBlock()
            .declareVar(underlyingClassName, "copy", newInstance(underlyingClassName))
            .declareVar(underlyingClassName, "src", cast(underlyingClassName, ref("und")));
        for (Map.Entry<String, JsonUnderlyingField> field : desc.getFieldDescriptorsInclSupertype().entrySet()) {
            String fieldName = field.getValue().getFieldName();
            EPTypeClass fieldType = field.getValue().getPropertyType();
            CodegenExpression sourceField = ref("src." + fieldName);
            CodegenExpression rhs;
            if (fieldType.getType().isArray()) {
                CodegenMethod arrayCopy = method.makeChild(fieldType, this.getClass(), classScope).addParam(fieldType, "src");
                rhs = localMethod(arrayCopy, sourceField);
                arrayCopy.getBlock()
                    .ifRefNullReturnNull("src")
                    .declareVar(fieldType, "copy", newArrayByLength(JavaClassHelper.getArrayComponentType(fieldType), arrayLength(ref("src"))))
                    .staticMethod(System.class, "arraycopy", ref("src"), constant(0), ref("copy"), constant(0), constant(0))
                    .methodReturn(ref("copy"));
            } else if (fieldType.getType() == Map.class) {
                CodegenMethod mapCopy = method.makeChild(EPTypePremade.MAP.getEPType(), this.getClass(), classScope).addParam(fieldType, "src");
                rhs = localMethod(mapCopy, sourceField);
                mapCopy.getBlock()
                    .ifRefNullReturnNull("src")
                    .methodReturn(newInstance(EPTypePremade.HASHMAP.getEPType(), ref("src")));
            } else {
                rhs = sourceField;
            }
            method.getBlock().assignRef(ref("copy." + fieldName), rhs);
        }
        method.getBlock().methodReturn(ref("copy"));
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
        for (Map.Entry<String, Object> property : desc.getPropertiesThisType().entrySet()) {
            JsonUnderlyingField field = desc.getFieldDescriptorsInclSupertype().get(property.getKey());
            JsonForgeDesc forge = desc.getForges().get(property.getKey());
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

    private void makeSetValue(CodegenMethod method, CodegenClassScope classScope) {
        method.getBlock()
            .declareVar(underlyingClassName, "bean", cast(underlyingClassName, ref("und")));

        CodegenExpression[] cases = getCasesNumberNtoM(desc);
        CodegenStatementSwitch switchStmt = method.getBlock().switchBlockExpressions(ref("num"), cases, false, false);
        makeNoSuchElementDefault(switchStmt, ref("num"));
        CodegenBlock[] blocks = switchStmt.getBlocks();

        int index = 0;
        for (Map.Entry<String, Object> property : desc.getPropertiesThisType().entrySet()) {
            JsonUnderlyingField field = desc.getFieldDescriptorsInclSupertype().get(property.getKey());
            String fieldName = "bean." + field.getFieldName();

            Object type = desc.getPropertiesThisType().get(property.getKey());
            if (type == EPTypeNull.INSTANCE) {
                // no action
            } else if (type instanceof EPTypeClass) {
                EPTypeClass classType = (EPTypeClass) type;
                if (classType.getType().isPrimitive()) {
                    blocks[index]
                        .ifRefNotNull("value")
                        .assignRef(fieldName, cast(JavaClassHelper.getBoxedType(classType), ref("value")));
                } else {
                    blocks[index].assignRef(fieldName, cast(classType, ref("value")));
                }
            } else if (type instanceof TypeBeanOrUnderlying) {
                EventType eventType = ((TypeBeanOrUnderlying) type).getEventType();
                if (eventType instanceof JsonEventType) {
                    JsonEventType jsonEventType = (JsonEventType) eventType;
                    CodegenExpression castAsBean = castUnderlying(jsonEventType.getDetail().getUnderlyingClassName(), cast(EventBean.EPTYPE, ref("value")));
                    CodegenExpression castUnd = cast(jsonEventType.getDetail().getUnderlyingClassName(), ref("value"));
                    blocks[index].assignRef(fieldName, conditional(instanceOf(ref("value"), EventBean.EPTYPE), castAsBean, castUnd));
                } else {
                    CodegenExpression castAsBean = castUnderlying(EPTypePremade.MAP.getEPType(), cast(EventBean.EPTYPE, ref("value")));
                    CodegenExpression castUnd = cast(EPTypePremade.MAP.getEPType(), ref("value"));
                    blocks[index].assignRef(fieldName, conditional(instanceOf(ref("value"), EventBean.EPTYPE), castAsBean, castUnd));
                }
            } else if (type instanceof TypeBeanOrUnderlying[]) {
                TypeBeanOrUnderlying[] typeDef = (TypeBeanOrUnderlying[]) type;
                EventType eventType = typeDef[0].getEventType();
                EPTypeClass arrayType = JavaClassHelper.getArrayType(eventType.getUnderlyingEPType());
                blocks[index]
                    .ifRefNull("value").assignRef(fieldName, constantNull()).blockReturnNoValue()
                    .ifCondition(instanceOf(ref("value"), arrayType)).assignRef(fieldName, cast(arrayType, ref("value"))).blockReturnNoValue()
                    .declareVar(EventBean.EPTYPEARRAY, "events", cast(EventBean.EPTYPEARRAY, ref("value")))
                    .declareVar(arrayType, "array", newArrayByLength(eventType.getUnderlyingEPType(), arrayLength(ref("events"))))
                    .forLoopIntSimple("i", arrayLength(ref("events")))
                    .assignArrayElement("array", ref("i"), castUnderlying(eventType.getUnderlyingEPType(), arrayAtIndex(ref("events"), ref("i"))))
                    .blockEnd()
                    .assignRef(fieldName, ref("array"));
            } else {
                throw new UnsupportedOperationException("Unrecognized type " + type);
            }
            index++;
        }
    }
}
