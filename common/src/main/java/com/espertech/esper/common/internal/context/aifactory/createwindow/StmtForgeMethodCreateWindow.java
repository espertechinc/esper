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
package com.espertech.esper.common.internal.context.aifactory.createwindow;

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.client.annotation.HintEnum;
import com.espertech.esper.common.client.util.StatementProperty;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenPackageScope;
import com.espertech.esper.common.internal.bytecodemodel.core.CodeGenerationIDGenerator;
import com.espertech.esper.common.internal.compile.stage1.spec.CreateWindowDesc;
import com.espertech.esper.common.internal.compile.stage1.spec.SelectClauseElementWildcard;
import com.espertech.esper.common.internal.compile.stage1.spec.SelectClauseStreamSelectorEnum;
import com.espertech.esper.common.internal.compile.stage1.spec.ViewSpec;
import com.espertech.esper.common.internal.compile.stage2.StatementSpecCompiled;
import com.espertech.esper.common.internal.compile.stage3.*;
import com.espertech.esper.common.internal.context.activator.ViewableActivatorFilterForge;
import com.espertech.esper.common.internal.context.aifactory.select.StreamJoinAnalysisResultCompileTime;
import com.espertech.esper.common.internal.context.module.StatementAIFactoryProvider;
import com.espertech.esper.common.internal.context.module.StatementFields;
import com.espertech.esper.common.internal.context.module.StatementInformationalsCompileTime;
import com.espertech.esper.common.internal.context.module.StatementProvider;
import com.espertech.esper.common.internal.epl.expression.core.*;
import com.espertech.esper.common.internal.epl.namedwindow.path.NamedWindowMetaData;
import com.espertech.esper.common.internal.epl.resultset.core.ResultSetProcessorDesc;
import com.espertech.esper.common.internal.epl.resultset.core.ResultSetProcessorFactoryFactory;
import com.espertech.esper.common.internal.epl.resultset.core.ResultSetProcessorFactoryProvider;
import com.espertech.esper.common.internal.epl.resultset.core.ResultSetSpec;
import com.espertech.esper.common.internal.epl.resultset.select.core.SelectSubscriberDescriptor;
import com.espertech.esper.common.internal.epl.streamtype.StreamTypeService;
import com.espertech.esper.common.internal.epl.streamtype.StreamTypeServiceImpl;
import com.espertech.esper.common.internal.epl.virtualdw.VirtualDWViewFactoryForge;
import com.espertech.esper.common.internal.schedule.ScheduleHandleCallbackProvider;
import com.espertech.esper.common.internal.view.core.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.espertech.esper.common.internal.epl.namedwindow.core.NamedWindowManagementService.ERROR_MSG_DATAWINDOWS;

public class StmtForgeMethodCreateWindow implements StmtForgeMethod {
    private final StatementBaseInfo base;

    public StmtForgeMethodCreateWindow(StatementBaseInfo base) {
        this.base = base;
    }

    public StmtForgeMethodResult make(String packageName, String classPostfix, StatementCompileTimeServices services) throws ExprValidationException {
        try {
            return build(packageName, classPostfix, services);
        } catch (ExprValidationException ex) {
            throw ex;
        } catch (Throwable t) {
            throw new ExprValidationException("Unexpected exception creating named window '" + base.getStatementSpec().getRaw().getCreateWindowDesc().getWindowName() + "': " + t.getMessage(), t);
        }
    }

