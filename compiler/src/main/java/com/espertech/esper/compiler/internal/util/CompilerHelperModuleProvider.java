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
package com.espertech.esper.compiler.internal.util;

import com.espertech.esper.common.client.EPCompiled;
import com.espertech.esper.common.client.EPCompiledManifest;
import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.client.meta.EventTypeMetadata;
import com.espertech.esper.common.client.module.ModuleProperty;
import com.espertech.esper.common.client.serde.DataInputOutputSerde;
import com.espertech.esper.common.client.type.EPType;
import com.espertech.esper.common.client.type.EPTypePremade;
import com.espertech.esper.common.internal.bytecodemodel.base.*;
import com.espertech.esper.common.internal.bytecodemodel.core.CodeGenerationIDGenerator;
import com.espertech.esper.common.internal.bytecodemodel.core.CodegenClass;
import com.espertech.esper.common.internal.bytecodemodel.core.CodegenClassMethods;
import com.espertech.esper.common.internal.bytecodemodel.core.CodegenClassType;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionEPType;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionRef;
import com.espertech.esper.common.internal.bytecodemodel.util.CodegenStackGenerator;
import com.espertech.esper.common.internal.bytecodemodel.util.IdentifierUtil;
import com.espertech.esper.common.internal.compile.stage1.Compilable;
import com.espertech.esper.common.internal.compile.stage1.spec.ExpressionDeclItem;
import com.espertech.esper.common.internal.compile.stage1.spec.ExpressionScriptProvided;
import com.espertech.esper.common.internal.compile.stage2.StatementSpecCompileException;
import com.espertech.esper.common.internal.compile.stage2.StatementSpecCompileSyntaxException;
import com.espertech.esper.common.internal.compile.stage3.ModuleCompileTimeServices;
import com.espertech.esper.common.internal.context.aifactory.core.*;
import com.espertech.esper.common.internal.context.compile.ContextMetaData;
import com.espertech.esper.common.internal.context.module.*;
import com.espertech.esper.common.internal.epl.classprovided.core.ClassProvided;
import com.espertech.esper.common.internal.epl.index.compile.IndexCompileTimeKey;
import com.espertech.esper.common.internal.epl.index.compile.IndexDetail;
import com.espertech.esper.common.internal.epl.index.compile.IndexDetailForge;
import com.espertech.esper.common.internal.epl.namedwindow.path.NamedWindowMetaData;
import com.espertech.esper.common.internal.epl.script.core.NameAndParamNum;
import com.espertech.esper.common.internal.epl.table.compiletime.TableMetaData;
import com.espertech.esper.common.internal.epl.variable.compiletime.VariableMetaData;
import com.espertech.esper.common.internal.event.avro.AvroSchemaEventType;
import com.espertech.esper.common.internal.event.bean.core.BeanEventType;
import com.espertech.esper.common.internal.event.core.BaseNestableEventType;
import com.espertech.esper.common.internal.event.core.EventTypeUtility;
import com.espertech.esper.common.internal.event.core.TypeBeanOrUnderlying;
import com.espertech.esper.common.internal.event.core.WrapperEventType;
import com.espertech.esper.common.internal.event.json.core.JsonEventType;
import com.espertech.esper.common.internal.event.map.MapEventType;
import com.espertech.esper.common.internal.event.path.EventTypeResolver;
import com.espertech.esper.common.internal.event.variant.VariantEventType;
import com.espertech.esper.common.internal.event.xml.BaseXMLEventType;
import com.espertech.esper.common.internal.event.xml.SchemaXMLEventType;
import com.espertech.esper.common.internal.serde.compiletime.resolve.DataInputOutputSerdeForge;
import com.espertech.esper.common.internal.util.CollectionUtil;
import com.espertech.esper.common.internal.util.SerializerUtil;
import com.espertech.esper.compiler.client.CompilerOptions;
import com.espertech.esper.compiler.client.EPCompileException;
import com.espertech.esper.compiler.client.EPCompileExceptionItem;
import com.espertech.esper.compiler.client.EPCompileExceptionSyntaxItem;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;
import static com.espertech.esper.compiler.internal.util.CompilerHelperStatementProvider.compileItem;
import static com.espertech.esper.compiler.internal.util.CompilerVersion.COMPILER_VERSION;

public class CompilerHelperModuleProvider {
    private final static int NUM_STATEMENT_NAMES_PER_BATCH = 1000;

    protected static EPCompiled compile(List<Compilable> compilables, String optionalModuleName, Map<ModuleProperty, Object> moduleProperties, ModuleCompileTimeServices compileTimeServices, CompilerOptions compilerOptions) throws EPCompileException {
        ConcurrentHashMap<String, byte[]> moduleBytes = new ConcurrentHashMap<>();
        EPCompiledManifest manifest;
        try {
            manifest = compileToBytes(moduleBytes, compilables, optionalModuleName, moduleProperties, compileTimeServices, compilerOptions);
        } catch (EPCompileException ex) {
            throw ex;
        } catch (Throwable t) {
            throw new EPCompileException("Unexpected exception compiling module: " + t.getMessage(), t, Collections.emptyList());
        }
        return new EPCompiled(moduleBytes, manifest);
    }

