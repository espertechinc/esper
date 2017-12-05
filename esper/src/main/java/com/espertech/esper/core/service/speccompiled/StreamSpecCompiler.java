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
package com.espertech.esper.core.service.speccompiled;

import com.espertech.esper.client.EPException;
import com.espertech.esper.client.EventType;
import com.espertech.esper.client.annotation.Audit;
import com.espertech.esper.client.annotation.AuditEnum;
import com.espertech.esper.collection.Pair;
import com.espertech.esper.core.service.ExprEvaluatorContextStatement;
import com.espertech.esper.core.service.StatementContext;
import com.espertech.esper.epl.core.streamtype.StreamTypeService;
import com.espertech.esper.epl.core.streamtype.StreamTypeServiceImpl;
import com.espertech.esper.epl.expression.core.*;
import com.espertech.esper.epl.expression.time.ExprTimePeriod;
import com.espertech.esper.epl.expression.time.ExprTimePeriodEvalDeltaConst;
import com.espertech.esper.epl.expression.time.ExprTimePeriodEvalDeltaConstGivenDelta;
import com.espertech.esper.epl.expression.visitor.ExprNodeSummaryVisitor;
import com.espertech.esper.epl.property.PropertyEvaluatorFactory;
import com.espertech.esper.epl.spec.*;
import com.espertech.esper.epl.table.mgmt.TableMetadata;
import com.espertech.esper.epl.util.ExprNodeUtilityRich;
import com.espertech.esper.event.EventAdapterException;
import com.espertech.esper.event.EventAdapterService;
import com.espertech.esper.event.EventTypeSPI;
import com.espertech.esper.filter.FilterSpecCompiler;
import com.espertech.esper.filterspec.FilterSpecCompiled;
import com.espertech.esper.filterspec.PropertyEvaluator;
import com.espertech.esper.pattern.*;
import com.espertech.esper.pattern.guard.GuardFactory;
import com.espertech.esper.pattern.guard.GuardParameterException;
import com.espertech.esper.pattern.observer.ObserverFactory;
import com.espertech.esper.pattern.observer.ObserverParameterException;
import com.espertech.esper.util.EPServiceProviderName;
import com.espertech.esper.util.JavaClassHelper;
import com.espertech.esper.util.UuidGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.StringWriter;
import java.net.URI;
import java.util.*;

public class StreamSpecCompiler {
    private final static Logger log = LoggerFactory.getLogger(StreamSpecCompiler.class);

    /**
     * Compiles a raw stream specification consisting event type information and filter expressions
     * to an validated, optimized form for use with filter service
     *
     * @param spec                    spec
     * @param statementContext        statement-level services
     * @param eventTypeReferences     event type names used by the statement
     * @param isInsertInto            true for insert-into
     * @param isJoin                  indicates whether a join or not a join
     * @param isContextDeclaration    indicates whether declared as part of the context declarations, if any
     * @param isOnTrigger             indicator for on-trigger
     * @param optionalStreamName      stream name
     * @param assignedTypeNumberStack for assigning nested type numbers
     * @return compiled stream
     * @throws ExprValidationException to indicate validation errors
     */
    public static StreamSpecCompiled compile(StreamSpecRaw spec,
                                             StatementContext statementContext,
                                             Set<String> eventTypeReferences,
                                             boolean isInsertInto,
                                             Collection<Integer> assignedTypeNumberStack,
                                             boolean isJoin,
                                             boolean isContextDeclaration,
                                             boolean isOnTrigger,
                                             String optionalStreamName)
            throws ExprValidationException {
        if (spec instanceof DBStatementStreamSpec) {
            return (DBStatementStreamSpec) spec;
        } else if (spec instanceof FilterStreamSpecRaw) {
            return compile((FilterStreamSpecRaw) spec, statementContext, eventTypeReferences, isInsertInto, assignedTypeNumberStack, isJoin, isContextDeclaration, isOnTrigger, optionalStreamName);
        } else if (spec instanceof PatternStreamSpecRaw) {
            return compile((PatternStreamSpecRaw) spec, statementContext, eventTypeReferences, isInsertInto, assignedTypeNumberStack, isJoin, isContextDeclaration, isOnTrigger, optionalStreamName);
        } else if (spec instanceof MethodStreamSpec) {
            return compile((MethodStreamSpec) spec);
        }
        throw new IllegalStateException("Unrecognized stream spec " + spec);
    }

