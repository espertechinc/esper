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
import com.espertech.esper.common.client.type.EPTypePremade;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenPackageScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenSymbolProviderEmpty;
import com.espertech.esper.common.internal.bytecodemodel.core.*;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.bytecodemodel.model.statement.CodegenStatementSwitch;
import com.espertech.esper.common.internal.bytecodemodel.util.CodegenStackGenerator;
import com.espertech.esper.common.internal.compile.stage3.StmtClassForgeable;
import com.espertech.esper.common.internal.compile.stage3.StmtClassForgeableType;
import com.espertech.esper.common.internal.event.json.core.JsonEventObjectBase;
import com.espertech.esper.common.internal.event.json.parser.forge.JsonForgeDesc;
import com.espertech.esper.common.internal.event.json.write.JsonWriteForgeRefs;

import java.util.*;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;
import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionRelational.CodegenRelational.GT;
import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionRelational.CodegenRelational.LT;
import static com.espertech.esper.common.internal.event.json.compiletime.StmtClassForgeableJsonUtil.getCasesNumberNtoM;
import static com.espertech.esper.common.internal.event.json.compiletime.StmtClassForgeableJsonUtil.makeNoSuchElementDefault;

public class StmtClassForgeableJsonUnderlying implements StmtClassForgeable {

    public final static String DYNAMIC_PROP_FIELD = "__dyn";

    private final String className;
    private final CodegenPackageScope packageScope;
    private final StmtClassForgeableJsonDesc desc;

    public StmtClassForgeableJsonUnderlying(String className, CodegenPackageScope packageScope, StmtClassForgeableJsonDesc desc) {
        this.className = className;
        this.packageScope = packageScope;
        this.desc = desc;
    }