    private static EPCompiledManifest compileToBytes(ConcurrentHashMap<String, byte[]> moduleBytes, List<Compilable> compilables, String optionalModuleName, Map<ModuleProperty, Object> moduleProperties, ModuleCompileTimeServices compileTimeServices, CompilerOptions compilerOptions) throws EPCompileException, IOException {
        String moduleAssignedName = optionalModuleName == null ? UUID.randomUUID().toString() : optionalModuleName;
        String moduleIdentPostfix = IdentifierUtil.getIdentifierMayStartNumeric(moduleAssignedName);

        // compile each statement
        List<String> statementClassNames = new ArrayList<>();
        Set<String> statementNames = new HashSet<>();
        List<EPCompileExceptionItem> exceptions = new ArrayList<>();
        List<EPCompileExceptionItem> postLatchThrowables = new ArrayList<>();
        CompilerPool compilerPool = new CompilerPool(compilables.size(), compileTimeServices, moduleBytes);

        try {
            int statementNumber = 0;
            for (Compilable compilable : compilables) {
                String className = null;
                EPCompileExceptionItem exception = null;

                try {
                    CompilableItem compilableItem = compileItem(compilable, optionalModuleName, moduleIdentPostfix, statementNumber, statementNames, compileTimeServices, compilerOptions);
                    className = compilableItem.getProviderClassName();

                    compilerPool.submit(statementNumber, compilableItem);

                    // there can be a post-compile step, which may block submitting further compilables
                    try {
                        compilableItem.getPostCompileLatch().awaitAndRun();
                    } catch (Throwable t) {
                        postLatchThrowables.add(new EPCompileExceptionItem(t.getMessage(), t, compilable.toEPL(), compilable.lineNumber()));
                    }
                } catch (StatementSpecCompileException ex) {
                    if (ex instanceof StatementSpecCompileSyntaxException) {
                        exception = new EPCompileExceptionSyntaxItem(ex.getMessage(), ex, ex.getExpression(), compilable.lineNumber());
                    } else {
                        exception = new EPCompileExceptionItem(ex.getMessage(), ex, ex.getExpression(), compilable.lineNumber());
                    }
                    exceptions.add(exception);
                } catch (RuntimeException ex) {
                    exception = new EPCompileExceptionItem(ex.getMessage(), ex, compilable.toEPL(), compilable.lineNumber());
                    exceptions.add(exception);
                }

                if (exception == null) {
                    statementClassNames.add(className);
                }
                statementNumber++;
            }
        } catch (InterruptedException | RuntimeException ex) {
            compilerPool.shutdownNow();
            throw new EPCompileException(ex.getMessage(), ex);
        }

        // await async compilation
        compilerPool.shutdownCollectResults();

        exceptions.addAll(postLatchThrowables);
        if (!exceptions.isEmpty()) {
            compilerPool.shutdown();
            EPCompileExceptionItem ex = exceptions.get(0);
            throw new EPCompileException(ex.getMessage() + " [" + ex.getExpression() + "]", ex, exceptions);
        }

        // compile module resource
        String moduleProviderClassName = compileModule(optionalModuleName, moduleProperties, statementClassNames, moduleIdentPostfix, moduleBytes, compileTimeServices);

        // remove path create-class class-provided byte code
        compileTimeServices.getClassProvidedCompileTimeResolver().removeFrom(moduleBytes);

        // add class-provided create-class classes to module bytes
        for (Map.Entry<String, ClassProvided> entry : compileTimeServices.getClassProvidedCompileTimeRegistry().getClasses().entrySet()) {
            moduleBytes.putAll(entry.getValue().getBytes());
        }

        // create module XML
        return new EPCompiledManifest(COMPILER_VERSION, moduleProviderClassName, null, compileTimeServices.getSerdeResolver().isTargetHA());
    }

