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

import com.espertech.esper.client.*;
import com.espertech.esper.client.util.EventUnderlyingType;
import com.espertech.esper.collection.Pair;
import com.espertech.esper.epl.agg.service.AggregationGroupByRollupLevel;
import com.espertech.esper.epl.core.eval.*;
import com.espertech.esper.epl.expression.core.*;
import com.espertech.esper.epl.named.NamedWindowMgmtService;
import com.espertech.esper.epl.named.NamedWindowProcessor;
import com.espertech.esper.epl.rettype.EPType;
import com.espertech.esper.epl.rettype.EPTypeHelper;
import com.espertech.esper.epl.rettype.EventEPType;
import com.espertech.esper.epl.rettype.EventMultiValuedEPType;
import com.espertech.esper.epl.spec.CreateSchemaDesc;
import com.espertech.esper.epl.spec.InsertIntoDesc;
import com.espertech.esper.epl.spec.SelectClauseExprCompiledSpec;
import com.espertech.esper.epl.spec.SelectClauseStreamCompiledSpec;
import com.espertech.esper.epl.table.mgmt.TableMetadata;
import com.espertech.esper.epl.table.mgmt.TableService;
import com.espertech.esper.event.*;
import com.espertech.esper.event.arr.ObjectArrayEventType;
import com.espertech.esper.event.avro.AvroConstantsNoDep;
import com.espertech.esper.event.avro.AvroSchemaEventType;
import com.espertech.esper.event.bean.BeanEventType;
import com.espertech.esper.event.map.MapEventType;
import com.espertech.esper.event.vaevent.ValueAddEventProcessor;
import com.espertech.esper.event.vaevent.ValueAddEventService;
import com.espertech.esper.event.vaevent.VariantEventType;
import com.espertech.esper.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Annotation;
import java.util.*;

/**
 * Processor for select-clause expressions that handles a list of selection items represented by
 * expression nodes. Computes results based on matching events.
 */
public class SelectExprProcessorHelper {
    private static final Logger log = LoggerFactory.getLogger(SelectExprProcessorHelper.class);

    private final Collection<Integer> assignedTypeNumberStack;
    private final List<SelectClauseExprCompiledSpec> selectionList;
    private final List<SelectExprStreamDesc> selectedStreams;
    private final InsertIntoDesc insertIntoDesc;
    private EventType optionalInsertIntoOverrideType;
    private final boolean isUsingWildcard;
    private final StreamTypeService typeService;
    private final EventAdapterService eventAdapterService;
    private final ValueAddEventService valueAddEventService;
    private final SelectExprEventTypeRegistry selectExprEventTypeRegistry;
    private final EngineImportService engineImportService;
    private final int statementId;
    private final String statementName;
    private final Annotation[] annotations;
    private final ConfigurationInformation configuration;
    private final NamedWindowMgmtService namedWindowMgmtService;
    private final TableService tableService;
    private final GroupByRollupInfo groupByRollupInfo;

    public SelectExprProcessorHelper(Collection<Integer> assignedTypeNumberStack,
                                     List<SelectClauseExprCompiledSpec> selectionList,
                                     List<SelectExprStreamDesc> selectedStreams,
                                     InsertIntoDesc insertIntoDesc,
                                     EventType optionalInsertIntoOverrideType,
                                     boolean isUsingWildcard,
                                     StreamTypeService typeService,
                                     EventAdapterService eventAdapterService,
                                     ValueAddEventService valueAddEventService,
                                     SelectExprEventTypeRegistry selectExprEventTypeRegistry,
                                     EngineImportService engineImportService,
                                     int statementId,
                                     String statementName,
                                     Annotation[] annotations,
                                     ConfigurationInformation configuration,
                                     NamedWindowMgmtService namedWindowMgmtService,
                                     TableService tableService,
                                     GroupByRollupInfo groupByRollupInfo) throws ExprValidationException {
        this.assignedTypeNumberStack = assignedTypeNumberStack;
        this.selectionList = selectionList;
        this.selectedStreams = selectedStreams;
        this.insertIntoDesc = insertIntoDesc;
        this.optionalInsertIntoOverrideType = optionalInsertIntoOverrideType;
        this.eventAdapterService = eventAdapterService;
        this.isUsingWildcard = isUsingWildcard;
        this.typeService = typeService;
        this.valueAddEventService = valueAddEventService;
        this.selectExprEventTypeRegistry = selectExprEventTypeRegistry;
        this.engineImportService = engineImportService;
        this.statementId = statementId;
        this.statementName = statementName;
        this.annotations = annotations;
        this.configuration = configuration;
        this.namedWindowMgmtService = namedWindowMgmtService;
        this.tableService = tableService;
        this.groupByRollupInfo = groupByRollupInfo;
    }

