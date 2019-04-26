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
package com.espertech.esper.common.internal.compile.stage2;

import com.espertech.esper.common.client.EPException;
import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.client.annotation.HookType;
import com.espertech.esper.common.client.meta.EventTypeApplicationType;
import com.espertech.esper.common.client.meta.EventTypeIdPair;
import com.espertech.esper.common.client.meta.EventTypeMetadata;
import com.espertech.esper.common.client.meta.EventTypeTypeClass;
import com.espertech.esper.common.client.util.EventTypeBusModifier;
import com.espertech.esper.common.client.util.NameAccessModifier;
import com.espertech.esper.common.internal.collection.Pair;
import com.espertech.esper.common.internal.compile.multikey.MultiKeyPlanner;
import com.espertech.esper.common.internal.compile.multikey.MultiKeyPlan;
import com.espertech.esper.common.internal.compile.stage1.spec.*;
import com.espertech.esper.common.internal.compile.stage3.StatementCompileTimeServices;
import com.espertech.esper.common.internal.compile.stage3.StmtClassForgeableFactory;
import com.espertech.esper.common.internal.epl.contained.PropertyEvaluatorForge;
import com.espertech.esper.common.internal.epl.contained.PropertyEvaluatorForgeFactory;
import com.espertech.esper.common.internal.epl.expression.core.*;
import com.espertech.esper.common.internal.epl.expression.time.eval.TimePeriodComputeConstGivenDeltaForge;
import com.espertech.esper.common.internal.epl.expression.time.eval.TimePeriodComputeForge;
import com.espertech.esper.common.internal.epl.expression.time.node.ExprTimePeriod;
import com.espertech.esper.common.internal.epl.expression.visitor.ExprNodeSummaryVisitor;
import com.espertech.esper.common.internal.epl.namedwindow.path.NamedWindowMetaData;
import com.espertech.esper.common.internal.epl.pattern.core.*;
import com.espertech.esper.common.internal.epl.pattern.everydistinct.EvalEveryDistinctForgeNode;
import com.espertech.esper.common.internal.epl.pattern.filter.EvalFilterForgeNode;
import com.espertech.esper.common.internal.epl.pattern.followedby.EvalFollowedByForgeNode;
import com.espertech.esper.common.internal.epl.pattern.guard.EvalGuardForgeNode;
import com.espertech.esper.common.internal.epl.pattern.guard.GuardForge;
import com.espertech.esper.common.internal.epl.pattern.guard.GuardParameterException;
import com.espertech.esper.common.internal.epl.pattern.matchuntil.EvalMatchUntilForgeNode;
import com.espertech.esper.common.internal.epl.pattern.observer.EvalObserverForgeNode;
import com.espertech.esper.common.internal.epl.pattern.observer.ObserverForge;
import com.espertech.esper.common.internal.epl.pattern.observer.ObserverParameterException;
import com.espertech.esper.common.internal.epl.streamtype.StreamTypeService;
import com.espertech.esper.common.internal.epl.streamtype.StreamTypeServiceImpl;
import com.espertech.esper.common.internal.epl.table.compiletime.TableMetaData;
import com.espertech.esper.common.internal.event.core.BaseNestableEventUtil;
import com.espertech.esper.common.internal.event.core.EventTypeCompileTimeResolver;
import com.espertech.esper.common.internal.event.map.MapEventType;
import com.espertech.esper.common.internal.serde.compiletime.eventtype.SerdeEventTypeUtility;
import com.espertech.esper.common.internal.settings.ClasspathImportUtil;
import com.espertech.esper.common.internal.util.JavaClassHelper;
import com.espertech.esper.common.internal.util.UuidGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class StreamSpecCompiler {
    private final static Logger log = LoggerFactory.getLogger(StreamSpecCompiler.class);

    public static StreamSpecCompiledDesc compile(StreamSpecRaw spec,
                                             Set<String> eventTypeReferences,
                                             boolean isInsertInto,
                                             boolean isJoin,
                                             boolean isContextDeclaration,
                                             boolean isOnTrigger,
                                             String optionalStreamName,
                                             int streamNum,
                                             StatementRawInfo statementRawInfo,
                                             StatementCompileTimeServices services)
            throws ExprValidationException {
        if (spec instanceof DBStatementStreamSpec) {
            return new StreamSpecCompiledDesc((DBStatementStreamSpec) spec, Collections.emptyList());
        } else if (spec instanceof FilterStreamSpecRaw) {
            return compileFilter((FilterStreamSpecRaw) spec, isInsertInto, isJoin, isContextDeclaration, isOnTrigger, optionalStreamName, statementRawInfo, services);
        } else if (spec instanceof PatternStreamSpecRaw) {
            return compilePattern((PatternStreamSpecRaw) spec, eventTypeReferences, isInsertInto, isJoin, isContextDeclaration, isOnTrigger, optionalStreamName, streamNum, statementRawInfo, services);
        } else if (spec instanceof MethodStreamSpec) {
            return new StreamSpecCompiledDesc(compileMethod((MethodStreamSpec) spec), Collections.emptyList());
        }
        throw new IllegalStateException("Unrecognized stream spec " + spec);
    }

    public static StreamSpecCompiledDesc compileFilter(FilterStreamSpecRaw streamSpec, boolean isInsertInto, boolean isJoin, boolean isContextDeclaration, boolean isOnTrigger, String optionalStreamName, StatementRawInfo statementRawInfo, StatementCompileTimeServices services)
            throws ExprValidationException {
        // Determine the event type
        FilterSpecRaw rawFilterSpec = streamSpec.getRawFilterSpec();
        String eventTypeName = rawFilterSpec.getEventTypeName();

        TableMetaData table = services.getTableCompileTimeResolver().resolve(eventTypeName);
        if (table != null) {
            if (streamSpec.getViewSpecs() != null && streamSpec.getViewSpecs().length > 0) {
                throw new ExprValidationException("Views are not supported with tables");
            }
            if (streamSpec.getRawFilterSpec().getOptionalPropertyEvalSpec() != null) {
                throw new ExprValidationException("Contained-event expressions are not supported with tables");
            }
            StreamTypeService streamTypeService = new StreamTypeServiceImpl(new EventType[]{table.getInternalEventType()}, new String[]{optionalStreamName}, new boolean[]{true}, false, false);
            FilterSpecValidatedDesc desc = FilterSpecCompiler.validateAllowSubquery(ExprNodeOrigin.FILTER, rawFilterSpec.getFilterExpressions(), streamTypeService, null, null, statementRawInfo, services);
            TableQueryStreamSpec tableStreamSpec = new TableQueryStreamSpec(streamSpec.getOptionalStreamName(), streamSpec.getViewSpecs(), streamSpec.getOptions(), table, desc.getExpressions());
            return new StreamSpecCompiledDesc(tableStreamSpec, desc.getAdditionalForgeables());
        }

        // Could be a named window
        NamedWindowMetaData namedWindowInfo = services.getNamedWindowCompileTimeResolver().resolve(eventTypeName);
        if (namedWindowInfo != null) {
            StreamTypeService streamTypeService = new StreamTypeServiceImpl(new EventType[]{namedWindowInfo.getEventType()}, new String[]{optionalStreamName}, new boolean[]{true}, false, false);

            FilterSpecValidatedDesc validated = FilterSpecCompiler.validateAllowSubquery(ExprNodeOrigin.FILTER, rawFilterSpec.getFilterExpressions(), streamTypeService, null, null, statementRawInfo, services);

            PropertyEvaluatorForge optionalPropertyEvaluator = null;
            if (rawFilterSpec.getOptionalPropertyEvalSpec() != null) {
                optionalPropertyEvaluator = PropertyEvaluatorForgeFactory.makeEvaluator(rawFilterSpec.getOptionalPropertyEvalSpec(), namedWindowInfo.getEventType(), streamSpec.getOptionalStreamName(), statementRawInfo, services);
            }
            NamedWindowConsumerStreamSpec consumer = new NamedWindowConsumerStreamSpec(namedWindowInfo, streamSpec.getOptionalStreamName(), streamSpec.getViewSpecs(), validated.getExpressions(), streamSpec.getOptions(), optionalPropertyEvaluator);
            return new StreamSpecCompiledDesc(consumer, validated.getAdditionalForgeables());
        }

        EventType eventType = resolveTypeName(eventTypeName, services.getEventTypeCompileTimeResolver());

        // Validate all nodes, make sure each returns a boolean and types are good;
        // Also decompose all AND super nodes into individual expressions
        StreamTypeService streamTypeService = new StreamTypeServiceImpl(new EventType[]{eventType}, new String[]{streamSpec.getOptionalStreamName()}, new boolean[]{true}, false, false);

        FilterSpecCompiledDesc desc = FilterSpecCompiler.makeFilterSpec(eventType, eventTypeName, rawFilterSpec.getFilterExpressions(), rawFilterSpec.getOptionalPropertyEvalSpec(),
            null, null,  // no tags
            streamTypeService, streamSpec.getOptionalStreamName(), statementRawInfo, services);
        FilterStreamSpecCompiled compiled = new FilterStreamSpecCompiled(desc.getFilterSpecCompiled(), streamSpec.getViewSpecs(), streamSpec.getOptionalStreamName(), streamSpec.getOptions());
        return new StreamSpecCompiledDesc(compiled, desc.getAdditionalForgeables());
    }

    public static StreamSpecCompiledDesc compilePattern(PatternStreamSpecRaw streamSpecRaw,
                                                           Set<String> eventTypeReferences,
                                                           boolean isInsertInto,
                                                           boolean isJoin,
                                                           boolean isContextDeclaration,
                                                           boolean isOnTrigger,
                                                           String optionalStreamName,
                                                           int streamNum,
                                                           StatementRawInfo statementRawInfo,
                                                           StatementCompileTimeServices services)
            throws ExprValidationException {
        return compilePatternWTags(streamSpecRaw, eventTypeReferences, isInsertInto, null, null, isJoin, isContextDeclaration, isOnTrigger, streamNum, statementRawInfo, services);
    }

    public static StreamSpecCompiledDesc compilePatternWTags(PatternStreamSpecRaw streamSpecRaw,
                                                                Set<String> eventTypeReferences,
                                                                boolean isInsertInto,
                                                                MatchEventSpec tags,
                                                                Set<String> priorAllTags,
                                                                boolean isJoin,
                                                                boolean isContextDeclaration,
                                                                boolean isOnTrigger,
                                                                int streamNum,
                                                                StatementRawInfo statementRawInfo,
                                                                StatementCompileTimeServices services)
            throws ExprValidationException {
        // validate
        if ((streamSpecRaw.isSuppressSameEventMatches() || streamSpecRaw.isDiscardPartialsOnMatch()) && (isJoin || isContextDeclaration || isOnTrigger)) {
            throw new ExprValidationException("Discard-partials and suppress-matches is not supported in a joins, context declaration and on-action");
        }

        if (tags == null) {
            tags = new MatchEventSpec();
        }
        Stack<EvalForgeNode> nodeStack = new Stack<EvalForgeNode>();

        // detemine ordered tags
        LinkedHashSet<String> allTagNamesOrdered = new LinkedHashSet<String>();
        Set<EvalForgeNode> filterFactoryNodes = EvalNodeUtil.recursiveGetChildNodes(streamSpecRaw.getEvalForgeNode(), FilterForFilterFactoryNodes.INSTANCE);
        if (priorAllTags != null) {
            allTagNamesOrdered.addAll(priorAllTags);
        }
        for (EvalForgeNode filterNode : filterFactoryNodes) {
            EvalFilterForgeNode forge = (EvalFilterForgeNode) filterNode;
            int tagNumber;
            if (forge.getEventAsName() != null) {
                if (!allTagNamesOrdered.contains(forge.getEventAsName())) {
                    allTagNamesOrdered.add(forge.getEventAsName());
                    tagNumber = allTagNamesOrdered.size() - 1;
                } else {
                    tagNumber = findTagNumber(forge.getEventAsName(), allTagNamesOrdered);
                }
                forge.setEventAsTagNumber(tagNumber);
            }
        }

        // construct root : assigns factory node ids
        EvalForgeNode top = streamSpecRaw.getEvalForgeNode();
        EvalRootForgeNode root = new EvalRootForgeNode(services.isAttachPatternText(), top, statementRawInfo.getAnnotations());
        List<StmtClassForgeableFactory> additionalForgeables = new ArrayList<>();
        recursiveCompile(top, tags, nodeStack, allTagNamesOrdered, streamNum, additionalForgeables, statementRawInfo, services);

        PatternCompileHook hook = (PatternCompileHook) ClasspathImportUtil.getAnnotationHook(statementRawInfo.getAnnotations(), HookType.INTERNAL_PATTERNCOMPILE, PatternCompileHook.class, services.getClasspathImportServiceCompileTime());
        if (hook != null) {
            hook.pattern(root);
        }

        PatternStreamSpecCompiled compiled = new PatternStreamSpecCompiled(root, tags.getTaggedEventTypes(), tags.getArrayEventTypes(), allTagNamesOrdered, streamSpecRaw.getViewSpecs(), streamSpecRaw.getOptionalStreamName(), streamSpecRaw.getOptions(), streamSpecRaw.isSuppressSameEventMatches(), streamSpecRaw.isDiscardPartialsOnMatch());
        return new StreamSpecCompiledDesc(compiled, additionalForgeables);
    }

    private static void recursiveCompile(EvalForgeNode evalNode, MatchEventSpec tags, Stack<EvalForgeNode> parentNodeStack, LinkedHashSet<String> allTagNamesOrdered, int streamNum, List<StmtClassForgeableFactory> additionalForgeables, StatementRawInfo statementRawInfo, StatementCompileTimeServices services) throws ExprValidationException {
        parentNodeStack.push(evalNode);
        for (EvalForgeNode child : evalNode.getChildNodes()) {
            recursiveCompile(child, tags, parentNodeStack, allTagNamesOrdered, streamNum, additionalForgeables, statementRawInfo, services);
        }
        parentNodeStack.pop();

        LinkedHashMap<String, Pair<EventType, String>> newTaggedEventTypes = null;
        LinkedHashMap<String, Pair<EventType, String>> newArrayEventTypes = null;

        if (evalNode instanceof EvalFilterForgeNode) {
            EvalFilterForgeNode filterNode = (EvalFilterForgeNode) evalNode;
            String eventName = filterNode.getRawFilterSpec().getEventTypeName();
            if (services.getTableCompileTimeResolver().resolve(eventName) != null) {
                throw new ExprValidationException("Tables cannot be used in pattern filter atoms");
            }

            EventType resolvedEventType = resolveTypeName(eventName, services.getEventTypeCompileTimeResolver());
            EventType finalEventType = resolvedEventType;
            String optionalTag = filterNode.getEventAsName();
            boolean isPropertyEvaluation = false;
            boolean isParentMatchUntil = isParentMatchUntil(evalNode, parentNodeStack);

            // obtain property event type, if final event type is properties
            if (filterNode.getRawFilterSpec().getOptionalPropertyEvalSpec() != null) {
                PropertyEvaluatorForge optionalPropertyEvaluator = PropertyEvaluatorForgeFactory.makeEvaluator(filterNode.getRawFilterSpec().getOptionalPropertyEvalSpec(), resolvedEventType, filterNode.getEventAsName(), statementRawInfo, services);
                finalEventType = optionalPropertyEvaluator.getFragmentEventType();
                isPropertyEvaluation = true;
            }

            // If a tag was supplied for the type, the tags must stay with this type, i.e. a=BeanA -> b=BeanA -> a=BeanB is a no
            if (optionalTag != null) {
                Pair<EventType, String> pair = tags.getTaggedEventTypes().get(optionalTag);
                EventType existingType = null;
                if (pair != null) {
                    existingType = pair.getFirst();
                }
                if (existingType == null) {
                    pair = tags.getArrayEventTypes().get(optionalTag);
                    if (pair != null) {
                        throw new ExprValidationException("Tag '" + optionalTag + "' for event '" + eventName +
                                "' used in the repeat-until operator cannot also appear in other filter expressions");
                    }
                }
                if ((existingType != null) && (existingType != finalEventType)) {
                    throw new ExprValidationException("Tag '" + optionalTag + "' for event '" + eventName +
                            "' has already been declared for events of type " + existingType.getUnderlyingType().getName());
                }
                pair = new Pair<>(finalEventType, eventName);

                // add tagged type
                if (isPropertyEvaluation || isParentMatchUntil) {
                    newArrayEventTypes = new LinkedHashMap<>();
                    newArrayEventTypes.put(optionalTag, pair);
                } else {
                    newTaggedEventTypes = new LinkedHashMap<>();
                    newTaggedEventTypes.put(optionalTag, pair);
                }

                List<StmtClassForgeableFactory> forgeables = SerdeEventTypeUtility.plan(pair.getFirst(), statementRawInfo, services.getSerdeEventTypeRegistry(), services.getSerdeResolver());
                additionalForgeables.addAll(forgeables);
            }

            // For this filter, filter types are all known tags at this time,
            // and additionally stream 0 (self) is our event type.
            // Stream type service allows resolution by property name event if that name appears in other tags.
            // by defaulting to stream zero.
            // Stream zero is always the current event type, all others follow the order of the map (stream 1 to N).
            String selfStreamName = optionalTag;
            if (selfStreamName == null) {
                selfStreamName = "s_" + UuidGenerator.generate();
            }
            LinkedHashMap<String, Pair<EventType, String>> filterTypes = new LinkedHashMap<String, Pair<EventType, String>>();
            Pair<EventType, String> typePair = new Pair<EventType, String>(finalEventType, eventName);
            filterTypes.put(selfStreamName, typePair);
            filterTypes.putAll(tags.getTaggedEventTypes());

            // for the filter, specify all tags used
            LinkedHashMap<String, Pair<EventType, String>> filterTaggedEventTypes = new LinkedHashMap<String, Pair<EventType, String>>(tags.getTaggedEventTypes());
            filterTaggedEventTypes.remove(optionalTag);

            // handle array tags (match-until clause)
            LinkedHashMap<String, Pair<EventType, String>> arrayCompositeEventTypes = null;
            if (tags.getArrayEventTypes() != null && !tags.getArrayEventTypes().isEmpty()) {
                arrayCompositeEventTypes = new LinkedHashMap<String, Pair<EventType, String>>();

                for (Map.Entry<String, Pair<EventType, String>> entry : tags.getArrayEventTypes().entrySet()) {
                    LinkedHashMap<String, Pair<EventType, String>> specificArrayType = new LinkedHashMap<String, Pair<EventType, String>>();
                    specificArrayType.put(entry.getKey(), entry.getValue());

                    String eventTypeName = services.getEventTypeNameGeneratorStatement().getAnonymousPatternNameWTag(streamNum, evalNode.getFactoryNodeId(), entry.getKey());
                    Map<String, Object> mapProps = getMapProperties(Collections.emptyMap(), specificArrayType);
                    EventTypeMetadata metadata = new EventTypeMetadata(eventTypeName, statementRawInfo.getModuleName(), EventTypeTypeClass.PATTERNDERIVED, EventTypeApplicationType.MAP, NameAccessModifier.TRANSIENT, EventTypeBusModifier.NONBUS, false, EventTypeIdPair.unassigned());
                    MapEventType mapEventType = BaseNestableEventUtil.makeMapTypeCompileTime(metadata, mapProps, null, null, null, null, services.getBeanEventTypeFactoryPrivate(), services.getEventTypeCompileTimeResolver());
                    services.getEventTypeCompileTimeRegistry().newType(mapEventType);

                    String tag = entry.getKey();
                    if (!filterTypes.containsKey(tag)) {
                        Pair<EventType, String> pair = new Pair<EventType, String>(mapEventType, tag);
                        filterTypes.put(tag, pair);
                        arrayCompositeEventTypes.put(tag, pair);
                    }

                    List<StmtClassForgeableFactory> forgeables = SerdeEventTypeUtility.plan(mapEventType, statementRawInfo, services.getSerdeEventTypeRegistry(), services.getSerdeResolver());
                    additionalForgeables.addAll(forgeables);
                }
            }

            StreamTypeService streamTypeService = new StreamTypeServiceImpl(filterTypes, true, false);
            List<ExprNode> exprNodes = filterNode.getRawFilterSpec().getFilterExpressions();

            FilterSpecCompiledDesc compiled = FilterSpecCompiler.makeFilterSpec(resolvedEventType, eventName, exprNodes,
                filterNode.getRawFilterSpec().getOptionalPropertyEvalSpec(), filterTaggedEventTypes, arrayCompositeEventTypes, streamTypeService, null, statementRawInfo, services);
            filterNode.setFilterSpec(compiled.getFilterSpecCompiled());
            additionalForgeables.addAll(compiled.getAdditionalForgeables());
        } else if (evalNode instanceof EvalObserverForgeNode) {
            EvalObserverForgeNode observerNode = (EvalObserverForgeNode) evalNode;
            try {
                ObserverForge observerForge = services.getPatternResolutionService().create(observerNode.getPatternObserverSpec());

                StreamTypeService streamTypeService = getStreamTypeService(tags.getTaggedEventTypes(), tags.getArrayEventTypes(), observerNode, streamNum, statementRawInfo, services);
                ExprValidationContext validationContext = new ExprValidationContextBuilder(streamTypeService, statementRawInfo, services).build();
                List<ExprNode> validated = validateExpressions(ExprNodeOrigin.PATTERNOBSERVER, observerNode.getPatternObserverSpec().getObjectParameters(), validationContext);

                MatchedEventConvertorForge convertor = new MatchedEventConvertorForge(tags.getTaggedEventTypes(), tags.getArrayEventTypes(), allTagNamesOrdered);

                observerNode.setObserverFactory(observerForge);
                observerForge.setObserverParameters(validated, convertor, validationContext);
            } catch (ObserverParameterException e) {
                throw new ExprValidationException("Invalid parameter for pattern observer '" + observerNode.toPrecedenceFreeEPL() + "': " + e.getMessage(), e);
            } catch (PatternObjectException e) {
                throw new ExprValidationException("Failed to resolve pattern observer '" + observerNode.toPrecedenceFreeEPL() + "': " + e.getMessage(), e);
            }
        } else if (evalNode instanceof EvalGuardForgeNode) {
            EvalGuardForgeNode guardNode = (EvalGuardForgeNode) evalNode;
            try {
                GuardForge guardForge = services.getPatternResolutionService().create(guardNode.getPatternGuardSpec());

                StreamTypeService streamTypeService = getStreamTypeService(tags.getTaggedEventTypes(), tags.getArrayEventTypes(), guardNode, streamNum, statementRawInfo, services);
                ExprValidationContext validationContext = new ExprValidationContextBuilder(streamTypeService, statementRawInfo, services).build();
                List<ExprNode> validated = validateExpressions(ExprNodeOrigin.PATTERNGUARD, guardNode.getPatternGuardSpec().getObjectParameters(), validationContext);

                MatchedEventConvertorForge convertor = new MatchedEventConvertorForge(tags.getTaggedEventTypes(), tags.getArrayEventTypes(), allTagNamesOrdered);

                guardNode.setGuardForge(guardForge);
                guardForge.setGuardParameters(validated, convertor, services);
            } catch (GuardParameterException e) {
                throw new ExprValidationException("Invalid parameter for pattern guard '" + guardNode.toPrecedenceFreeEPL() + "': " + e.getMessage(), e);
            } catch (PatternObjectException e) {
                throw new ExprValidationException("Failed to resolve pattern guard '" + guardNode.toPrecedenceFreeEPL() + "': " + e.getMessage(), e);
            }
        } else if (evalNode instanceof EvalEveryDistinctForgeNode) {
            EvalEveryDistinctForgeNode distinctNode = (EvalEveryDistinctForgeNode) evalNode;
            MatchEventSpec matchEventFromChildNodes = analyzeMatchEvent(distinctNode);
            StreamTypeService streamTypeService = getStreamTypeService(matchEventFromChildNodes.getTaggedEventTypes(), matchEventFromChildNodes.getArrayEventTypes(), distinctNode, streamNum, statementRawInfo, services);
            ExprValidationContext validationContext = new ExprValidationContextBuilder(streamTypeService, statementRawInfo, services).build();
            List<ExprNode> validated;
            try {
                validated = validateExpressions(ExprNodeOrigin.PATTERNEVERYDISTINCT, distinctNode.getExpressions(), validationContext);
            } catch (ExprValidationPropertyException ex) {
                throw new ExprValidationPropertyException(ex.getMessage() + ", every-distinct requires that all properties resolve from sub-expressions to the every-distinct", ex.getCause());
            }

            MatchedEventConvertorForge convertor = new MatchedEventConvertorForge(matchEventFromChildNodes.getTaggedEventTypes(), matchEventFromChildNodes.getArrayEventTypes(), allTagNamesOrdered);

            distinctNode.setConvertor(convertor);

            // Determine whether some expressions are constants or time period
            List<ExprNode> distinctExpressions = new ArrayList<ExprNode>();
            TimePeriodComputeForge timePeriodComputeForge = null;
            ExprNode expiryTimeExp = null;
            int count = -1;
            int last = validated.size() - 1;
            for (ExprNode expr : validated) {
                count++;
                if (count == last && expr instanceof ExprTimePeriod) {
                    expiryTimeExp = expr;
                    ExprTimePeriod timePeriodExpr = (ExprTimePeriod) expiryTimeExp;
                    timePeriodComputeForge = timePeriodExpr.getTimePeriodComputeForge();
                } else if (expr.getForge().getForgeConstantType().isCompileTimeConstant()) {
                    if (count == last) {
                        Object value = expr.getForge().getExprEvaluator().evaluate(null, true, null);
                        if (!(value instanceof Number)) {
                            throw new ExprValidationException("Invalid parameter for every-distinct, expected number of seconds constant (constant not considered for distinct)");
                        }
                        Number secondsExpire = (Number) expr.getForge().getExprEvaluator().evaluate(null, true, null);
                        Long timeExpire = secondsExpire == null ? null : services.getClasspathImportServiceCompileTime().getTimeAbacus().deltaForSecondsNumber(secondsExpire);
                        if (timeExpire != null && timeExpire > 0) {
                            timePeriodComputeForge = new TimePeriodComputeConstGivenDeltaForge(timeExpire);
                            expiryTimeExp = expr;
                        } else {
                            log.warn("Invalid seconds-expire " + timeExpire + " for " + ExprNodeUtilityPrint.toExpressionStringMinPrecedenceSafe(expr));
                        }
                    } else {
                        log.warn("Every-distinct node utilizes an expression returning a constant value, please check expression '" + ExprNodeUtilityPrint.toExpressionStringMinPrecedenceSafe(expr) + "', not adding expression to distinct-value expression list");
                    }
                } else {
                    distinctExpressions.add(expr);
                }
            }
            if (distinctExpressions.isEmpty()) {
                throw new ExprValidationException("Every-distinct node requires one or more distinct-value expressions that each return non-constant result values");
            }

            MultiKeyPlan multiKeyPlan = MultiKeyPlanner.planMultiKey(distinctExpressions.toArray(new ExprNode[0]), false, statementRawInfo, services.getSerdeResolver());
            distinctNode.setDistinctExpressions(distinctExpressions, multiKeyPlan.getClassRef(), timePeriodComputeForge, expiryTimeExp);
            additionalForgeables.addAll(multiKeyPlan.getMultiKeyForgeables());
        } else if (evalNode instanceof EvalMatchUntilForgeNode) {
            EvalMatchUntilForgeNode matchUntilNode = (EvalMatchUntilForgeNode) evalNode;

            // compile bounds expressions, if any
            MatchEventSpec untilMatchEventSpec = new MatchEventSpec(tags.getTaggedEventTypes(), tags.getArrayEventTypes());
            StreamTypeService streamTypeService = getStreamTypeService(untilMatchEventSpec.getTaggedEventTypes(), untilMatchEventSpec.getArrayEventTypes(), matchUntilNode, streamNum, statementRawInfo, services);
            ExprValidationContext validationContext = new ExprValidationContextBuilder(streamTypeService, statementRawInfo, services).build();

            ExprNode lower = validateBounds(matchUntilNode.getLowerBounds(), validationContext);
            matchUntilNode.setLowerBounds(lower);

            ExprNode upper = validateBounds(matchUntilNode.getUpperBounds(), validationContext);
            matchUntilNode.setUpperBounds(upper);

            ExprNode single = validateBounds(matchUntilNode.getSingleBound(), validationContext);
            matchUntilNode.setSingleBound(single);

            boolean tightlyBound;
            if (matchUntilNode.getSingleBound() != null) {
                validateMatchUntil(matchUntilNode.getSingleBound(), matchUntilNode.getSingleBound(), false);
                tightlyBound = true;
            } else {
                boolean allowZeroLowerBounds = matchUntilNode.getLowerBounds() != null && matchUntilNode.getUpperBounds() != null;
                tightlyBound = validateMatchUntil(matchUntilNode.getLowerBounds(), matchUntilNode.getUpperBounds(), allowZeroLowerBounds);
            }
            if (matchUntilNode.getSingleBound() == null && !tightlyBound && matchUntilNode.getChildNodes().size() < 2) {
                throw new ExprValidationException("Variable bounds repeat operator requires an until-expression");
            }

            MatchedEventConvertorForge convertor = new MatchedEventConvertorForge(untilMatchEventSpec.getTaggedEventTypes(), untilMatchEventSpec.getArrayEventTypes(), allTagNamesOrdered);
            matchUntilNode.setConvertor(convertor);

            // compile new tag lists
            Set<String> arrayTags = null;
            EvalNodeAnalysisResult matchUntilAnalysisResult = EvalNodeUtil.recursiveAnalyzeChildNodes(matchUntilNode.getChildNodes().get(0));
            for (EvalFilterForgeNode filterNode : matchUntilAnalysisResult.getFilterNodes()) {
                String optionalTag = filterNode.getEventAsName();
                if (optionalTag != null) {
                    if (arrayTags == null) {
                        arrayTags = new HashSet<String>();
                    }
                    arrayTags.add(optionalTag);
                }
            }

            if (arrayTags != null) {
                for (String arrayTag : arrayTags) {
                    if (!tags.getArrayEventTypes().containsKey(arrayTag)) {
                        tags.getArrayEventTypes().put(arrayTag, tags.getTaggedEventTypes().get(arrayTag));
                        tags.getTaggedEventTypes().remove(arrayTag);
                    }
                }
            }
            matchUntilNode.setTagsArrayedSet(getIndexesForTags(allTagNamesOrdered, arrayTags));
        } else if (evalNode instanceof EvalFollowedByForgeNode) {
            EvalFollowedByForgeNode followedByNode = (EvalFollowedByForgeNode) evalNode;
            StreamTypeService streamTypeService = new StreamTypeServiceImpl(false);
            ExprValidationContext validationContext = new ExprValidationContextBuilder(streamTypeService, statementRawInfo, services).build();

            if (followedByNode.getOptionalMaxExpressions() != null) {
                List<ExprNode> validated = new ArrayList<ExprNode>();
                for (ExprNode maxExpr : followedByNode.getOptionalMaxExpressions()) {
                    if (maxExpr == null) {
                        validated.add(null);
                    } else {
                        ExprNodeSummaryVisitor visitor = new ExprNodeSummaryVisitor();
                        maxExpr.accept(visitor);
                        if (!visitor.isPlain()) {
                            String errorMessage = "Invalid maximum expression in followed-by, " + visitor.getMessage() + " are not allowed within the expression";
                            log.error(errorMessage);
                            throw new ExprValidationException(errorMessage);
                        }

                        ExprNode validatedExpr = ExprNodeUtilityValidate.getValidatedSubtree(ExprNodeOrigin.FOLLOWEDBYMAX, maxExpr, validationContext);
                        validated.add(validatedExpr);
                        Class returnType = validatedExpr.getForge().getEvaluationType();
                        if ((returnType == null) || (!JavaClassHelper.isNumeric(returnType))) {
                            String message = "Invalid maximum expression in followed-by, the expression must return an integer value";
                            throw new ExprValidationException(message);
                        }
                    }
                }
                followedByNode.setOptionalMaxExpressions(validated);
            }
        }

        if (newTaggedEventTypes != null) {
            tags.getTaggedEventTypes().putAll(newTaggedEventTypes);
        }
        if (newArrayEventTypes != null) {
            tags.getArrayEventTypes().putAll(newArrayEventTypes);
        }
    }

    private static ExprNode validateBounds(ExprNode bounds, ExprValidationContext validationContext) throws ExprValidationException {
        String message = "Match-until bounds value expressions must return a numeric value";
        if (bounds != null) {
            ExprNode validated = ExprNodeUtilityValidate.getValidatedSubtree(ExprNodeOrigin.PATTERNMATCHUNTILBOUNDS, bounds, validationContext);
            Class returnType = validated.getForge().getEvaluationType();
            if ((returnType == null) || (!JavaClassHelper.isNumeric(returnType))) {
                throw new ExprValidationException(message);
            }
            return validated;
        }
        return null;
    }

    private static int[] getIndexesForTags(LinkedHashSet<String> allTagNamesOrdered, Set<String> arrayTags) {
        if (arrayTags == null || arrayTags.isEmpty()) {
            return new int[0];
        }
        int[] indexes = new int[arrayTags.size()];
        int count = 0;
        for (String arrayTag : arrayTags) {
            int index = 0;
            int found = findTagNumber(arrayTag, allTagNamesOrdered);
            indexes[count] = found;
            count++;
        }
        return indexes;
    }

    private static boolean isParentMatchUntil(EvalForgeNode currentNode, Stack<EvalForgeNode> parentNodeStack) {
        if (parentNodeStack.isEmpty()) {
            return false;
        }

        for (EvalForgeNode deepParent : parentNodeStack) {
            if (deepParent instanceof EvalMatchUntilForgeNode) {
                EvalMatchUntilForgeNode matchUntilFactoryNode = (EvalMatchUntilForgeNode) deepParent;
                if (matchUntilFactoryNode.getChildNodes().get(0) == currentNode) {
                    return true;
                }
            }
        }
        return false;
    }

    private static MatchEventSpec analyzeMatchEvent(EvalForgeNode relativeNode) {
        LinkedHashMap<String, Pair<EventType, String>> taggedEventTypes = new LinkedHashMap<String, Pair<EventType, String>>();
        LinkedHashMap<String, Pair<EventType, String>> arrayEventTypes = new LinkedHashMap<String, Pair<EventType, String>>();

        // Determine all the filter nodes used in the pattern
        EvalNodeAnalysisResult evalNodeAnalysisResult = EvalNodeUtil.recursiveAnalyzeChildNodes(relativeNode);

        // collect all filters underneath
        for (EvalFilterForgeNode filterNode : evalNodeAnalysisResult.getFilterNodes()) {
            String optionalTag = filterNode.getEventAsName();
            if (optionalTag != null) {
                taggedEventTypes.put(optionalTag, new Pair<EventType, String>(filterNode.getFilterSpecCompiled().getFilterForEventType(), filterNode.getFilterSpecCompiled().getFilterForEventTypeName()));
            }
        }

        // collect those filters under a repeat since they are arrays
        Set<String> arrayTags = new HashSet<String>();
        for (EvalMatchUntilForgeNode matchUntilNode : evalNodeAnalysisResult.getRepeatNodes()) {
            EvalNodeAnalysisResult matchUntilAnalysisResult = EvalNodeUtil.recursiveAnalyzeChildNodes(matchUntilNode.getChildNodes().get(0));
            for (EvalFilterForgeNode filterNode : matchUntilAnalysisResult.getFilterNodes()) {
                String optionalTag = filterNode.getEventAsName();
                if (optionalTag != null) {
                    arrayTags.add(optionalTag);
                }
            }
        }

        // for each array tag change collection
        for (String arrayTag : arrayTags) {
            if (taggedEventTypes.get(arrayTag) != null) {
                arrayEventTypes.put(arrayTag, taggedEventTypes.get(arrayTag));
                taggedEventTypes.remove(arrayTag);
            }
        }

        return new MatchEventSpec(taggedEventTypes, arrayEventTypes);
    }

    public static StreamSpecCompiled compileMethod(MethodStreamSpec methodStreamSpec) throws ExprValidationException {
        if (!methodStreamSpec.getIdent().equals("method")) {
            throw new ExprValidationException("Expecting keyword 'method', found '" + methodStreamSpec.getIdent() + "'");
        }
        if (methodStreamSpec.getMethodName() == null) {
            throw new ExprValidationException("No method name specified for method-based join");
        }
        return methodStreamSpec;
    }

    private static StreamTypeService getStreamTypeService(Map<String, Pair<EventType, String>> taggedEventTypes, Map<String, Pair<EventType, String>> arrayEventTypes, EvalForgeNode forge, int streamNum, StatementRawInfo statementRawInfo, StatementCompileTimeServices services) {
        LinkedHashMap<String, Pair<EventType, String>> filterTypes = new LinkedHashMap<String, Pair<EventType, String>>();
        filterTypes.putAll(taggedEventTypes);

        // handle array tags (match-until clause)
        if (arrayEventTypes != null) {
            String eventTypeName = services.getEventTypeNameGeneratorStatement().getAnonymousPatternName(streamNum, forge.getFactoryNodeId());
            EventTypeMetadata metadata = new EventTypeMetadata(eventTypeName, statementRawInfo.getModuleName(), EventTypeTypeClass.PATTERNDERIVED, EventTypeApplicationType.MAP, NameAccessModifier.TRANSIENT, EventTypeBusModifier.NONBUS, false, EventTypeIdPair.unassigned());
            Map<String, Object> mapProperties = getMapProperties(new HashMap(), arrayEventTypes);
            MapEventType mapEventType = BaseNestableEventUtil.makeMapTypeCompileTime(metadata, mapProperties, null, null, null, null, services.getBeanEventTypeFactoryPrivate(), services.getEventTypeCompileTimeResolver());
            services.getEventTypeCompileTimeRegistry().newType(mapEventType);

            EventType arrayTagCompositeEventType = mapEventType;
            for (Map.Entry<String, Pair<EventType, String>> entry : arrayEventTypes.entrySet()) {
                String tag = entry.getKey();
                if (!filterTypes.containsKey(tag)) {
                    Pair<EventType, String> pair = new Pair<EventType, String>(arrayTagCompositeEventType, tag);
                    filterTypes.put(tag, pair);
                }
            }
        }

        return new StreamTypeServiceImpl(filterTypes, true, false);
    }

    private static int findTagNumber(String findTag, LinkedHashSet<String> allTagNamesOrdered) {
        int index = 0;
        for (String tag : allTagNamesOrdered) {
            if (findTag.equals(tag)) {
                return index;
            }
            index++;
        }
        throw new EPException("Failed to find tag '" + findTag + "' among known tags");
    }

    private static List<ExprNode> validateExpressions(ExprNodeOrigin exprNodeOrigin, List<ExprNode> objectParameters, ExprValidationContext validationContext)
            throws ExprValidationException {
        if (objectParameters == null) {
            return objectParameters;
        }
        List<ExprNode> validated = new ArrayList<ExprNode>();
        for (ExprNode node : objectParameters) {
            validated.add(ExprNodeUtilityValidate.getValidatedSubtree(exprNodeOrigin, node, validationContext));
        }
        return validated;
    }

    private static class FilterForFilterFactoryNodes implements EvalNodeUtilFactoryFilter {
        public final static FilterForFilterFactoryNodes INSTANCE = new FilterForFilterFactoryNodes();

        public boolean consider(EvalForgeNode node) {
            return node instanceof EvalFilterForgeNode;
        }
    }

    public static EventType resolveTypeName(String eventTypeName, EventTypeCompileTimeResolver eventTypeCompileTimeResolver) throws ExprValidationException {
        EventType eventType = eventTypeCompileTimeResolver.getTypeByName(eventTypeName);
        if (eventType == null) {
            throw new ExprValidationException("Failed to resolve event type, named window or table by name '" + eventTypeName + "'");
        }
        return eventType;
    }

    private static Map<String, Object> getMapProperties(Map<String, Pair<EventType, String>> taggedEventTypes, Map<String, Pair<EventType, String>> arrayEventTypes) {
        Map<String, Object> mapProperties = new LinkedHashMap<String, Object>();
        for (Map.Entry<String, Pair<EventType, String>> entry : taggedEventTypes.entrySet()) {
            mapProperties.put(entry.getKey(), entry.getValue().getFirst());
        }
        for (Map.Entry<String, Pair<EventType, String>> entry : arrayEventTypes.entrySet()) {
            mapProperties.put(entry.getKey(), new EventType[]{entry.getValue().getFirst()});
        }
        return mapProperties;
    }

    /**
     * Validate.
     *
     * @param lowerBounds      is the lower bounds, or null if none supplied
     * @param upperBounds      is the upper bounds, or null if none supplied
     * @param isAllowLowerZero true to allow zero value for lower range
     * @return true if closed range of constants and the constants are the same value
     * @throws ExprValidationException validation ex
     */
    public static boolean validateMatchUntil(ExprNode lowerBounds, ExprNode upperBounds, boolean isAllowLowerZero) throws ExprValidationException {
        boolean isConstants = true;
        Object constantLower = null;
        String numericMessage = "Match-until bounds expect a numeric or expression value";
        if (lowerBounds != null && lowerBounds.getForge().getForgeConstantType() == ExprForgeConstantType.COMPILETIMECONST) {
            constantLower = lowerBounds.getForge().getExprEvaluator().evaluate(null, true, null);
            if (constantLower == null || !(constantLower instanceof Number)) {
                throw new ExprValidationException(numericMessage);
            }
        } else {
            isConstants = lowerBounds == null;
        }

        Object constantUpper = null;
        if (upperBounds != null && upperBounds.getForge().getForgeConstantType() == ExprForgeConstantType.COMPILETIMECONST) {
            constantUpper = upperBounds.getForge().getExprEvaluator().evaluate(null, true, null);
            if (constantUpper == null || !(constantUpper instanceof Number)) {
                throw new ExprValidationException(numericMessage);
            }
        } else {
            isConstants = isConstants && upperBounds == null;
        }

        if (!isConstants) {
            return true;
        }

        if (constantLower != null && constantUpper != null) {
            Integer lower = ((Number) constantLower).intValue();
            Integer upper = ((Number) constantUpper).intValue();
            if (lower > upper) {
                throw new ExprValidationException("Incorrect range specification, lower bounds value '" + lower +
                        "' is higher then higher bounds '" + upper + "'");
            }
        }
        verifyMatchUntilConstant(constantLower, isAllowLowerZero);
        verifyMatchUntilConstant(constantUpper, false);

        return constantLower != null && constantUpper != null && constantLower.equals(constantUpper);
    }

    private static void verifyMatchUntilConstant(Object value, boolean isAllowZero) throws ExprValidationException {
        if (value != null) {
            Integer bound = ((Number) value).intValue();
            if (isAllowZero) {
                if (bound < 0) {
                    throw new ExprValidationException("Incorrect range specification, a bounds value of negative value is not allowed");
                }
            } else {
                if (bound <= 0) {
                    throw new ExprValidationException("Incorrect range specification, a bounds value of zero or negative value is not allowed");
                }
            }
        }
    }
}