    private static String compileModule(String optionalModuleName, Map<ModuleProperty, Object> moduleProperties, List<String> statementClassNames, String moduleIdentPostfix, Map<String, byte[]> moduleBytes, ModuleCompileTimeServices compileTimeServices) {
        // write code to create an implementation of StatementResource
        CodegenPackageScope packageScope = new CodegenPackageScope(compileTimeServices.getPackageName(), null, compileTimeServices.isInstrumented());
        String moduleClassName = CodeGenerationIDGenerator.generateClassNameSimple(ModuleProvider.class, moduleIdentPostfix);
        CodegenClassScope classScope = new CodegenClassScope(true, packageScope, moduleClassName);
        CodegenClassMethods methods = new CodegenClassMethods();

        // provide module name
        CodegenMethod getModuleNameMethodOpt = null;
        if (optionalModuleName != null) {
            getModuleNameMethodOpt = CodegenMethod.makeParentNode(EPTypePremade.STRING.getEPType(), EPCompilerImpl.class, CodegenSymbolProviderEmpty.INSTANCE, classScope);
            getModuleNameMethodOpt.getBlock().methodReturn(constant(optionalModuleName));
        }

        // provide module properties
        CodegenMethod getModulePropertiesMethodOpt = null;
        if (!moduleProperties.isEmpty()) {
            getModulePropertiesMethodOpt = CodegenMethod.makeParentNode(EPTypePremade.MAP.getEPType(), EPCompilerImpl.class, CodegenSymbolProviderEmpty.INSTANCE, classScope);
            makeModuleProperties(moduleProperties, getModulePropertiesMethodOpt);
        }

        // provide module dependencies
        CodegenMethod getModuleDependenciesMethod = CodegenMethod.makeParentNode(ModuleDependenciesRuntime.EPTYPE, EPCompilerImpl.class, CodegenSymbolProviderEmpty.INSTANCE, classScope);
        compileTimeServices.getModuleDependencies().make(getModuleDependenciesMethod, classScope);

        // register types
        CodegenMethod initializeEventTypesMethodOpt = makeInitEventTypesOptional(classScope, compileTimeServices);

        // register named windows
        CodegenMethod initializeNamedWindowsMethodOpt = makeInitNamedWindowsOptional(classScope, compileTimeServices);

        // register tables
        CodegenMethod initializeTablesMethodOpt = makeInitTablesOptional(classScope, compileTimeServices);

        // register indexes
        CodegenMethod initializeIndexesMethodOpt = makeInitIndexesOptional(classScope, compileTimeServices);

        // register contexts
        CodegenMethod initializeContextsMethodOpt = makeInitContextsOptional(classScope, compileTimeServices);

        // register variables
        CodegenMethod initializeVariablesMethodOpt = makeInitVariablesOptional(classScope, compileTimeServices);

        // register expressions
        CodegenMethod initializeExprDeclaredMethodOpt = makeInitDeclExprOptional(classScope, compileTimeServices);

        // register scripts
        CodegenMethod initializeScriptsMethodOpt = makeInitScriptsOptional(classScope, compileTimeServices);

        // register provided classes
        CodegenMethod initializeClassProvidedMethodOpt = makeInitClassProvidedOptional(classScope, compileTimeServices);

        // instantiate factories for statements
        CodegenMethod statementsMethod = CodegenMethod.makeParentNode(EPTypePremade.LIST.getEPType(), EPCompilerImpl.class, CodegenSymbolProviderEmpty.INSTANCE, classScope);
        makeStatementsMethod(statementsMethod, statementClassNames, classScope);

        // build stack
        if (getModuleNameMethodOpt != null) {
            CodegenStackGenerator.recursiveBuildStack(getModuleNameMethodOpt, "getModuleName", methods);
        }
        if (getModulePropertiesMethodOpt != null) {
            CodegenStackGenerator.recursiveBuildStack(getModulePropertiesMethodOpt, "getModuleProperties", methods);
        }
        CodegenStackGenerator.recursiveBuildStack(getModuleDependenciesMethod, "getModuleDependencies", methods);
        if (initializeEventTypesMethodOpt != null) {
            CodegenStackGenerator.recursiveBuildStack(initializeEventTypesMethodOpt, "initializeEventTypes", methods);
        }
        if (initializeNamedWindowsMethodOpt != null) {
            CodegenStackGenerator.recursiveBuildStack(initializeNamedWindowsMethodOpt, "initializeNamedWindows", methods);
        }
        if (initializeTablesMethodOpt != null) {
            CodegenStackGenerator.recursiveBuildStack(initializeTablesMethodOpt, "initializeTables", methods);
        }
        if (initializeIndexesMethodOpt != null) {
            CodegenStackGenerator.recursiveBuildStack(initializeIndexesMethodOpt, "initializeIndexes", methods);
        }
        if (initializeContextsMethodOpt != null) {
            CodegenStackGenerator.recursiveBuildStack(initializeContextsMethodOpt, "initializeContexts", methods);
        }
        if (initializeVariablesMethodOpt != null) {
            CodegenStackGenerator.recursiveBuildStack(initializeVariablesMethodOpt, "initializeVariables", methods);
        }
        if (initializeExprDeclaredMethodOpt != null) {
            CodegenStackGenerator.recursiveBuildStack(initializeExprDeclaredMethodOpt, "initializeExprDeclareds", methods);
        }
        if (initializeScriptsMethodOpt != null) {
            CodegenStackGenerator.recursiveBuildStack(initializeScriptsMethodOpt, "initializeScripts", methods);
        }
        if (initializeClassProvidedMethodOpt != null) {
            CodegenStackGenerator.recursiveBuildStack(initializeClassProvidedMethodOpt, "initializeClassProvided", methods);
        }
        CodegenStackGenerator.recursiveBuildStack(statementsMethod, "statements", methods);

        CodegenClass clazz = new CodegenClass(CodegenClassType.MODULEPROVIDER, ModuleProvider.EPTYPE, moduleClassName, classScope, Collections.emptyList(), null, methods, Collections.emptyList());
        JaninoCompiler.compile(clazz, moduleBytes, moduleBytes, compileTimeServices);

        return CodeGenerationIDGenerator.generateClassNameWithPackage(compileTimeServices.getPackageName(), ModuleProvider.class, moduleIdentPostfix);
    }

    private static CodegenMethod makeInitClassProvidedOptional(CodegenClassScope classScope, ModuleCompileTimeServices compileTimeServices) {
        if (compileTimeServices.getClassProvidedCompileTimeRegistry().getClasses().isEmpty()) {
            return null;
        }
        ModuleClassProvidedInitializeSymbol symbols = new ModuleClassProvidedInitializeSymbol();
        CodegenMethod method = CodegenMethod.makeParentNode(EPTypePremade.VOID.getEPType(), EPCompilerImpl.class, symbols, classScope).addParam(EPModuleClassProvidedInitServices.EPTYPE, ModuleClassProvidedInitializeSymbol.REF_INITSVC.getRef());
        for (Map.Entry<String, ClassProvided> clazz : compileTimeServices.getClassProvidedCompileTimeRegistry().getClasses().entrySet()) {
            CodegenMethod addClassProvided = registerClassProvidedCodegen(clazz, method, classScope, symbols);
            method.getBlock().expression(localMethod(addClassProvided));
        }
        return method;
    }

    private static CodegenMethod makeInitScriptsOptional(CodegenClassScope classScope, ModuleCompileTimeServices compileTimeServices) {
        if (compileTimeServices.getScriptCompileTimeRegistry().getScripts().isEmpty()) {
            return null;
        }
        ModuleScriptInitializeSymbol symbols = new ModuleScriptInitializeSymbol();
        CodegenMethod method = CodegenMethod.makeParentNode(EPTypePremade.VOID.getEPType(), EPCompilerImpl.class, symbols, classScope).addParam(EPModuleScriptInitServices.EPTYPE, ModuleScriptInitializeSymbol.REF_INITSVC.getRef());
        for (Map.Entry<NameAndParamNum, ExpressionScriptProvided> expression : compileTimeServices.getScriptCompileTimeRegistry().getScripts().entrySet()) {
            CodegenMethod addScript = registerScriptCodegen(expression, method, classScope, symbols);
            method.getBlock().expression(localMethod(addScript));
        }
        return method;
    }

    private static CodegenMethod makeInitDeclExprOptional(CodegenClassScope classScope, ModuleCompileTimeServices compileTimeServices) {
        if (compileTimeServices.getExprDeclaredCompileTimeRegistry().getExpressions().isEmpty()) {
            return null;
        }
        ModuleExpressionDeclaredInitializeSymbol symbols = new ModuleExpressionDeclaredInitializeSymbol();
        CodegenMethod method = CodegenMethod.makeParentNode(EPTypePremade.VOID.getEPType(), EPCompilerImpl.class, symbols, classScope).addParam(EPModuleExprDeclaredInitServices.EPTYPE, ModuleExpressionDeclaredInitializeSymbol.REF_INITSVC.getRef());
        for (Map.Entry<String, ExpressionDeclItem> expression : compileTimeServices.getExprDeclaredCompileTimeRegistry().getExpressions().entrySet()) {
            CodegenMethod addExpression = registerExprDeclaredCodegen(expression, method, classScope, symbols);
            method.getBlock().expression(localMethod(addExpression));
        }
        return method;
    }