    public SelectExprProcessor getEvaluator() throws ExprValidationException {

        // Get the named and un-named stream selectors (i.e. select s0.* from S0 as s0), if any
        List<SelectClauseStreamCompiledSpec> namedStreams = new ArrayList<SelectClauseStreamCompiledSpec>();
        List<SelectExprStreamDesc> unnamedStreams = new ArrayList<SelectExprStreamDesc>();
        for (SelectExprStreamDesc spec : selectedStreams) {
            // handle special "transpose(...)" function
            if ((spec.getStreamSelected() != null && spec.getStreamSelected().getOptionalName() == null)
                    ||
                    (spec.getExpressionSelectedAsStream() != null)) {
                unnamedStreams.add(spec);
            } else {
                namedStreams.add(spec.getStreamSelected());
                if (spec.getStreamSelected().isProperty()) {
                    throw new ExprValidationException("The property wildcard syntax must be used without column name");
                }
            }
        }

        // Error if there are more then one un-named streams (i.e. select s0.*, s1.* from S0 as s0, S1 as s1)
        // Thus there is only 1 unnamed stream selector maximum.
        if (unnamedStreams.size() > 1) {
            throw new ExprValidationException("A column name must be supplied for all but one stream if multiple streams are selected via the stream.* notation");
        }

        if (selectedStreams.isEmpty() && selectionList.isEmpty() && !isUsingWildcard) {
            throw new IllegalArgumentException("Empty selection list not supported");
        }

        for (SelectClauseExprCompiledSpec entry : selectionList) {
            if (entry.getAssignedName() == null) {
                throw new IllegalArgumentException("Expected name for each expression has not been supplied");
            }
        }

        // Verify insert into clause
        if (insertIntoDesc != null) {
            verifyInsertInto(insertIntoDesc, selectionList);
        }

        // Build a subordinate wildcard processor for joins
        SelectExprProcessor joinWildcardProcessor = null;
        if (typeService.getStreamNames().length > 1 && isUsingWildcard) {
            joinWildcardProcessor = SelectExprJoinWildcardProcessorFactory.create(assignedTypeNumberStack, statementId, statementName, typeService.getStreamNames(), typeService.getEventTypes(), eventAdapterService, null, selectExprEventTypeRegistry, engineImportService, annotations, configuration, tableService, typeService.getEngineURIQualifier());
        }

        // Resolve underlying event type in the case of wildcard select
        EventType eventType = null;
        boolean singleStreamWrapper = false;
        if (isUsingWildcard) {
            if (joinWildcardProcessor != null) {
                eventType = joinWildcardProcessor.getResultEventType();
            } else {
                eventType = typeService.getEventTypes()[0];
                if (eventType instanceof WrapperEventType) {
                    singleStreamWrapper = true;
                }
            }
        }

        // Find if there is any fragments selected
        EventType insertIntoTargetType = null;
        if (insertIntoDesc != null) {
            if (optionalInsertIntoOverrideType != null) {
                insertIntoTargetType = optionalInsertIntoOverrideType;
            } else {
                insertIntoTargetType = eventAdapterService.getExistsTypeByName(insertIntoDesc.getEventTypeName());
                TableMetadata tableMetadata = tableService.getTableMetadata(insertIntoDesc.getEventTypeName());
                if (tableMetadata != null) {
                    insertIntoTargetType = tableMetadata.getInternalEventType();
                    optionalInsertIntoOverrideType = insertIntoTargetType;
                }
            }
        }

        // Obtain insert-into per-column type information, when available
        EPType[] insertIntoTargetsPerCol = determineInsertedEventTypeTargets(insertIntoTargetType, selectionList);

        // Get expression nodes
        ExprEvaluator[] exprEvaluators = new ExprEvaluator[selectionList.size()];
        ExprNode[] exprNodes = new ExprNode[selectionList.size()];
        Object[] expressionReturnTypes = new Object[selectionList.size()];
        for (int i = 0; i < selectionList.size(); i++) {
            SelectClauseExprCompiledSpec spec = selectionList.get(i);
            ExprNode expr = spec.getSelectExpression();
            ExprEvaluator evaluator = expr.getExprEvaluator();
            exprNodes[i] = expr;

            // if there is insert-into specification, use that
            if (insertIntoDesc != null) {
                // handle insert-into, with well-defined target event-typed column, and enumeration
                TypeAndFunctionPair pair = handleInsertIntoEnumeration(spec.getProvidedName(), insertIntoTargetsPerCol[i], evaluator, engineImportService);
                if (pair != null) {
                    expressionReturnTypes[i] = pair.getType();
                    exprEvaluators[i] = pair.getFunction();
                    continue;
                }

                // handle insert-into with well-defined target event-typed column, and typable expression
                pair = handleInsertIntoTypableExpression(insertIntoTargetsPerCol[i], evaluator, engineImportService);
                if (pair != null) {
                    expressionReturnTypes[i] = pair.getType();
                    exprEvaluators[i] = pair.getFunction();
                    continue;
                }
            }

            // handle @eventbean annotation, i.e. well-defined type through enumeration
            TypeAndFunctionPair pair = handleAtEventbeanEnumeration(spec.isEvents(), evaluator);
            if (pair != null) {
                expressionReturnTypes[i] = pair.getType();
                exprEvaluators[i] = pair.getFunction();
                continue;
            }

            // handle typeable return, i.e. typable multi-column return without provided target type
            pair = handleTypableExpression(evaluator, i);
            if (pair != null) {
                expressionReturnTypes[i] = pair.getType();
                exprEvaluators[i] = pair.getFunction();
                continue;
            }

            // handle select-clause expressions that match group-by expressions with rollup and therefore should be boxed types as rollup can produce a null value
            if (groupByRollupInfo != null && groupByRollupInfo.getRollupDesc() != null) {
                Class returnType = evaluator.getType();
                Class returnTypeBoxed = JavaClassHelper.getBoxedType(returnType);
                if (returnType != returnTypeBoxed && isGroupByRollupNullableExpression(expr, groupByRollupInfo)) {
                    exprEvaluators[i] = evaluator;
                    expressionReturnTypes[i] = returnTypeBoxed;
                    continue;
                }
            }

            // assign normal expected return type
            exprEvaluators[i] = evaluator;
            expressionReturnTypes[i] = exprEvaluators[i].getType();
        }

        // Get column names
        String[] columnNames;
        String[] columnNamesAsProvided;
        if ((insertIntoDesc != null) && (!insertIntoDesc.getColumnNames().isEmpty())) {
            columnNames = insertIntoDesc.getColumnNames().toArray(new String[insertIntoDesc.getColumnNames().size()]);
            columnNamesAsProvided = columnNames;
        } else if (!selectedStreams.isEmpty()) { // handle stream selection column names
            int numStreamColumnsJoin = 0;
            if (isUsingWildcard && typeService.getEventTypes().length > 1) {
                numStreamColumnsJoin = typeService.getEventTypes().length;
            }
            columnNames = new String[selectionList.size() + namedStreams.size() + numStreamColumnsJoin];
            columnNamesAsProvided = new String[columnNames.length];
            int count = 0;
            for (SelectClauseExprCompiledSpec aSelectionList : selectionList) {
                columnNames[count] = aSelectionList.getAssignedName();
                columnNamesAsProvided[count] = aSelectionList.getProvidedName();
                count++;
            }
            for (SelectClauseStreamCompiledSpec aSelectionList : namedStreams) {
                columnNames[count] = aSelectionList.getOptionalName();
                columnNamesAsProvided[count] = aSelectionList.getOptionalName();
                count++;
            }
            // for wildcard joins, add the streams themselves
            if (isUsingWildcard && typeService.getEventTypes().length > 1) {
                for (String streamName : typeService.getStreamNames()) {
                    columnNames[count] = streamName;
                    columnNamesAsProvided[count] = streamName;
                    count++;
                }
            }
        } else {
            // handle regular column names
            columnNames = new String[selectionList.size()];
            columnNamesAsProvided = new String[selectionList.size()];
            for (int i = 0; i < selectionList.size(); i++) {
                columnNames[i] = selectionList.get(i).getAssignedName();
                columnNamesAsProvided[i] = selectionList.get(i).getProvidedName();
            }
        }

        // Find if there is any fragment event types:
        // This is a special case for fragments: select a, b from pattern [a=A -> b=B]
        // We'd like to maintain 'A' and 'B' EventType in the Map type, and 'a' and 'b' EventBeans in the event bean
        for (int i = 0; i < selectionList.size(); i++) {
            if (!(exprNodes[i] instanceof ExprIdentNode)) {
                continue;
            }

            ExprIdentNode identNode = (ExprIdentNode) exprNodes[i];
            String propertyName = identNode.getResolvedPropertyName();
            final int streamNum = identNode.getStreamId();

            EventType eventTypeStream = typeService.getEventTypes()[streamNum];
            if (eventTypeStream instanceof NativeEventType) {
                continue;   // we do not transpose the native type for performance reasons
            }

            FragmentEventType fragmentType = eventTypeStream.getFragmentType(propertyName);
            if ((fragmentType == null) || (fragmentType.isNative())) {
                continue;   // we also ignore native Java classes as fragments for performance reasons
            }

            // may need to unwrap the fragment if the target type has this underlying type
            FragmentEventType targetFragment = null;
            if (insertIntoTargetType != null) {
                targetFragment = insertIntoTargetType.getFragmentType(columnNames[i]);
            }
            if ((insertIntoTargetType != null) &&
                    (fragmentType.getFragmentType().getUnderlyingType() == expressionReturnTypes[i]) &&
                    ((targetFragment == null) || (targetFragment != null && targetFragment.isNative()))) {
                EventPropertyGetter getter = eventTypeStream.getGetter(propertyName);
                Class returnType = eventTypeStream.getPropertyType(propertyName);
                exprEvaluators[i] = new SelectExprProcessorEvalByGetter(streamNum, getter, returnType);
            } else if ((insertIntoTargetType != null) && expressionReturnTypes[i] instanceof Class &&
                    (fragmentType.getFragmentType().getUnderlyingType() == ((Class) expressionReturnTypes[i]).getComponentType()) &&
                    ((targetFragment == null) || (targetFragment != null && targetFragment.isNative()))) {
                // same for arrays: may need to unwrap the fragment if the target type has this underlying type
                EventPropertyGetter getter = eventTypeStream.getGetter(propertyName);
                Class returnType = JavaClassHelper.getArrayType(eventTypeStream.getPropertyType(propertyName));
                exprEvaluators[i] = new SelectExprProcessorEvalByGetter(streamNum, getter, returnType);
            } else {
                EventPropertyGetter getter = eventTypeStream.getGetter(propertyName);
                FragmentEventType fragType = eventTypeStream.getFragmentType(propertyName);
                Class undType = fragType.getFragmentType().getUnderlyingType();
                Class returnType = fragType.isIndexed() ? JavaClassHelper.getArrayType(undType) : undType;
                exprEvaluators[i] = new SelectExprProcessorEvalByGetterFragment(streamNum, getter, returnType);
                if (!fragmentType.isIndexed()) {
                    expressionReturnTypes[i] = fragmentType.getFragmentType();
                } else {
                    expressionReturnTypes[i] = new EventType[]{fragmentType.getFragmentType()};
                }
            }
        }

        // Find if there is any stream expression (ExprStreamNode) :
        // This is a special case for stream selection: select a, b from A as a, B as b
        // We'd like to maintain 'A' and 'B' EventType in the Map type, and 'a' and 'b' EventBeans in the event bean
        for (int i = 0; i < selectionList.size(); i++) {
            Pair<ExprEvaluator, Object> pair = handleUnderlyingStreamInsert(exprEvaluators[i], namedWindowMgmtService, eventAdapterService);
            if (pair != null) {
                exprEvaluators[i] = pair.getFirst();
                expressionReturnTypes[i] = pair.getSecond();
            }
        }

        // Build event type that reflects all selected properties
        Map<String, Object> selPropertyTypes = new LinkedHashMap<String, Object>();
        int count = 0;
        for (int i = 0; i < exprEvaluators.length; i++) {
            Object expressionReturnType = expressionReturnTypes[count];
            selPropertyTypes.put(columnNames[count], expressionReturnType);
            count++;
        }
        if (!selectedStreams.isEmpty()) {
            for (SelectClauseStreamCompiledSpec element : namedStreams) {
                EventType eventTypeStream;
                if (element.getTableMetadata() != null) {
                    eventTypeStream = element.getTableMetadata().getPublicEventType();
                } else {
                    eventTypeStream = typeService.getEventTypes()[element.getStreamNumber()];
                }
                selPropertyTypes.put(columnNames[count], eventTypeStream);
                count++;
            }
            if (isUsingWildcard && typeService.getEventTypes().length > 1) {
                for (int i = 0; i < typeService.getEventTypes().length; i++) {
                    EventType eventTypeStream = typeService.getEventTypes()[i];
                    selPropertyTypes.put(columnNames[count], eventTypeStream);
                    count++;
                }
            }
        }

        // Handle stream selection
        EventType underlyingEventType = null;
        int underlyingStreamNumber = 0;
        boolean underlyingIsFragmentEvent = false;
        EventPropertyGetter underlyingPropertyEventGetter = null;
        ExprEvaluator underlyingExprEvaluator = null;
        EventUnderlyingType representation = EventRepresentationUtil.getRepresentation(annotations, configuration, CreateSchemaDesc.AssignedType.NONE);

        if (!selectedStreams.isEmpty()) {
            // Resolve underlying event type in the case of wildcard or non-named stream select.
            // Determine if the we are considering a tagged event or a stream name.
            if (isUsingWildcard || (!unnamedStreams.isEmpty())) {
                if (!unnamedStreams.isEmpty()) {
                    if (unnamedStreams.get(0).getStreamSelected() != null) {
                        SelectClauseStreamCompiledSpec streamSpec = unnamedStreams.get(0).getStreamSelected();

                        // the tag.* syntax for :  select tag.* from pattern [tag = A]
                        underlyingStreamNumber = streamSpec.getStreamNumber();
                        if (streamSpec.isFragmentEvent()) {
                            EventType compositeMap = typeService.getEventTypes()[underlyingStreamNumber];
                            FragmentEventType fragment = compositeMap.getFragmentType(streamSpec.getStreamName());
                            underlyingEventType = fragment.getFragmentType();
                            underlyingIsFragmentEvent = true;
                        } else if (streamSpec.isProperty()) {
                            // the property.* syntax for :  select property.* from A
                            String propertyName = streamSpec.getStreamName();
                            Class propertyType = streamSpec.getPropertyType();
                            int streamNumber = streamSpec.getStreamNumber();

                            if (JavaClassHelper.isJavaBuiltinDataType(streamSpec.getPropertyType())) {
                                throw new ExprValidationException("The property wildcard syntax cannot be used on built-in types as returned by property '" + propertyName + "'");
                            }

                            // create or get an underlying type for that Class
                            underlyingEventType = eventAdapterService.addBeanType(propertyType.getName(), propertyType, false, false, false);
                            selectExprEventTypeRegistry.add(underlyingEventType);
                            underlyingPropertyEventGetter = typeService.getEventTypes()[streamNumber].getGetter(propertyName);
                            if (underlyingPropertyEventGetter == null) {
                                throw new ExprValidationException("Unexpected error resolving property getter for property " + propertyName);
                            }
                        } else {
                            // the stream.* syntax for:  select a.* from A as a
                            underlyingEventType = typeService.getEventTypes()[underlyingStreamNumber];
                        }
                    } else {
                        // handle case where the unnamed stream is a "transpose" function, for non-insert-into
                        if (insertIntoDesc == null || insertIntoTargetType == null) {
                            ExprNode expression = unnamedStreams.get(0).getExpressionSelectedAsStream().getSelectExpression();
                            Class returnType = expression.getExprEvaluator().getType();
                            if (returnType == Object[].class || JavaClassHelper.isImplementsInterface(returnType, Map.class) || JavaClassHelper.isJavaBuiltinDataType(returnType)) {
                                throw new ExprValidationException("Invalid expression return type '" + returnType.getName() + "' for transpose function");
                            }
                            underlyingEventType = eventAdapterService.addBeanType(returnType.getName(), returnType, false, false, false);
                            selectExprEventTypeRegistry.add(underlyingEventType);
                            underlyingExprEvaluator = expression.getExprEvaluator();
                        }
                    }
                } else {
                    // no un-named stream selectors, but a wildcard was specified
                    if (typeService.getEventTypes().length == 1) {
                        // not a join, we are using the selected event
                        underlyingEventType = typeService.getEventTypes()[0];
                        if (underlyingEventType instanceof WrapperEventType) {
                            singleStreamWrapper = true;
                        }
                    } else {
                        // For joins, all results are placed in a map with properties for each stream
                        underlyingEventType = null;
                    }
                }
            }
        }

        SelectExprContext selectExprContext = new SelectExprContext(exprEvaluators, columnNames, eventAdapterService);

        if (insertIntoDesc == null) {
            if (!selectedStreams.isEmpty()) {
                EventType resultEventType;
                if (underlyingEventType != null) {
                    TableMetadata tableMetadata = tableService.getTableMetadataFromEventType(underlyingEventType);
                    if (tableMetadata != null) {
                        underlyingEventType = tableMetadata.getPublicEventType();
                    }
                    resultEventType = eventAdapterService.createAnonymousWrapperType(statementId + "_wrapout_" + CollectionUtil.toString(assignedTypeNumberStack, "_"), underlyingEventType, selPropertyTypes);
                    return new EvalSelectStreamWUnderlying(selectExprContext, resultEventType, namedStreams, isUsingWildcard,
                            unnamedStreams, singleStreamWrapper, underlyingIsFragmentEvent, underlyingStreamNumber, underlyingPropertyEventGetter, underlyingExprEvaluator, tableMetadata);
                } else {
                    resultEventType = eventAdapterService.createAnonymousMapType(statementId + "_mapout_" + CollectionUtil.toString(assignedTypeNumberStack, "_"), selPropertyTypes, true);
                    return new EvalSelectStreamNoUnderlyingMap(selectExprContext, resultEventType, namedStreams, isUsingWildcard);
                }
            }

            if (isUsingWildcard) {
                EventType resultEventType = eventAdapterService.createAnonymousWrapperType(statementId + "_wrapoutwild_" + CollectionUtil.toString(assignedTypeNumberStack, "_"), eventType, selPropertyTypes);
                if (singleStreamWrapper) {
                    return new EvalSelectWildcardSSWrapper(selectExprContext, resultEventType);
                }
                if (joinWildcardProcessor == null) {
                    return new EvalSelectWildcard(selectExprContext, resultEventType);
                }
                return new EvalSelectWildcardJoin(selectExprContext, resultEventType, joinWildcardProcessor);
            }

            EventType resultEventType;
            if (representation == EventUnderlyingType.OBJECTARRAY) {
                resultEventType = eventAdapterService.createAnonymousObjectArrayType(statementId + "_result_" + CollectionUtil.toString(assignedTypeNumberStack, "_"), selPropertyTypes);
            } else if (representation == EventUnderlyingType.AVRO) {
                resultEventType = eventAdapterService.createAnonymousAvroType(statementId + "_result_" + CollectionUtil.toString(assignedTypeNumberStack, "_"), selPropertyTypes, annotations, statementName, typeService.getEngineURIQualifier());
            } else {
                resultEventType = eventAdapterService.createAnonymousMapType(statementId + "_result_" + CollectionUtil.toString(assignedTypeNumberStack, "_"), selPropertyTypes, true);
            }

            if (selectExprContext.getExpressionNodes().length == 0) {
                return new EvalSelectNoWildcardEmptyProps(selectExprContext, resultEventType);
            } else {
                if (representation == EventUnderlyingType.OBJECTARRAY) {
                    return new EvalSelectNoWildcardObjectArray(selectExprContext, resultEventType);
                } else if (representation == EventUnderlyingType.AVRO) {
                    return eventAdapterService.getEventAdapterAvroHandler().getOutputFactory().makeSelectNoWildcard(selectExprContext, resultEventType, tableService, statementName, typeService.getEngineURIQualifier());
                }
                return new EvalSelectNoWildcardMap(selectExprContext, resultEventType);
            }
        }

        EventType vaeInnerEventType = null;
        boolean singleColumnWrapOrBeanCoercion = false;       // Additional single-column coercion for non-wrapped type done by SelectExprInsertEventBeanFactory
        boolean isRevisionEvent = false;

        try {
            if (!selectedStreams.isEmpty()) {
                EventType resultEventType;

                // handle "transpose" special function with predefined target type
                if (insertIntoTargetType != null && selectedStreams.get(0).getExpressionSelectedAsStream() != null) {
                    if (exprEvaluators.length != 0) {
                        throw new ExprValidationException("Cannot transpose additional properties in the select-clause to target event type '" +
                                insertIntoTargetType.getName() +
                                "' with underlying type '" + insertIntoTargetType.getUnderlyingType().getName() + "', the " + EngineImportService.EXT_SINGLEROW_FUNCTION_TRANSPOSE + " function must occur alone in the select clause");
                    }
                    ExprNode expression = unnamedStreams.get(0).getExpressionSelectedAsStream().getSelectExpression();
                    Class returnType = expression.getExprEvaluator().getType();
                    if (insertIntoTargetType instanceof ObjectArrayEventType && returnType == Object[].class) {
                        return new SelectExprInsertEventBeanFactory.SelectExprInsertNativeExpressionCoerceObjectArray(insertIntoTargetType, expression.getExprEvaluator(), eventAdapterService);
                    } else if (insertIntoTargetType instanceof MapEventType && JavaClassHelper.isImplementsInterface(returnType, Map.class)) {
                        return new SelectExprInsertEventBeanFactory.SelectExprInsertNativeExpressionCoerceMap(insertIntoTargetType, expression.getExprEvaluator(), eventAdapterService);
                    } else if (insertIntoTargetType instanceof BeanEventType && JavaClassHelper.isSubclassOrImplementsInterface(returnType, insertIntoTargetType.getUnderlyingType())) {
                        return new SelectExprInsertEventBeanFactory.SelectExprInsertNativeExpressionCoerceNative(insertIntoTargetType, expression.getExprEvaluator(), eventAdapterService);
                    } else if (insertIntoTargetType instanceof AvroSchemaEventType && returnType.getName().equals(AvroConstantsNoDep.GENERIC_RECORD_CLASSNAME)) {
                        return new SelectExprInsertEventBeanFactory.SelectExprInsertNativeExpressionCoerceAvro(insertIntoTargetType, expression.getExprEvaluator(), eventAdapterService);
                    } else if (insertIntoTargetType instanceof WrapperEventType) {
                        // for native event types as they got renamed, they become wrappers
                        // check if the proposed wrapper is compatible with the existing wrapper
                        WrapperEventType existing = (WrapperEventType) insertIntoTargetType;
                        if (existing.getUnderlyingEventType() instanceof BeanEventType) {
                            BeanEventType innerType = (BeanEventType) existing.getUnderlyingEventType();
                            ExprEvaluator evalExprEvaluator = unnamedStreams.get(0).getExpressionSelectedAsStream().getSelectExpression().getExprEvaluator();
                            if (!JavaClassHelper.isSubclassOrImplementsInterface(evalExprEvaluator.getType(), innerType.getUnderlyingType())) {
                                throw new ExprValidationException("Invalid expression return type '" + evalExprEvaluator.getType() + "' for transpose function, expected '" + innerType.getUnderlyingType().getSimpleName() + "'");
                            }
                            resultEventType = eventAdapterService.addWrapperType(insertIntoTargetType.getName(), existing.getUnderlyingEventType(), selPropertyTypes, false, true);
                            return new EvalSelectStreamWUnderlying(selectExprContext, resultEventType, namedStreams, isUsingWildcard,
                                    unnamedStreams, false, false, underlyingStreamNumber, null, evalExprEvaluator, null);
                        }
                    }
                    throw EvalInsertUtil.makeEventTypeCastException(returnType, insertIntoTargetType);
                }

                if (underlyingEventType != null) {
                    // a single stream was selected via "stream.*" and there is no column name
                    // recast as a Map-type
                    if (underlyingEventType instanceof MapEventType && insertIntoTargetType instanceof MapEventType) {
                        return EvalSelectStreamWUndRecastMapFactory.make(typeService.getEventTypes(), selectExprContext, selectedStreams.get(0).getStreamSelected().getStreamNumber(), insertIntoTargetType, exprNodes, engineImportService, statementName, typeService.getEngineURIQualifier());
                    }

                    // recast as a Object-array-type
                    if (underlyingEventType instanceof ObjectArrayEventType && insertIntoTargetType instanceof ObjectArrayEventType) {
                        return EvalSelectStreamWUndRecastObjectArrayFactory.make(typeService.getEventTypes(), selectExprContext, selectedStreams.get(0).getStreamSelected().getStreamNumber(), insertIntoTargetType, exprNodes, engineImportService, statementName, typeService.getEngineURIQualifier());
                    }

                    // recast as a Avro-type
                    if (underlyingEventType instanceof AvroSchemaEventType && insertIntoTargetType instanceof AvroSchemaEventType) {
                        return eventAdapterService.getEventAdapterAvroHandler().getOutputFactory().makeRecast(typeService.getEventTypes(), selectExprContext, selectedStreams.get(0).getStreamSelected().getStreamNumber(), (AvroSchemaEventType) insertIntoTargetType, exprNodes, statementName, typeService.getEngineURIQualifier());
                    }

                    // recast as a Bean-type
                    if (underlyingEventType instanceof BeanEventType && insertIntoTargetType instanceof BeanEventType) {
                        return new EvalInsertBeanRecast(insertIntoTargetType, eventAdapterService, selectedStreams.get(0).getStreamSelected().getStreamNumber(), typeService.getEventTypes());
                    }

                    // wrap if no recast possible
                    TableMetadata tableMetadata = tableService.getTableMetadataFromEventType(underlyingEventType);
                    if (tableMetadata != null) {
                        underlyingEventType = tableMetadata.getPublicEventType();
                    }
                    resultEventType = eventAdapterService.addWrapperType(insertIntoDesc.getEventTypeName(), underlyingEventType, selPropertyTypes, false, true);
                    return new EvalSelectStreamWUnderlying(selectExprContext, resultEventType, namedStreams, isUsingWildcard,
                            unnamedStreams, singleStreamWrapper, underlyingIsFragmentEvent, underlyingStreamNumber, underlyingPropertyEventGetter, underlyingExprEvaluator, tableMetadata);
                } else {
                    // there are one or more streams selected with column name such as "stream.* as columnOne"
                    if (insertIntoTargetType instanceof BeanEventType) {
                        String name = selectedStreams.get(0).getStreamSelected().getStreamName();
                        String alias = selectedStreams.get(0).getStreamSelected().getOptionalName();
                        String syntaxUsed = name + ".*" + (alias != null ? " as " + alias : "");
                        String syntaxInstead = name + (alias != null ? " as " + alias : "");
                        throw new ExprValidationException("The '" + syntaxUsed + "' syntax is not allowed when inserting into an existing bean event type, use the '" + syntaxInstead + "' syntax instead");
                    }
                    if (insertIntoTargetType == null || insertIntoTargetType instanceof MapEventType) {
                        resultEventType = eventAdapterService.addNestableMapType(insertIntoDesc.getEventTypeName(), selPropertyTypes, null, false, false, false, false, true);
                        Set<String> propertiesToUnwrap = getEventBeanToObjectProps(selPropertyTypes, resultEventType);
                        if (propertiesToUnwrap.isEmpty()) {
                            return new EvalSelectStreamNoUnderlyingMap(selectExprContext, resultEventType, namedStreams, isUsingWildcard);
                        } else {
                            return new EvalSelectStreamNoUndWEventBeanToObj(selectExprContext, resultEventType, namedStreams, isUsingWildcard, propertiesToUnwrap);
                        }
                    } else if (insertIntoTargetType instanceof ObjectArrayEventType) {
                        Set<String> propertiesToUnwrap = getEventBeanToObjectProps(selPropertyTypes, insertIntoTargetType);
                        if (propertiesToUnwrap.isEmpty()) {
                            return new EvalSelectStreamNoUnderlyingObjectArray(selectExprContext, insertIntoTargetType, namedStreams, isUsingWildcard);
                        } else {
                            return new EvalSelectStreamNoUndWEventBeanToObjObjArray(selectExprContext, insertIntoTargetType, namedStreams, isUsingWildcard, propertiesToUnwrap);
                        }
                    } else if (insertIntoTargetType instanceof AvroSchemaEventType) {
                        throw new ExprValidationException("Avro event type does not allow contained beans");
                    } else {
                        throw new IllegalStateException("Unrecognized event type " + insertIntoTargetType);
                    }
                }
            }

            ValueAddEventProcessor vaeProcessor = valueAddEventService.getValueAddProcessor(insertIntoDesc.getEventTypeName());
            EventType resultEventType;
            if (isUsingWildcard) {
                if (vaeProcessor != null) {
                    resultEventType = vaeProcessor.getValueAddEventType();
                    isRevisionEvent = true;
                    vaeProcessor.validateEventType(eventType);
                } else {
                    if (insertIntoTargetType != null) {

                        // handle insert-into with fast coercion (no additional properties selected)
                        if (selPropertyTypes.isEmpty()) {
                            if (insertIntoTargetType instanceof BeanEventType && eventType instanceof BeanEventType) {
                                return new EvalInsertBeanRecast(insertIntoTargetType, eventAdapterService, 0, typeService.getEventTypes());
                            }
                            if (insertIntoTargetType instanceof ObjectArrayEventType && eventType instanceof ObjectArrayEventType) {
                                ObjectArrayEventType target = (ObjectArrayEventType) insertIntoTargetType;
                                ObjectArrayEventType source = (ObjectArrayEventType) eventType;
                                String msg = BaseNestableEventType.isDeepEqualsProperties(eventType.getName(), source.getTypes(), target.getTypes());
                                if (msg == null) {
                                    return new EvalInsertCoercionObjectArray(insertIntoTargetType, eventAdapterService);
                                }
                            }
                            if (insertIntoTargetType instanceof MapEventType && eventType instanceof MapEventType) {
                                return new EvalInsertCoercionMap(insertIntoTargetType, eventAdapterService);
                            }
                            if (insertIntoTargetType instanceof AvroSchemaEventType && eventType instanceof AvroSchemaEventType) {
                                return new EvalInsertCoercionAvro(insertIntoTargetType, eventAdapterService);
                            }
                            if (insertIntoTargetType instanceof WrapperEventType && eventType instanceof BeanEventType) {
                                WrapperEventType wrapperType = (WrapperEventType) insertIntoTargetType;
                                if (wrapperType.getUnderlyingEventType() instanceof BeanEventType) {
                                    return new EvalInsertBeanWrapRecast(wrapperType, eventAdapterService, 0, typeService.getEventTypes());
                                }
                            }
                        }

                        // handle insert-into by generating the writer with possible additional properties
                        SelectExprProcessor existingTypeProcessor = SelectExprInsertEventBeanFactory.getInsertUnderlyingNonJoin(eventAdapterService, insertIntoTargetType, isUsingWildcard, typeService, exprEvaluators, columnNames, expressionReturnTypes, engineImportService, insertIntoDesc, columnNamesAsProvided, true, statementName);
                        if (existingTypeProcessor != null) {
                            return existingTypeProcessor;
                        }
                    }

                    if (selPropertyTypes.isEmpty() && eventType instanceof BeanEventType) {
                        BeanEventType beanEventType = (BeanEventType) eventType;
                        resultEventType = eventAdapterService.addBeanTypeByName(insertIntoDesc.getEventTypeName(), beanEventType.getUnderlyingType(), false);
                    } else {
                        resultEventType = eventAdapterService.addWrapperType(insertIntoDesc.getEventTypeName(), eventType, selPropertyTypes, false, true);
                    }
                }

                if (singleStreamWrapper) {
                    if (!isRevisionEvent) {
                        return new EvalInsertWildcardSSWrapper(selectExprContext, resultEventType);
                    } else {
                        return new EvalInsertWildcardSSWrapperRevision(selectExprContext, resultEventType, vaeProcessor);
                    }
                }
                if (joinWildcardProcessor == null) {
                    if (!isRevisionEvent) {
                        if (resultEventType instanceof WrapperEventType) {
                            return new EvalInsertWildcardWrapper(selectExprContext, resultEventType);
                        } else {
                            return new EvalInsertWildcardBean(selectExprContext, resultEventType);
                        }
                    } else {
                        if (exprEvaluators.length == 0) {
                            return new EvalInsertWildcardRevision(selectExprContext, resultEventType, vaeProcessor);
                        } else {
                            EventType wrappingEventType = eventAdapterService.addWrapperType(insertIntoDesc.getEventTypeName() + "_wrapped", eventType, selPropertyTypes, false, true);
                            return new EvalInsertWildcardRevisionWrapper(selectExprContext, resultEventType, vaeProcessor, wrappingEventType);
                        }
                    }
                } else {
                    if (!isRevisionEvent) {
                        return new EvalInsertWildcardJoin(selectExprContext, resultEventType, joinWildcardProcessor);
                    } else {
                        return new EvalInsertWildcardJoinRevision(selectExprContext, resultEventType, joinWildcardProcessor, vaeProcessor);
                    }
                }
            }

            // not using wildcard
            resultEventType = null;
            if ((columnNames.length == 1) && (insertIntoDesc.getColumnNames().size() == 0)) {
                if (insertIntoTargetType != null) {
                    // check if the existing type and new type are compatible
                    Object columnOneType = expressionReturnTypes[0];
                    if (insertIntoTargetType instanceof WrapperEventType) {
                        WrapperEventType wrapperType = (WrapperEventType) insertIntoTargetType;
                        // Map and Object both supported
                        if (wrapperType.getUnderlyingEventType().getUnderlyingType() == columnOneType) {
                            singleColumnWrapOrBeanCoercion = true;
                            resultEventType = insertIntoTargetType;
                        }
                    }
                    if ((insertIntoTargetType instanceof BeanEventType) && (columnOneType instanceof Class)) {
                        BeanEventType beanType = (BeanEventType) insertIntoTargetType;
                        // Map and Object both supported
                        if (JavaClassHelper.isSubclassOrImplementsInterface((Class) columnOneType, beanType.getUnderlyingType())) {
                            singleColumnWrapOrBeanCoercion = true;
                            resultEventType = insertIntoTargetType;
                        }
                    }
                }
            }
            if (singleColumnWrapOrBeanCoercion) {
                if (!isRevisionEvent) {
                    if (resultEventType instanceof WrapperEventType) {
                        WrapperEventType wrapper = (WrapperEventType) resultEventType;
                        if (wrapper.getUnderlyingEventType() instanceof MapEventType) {
                            return new EvalInsertNoWildcardSingleColCoercionMapWrap(selectExprContext, wrapper);
                        } else if (wrapper.getUnderlyingEventType() instanceof ObjectArrayEventType) {
                            return new EvalInsertNoWildcardSingleColCoercionObjectArrayWrap(selectExprContext, wrapper);
                        } else if (wrapper.getUnderlyingEventType() instanceof AvroSchemaEventType) {
                            return new EvalInsertNoWildcardSingleColCoercionAvroWrap(selectExprContext, wrapper);
                        } else if (wrapper.getUnderlyingEventType() instanceof VariantEventType) {
                            VariantEventType variantEventType = (VariantEventType) wrapper.getUnderlyingEventType();
                            vaeProcessor = valueAddEventService.getValueAddProcessor(variantEventType.getName());
                            return new EvalInsertNoWildcardSingleColCoercionBeanWrapVariant(selectExprContext, wrapper, vaeProcessor);
                        } else {
                            return new EvalInsertNoWildcardSingleColCoercionBeanWrap(selectExprContext, wrapper);
                        }
                    } else {
                        if (resultEventType instanceof BeanEventType) {
                            return new EvalInsertNoWildcardSingleColCoercionBean(selectExprContext, resultEventType);
                        }
                    }
                } else {
                    if (resultEventType instanceof BeanEventType) {
                        return new EvalInsertNoWildcardSingleColCoercionRevisionBean(selectExprContext, resultEventType, vaeProcessor, vaeInnerEventType);
                    } else {
                        TriFunction<EventAdapterService, Object, EventType, EventBean> func;
                        if (resultEventType instanceof MapEventType) {
                            func = new TriFunction<EventAdapterService, Object, EventType, EventBean>() {
                                public EventBean apply(EventAdapterService eventAdapterService, Object und, EventType type) {
                                    return eventAdapterService.adapterForTypedMap((Map) und, type);
                                }
                            };
                        } else if (resultEventType instanceof ObjectArrayEventType) {
                            func = new TriFunction<EventAdapterService, Object, EventType, EventBean>() {
                                public EventBean apply(EventAdapterService eventAdapterService, Object und, EventType type) {
                                    return eventAdapterService.adapterForTypedObjectArray((Object[]) und, type);
                                }
                            };
                        } else if (resultEventType instanceof AvroSchemaEventType) {
                            func = new TriFunction<EventAdapterService, Object, EventType, EventBean>() {
                                public EventBean apply(EventAdapterService eventAdapterService, Object und, EventType type) {
                                    return eventAdapterService.adapterForTypedAvro(und, type);
                                }
                            };
                        } else {
                            func = new TriFunction<EventAdapterService, Object, EventType, EventBean>() {
                                public EventBean apply(EventAdapterService eventAdapterService, Object und, EventType type) {
                                    return eventAdapterService.adapterForTypedBean(und, type);
                                }
                            };
                        }
                        return new EvalInsertNoWildcardSingleColCoercionRevisionFunc(selectExprContext, resultEventType, vaeProcessor, vaeInnerEventType, func);
                    }
                }
            }
            if (resultEventType == null) {
                if (vaeProcessor != null) {
                    // Use an anonymous type if the target is not a variant stream
                    if (valueAddEventService.getValueAddProcessor(insertIntoDesc.getEventTypeName()) == null) {
                        resultEventType = eventAdapterService.createAnonymousMapType(statementId + "_vae_" + CollectionUtil.toString(assignedTypeNumberStack, "_"), selPropertyTypes, true);
                    } else {
                        String statementName = "stmt_" + statementId + "_insert";
                        resultEventType = eventAdapterService.addNestableMapType(statementName, selPropertyTypes, null, false, false, false, false, true);
                    }
                } else {
                    EventType existingType = insertIntoTargetType;
                    if (existingType == null) {
                        // The type may however be an auto-import or fully-qualified class name
                        Class clazz = null;
                        try {
                            clazz = this.engineImportService.resolveClass(insertIntoDesc.getEventTypeName(), false);
                        } catch (EngineImportException e) {
                            log.debug("Target stream name '" + insertIntoDesc.getEventTypeName() + "' is not resolved as a class name");
                        }
                        if (clazz != null) {
                            existingType = eventAdapterService.addBeanType(clazz.getName(), clazz, false, false, false);
                        }
                    }

                    SelectExprProcessor selectExprInsertEventBean = null;
                    if (existingType != null) {
                        selectExprInsertEventBean = SelectExprInsertEventBeanFactory.getInsertUnderlyingNonJoin(eventAdapterService, existingType, isUsingWildcard, typeService, exprEvaluators, columnNames, expressionReturnTypes, engineImportService, insertIntoDesc, columnNamesAsProvided, false, statementName);
                    }
                    if (selectExprInsertEventBean != null) {
                        return selectExprInsertEventBean;
                    } else {
                        // use the provided override-type if there is one
                        if (optionalInsertIntoOverrideType != null) {
                            resultEventType = insertIntoTargetType;
                        } else if (existingType instanceof AvroSchemaEventType) {
                            eventAdapterService.getEventAdapterAvroHandler().avroCompat(existingType, selPropertyTypes);
                            resultEventType = existingType;
                        } else {
                            EventUnderlyingType out = EventRepresentationUtil.getRepresentation(annotations, configuration, CreateSchemaDesc.AssignedType.NONE);
                            if (out == EventUnderlyingType.MAP) {
                                resultEventType = eventAdapterService.addNestableMapType(insertIntoDesc.getEventTypeName(), selPropertyTypes, null, false, false, false, false, true);
                            } else if (out == EventUnderlyingType.OBJECTARRAY) {
                                resultEventType = eventAdapterService.addNestableObjectArrayType(insertIntoDesc.getEventTypeName(), selPropertyTypes, null, false, false, false, false, true, false, null);
                            } else if (out == EventUnderlyingType.AVRO) {
                                resultEventType = eventAdapterService.addAvroType(insertIntoDesc.getEventTypeName(), selPropertyTypes, false, false, false, false, true, annotations, null, statementName, typeService.getEngineURIQualifier());
                            } else {
                                throw new IllegalStateException("Unrecognized code " + out);
                            }
                        }
                    }
                }
            }
            if (vaeProcessor != null) {
                vaeProcessor.validateEventType(resultEventType);
                vaeInnerEventType = resultEventType;
                resultEventType = vaeProcessor.getValueAddEventType();
                isRevisionEvent = true;
            }

            if (!isRevisionEvent) {
                if (resultEventType instanceof MapEventType) {
                    return new EvalInsertNoWildcardMap(selectExprContext, resultEventType);
                } else if (resultEventType instanceof ObjectArrayEventType) {
                    return makeObjectArrayConsiderReorder(selectExprContext, (ObjectArrayEventType) resultEventType, statementName, typeService.getEngineURIQualifier());
                } else if (resultEventType instanceof AvroSchemaEventType) {
                    return eventAdapterService.getEventAdapterAvroHandler().getOutputFactory().makeSelectNoWildcard(selectExprContext, resultEventType, tableService, statementName, typeService.getEngineURIQualifier());
                } else {
                    throw new IllegalStateException("Unrecognized output type " + resultEventType);
                }
            } else {
                return new EvalInsertNoWildcardRevision(selectExprContext, resultEventType, vaeProcessor, vaeInnerEventType);
            }
        } catch (EventAdapterException ex) {
            log.debug("Exception provided by event adapter: " + ex.getMessage(), ex);
            throw new ExprValidationException(ex.getMessage(), ex);
        }
    }

