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
package com.espertech.esper.common.internal.context.aifactory.createindex;

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.client.util.NameAccessModifier;
import com.espertech.esper.common.client.util.StatementProperty;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenPackageScope;
import com.espertech.esper.common.internal.bytecodemodel.core.CodeGenerationIDGenerator;
import com.espertech.esper.common.internal.compile.multikey.MultiKeyPlan;
import com.espertech.esper.common.internal.compile.multikey.MultiKeyPlanner;
import com.espertech.esper.common.internal.compile.stage1.spec.CreateIndexDesc;
import com.espertech.esper.common.internal.compile.stage3.*;
import com.espertech.esper.common.internal.context.module.StatementAIFactoryProvider;
import com.espertech.esper.common.internal.context.module.StatementInformationalsCompileTime;
import com.espertech.esper.common.internal.context.module.StatementProvider;
import com.espertech.esper.common.internal.epl.expression.core.ExprValidationException;
import com.espertech.esper.common.internal.epl.index.compile.IndexCompileTimeKey;
import com.espertech.esper.common.internal.epl.index.compile.IndexDetailForge;
import com.espertech.esper.common.internal.epl.join.lookup.IndexMultiKey;
import com.espertech.esper.common.internal.epl.join.queryplan.QueryPlanIndexItemForge;
import com.espertech.esper.common.internal.epl.lookup.AdvancedIndexIndexMultiKeyPart;
import com.espertech.esper.common.internal.epl.lookupplansubord.EventTableIndexUtil;
import com.espertech.esper.common.internal.epl.namedwindow.path.NamedWindowMetaData;
import com.espertech.esper.common.internal.epl.resultset.select.core.SelectSubscriberDescriptor;
import com.espertech.esper.common.internal.epl.table.compiletime.TableMetaData;
import com.espertech.esper.common.internal.epl.util.EPLValidationUtil;
import com.espertech.esper.common.internal.serde.compiletime.resolve.DataInputOutputSerdeForge;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Starts and provides the stop method for EPL statements.
 */
public class StmtForgeMethodCreateIndex implements StmtForgeMethod {

    private final StatementBaseInfo base;

    public StmtForgeMethodCreateIndex(StatementBaseInfo base) {
        this.base = base;
    }