    private static CodegenMethod makeInitVariablesOptional(CodegenClassScope classScope, ModuleCompileTimeServices compileTimeServices) {
        if (compileTimeServices.getVariableCompileTimeRegistry().getVariables().isEmpty()) {
            return null;
        }
        ModuleVariableInitializeSymbol symbols = new ModuleVariableInitializeSymbol();
        CodegenMethod method = CodegenMethod.makeParentNode(EPTypePremade.VOID.getEPType(), EPCompilerImpl.class, symbols, classScope).addParam(EPModuleVariableInitServices.EPTYPE, ModuleVariableInitializeSymbol.REF_INITSVC.getRef());
        for (Map.Entry<String, VariableMetaData> variable : compileTimeServices.getVariableCompileTimeRegistry().getVariables().entrySet()) {
            CodegenMethod addVariable = registerVariableCodegen(variable, method, classScope, symbols);
            method.getBlock().expression(localMethod(addVariable));
        }
        return method;
    }

    private static CodegenMethod makeInitContextsOptional(CodegenClassScope classScope, ModuleCompileTimeServices compileTimeServices) {
        if (compileTimeServices.getContextCompileTimeRegistry().getContexts().isEmpty()) {
            return null;
        }
        ModuleContextInitializeSymbol symbols = new ModuleContextInitializeSymbol();
        CodegenMethod method = CodegenMethod.makeParentNode(EPTypePremade.VOID.getEPType(), EPCompilerImpl.class, symbols, classScope).addParam(EPModuleContextInitServices.EPTYPE, ModuleContextInitializeSymbol.REF_INITSVC.getRef());
        for (Map.Entry<String, ContextMetaData> context : compileTimeServices.getContextCompileTimeRegistry().getContexts().entrySet()) {
            CodegenMethod addContext = registerContextCodegen(context, method, classScope, symbols);
            method.getBlock().expression(localMethod(addContext));
        }
        return method;
    }

    private static CodegenMethod makeInitIndexesOptional(CodegenClassScope classScope, ModuleCompileTimeServices compileTimeServices) {
        if (compileTimeServices.getIndexCompileTimeRegistry().getIndexes().isEmpty()) {
            return null;
        }
        ModuleIndexesInitializeSymbol symbols = new ModuleIndexesInitializeSymbol();
        CodegenMethod method = CodegenMethod.makeParentNode(EPTypePremade.VOID.getEPType(), EPCompilerImpl.class, symbols, classScope).addParam(EPModuleIndexInitServices.EPTYPE, EPModuleIndexInitServices.REF.getRef());
        for (Map.Entry<IndexCompileTimeKey, IndexDetailForge> index : compileTimeServices.getIndexCompileTimeRegistry().getIndexes().entrySet()) {
            CodegenMethod addIndex = registerIndexCodegen(index, method, classScope, symbols);
            method.getBlock().expression(localMethod(addIndex));
        }
        return method;
    }

    private static CodegenMethod makeInitTablesOptional(CodegenClassScope classScope, ModuleCompileTimeServices compileTimeServices) {
        if (compileTimeServices.getTableCompileTimeRegistry().getTables().isEmpty()) {
            return null;
        }
        ModuleTableInitializeSymbol symbols = new ModuleTableInitializeSymbol();
        CodegenMethod method = CodegenMethod.makeParentNode(EPTypePremade.VOID.getEPType(), EPCompilerImpl.class, symbols, classScope).addParam(EPModuleTableInitServices.EPTYPE, ModuleTableInitializeSymbol.REF_INITSVC.getRef());
        for (Map.Entry<String, TableMetaData> table : compileTimeServices.getTableCompileTimeRegistry().getTables().entrySet()) {
            CodegenMethod addTable = registerTableCodegen(table, method, classScope, symbols);
            method.getBlock().expression(localMethod(addTable));
        }
        return method;
    }

    private static CodegenMethod makeInitNamedWindowsOptional(CodegenClassScope classScope, ModuleCompileTimeServices compileTimeServices) {
        if (compileTimeServices.getNamedWindowCompileTimeRegistry().getNamedWindows().isEmpty()) {
            return null;
        }
        ModuleNamedWindowInitializeSymbol symbols = new ModuleNamedWindowInitializeSymbol();
        CodegenMethod method = CodegenMethod.makeParentNode(EPTypePremade.VOID.getEPType(), EPCompilerImpl.class, symbols, classScope).addParam(EPModuleNamedWindowInitServices.EPTYPE, ModuleNamedWindowInitializeSymbol.REF_INITSVC.getRef());
        for (Map.Entry<String, NamedWindowMetaData> namedWindow : compileTimeServices.getNamedWindowCompileTimeRegistry().getNamedWindows().entrySet()) {
            CodegenMethod addNamedWindow = registerNamedWindowCodegen(namedWindow, method, classScope, symbols);
            method.getBlock().expression(localMethod(addNamedWindow));
        }
        return method;
    }

