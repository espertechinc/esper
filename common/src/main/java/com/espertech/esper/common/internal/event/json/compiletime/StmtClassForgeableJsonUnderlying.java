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
import com.espertech.esper.common.internal.bytecodemodel.base.*;
import com.espertech.esper.common.internal.bytecodemodel.core.*;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionRef;
import com.espertech.esper.common.internal.bytecodemodel.model.statement.CodegenStatementSwitch;
import com.espertech.esper.common.internal.bytecodemodel.util.CodegenStackGenerator;
import com.espertech.esper.common.internal.compile.stage3.StmtClassForgeable;
import com.espertech.esper.common.internal.compile.stage3.StmtClassForgeableType;
import com.espertech.esper.common.internal.event.core.TypeBeanOrUnderlying;
import com.espertech.esper.common.internal.event.json.core.JsonEventType;
import com.espertech.esper.common.internal.event.json.core.JsonEventObjectBase;
import com.espertech.esper.common.internal.event.json.parser.forge.JsonForgeDesc;
import com.espertech.esper.common.internal.event.json.write.JsonWriteForgeRefs;
import com.espertech.esper.common.internal.util.JavaClassHelper;

import java.io.IOException;
import java.util.*;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;
import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionRelational.CodegenRelational.GT;
import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionRelational.CodegenRelational.LT;

public class StmtClassForgeableJsonUnderlying implements StmtClassForgeable {

    public final static String DYNAMIC_PROP_FIELD = "__dyn";

    private final String className;
    private final CodegenPackageScope packageScope;
    private final Map<String, Object> propertiesThisType;
    private final Map<String, JsonUnderlyingField> fieldDescriptorsInclSupertype;
    private final boolean dynamic;
    private final int numFieldsSupertype;
    private final JsonEventType optionalSupertype;
    private final Map<String, JsonForgeDesc> forges;

    public StmtClassForgeableJsonUnderlying(String className, CodegenPackageScope packageScope, Map<String, Object> propertiesThisType, Map<String, JsonUnderlyingField> fieldDescriptorsInclSupertype, boolean dynamic, int numFieldsSupertype, JsonEventType optionalSupertype, Map<String, JsonForgeDesc> forges) {
        this.className = className;
        this.packageScope = packageScope;
        this.propertiesThisType = propertiesThisType;
        this.fieldDescriptorsInclSupertype = fieldDescriptorsInclSupertype;
        this.dynamic = dynamic;
        this.numFieldsSupertype = numFieldsSupertype;
        this.optionalSupertype = optionalSupertype;
        this.forges = forges;
    }

