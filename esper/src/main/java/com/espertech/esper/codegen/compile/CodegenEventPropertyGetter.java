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
package com.espertech.esper.codegen.compile;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.EventPropertyGetter;
import com.espertech.esper.client.EventPropertyGetterIndexed;
import com.espertech.esper.client.EventPropertyGetterMapped;
import com.espertech.esper.codegen.core.*;
import com.espertech.esper.codegen.model.method.CodegenParamSet;
import com.espertech.esper.codegen.model.method.CodegenParamSetMulti;
import com.espertech.esper.codegen.model.method.CodegenParamSetSingle;
import com.espertech.esper.epl.core.EngineImportService;
import com.espertech.esper.event.EventPropertyGetterIndexedSPI;
import com.espertech.esper.event.EventPropertyGetterMappedSPI;
import com.espertech.esper.event.EventPropertyGetterSPI;

import java.io.StringWriter;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.ref;

public class CodegenEventPropertyGetter {

    private final static CodegenMethodFootprint GETTER_GET_FP;
    private final static CodegenMethodFootprint GETTER_EXISTS_FP;
    private final static CodegenMethodFootprint GETTER_FRAGMENT_FP;
    private final static CodegenMethodFootprint GETTER_GET_INDEXED_FP;
    private final static CodegenMethodFootprint GETTER_GET_MAPPED_FP;

    static {
        List<CodegenParamSet> paramsGetSimple = Collections.<CodegenParamSet>singletonList(new CodegenParamSetSingle(new CodegenNamedParam(EventBean.class, "bean")));
        GETTER_GET_FP = new CodegenMethodFootprint(Object.class, "get", paramsGetSimple, null);
        GETTER_EXISTS_FP = new CodegenMethodFootprint(boolean.class, "isExistsProperty", paramsGetSimple, null);
        GETTER_FRAGMENT_FP = new CodegenMethodFootprint(Object.class, "getFragment", paramsGetSimple, null);

        List<CodegenParamSet> paramsGetIndexed = Collections.<CodegenParamSet>singletonList(new CodegenParamSetMulti(Arrays.asList(new CodegenNamedParam(EventBean.class, "bean"), new CodegenNamedParam(int.class, "index"))));
        GETTER_GET_INDEXED_FP = new CodegenMethodFootprint(Object.class, "get", paramsGetIndexed, null);

        List<CodegenParamSet> paramsGetMapped = Collections.<CodegenParamSet>singletonList(new CodegenParamSetMulti(Arrays.asList(new CodegenNamedParam(EventBean.class, "bean"), new CodegenNamedParam(String.class, "key"))));
        GETTER_GET_MAPPED_FP = new CodegenMethodFootprint(Object.class, "get", paramsGetMapped, null);
    }

    public static EventPropertyGetter compile(String engineURI, EngineImportService engineImportService, EventPropertyGetterSPI getterSPI, Supplier<String> debugInfoSupplier, boolean includeCodeComments) throws CodegenCompilerException {
        CodegenContext codegenContext = new CodegenContext(includeCodeComments);
        CodegenMethod getMethod = new CodegenMethod(GETTER_GET_FP, getterSPI.eventBeanGetCodegen(ref("bean"), codegenContext));
        CodegenMethod isExistsPropertyMethod = new CodegenMethod(GETTER_EXISTS_FP, getterSPI.eventBeanExistsCodegen(ref("bean"), codegenContext));
        CodegenMethod fragmentMethod = new CodegenMethod(GETTER_FRAGMENT_FP, getterSPI.eventBeanFragmentCodegen(ref("bean"), codegenContext));
        CodegenClass clazz = new CodegenClass(EventPropertyGetter.class, codegenContext, engineURI, getMethod, isExistsPropertyMethod, fragmentMethod);
        return CodegenClassGenerator.compile(clazz, engineImportService, EventPropertyGetter.class, debugInfoSupplier);
    }

    public static EventPropertyGetterIndexed compile(String engineURI, EngineImportService engineImportService, EventPropertyGetterIndexedSPI getterSPI, Supplier<String> debugInfoSupplier, boolean includeCodeComments) throws CodegenCompilerException {
        CodegenContext codegenContext = new CodegenContext(includeCodeComments);
        CodegenMethod getMethod = new CodegenMethod(GETTER_GET_INDEXED_FP, getterSPI.eventBeanGetIndexedCodegen(codegenContext, ref("bean"), ref("index")));
        CodegenClass clazz = new CodegenClass(EventPropertyGetterIndexed.class, codegenContext, engineURI, getMethod);
        return CodegenClassGenerator.compile(clazz, engineImportService, EventPropertyGetterIndexed.class, debugInfoSupplier);
    }

    public static EventPropertyGetterMapped compile(String engineURI, EngineImportService engineImportService, EventPropertyGetterMappedSPI getterSPI, Supplier<String> debugInfoSupplier, boolean includeCodeComments) throws CodegenCompilerException {
        CodegenContext codegenContext = new CodegenContext(includeCodeComments);
        CodegenMethod getMethod = new CodegenMethod(GETTER_GET_MAPPED_FP, getterSPI.eventBeanGetMappedCodegen(codegenContext, ref("bean"), ref("key")));
        CodegenClass clazz = new CodegenClass(EventPropertyGetterMapped.class, codegenContext, engineURI, getMethod);
        return CodegenClassGenerator.compile(clazz, engineImportService, EventPropertyGetterMapped.class, debugInfoSupplier);
    }
}