    private static void makeStatementsMethod(CodegenMethod statementsMethod, List<String> statementClassNames, CodegenClassScope classScope) {
        CodegenExpression returnValue;
        if (statementClassNames.isEmpty()) {
            returnValue = staticMethod(Collections.class, "emptyList");
        } else if (statementClassNames.size() == 1) {
            returnValue = staticMethod(Collections.class, "singletonList", newInstance(statementClassNames.get(0)));
        } else {
            statementsMethod.getBlock().declareVar(EPTypePremade.LIST.getEPType(), "statements", newInstance(EPTypePremade.ARRAYLIST.getEPType(), constant(statementClassNames.size())));
            if (statementClassNames.size() <= NUM_STATEMENT_NAMES_PER_BATCH) {
                makeStatementsAdd(statementsMethod, statementClassNames);
            } else {
                // subdivide to N each
                List<List<String>> lists = CollectionUtil.subdivide(statementClassNames, NUM_STATEMENT_NAMES_PER_BATCH);
                for (List<String> names : lists) {
                    CodegenMethod sub = statementsMethod.makeChild(EPTypePremade.VOID.getEPType(), CompilerHelperModuleProvider.class, classScope).addParam(EPTypePremade.LIST.getEPType(), "statements");
                    makeStatementsAdd(sub, names);
                    statementsMethod.getBlock().localMethod(sub, ref("statements"));
                }
            }
            returnValue = ref("statements");
        }
        statementsMethod.getBlock().methodReturn(returnValue);
    }

    private static void makeStatementsAdd(CodegenMethod statementsMethod, Collection<String> statementClassNames) {
        for (String statementClassName : statementClassNames) {
            statementsMethod.getBlock().exprDotMethod(ref("statements"), "add", CodegenExpressionBuilder.newInstance(statementClassName));
        }
    }

    private static void makeModuleProperties(Map<ModuleProperty, Object> props, CodegenMethod method) {
        if (props.isEmpty()) {
            method.getBlock().methodReturn(staticMethod(Collections.class, "emptyMap"));
            return;
        }
        if (props.size() == 1) {
            Map.Entry<ModuleProperty, Object> entry = props.entrySet().iterator().next();
            method.getBlock().methodReturn(staticMethod(Collections.class, "singletonMap", makeModulePropKey(entry.getKey()), makeModulePropValue(entry.getValue())));
            return;
        }
        method.getBlock().declareVar(EPTypePremade.MAP.getEPType(), "props", newInstance(EPTypePremade.HASHMAP.getEPType(), constant(CollectionUtil.capacityHashMap(props.size()))));
        for (Map.Entry<ModuleProperty, Object> entry : props.entrySet()) {
            method.getBlock().exprDotMethod(ref("props"), "put", makeModulePropKey(entry.getKey()), makeModulePropValue(entry.getValue()));
        }
        method.getBlock().methodReturn(ref("props"));
    }

    private static CodegenExpression makeModulePropKey(ModuleProperty key) {
        return enumValue(ModuleProperty.class, key.name());
    }

    private static CodegenExpression makeModulePropValue(Object value) {
        return SerializerUtil.expressionForUserObject(value);
    }

    private static CodegenMethod registerClassProvidedCodegen(Map.Entry<String, ClassProvided> classProvided, CodegenMethodScope parent, CodegenClassScope classScope, ModuleClassProvidedInitializeSymbol symbols) {
        CodegenMethod method = parent.makeChild(EPTypePremade.VOID.getEPType(), EPCompilerImpl.class, classScope);
        method.getBlock()
            .expression(exprDotMethodChain(symbols.getAddInitSvc(method)).add(EPModuleClassProvidedInitServices.GETCLASSPROVIDEDCOLLECTOR)
                .add("registerClass", constant(classProvided.getKey()), classProvided.getValue().make(method, classScope)));
        return method;
    }

    private static CodegenMethod registerScriptCodegen(Map.Entry<NameAndParamNum, ExpressionScriptProvided> script, CodegenMethodScope parent, CodegenClassScope classScope, ModuleScriptInitializeSymbol symbols) {
        CodegenMethod method = parent.makeChild(EPTypePremade.VOID.getEPType(), EPCompilerImpl.class, classScope);
        method.getBlock()
            .expression(exprDotMethodChain(symbols.getAddInitSvc(method)).add(EPModuleScriptInitServices.GETSCRIPTCOLLECTOR)
                .add("registerScript", constant(script.getKey().getName()), constant(script.getKey().getParamNum()), script.getValue().make(method, classScope)));
        return method;
    }

    private static CodegenMethod registerExprDeclaredCodegen(Map.Entry<String, ExpressionDeclItem> expression, CodegenMethod parent, CodegenClassScope classScope, ModuleExpressionDeclaredInitializeSymbol symbols) {
        CodegenMethod method = parent.makeChild(EPTypePremade.VOID.getEPType(), EPCompilerImpl.class, classScope);

        ExpressionDeclItem item = expression.getValue();
        byte[] bytes = SerializerUtil.objectToByteArr(item.getOptionalSoda());
        item.setOptionalSodaBytes(() -> bytes);

        method.getBlock()
            .declareVar(ExpressionDeclItem.EPTYPE, "detail", expression.getValue().make(method, symbols, classScope))
            .expression(exprDotMethodChain(symbols.getAddInitSvc(method)).add(EPModuleExprDeclaredInitServices.GETEXPRDECLAREDCOLLECTOR)
                .add("registerExprDeclared", constant(expression.getKey()), ref("detail")));
        return method;
    }

    protected static CodegenMethod makeInitEventTypesOptional(CodegenClassScope classScope, ModuleCompileTimeServices compileTimeServices) {
        if (!hasEventTypes(compileTimeServices)) {
            return null;
        }
        ModuleEventTypeInitializeSymbol symbolsEventTypeInit = new ModuleEventTypeInitializeSymbol();
        CodegenMethod initializeEventTypesMethod = CodegenMethod.makeParentNode(EPTypePremade.VOID.getEPType(), EPCompilerImpl.class, symbolsEventTypeInit, classScope).addParam(EPModuleEventTypeInitServices.EPTYPE, ModuleEventTypeInitializeSymbol.REF_INITSVC.getRef());
        for (EventType eventType : compileTimeServices.getEventTypeCompileTimeRegistry().getNewTypesAdded()) {
            CodegenMethod addType = registerEventTypeCodegen(eventType, initializeEventTypesMethod, classScope, symbolsEventTypeInit);
            initializeEventTypesMethod.getBlock().expression(localMethod(addType));
        }

        if (compileTimeServices.getSerdeEventTypeRegistry().isTargetHA()) {
            for (Map.Entry<EventType, DataInputOutputSerdeForge> pair : compileTimeServices.getSerdeEventTypeRegistry().getEventTypes().entrySet()) {
                CodegenMethod addSerde = registerEventTypeSerdeCodegen(pair.getKey(), pair.getValue(), initializeEventTypesMethod, classScope, symbolsEventTypeInit);
                initializeEventTypesMethod.getBlock().expression(localMethod(addSerde));
            }
        }
        return initializeEventTypesMethod;
    }