    public CodegenClass forge(boolean includeDebugSymbols, boolean fireAndForget) {
        CodegenCtor ctor = new CodegenCtor(StmtClassForgeableJsonUnderlying.class, includeDebugSymbols, Collections.emptyList());
        if (needDynamic()) {
            ctor.getBlock().assignRef(DYNAMIC_PROP_FIELD, newInstance(LinkedHashMap.class));
        }

        CodegenClassMethods methods = new CodegenClassMethods();
        CodegenClassScope classScope = new CodegenClassScope(includeDebugSymbols, packageScope, className);

        List<CodegenTypedParam> explicitMembers = new ArrayList<>(propertiesThisType.size());
        if (needDynamic()) {
            explicitMembers.add(new CodegenTypedParam(Map.class, DYNAMIC_PROP_FIELD, false, true));
        }
        // add members
        for (Map.Entry<String, Object> property : propertiesThisType.entrySet()) {
            JsonUnderlyingField field = fieldDescriptorsInclSupertype.get(property.getKey());
            explicitMembers.add(new CodegenTypedParam(field.getPropertyType(), field.getFieldName(), false, true));
        }

        // getNativeSize
        CodegenMethod getNativeSizeMethod = CodegenMethod.makeParentNode(int.class, this.getClass(), CodegenSymbolProviderEmpty.INSTANCE, classScope);
        getNativeSizeMethod.getBlock().methodReturn(constant(propertiesThisType.size() + numFieldsSupertype));
        CodegenStackGenerator.recursiveBuildStack(getNativeSizeMethod, "getNativeSize", methods);

        // getNativeEntry
        CodegenMethod getNativeEntryMethod = CodegenMethod.makeParentNode(Map.Entry.class, this.getClass(), CodegenSymbolProviderEmpty.INSTANCE, classScope)
            .addParam(int.class, "num");
        makeGetNativeEntry(getNativeEntryMethod, classScope);
        CodegenStackGenerator.recursiveBuildStack(getNativeEntryMethod, "getNativeEntry", methods);

        // getNativeEntry
        CodegenMethod getNativeKey = CodegenMethod.makeParentNode(String.class, this.getClass(), CodegenSymbolProviderEmpty.INSTANCE, classScope)
            .addParam(int.class, "num");
        makeGetNativeKey(getNativeKey);
        CodegenStackGenerator.recursiveBuildStack(getNativeKey, "getNativeKey", methods);

        // nativeContainsKey
        CodegenMethod nativeContainsKeyMethod = CodegenMethod.makeParentNode(boolean.class, this.getClass(), CodegenSymbolProviderEmpty.INSTANCE, classScope)
            .addParam(Object.class, "name");
        makeNativeContainsKey(nativeContainsKeyMethod);
        CodegenStackGenerator.recursiveBuildStack(nativeContainsKeyMethod, "nativeContainsKey", methods);

        // getNativeValue
        CodegenMethod getNativeValueMethod = CodegenMethod.makeParentNode(Object.class, this.getClass(), CodegenSymbolProviderEmpty.INSTANCE, classScope)
            .addParam(int.class, "num");
        makeGetNativeValueMethod(getNativeValueMethod, classScope);
        CodegenStackGenerator.recursiveBuildStack(getNativeValueMethod, "getNativeValue", methods);

        // getNativeNum
        CodegenMethod getNativeNumMethod = CodegenMethod.makeParentNode(int.class, this.getClass(), CodegenSymbolProviderEmpty.INSTANCE, classScope)
            .addParam(String.class, "name");
        makeGetNativeNum(getNativeNumMethod, classScope);
        CodegenStackGenerator.recursiveBuildStack(getNativeNumMethod, "getNativeNum", methods);

        // setNativeValue
        CodegenMethod setNativeValueMethod = CodegenMethod.makeParentNode(void.class, this.getClass(), CodegenSymbolProviderEmpty.INSTANCE, classScope)
            .addParam(int.class, "num").addParam(Object.class, "value");
        makeSetNativeValue(setNativeValueMethod, classScope);
        CodegenStackGenerator.recursiveBuildStack(setNativeValueMethod, "setNativeValue", methods);

        // nativeWrite
        CodegenMethod nativeWriteMethod = CodegenMethod.makeParentNode(void.class, this.getClass(), CodegenSymbolProviderEmpty.INSTANCE, classScope)
            .addParam(JsonWriter.class, "writer").addThrown(IOException.class);
        makeNativeWrite(nativeWriteMethod, classScope);
        CodegenStackGenerator.recursiveBuildStack(nativeWriteMethod, "nativeWrite", methods);

        if (!parentDynamic()) {
            // addJsonValue
            CodegenMethod addJsonValueMethod = CodegenMethod.makeParentNode(void.class, this.getClass(), CodegenSymbolProviderEmpty.INSTANCE, classScope)
                .addParam(String.class, "name").addParam(Object.class, "value");
            if (needDynamic()) {
                addJsonValueMethod.getBlock().exprDotMethod(ref(DYNAMIC_PROP_FIELD), "put", ref("name"), ref("value"));
            }
            CodegenStackGenerator.recursiveBuildStack(addJsonValueMethod, "addJsonValue", methods);

            // getJsonValues
            CodegenMethod getJsonValuesMethod = CodegenMethod.makeParentNode(Map.class, this.getClass(), CodegenSymbolProviderEmpty.INSTANCE, classScope);
            getJsonValuesMethod.getBlock().methodReturn(dynamic ? ref(DYNAMIC_PROP_FIELD) : publicConstValue(Collections.class, "EMPTY_MAP"));
            CodegenStackGenerator.recursiveBuildStack(getJsonValuesMethod, "getJsonValues", methods);
        }

        CodegenClass clazz = new CodegenClass(CodegenClassType.JSONEVENT, className, classScope, explicitMembers, ctor, methods, Collections.emptyList());
        if (optionalSupertype == null) {
            clazz.getSupers().setClassExtended(JsonEventObjectBase.class);
        } else {
            clazz.getSupers().setClassExtended(optionalSupertype.getUnderlyingType());
        }
        return clazz;
    }

    public String getClassName() {
        return className;
    }

    public StmtClassForgeableType getForgeableType() {
        return StmtClassForgeableType.JSONEVENT;
    }

    private void makeNativeWrite(CodegenMethod method, CodegenClassScope classScope) {
        boolean first = true;
        if (optionalSupertype != null && !optionalSupertype.getTypes().isEmpty()) {
            method.getBlock().exprDotMethod(ref("super"), "nativeWrite", ref("writer"));
            first = false;
        }
        for (Map.Entry<String, Object> property : propertiesThisType.entrySet()) {
            JsonUnderlyingField field = fieldDescriptorsInclSupertype.get(property.getKey());
            JsonForgeDesc forge = forges.get(property.getKey());
            String fieldName = field.getFieldName();
            if (!first) {
                method.getBlock().exprDotMethod(ref("writer"), "writeObjectSeparator");
            }
            first = false;
            CodegenExpression write = forge.getWriteForge().codegenWrite(new JsonWriteForgeRefs(ref("writer"), ref(fieldName), constant(property.getKey())), method, classScope);
            method.getBlock()
                .exprDotMethod(ref("writer"), "writeMemberName", constant(property.getKey()))
                .exprDotMethod(ref("writer"), "writeMemberSeparator")
                .expression(write);
        }
    }

