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
package com.espertech.esper.common.internal.compile.faf;

import com.espertech.esper.common.client.EPException;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenPackageScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenSymbolProviderEmpty;
import com.espertech.esper.common.internal.bytecodemodel.core.*;
import com.espertech.esper.common.internal.bytecodemodel.util.CodegenStackGenerator;
import com.espertech.esper.common.internal.compile.stage3.StmtClassForgeable;
import com.espertech.esper.common.internal.compile.stage3.StmtClassForgeableStmtFields;
import com.espertech.esper.common.internal.compile.stage3.StmtClassForgeableType;
import com.espertech.esper.common.internal.context.aifactory.core.SAIFFInitializeSymbol;
import com.espertech.esper.common.internal.context.module.EPStatementInitServices;
import com.espertech.esper.common.internal.epl.fafquery.querymethod.*;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.localMethod;
import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.ref;

public class StmtClassForgeableQueryMethodProvider implements StmtClassForgeable {
    private final static String MEMBERNAME_QUERYMETHOD = "queryMethod";

    private final String className;
    private final CodegenPackageScope packageScope;
    private final FAFQueryMethodForge forge;

    public StmtClassForgeableQueryMethodProvider(String className, CodegenPackageScope packageScope, FAFQueryMethodForge forge) {
        this.className = className;
        this.packageScope = packageScope;
        this.forge = forge;
    }

    public CodegenClass forge(boolean includeDebugSymbols, boolean fireAndForget) {
        Supplier<String> debugInformationProvider = new Supplier<String>() {
            public String get() {
                StringWriter writer = new StringWriter();
                writer.append("FAF query");
                return writer.toString();
            }
        };

        try {
            List<CodegenInnerClass> innerClasses = new ArrayList<>();

            // build ctor
            List<CodegenTypedParam> ctorParms = new ArrayList<>();
            ctorParms.add(new CodegenTypedParam(EPStatementInitServices.class, EPStatementInitServices.REF.getRef(), false));
            CodegenCtor providerCtor = new CodegenCtor(this.getClass(), includeDebugSymbols, ctorParms);
            CodegenClassScope classScope = new CodegenClassScope(includeDebugSymbols, packageScope, className);

            // add query method member
            List<CodegenTypedParam> providerExplicitMembers = new ArrayList<>(2);
            providerExplicitMembers.add(new CodegenTypedParam(FAFQueryMethod.class, MEMBERNAME_QUERYMETHOD));

            SAIFFInitializeSymbol symbols = new SAIFFInitializeSymbol();
            CodegenMethod makeMethod = providerCtor.makeChildWithScope(FAFQueryMethod.class, this.getClass(), symbols, classScope).addParam(EPStatementInitServices.class, EPStatementInitServices.REF.getRef());
            providerCtor.getBlock()
                    .staticMethod(packageScope.getFieldsClassNameOptional(), "init", EPStatementInitServices.REF)
                    .assignMember(MEMBERNAME_QUERYMETHOD, localMethod(makeMethod, EPStatementInitServices.REF));
            forge.makeMethod(makeMethod, symbols, classScope);

            // make provider methods
            CodegenMethod getQueryMethod = CodegenMethod.makeParentNode(FAFQueryMethod.class, this.getClass(), CodegenSymbolProviderEmpty.INSTANCE, classScope);
            getQueryMethod.getBlock().methodReturn(ref(MEMBERNAME_QUERYMETHOD));

            // add get-informational methods
            CodegenMethod getQueryInformationals = CodegenMethod.makeParentNode(FAFQueryInformationals.class, this.getClass(), CodegenSymbolProviderEmpty.INSTANCE, classScope);
            FAFQueryInformationals queryInformationals = FAFQueryInformationals.from(packageScope.getSubstitutionParamsByNumber(), packageScope.getSubstitutionParamsByName());
            getQueryInformationals.getBlock().methodReturn(queryInformationals.make(getQueryInformationals, classScope));

            // add get-statement-fields method
            CodegenMethod getSubstitutionFieldSetter = CodegenMethod.makeParentNode(FAFQueryMethodAssignerSetter.class, this.getClass(), CodegenSymbolProviderEmpty.INSTANCE, classScope);
            StmtClassForgeableStmtFields.makeSubstitutionSetter(packageScope, getSubstitutionFieldSetter, classScope);

            // make provider methods
            CodegenClassMethods methods = new CodegenClassMethods();
            CodegenStackGenerator.recursiveBuildStack(providerCtor, "ctor", methods);
            CodegenStackGenerator.recursiveBuildStack(getQueryMethod, "getQueryMethod", methods);
            CodegenStackGenerator.recursiveBuildStack(getQueryInformationals, "getQueryInformationals", methods);
            CodegenStackGenerator.recursiveBuildStack(getSubstitutionFieldSetter, "getSubstitutionFieldSetter", methods);

            // render and compile
            return new CodegenClass(CodegenClassType.FAFQUERYMETHODPROVIDER, FAFQueryMethodProvider.class, className, classScope, providerExplicitMembers, providerCtor, methods, innerClasses);
        } catch (Throwable t) {
            throw new EPException("Fatal exception during code-generation for " + debugInformationProvider.get() + " : " + t.getMessage(), t);
        }
    }

    public String getClassName() {
        return className;
    }

    public StmtClassForgeableType getForgeableType() {
        return StmtClassForgeableType.FAF;
    }
}