    private static boolean hasEventTypes(ModuleCompileTimeServices compileTimeServices) {
        boolean has = !compileTimeServices.getEventTypeCompileTimeRegistry().getNewTypesAdded().isEmpty();
        if (!has) {
            has = !compileTimeServices.getSerdeEventTypeRegistry().getEventTypes().isEmpty();
        }
        return has;
    }

    private static CodegenMethod registerNamedWindowCodegen(Map.Entry<String, NamedWindowMetaData> namedWindow, CodegenMethodScope parent, CodegenClassScope classScope, ModuleNamedWindowInitializeSymbol symbols) {
        CodegenMethod method = parent.makeChild(EPTypePremade.VOID.getEPType(), EPCompilerImpl.class, classScope);
        method.getBlock()
            .declareVar(NamedWindowMetaData.EPTYPE, "detail", namedWindow.getValue().make(symbols.getAddInitSvc(method)))
            .expression(exprDotMethodChain(symbols.getAddInitSvc(method)).add(EPModuleNamedWindowInitServices.GETNAMEDWINDOWCOLLECTOR).add("registerNamedWindow",
                constant(namedWindow.getKey()), ref("detail")));
        return method;
    }

    private static CodegenMethod registerTableCodegen(Map.Entry<String, TableMetaData> table, CodegenMethodScope parent, CodegenClassScope classScope, ModuleTableInitializeSymbol symbols) {
        CodegenMethod method = parent.makeChild(EPTypePremade.VOID.getEPType(), EPCompilerImpl.class, classScope);
        method.getBlock()
            .declareVar(TableMetaData.EPTYPE, "detail", table.getValue().make(parent, symbols, classScope))
            .expression(exprDotMethodChain(symbols.getAddInitSvc(method)).add(EPModuleTableInitServices.GETTABLECOLLECTOR).add("registerTable",
                constant(table.getKey()), ref("detail")));
        return method;
    }

    private static CodegenMethod registerIndexCodegen(Map.Entry<IndexCompileTimeKey, IndexDetailForge> index, CodegenMethodScope parent, CodegenClassScope classScope, ModuleIndexesInitializeSymbol symbols) {
        CodegenMethod method = parent.makeChild(EPTypePremade.VOID.getEPType(), EPCompilerImpl.class, classScope);
        method.getBlock()
            .declareVar(IndexCompileTimeKey.EPTYPE, "key", index.getKey().make(symbols.getAddInitSvc(method)))
            .declareVar(IndexDetail.EPTYPE, "detail", index.getValue().make(method, symbols, classScope))
            .expression(exprDotMethodChain(symbols.getAddInitSvc(method)).add(EPModuleIndexInitServices.GETINDEXCOLLECTOR)
                .add("registerIndex", ref("key"), ref("detail")));
        return method;
    }

    private static CodegenMethod registerContextCodegen(Map.Entry<String, ContextMetaData> context, CodegenMethod parent, CodegenClassScope classScope, ModuleContextInitializeSymbol symbols) {
        CodegenMethod method = parent.makeChild(EPTypePremade.VOID.getEPType(), EPCompilerImpl.class, classScope);
        method.getBlock()
            .declareVar(ContextMetaData.EPTYPE, "detail", context.getValue().make(symbols.getAddInitSvc(method)))
            .expression(exprDotMethodChain(symbols.getAddInitSvc(method)).add(EPModuleContextInitServices.GETCONTEXTCOLLECTOR)
                .add("registerContext", constant(context.getKey()), ref("detail")));
        return method;
    }

    private static CodegenMethod registerVariableCodegen(Map.Entry<String, VariableMetaData> variable, CodegenMethodScope parent, CodegenClassScope classScope, ModuleVariableInitializeSymbol symbols) {
        CodegenMethod method = parent.makeChild(EPTypePremade.VOID.getEPType(), EPCompilerImpl.class, classScope);
        method.getBlock()
            .declareVar(VariableMetaData.EPTYPE, "detail", variable.getValue().make(symbols.getAddInitSvc(method)))
            .expression(exprDotMethodChain(symbols.getAddInitSvc(method)).add(EPModuleVariableInitServices.GETVARIABLECOLLECTOR)
                .add("registerVariable", constant(variable.getKey()), ref("detail")));
        return method;
    }