    private boolean isGroupByRollupNullableExpression(ExprNode expr, GroupByRollupInfo groupByRollupInfo) {
        // if all levels include this key, we are fine
        for (AggregationGroupByRollupLevel level : groupByRollupInfo.getRollupDesc().getLevels()) {
            if (level.isAggregationTop()) {
                return true;
            }
            boolean found = false;
            for (int rollupKeyIndex : level.getRollupKeys()) {
                ExprNode groupExpression = groupByRollupInfo.getExprNodes()[rollupKeyIndex];
                if (ExprNodeUtility.deepEquals(groupExpression, expr)) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                return true;
            }
        }
        return false;
    }

    private SelectExprProcessor makeObjectArrayConsiderReorder(SelectExprContext selectExprContext, ObjectArrayEventType resultEventType, String statementName, String engineURI)
            throws ExprValidationException {
        TypeWidener[] wideners = new TypeWidener[selectExprContext.getColumnNames().length];
        int[] remapped = new int[selectExprContext.getColumnNames().length];
        boolean needRemap = false;
        for (int i = 0; i < selectExprContext.getColumnNames().length; i++) {
            String colName = selectExprContext.getColumnNames()[i];
            int index = CollectionUtil.findItem(resultEventType.getPropertyNames(), colName);
            if (index == -1) {
                throw new ExprValidationException("Could not find property '" + colName + "' in " + getTypeNameConsiderTable(resultEventType, tableService));
            }
            remapped[i] = index;
            if (index != i) {
                needRemap = true;
            }
            Class sourceColumnType = selectExprContext.getExpressionNodes()[i].getType();
            Class targetPropType = resultEventType.getPropertyType(colName);
            wideners[i] = TypeWidenerFactory.getCheckPropertyAssignType(colName, sourceColumnType, targetPropType, colName, false, eventAdapterService.getTypeWidenerCustomizer(resultEventType), statementName, engineURI);
        }

        if (!needRemap) {
            return new EvalInsertNoWildcardObjectArray(selectExprContext, resultEventType);
        }
        if (CollectionUtil.isAllNullArray(wideners)) {
            return new EvalInsertNoWildcardObjectArrayRemap(selectExprContext, resultEventType, remapped);
        }
        return new EvalInsertNoWildcardObjectArrayRemapWWiden(selectExprContext, resultEventType, remapped, wideners);
    }

