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
package com.espertech.esper.common.internal.context.aifactory.createcontext;

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.client.meta.EventTypeApplicationType;
import com.espertech.esper.common.client.meta.EventTypeIdPair;
import com.espertech.esper.common.client.meta.EventTypeMetadata;
import com.espertech.esper.common.client.meta.EventTypeTypeClass;
import com.espertech.esper.common.client.util.EventTypeBusModifier;
import com.espertech.esper.common.client.util.NameAccessModifier;
import com.espertech.esper.common.client.util.StatementProperty;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenPackageScope;
import com.espertech.esper.common.internal.bytecodemodel.core.CodeGenerationIDGenerator;
import com.espertech.esper.common.internal.collection.Pair;
import com.espertech.esper.common.internal.compile.multikey.MultiKeyPlanner;
import com.espertech.esper.common.internal.compile.multikey.MultiKeyPlan;
import com.espertech.esper.common.internal.compile.stage1.spec.*;
import com.espertech.esper.common.internal.compile.stage2.*;
import com.espertech.esper.common.internal.compile.stage3.*;
import com.espertech.esper.common.internal.context.compile.ContextMetaData;
import com.espertech.esper.common.internal.context.controller.category.ContextControllerCategoryFactoryForge;
import com.espertech.esper.common.internal.context.controller.core.ContextControllerFactoryEnv;
import com.espertech.esper.common.internal.context.controller.core.ContextControllerFactoryForge;
import com.espertech.esper.common.internal.context.controller.core.ContextControllerPortableInfo;
import com.espertech.esper.common.internal.context.controller.hash.ContextControllerHashFactoryForge;
import com.espertech.esper.common.internal.context.controller.initterm.ContextControllerInitTermFactoryForge;
import com.espertech.esper.common.internal.context.controller.keyed.ContextControllerKeyedFactoryForge;
import com.espertech.esper.common.internal.context.module.StatementAIFactoryProvider;
import com.espertech.esper.common.internal.context.module.StatementFields;
import com.espertech.esper.common.internal.context.module.StatementInformationalsCompileTime;
import com.espertech.esper.common.internal.context.module.StatementProvider;
import com.espertech.esper.common.internal.context.util.ContextPropertyEventType;
import com.espertech.esper.common.internal.epl.expression.core.*;
import com.espertech.esper.common.internal.epl.pattern.core.EvalForgeNode;
import com.espertech.esper.common.internal.epl.pattern.core.PatternContext;
import com.espertech.esper.common.internal.epl.pattern.filter.EvalFilterForgeNode;
import com.espertech.esper.common.internal.epl.resultset.select.core.SelectSubscriberDescriptor;
import com.espertech.esper.common.internal.epl.streamtype.StreamTypeServiceImpl;
import com.espertech.esper.common.internal.event.core.*;
import com.espertech.esper.common.internal.event.map.MapEventType;
import com.espertech.esper.common.internal.filterspec.FilterSpecParamExprNodeForge;
import com.espertech.esper.common.internal.schedule.ScheduleExpressionUtil;
import com.espertech.esper.common.internal.schedule.ScheduleHandleCallbackProvider;
import com.espertech.esper.common.internal.serde.compiletime.eventtype.SerdeEventTypeUtility;
import com.espertech.esper.common.internal.serde.compiletime.resolve.DataInputOutputSerdeForge;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class StmtForgeMethodCreateContext implements StmtForgeMethod {

    private final StatementBaseInfo base;

    public StmtForgeMethodCreateContext(StatementBaseInfo base) {
        this.base = base;
    }

    public StmtForgeMethodResult make(String packageName, String classPostfix, StatementCompileTimeServices services) throws ExprValidationException {
        StatementSpecCompiled statementSpec = base.getStatementSpec();
        if (statementSpec.getRaw().getOptionalContextName() != null) {
            throw new ExprValidationException("A create-context statement cannot itself be associated to a context, please declare a nested context instead");
        }
        List<FilterSpecCompiled> filterSpecCompileds = new ArrayList<>();
        List<ScheduleHandleCallbackProvider> scheduleHandleCallbackProviders = new ArrayList<>();
        List<FilterSpecParamExprNodeForge> filterBooleanExpressions = new ArrayList<>();

        final CreateContextDesc context = statementSpec.getRaw().getCreateContextDesc();
        if (services.getContextCompileTimeResolver().getContextInfo(context.getContextName()) != null) {
            throw new ExprValidationException("Context by name '" + context.getContextName() + "' already exists");
        }

        // compile filter specs, if any
        CreateContextValidationEnv validationEnv = new CreateContextValidationEnv(context.getContextName(), base.getStatementRawInfo(), services, filterSpecCompileds, scheduleHandleCallbackProviders, filterBooleanExpressions);
        List<StmtClassForgeableFactory> additionalForgeables = validateContextDetail(context.getContextDetail(), 0, validationEnv);

        // get controller factory forges
        ContextControllerFactoryForge[] controllerFactoryForges = getForges(context.getContextName(), context.getContextDetail());

        // build context properties type information
        Map<String, Object> contextProps = makeContextProperies(controllerFactoryForges, base.getStatementRawInfo(), services);

        // allocate type for context properties
        String contextEventTypeName = services.getEventTypeNameGeneratorStatement().getContextPropertyTypeName(context.getContextName());
        EventTypeMetadata metadata = new EventTypeMetadata(contextEventTypeName, base.getModuleName(), EventTypeTypeClass.CONTEXTPROPDERIVED, EventTypeApplicationType.MAP, NameAccessModifier.TRANSIENT, EventTypeBusModifier.NONBUS, false, EventTypeIdPair.unassigned());
        MapEventType contextPropertiesType = BaseNestableEventUtil.makeMapTypeCompileTime(metadata, contextProps, null, null, null, null, services.getBeanEventTypeFactoryPrivate(), services.getEventTypeCompileTimeResolver());
        services.getEventTypeCompileTimeRegistry().newType(contextPropertiesType);

        // register context
        NameAccessModifier visibilityContext = services.getModuleVisibilityRules().getAccessModifierContext(base, context.getContextName());
        ContextControllerPortableInfo[] validationInfo = new ContextControllerPortableInfo[controllerFactoryForges.length];
        for (int i = 0; i < validationInfo.length; i++) {
            validationInfo[i] = controllerFactoryForges[i].getValidationInfo();
        }
        ContextMetaData detail = new ContextMetaData(context.getContextName(), base.getModuleName(), visibilityContext, contextPropertiesType, validationInfo);
        services.getContextCompileTimeRegistry().newContext(detail);

        // define output event type
        String statementEventTypeName = services.getEventTypeNameGeneratorStatement().getContextStatementTypeName(context.getContextName());
        EventTypeMetadata statementTypeMetadata = new EventTypeMetadata(statementEventTypeName, base.getModuleName(), EventTypeTypeClass.STATEMENTOUT, EventTypeApplicationType.MAP, NameAccessModifier.TRANSIENT, EventTypeBusModifier.NONBUS, false, EventTypeIdPair.unassigned());
        EventType statementEventType = BaseNestableEventUtil.makeMapTypeCompileTime(statementTypeMetadata, Collections.emptyMap(), null, null, null, null, services.getBeanEventTypeFactoryPrivate(), services.getEventTypeCompileTimeResolver());
        services.getEventTypeCompileTimeRegistry().newType(statementEventType);

        String statementFieldsClassName = CodeGenerationIDGenerator.generateClassNameSimple(StatementFields.class, classPostfix);
        CodegenPackageScope packageScope = new CodegenPackageScope(packageName, statementFieldsClassName, services.isInstrumented());

        List<StmtClassForgeable> forgeables = new ArrayList<>();
        for (StmtClassForgeableFactory additional : additionalForgeables) {
            forgeables.add(additional.make(packageScope, classPostfix));
        }

        String statementProviderClassName = CodeGenerationIDGenerator.generateClassNameSimple(StatementProvider.class, classPostfix);
        String statementAIFactoryProviderClassName = CodeGenerationIDGenerator.generateClassNameSimple(StatementAIFactoryProvider.class, classPostfix);

        StatementAgentInstanceFactoryCreateContextForge forge = new StatementAgentInstanceFactoryCreateContextForge(context.getContextName(), statementEventType);
        forgeables.add(new StmtClassForgeableAIFactoryProviderCreateContext(statementAIFactoryProviderClassName, packageScope, context.getContextName(), controllerFactoryForges, contextPropertiesType, forge));

        SelectSubscriberDescriptor selectSubscriberDescriptor = new SelectSubscriberDescriptor();
        StatementInformationalsCompileTime informationals = StatementInformationalsUtil.getInformationals(base, filterSpecCompileds, scheduleHandleCallbackProviders, Collections.emptyList(), false, selectSubscriberDescriptor, packageScope, services);
        informationals.getProperties().put(StatementProperty.CREATEOBJECTNAME, context.getContextName());
        forgeables.add(new StmtClassForgeableStmtProvider(statementAIFactoryProviderClassName, statementProviderClassName, informationals, packageScope));
        forgeables.add(new StmtClassForgeableStmtFields(statementFieldsClassName, packageScope, 0));

        return new StmtForgeMethodResult(forgeables, filterSpecCompileds, scheduleHandleCallbackProviders, Collections.emptyList(), FilterSpecCompiled.makeExprNodeList(filterSpecCompileds, filterBooleanExpressions));
    }

    private Map<String, Object> makeContextProperies(ContextControllerFactoryForge[] controllers, StatementRawInfo statementRawInfo, StatementCompileTimeServices services) throws ExprValidationException {

        LinkedHashMap<String, Object> props = new LinkedHashMap<>();
        props.put(ContextPropertyEventType.PROP_CTX_NAME, String.class);
        props.put(ContextPropertyEventType.PROP_CTX_ID, Integer.class);

        if (controllers.length == 1) {
            controllers[0].validateGetContextProps(props, controllers[0].getFactoryEnv().getOutermostContextName(), statementRawInfo, services);
            return props;
        }

        for (int level = 0; level < controllers.length; level++) {
            String nestedContextName = controllers[level].getFactoryEnv().getContextName();
            LinkedHashMap<String, Object> propsPerLevel = new LinkedHashMap<>();
            propsPerLevel.put(ContextPropertyEventType.PROP_CTX_NAME, String.class);
            if (level == controllers.length - 1) {
                propsPerLevel.put(ContextPropertyEventType.PROP_CTX_ID, Integer.class);
            }
            controllers[level].validateGetContextProps(propsPerLevel, nestedContextName, statementRawInfo, services);
            props.put(nestedContextName, propsPerLevel);
        }

        return props;
    }

    private List<StmtClassForgeableFactory> validateContextDetail(ContextSpec contextSpec, int nestingLevel, CreateContextValidationEnv validationEnv) throws ExprValidationException {
        Set<String> eventTypesReferenced = new HashSet<>();
        List<StmtClassForgeableFactory> additionalForgeables = new ArrayList<>(2);

        if (contextSpec instanceof ContextSpecKeyed) {
            ContextSpecKeyed segmented = (ContextSpecKeyed) contextSpec;
            Map<String, EventType> asNames = new HashMap<>();
            boolean partitionHasNameAssignment = false;
            Class[] getterTypes = null;
            for (ContextSpecKeyedItem partition : segmented.getItems()) {
                Pair<FilterSpecCompiled, List<StmtClassForgeableFactory>> pair = compilePartitonedFilterSpec(partition.getFilterSpecRaw(), eventTypesReferenced, validationEnv);
                FilterSpecCompiled filterSpecCompiled = pair.getFirst();
                additionalForgeables.addAll(pair.getSecond());
                partition.setFilterSpecCompiled(filterSpecCompiled);

                EventPropertyGetterSPI[] getters = new EventPropertyGetterSPI[partition.getPropertyNames().size()];
                DataInputOutputSerdeForge[] serdes = new DataInputOutputSerdeForge[partition.getPropertyNames().size()];
                EventTypeSPI eventType = (EventTypeSPI) filterSpecCompiled.getFilterForEventType();
                getterTypes = new Class[partition.getPropertyNames().size()];
                for (int i = 0; i < partition.getPropertyNames().size(); i++) {
                    String propertyName = partition.getPropertyNames().get(i);
                    EventPropertyGetterSPI getter = eventType.getGetterSPI(propertyName);
                    if (getter == null) {
                        throw new ExprValidationException("For context '" + validationEnv.getContextName() + "' property name '" + propertyName + "' not found on type " + eventType.getName());
                    }
                    getters[i] = getter;
                    getterTypes[i] = eventType.getPropertyType(propertyName);
                    serdes[i] = validationEnv.getServices().getSerdeResolver().serdeForFilter(getterTypes[i], validationEnv.getStatementRawInfo());
                }
                partition.setGetters(getters);
                partition.setLookupableSerdes(serdes);

                if (partition.getAliasName() != null) {
                    partitionHasNameAssignment = true;
                    validateAsName(asNames, partition.getAliasName(), filterSpecCompiled.getFilterForEventType());
                }
            }

            // plan multi-key, make sure we use the same multikey for all items
            MultiKeyPlan multiKeyPlan = MultiKeyPlanner.planMultiKey(getterTypes, false, base.getStatementRawInfo(), validationEnv.getServices().getSerdeResolver());
            additionalForgeables.addAll(multiKeyPlan.getMultiKeyForgeables());
            for (ContextSpecKeyedItem partition : segmented.getItems()) {
                partition.setKeyMultiKey(multiKeyPlan.getClassRef());
            }
            segmented.setMultiKeyClassRef(multiKeyPlan.getClassRef());

            if (segmented.getOptionalInit() != null) {
                asNames.clear();
                for (ContextSpecConditionFilter initCondition : segmented.getOptionalInit()) {
                    ContextDetailMatchPair pair = validateRewriteContextCondition(true, nestingLevel, initCondition, eventTypesReferenced, new MatchEventSpec(), Collections.emptySet(), validationEnv);
                    additionalForgeables.addAll(pair.additionalForgeables);

                    EventType filterForType = initCondition.getFilterSpecCompiled().getFilterForEventType();
                    boolean found = false;
                    for (ContextSpecKeyedItem partition : segmented.getItems()) {
                        if (partition.getFilterSpecCompiled().getFilterForEventType() == filterForType) {
                            found = true;
                            break;
                        }
                    }
                    if (!found) {
                        throw new ExprValidationException("Segmented context '" + validationEnv.getContextName() + "' requires that all of the event types that are listed in the initialized-by also appear in the partition-by, type '" + filterForType.getName() + "' is not one of the types listed in partition-by");
                    }
                    if (initCondition.getOptionalFilterAsName() != null) {
                        if (partitionHasNameAssignment) {
                            throw new ExprValidationException("Segmented context '" + validationEnv.getContextName() + "' requires that either partition-by or initialized-by assign stream names, but not both");
                        }
                        validateAsName(asNames, initCondition.getOptionalFilterAsName(), filterForType);
                    }
                }
            }
            if (segmented.getOptionalTermination() != null) {
                MatchEventSpec matchEventSpec = new MatchEventSpec();
                LinkedHashSet<String> allTags = new LinkedHashSet<>();
                for (ContextSpecKeyedItem partition : segmented.getItems()) {
                    if (partition.getAliasName() != null) {
                        allTags.add(partition.getAliasName());
                        EventType eventType = partition.getFilterSpecCompiled().getFilterForEventType();
                        matchEventSpec.getTaggedEventTypes().put(partition.getAliasName(), new Pair<>(eventType, partition.getFilterSpecRaw().getEventTypeName()));
                        List<StmtClassForgeableFactory> serdeForgeables = SerdeEventTypeUtility.plan(eventType, validationEnv.getStatementRawInfo(), validationEnv.getServices().getSerdeEventTypeRegistry(), validationEnv.getServices().getSerdeResolver());
                        additionalForgeables.addAll(serdeForgeables);
                    }
                }
                if (segmented.getOptionalInit() != null) {
                    for (ContextSpecConditionFilter initCondition : segmented.getOptionalInit()) {
                        if (initCondition.getOptionalFilterAsName() != null) {
                            allTags.add(initCondition.getOptionalFilterAsName());
                            matchEventSpec.getTaggedEventTypes().put(initCondition.getOptionalFilterAsName(), new Pair<>(initCondition.getFilterSpecCompiled().getFilterForEventType(), initCondition.getFilterSpecRaw().getEventTypeName()));
                        }
                    }
                }
                ContextDetailMatchPair endCondition = validateRewriteContextCondition(false, nestingLevel, segmented.getOptionalTermination(), eventTypesReferenced, matchEventSpec, allTags, validationEnv);
                additionalForgeables.addAll(endCondition.additionalForgeables);
                segmented.setOptionalTermination(endCondition.getCondition());
            }
        } else if (contextSpec instanceof ContextSpecCategory) {

            // compile filter
            ContextSpecCategory category = (ContextSpecCategory) contextSpec;
            validateNotTable(category.getFilterSpecRaw().getEventTypeName(), validationEnv.getServices());
            FilterStreamSpecRaw raw = new FilterStreamSpecRaw(category.getFilterSpecRaw(), ViewSpec.EMPTY_VIEWSPEC_ARRAY, null, StreamSpecOptions.DEFAULT);
            StreamSpecCompiledDesc compiledDesc = StreamSpecCompiler.compileFilter(raw, false, false, true, false, null, validationEnv.getStatementRawInfo(), validationEnv.getServices());
            FilterStreamSpecCompiled result = (FilterStreamSpecCompiled) compiledDesc.getStreamSpecCompiled();
            additionalForgeables.addAll(compiledDesc.getAdditionalForgeables());
            category.setFilterSpecCompiled(result.getFilterSpecCompiled());
            validationEnv.getFilterSpecCompileds().add(result.getFilterSpecCompiled());

            // compile expressions
            for (ContextSpecCategoryItem item : category.getItems()) {
                validateNotTable(category.getFilterSpecRaw().getEventTypeName(), validationEnv.getServices());
                FilterSpecRaw filterSpecRaw = new FilterSpecRaw(category.getFilterSpecRaw().getEventTypeName(), Collections.singletonList(item.getExpression()), null);
                FilterStreamSpecRaw rawExpr = new FilterStreamSpecRaw(filterSpecRaw, ViewSpec.EMPTY_VIEWSPEC_ARRAY, null, StreamSpecOptions.DEFAULT);
                StreamSpecCompiledDesc compiledDescItems = StreamSpecCompiler.compileFilter(rawExpr, false, false, true, false, null, validationEnv.getStatementRawInfo(), validationEnv.getServices());
                FilterStreamSpecCompiled compiled = (FilterStreamSpecCompiled) compiledDescItems.getStreamSpecCompiled();
                additionalForgeables.addAll(compiledDescItems.getAdditionalForgeables());
                compiled.getFilterSpecCompiled().traverseFilterBooleanExpr(validationEnv.getFilterBooleanExpressions()::add);
                item.setCompiledFilterParam(compiled.getFilterSpecCompiled().getParameters());
            }
        } else if (contextSpec instanceof ContextSpecHash) {
            ContextSpecHash hashed = (ContextSpecHash) contextSpec;
            for (ContextSpecHashItem hashItem : hashed.getItems()) {
                FilterStreamSpecRaw raw = new FilterStreamSpecRaw(hashItem.getFilterSpecRaw(), ViewSpec.EMPTY_VIEWSPEC_ARRAY, null, StreamSpecOptions.DEFAULT);
                validateNotTable(hashItem.getFilterSpecRaw().getEventTypeName(), validationEnv.getServices());
                StreamSpecCompiledDesc compiledDesc = StreamSpecCompiler.compile(raw, eventTypesReferenced, false, false, true, false, null, 0, validationEnv.getStatementRawInfo(), validationEnv.getServices());
                additionalForgeables.addAll(compiledDesc.getAdditionalForgeables());
                FilterStreamSpecCompiled result = (FilterStreamSpecCompiled)  compiledDesc.getStreamSpecCompiled();
                validationEnv.getFilterSpecCompileds().add(result.getFilterSpecCompiled());
                hashItem.setFilterSpecCompiled(result.getFilterSpecCompiled());

                // validate parameters
                StreamTypeServiceImpl streamTypes = new StreamTypeServiceImpl(result.getFilterSpecCompiled().getFilterForEventType(), null, true);
                ExprValidationContext validationContext = new ExprValidationContextBuilder(streamTypes, validationEnv.getStatementRawInfo(), validationEnv.getServices()).withIsFilterExpression(true).build();
                ExprNodeUtilityValidate.validate(ExprNodeOrigin.CONTEXT, Collections.singletonList(hashItem.getFunction()), validationContext);
            }
        } else if (contextSpec instanceof ContextSpecInitiatedTerminated) {
            ContextSpecInitiatedTerminated def = (ContextSpecInitiatedTerminated) contextSpec;
            ContextDetailMatchPair startCondition = validateRewriteContextCondition(true, nestingLevel, def.getStartCondition(), eventTypesReferenced, new MatchEventSpec(), new LinkedHashSet<String>(), validationEnv);
            additionalForgeables.addAll(startCondition.additionalForgeables);
            ContextDetailMatchPair endCondition = validateRewriteContextCondition(false, nestingLevel, def.getEndCondition(), eventTypesReferenced, startCondition.getMatches(), startCondition.getAllTags(), validationEnv);
            additionalForgeables.addAll(endCondition.additionalForgeables);
            def.setStartCondition(startCondition.getCondition());
            def.setEndCondition(endCondition.getCondition());

            if (def.getDistinctExpressions() != null) {
                if (!(startCondition.getCondition() instanceof ContextSpecConditionFilter)) {
                    throw new ExprValidationException("Distinct-expressions require a stream as the initiated-by condition");
                }
                ExprNode[] distinctExpressions = def.getDistinctExpressions();
                if (distinctExpressions.length == 0) {
                    throw new ExprValidationException("Distinct-expressions have not been provided");
                }
                ContextSpecConditionFilter filter = (ContextSpecConditionFilter) startCondition.getCondition();
                if (filter.getOptionalFilterAsName() == null) {
                    throw new ExprValidationException("Distinct-expressions require that a stream name is assigned to the stream using 'as'");
                }
                StreamTypeServiceImpl types = new StreamTypeServiceImpl(filter.getFilterSpecCompiled().getFilterForEventType(), filter.getOptionalFilterAsName(), true);
                ExprValidationContext validationContext = new ExprValidationContextBuilder(types, validationEnv.getStatementRawInfo(), validationEnv.getServices()).withAllowBindingConsumption(true).build();
                for (int i = 0; i < distinctExpressions.length; i++) {
                    ExprNodeUtilityValidate.validatePlainExpression(ExprNodeOrigin.CONTEXTDISTINCT, distinctExpressions[i]);
                    distinctExpressions[i] = ExprNodeUtilityValidate.getValidatedSubtree(ExprNodeOrigin.CONTEXTDISTINCT, distinctExpressions[i], validationContext);
                }

                MultiKeyPlan multiKeyPlan = MultiKeyPlanner.planMultiKey(distinctExpressions, false, base.getStatementRawInfo(), validationEnv.getServices().getSerdeResolver());
                def.setDistinctMultiKey(multiKeyPlan.getClassRef());
                additionalForgeables.addAll(multiKeyPlan.getMultiKeyForgeables());
            }
        } else if (contextSpec instanceof ContextNested) {
            ContextNested nested = (ContextNested) contextSpec;
            int level = 0;
            Set<String> namesUsed = new HashSet<String>();
            namesUsed.add(validationEnv.getContextName());
            for (CreateContextDesc nestedContext : nested.getContexts()) {
                if (namesUsed.contains(nestedContext.getContextName())) {
                    throw new ExprValidationException("Context by name '" + nestedContext.getContextName() + "' has already been declared within nested context '" + validationEnv.getContextName() + "'");
                }
                namesUsed.add(nestedContext.getContextName());

                List<StmtClassForgeableFactory> forgeables = validateContextDetail(nestedContext.getContextDetail(), level, validationEnv);
                additionalForgeables.addAll(forgeables);

                level++;
            }
        } else {
            throw new IllegalStateException("Unrecognized context detail " + contextSpec);
        }

        return additionalForgeables;
    }

    private Pair<FilterSpecCompiled, List<StmtClassForgeableFactory>> compilePartitonedFilterSpec(FilterSpecRaw filterSpecRaw, Set<String> eventTypesReferenced, CreateContextValidationEnv validationEnv) throws ExprValidationException {
        validateNotTable(filterSpecRaw.getEventTypeName(), validationEnv.getServices());
        FilterStreamSpecRaw raw = new FilterStreamSpecRaw(filterSpecRaw, ViewSpec.EMPTY_VIEWSPEC_ARRAY, null, StreamSpecOptions.DEFAULT);
        StreamSpecCompiledDesc compiledDesc = StreamSpecCompiler.compile(raw, eventTypesReferenced, false, false, true, false, null, 0, validationEnv.getStatementRawInfo(), validationEnv.getServices());
        if (!(compiledDesc.getStreamSpecCompiled() instanceof FilterStreamSpecCompiled)) {
            throw new ExprValidationException("Partition criteria may not include named windows");
        }
        FilterStreamSpecCompiled filters = (FilterStreamSpecCompiled) compiledDesc.getStreamSpecCompiled();
        FilterSpecCompiled spec = filters.getFilterSpecCompiled();
        validationEnv.getFilterSpecCompileds().add(spec);
        return new Pair<>(spec, compiledDesc.getAdditionalForgeables());
    }

    private void validateNotTable(String eventTypeName, StatementCompileTimeServices services) throws ExprValidationException {
        if (services.getTableCompileTimeResolver().resolve(eventTypeName) != null) {
            throw new ExprValidationException("Tables cannot be used in a context declaration");
        }
    }

    private void validateAsName(Map<String, EventType> asNames, String asName, EventType filterForType) throws ExprValidationException {
        EventType existing = asNames.get(asName);
        if (existing != null && !EventTypeUtility.isTypeOrSubTypeOf(filterForType, existing)) {
            throw new ExprValidationException("Name '" + asName + "' already used for type '" + existing.getName() + "'");
        }
        if (existing == null) {
            asNames.put(asName, filterForType);
        }
    }

    private ContextControllerFactoryForge[] getForges(String contextName, ContextSpec contextDetail) throws ExprValidationException {
        if (!(contextDetail instanceof ContextNested)) {
            ContextControllerFactoryEnv factoryEnv = new ContextControllerFactoryEnv(contextName, contextName, 1, 1);
            return new ContextControllerFactoryForge[]{make(factoryEnv, contextDetail)};
        }
        ContextNested nested = (ContextNested) contextDetail;
        ContextControllerFactoryForge[] forges = new ContextControllerFactoryForge[nested.getContexts().size()];
        int nestingLevel = 1;
        for (CreateContextDesc desc : nested.getContexts()) {
            ContextControllerFactoryEnv factoryEnv = new ContextControllerFactoryEnv(contextName, desc.getContextName(), nestingLevel, nested.getContexts().size());
            forges[nestingLevel - 1] = make(factoryEnv, desc.getContextDetail());
            nestingLevel++;
        }
        return forges;
    }

    private ContextControllerFactoryForge make(ContextControllerFactoryEnv factoryContext, ContextSpec detail) throws ExprValidationException {
        ContextControllerFactoryForge forge;
        if (detail instanceof ContextSpecInitiatedTerminated) {
            forge = new ContextControllerInitTermFactoryForge(factoryContext, (ContextSpecInitiatedTerminated) detail);
        } else if (detail instanceof ContextSpecKeyed) {
            forge = new ContextControllerKeyedFactoryForge(factoryContext, (ContextSpecKeyed) detail);
        } else if (detail instanceof ContextSpecCategory) {
            forge = new ContextControllerCategoryFactoryForge(factoryContext, (ContextSpecCategory) detail);
        } else if (detail instanceof ContextSpecHash) {
            forge = new ContextControllerHashFactoryForge(factoryContext, (ContextSpecHash) detail);
        } else {
            throw new UnsupportedOperationException("Context detail " + detail + " is not yet supported in a nested context");
        }

        return forge;
    }

    private ContextDetailMatchPair validateRewriteContextCondition(boolean isStartCondition, int nestingLevel, ContextSpecCondition endpoint, Set<String> eventTypesReferenced, MatchEventSpec priorMatches, Set<String> priorAllTags, CreateContextValidationEnv validationEnv) throws ExprValidationException {
        if (endpoint instanceof ContextSpecConditionCrontab) {
            ContextSpecConditionCrontab crontab = (ContextSpecConditionCrontab) endpoint;
            ExprForge[][] forgesPerCrontab = new ExprForge[crontab.getCrontabs().size()][];
            for (int i = 0; i < crontab.getCrontabs().size(); i++) {
                List<ExprNode> item = crontab.getCrontabs().get(i);
                ExprForge[] forges = ScheduleExpressionUtil.crontabScheduleValidate(ExprNodeOrigin.CONTEXTCONDITION, item, false, validationEnv.getStatementRawInfo(), validationEnv.getServices());
                forgesPerCrontab[i] = forges;
            }
            crontab.setForgesPerCrontab(forgesPerCrontab);
            validationEnv.getScheduleHandleCallbackProviders().add(crontab);
            return new ContextDetailMatchPair(crontab, new MatchEventSpec(), new LinkedHashSet<>(), Collections.emptyList());
        }

        if (endpoint instanceof ContextSpecConditionTimePeriod) {
            ContextSpecConditionTimePeriod timePeriod = (ContextSpecConditionTimePeriod) endpoint;
            ExprValidationContext validationContext = new ExprValidationContextBuilder(new StreamTypeServiceImpl(false), validationEnv.getStatementRawInfo(), validationEnv.getServices()).build();
            ExprNodeUtilityValidate.getValidatedSubtree(ExprNodeOrigin.CONTEXTCONDITION, timePeriod.getTimePeriod(), validationContext);
            if (timePeriod.getTimePeriod().isConstantResult()) {
                if (timePeriod.getTimePeriod().evaluateAsSeconds(null, true, null) < 0) {
                    throw new ExprValidationException("Invalid negative time period expression '" + ExprNodeUtilityPrint.toExpressionStringMinPrecedenceSafe(timePeriod.getTimePeriod()) + "'");
                }
            }
            validationEnv.getScheduleHandleCallbackProviders().add(timePeriod);
            return new ContextDetailMatchPair(timePeriod, new MatchEventSpec(), new LinkedHashSet<>(), Collections.emptyList());
        }

        if (endpoint instanceof ContextSpecConditionPattern) {
            ContextSpecConditionPattern pattern = (ContextSpecConditionPattern) endpoint;
            PatternValidatedDesc validatedDesc = validatePatternContextConditionPattern(isStartCondition, nestingLevel, pattern, eventTypesReferenced, priorMatches, priorAllTags, validationEnv);
            return new ContextDetailMatchPair(pattern, validatedDesc.getMatchEventSpec(), validatedDesc.getAllTags(), validatedDesc.additionalForgeables);
        }

        if (endpoint instanceof ContextSpecConditionFilter) {
            ContextSpecConditionFilter filter = (ContextSpecConditionFilter) endpoint;
            validateNotTable(filter.getFilterSpecRaw().getEventTypeName(), validationEnv.getServices());

            // compile as filter if there are no prior match to consider
            if (priorMatches == null || (priorMatches.getArrayEventTypes().isEmpty() && priorMatches.getTaggedEventTypes().isEmpty())) {
                FilterStreamSpecRaw rawExpr = new FilterStreamSpecRaw(filter.getFilterSpecRaw(), ViewSpec.EMPTY_VIEWSPEC_ARRAY, null, StreamSpecOptions.DEFAULT);
                StreamSpecCompiledDesc compiledDesc = StreamSpecCompiler.compile(rawExpr, eventTypesReferenced, false, false, true, false, filter.getOptionalFilterAsName(), 0, validationEnv.getStatementRawInfo(), validationEnv.getServices());
                FilterStreamSpecCompiled compiled = (FilterStreamSpecCompiled) compiledDesc.getStreamSpecCompiled();
                filter.setFilterSpecCompiled(compiled.getFilterSpecCompiled());
                MatchEventSpec matchEventSpec = new MatchEventSpec();
                EventType filterForType = compiled.getFilterSpecCompiled().getFilterForEventType();
                LinkedHashSet<String> allTags = new LinkedHashSet<String>();
                if (filter.getOptionalFilterAsName() != null) {
                    matchEventSpec.getTaggedEventTypes().put(filter.getOptionalFilterAsName(), new Pair<EventType, String>(filterForType, rawExpr.getRawFilterSpec().getEventTypeName()));
                    allTags.add(filter.getOptionalFilterAsName());
                }
                validationEnv.getFilterSpecCompileds().add(compiled.getFilterSpecCompiled());
                List<StmtClassForgeableFactory> serdeForgeables = SerdeEventTypeUtility.plan(filter.getFilterSpecCompiled().getFilterForEventType(), validationEnv.getStatementRawInfo(), validationEnv.getServices().getSerdeEventTypeRegistry(), validationEnv.getServices().getSerdeResolver());
                List<StmtClassForgeableFactory> allForgeables = Stream.concat(compiledDesc.getAdditionalForgeables().stream(), serdeForgeables.stream()).collect(Collectors.toList());
                return new ContextDetailMatchPair(filter, matchEventSpec, allTags, allForgeables);
            }

            // compile as pattern if there are prior matches to consider, since this is a type of followed-by relationship
            EvalForgeNode forgeNode = new EvalFilterForgeNode(validationEnv.getServices().isAttachPatternText(), filter.getFilterSpecRaw(), filter.getOptionalFilterAsName(), 0);
            ContextSpecConditionPattern pattern = new ContextSpecConditionPattern(forgeNode, true, false);
            PatternValidatedDesc validated = validatePatternContextConditionPattern(isStartCondition, nestingLevel, pattern, eventTypesReferenced, priorMatches, priorAllTags, validationEnv);
            return new ContextDetailMatchPair(pattern, validated.getMatchEventSpec(), validated.getAllTags(), validated.additionalForgeables);
        } else if (endpoint instanceof ContextSpecConditionImmediate || endpoint instanceof ContextSpecConditionNever) {
            return new ContextDetailMatchPair(endpoint, new MatchEventSpec(), new LinkedHashSet<String>(), Collections.emptyList());
        } else {
            throw new IllegalStateException("Unrecognized endpoint type " + endpoint);
        }
    }

    private PatternValidatedDesc validatePatternContextConditionPattern(boolean isStartCondition, int nestingLevel, ContextSpecConditionPattern pattern, Set<String> eventTypesReferenced, MatchEventSpec priorMatches, Set<String> priorAllTags, CreateContextValidationEnv validationEnv)
            throws ExprValidationException {
        PatternStreamSpecRaw raw = new PatternStreamSpecRaw(pattern.getPatternRaw(), ViewSpec.EMPTY_VIEWSPEC_ARRAY, null, StreamSpecOptions.DEFAULT, false, false);
        StreamSpecCompiledDesc compiledDesc = StreamSpecCompiler.compilePatternWTags(raw, eventTypesReferenced, false, priorMatches, priorAllTags, false, true, false, 0, validationEnv.getStatementRawInfo(), validationEnv.getServices());
        PatternStreamSpecCompiled compiled = (PatternStreamSpecCompiled) compiledDesc.getStreamSpecCompiled();
        pattern.setPatternCompiled(compiled);

        pattern.setPatternContext(new PatternContext(0, compiled.getMatchedEventMapMeta(), true, nestingLevel, isStartCondition));

        List<EvalForgeNode> forges = compiled.getRoot().collectFactories();
        for (EvalForgeNode forge : forges) {
            forge.collectSelfFilterAndSchedule(validationEnv.getFilterSpecCompileds(), validationEnv.getScheduleHandleCallbackProviders());
        }

        MatchEventSpec spec = new MatchEventSpec(compiled.getTaggedEventTypes(), compiled.getArrayEventTypes());
        return new PatternValidatedDesc(spec, compiled.getAllTags(), compiledDesc.getAdditionalForgeables());
    }

    private static class ContextDetailMatchPair {
        private final ContextSpecCondition condition;
        private final MatchEventSpec matches;
        private final Set<String> allTags;
        private final List<StmtClassForgeableFactory> additionalForgeables;

        public ContextDetailMatchPair(ContextSpecCondition condition, MatchEventSpec matches, Set<String> allTags, List<StmtClassForgeableFactory> additionalForgeables) {
            this.condition = condition;
            this.matches = matches;
            this.allTags = allTags;
            this.additionalForgeables = additionalForgeables;
        }

        public ContextSpecCondition getCondition() {
            return condition;
        }

        public MatchEventSpec getMatches() {
            return matches;
        }

        public Set<String> getAllTags() {
            return allTags;
        }

        public List<StmtClassForgeableFactory> getAdditionalForgeables() {
            return additionalForgeables;
        }
    }

    private static class PatternValidatedDesc {
        private final MatchEventSpec matchEventSpec;
        private final Set<String> allTags;
        private final List<StmtClassForgeableFactory> additionalForgeables;

        public PatternValidatedDesc(MatchEventSpec matchEventSpec, Set<String> allTags, List<StmtClassForgeableFactory> additionalForgeables) {
            this.matchEventSpec = matchEventSpec;
            this.allTags = allTags;
            this.additionalForgeables = additionalForgeables;
        }

        public MatchEventSpec getMatchEventSpec() {
            return matchEventSpec;
        }

        public Set<String> getAllTags() {
            return allTags;
        }

        public List<StmtClassForgeableFactory> getAdditionalForgeables() {
            return additionalForgeables;
        }
    }
}