    private static CodegenMethod registerEventTypeCodegen(EventType eventType, CodegenMethodScope parent, CodegenClassScope classScope, ModuleEventTypeInitializeSymbol symbols) {
        CodegenMethod method = parent.makeChild(EPTypePremade.VOID.getEPType(), EPCompilerImpl.class, classScope);

        // metadata
        method.getBlock().declareVar(EventTypeMetadata.EPTYPE, "metadata", eventType.getMetadata().toExpression());

        if (eventType instanceof JsonEventType) {
            JsonEventType jsonEventType = (JsonEventType) eventType;
            method.getBlock().declareVar(EPTypePremade.LINKEDHASHMAP.getEPType(), "props", localMethod(makePropsCodegen(jsonEventType.getTypes(), method, symbols, classScope, () -> jsonEventType.getDeepSuperTypes())));
            String[] superTypeNames = getSupertypeNames(jsonEventType);
            CodegenExpression detailExpr = jsonEventType.getDetail().toExpression(method, classScope);
            method.getBlock().expression(exprDotMethodChain(symbols.getAddInitSvc(method)).add(EPModuleEventTypeInitServices.GETEVENTTYPECOLLECTOR).add("registerJson", ref("metadata"), ref("props"),
                constant(superTypeNames), constant(jsonEventType.getStartTimestampPropertyName()), constant(jsonEventType.getEndTimestampPropertyName()), detailExpr));
        } else if (eventType instanceof BaseNestableEventType) {
            BaseNestableEventType baseNestable = (BaseNestableEventType) eventType;
            method.getBlock().declareVar(EPTypePremade.LINKEDHASHMAP.getEPType(), "props", localMethod(makePropsCodegen(baseNestable.getTypes(), method, symbols, classScope, () -> baseNestable.getDeepSuperTypes())));
            String registerMethodName = eventType instanceof MapEventType ? "registerMap" : "registerObjectArray";
            String[] superTypeNames = getSupertypeNames(baseNestable);
            method.getBlock().expression(exprDotMethodChain(symbols.getAddInitSvc(method)).add(EPModuleEventTypeInitServices.GETEVENTTYPECOLLECTOR).add(registerMethodName, ref("metadata"), ref("props"),
                constant(superTypeNames), constant(baseNestable.getStartTimestampPropertyName()), constant(baseNestable.getEndTimestampPropertyName())));
        } else if (eventType instanceof WrapperEventType) {
            WrapperEventType wrapper = (WrapperEventType) eventType;
            method.getBlock().declareVar(EventType.EPTYPE, "inner", EventTypeUtility.resolveTypeCodegen(((WrapperEventType) eventType).getUnderlyingEventType(), symbols.getAddInitSvc(method)));
            method.getBlock().declareVar(EPTypePremade.LINKEDHASHMAP.getEPType(), "props", localMethod(makePropsCodegen(wrapper.getUnderlyingMapType().getTypes(), method, symbols, classScope, () -> wrapper.getUnderlyingMapType().getDeepSuperTypes())));
            method.getBlock().expression(exprDotMethodChain(symbols.getAddInitSvc(method)).add(EPModuleEventTypeInitServices.GETEVENTTYPECOLLECTOR).add("registerWrapper", ref("metadata"), ref("inner"), ref("props")));
        } else if (eventType instanceof BeanEventType) {
            BeanEventType beanType = (BeanEventType) eventType;
            CodegenExpression superTypes = makeSupertypes(beanType.getSuperTypes(), symbols.getAddInitSvc(method));
            CodegenExpression deepSuperTypes = makeDeepSupertypes(beanType.getDeepSuperTypesAsSet(), method, symbols, classScope);
            method.getBlock().expression(exprDotMethodChain(symbols.getAddInitSvc(method)).add(EPModuleEventTypeInitServices.GETEVENTTYPECOLLECTOR).add("registerBean", ref("metadata"),
                constant(beanType.getUnderlyingEPType()),
                constant(beanType.getStartTimestampPropertyName()), constant(beanType.getEndTimestampPropertyName()),
                superTypes, deepSuperTypes));
        } else if (eventType instanceof SchemaXMLEventType && ((SchemaXMLEventType) eventType).getRepresentsFragmentOfProperty() != null) {
            SchemaXMLEventType xmlType = (SchemaXMLEventType) eventType;
            method.getBlock().expression(exprDotMethodChain(symbols.getAddInitSvc(method)).add(EPModuleEventTypeInitServices.GETEVENTTYPECOLLECTOR).add("registerXML", ref("metadata"),
                constant(xmlType.getRepresentsFragmentOfProperty()), constant(xmlType.getRepresentsOriginalTypeName())));
        } else if (eventType instanceof BaseXMLEventType) {
            BaseXMLEventType xmlType = (BaseXMLEventType) eventType;
            method.getBlock().expression(exprDotMethodChain(symbols.getAddInitSvc(method)).add(EPModuleEventTypeInitServices.GETEVENTTYPECOLLECTOR).add("registerXMLNewType", ref("metadata"),
                xmlType.getConfigurationEventTypeXMLDOM().toExpression(method, classScope)));
        } else if (eventType instanceof AvroSchemaEventType) {
            AvroSchemaEventType avroType = (AvroSchemaEventType) eventType;
            String[] superTypeNames = getSupertypeNames(avroType);
            method.getBlock().expression(exprDotMethodChain(symbols.getAddInitSvc(method)).add(EPModuleEventTypeInitServices.GETEVENTTYPECOLLECTOR).add("registerAvro", ref("metadata"),
                constant(avroType.getSchema().toString()), constant(superTypeNames)));
        } else if (eventType instanceof VariantEventType) {
            VariantEventType variantEventType = (VariantEventType) eventType;
            method.getBlock().expression(exprDotMethodChain(symbols.getAddInitSvc(method)).add(EPModuleEventTypeInitServices.GETEVENTTYPECOLLECTOR).add("registerVariant", ref("metadata"),
                EventTypeUtility.resolveTypeArrayCodegen(variantEventType.getVariants(), symbols.getAddInitSvc(method)), constant(variantEventType.isVariantAny())));
        } else {
            throw new IllegalStateException("Event type '" + eventType + "' cannot be registered");
        }

        return method;
    }

    private static String[] getSupertypeNames(EventType eventType) {
        if (eventType.getSuperTypes() != null && eventType.getSuperTypes().length > 0) {
            String[] superTypeNames = new String[eventType.getSuperTypes().length];
            for (int i = 0; i < eventType.getSuperTypes().length; i++) {
                superTypeNames[i] = eventType.getSuperTypes()[i].getName();
            }
            return superTypeNames;
        }
        return new String[0];
    }

