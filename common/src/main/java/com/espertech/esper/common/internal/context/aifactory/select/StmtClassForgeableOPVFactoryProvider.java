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
package com.espertech.esper.common.internal.context.aifactory.select;

import com.espertech.esper.common.client.EPException;
import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.client.type.EPTypePremade;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenPackageScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenSymbolProviderEmpty;
import com.espertech.esper.common.internal.bytecodemodel.core.*;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder;
import com.espertech.esper.common.internal.bytecodemodel.util.CodegenStackGenerator;
import com.espertech.esper.common.internal.compile.stage2.StatementRawInfo;
import com.espertech.esper.common.internal.compile.stage3.StmtClassForgeable;
import com.espertech.esper.common.internal.compile.stage3.StmtClassForgeableType;
import com.espertech.esper.common.internal.context.aifactory.core.SAIFFInitializeSymbol;
import com.espertech.esper.common.internal.context.module.EPStatementInitServices;
import com.espertech.esper.common.internal.context.util.AgentInstanceContext;
import com.espertech.esper.common.internal.context.util.AgentInstanceStopServices;
import com.espertech.esper.common.internal.context.util.StatementResultService;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.common.internal.epl.output.condition.OutputCondition;
import com.espertech.esper.common.internal.epl.output.core.OutputProcessView;
import com.espertech.esper.common.internal.epl.output.core.OutputProcessViewFactory;
import com.espertech.esper.common.internal.epl.output.core.OutputProcessViewFactoryForge;
import com.espertech.esper.common.internal.epl.output.core.OutputProcessViewFactoryProvider;
import com.espertech.esper.common.internal.epl.resultset.core.ResultSetProcessor;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;
import static com.espertech.esper.common.internal.epl.output.core.OutputProcessViewCodegenNames.*;
import static com.espertech.esper.common.internal.epl.resultset.codegen.ResultSetProcessorCodegenNames.NAME_NEWDATA;
import static com.espertech.esper.common.internal.epl.resultset.codegen.ResultSetProcessorCodegenNames.NAME_OLDDATA;

public class StmtClassForgeableOPVFactoryProvider implements StmtClassForgeable {
    private final static String MEMBERNAME_OPVFACTORY = "opvFactory";
    private final static String CLASSNAME_OUTPUTPROCESSVIEWFACTORY = "OPVFactory";
    private final static String CLASSNAME_OUTPUTPROCESSVIEW = "OPV";
    private final static String MEMBERNAME_STATEMENTRESULTSVC = "statementResultService";

    private final String className;
    private final OutputProcessViewFactoryForge spec;
    private final CodegenPackageScope packageScope;
    private final int numStreams;
    private final StatementRawInfo raw;

    public StmtClassForgeableOPVFactoryProvider(String className, OutputProcessViewFactoryForge spec, CodegenPackageScope packageScope, int numStreams, StatementRawInfo raw) {
        this.className = className;
        this.spec = spec;
        this.packageScope = packageScope;
        this.numStreams = numStreams;
        this.raw = raw;
    }

