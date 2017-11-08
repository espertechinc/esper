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
package com.espertech.esper.event;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.EventPropertyGetter;
import com.espertech.esper.client.EventPropertyGetterIndexed;
import com.espertech.esper.client.EventPropertyGetterMapped;
import com.espertech.esper.codegen.base.CodegenClassScope;
import com.espertech.esper.codegen.base.CodegenMethodNode;
import com.espertech.esper.codegen.base.CodegenSymbolProviderEmpty;
import com.espertech.esper.codegen.compile.CodegenClassGenerator;
import com.espertech.esper.codegen.compile.CodegenCompilerException;
import com.espertech.esper.codegen.core.CodeGenerationIDGenerator;
import com.espertech.esper.codegen.core.CodegenClass;
import com.espertech.esper.codegen.core.CodegenClassMethods;
import com.espertech.esper.codegen.util.CodegenStackGenerator;
import com.espertech.esper.epl.core.engineimport.EngineImportService;

import java.util.Collections;
import java.util.function.Supplier;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.ref;

public class EventPropertyGetterCompiler {
    
    public static EventPropertyGetter compile(EngineImportService engineImportService, EventPropertyGetterSPI getterSPI, Supplier<String> debugInfoSupplier, boolean includeCodeComments) throws CodegenCompilerException {
        CodegenClassScope codegenClassScope = new CodegenClassScope(includeCodeComments);

        CodegenMethodNode getMethod = CodegenMethodNode.makeParentNode(Object.class, getterSPI.getClass(), CodegenSymbolProviderEmpty.INSTANCE, codegenClassScope).addParam(EventBean.class, "bean");
        getMethod.getBlock().methodReturn(getterSPI.eventBeanGetCodegen(ref("bean"), getMethod, codegenClassScope));

        CodegenMethodNode existsMethod = CodegenMethodNode.makeParentNode(boolean.class, getterSPI.getClass(), CodegenSymbolProviderEmpty.INSTANCE, codegenClassScope).addParam(EventBean.class, "bean");
        existsMethod.getBlock().methodReturn(getterSPI.eventBeanExistsCodegen(ref("bean"), existsMethod, codegenClassScope));

        CodegenMethodNode fragmentMethod = CodegenMethodNode.makeParentNode(Object.class, getterSPI.getClass(), CodegenSymbolProviderEmpty.INSTANCE, codegenClassScope).addParam(EventBean.class, "bean");
        fragmentMethod.getBlock().methodReturn(getterSPI.eventBeanFragmentCodegen(ref("bean"), fragmentMethod, codegenClassScope));

        CodegenClassMethods methods = new CodegenClassMethods();
        CodegenStackGenerator.recursiveBuildStack(getMethod, "get", methods);
        CodegenStackGenerator.recursiveBuildStack(existsMethod, "isExistsProperty", methods);
        CodegenStackGenerator.recursiveBuildStack(fragmentMethod, "getFragment", methods);

        String className = CodeGenerationIDGenerator.generateClassName(EventPropertyGetter.class);
        CodegenClass clazz = new CodegenClass(EventPropertyGetter.class, engineImportService.getCodegenCompiler().getPackageName(), className, codegenClassScope, Collections.emptyList(), null, methods, Collections.emptyList());
        return CodegenClassGenerator.compile(clazz, engineImportService, EventPropertyGetter.class, debugInfoSupplier);
    }

    public static EventPropertyGetterIndexed compile(EngineImportService engineImportService, EventPropertyGetterIndexedSPI getterSPI, Supplier<String> debugInfoSupplier, boolean includeCodeComments) throws CodegenCompilerException {
        CodegenClassScope codegenClassScope = new CodegenClassScope(includeCodeComments);

        CodegenMethodNode getMethod = CodegenMethodNode.makeParentNode(Object.class, getterSPI.getClass(), CodegenSymbolProviderEmpty.INSTANCE, codegenClassScope).addParam(EventBean.class, "bean").addParam(int.class, "index");
        getMethod.getBlock().methodReturn(getterSPI.eventBeanGetIndexedCodegen(getMethod, codegenClassScope, ref("bean"), ref("index")));

        CodegenClassMethods methods = new CodegenClassMethods();
        CodegenStackGenerator.recursiveBuildStack(getMethod, "get", methods);

        String className = CodeGenerationIDGenerator.generateClassName(EventPropertyGetterIndexed.class);
        CodegenClass clazz = new CodegenClass(EventPropertyGetterIndexed.class, engineImportService.getCodegenCompiler().getPackageName(), className, codegenClassScope, Collections.emptyList(), null, methods, Collections.emptyList());
        return CodegenClassGenerator.compile(clazz, engineImportService, EventPropertyGetterIndexed.class, debugInfoSupplier);
    }

    public static EventPropertyGetterMapped compile(EngineImportService engineImportService, EventPropertyGetterMappedSPI getterSPI, Supplier<String> debugInfoSupplier, boolean includeCodeComments) throws CodegenCompilerException {
        CodegenClassScope codegenClassScope = new CodegenClassScope(includeCodeComments);

        CodegenMethodNode getMethod = CodegenMethodNode.makeParentNode(Object.class, getterSPI.getClass(), CodegenSymbolProviderEmpty.INSTANCE, codegenClassScope).addParam(EventBean.class, "bean").addParam(String.class, "key");
        getMethod.getBlock().methodReturn(getterSPI.eventBeanGetMappedCodegen(getMethod, codegenClassScope, ref("bean"), ref("key")));

        CodegenClassMethods methods = new CodegenClassMethods();
        CodegenStackGenerator.recursiveBuildStack(getMethod, "get", methods);

        String className = CodeGenerationIDGenerator.generateClassName(EventPropertyGetterMapped.class);
        CodegenClass clazz = new CodegenClass(EventPropertyGetterMapped.class, engineImportService.getCodegenCompiler().getPackageName(), className, codegenClassScope, Collections.emptyList(), null, methods, Collections.emptyList());
        return CodegenClassGenerator.compile(clazz, engineImportService, EventPropertyGetterMapped.class, debugInfoSupplier);
    }
}
