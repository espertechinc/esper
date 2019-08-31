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
package com.espertech.esper.common.internal.context.aifactory.createschema;

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.client.configuration.common.ConfigurationCommonVariantStream;
import com.espertech.esper.common.client.meta.EventTypeApplicationType;
import com.espertech.esper.common.client.meta.EventTypeIdPair;
import com.espertech.esper.common.client.meta.EventTypeMetadata;
import com.espertech.esper.common.client.meta.EventTypeTypeClass;
import com.espertech.esper.common.client.util.EventTypeBusModifier;
import com.espertech.esper.common.client.util.NameAccessModifier;
import com.espertech.esper.common.client.util.StatementProperty;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenPackageScope;
import com.espertech.esper.common.internal.bytecodemodel.core.CodeGenerationIDGenerator;
import com.espertech.esper.common.internal.compile.stage1.spec.CreateSchemaDesc;
import com.espertech.esper.common.internal.compile.stage2.StatementSpecCompiled;
import com.espertech.esper.common.internal.compile.stage3.*;
import com.espertech.esper.common.internal.context.module.StatementAIFactoryProvider;
import com.espertech.esper.common.internal.context.module.StatementInformationalsCompileTime;
import com.espertech.esper.common.internal.context.module.StatementProvider;
import com.espertech.esper.common.internal.epl.expression.core.ExprValidationException;
import com.espertech.esper.common.internal.epl.resultset.select.core.SelectSubscriberDescriptor;
import com.espertech.esper.common.internal.epl.util.EPLValidationUtil;
import com.espertech.esper.common.internal.event.core.EventTypeForgablesPair;
import com.espertech.esper.common.internal.event.core.EventTypeUtility;
import com.espertech.esper.common.internal.event.variant.VariantEventType;
import com.espertech.esper.common.internal.event.variant.VariantSpec;

import java.util.*;

public class StmtForgeMethodCreateSchema implements StmtForgeMethod {

    private final StatementBaseInfo base;

    public StmtForgeMethodCreateSchema(StatementBaseInfo base) {
        this.base = base;
    }

    public StmtForgeMethodResult make(String packageName, String classPostfix, StatementCompileTimeServices services) throws ExprValidationException {
        StatementSpecCompiled statementSpec = base.getStatementSpec();

        CreateSchemaDesc spec = statementSpec.getRaw().getCreateSchemaDesc();

        if (services.getEventTypeCompileTimeResolver().getTypeByName(spec.getSchemaName()) != null) {
            throw new ExprValidationException("Event type named '" + spec.getSchemaName() + "' has already been declared");
        }

        EPLValidationUtil.validateTableExists(services.getTableCompileTimeResolver(), spec.getSchemaName());
        EventTypeForgablesPair eventTypeForgablesPair = handleCreateSchema(spec, services);

        CodegenPackageScope packageScope = new CodegenPackageScope(packageName, null, services.isInstrumented());

        String aiFactoryProviderClassName = CodeGenerationIDGenerator.generateClassNameSimple(StatementAIFactoryProvider.class, classPostfix);
        StatementAgentInstanceFactoryCreateSchemaForge forge = new StatementAgentInstanceFactoryCreateSchemaForge(eventTypeForgablesPair.getEventType());
        StmtClassForgeableAIFactoryProviderCreateSchema aiFactoryForgeable = new StmtClassForgeableAIFactoryProviderCreateSchema(aiFactoryProviderClassName, packageScope, forge);

        SelectSubscriberDescriptor selectSubscriberDescriptor = new SelectSubscriberDescriptor();
        StatementInformationalsCompileTime informationals = StatementInformationalsUtil.getInformationals(base, Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), false, selectSubscriberDescriptor, packageScope, services);
        informationals.getProperties().put(StatementProperty.CREATEOBJECTNAME, spec.getSchemaName());
        String statementProviderClassName = CodeGenerationIDGenerator.generateClassNameSimple(StatementProvider.class, classPostfix);
        StmtClassForgeableStmtProvider stmtProvider = new StmtClassForgeableStmtProvider(aiFactoryProviderClassName, statementProviderClassName, informationals, packageScope);

        List<StmtClassForgeable> forgeables = new ArrayList<>();
        for (StmtClassForgeableFactory additional : eventTypeForgablesPair.getAdditionalForgeables()) {
            forgeables.add(additional.make(packageScope, classPostfix));
        }
        forgeables.add(aiFactoryForgeable);
        forgeables.add(stmtProvider);
        return new StmtForgeMethodResult(forgeables, Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), Collections.emptyList());
    }

    private EventTypeForgablesPair handleCreateSchema(CreateSchemaDesc spec, StatementCompileTimeServices services)
            throws ExprValidationException {

        EventTypeForgablesPair pair;
        try {
            if (spec.getAssignedType() != CreateSchemaDesc.AssignedType.VARIANT) {
                pair = EventTypeUtility.createNonVariantType(false, spec, base, services);
            } else {
                EventType eventType = handleVariantType(spec, services);
                pair = new EventTypeForgablesPair(eventType, Collections.emptyList());
            }
        } catch (RuntimeException ex) {
            throw new ExprValidationException(ex.getMessage(), ex);
        }
        return pair;
    }

    private EventType handleVariantType(CreateSchemaDesc spec, StatementCompileTimeServices services) throws ExprValidationException {
        if (spec.getCopyFrom() != null && !spec.getCopyFrom().isEmpty()) {
            throw new ExprValidationException("Copy-from types are not allowed with variant types");
        }
        String eventTypeName = spec.getSchemaName();

        // determine typing
        boolean isAny = false;
        Set<EventType> types = new LinkedHashSet<EventType>();
        for (String typeName : spec.getTypes()) {
            if (typeName.trim().equals("*")) {
                isAny = true;
            } else {
                EventType eventType = services.getEventTypeCompileTimeResolver().getTypeByName(typeName);
                if (eventType == null) {
                    throw new ExprValidationException("Event type by name '" + typeName + "' could not be found for use in variant stream by name '" + eventTypeName + "'");
                }
                types.add(eventType);
            }
        }
        EventType[] eventTypes = types.toArray(new EventType[types.size()]);
        VariantSpec variantSpec = new VariantSpec(eventTypes, isAny ? ConfigurationCommonVariantStream.TypeVariance.ANY : ConfigurationCommonVariantStream.TypeVariance.PREDEFINED);

        NameAccessModifier visibility = services.getModuleVisibilityRules().getAccessModifierEventType(base.getStatementRawInfo(), spec.getSchemaName());
        EventTypeBusModifier eventBusVisibility = services.getModuleVisibilityRules().getBusModifierEventType(base.getStatementRawInfo(), eventTypeName);
        EventTypeUtility.validateModifiers(spec.getSchemaName(), eventBusVisibility, visibility);

        EventTypeMetadata metadata = new EventTypeMetadata(eventTypeName, base.getModuleName(), EventTypeTypeClass.VARIANT, EventTypeApplicationType.VARIANT, visibility, eventBusVisibility, false, EventTypeIdPair.unassigned());
        VariantEventType variantEventType = new VariantEventType(metadata, variantSpec);
        services.getEventTypeCompileTimeRegistry().newType(variantEventType);
        return variantEventType;
    }
}