    private static CodegenMethod registerEventTypeSerdeCodegen(EventType eventType, DataInputOutputSerdeForge serdeForge, CodegenMethodScope parent, CodegenClassScope classScope, ModuleEventTypeInitializeSymbol symbols) {
        CodegenMethod method = parent.makeChild(EPTypePremade.VOID.getEPType(), EPCompilerImpl.class, classScope);
        method.getBlock()
            .declareVar(EventTypeMetadata.EPTYPE, "metadata", eventType.getMetadata().toExpression())
            .declareVar(EventTypeResolver.EPTYPE, "resolver", exprDotMethod(symbols.getAddInitSvc(method), EPModuleEventTypeInitServices.GETEVENTTYPERESOLVER))
            .declareVar(DataInputOutputSerde.EPTYPE, "serde", serdeForge.codegen(method, classScope, ref("resolver")));
        method.getBlock().expression(exprDotMethodChain(symbols.getAddInitSvc(method)).add(EPModuleEventTypeInitServices.GETEVENTTYPECOLLECTOR).add("registerSerde", ref("metadata"), ref("serde"), constant(eventType.getUnderlyingEPType())));
        return method;
    }

    private static CodegenExpression makeDeepSupertypes(Set<EventType> deepSuperTypes, CodegenMethodScope parent, ModuleEventTypeInitializeSymbol symbols, CodegenClassScope classScope) {
        if (deepSuperTypes == null || deepSuperTypes.isEmpty()) {
            return staticMethod(Collections.class, "emptySet");
        }
        if (deepSuperTypes.size() == 1) {
            return staticMethod(Collections.class, "singleton", EventTypeUtility.resolveTypeCodegen(deepSuperTypes.iterator().next(), symbols.getAddInitSvc(parent)));
        }
        CodegenMethod method = parent.makeChild(EPTypePremade.SET.getEPType(), CompilerHelperModuleProvider.class, classScope);
        method.getBlock().declareVar(EPTypePremade.SET.getEPType(), "dst", newInstance(EPTypePremade.LINKEDHASHSET.getEPType(), constant(CollectionUtil.capacityHashMap(deepSuperTypes.size()))));
        for (EventType eventType : deepSuperTypes) {
            method.getBlock().exprDotMethod(ref("dst"), "add", EventTypeUtility.resolveTypeCodegen(eventType, symbols.getAddInitSvc(method)));
        }
        method.getBlock().methodReturn(ref("dst"));
        return localMethod(method);
    }

    private static CodegenExpression makeSupertypes(EventType[] superTypes, CodegenExpressionRef initSvcRef) {
        if (superTypes == null || superTypes.length == 0) {
            return constantNull();
        }
        CodegenExpression[] expressions = new CodegenExpression[superTypes.length];
        for (int i = 0; i < superTypes.length; i++) {
            expressions[i] = EventTypeUtility.resolveTypeCodegen(superTypes[i], initSvcRef);
        }
        return newArrayWithInit(EventType.EPTYPE, expressions);
    }

    private static CodegenMethod makePropsCodegen(Map<String, Object> types, CodegenMethodScope parent, ModuleEventTypeInitializeSymbol symbols, CodegenClassScope classScope, Supplier<Iterator<EventType>> deepSuperTypes) {
        CodegenMethod method = parent.makeChild(EPTypePremade.LINKEDHASHMAP.getEPType(), CompilerHelperModuleProvider.class, classScope);
        symbols.getAddInitSvc(method);

        method.getBlock().declareVar(EPTypePremade.LINKEDHASHMAP.getEPType(), "props", newInstance(EPTypePremade.LINKEDHASHMAP.getEPType()));
        for (Map.Entry<String, Object> entry : types.entrySet()) {
            boolean propertyOfSupertype = isPropertyOfSupertype(deepSuperTypes, entry.getKey());
            if (propertyOfSupertype) {
                continue;
            }

            Object type = entry.getValue();
            CodegenExpression typeResolver;
            if (type instanceof EPType) {
                EPType eptype = (EPType) type;
                typeResolver = CodegenExpressionEPType.toExpression(eptype);
            } else if (type instanceof EventType) {
                EventType innerType = (EventType) type;
                typeResolver = EventTypeUtility.resolveTypeCodegen(innerType, ModuleEventTypeInitializeSymbol.REF_INITSVC);
            } else if (type instanceof EventType[]) {
                EventType[] innerType = (EventType[]) type;
                CodegenExpression typeExpr = EventTypeUtility.resolveTypeCodegen(innerType[0], ModuleEventTypeInitializeSymbol.REF_INITSVC);
                typeResolver = newArrayWithInit(EventType.EPTYPE, typeExpr);
            } else if (type == null) {
                typeResolver = constantNull();
            } else if (type instanceof TypeBeanOrUnderlying) {
                EventType innerType = ((TypeBeanOrUnderlying) type).getEventType();
                CodegenExpression innerTypeExpr = EventTypeUtility.resolveTypeCodegen(innerType, ModuleEventTypeInitializeSymbol.REF_INITSVC);
                typeResolver = newInstance(TypeBeanOrUnderlying.EPTYPE, innerTypeExpr);
            } else if (type instanceof TypeBeanOrUnderlying[]) {
                EventType innerType = ((TypeBeanOrUnderlying[]) type)[0].getEventType();
                CodegenExpression innerTypeExpr = EventTypeUtility.resolveTypeCodegen(innerType, ModuleEventTypeInitializeSymbol.REF_INITSVC);
                typeResolver = newArrayWithInit(TypeBeanOrUnderlying.EPTYPE, newInstance(TypeBeanOrUnderlying.EPTYPE, innerTypeExpr));
            } else if (type instanceof Map) {
                typeResolver = localMethod(makePropsCodegen((Map<String, Object>) type, parent, symbols, classScope, null));
            } else {
                throw new IllegalStateException("Unrecognized type '" + type + "'");
            }
            method.getBlock().exprDotMethod(ref("props"), "put", constant(entry.getKey()), typeResolver);
        }
        method.getBlock().methodReturn(ref("props"));
        return method;
    }

    private static boolean isPropertyOfSupertype(Supplier<Iterator<EventType>> deepSuperTypes, String key) {
        if (deepSuperTypes == null) {
            return false;
        }
        Iterator<EventType> deepSuperTypesIterator = deepSuperTypes.get();
        while (deepSuperTypesIterator.hasNext()) {
            EventType type = deepSuperTypesIterator.next();
            if (type.isProperty(key)) {
                return true;
            }
        }
        return false;
    }
}