    private String getTypeNameConsiderTable(ObjectArrayEventType resultEventType, TableService tableService) {
        TableMetadata metadata = tableService.getTableMetadataFromEventType(resultEventType);
        if (metadata != null) {
            return "table '" + metadata.getTableName() + "'";
        }
        return "type '" + resultEventType.getName() + "'";
    }

    private Pair<ExprEvaluator, Object> handleUnderlyingStreamInsert(ExprEvaluator exprEvaluator, NamedWindowMgmtService namedWindowMgmtService, final EventAdapterService eventAdapterService) {
        if (!(exprEvaluator instanceof ExprStreamUnderlyingNode)) {
            return null;
        }
        final ExprStreamUnderlyingNode undNode = (ExprStreamUnderlyingNode) exprEvaluator;
        final int streamNum = undNode.getStreamId();
        final Class returnType = undNode.getExprEvaluator().getType();
        final EventType namedWindowAsType = getNamedWindowUnderlyingType(namedWindowMgmtService, eventAdapterService, typeService.getEventTypes()[streamNum]);
        final TableMetadata tableMetadata = tableService.getTableMetadataFromEventType(typeService.getEventTypes()[streamNum]);

        EventType eventTypeStream;
        ExprEvaluator evaluator;
        if (tableMetadata != null) {
            eventTypeStream = tableMetadata.getPublicEventType();
            evaluator = new SelectExprProcessorEvalStreamInsertTable(streamNum, undNode, tableMetadata, returnType);
        } else if (namedWindowAsType == null) {
            eventTypeStream = typeService.getEventTypes()[streamNum];
            evaluator = new SelectExprProcessorEvalStreamInsertUnd(undNode, streamNum, returnType);
        } else {
            eventTypeStream = namedWindowAsType;
            evaluator = new SelectExprProcessorEvalStreamInsertNamedWindow(streamNum, namedWindowAsType, returnType, eventAdapterService);
        }

        return new Pair<ExprEvaluator, Object>(evaluator, eventTypeStream);
    }

