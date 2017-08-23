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
package com.espertech.esper.epl.expression.codegen;

import com.espertech.esper.client.EPException;
import com.espertech.esper.codegen.base.CodegenClassScope;
import com.espertech.esper.codegen.base.CodegenMethodNode;
import com.espertech.esper.codegen.compile.CodegenClassGenerator;
import com.espertech.esper.codegen.compile.CodegenCompilerException;
import com.espertech.esper.codegen.compile.CodegenMessageUtil;
import com.espertech.esper.codegen.core.CodegenClass;
import com.espertech.esper.codegen.core.CodegenClassMethods;
import com.espertech.esper.codegen.model.expression.CodegenExpression;
import com.espertech.esper.codegen.util.CodegenStackGenerator;
import com.espertech.esper.epl.core.engineimport.EngineImportService;
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.expression.core.ExprForge;
import com.espertech.esper.epl.expression.core.ExprForgeComplexityEnum;
import com.espertech.esper.epl.expression.core.ExprPrecedenceEnum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.StringWriter;
import java.util.function.Supplier;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.constantNull;

public class ExprNodeCompiler {
    private static Logger log = LoggerFactory.getLogger(ExprNodeCompiler.class);

    public static ExprEvaluator allocateEvaluator(ExprForge forge, EngineImportService engineImportService, Class compiledByClass, boolean onDemandQuery, String statementName) {
        if (!engineImportService.getCodeGeneration().isEnableExpression() || onDemandQuery || forge.getComplexity() != ExprForgeComplexityEnum.INTER) {
            return forge.getExprEvaluator();
        }
        boolean includeCodeComments = engineImportService.getCodeGeneration().isIncludeComments();

        Supplier<String> debugInformationProvider = new Supplier<String>() {
            public String get() {
                StringWriter writer = new StringWriter();
                writer.append("statement '")
                        .append(statementName)
                        .append("' expression '");
                try {
                    forge.getForgeRenderable().toEPL(writer, ExprPrecedenceEnum.MINIMUM);
                } catch (Throwable t) {
                    log.warn("Exception rendering expression: " + t.getMessage(), t);
                    writer.append("(exception rendering expression) ");
                    writer.append(forge.getClass().getSimpleName());
                }
                writer.append("' requestor-class '")
                        .append(compiledByClass.getSimpleName())
                        .append("'");
                return writer.toString();
            }
        };

        try {
            CodegenClassScope codegenClassScope = new CodegenClassScope();
            ExprForgeCodegenSymbol exprSymbol = new ExprForgeCodegenSymbol(true);
            CodegenMethodNode topNode = CodegenMethodNode.makeParentNode(Object.class, ExprNodeCompiler.class, exprSymbol).addParam(ExprForgeCodegenNames.PARAMS);

            // generate expression
            CodegenExpression expression = forge.evaluateCodegen(Object.class, topNode, exprSymbol, codegenClassScope);

            // generate code for derived symbols
            exprSymbol.derivedSymbolsCodegen(topNode, topNode.getBlock());

            // add expression to end
            if (forge.getEvaluationType() == void.class) {
                topNode.getBlock()
                        .expression(expression)
                        .methodReturn(constantNull());
            } else {
                topNode.getBlock().methodReturn(expression);
            }

            // build stack
            CodegenClassMethods methods = new CodegenClassMethods();
            CodegenStackGenerator.recursiveBuildStack(topNode, "evaluate", methods);

            CodegenClass clazz = new CodegenClass(ExprEvaluator.class, codegenClassScope, engineImportService.getEngineURI(), methods);
            return CodegenClassGenerator.compile(clazz, engineImportService, ExprEvaluator.class, debugInformationProvider);
        } catch (CodegenCompilerException ex) {
            boolean fallback = engineImportService.getCodeGeneration().isEnableFallback();
            String message = CodegenMessageUtil.getFailedCompileLogMessageWithCode(ex, debugInformationProvider, fallback);
            if (fallback) {
                log.warn(message, ex);
            } else {
                log.error(message, ex);
            }
            return handleThrowable(engineImportService, ex, forge, debugInformationProvider);
        } catch (Throwable t) {
            return handleThrowable(engineImportService, t, forge, debugInformationProvider);
        }
    }

    private static ExprEvaluator handleThrowable(EngineImportService engineImportService, Throwable t, ExprForge forge, Supplier<String> debugInformationProvider) {
        if (engineImportService.getCodeGeneration().isEnableFallback()) {
            return forge.getExprEvaluator();
        }
        throw new EPException("Fatal exception during code-generation for " + debugInformationProvider.get() + " (see error log for further details): " + t.getMessage(), t);
    }
}
