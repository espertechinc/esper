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
package com.espertech.esper.epl.core;

import com.espertech.esper.client.EPException;
import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.EventType;
import com.espertech.esper.codegen.base.CodegenClassScope;
import com.espertech.esper.codegen.base.CodegenMember;
import com.espertech.esper.codegen.base.CodegenSymbolProvider;
import com.espertech.esper.codegen.compile.CodegenClassGenerator;
import com.espertech.esper.codegen.compile.CodegenCompilerException;
import com.espertech.esper.codegen.compile.CodegenMessageUtil;
import com.espertech.esper.codegen.core.*;
import com.espertech.esper.codegen.util.CodegenStackGenerator;
import com.espertech.esper.epl.expression.codegen.ExprForgeCodegenNames;
import com.espertech.esper.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.codegen.base.CodegenMethodNode;
import com.espertech.esper.epl.expression.codegen.ExprNodeCompiler;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.event.EventAdapterService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.StringWriter;
import java.util.Map;
import java.util.function.Supplier;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.localMethod;

public class SelectExprProcessorCompiler {
    private static Logger log = LoggerFactory.getLogger(ExprNodeCompiler.class);

    public static SelectExprProcessor allocateSelectExprEvaluator(EventAdapterService eventAdapterService, SelectExprProcessorForge forge, EngineImportService engineImportService, Class compiledByClass, boolean onDemandQuery, String statementName) {
        if (!engineImportService.getCodeGeneration().isEnableSelectClause() || onDemandQuery) {
            return forge.getSelectExprProcessor(engineImportService, onDemandQuery, statementName);
        }
        boolean includeCodeComments = engineImportService.getCodeGeneration().isIncludeComments();

        Supplier<String> debugInformationProvider = new Supplier<String>() {
            public String get() {
                StringWriter writer = new StringWriter();
                writer.append("statement '")
                        .append(statementName)
                        .append("' select-clause-processor");
                writer.append(" requestor-class '")
                        .append(compiledByClass.getSimpleName())
                        .append("'");
                return writer.toString();
            }
        };

        try {
            CodegenClassScope codegenClassScope = new CodegenClassScope();
            ExprForgeCodegenSymbol exprSymbol = new ExprForgeCodegenSymbol(true);
            SelectExprProcessorCodegenSymbol selectEnv = new SelectExprProcessorCodegenSymbol();
            CodegenSymbolProvider symbolProvider = new CodegenSymbolProvider() {
                public void provide(Map<String, Class> symbols) {
                    exprSymbol.provide(symbols);
                    selectEnv.provide(symbols);
                }
            };

            CodegenMember memberResultEventType = codegenClassScope.makeAddMember(EventType.class, forge.getResultEventType());
            CodegenMember memberEventAdapterService = codegenClassScope.makeAddMember(EventAdapterService.class, eventAdapterService);

            CodegenMethodNode topNode = CodegenMethodNode.makeParentNode(EventBean.class, SelectExprProcessorCompiler.class, symbolProvider)
                    .addParam(EventBean[].class, ExprForgeCodegenNames.NAME_EPS)
                    .addParam(boolean.class, ExprForgeCodegenNames.NAME_ISNEWDATA)
                    .addParam(boolean.class, SelectExprProcessorCodegenSymbol.NAME_ISSYNTHESIZE)
                    .addParam(ExprEvaluatorContext.class, ExprForgeCodegenNames.NAME_EXPREVALCONTEXT);
            CodegenMethodNode method = forge.processCodegen(memberResultEventType, memberEventAdapterService, topNode, selectEnv, exprSymbol, codegenClassScope);
            exprSymbol.derivedSymbolsCodegen(topNode, topNode.getBlock());
            topNode.getBlock().methodReturn(localMethod(method));

            CodegenClassMethods methods = new CodegenClassMethods();
            CodegenStackGenerator.recursiveBuildStack(topNode, "process", methods);

            // render and compile
            CodegenClass clazz = new CodegenClass(SelectExprProcessor.class, codegenClassScope, engineImportService.getEngineURI(), methods);
            return CodegenClassGenerator.compile(clazz, engineImportService, SelectExprProcessor.class, debugInformationProvider);
        } catch (CodegenCompilerException ex) {
            boolean fallback = engineImportService.getCodeGeneration().isEnableFallback();
            String message = CodegenMessageUtil.getFailedCompileLogMessageWithCode(ex, debugInformationProvider, fallback);
            if (fallback) {
                log.warn(message, ex);
            } else {
                log.error(message, ex);
            }
            return handleThrowable(engineImportService, ex, forge, debugInformationProvider, onDemandQuery, statementName);
        } catch (Throwable t) {
            return handleThrowable(engineImportService, t, forge, debugInformationProvider, onDemandQuery, statementName);
        }
    }

    private static SelectExprProcessor handleThrowable(EngineImportService engineImportService, Throwable t, SelectExprProcessorForge forge, Supplier<String> debugInformationProvider, boolean isFireAndForget, String statementName) {
        if (engineImportService.getCodeGeneration().isEnableFallback()) {
            return forge.getSelectExprProcessor(engineImportService, isFireAndForget, statementName);
        }
        throw new EPException("Fatal exception during code-generation for " + debugInformationProvider.get() + " (see error log for further details): " + t.getMessage(), t);
    }
}