    private void makeGetNativeNum(CodegenMethod method, CodegenClassScope classScope) {
        if (numFieldsSupertype > 0) {
            method.getBlock()
                .declareVar(int.class, "parent", exprDotMethod(ref("super"), "getNativeNum", ref("name")))
                .ifCondition(relational(ref("parent"), GT, constant(-1)))
                .blockReturn(ref("parent"));
        }

        CodegenExpression[] expressions = new CodegenExpression[propertiesThisType.size()];
        for (Map.Entry<String, Object> property : propertiesThisType.entrySet()) {
            JsonUnderlyingField field = fieldDescriptorsInclSupertype.get(property.getKey());
            expressions[field.getPropertyNumber() - numFieldsSupertype] = constant(property.getKey());
        }

        CodegenStatementSwitch switchStmt = method.getBlock().switchBlockExpressions(ref("name"), expressions, true, false);
        for (int i = 0; i < switchStmt.getBlocks().length; i++) {
            switchStmt.getBlocks()[i].blockReturn(constant(numFieldsSupertype + i));
        }
        method.getBlock().methodReturn(constant(-1));
    }

    private void makeGetNativeValueMethod(CodegenMethod method, CodegenClassScope classScope) {
        if (numFieldsSupertype > 0) {
            method.getBlock()
                .ifCondition(relational(ref("num"), LT, constant(numFieldsSupertype)))
                .blockReturn(exprDotMethod(ref("super"), "getNativeValue", ref("num")));
        }

        CodegenExpression[] cases = getCasesNumberNtoM();
        CodegenStatementSwitch switchStmt = method.getBlock().switchBlockExpressions(ref("num"), cases, true, false);
        makeNoSuchElementDefault(switchStmt, ref("num"));
        int index = 0;
        for (Map.Entry<String, Object> property : propertiesThisType.entrySet()) {
            JsonUnderlyingField field = fieldDescriptorsInclSupertype.get(property.getKey());
            switchStmt.getBlocks()[index].blockReturn(ref(field.getFieldName()));
            index++;
        }
    }

    private void makeSetNativeValue(CodegenMethod method, CodegenClassScope classScope) {
        if (numFieldsSupertype > 0) {
            method.getBlock()
                .ifCondition(relational(ref("num"), LT, constant(numFieldsSupertype)))
                .exprDotMethod(ref("super"), "setNativeValue", ref("num"), ref("value"))
                .blockReturnNoValue();
        }

        CodegenExpression[] cases = getCasesNumberNtoM();
        CodegenStatementSwitch switchStmt = method.getBlock().switchBlockExpressions(ref("num"), cases, false, false);
        makeNoSuchElementDefault(switchStmt, ref("num"));
        CodegenBlock[] blocks = switchStmt.getBlocks();

        int index = 0;
        for (Map.Entry<String, Object> property : propertiesThisType.entrySet()) {
            JsonUnderlyingField field = fieldDescriptorsInclSupertype.get(property.getKey());
            String fieldName = field.getFieldName();

            Object type = propertiesThisType.get(property.getKey());
            if (type == null) {
                // no action
            } else if (type instanceof Class) {
                blocks[index].assignRef(fieldName, cast((Class) type, ref("value")));
            } else if (type instanceof TypeBeanOrUnderlying) {
                EventType eventType = ((TypeBeanOrUnderlying) type).getEventType();
                if (eventType instanceof JsonEventType) {
                    JsonEventType jsonEventType = (JsonEventType) eventType;
                    CodegenExpression castAsBean = castUnderlying(jsonEventType.getDetail().getUnderlyingClassName(), cast(EventBean.class, ref("value")));
                    CodegenExpression castUnd = cast(jsonEventType.getDetail().getUnderlyingClassName(), ref("value"));
                    blocks[index].assignRef(fieldName, conditional(instanceOf(ref("value"), EventBean.class), castAsBean, castUnd));
                } else {
                    CodegenExpression castAsBean = castUnderlying(Map.class, cast(EventBean.class, ref("value")));
                    CodegenExpression castUnd = cast(Map.class, ref("value"));
                    blocks[index].assignRef(fieldName, conditional(instanceOf(ref("value"), EventBean.class), castAsBean, castUnd));
                }
            } else if (type instanceof TypeBeanOrUnderlying[]) {
                TypeBeanOrUnderlying[] typeDef = (TypeBeanOrUnderlying[]) type;
                EventType eventType = typeDef[0].getEventType();
                Class arrayType = JavaClassHelper.getArrayType(eventType.getUnderlyingType());
                blocks[index]
                    .ifRefNull("value").assignRef(fieldName, constantNull()).blockReturnNoValue()
                    .ifCondition(instanceOf(ref("value"), arrayType)).assignRef(fieldName, cast(arrayType, ref("value"))).blockReturnNoValue()
                    .declareVar(EventBean[].class, "events", cast(EventBean[].class, ref("value")))
                    .declareVar(arrayType, "array", newArrayByLength(eventType.getUnderlyingType(), arrayLength(ref("events"))))
                    .forLoopIntSimple("i", arrayLength(ref("events")))
                    .assignArrayElement("array", ref("i"), castUnderlying(eventType.getUnderlyingType(), arrayAtIndex(ref("events"), ref("i"))))
                    .blockEnd()
                    .assignRef(fieldName, ref("array"));
            } else {
                throw new UnsupportedOperationException("Unrecognized type " + type);
            }
            index++;
        }
    }