    public CodegenClass forge(boolean includeDebugSymbols, boolean fireAndForget) {
        CodegenCtor ctor = new CodegenCtor(StmtClassForgeableJsonUnderlying.class, includeDebugSymbols, Collections.emptyList());
        if (needDynamic()) {
            ctor.getBlock().assignRef(DYNAMIC_PROP_FIELD, newInstance(EPTypePremade.LINKEDHASHMAP.getEPType()));
        }

        CodegenClassMethods methods = new CodegenClassMethods();
        CodegenClassScope classScope = new CodegenClassScope(includeDebugSymbols, packageScope, className);

        List<CodegenTypedParam> explicitMembers = new ArrayList<>(desc.getPropertiesThisType().size());
        if (needDynamic()) {
            explicitMembers.add(new CodegenTypedParam(EPTypePremade.MAP.getEPType(), DYNAMIC_PROP_FIELD, false, true));
        }
        // add members
        for (Map.Entry<String, Object> property : desc.getPropertiesThisType().entrySet()) {
            JsonUnderlyingField field = desc.getFieldDescriptorsInclSupertype().get(property.getKey());
            explicitMembers.add(new CodegenTypedParam(field.getPropertyType(), field.getFieldName(), false, true));
        }

        // getNativeSize
        CodegenMethod getNativeSizeMethod = CodegenMethod.makeParentNode(EPTypePremade.INTEGERPRIMITIVE.getEPType(), this.getClass(), CodegenSymbolProviderEmpty.INSTANCE, classScope);
        getNativeSizeMethod.getBlock().methodReturn(constant(desc.getPropertiesThisType().size() + desc.getNumFieldsSupertype()));
        CodegenStackGenerator.recursiveBuildStack(getNativeSizeMethod, "getNativeSize", methods);

        // getNativeEntry
        CodegenMethod getNativeEntryMethod = CodegenMethod.makeParentNode(EPTypePremade.MAPENTRY.getEPType(), this.getClass(), CodegenSymbolProviderEmpty.INSTANCE, classScope)
                .addParam(EPTypePremade.INTEGERPRIMITIVE.getEPType(), "num");
        makeGetNativeEntry(getNativeEntryMethod, classScope);
        CodegenStackGenerator.recursiveBuildStack(getNativeEntryMethod, "getNativeEntry", methods);

        // getNativeEntry
        CodegenMethod getNativeKey = CodegenMethod.makeParentNode(EPTypePremade.STRING.getEPType(), this.getClass(), CodegenSymbolProviderEmpty.INSTANCE, classScope)
                .addParam(EPTypePremade.INTEGERPRIMITIVE.getEPType(), "num");
        makeGetNativeKey(getNativeKey);
        CodegenStackGenerator.recursiveBuildStack(getNativeKey, "getNativeKey", methods);

        // nativeContainsKey
        CodegenMethod nativeContainsKeyMethod = CodegenMethod.makeParentNode(EPTypePremade.BOOLEANPRIMITIVE.getEPType(), this.getClass(), CodegenSymbolProviderEmpty.INSTANCE, classScope)
                .addParam(EPTypePremade.OBJECT.getEPType(), "name");
        makeNativeContainsKey(nativeContainsKeyMethod);
        CodegenStackGenerator.recursiveBuildStack(nativeContainsKeyMethod, "nativeContainsKey", methods);

        // getNativeValue
        CodegenMethod getNativeValueMethod = CodegenMethod.makeParentNode(EPTypePremade.OBJECT.getEPType(), this.getClass(), CodegenSymbolProviderEmpty.INSTANCE, classScope)
                .addParam(EPTypePremade.INTEGERPRIMITIVE.getEPType(), "num");
        makeGetNativeValueMethod(getNativeValueMethod, classScope);
        CodegenStackGenerator.recursiveBuildStack(getNativeValueMethod, "getNativeValue", methods);

        // getNativeNum
        CodegenMethod getNativeNumMethod = CodegenMethod.makeParentNode(EPTypePremade.INTEGERPRIMITIVE.getEPType(), this.getClass(), CodegenSymbolProviderEmpty.INSTANCE, classScope)
                .addParam(EPTypePremade.STRING.getEPType(), "name");
        makeGetNativeNum(getNativeNumMethod, classScope);
        CodegenStackGenerator.recursiveBuildStack(getNativeNumMethod, "getNativeNum", methods);

        // nativeWrite
        CodegenMethod nativeWriteMethod = CodegenMethod.makeParentNode(EPTypePremade.VOID.getEPType(), this.getClass(), CodegenSymbolProviderEmpty.INSTANCE, classScope)
                .addParam(JsonWriter.EPTYPE, "writer").addThrown(EPTypePremade.IOEXCEPTION.getEPType());
        makeNativeWrite(nativeWriteMethod, classScope);
        CodegenStackGenerator.recursiveBuildStack(nativeWriteMethod, "nativeWrite", methods);

        if (!parentDynamic()) {
            // addJsonValue
            CodegenMethod addJsonValueMethod = CodegenMethod.makeParentNode(EPTypePremade.VOID.getEPType(), this.getClass(), CodegenSymbolProviderEmpty.INSTANCE, classScope)
                    .addParam(EPTypePremade.STRING.getEPType(), "name").addParam(EPTypePremade.OBJECT.getEPType(), "value");
            if (needDynamic()) {
                addJsonValueMethod.getBlock().exprDotMethod(ref(DYNAMIC_PROP_FIELD), "put", ref("name"), ref("value"));
            }
            CodegenStackGenerator.recursiveBuildStack(addJsonValueMethod, "addJsonValue", methods);

            // getJsonValues
            CodegenMethod getJsonValuesMethod = CodegenMethod.makeParentNode(EPTypePremade.MAP.getEPType(), this.getClass(), CodegenSymbolProviderEmpty.INSTANCE, classScope);
            getJsonValuesMethod.getBlock().methodReturn(desc.isDynamic() ? ref(DYNAMIC_PROP_FIELD) : publicConstValue(Collections.class, "EMPTY_MAP"));
            CodegenStackGenerator.recursiveBuildStack(getJsonValuesMethod, "getJsonValues", methods);
        }

        CodegenClass clazz = new CodegenClass(CodegenClassType.JSONEVENT, className, classScope, explicitMembers, ctor, methods, Collections.emptyList());
        if (desc.getOptionalSupertype() == null) {
            clazz.getSupers().setClassExtended(JsonEventObjectBase.EPTYPE);
        } else {
            clazz.getSupers().setClassExtended(desc.getOptionalSupertype().getUnderlyingEPType());
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
        if (desc.getOptionalSupertype() != null && !desc.getOptionalSupertype().getTypes().isEmpty()) {
            method.getBlock().exprDotMethod(ref("super"), "nativeWrite", ref("writer"));
            first = false;
        }
        for (Map.Entry<String, Object> property : desc.getPropertiesThisType().entrySet()) {
            JsonUnderlyingField field = desc.getFieldDescriptorsInclSupertype().get(property.getKey());
            JsonForgeDesc forge = desc.getForges().get(property.getKey());
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
        if (desc.getNumFieldsSupertype() > 0) {
            method.getBlock()
                    .declareVar(EPTypePremade.INTEGERPRIMITIVE.getEPType(), "parent", exprDotMethod(ref("super"), "getNativeNum", ref("name")))
                    .ifCondition(relational(ref("parent"), GT, constant(-1)))
                    .blockReturn(ref("parent"));
        }

        CodegenExpression[] expressions = new CodegenExpression[desc.getPropertiesThisType().size()];
        for (Map.Entry<String, Object> property : desc.getPropertiesThisType().entrySet()) {
            JsonUnderlyingField field = desc.getFieldDescriptorsInclSupertype().get(property.getKey());
            expressions[field.getPropertyNumber() - desc.getNumFieldsSupertype()] = constant(property.getKey());
        }

        CodegenStatementSwitch switchStmt = method.getBlock().switchBlockExpressions(ref("name"), expressions, true, false);
        for (int i = 0; i < switchStmt.getBlocks().length; i++) {
            switchStmt.getBlocks()[i].blockReturn(constant(desc.getNumFieldsSupertype() + i));
        }
        method.getBlock().methodReturn(constant(-1));
    }

    private void makeGetNativeValueMethod(CodegenMethod method, CodegenClassScope classScope) {
        if (desc.getNumFieldsSupertype() > 0) {
            method.getBlock()
                    .ifCondition(relational(ref("num"), LT, constant(desc.getNumFieldsSupertype())))
                    .blockReturn(exprDotMethod(ref("super"), "getNativeValue", ref("num")));
        }

        CodegenExpression[] cases = getCasesNumberNtoM(desc);
        CodegenStatementSwitch switchStmt = method.getBlock().switchBlockExpressions(ref("num"), cases, true, false);
        makeNoSuchElementDefault(switchStmt, ref("num"));
        int index = 0;
        for (Map.Entry<String, Object> property : desc.getPropertiesThisType().entrySet()) {
            JsonUnderlyingField field = desc.getFieldDescriptorsInclSupertype().get(property.getKey());
            switchStmt.getBlocks()[index].blockReturn(ref(field.getFieldName()));
            index++;
        }
    }

    private void makeGetNativeEntry(CodegenMethod method, CodegenClassScope classScope) {
        CodegenMethod toEntry = method.makeChild(EPTypePremade.MAPENTRY.getEPType(), this.getClass(), classScope).addParam(EPTypePremade.STRING.getEPType(), "name").addParam(EPTypePremade.OBJECT.getEPType(), "value");
        toEntry.getBlock().methodReturn(newInstance(EPTypePremade.ABSTRACTMAPSIMPLEENTRY.getEPType(), ref("name"), ref("value")));

        if (desc.getNumFieldsSupertype() > 0) {
            method.getBlock()
                    .ifCondition(relational(ref("num"), LT, constant(desc.getNumFieldsSupertype())))
                    .blockReturn(exprDotMethod(ref("super"), "getNativeEntry", ref("num")));
        }

        CodegenExpression[] cases = getCasesNumberNtoM(desc);
        CodegenStatementSwitch switchStmt = method.getBlock().switchBlockExpressions(ref("num"), cases, true, false);
        makeNoSuchElementDefault(switchStmt, ref("num"));

        int index = 0;
        for (Map.Entry<String, Object> property : desc.getPropertiesThisType().entrySet()) {
            JsonUnderlyingField field = desc.getFieldDescriptorsInclSupertype().get(property.getKey());
            switchStmt.getBlocks()[index].blockReturn(localMethod(toEntry, constant(property.getKey()), ref(field.getFieldName())));
            index++;
        }
    }

    private void makeGetNativeKey(CodegenMethod method) {
        if (desc.getNumFieldsSupertype() > 0) {
            method.getBlock()
                    .ifCondition(relational(ref("num"), LT, constant(desc.getNumFieldsSupertype())))
                    .blockReturn(exprDotMethod(ref("super"), "getNativeKey", ref("num")));
        }

        CodegenExpression[] cases = getCasesNumberNtoM(desc);
        CodegenStatementSwitch switchStmt = method.getBlock().switchBlockExpressions(ref("num"), cases, true, false);
        makeNoSuchElementDefault(switchStmt, ref("num"));

        int index = 0;
        for (Map.Entry<String, Object> property : desc.getPropertiesThisType().entrySet()) {
            switchStmt.getBlocks()[index].blockReturn(constant(property.getKey()));
            index++;
        }
    }

    private void makeNativeContainsKey(CodegenMethod method) {
        if (desc.getOptionalSupertype() != null) {
            method.getBlock()
                    .declareVar(EPTypePremade.BOOLEANBOXED.getEPType(), "parent", exprDotMethod(ref("super"), "nativeContainsKey", ref("name")))
                    .ifCondition(ref("parent")).blockReturn(constantTrue());
        }
        if (desc.getPropertiesThisType().isEmpty()) {
            method.getBlock().methodReturn(constantFalse());
            return;
        }
        Set<String> names = desc.getPropertiesThisType().keySet();
        Iterator<String> it = names.iterator();
        CodegenExpression or = exprDotMethod(ref("name"), "equals", constant(it.next()));
        while (it.hasNext()) {
            or = or(or, exprDotMethod(ref("name"), "equals", constant(it.next())));
        }
        method.getBlock().methodReturn(or);
    }

    private boolean needDynamic() {
        return desc.isDynamic() && !parentDynamic();
    }

    private boolean parentDynamic() {
        return desc.getOptionalSupertype() != null && desc.getOptionalSupertype().getDetail().isDynamic();
    }
}