    public StmtForgeMethodResult make(String packageName, String classPostfix, StatementCompileTimeServices services) throws ExprValidationException {

        CreateIndexDesc spec = base.getStatementSpec().getRaw().getCreateIndexDesc();

        String infraName = spec.getWindowName();
        NamedWindowMetaData namedWindow = services.getNamedWindowCompileTimeResolver().resolve(infraName);
        TableMetaData table = services.getTableCompileTimeResolver().resolve(infraName);
        if (namedWindow == null && table == null) {
            throw new ExprValidationException("A named window or table by name '" + infraName + "' does not exist");
        }
        if (namedWindow != null && table != null) {
            throw new ExprValidationException("A named window or table by name '" + infraName + "' are both found");
        }

        String infraModuleName;
        NameAccessModifier infraVisibility;
        EventType indexedEventType;
        String infraContextName;
        if (namedWindow != null) {
            infraModuleName = namedWindow.getNamedWindowModuleName();
            infraVisibility = namedWindow.getEventType().getMetadata().getAccessModifier();
            indexedEventType = namedWindow.getEventType();
            infraContextName = namedWindow.getContextName();
        } else {
            infraModuleName = table.getTableModuleName();
            infraVisibility = table.getTableVisibility();
            indexedEventType = table.getInternalEventType();
            infraContextName = table.getOptionalContextName();

            if (!table.isKeyed()) {
                throw new ExprValidationException("Tables without primary key column(s) do not allow creating an index");
            }
        }
        EPLValidationUtil.validateContextName(namedWindow == null, infraName, infraContextName, base.getStatementSpec().getRaw().getOptionalContextName(), true);

        // validate index
        QueryPlanIndexItemForge explicitIndexDesc = EventTableIndexUtil.validateCompileExplicitIndex(spec.getIndexName(), spec.isUnique(), spec.getColumns(), indexedEventType, base.getStatementRawInfo(), services);
        AdvancedIndexIndexMultiKeyPart advancedIndexDesc = explicitIndexDesc.getAdvancedIndexProvisionDesc() == null ? null : explicitIndexDesc.getAdvancedIndexProvisionDesc().getIndexDesc().getAdvancedIndexDescRuntime();
        final IndexMultiKey imk = new IndexMultiKey(spec.isUnique(), explicitIndexDesc.getHashPropsAsList(), explicitIndexDesc.getBtreePropsAsList(), advancedIndexDesc);

        // add index as a new index to module-init
        IndexCompileTimeKey indexKey = new IndexCompileTimeKey(infraModuleName, infraName, infraVisibility, namedWindow != null, spec.getIndexName(), base.getModuleName());
        services.getIndexCompileTimeRegistry().newIndex(indexKey, new IndexDetailForge(imk, explicitIndexDesc));

        // add index current named window information
        if (namedWindow != null) {
            namedWindow.addIndex(spec.getIndexName(), base.getModuleName(), imk, explicitIndexDesc.toRuntime());
        } else {
            table.addIndex(spec.getIndexName(), base.getModuleName(), imk, explicitIndexDesc.toRuntime());
        }

        // determine multikey plan
        MultiKeyPlan multiKeyPlan = MultiKeyPlanner.planMultiKey(explicitIndexDesc.getHashTypes(), false, base.getStatementRawInfo(), services.getSerdeResolver());
        explicitIndexDesc.setHashMultiKeyClasses(multiKeyPlan.getClassRef());
        DataInputOutputSerdeForge[] rangeSerdes = new DataInputOutputSerdeForge[explicitIndexDesc.getRangeProps().length];
        for (int i = 0; i < explicitIndexDesc.getRangeProps().length; i++) {
            rangeSerdes[i] = services.getSerdeResolver().serdeForIndexBtree(explicitIndexDesc.getRangeTypes()[i], base.getStatementRawInfo());
        }
        explicitIndexDesc.setRangeSerdes(rangeSerdes);

        CodegenPackageScope packageScope = new CodegenPackageScope(packageName, null, services.isInstrumented());

        String aiFactoryProviderClassName = CodeGenerationIDGenerator.generateClassNameSimple(StatementAIFactoryProvider.class, classPostfix);
        StatementAgentInstanceFactoryCreateIndexForge forge = new StatementAgentInstanceFactoryCreateIndexForge(indexedEventType, spec.getIndexName(), base.getModuleName(), explicitIndexDesc, imk, namedWindow, table);
        StmtClassForgeableAIFactoryProviderCreateIndex aiFactoryForgeable = new StmtClassForgeableAIFactoryProviderCreateIndex(aiFactoryProviderClassName, packageScope, forge);

        SelectSubscriberDescriptor selectSubscriberDescriptor = new SelectSubscriberDescriptor();
        StatementInformationalsCompileTime informationals = StatementInformationalsUtil.getInformationals(base, Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), true, selectSubscriberDescriptor, packageScope, services);
        informationals.getProperties().put(StatementProperty.CREATEOBJECTNAME, spec.getIndexName());
        String statementProviderClassName = CodeGenerationIDGenerator.generateClassNameSimple(StatementProvider.class, classPostfix);
        StmtClassForgeableStmtProvider stmtProvider = new StmtClassForgeableStmtProvider(aiFactoryProviderClassName, statementProviderClassName, informationals, packageScope);

        List<StmtClassForgeable> forgeables = new ArrayList<>();
        for (StmtClassForgeableFactory additional : multiKeyPlan.getMultiKeyForgeables()) {
            forgeables.add(additional.make(packageScope, classPostfix));
        }
        forgeables.add(aiFactoryForgeable);
        forgeables.add(stmtProvider);
        return new StmtForgeMethodResult(forgeables, Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), Collections.emptyList());
    }
}