    public static StreamSpecCompiled compile(FilterStreamSpecRaw streamSpec, StatementContext context, Set<String> eventTypeReferences, boolean isInsertInto, Collection<Integer> assignedTypeNumberStack, boolean isJoin, boolean isContextDeclaration, boolean isOnTrigger, String optionalStreamName)
            throws ExprValidationException {
        // Determine the event type
        FilterSpecRaw rawFilterSpec = streamSpec.getRawFilterSpec();
        String eventName = rawFilterSpec.getEventTypeName();

        if (context.getTableService() != null && context.getTableService().getTableMetadata(eventName) != null) {
            if (streamSpec.getViewSpecs() != null && streamSpec.getViewSpecs().length > 0) {
                throw new ExprValidationException("Views are not supported with tables");
            }
            if (streamSpec.getRawFilterSpec().getOptionalPropertyEvalSpec() != null) {
                throw new ExprValidationException("Contained-event expressions are not supported with tables");
            }
            TableMetadata tableMetadata = context.getTableService().getTableMetadata(eventName);
            StreamTypeService streamTypeService = new StreamTypeServiceImpl(new EventType[]{tableMetadata.getInternalEventType()}, new String[]{optionalStreamName}, new boolean[]{true}, context.getEngineURI(), false, false);
            List<ExprNode> validatedNodes = FilterSpecCompiler.validateAllowSubquery(ExprNodeOrigin.FILTER, rawFilterSpec.getFilterExpressions(), streamTypeService, context, null, null);
            return new TableQueryStreamSpec(streamSpec.getOptionalStreamName(), streamSpec.getViewSpecs(), streamSpec.getOptions(), eventName, validatedNodes);
        }

        // Could be a named window
        if (context.getNamedWindowMgmtService().isNamedWindow(eventName)) {
            EventType namedWindowType = context.getNamedWindowMgmtService().getProcessor(eventName).getTailView().getEventType();
            StreamTypeService streamTypeService = new StreamTypeServiceImpl(new EventType[]{namedWindowType}, new String[]{optionalStreamName}, new boolean[]{true}, context.getEngineURI(), false, false);

            List<ExprNode> validatedNodes = FilterSpecCompiler.validateAllowSubquery(ExprNodeOrigin.FILTER, rawFilterSpec.getFilterExpressions(), streamTypeService, context, null, null);

            PropertyEvaluator optionalPropertyEvaluator = null;
            if (rawFilterSpec.getOptionalPropertyEvalSpec() != null) {
                optionalPropertyEvaluator = PropertyEvaluatorFactory.makeEvaluator(rawFilterSpec.getOptionalPropertyEvalSpec(), namedWindowType, streamSpec.getOptionalStreamName(), context.getEventAdapterService(), context.getEngineImportService(), context.getTimeProvider(), context.getVariableService(), context.getTableService(), context.getEngineURI(), context.getStatementId(), context.getStatementName(), context.getAnnotations(), assignedTypeNumberStack, context.getConfigSnapshot(), context.getNamedWindowMgmtService(), context.getStatementExtensionServicesContext());
            }
            eventTypeReferences.add(((EventTypeSPI) namedWindowType).getMetadata().getPrimaryName());
            return new NamedWindowConsumerStreamSpec(eventName, streamSpec.getOptionalStreamName(), streamSpec.getViewSpecs(), validatedNodes, streamSpec.getOptions(), optionalPropertyEvaluator);
        }

        EventType eventType = null;

        if (context.getValueAddEventService().isRevisionTypeName(eventName)) {
            eventType = context.getValueAddEventService().getValueAddUnderlyingType(eventName);
            eventTypeReferences.add(((EventTypeSPI) eventType).getMetadata().getPrimaryName());
        }

        if (eventType == null) {
            eventType = resolveType(context.getEngineURI(), eventName, context.getEventAdapterService(), context.getPlugInTypeResolutionURIs());
            if (eventType instanceof EventTypeSPI) {
                eventTypeReferences.add(((EventTypeSPI) eventType).getMetadata().getPrimaryName());
            }
        }

        // Validate all nodes, make sure each returns a boolean and types are good;
        // Also decompose all AND super nodes into individual expressions
        StreamTypeService streamTypeService = new StreamTypeServiceImpl(new EventType[]{eventType}, new String[]{streamSpec.getOptionalStreamName()}, new boolean[]{true}, context.getEngineURI(), false, false);

        FilterSpecCompiled spec = FilterSpecCompiler.makeFilterSpec(eventType, eventName, rawFilterSpec.getFilterExpressions(), rawFilterSpec.getOptionalPropertyEvalSpec(),
                null, null,  // no tags
                streamTypeService, streamSpec.getOptionalStreamName(), context, assignedTypeNumberStack);

        return new FilterStreamSpecCompiled(spec, streamSpec.getViewSpecs(), streamSpec.getOptionalStreamName(), streamSpec.getOptions());
    }