    public CodegenClass forge(boolean includeDebugSymbols, boolean fireAndForget) {
        Supplier<String> debugInformationProvider = new Supplier<String>() {
            public String get() {
                StringWriter writer = new StringWriter();
                raw.appendCodeDebugInfo(writer);
                writer.append(" output-processor ").append(spec.getClass().getSimpleName());
                return writer.toString();
            }
        };

        try {
            List<CodegenInnerClass> innerClasses = new ArrayList<>();

            // build ctor
            List<CodegenTypedParam> ctorParms = new ArrayList<>();
            ctorParms.add(new CodegenTypedParam(EPStatementInitServices.EPTYPE, EPStatementInitServices.REF.getRef(), false));
            CodegenCtor providerCtor = new CodegenCtor(StmtClassForgeableOPVFactoryProvider.class, includeDebugSymbols, ctorParms);
            CodegenClassScope classScope = new CodegenClassScope(includeDebugSymbols, packageScope, className);
            List<CodegenTypedParam> providerExplicitMembers = new ArrayList<>();
            providerExplicitMembers.add(new CodegenTypedParam(OutputProcessViewFactory.EPTYPE, MEMBERNAME_OPVFACTORY));

            if (spec.isCodeGenerated()) {
                // make factory and view both, assign to member
                providerExplicitMembers.add(new CodegenTypedParam(StatementResultService.EPTYPE, MEMBERNAME_STATEMENTRESULTSVC));
                makeOPVFactory(classScope, innerClasses, providerCtor, className);
                makeOPV(classScope, innerClasses, Collections.emptyList(), providerCtor, className, spec, numStreams);
            } else {
                // build factory from existing classes
                SAIFFInitializeSymbol symbols = new SAIFFInitializeSymbol();
                CodegenMethod init = providerCtor.makeChildWithScope(OutputProcessViewFactory.EPTYPE, this.getClass(), symbols, classScope).addParam(EPStatementInitServices.EPTYPE, EPStatementInitServices.REF.getRef());
                spec.provideCodegen(init, symbols, classScope);
                providerCtor.getBlock().assignMember(MEMBERNAME_OPVFACTORY, localMethod(init, EPStatementInitServices.REF));
            }

            // make get-factory method
            CodegenMethod getFactoryMethod = CodegenMethod.makeParentNode(OutputProcessViewFactory.EPTYPE, this.getClass(), CodegenSymbolProviderEmpty.INSTANCE, classScope);
            getFactoryMethod.getBlock().methodReturn(ref(MEMBERNAME_OPVFACTORY));

            CodegenClassMethods methods = new CodegenClassMethods();
            CodegenStackGenerator.recursiveBuildStack(providerCtor, "ctor", methods);
            CodegenStackGenerator.recursiveBuildStack(getFactoryMethod, "getOutputProcessViewFactory", methods);

            // render and compile
            return new CodegenClass(CodegenClassType.OUTPUTPROCESSVIEWFACTORYPROVIDER, OutputProcessViewFactoryProvider.EPTYPE, className, classScope, providerExplicitMembers, providerCtor, methods, innerClasses);
        } catch (Throwable t) {
            throw new EPException("Fatal exception during code-generation for " + debugInformationProvider.get() + " : " + t.getMessage(), t);
        }
    }

    public String getClassName() {
        return className;
    }

    public StmtClassForgeableType getForgeableType() {
        return StmtClassForgeableType.OPVPROVIDER;
    }

    private static void makeOPVFactory(CodegenClassScope classScope, List<CodegenInnerClass> innerClasses, CodegenCtor providerCtor, String providerClassName) {
        CodegenMethod makeViewMethod = CodegenMethod.makeParentNode(OutputProcessView.EPTYPE, StmtClassForgeableOPVFactoryProvider.class, CodegenSymbolProviderEmpty.INSTANCE, classScope)
                .addParam(ResultSetProcessor.EPTYPE, NAME_RESULTSETPROCESSOR)
                .addParam(AgentInstanceContext.EPTYPE, NAME_AGENTINSTANCECONTEXT);
        makeViewMethod.getBlock().methodReturn(CodegenExpressionBuilder.newInstance(CLASSNAME_OUTPUTPROCESSVIEW, ref("o"), MEMBER_RESULTSETPROCESSOR, MEMBER_AGENTINSTANCECONTEXT));
        CodegenClassMethods methods = new CodegenClassMethods();
        CodegenStackGenerator.recursiveBuildStack(makeViewMethod, "makeView", methods);

        List<CodegenTypedParam> ctorParams = Collections.singletonList(new CodegenTypedParam(providerClassName, "o"));
        CodegenCtor ctor = new CodegenCtor(StmtClassForgeableOPVFactoryProvider.class, classScope, ctorParams);

        CodegenInnerClass innerClass = new CodegenInnerClass(CLASSNAME_OUTPUTPROCESSVIEWFACTORY, OutputProcessViewFactory.EPTYPE, ctor, Collections.emptyList(), methods);
        innerClasses.add(innerClass);

        providerCtor.getBlock().assignMember(MEMBERNAME_OPVFACTORY, CodegenExpressionBuilder.newInstance(CLASSNAME_OUTPUTPROCESSVIEWFACTORY, ref("this")))
                .assignMember(MEMBERNAME_STATEMENTRESULTSVC, exprDotMethod(EPStatementInitServices.REF, EPStatementInitServices.GETSTATEMENTRESULTSERVICE));
    }

