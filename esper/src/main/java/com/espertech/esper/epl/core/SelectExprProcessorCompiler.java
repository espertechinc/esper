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
import com.espertech.esper.codegen.compile.CodegenClassGenerator;
import com.espertech.esper.codegen.compile.CodegenCompilerException;
import com.espertech.esper.codegen.compile.CodegenMessageUtil;
import com.espertech.esper.codegen.core.*;
import com.espertech.esper.codegen.model.expression.CodegenExpression;
import com.espertech.esper.codegen.model.method.CodegenParamSet;
import com.espertech.esper.codegen.model.method.CodegenParamSetSelectPremade;
import com.espertech.esper.event.EventAdapterService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.StringWriter;
import java.util.Collections;
import java.util.function.Supplier;

public class SelectExprProcessorCompiler {
    private static Logger log = LoggerFactory.getLogger(com.espertech.esper.epl.expression.core.ExprNodeCompiler.class);

    private final static CodegenMethodFootprint PROCESS_FP;
    static {
        PROCESS_FP = new CodegenMethodFootprint(EventBean.class, new CodegenMethodId("process"), Collections.<CodegenParamSet>singletonList(CodegenParamSetSelectPremade.INSTANCE), null);
    }

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
            CodegenContext context = new CodegenContext(includeCodeComments);
            CodegenMember memberResultEventType = context.makeAddMember(EventType.class, forge.getResultEventType());
            CodegenMember memberEventAdapterService = context.makeAddMember(EventAdapterService.class, eventAdapterService);
            CodegenExpression processExpression = forge.processCodegen(memberResultEventType, memberEventAdapterService, CodegenParamSetSelectPremade.INSTANCE, context);

            CodegenMethod processMethod = new CodegenMethod(PROCESS_FP, processExpression);
            CodegenClass clazz = new CodegenClass(SelectExprProcessor.class, context, engineImportService.getEngineURI(), processMethod);
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