    /**
     * Resolves a given event name to an event type.
     *
     * @param eventName              is the name to resolve
     * @param eventAdapterService    for resolving event types
     * @param engineURI              the provider URI
     * @param optionalResolutionURIs is URIs for resolving the event name against plug-inn event representations, if any
     * @return event type
     * @throws ExprValidationException if the info cannot be resolved
     */
    public static EventType resolveType(String engineURI, String eventName, EventAdapterService eventAdapterService, URI[] optionalResolutionURIs)
            throws ExprValidationException {
        EventType eventType = eventAdapterService.getExistsTypeByName(eventName);

        // may already be known
        if (eventType != null) {
            return eventType;
        }

        String engineURIQualifier = engineURI;
        if (engineURI == null || EPServiceProviderName.DEFAULT_ENGINE_URI.equals(engineURI)) {
            engineURIQualifier = EPServiceProviderName.DEFAULT_ENGINE_URI_QUALIFIER;
        }

        // The event name can be prefixed by the engine URI, i.e. "select * from default.MyEvent"
        if (eventName.startsWith(engineURIQualifier)) {
            int indexDot = eventName.indexOf(".");
            if (indexDot > 0) {
                String eventNameURI = eventName.substring(0, indexDot);
                String eventNameRemainder = eventName.substring(indexDot + 1);

                if (engineURIQualifier.equals(eventNameURI)) {
                    eventType = eventAdapterService.getExistsTypeByName(eventNameRemainder);
                }
            }
        }

        // may now be known
        if (eventType != null) {
            return eventType;
        }

        // The type is not known yet, attempt to add as a JavaBean type with the same name
        String message = null;
        try {
            eventType = eventAdapterService.addBeanType(eventName, eventName, true, false, false, false);
        } catch (EventAdapterException ex) {
            log.debug(".resolveType Event type named '" + eventName + "' not resolved as Java-Class event");
            message = "Failed to resolve event type: " + ex.getMessage();
        }

        // Attempt to use plug-in event types
        try {
            eventType = eventAdapterService.addPlugInEventType(eventName, optionalResolutionURIs, null);
        } catch (EventAdapterException ex) {
            log.debug(".resolveType Event type named '" + eventName + "' not resolved by plug-in event representations");
            // remains unresolved
        }

        if (eventType == null) {
            throw new ExprValidationException(message);
        }
        return eventType;
    }

    public static PatternStreamSpecCompiled compile(PatternStreamSpecRaw streamSpecRaw,
                                                    StatementContext context,
                                                    Set<String> eventTypeReferences,
                                                    boolean isInsertInto,
                                                    Collection<Integer> assignedTypeNumberStack,
                                                    boolean isJoin,
                                                    boolean isContextDeclaration,
                                                    boolean isOnTrigger,
                                                    String optionalStreamName)
            throws ExprValidationException {
        return compileInternal(streamSpecRaw, context, eventTypeReferences, isInsertInto, assignedTypeNumberStack, null, null, isJoin, isContextDeclaration, isOnTrigger);
    }

    public static PatternStreamSpecCompiled compile(PatternStreamSpecRaw streamSpecRaw,
                                                    StatementContext context,
                                                    Set<String> eventTypeReferences,
                                                    boolean isInsertInto,
                                                    Collection<Integer> assignedTypeNumberStack,
                                                    MatchEventSpec priorTags,
                                                    Set<String> priorAllTags,
                                                    boolean isJoin,
                                                    boolean isContextDeclaration,
                                                    boolean isOnTrigger)
            throws ExprValidationException {
        return compileInternal(streamSpecRaw, context, eventTypeReferences, isInsertInto, assignedTypeNumberStack, priorTags, priorAllTags, isJoin, isContextDeclaration, isOnTrigger);
    }