    private static void makeOPV(CodegenClassScope classScope, List<CodegenInnerClass> innerClasses, List<CodegenTypedParam> factoryExplicitMembers, CodegenCtor factoryCtor, String classNameParent, OutputProcessViewFactoryForge forge, int numStreams) {

        List<CodegenTypedParam> ctorParams = new ArrayList<>();
        ctorParams.add(new CodegenTypedParam(classNameParent, "o"));
        ctorParams.add(new CodegenTypedParam(ResultSetProcessor.EPTYPE, NAME_RESULTSETPROCESSOR));
        ctorParams.add(new CodegenTypedParam(AgentInstanceContext.EPTYPE, NAME_AGENTINSTANCECONTEXT));

        // make ctor code
        CodegenCtor serviceCtor = new CodegenCtor(StmtClassForgeableOPVFactoryProvider.class, classScope, ctorParams);

        // Get-Result-Type Method
        CodegenMethod getEventTypeMethod = CodegenMethod.makeParentNode(EventType.EPTYPE, forge.getClass(), CodegenSymbolProviderEmpty.INSTANCE, classScope);
        getEventTypeMethod.getBlock().methodReturn(exprDotMethod(member(NAME_RESULTSETPROCESSOR), "getResultEventType"));

        // Process-View-Result Method
        CodegenMethod updateMethod = CodegenMethod.makeParentNode(EPTypePremade.VOID.getEPType(), forge.getClass(), CodegenSymbolProviderEmpty.INSTANCE, classScope)
                .addParam(EventBean.EPTYPEARRAY, NAME_NEWDATA).addParam(EventBean.EPTYPEARRAY, NAME_OLDDATA);
        if (numStreams == 1) {
            forge.updateCodegen(updateMethod, classScope);
        } else {
            updateMethod.getBlock().methodThrowUnsupported();
        }

        // Process-Join-Result Method
        CodegenMethod processMethod = CodegenMethod.makeParentNode(EPTypePremade.VOID.getEPType(), forge.getClass(), CodegenSymbolProviderEmpty.INSTANCE, classScope)
                .addParam(EPTypePremade.SET.getEPType(), NAME_NEWDATA).addParam(EPTypePremade.SET.getEPType(), NAME_OLDDATA).addParam(ExprEvaluatorContext.EPTYPE, "not_applicable");
        if (numStreams == 1) {
            processMethod.getBlock().methodThrowUnsupported();
        } else {
            forge.processCodegen(processMethod, classScope);
        }

        // Stop-Method (generates last as other methods may allocate members)
        CodegenMethod iteratorMethod = CodegenMethod.makeParentNode(EPTypePremade.ITERATOR.getEPType(), forge.getClass(), CodegenSymbolProviderEmpty.INSTANCE, classScope);
        forge.iteratorCodegen(iteratorMethod, classScope);

        // GetNumChangesetRows-Methods (always zero for generated code)
        CodegenMethod getNumChangesetRowsMethod = CodegenMethod.makeParentNode(EPTypePremade.INTEGERPRIMITIVE.getEPType(), forge.getClass(), CodegenSymbolProviderEmpty.INSTANCE, classScope);
        getNumChangesetRowsMethod.getBlock().methodReturn(constant(0));

        // GetOptionalOutputCondition-Method (always null for generated code)
        CodegenMethod getOptionalOutputConditionMethod = CodegenMethod.makeParentNode(OutputCondition.EPTYPE, forge.getClass(), CodegenSymbolProviderEmpty.INSTANCE, classScope);
        getOptionalOutputConditionMethod.getBlock().methodReturn(constantNull());

        // Stop-Method (no action for generated code)
        CodegenMethod stopMethod = CodegenMethod.makeParentNode(EPTypePremade.VOID.getEPType(), forge.getClass(), CodegenSymbolProviderEmpty.INSTANCE, classScope).addParam(AgentInstanceStopServices.EPTYPE, "svc");

        // Terminate-Method (no action for generated code)
        CodegenMethod terminatedMethod = CodegenMethod.makeParentNode(EPTypePremade.VOID.getEPType(), forge.getClass(), CodegenSymbolProviderEmpty.INSTANCE, classScope);

        CodegenClassMethods innerMethods = new CodegenClassMethods();
        CodegenStackGenerator.recursiveBuildStack(getEventTypeMethod, "getEventType", innerMethods);
        CodegenStackGenerator.recursiveBuildStack(updateMethod, "update", innerMethods);
        CodegenStackGenerator.recursiveBuildStack(processMethod, "process", innerMethods);
        CodegenStackGenerator.recursiveBuildStack(iteratorMethod, "iterator", innerMethods);
        CodegenStackGenerator.recursiveBuildStack(getNumChangesetRowsMethod, "getNumChangesetRows", innerMethods);
        CodegenStackGenerator.recursiveBuildStack(getOptionalOutputConditionMethod, "getOptionalOutputCondition", innerMethods);
        CodegenStackGenerator.recursiveBuildStack(stopMethod, "stop", innerMethods);
        CodegenStackGenerator.recursiveBuildStack(terminatedMethod, "terminated", innerMethods);

        CodegenInnerClass innerClass = new CodegenInnerClass(CLASSNAME_OUTPUTPROCESSVIEW, OutputProcessView.EPTYPE, serviceCtor, Collections.emptyList(), innerMethods);
        innerClasses.add(innerClass);
    }
}