    private EventType getNamedWindowUnderlyingType(NamedWindowMgmtService namedWindowMgmtService, EventAdapterService eventAdapterService, EventType eventType) {
        if (!namedWindowMgmtService.isNamedWindow(eventType.getName())) {
            return null;
        }
        NamedWindowProcessor processor = namedWindowMgmtService.getProcessor(eventType.getName());
        if (processor.getEventTypeAsName() == null) {
            return null;
        }
        return eventAdapterService.getExistsTypeByName(processor.getEventTypeAsName());
    }

    private static EPType[] determineInsertedEventTypeTargets(EventType targetType, List<SelectClauseExprCompiledSpec> selectionList) {
        EPType[] targets = new EPType[selectionList.size()];
        if (targetType == null) {
            return targets;
        }

        for (int i = 0; i < selectionList.size(); i++) {
            SelectClauseExprCompiledSpec expr = selectionList.get(i);
            if (expr.getProvidedName() == null) {
                continue;
            }

            EventPropertyDescriptor desc = targetType.getPropertyDescriptor(expr.getProvidedName());
            if (desc == null) {
                continue;
            }

            if (!desc.isFragment()) {
                continue;
            }

            FragmentEventType fragmentEventType = targetType.getFragmentType(expr.getProvidedName());
            if (fragmentEventType == null) {
                continue;
            }

            if (fragmentEventType.isIndexed()) {
                targets[i] = EPTypeHelper.collectionOfEvents(fragmentEventType.getFragmentType());
            } else {
                targets[i] = EPTypeHelper.singleEvent(fragmentEventType.getFragmentType());
            }
        }

        return targets;
    }