    private static PatternStreamSpecCompiled compileInternal(PatternStreamSpecRaw streamSpecRaw,
                                                             StatementContext context,
                                                             Set<String> eventTypeReferences,
                                                             boolean isInsertInto,
                                                             Collection<Integer> assignedTypeNumberStack,
                                                             MatchEventSpec tags,
                                                             Set<String> priorAllTags,
                                                             boolean isJoin,
                                                             boolean isContextDeclaration,
                                                             boolean isOnTrigger)
            throws ExprValidationException {
        // validate
        if ((streamSpecRaw.isSuppressSameEventMatches() || streamSpecRaw.isDiscardPartialsOnMatch()) && (isJoin || isContextDeclaration || isOnTrigger)) {
            throw new ExprValidationException("Discard-partials and suppress-matches is not supported in a joins, context declaration and on-action");
        }

        if (tags == null) {
            tags = new MatchEventSpec();
        }
        Deque<Integer> subexpressionIdStack = new ArrayDeque<Integer>(assignedTypeNumberStack);
        ExprEvaluatorContext evaluatorContextStmt = new ExprEvaluatorContextStatement(context, false);
        Stack<EvalFactoryNode> nodeStack = new Stack<EvalFactoryNode>();

        // detemine ordered tags
        Set<EvalFactoryNode> filterFactoryNodes = EvalNodeUtil.recursiveGetChildNodes(streamSpecRaw.getEvalFactoryNode(), FilterForFilterFactoryNodes.INSTANCE);
        LinkedHashSet<String> allTagNamesOrdered = new LinkedHashSet<String>();
        if (priorAllTags != null) {
            allTagNamesOrdered.addAll(priorAllTags);
        }
        for (EvalFactoryNode filterNode : filterFactoryNodes) {
            EvalFilterFactoryNode factory = (EvalFilterFactoryNode) filterNode;
            int tagNumber;
            if (factory.getEventAsName() != null) {
                if (!allTagNamesOrdered.contains(factory.getEventAsName())) {
                    allTagNamesOrdered.add(factory.getEventAsName());
                    tagNumber = allTagNamesOrdered.size() - 1;
                } else {
                    tagNumber = findTagNumber(factory.getEventAsName(), allTagNamesOrdered);
                }
                factory.setEventAsTagNumber(tagNumber);
            }
        }

        recursiveCompile(streamSpecRaw.getEvalFactoryNode(), context, evaluatorContextStmt, eventTypeReferences, isInsertInto, tags, subexpressionIdStack, nodeStack, allTagNamesOrdered);

        Audit auditPattern = AuditEnum.PATTERN.getAudit(context.getAnnotations());
        Audit auditPatternInstance = AuditEnum.PATTERNINSTANCES.getAudit(context.getAnnotations());
        EvalFactoryNode compiledEvalFactoryNode = streamSpecRaw.getEvalFactoryNode();
        if (context.getPatternNodeFactory().isAuditSupported() && (auditPattern != null || auditPatternInstance != null)) {
            EvalAuditInstanceCount instanceCount = new EvalAuditInstanceCount();
            compiledEvalFactoryNode = recursiveAddAuditNode(context.getPatternNodeFactory(), null, auditPattern != null, auditPatternInstance != null, streamSpecRaw.getEvalFactoryNode(), instanceCount);
        }

        return new PatternStreamSpecCompiled(compiledEvalFactoryNode, tags.getTaggedEventTypes(), tags.getArrayEventTypes(), allTagNamesOrdered, streamSpecRaw.getViewSpecs(), streamSpecRaw.getOptionalStreamName(), streamSpecRaw.getOptions(), streamSpecRaw.isSuppressSameEventMatches(), streamSpecRaw.isDiscardPartialsOnMatch());
    }