    private void makeGetNativeEntry(CodegenMethod method, CodegenClassScope classScope) {
        CodegenMethod toEntry = method.makeChild(Map.Entry.class, this.getClass(), classScope).addParam(String.class, "name").addParam(Object.class, "value");
        toEntry.getBlock().methodReturn(newInstance(AbstractMap.SimpleEntry.class, ref("name"), ref("value")));

        if (numFieldsSupertype > 0) {
            method.getBlock()
                .ifCondition(relational(ref("num"), LT, constant(numFieldsSupertype)))
                .blockReturn(exprDotMethod(ref("super"), "getNativeEntry", ref("num")));
        }

        CodegenExpression[] cases = getCasesNumberNtoM();
        CodegenStatementSwitch switchStmt = method.getBlock().switchBlockExpressions(ref("num"), cases, true, false);
        makeNoSuchElementDefault(switchStmt, ref("num"));

        int index = 0;
        for (Map.Entry<String, Object> property : propertiesThisType.entrySet()) {
            JsonUnderlyingField field = fieldDescriptorsInclSupertype.get(property.getKey());
            switchStmt.getBlocks()[index].blockReturn(localMethod(toEntry, constant(property.getKey()), ref(field.getFieldName())));
            index++;
        }
    }

    private void makeGetNativeKey(CodegenMethod method) {
        if (numFieldsSupertype > 0) {
            method.getBlock()
                .ifCondition(relational(ref("num"), LT, constant(numFieldsSupertype)))
                .blockReturn(exprDotMethod(ref("super"), "getNativeKey", ref("num")));
        }

        CodegenExpression[] cases = getCasesNumberNtoM();
        CodegenStatementSwitch switchStmt = method.getBlock().switchBlockExpressions(ref("num"), cases, true, false);
        makeNoSuchElementDefault(switchStmt, ref("num"));

        int index = 0;
        for (Map.Entry<String, Object> property : propertiesThisType.entrySet()) {
            switchStmt.getBlocks()[index].blockReturn(constant(property.getKey()));
            index++;
        }
    }

    private void makeNativeContainsKey(CodegenMethod method) {
        if (optionalSupertype != null) {
            method.getBlock()
                .declareVar(boolean.class, "parent", exprDotMethod(ref("super"), "nativeContainsKey", ref("name")))
                .ifCondition(ref("parent")).blockReturn(constantTrue());
        }
        if (propertiesThisType.isEmpty()) {
            method.getBlock().methodReturn(constantFalse());
            return;
        }
        Set<String> names = propertiesThisType.keySet();
        Iterator<String> it = names.iterator();
        CodegenExpression or = exprDotMethod(ref("name"), "equals", constant(it.next()));
        while (it.hasNext()) {
            or = or(or, exprDotMethod(ref("name"), "equals", constant(it.next())));
        }
        method.getBlock().methodReturn(or);
    }

    private CodegenExpression[] getCasesNumberNtoM() {
        CodegenExpression[] cases = new CodegenExpression[propertiesThisType.size()];
        int index = 0;
        for (Map.Entry<String, Object> property : propertiesThisType.entrySet()) {
            JsonUnderlyingField field = fieldDescriptorsInclSupertype.get(property.getKey());
            cases[index] = constant(field.getPropertyNumber());
            index++;
        }
        return cases;
    }

    private void makeNoSuchElementDefault(CodegenStatementSwitch switchStmt, CodegenExpressionRef num) {
        switchStmt.getDefaultBlock().blockThrow(newInstance(NoSuchElementException.class, concat(constant("Field at number "), num)));
    }

    private boolean needDynamic() {
        return dynamic && !parentDynamic();
    }

    private boolean parentDynamic() {
        return optionalSupertype != null && optionalSupertype.getDetail().isDynamic();
    }
}