    private TypeAndFunctionPair handleTypableExpression(ExprEvaluator exprEvaluator, int expressionNum)
            throws ExprValidationException {
        if (!(exprEvaluator instanceof ExprEvaluatorTypableReturn)) {
            return null;
        }

        ExprEvaluatorTypableReturn typable = (ExprEvaluatorTypableReturn) exprEvaluator;
        LinkedHashMap<String, Object> eventTypeExpr = typable.getRowProperties();
        if (eventTypeExpr == null) {
            return null;
        }

        EventType mapType = eventAdapterService.createAnonymousMapType(statementId + "_innereval_" + CollectionUtil.toString(assignedTypeNumberStack, "_") + "_" + expressionNum, eventTypeExpr, true);
        ExprEvaluator evaluatorFragment = new SelectExprProcessorEvalTypableMap(mapType, exprEvaluator, eventAdapterService);

        return new TypeAndFunctionPair(mapType, evaluatorFragment);
    }

    private TypeAndFunctionPair handleInsertIntoEnumeration(String insertIntoColName, EPType insertIntoTarget, ExprEvaluator exprEvaluator, EngineImportService engineImportService)
            throws ExprValidationException {
        if (!(exprEvaluator instanceof ExprEvaluatorEnumeration) || insertIntoTarget == null
                || (!EPTypeHelper.isCarryEvent(insertIntoTarget))) {
            return null;
        }

        final ExprEvaluatorEnumeration enumeration = (ExprEvaluatorEnumeration) exprEvaluator;
        final EventType eventTypeSingle = enumeration.getEventTypeSingle(eventAdapterService, statementId);
        final EventType eventTypeColl = enumeration.getEventTypeCollection(eventAdapterService, statementId);
        final EventType sourceType = eventTypeSingle != null ? eventTypeSingle : eventTypeColl;
        if (eventTypeColl == null && eventTypeSingle == null) {
            return null;    // enumeration is untyped events (select-clause provided to subquery or 'new' operator)
        }
        if (((EventTypeSPI) sourceType).getMetadata().getTypeClass() == EventTypeMetadata.TypeClass.ANONYMOUS) {
            return null;    // we don't allow anonymous types here, thus excluding subquery multi-column selection
        }

        // check type info
        final EventType targetType = EPTypeHelper.getEventType(insertIntoTarget);
        checkTypeCompatible(insertIntoColName, targetType, sourceType);

        // handle collection target - produce EventBean[]
        if (insertIntoTarget instanceof EventMultiValuedEPType) {
            if (eventTypeColl != null) {
                ExprEvaluator evaluatorFragment = new ExprEvaluator() {
                    public Object evaluate(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
                        Collection<EventBean> events = enumeration.evaluateGetROCollectionEvents(eventsPerStream, isNewData, exprEvaluatorContext);
                        if (events == null) {
                            return null;
                        }
                        return events.toArray(new EventBean[events.size()]);
                    }

                    public Class getType() {
                        return JavaClassHelper.getArrayType(targetType.getUnderlyingType());
                    }
                };
                return new TypeAndFunctionPair(new EventType[]{targetType}, evaluatorFragment);
            }
            ExprEvaluator evaluatorFragment = new ExprEvaluator() {
                public Object evaluate(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
                    EventBean event = enumeration.evaluateGetEventBean(eventsPerStream, isNewData, exprEvaluatorContext);
                    if (event == null) {
                        return null;
                    }
                    return new EventBean[]{event};
                }

                public Class getType() {
                    return JavaClassHelper.getArrayType(targetType.getUnderlyingType());
                }
            };
            return new TypeAndFunctionPair(new EventType[]{targetType}, evaluatorFragment);
        }

        // handle single-bean target
        // handle single-source
        if (eventTypeSingle != null) {
            ExprEvaluator evaluatorFragment = new ExprEvaluator() {
                public Object evaluate(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
                    return enumeration.evaluateGetEventBean(eventsPerStream, isNewData, exprEvaluatorContext);
                }

                public Class getType() {
                    return targetType.getUnderlyingType();
                }
            };
            return new TypeAndFunctionPair(targetType, evaluatorFragment);
        }

        // handle collection-source by taking the first
        ExprEvaluator evaluatorFragment = new ExprEvaluator() {
            public Object evaluate(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
                Collection<EventBean> events = enumeration.evaluateGetROCollectionEvents(eventsPerStream, isNewData, exprEvaluatorContext);
                if (events == null || events.size() == 0) {
                    return null;
                }
                return EventBeanUtility.getNonemptyFirstEvent(events);
            }

            public Class getType() {
                return targetType.getUnderlyingType();
            }
        };
        return new TypeAndFunctionPair(targetType, evaluatorFragment);
    }