    private StmtForgeMethodResult build(String packageName, String classPostfix, StatementCompileTimeServices services) throws ExprValidationException {

        List<StmtClassForgeableFactory> additionalForgeables = new ArrayList<>(2);
        CreateWindowCompileResult compileResult = CreateWindowUtil.handleCreateWindow(base, services);
        additionalForgeables.addAll(compileResult.getAdditionalForgeables());
        EventType namedWindowType = compileResult.getFilterSpecCompiled().getFilterForEventType();

        // view must be non-empty list
        CreateWindowDesc createWindowDesc = base.getStatementSpec().getRaw().getCreateWindowDesc();
        if (createWindowDesc.getViewSpecs().isEmpty()) {
            throw new ExprValidationException(ERROR_MSG_DATAWINDOWS);
        }

        if (services.getNamedWindowCompileTimeResolver().resolve(createWindowDesc.getWindowName()) != null) {
            throw new ExprValidationException("Named window named '" + createWindowDesc.getWindowName() + "' has already been declared");
        }

        // build forge
        ViewableActivatorFilterForge activator = new ViewableActivatorFilterForge(compileResult.getFilterSpecCompiled(), false, 0, false, -1);

        List<ViewSpec> viewSpecs = createWindowDesc.getViewSpecs();
        ViewFactoryForgeArgs viewArgs = new ViewFactoryForgeArgs(0, false, -1, createWindowDesc.getStreamSpecOptions(), createWindowDesc.getWindowName(), base.getStatementRawInfo(), services);
        ViewFactoryForgeDesc viewForgeDesc = ViewFactoryForgeUtil.createForges(viewSpecs.toArray(new ViewSpec[viewSpecs.size()]), viewArgs, namedWindowType);
        additionalForgeables.addAll(viewForgeDesc.getMultikeyForges());

        List<ViewFactoryForge> viewForges = viewForgeDesc.getForges();
        List<ScheduleHandleCallbackProvider> schedules = new ArrayList<>();
        ViewFactoryForgeUtil.determineViewSchedules(viewForges, schedules);
        verifyDataWindowViewFactoryChain(viewForges);
        Set<String> optionalUniqueKeyProps = StreamJoinAnalysisResultCompileTime.getUniqueCandidateProperties(viewForges, base.getStatementSpec().getAnnotations());
        String[] uniqueKeyProArray = optionalUniqueKeyProps == null ? null : optionalUniqueKeyProps.toArray(new String[optionalUniqueKeyProps.size()]);

        NamedWindowMetaData insertFromNamedWindow = null;
        ExprNode insertFromFilter = null;
        if (createWindowDesc.isInsert() || createWindowDesc.getInsertFilter() != null) {
            String name = createWindowDesc.getAsEventTypeName();
            insertFromNamedWindow = services.getNamedWindowCompileTimeResolver().resolve(name);
            if (insertFromNamedWindow == null) {
                throw new ExprValidationException("A named window by name '" + name + "' could not be located, the insert-keyword requires an existing named window");
            }
            insertFromFilter = createWindowDesc.getInsertFilter();

            if (insertFromFilter != null) {
                String checkMinimal = ExprNodeUtilityValidate.isMinimalExpression(insertFromFilter);
                if (checkMinimal != null) {
                    throw new ExprValidationException("Create window where-clause may not have " + checkMinimal);
                }
                StreamTypeService streamTypeService = new StreamTypeServiceImpl(insertFromNamedWindow.getEventType(), name, true);
                ExprValidationContext validationContext = new ExprValidationContextBuilder(streamTypeService, base.getStatementRawInfo(), services).build();
                insertFromFilter = ExprNodeUtilityValidate.getValidatedSubtree(ExprNodeOrigin.CREATEWINDOWFILTER, insertFromFilter, validationContext);
            }
        }

        // handle output format
        StatementSpecCompiled defaultSelectAllSpec = new StatementSpecCompiled();
        defaultSelectAllSpec.getSelectClauseCompiled().setSelectExprList(new SelectClauseElementWildcard());
        defaultSelectAllSpec.getRaw().setSelectStreamDirEnum(SelectClauseStreamSelectorEnum.RSTREAM_ISTREAM_BOTH);
        StreamTypeService typeService = new StreamTypeServiceImpl(new EventType[]{namedWindowType}, new String[]{createWindowDesc.getWindowName()}, new boolean[]{true}, false, false);
        ResultSetProcessorDesc resultSetProcessor = ResultSetProcessorFactoryFactory.getProcessorPrototype(new ResultSetSpec(defaultSelectAllSpec),
                typeService, null, new boolean[1], false, base.getContextPropertyRegistry(), false, false, base.getStatementRawInfo(), services);
        String classNameRSP = CodeGenerationIDGenerator.generateClassNameSimple(ResultSetProcessorFactoryProvider.class, classPostfix);
        SelectSubscriberDescriptor selectSubscriberDescriptor = resultSetProcessor.getSelectSubscriberDescriptor();

        StatementAgentInstanceFactoryCreateNWForge forge = new StatementAgentInstanceFactoryCreateNWForge(activator, createWindowDesc.getWindowName(), viewForges,
                insertFromNamedWindow, insertFromFilter, compileResult.getAsEventType(), classNameRSP);

        // add named window
        boolean isBatchingDataWindow = determineBatchingDataWindow(viewForges);
        boolean virtualDataWindow = viewForges.get(0) instanceof VirtualDWViewFactoryForge;
        boolean isEnableIndexShare = virtualDataWindow || HintEnum.ENABLE_WINDOW_SUBQUERY_INDEXSHARE.getHint(base.getStatementSpec().getAnnotations()) != null;
        NamedWindowMetaData metaData = new NamedWindowMetaData(namedWindowType, base.getModuleName(), base.getContextName(), uniqueKeyProArray, isBatchingDataWindow, isEnableIndexShare, compileResult.getAsEventType(), virtualDataWindow);
        services.getNamedWindowCompileTimeRegistry().newNamedWindow(metaData);

        // build forge list
        List<StmtClassForgeable> forgeables = new ArrayList<>(2);

        String statementFieldsClassName = CodeGenerationIDGenerator.generateClassNameSimple(StatementFields.class, classPostfix);
        CodegenPackageScope packageScope = new CodegenPackageScope(packageName, statementFieldsClassName, services.isInstrumented());

        for (StmtClassForgeableFactory additional : additionalForgeables) {
            forgeables.add(additional.make(packageScope, classPostfix));
        }

        forgeables.add(new StmtClassForgeableRSPFactoryProvider(classNameRSP, resultSetProcessor, packageScope, base.getStatementRawInfo()));

        String aiFactoryProviderClassName = CodeGenerationIDGenerator.generateClassNameSimple(StatementAIFactoryProvider.class, classPostfix);
        StmtClassForgeableAIFactoryProviderCreateNW aiFactoryForgeable = new StmtClassForgeableAIFactoryProviderCreateNW(aiFactoryProviderClassName, packageScope, forge, createWindowDesc.getWindowName());
        forgeables.add(aiFactoryForgeable);

        String statementProviderClassName = CodeGenerationIDGenerator.generateClassNameSimple(StatementProvider.class, classPostfix);
        StatementInformationalsCompileTime informationals = StatementInformationalsUtil.getInformationals(base, Collections.singletonList(compileResult.getFilterSpecCompiled()), schedules, Collections.emptyList(), true, selectSubscriberDescriptor, packageScope, services);
        informationals.getProperties().put(StatementProperty.CREATEOBJECTNAME, createWindowDesc.getWindowName());

        forgeables.add(new StmtClassForgeableStmtProvider(aiFactoryProviderClassName, statementProviderClassName, informationals, packageScope));
        forgeables.add(new StmtClassForgeableStmtFields(statementFieldsClassName, packageScope, 1));

        return new StmtForgeMethodResult(forgeables, Collections.singletonList(compileResult.getFilterSpecCompiled()), schedules, Collections.emptyList(), Collections.emptyList());
    }

    private static boolean determineBatchingDataWindow(List<ViewFactoryForge> forges) {
        for (ViewFactoryForge forge : forges) {
            if (forge instanceof DataWindowBatchingViewForge) {
                return true;
            }
        }
        return false;
    }

    private void verifyDataWindowViewFactoryChain(List<ViewFactoryForge> forges) throws ExprValidationException {
        AtomicBoolean hasDataWindow = new AtomicBoolean();
        ViewForgeVisitor visitor = forge -> {
            if (forge instanceof DataWindowViewForge) {
                hasDataWindow.set(true);
            }
        };

        for (ViewFactoryForge forge : forges) {
            forge.accept(visitor);
        }
        if (!hasDataWindow.get()) {
            throw new ExprValidationException(ERROR_MSG_DATAWINDOWS);
        }
    }
}