    private static void recursiveCompile(EvalFactoryNode evalNode, StatementContext context, ExprEvaluatorContext evaluatorContext, Set<String> eventTypeReferences, boolean isInsertInto, MatchEventSpec tags, Deque<Integer> subexpressionIdStack, Stack<EvalFactoryNode> parentNodeStack, LinkedHashSet<String> allTagNamesOrdered) throws ExprValidationException {
        int counter = 0;
        parentNodeStack.push(evalNode);
        for (EvalFactoryNode child : evalNode.getChildNodes()) {
            subexpressionIdStack.addLast(counter++);
            recursiveCompile(child, context, evaluatorContext, eventTypeReferences, isInsertInto, tags, subexpressionIdStack, parentNodeStack, allTagNamesOrdered);
            subexpressionIdStack.removeLast();
        }
        parentNodeStack.pop();

        LinkedHashMap<String, Pair<EventType, String>> newTaggedEventTypes = null;
        LinkedHashMap<String, Pair<EventType, String>> newArrayEventTypes = null;

        if (evalNode instanceof EvalFilterFactoryNode) {
            EvalFilterFactoryNode filterNode = (EvalFilterFactoryNode) evalNode;
            String eventName = filterNode.getRawFilterSpec().getEventTypeName();
            if (context.getTableService().getTableMetadata(eventName) != null) {
                throw new ExprValidationException("Tables cannot be used in pattern filter atoms");
            }

            EventType resolvedEventType = resolveType(context.getEngineURI(), eventName, context.getEventAdapterService(), context.getPlugInTypeResolutionURIs());
            EventType finalEventType = resolvedEventType;
            String optionalTag = filterNode.getEventAsName();
            boolean isPropertyEvaluation = false;
            boolean isParentMatchUntil = isParentMatchUntil(evalNode, parentNodeStack);

            // obtain property event type, if final event type is properties
            if (filterNode.getRawFilterSpec().getOptionalPropertyEvalSpec() != null) {
                PropertyEvaluator optionalPropertyEvaluator = PropertyEvaluatorFactory.makeEvaluator(filterNode.getRawFilterSpec().getOptionalPropertyEvalSpec(), resolvedEventType, filterNode.getEventAsName(), context.getEventAdapterService(), context.getEngineImportService(), context.getSchedulingService(), context.getVariableService(), context.getTableService(), context.getEngineURI(), context.getStatementId(), context.getStatementName(), context.getAnnotations(), subexpressionIdStack, context.getConfigSnapshot(), context.getNamedWindowMgmtService(), context.getStatementExtensionServicesContext());
                finalEventType = optionalPropertyEvaluator.getFragmentEventType();
                isPropertyEvaluation = true;
            }

            if (finalEventType instanceof EventTypeSPI) {
                eventTypeReferences.add(((EventTypeSPI) finalEventType).getMetadata().getPrimaryName());
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
                pair = new Pair<EventType, String>(finalEventType, eventName);

                // add tagged type
                if (isPropertyEvaluation || isParentMatchUntil) {
                    newArrayEventTypes = new LinkedHashMap<String, Pair<EventType, String>>();
                    newArrayEventTypes.put(optionalTag, pair);
                } else {
                    newTaggedEventTypes = new LinkedHashMap<String, Pair<EventType, String>>();
                    newTaggedEventTypes.put(optionalTag, pair);
                }
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
                String patternSubexEventType = getPatternSubexEventType(context.getStatementId(), "pattern", subexpressionIdStack);

                for (Map.Entry<String, Pair<EventType, String>> entry : tags.getArrayEventTypes().entrySet()) {
                    LinkedHashMap<String, Pair<EventType, String>> specificArrayType = new LinkedHashMap<String, Pair<EventType, String>>();
                    specificArrayType.put(entry.getKey(), entry.getValue());
                    EventType arrayTagCompositeEventType = context.getEventAdapterService().createSemiAnonymousMapType(patternSubexEventType, Collections.<String, Pair<EventType, String>>emptyMap(), specificArrayType, isInsertInto);
                    context.getStatementSemiAnonymousTypeRegistry().register(arrayTagCompositeEventType);

                    String tag = entry.getKey();
                    if (!filterTypes.containsKey(tag)) {
                        Pair<EventType, String> pair = new Pair<EventType, String>(arrayTagCompositeEventType, tag);
                        filterTypes.put(tag, pair);
                        arrayCompositeEventTypes.put(tag, pair);
                    }
                }
            }

            StreamTypeService streamTypeService = new StreamTypeServiceImpl(filterTypes, context.getEngineURI(), true, false);
            List<ExprNode> exprNodes = filterNode.getRawFilterSpec().getFilterExpressions();

            FilterSpecCompiled spec = FilterSpecCompiler.makeFilterSpec(resolvedEventType, eventName, exprNodes,
                    filterNode.getRawFilterSpec().getOptionalPropertyEvalSpec(), filterTaggedEventTypes, arrayCompositeEventTypes, streamTypeService,
                    null, context, subexpressionIdStack);
            filterNode.setFilterSpec(spec);
        } else if (evalNode instanceof EvalObserverFactoryNode) {
            EvalObserverFactoryNode observerNode = (EvalObserverFactoryNode) evalNode;
            try {
                ObserverFactory observerFactory = context.getPatternResolutionService().create(observerNode.getPatternObserverSpec());

                StreamTypeService streamTypeService = getStreamTypeService(context.getEngineURI(), context.getStatementId(), context.getEventAdapterService(), tags.getTaggedEventTypes(), tags.getArrayEventTypes(), subexpressionIdStack, "observer", context);
                ExprValidationContext validationContext = new ExprValidationContext(streamTypeService, context.getEngineImportService(), context.getStatementExtensionServicesContext(), null, context.getSchedulingService(), context.getVariableService(), context.getTableService(), evaluatorContext, context.getEventAdapterService(), context.getStatementName(), context.getStatementId(), context.getAnnotations(), context.getContextDescriptor(), false, false, false, false, null, false);
                List<ExprNode> validated = validateExpressions(ExprNodeOrigin.PATTERNOBSERVER, observerNode.getPatternObserverSpec().getObjectParameters(), validationContext);

                MatchedEventConvertor convertor = new MatchedEventConvertorImpl(tags.getTaggedEventTypes(), tags.getArrayEventTypes(), allTagNamesOrdered, context.getEventAdapterService());

                observerNode.setObserverFactory(observerFactory);
                observerFactory.setObserverParameters(validated, convertor, validationContext);
            } catch (ObserverParameterException e) {
                throw new ExprValidationException("Invalid parameter for pattern observer '" + observerNode.toPrecedenceFreeEPL() + "': " + e.getMessage(), e);
            } catch (PatternObjectException e) {
                throw new ExprValidationException("Failed to resolve pattern observer '" + observerNode.toPrecedenceFreeEPL() + "': " + e.getMessage(), e);
            }
        } else if (evalNode instanceof EvalGuardFactoryNode) {
            EvalGuardFactoryNode guardNode = (EvalGuardFactoryNode) evalNode;
            try {
                GuardFactory guardFactory = context.getPatternResolutionService().create(guardNode.getPatternGuardSpec());

                StreamTypeService streamTypeService = getStreamTypeService(context.getEngineURI(), context.getStatementId(), context.getEventAdapterService(), tags.getTaggedEventTypes(), tags.getArrayEventTypes(), subexpressionIdStack, "guard", context);
                ExprValidationContext validationContext = new ExprValidationContext(streamTypeService, context.getEngineImportService(), context.getStatementExtensionServicesContext(), null, context.getSchedulingService(), context.getVariableService(), context.getTableService(), evaluatorContext, context.getEventAdapterService(), context.getStatementName(), context.getStatementId(), context.getAnnotations(), context.getContextDescriptor(), false, false, false, false, null, false);
                List<ExprNode> validated = validateExpressions(ExprNodeOrigin.PATTERNGUARD, guardNode.getPatternGuardSpec().getObjectParameters(), validationContext);

                MatchedEventConvertor convertor = new MatchedEventConvertorImpl(tags.getTaggedEventTypes(), tags.getArrayEventTypes(), allTagNamesOrdered, context.getEventAdapterService());

                guardNode.setGuardFactory(guardFactory);
                guardFactory.setGuardParameters(validated, convertor);
            } catch (GuardParameterException e) {
                throw new ExprValidationException("Invalid parameter for pattern guard '" + guardNode.toPrecedenceFreeEPL() + "': " + e.getMessage(), e);
            } catch (PatternObjectException e) {
                throw new ExprValidationException("Failed to resolve pattern guard '" + guardNode.toPrecedenceFreeEPL() + "': " + e.getMessage(), e);
            }
        } else if (evalNode instanceof EvalEveryDistinctFactoryNode) {
            EvalEveryDistinctFactoryNode distinctNode = (EvalEveryDistinctFactoryNode) evalNode;
            MatchEventSpec matchEventFromChildNodes = analyzeMatchEvent(distinctNode);
            StreamTypeService streamTypeService = getStreamTypeService(context.getEngineURI(), context.getStatementId(), context.getEventAdapterService(), matchEventFromChildNodes.getTaggedEventTypes(), matchEventFromChildNodes.getArrayEventTypes(), subexpressionIdStack, "every-distinct", context);
            ExprValidationContext validationContext = new ExprValidationContext(streamTypeService, context.getEngineImportService(), context.getStatementExtensionServicesContext(), null, context.getSchedulingService(), context.getVariableService(), context.getTableService(), evaluatorContext, context.getEventAdapterService(), context.getStatementName(), context.getStatementId(), context.getAnnotations(), context.getContextDescriptor(), false, false, false, false, null, false);
            List<ExprNode> validated;
            try {
                validated = validateExpressions(ExprNodeOrigin.PATTERNEVERYDISTINCT, distinctNode.getExpressions(), validationContext);
            } catch (ExprValidationPropertyException ex) {
                throw new ExprValidationPropertyException(ex.getMessage() + ", every-distinct requires that all properties resolve from sub-expressions to the every-distinct", ex.getCause());
            }

            MatchedEventConvertor convertor = new MatchedEventConvertorImpl(matchEventFromChildNodes.getTaggedEventTypes(), matchEventFromChildNodes.getArrayEventTypes(), allTagNamesOrdered, context.getEventAdapterService());

            distinctNode.setConvertor(convertor);

            // Determine whether some expressions are constants or time period
            List<ExprNode> distinctExpressions = new ArrayList<ExprNode>();
            ExprTimePeriodEvalDeltaConst timeDeltaComputation = null;
            ExprNode expiryTimeExp = null;
            int count = -1;
            int last = validated.size() - 1;
            for (ExprNode expr : validated) {
                count++;
                if (count == last && expr instanceof ExprTimePeriod) {
                    expiryTimeExp = expr;
                    ExprTimePeriod timePeriodExpr = (ExprTimePeriod) expiryTimeExp;
                    timeDeltaComputation = timePeriodExpr.constEvaluator(new ExprEvaluatorContextStatement(context, false));
                } else if (expr.isConstantResult()) {
                    if (count == last) {
                        Object value = expr.getForge().getExprEvaluator().evaluate(null, true, evaluatorContext);
                        if (!(value instanceof Number)) {
                            throw new ExprValidationException("Invalid parameter for every-distinct, expected number of seconds constant (constant not considered for distinct)");
                        }
                        Number secondsExpire = (Number) expr.getForge().getExprEvaluator().evaluate(null, true, evaluatorContext);
                        Long timeExpire = secondsExpire == null ? null : context.getTimeAbacus().deltaForSecondsNumber(secondsExpire);
                        if (timeExpire != null && timeExpire > 0) {
                            timeDeltaComputation = new ExprTimePeriodEvalDeltaConstGivenDelta(timeExpire);
                            expiryTimeExp = expr;
                        } else {
                            log.warn("Invalid seconds-expire " + timeExpire + " for " + ExprNodeUtilityCore.toExpressionStringMinPrecedenceSafe(expr));
                        }
                    } else {
                        log.warn("Every-distinct node utilizes an expression returning a constant value, please check expression '" + ExprNodeUtilityCore.toExpressionStringMinPrecedenceSafe(expr) + "', not adding expression to distinct-value expression list");
                    }
                } else {
                    distinctExpressions.add(expr);
                }
            }
            if (distinctExpressions.isEmpty()) {
                throw new ExprValidationException("Every-distinct node requires one or more distinct-value expressions that each return non-constant result values");
            }
            distinctNode.setDistinctExpressions(distinctExpressions, timeDeltaComputation, expiryTimeExp, context.getEngineImportService(), context.getStatementName());
        } else if (evalNode instanceof EvalMatchUntilFactoryNode) {
            EvalMatchUntilFactoryNode matchUntilNode = (EvalMatchUntilFactoryNode) evalNode;

            // compile bounds expressions, if any
            MatchEventSpec untilMatchEventSpec = new MatchEventSpec(tags.getTaggedEventTypes(), tags.getArrayEventTypes());
            StreamTypeService streamTypeService = getStreamTypeService(context.getEngineURI(), context.getStatementId(), context.getEventAdapterService(), untilMatchEventSpec.getTaggedEventTypes(), untilMatchEventSpec.getArrayEventTypes(), subexpressionIdStack, "until", context);
            ExprValidationContext validationContext = new ExprValidationContext(streamTypeService, context.getEngineImportService(), context.getStatementExtensionServicesContext(), null, context.getSchedulingService(), context.getVariableService(), context.getTableService(), evaluatorContext, context.getEventAdapterService(), context.getStatementName(), context.getStatementId(), context.getAnnotations(), context.getContextDescriptor(), false, false, false, false, null, false);

            ExprNode lower = validateBounds(matchUntilNode.getLowerBounds(), validationContext);
            matchUntilNode.setLowerBounds(lower);

            ExprNode upper = validateBounds(matchUntilNode.getUpperBounds(), validationContext);
            matchUntilNode.setUpperBounds(upper);

            ExprNode single = validateBounds(matchUntilNode.getSingleBound(), validationContext);
            matchUntilNode.setSingleBound(single);

            MatchedEventConvertor convertor = new MatchedEventConvertorImpl(untilMatchEventSpec.getTaggedEventTypes(), untilMatchEventSpec.getArrayEventTypes(), allTagNamesOrdered, context.getEventAdapterService());
            matchUntilNode.setConvertor(convertor);

            // compile new tag lists
            Set<String> arrayTags = null;
            EvalNodeAnalysisResult matchUntilAnalysisResult = EvalNodeUtil.recursiveAnalyzeChildNodes(matchUntilNode.getChildNodes().get(0));
            for (EvalFilterFactoryNode filterNode : matchUntilAnalysisResult.getFilterNodes()) {
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
        } else if (evalNode instanceof EvalFollowedByFactoryNode) {
            EvalFollowedByFactoryNode followedByNode = (EvalFollowedByFactoryNode) evalNode;
            StreamTypeService streamTypeService = new StreamTypeServiceImpl(context.getEngineURI(), false);
            ExprValidationContext validationContext = new ExprValidationContext(streamTypeService, context.getEngineImportService(), context.getStatementExtensionServicesContext(), null, context.getSchedulingService(), context.getVariableService(), context.getTableService(), evaluatorContext, context.getEventAdapterService(), context.getStatementName(), context.getStatementId(), context.getAnnotations(), context.getContextDescriptor(), false, false, false, false, null, false);

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

                        ExprNode validatedExpr = ExprNodeUtilityRich.getValidatedSubtree(ExprNodeOrigin.FOLLOWEDBYMAX, maxExpr, validationContext);
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
            ExprNode validated = ExprNodeUtilityRich.getValidatedSubtree(ExprNodeOrigin.PATTERNMATCHUNTILBOUNDS, bounds, validationContext);
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


    private static boolean isParentMatchUntil(EvalFactoryNode currentNode, Stack<EvalFactoryNode> parentNodeStack) {
        if (parentNodeStack.isEmpty()) {
            return false;
        }

        for (EvalFactoryNode deepParent : parentNodeStack) {
            if (deepParent instanceof EvalMatchUntilFactoryNode) {
                EvalMatchUntilFactoryNode matchUntilFactoryNode = (EvalMatchUntilFactoryNode) deepParent;
                if (matchUntilFactoryNode.getChildNodes().get(0) == currentNode) {
                    return true;
                }
            }
        }
        return false;
    }

    private static List<ExprNode> validateExpressions(ExprNodeOrigin exprNodeOrigin, List<ExprNode> objectParameters, ExprValidationContext validationContext)
            throws ExprValidationException {
        if (objectParameters == null) {
            return objectParameters;
        }
        List<ExprNode> validated = new ArrayList<ExprNode>();
        for (ExprNode node : objectParameters) {
            validated.add(ExprNodeUtilityRich.getValidatedSubtree(exprNodeOrigin, node, validationContext));
        }
        return validated;
    }

    private static StreamTypeService getStreamTypeService(String engineURI, int statementId, EventAdapterService eventAdapterService, Map<String, Pair<EventType, String>> taggedEventTypes, Map<String, Pair<EventType, String>> arrayEventTypes, Deque<Integer> subexpressionIdStack, String objectType, StatementContext statementContext) {
        LinkedHashMap<String, Pair<EventType, String>> filterTypes = new LinkedHashMap<String, Pair<EventType, String>>();
        filterTypes.putAll(taggedEventTypes);

        // handle array tags (match-until clause)
        if (arrayEventTypes != null) {
            String patternSubexEventType = getPatternSubexEventType(statementId, objectType, subexpressionIdStack);
            EventType arrayTagCompositeEventType = eventAdapterService.createSemiAnonymousMapType(patternSubexEventType, new HashMap(), arrayEventTypes, false);
            statementContext.getStatementSemiAnonymousTypeRegistry().register(arrayTagCompositeEventType);
            for (Map.Entry<String, Pair<EventType, String>> entry : arrayEventTypes.entrySet()) {
                String tag = entry.getKey();
                if (!filterTypes.containsKey(tag)) {
                    Pair<EventType, String> pair = new Pair<EventType, String>(arrayTagCompositeEventType, tag);
                    filterTypes.put(tag, pair);
                }
            }
        }

        return new StreamTypeServiceImpl(filterTypes, engineURI, true, false);
    }

    private static String getPatternSubexEventType(int statementId, String objectType, Deque<Integer> subexpressionIdStack) {
        StringWriter writer = new StringWriter();
        writer.append(Integer.toString(statementId));
        writer.append("_");
        writer.append(objectType);
        for (Integer num : subexpressionIdStack) {
            writer.append("_");
            writer.append(Integer.toString(num));
        }
        return writer.toString();
    }

    private static EvalFactoryNode recursiveAddAuditNode(PatternNodeFactory patternNodeFactory, EvalFactoryNode parentNode, boolean auditPattern, boolean auditPatternInstance, EvalFactoryNode evalNode, EvalAuditInstanceCount instanceCount) {
        StringWriter writer = new StringWriter();
        evalNode.toEPL(writer, PatternExpressionPrecedenceEnum.MINIMUM);
        String expressionText = writer.toString();
        boolean filterChildNonQuitting = parentNode != null && parentNode.isFilterChildNonQuitting();
        EvalFactoryNode audit = patternNodeFactory.makeAuditNode(auditPattern, auditPatternInstance, expressionText, instanceCount, filterChildNonQuitting);
        audit.addChildNode(evalNode);

        List<EvalFactoryNode> newChildNodes = new ArrayList<EvalFactoryNode>();
        for (EvalFactoryNode child : evalNode.getChildNodes()) {
            newChildNodes.add(recursiveAddAuditNode(patternNodeFactory, evalNode, auditPattern, auditPatternInstance, child, instanceCount));
        }

        evalNode.getChildNodes().clear();
        evalNode.addChildNodes(newChildNodes);

        return audit;
    }

    private static MatchEventSpec analyzeMatchEvent(EvalFactoryNode relativeNode) {
        LinkedHashMap<String, Pair<EventType, String>> taggedEventTypes = new LinkedHashMap<String, Pair<EventType, String>>();
        LinkedHashMap<String, Pair<EventType, String>> arrayEventTypes = new LinkedHashMap<String, Pair<EventType, String>>();

        // Determine all the filter nodes used in the pattern
        EvalNodeAnalysisResult evalNodeAnalysisResult = EvalNodeUtil.recursiveAnalyzeChildNodes(relativeNode);

        // collect all filters underneath
        for (EvalFilterFactoryNode filterNode : evalNodeAnalysisResult.getFilterNodes()) {
            String optionalTag = filterNode.getEventAsName();
            if (optionalTag != null) {
                taggedEventTypes.put(optionalTag, new Pair<EventType, String>(filterNode.getFilterSpec().getFilterForEventType(), filterNode.getFilterSpec().getFilterForEventTypeName()));
            }
        }

        // collect those filters under a repeat since they are arrays
        Set<String> arrayTags = new HashSet<String>();
        for (EvalMatchUntilFactoryNode matchUntilNode : evalNodeAnalysisResult.getRepeatNodes()) {
            EvalNodeAnalysisResult matchUntilAnalysisResult = EvalNodeUtil.recursiveAnalyzeChildNodes(matchUntilNode.getChildNodes().get(0));
            for (EvalFilterFactoryNode filterNode : matchUntilAnalysisResult.getFilterNodes()) {
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

    public static StreamSpecCompiled compile(MethodStreamSpec methodStreamSpec) throws ExprValidationException {
        if (!methodStreamSpec.getIdent().equals("method")) {
            throw new ExprValidationException("Expecting keyword 'method', found '" + methodStreamSpec.getIdent() + "'");
        }
        if (methodStreamSpec.getMethodName() == null) {
            throw new ExprValidationException("No method name specified for method-based join");
        }
        return methodStreamSpec;
    }

    public static class FilterForFilterFactoryNodes implements EvalNodeUtilFactoryFilter {
        public final static FilterForFilterFactoryNodes INSTANCE = new FilterForFilterFactoryNodes();

        public boolean consider(EvalFactoryNode node) {
            return node instanceof EvalFilterFactoryNode;
        }
    }
}