    private void checkTypeCompatible(String insertIntoCol, EventType targetType, EventType selectedType)
            throws ExprValidationException {
        if (!EventTypeUtility.isTypeOrSubTypeOf(targetType, selectedType)) {
            throw new ExprValidationException(
                    "Incompatible type detected attempting to insert into column '" +
                            insertIntoCol + "' type '" + targetType.getName() + "' compared to selected type '" + selectedType.getName() + "'");
        }
    }

    private TypeAndFunctionPair handleInsertIntoTypableExpression(EPType insertIntoTarget, ExprEvaluator exprEvaluator, EngineImportService engineImportService)
            throws ExprValidationException {
        if (!(exprEvaluator instanceof ExprEvaluatorTypableReturn)
                || insertIntoTarget == null
                || (!EPTypeHelper.isCarryEvent(insertIntoTarget))) {
            return null;
        }

        final EventType targetType = EPTypeHelper.getEventType(insertIntoTarget);
        final ExprEvaluatorTypableReturn typable = (ExprEvaluatorTypableReturn) exprEvaluator;
        if (typable.isMultirow() == null) { // not typable after all
            return null;
        }
        LinkedHashMap<String, Object> eventTypeExpr = typable.getRowProperties();
        if (eventTypeExpr == null) {
            return null;
        }

        Set<WriteablePropertyDescriptor> writables = eventAdapterService.getWriteableProperties(targetType, false);
        List<WriteablePropertyDescriptor> written = new ArrayList<WriteablePropertyDescriptor>();
        List<Map.Entry<String, Object>> writtenOffered = new ArrayList<Map.Entry<String, Object>>();

        // from Map<String, Object> determine properties and type widening that may be required
        for (Map.Entry<String, Object> offeredProperty : eventTypeExpr.entrySet()) {
            WriteablePropertyDescriptor writable = EventTypeUtility.findWritable(offeredProperty.getKey(), writables);
            if (writable == null) {
                throw new ExprValidationException("Failed to find property '" + offeredProperty.getKey() + "' among properties for target event type '" + targetType.getName() + "'");
            }
            written.add(writable);
            writtenOffered.add(offeredProperty);
        }

        // determine widening and column type compatibility
        final TypeWidener[] wideners = new TypeWidener[written.size()];
        TypeWidenerCustomizer typeWidenerCustomizer = eventAdapterService.getTypeWidenerCustomizer(targetType);
        for (int i = 0; i < written.size(); i++) {
            Class expected = written.get(i).getType();
            Map.Entry<String, Object> provided = writtenOffered.get(i);
            if (provided.getValue() instanceof Class) {
                wideners[i] = TypeWidenerFactory.getCheckPropertyAssignType(provided.getKey(), (Class) provided.getValue(),
                        expected, written.get(i).getPropertyName(), false, typeWidenerCustomizer, statementName, typeService.getEngineURIQualifier());
            }
        }
        final boolean hasWideners = !CollectionUtil.isAllNullArray(wideners);

        // obtain factory
        WriteablePropertyDescriptor[] writtenArray = written.toArray(new WriteablePropertyDescriptor[written.size()]);
        EventBeanManufacturer manufacturer;
        try {
            manufacturer = eventAdapterService.getManufacturer(targetType, writtenArray, engineImportService, false);
        } catch (EventBeanManufactureException e) {
            throw new ExprValidationException("Failed to obtain eventbean factory: " + e.getMessage(), e);
        }

        // handle collection
        final EventBeanManufacturer factory = manufacturer;
        if (insertIntoTarget instanceof EventMultiValuedEPType && typable.isMultirow()) {
            ExprEvaluator evaluatorFragment = new ExprEvaluator() {
                public Object evaluate(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
                    Object[][] rows = typable.evaluateTypableMulti(eventsPerStream, isNewData, exprEvaluatorContext);
                    if (rows == null) {
                        return null;
                    }
                    if (rows.length == 0) {
                        return new EventBean[0];
                    }
                    if (hasWideners) {
                        applyWideners(rows, wideners);
                    }
                    EventBean[] events = new EventBean[rows.length];
                    for (int i = 0; i < events.length; i++) {
                        events[i] = factory.make(rows[i]);
                    }
                    return events;
                }

                public Class getType() {
                    return JavaClassHelper.getArrayType(targetType.getUnderlyingType());
                }
            };

            return new TypeAndFunctionPair(new EventType[]{targetType}, evaluatorFragment);
        } else if (insertIntoTarget instanceof EventMultiValuedEPType && !typable.isMultirow()) {
            ExprEvaluator evaluatorFragment = new ExprEvaluator() {
                public Object evaluate(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
                    Object[] row = typable.evaluateTypableSingle(eventsPerStream, isNewData, exprEvaluatorContext);
                    if (row == null) {
                        return null;
                    }
                    if (hasWideners) {
                        applyWideners(row, wideners);
                    }
                    return new EventBean[]{factory.make(row)};
                }

                public Class getType() {
                    return JavaClassHelper.getArrayType(targetType.getUnderlyingType());
                }
            };
            return new TypeAndFunctionPair(new EventType[]{targetType}, evaluatorFragment);
        } else if (insertIntoTarget instanceof EventEPType && !typable.isMultirow()) {
            ExprEvaluator evaluatorFragment = new ExprEvaluator() {
                public Object evaluate(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
                    Object[] row = typable.evaluateTypableSingle(eventsPerStream, isNewData, exprEvaluatorContext);
                    if (row == null) {
                        return null;
                    }
                    if (hasWideners) {
                        applyWideners(row, wideners);
                    }
                    return factory.make(row);
                }

                public Class getType() {
                    return JavaClassHelper.getArrayType(targetType.getUnderlyingType());
                }
            };
            return new TypeAndFunctionPair(targetType, evaluatorFragment);
        }

        // we are discarding all but the first row
        ExprEvaluator evaluatorFragment = new ExprEvaluator() {
            public Object evaluate(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
                Object[][] rows = typable.evaluateTypableMulti(eventsPerStream, isNewData, exprEvaluatorContext);
                if (rows == null) {
                    return null;
                }
                if (rows.length == 0) {
                    return new EventBean[0];
                }
                if (hasWideners) {
                    applyWideners(rows[0], wideners);
                }
                return factory.make(rows[0]);
            }

            public Class getType() {
                return JavaClassHelper.getArrayType(targetType.getUnderlyingType());
            }
        };
        return new TypeAndFunctionPair(targetType, evaluatorFragment);
    }

    private void applyWideners(Object[] row, TypeWidener[] wideners) {
        for (int i = 0; i < wideners.length; i++) {
            if (wideners[i] != null) {
                row[i] = wideners[i].widen(row[i]);
            }
        }
    }

    private void applyWideners(Object[][] rows, TypeWidener[] wideners) {
        for (Object[] row : rows) {
            applyWideners(row, wideners);
        }
    }

    private TypeAndFunctionPair handleAtEventbeanEnumeration(boolean isEventBeans, ExprEvaluator evaluator)
            throws ExprValidationException {
        if (!(evaluator instanceof ExprEvaluatorEnumeration) || !isEventBeans) {
            return null;
        }

        final ExprEvaluatorEnumeration enumEval = (ExprEvaluatorEnumeration) evaluator;
        final EventType eventTypeSingle = enumEval.getEventTypeSingle(eventAdapterService, statementId);
        if (eventTypeSingle != null) {
            final TableMetadata tableMetadata = tableService.getTableMetadataFromEventType(eventTypeSingle);
            if (tableMetadata == null) {
                ExprEvaluator evaluatorFragment = new ExprEvaluator() {
                    public Object evaluate(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
                        return enumEval.evaluateGetEventBean(eventsPerStream, isNewData, exprEvaluatorContext);
                    }

                    public Class getType() {
                        return eventTypeSingle.getUnderlyingType();
                    }
                };
                return new TypeAndFunctionPair(eventTypeSingle, evaluatorFragment);
            }
            ExprEvaluator evaluatorFragment = new ExprEvaluator() {
                public Object evaluate(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
                    EventBean event = enumEval.evaluateGetEventBean(eventsPerStream, isNewData, exprEvaluatorContext);
                    if (event == null) {
                        return null;
                    }
                    return tableMetadata.getEventToPublic().convert(event, eventsPerStream, isNewData, exprEvaluatorContext);
                }

                public Class getType() {
                    return tableMetadata.getPublicEventType().getUnderlyingType();
                }
            };
            return new TypeAndFunctionPair(tableMetadata.getPublicEventType(), evaluatorFragment);
        }

        final EventType eventTypeColl = enumEval.getEventTypeCollection(eventAdapterService, statementId);
        if (eventTypeColl != null) {
            final TableMetadata tableMetadata = tableService.getTableMetadataFromEventType(eventTypeColl);
            if (tableMetadata == null) {
                ExprEvaluator evaluatorFragment = new ExprEvaluator() {
                    public Object evaluate(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
                        // the protocol is EventBean[]
                        Object result = enumEval.evaluateGetROCollectionEvents(eventsPerStream, isNewData, exprEvaluatorContext);
                        if (result != null && result instanceof Collection) {
                            Collection<EventBean> events = (Collection<EventBean>) result;
                            return events.toArray(new EventBean[events.size()]);
                        }
                        return result;
                    }

                    public Class getType() {
                        return JavaClassHelper.getArrayType(eventTypeColl.getUnderlyingType());
                    }
                };
                return new TypeAndFunctionPair(new EventType[]{eventTypeColl}, evaluatorFragment);
            }
            ExprEvaluator evaluatorFragment = new ExprEvaluator() {
                public Object evaluate(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
                    // the protocol is EventBean[]
                    Object result = enumEval.evaluateGetROCollectionEvents(eventsPerStream, isNewData, exprEvaluatorContext);
                    if (result == null) {
                        return null;
                    }
                    if (result instanceof Collection) {
                        Collection<EventBean> events = (Collection<EventBean>) result;
                        EventBean[] out = new EventBean[events.size()];
                        int index = 0;
                        for (EventBean event : events) {
                            out[index++] = tableMetadata.getEventToPublic().convert(event, eventsPerStream, isNewData, exprEvaluatorContext);
                        }
                        return out;
                    }
                    EventBean[] events = (EventBean[]) result;
                    for (int i = 0; i < events.length; i++) {
                        events[i] = tableMetadata.getEventToPublic().convert(events[i], eventsPerStream, isNewData, exprEvaluatorContext);
                    }
                    return events;
                }

                public Class getType() {
                    return JavaClassHelper.getArrayType(tableMetadata.getPublicEventType().getUnderlyingType());
                }
            };
            return new TypeAndFunctionPair(new EventType[]{tableMetadata.getPublicEventType()}, evaluatorFragment);
        }

        return null;
    }

    // Determine which properties provided by the Map must be downcast from EventBean to Object
    private static Set<String> getEventBeanToObjectProps(Map<String, Object> selPropertyTypes, EventType resultEventType) {

        if (!(resultEventType instanceof BaseNestableEventType)) {
            return Collections.emptySet();
        }
        BaseNestableEventType mapEventType = (BaseNestableEventType) resultEventType;
        Set<String> props = null;
        for (Map.Entry<String, Object> entry : selPropertyTypes.entrySet()) {
            if (entry.getValue() instanceof BeanEventType && mapEventType.getTypes().get(entry.getKey()) instanceof Class) {
                if (props == null) {
                    props = new HashSet<String>();
                }
                props.add(entry.getKey());
            }
        }
        if (props == null) {
            return Collections.emptySet();
        }
        return props;
    }

    private static void verifyInsertInto(InsertIntoDesc insertIntoDesc,
                                         List<SelectClauseExprCompiledSpec> selectionList)
            throws ExprValidationException {
        // Verify all column names are unique
        Set<String> names = new HashSet<String>();
        for (String element : insertIntoDesc.getColumnNames()) {
            if (names.contains(element)) {
                throw new ExprValidationException("Property name '" + element + "' appears more then once in insert-into clause");
            }
            names.add(element);
        }

        // Verify number of columns matches the select clause
        if ((!insertIntoDesc.getColumnNames().isEmpty()) &&
                (insertIntoDesc.getColumnNames().size() != selectionList.size())) {
            throw new ExprValidationException("Number of supplied values in the select or values clause does not match insert-into clause");
        }
    }

    private static class TypeAndFunctionPair {
        private final Object type;
        private final ExprEvaluator function;

        private TypeAndFunctionPair(Object type, ExprEvaluator function) {
            this.type = type;
            this.function = function;
        }

        public Object getType() {
            return type;
        }

        public ExprEvaluator getFunction() {
            return function;
        }
    }
}
