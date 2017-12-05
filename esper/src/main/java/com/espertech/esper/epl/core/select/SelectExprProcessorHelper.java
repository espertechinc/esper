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
package com.espertech.esper.epl.core.select;

import com.espertech.esper.client.*;
import com.espertech.esper.client.util.EventUnderlyingType;
import com.espertech.esper.codegen.base.CodegenBlock;
import com.espertech.esper.codegen.base.CodegenClassScope;
import com.espertech.esper.codegen.base.CodegenMethodNode;
import com.espertech.esper.codegen.base.CodegenMethodScope;
import com.espertech.esper.codegen.model.expression.CodegenExpression;
import com.espertech.esper.codegen.model.expression.CodegenExpressionRef;
import com.espertech.esper.collection.Pair;
import com.espertech.esper.epl.agg.service.common.AggregationGroupByRollupLevel;
import com.espertech.esper.epl.core.engineimport.EngineImportException;
import com.espertech.esper.epl.core.engineimport.EngineImportService;
import com.espertech.esper.epl.core.resultset.core.GroupByRollupInfo;
import com.espertech.esper.epl.core.select.eval.*;
import com.espertech.esper.epl.core.streamtype.StreamTypeService;
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
import com.espertech.esper.core.service.speccompiled.SelectClauseStreamCompiledSpec;
import com.espertech.esper.epl.table.mgmt.TableMetadata;
import com.espertech.esper.epl.table.mgmt.TableService;
import com.espertech.esper.epl.util.EventRepresentationUtil;
import com.espertech.esper.event.*;
import com.espertech.esper.event.arr.ObjectArrayEventType;
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

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.*;

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

    public SelectExprProcessorForge getForge() throws ExprValidationException {

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
        SelectExprProcessorForge joinWildcardProcessor = null;
        if (typeService.getStreamNames().length > 1 && isUsingWildcard) {
            joinWildcardProcessor = SelectExprJoinWildcardProcessorFactory.create(assignedTypeNumberStack, statementId, statementName, typeService.getStreamNames(), typeService.getEventTypes(), eventAdapterService, null, selectExprEventTypeRegistry, engineImportService, annotations, configuration, tableService, typeService.getEngineURIQualifier(), typeService.isOnDemandStreams());
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
        ExprForge[] exprForges = new ExprForge[selectionList.size()];
        ExprNode[] exprNodes = new ExprNode[selectionList.size()];
        Object[] expressionReturnTypes = new Object[selectionList.size()];
        for (int i = 0; i < selectionList.size(); i++) {
            SelectClauseExprCompiledSpec spec = selectionList.get(i);
            ExprNode expr = spec.getSelectExpression();
            ExprForge forge = expr.getForge();
            exprNodes[i] = expr;

            // if there is insert-into specification, use that
            if (insertIntoDesc != null) {
                // handle insert-into, with well-defined target event-typed column, and enumeration
                TypeAndForgePair pair = handleInsertIntoEnumeration(spec.getProvidedName(), insertIntoTargetsPerCol[i], forge);
                if (pair != null) {
                    expressionReturnTypes[i] = pair.getType();
                    exprForges[i] = pair.getForge();
                    continue;
                }

                // handle insert-into with well-defined target event-typed column, and typable expression
                pair = handleInsertIntoTypableExpression(insertIntoTargetsPerCol[i], forge, engineImportService);
                if (pair != null) {
                    expressionReturnTypes[i] = pair.getType();
                    exprForges[i] = pair.getForge();
                    continue;
                }
            }

            // handle @eventbean annotation, i.e. well-defined type through enumeration
            TypeAndForgePair pair = handleAtEventbeanEnumeration(spec.isEvents(), forge);
            if (pair != null) {
                expressionReturnTypes[i] = pair.getType();
                exprForges[i] = pair.getForge();
                continue;
            }

            // handle typeable return, i.e. typable multi-column return without provided target type
            pair = handleTypableExpression(forge, i);
            if (pair != null) {
                expressionReturnTypes[i] = pair.getType();
                exprForges[i] = pair.getForge();
                continue;
            }

            // handle select-clause expressions that match group-by expressions with rollup and therefore should be boxed types as rollup can produce a null value
            if (groupByRollupInfo != null && groupByRollupInfo.getRollupDesc() != null) {
                Class returnType = forge.getEvaluationType();
                Class returnTypeBoxed = JavaClassHelper.getBoxedType(returnType);
                if (returnType != returnTypeBoxed && isGroupByRollupNullableExpression(expr, groupByRollupInfo)) {
                    exprForges[i] = forge;
                    expressionReturnTypes[i] = returnTypeBoxed;
                    continue;
                }
            }

            // assign normal expected return type
            exprForges[i] = forge;
            expressionReturnTypes[i] = exprForges[i].getEvaluationType();
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
                EventPropertyGetterSPI getter = ((EventTypeSPI) eventTypeStream).getGetterSPI(propertyName);
                Class returnType = eventTypeStream.getPropertyType(propertyName);
                exprForges[i] = new SelectExprProcessorEvalByGetter(streamNum, getter, returnType);
            } else if ((insertIntoTargetType != null) && expressionReturnTypes[i] instanceof Class &&
                    (fragmentType.getFragmentType().getUnderlyingType() == ((Class) expressionReturnTypes[i]).getComponentType()) &&
                    ((targetFragment == null) || (targetFragment != null && targetFragment.isNative()))) {
                // same for arrays: may need to unwrap the fragment if the target type has this underlying type
                EventPropertyGetterSPI getter = ((EventTypeSPI) eventTypeStream).getGetterSPI(propertyName);
                Class returnType = eventTypeStream.getPropertyType(propertyName);
                exprForges[i] = new SelectExprProcessorEvalByGetter(streamNum, getter, returnType);
            } else {
                EventPropertyGetterSPI getter = ((EventTypeSPI) eventTypeStream).getGetterSPI(propertyName);
                FragmentEventType fragType = eventTypeStream.getFragmentType(propertyName);
                Class undType = fragType.getFragmentType().getUnderlyingType();
                Class returnType = fragType.isIndexed() ? JavaClassHelper.getArrayType(undType) : undType;
                exprForges[i] = new SelectExprProcessorEvalByGetterFragment(streamNum, getter, returnType, fragmentType);
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
            Pair<ExprForge, Object> pair = handleUnderlyingStreamInsert(exprForges[i], namedWindowMgmtService, eventAdapterService);
            if (pair != null) {
                exprForges[i] = pair.getFirst();
                expressionReturnTypes[i] = pair.getSecond();
            }
        }

        // Build event type that reflects all selected properties
        Map<String, Object> selPropertyTypes = new LinkedHashMap<String, Object>();
        int count = 0;
        for (int i = 0; i < exprForges.length; i++) {
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
        EventPropertyGetterSPI underlyingPropertyEventGetter = null;
        ExprForge underlyingExprForge = null;

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
                            underlyingPropertyEventGetter = ((EventTypeSPI) typeService.getEventTypes()[streamNumber]).getGetterSPI(propertyName);
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
                            Class returnType = expression.getForge().getEvaluationType();
                            if (returnType == Object[].class || JavaClassHelper.isImplementsInterface(returnType, Map.class) || JavaClassHelper.isJavaBuiltinDataType(returnType)) {
                                throw new ExprValidationException("Invalid expression return type '" + returnType.getName() + "' for transpose function");
                            }
                            underlyingEventType = eventAdapterService.addBeanType(returnType.getName(), returnType, false, false, false);
                            selectExprEventTypeRegistry.add(underlyingEventType);
                            underlyingExprForge = expression.getForge();
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

        // obtains evaluators
        SelectExprForgeContext selectExprForgeContext = new SelectExprForgeContext(exprForges, columnNames, eventAdapterService, typeService.getEventTypes());

        if (insertIntoDesc == null) {
            if (!selectedStreams.isEmpty()) {
                EventType resultEventType;
                if (underlyingEventType != null) {
                    TableMetadata tableMetadata = tableService.getTableMetadataFromEventType(underlyingEventType);
                    if (tableMetadata != null) {
                        underlyingEventType = tableMetadata.getPublicEventType();
                    }
                    resultEventType = eventAdapterService.createAnonymousWrapperType(statementId + "_wrapout_" + CollectionUtil.toString(assignedTypeNumberStack, "_"), underlyingEventType, selPropertyTypes);
                    return new EvalSelectStreamWUnderlying(selectExprForgeContext, resultEventType, namedStreams, isUsingWildcard,
                            unnamedStreams, singleStreamWrapper, underlyingIsFragmentEvent, underlyingStreamNumber, underlyingPropertyEventGetter, underlyingExprForge, tableMetadata, typeService.getEventTypes());
                } else {
                    resultEventType = eventAdapterService.createAnonymousMapType(statementId + "_mapout_" + CollectionUtil.toString(assignedTypeNumberStack, "_"), selPropertyTypes, true);
                    return new EvalSelectStreamNoUnderlyingMap(selectExprForgeContext, resultEventType, namedStreams, isUsingWildcard);
                }
            }

            if (isUsingWildcard) {
                EventType resultEventType = eventAdapterService.createAnonymousWrapperType(statementId + "_wrapoutwild_" + CollectionUtil.toString(assignedTypeNumberStack, "_"), eventType, selPropertyTypes);
                if (singleStreamWrapper) {
                    return new EvalInsertWildcardSSWrapper(selectExprForgeContext, resultEventType);
                }
                if (joinWildcardProcessor == null) {
                    return new EvalSelectWildcard(selectExprForgeContext, resultEventType);
                }
                return new EvalSelectWildcardJoin(selectExprForgeContext, resultEventType, joinWildcardProcessor);
            }

            EventType resultEventType;
            EventUnderlyingType representation = EventRepresentationUtil.getRepresentation(annotations, configuration, CreateSchemaDesc.AssignedType.NONE);
            if (representation == EventUnderlyingType.OBJECTARRAY) {
                resultEventType = eventAdapterService.createAnonymousObjectArrayType(statementId + "_result_" + CollectionUtil.toString(assignedTypeNumberStack, "_"), selPropertyTypes);
            } else if (representation == EventUnderlyingType.AVRO) {
                resultEventType = eventAdapterService.createAnonymousAvroType(statementId + "_result_" + CollectionUtil.toString(assignedTypeNumberStack, "_"), selPropertyTypes, annotations, statementName, typeService.getEngineURIQualifier());
            } else {
                resultEventType = eventAdapterService.createAnonymousMapType(statementId + "_result_" + CollectionUtil.toString(assignedTypeNumberStack, "_"), selPropertyTypes, true);
            }

            if (selectExprForgeContext.getExprForges().length == 0) {
                return new EvalSelectNoWildcardEmptyProps(selectExprForgeContext, resultEventType);
            } else {
                if (representation == EventUnderlyingType.OBJECTARRAY) {
                    return new EvalSelectNoWildcardObjectArray(selectExprForgeContext, resultEventType);
                } else if (representation == EventUnderlyingType.AVRO) {
                    return eventAdapterService.getEventAdapterAvroHandler().getOutputFactory().makeSelectNoWildcard(selectExprForgeContext, exprForges, resultEventType, tableService, statementName, typeService.getEngineURIQualifier());
                }
                return new EvalSelectNoWildcardMap(selectExprForgeContext, resultEventType);
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
                    if (exprForges.length != 0) {
                        throw new ExprValidationException("Cannot transpose additional properties in the select-clause to target event type '" +
                                insertIntoTargetType.getName() +
                                "' with underlying type '" + insertIntoTargetType.getUnderlyingType().getName() + "', the " + EngineImportService.EXT_SINGLEROW_FUNCTION_TRANSPOSE + " function must occur alone in the select clause");
                    }
                    ExprNode expression = unnamedStreams.get(0).getExpressionSelectedAsStream().getSelectExpression();
                    Class returnType = expression.getForge().getEvaluationType();
                    if (insertIntoTargetType instanceof ObjectArrayEventType && returnType == Object[].class) {
                        return new SelectExprInsertEventBeanFactory.SelectExprInsertNativeExpressionCoerceObjectArray(insertIntoTargetType, expression.getForge(), eventAdapterService);
                    } else if (insertIntoTargetType instanceof MapEventType && JavaClassHelper.isImplementsInterface(returnType, Map.class)) {
                        return new SelectExprInsertEventBeanFactory.SelectExprInsertNativeExpressionCoerceMap(insertIntoTargetType, expression.getForge(), eventAdapterService);
                    } else if (insertIntoTargetType instanceof BeanEventType && JavaClassHelper.isSubclassOrImplementsInterface(returnType, insertIntoTargetType.getUnderlyingType())) {
                        return new SelectExprInsertEventBeanFactory.SelectExprInsertNativeExpressionCoerceNative(insertIntoTargetType, expression.getForge(), eventAdapterService);
                    } else if (insertIntoTargetType instanceof AvroSchemaEventType && returnType.getName().equals(JavaClassHelper.APACHE_AVRO_GENERIC_RECORD_CLASSNAME)) {
                        return new SelectExprInsertEventBeanFactory.SelectExprInsertNativeExpressionCoerceAvro(insertIntoTargetType, expression.getForge(), eventAdapterService);
                    } else if (insertIntoTargetType instanceof WrapperEventType) {
                        // for native event types as they got renamed, they become wrappers
                        // check if the proposed wrapper is compatible with the existing wrapper
                        WrapperEventType existing = (WrapperEventType) insertIntoTargetType;
                        if (existing.getUnderlyingEventType() instanceof BeanEventType) {
                            BeanEventType innerType = (BeanEventType) existing.getUnderlyingEventType();
                            ExprNode exprNode = unnamedStreams.get(0).getExpressionSelectedAsStream().getSelectExpression();
                            if (!JavaClassHelper.isSubclassOrImplementsInterface(exprNode.getForge().getEvaluationType(), innerType.getUnderlyingType())) {
                                throw new ExprValidationException("Invalid expression return type '" + exprNode.getForge().getEvaluationType() + "' for transpose function, expected '" + innerType.getUnderlyingType().getSimpleName() + "'");
                            }
                            ExprForge evalExprForge = exprNode.getForge();
                            resultEventType = eventAdapterService.addWrapperType(insertIntoTargetType.getName(), existing.getUnderlyingEventType(), selPropertyTypes, false, true);
                            return new EvalSelectStreamWUnderlying(selectExprForgeContext, resultEventType, namedStreams, isUsingWildcard,
                                    unnamedStreams, false, false, underlyingStreamNumber, null, evalExprForge, null, typeService.getEventTypes());
                        }
                    }
                    throw EvalInsertUtil.makeEventTypeCastException(returnType, insertIntoTargetType);
                }

                if (underlyingEventType != null) {
                    // a single stream was selected via "stream.*" and there is no column name
                    // recast as a Map-type
                    if (underlyingEventType instanceof MapEventType && insertIntoTargetType instanceof MapEventType) {
                        return EvalSelectStreamWUndRecastMapFactory.make(typeService.getEventTypes(), selectExprForgeContext, selectedStreams.get(0).getStreamSelected().getStreamNumber(), insertIntoTargetType, exprNodes, engineImportService, statementName, typeService.getEngineURIQualifier());
                    }

                    // recast as a Object-array-type
                    if (underlyingEventType instanceof ObjectArrayEventType && insertIntoTargetType instanceof ObjectArrayEventType) {
                        return EvalSelectStreamWUndRecastObjectArrayFactory.make(typeService.getEventTypes(), selectExprForgeContext, selectedStreams.get(0).getStreamSelected().getStreamNumber(), insertIntoTargetType, exprNodes, engineImportService, statementName, typeService.getEngineURIQualifier());
                    }

                    // recast as a Avro-type
                    if (underlyingEventType instanceof AvroSchemaEventType && insertIntoTargetType instanceof AvroSchemaEventType) {
                        return eventAdapterService.getEventAdapterAvroHandler().getOutputFactory().makeRecast(typeService.getEventTypes(), selectExprForgeContext, selectedStreams.get(0).getStreamSelected().getStreamNumber(), (AvroSchemaEventType) insertIntoTargetType, exprNodes, statementName, typeService.getEngineURIQualifier());
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
                    return new EvalSelectStreamWUnderlying(selectExprForgeContext, resultEventType, namedStreams, isUsingWildcard,
                            unnamedStreams, singleStreamWrapper, underlyingIsFragmentEvent, underlyingStreamNumber, underlyingPropertyEventGetter, underlyingExprForge, tableMetadata, typeService.getEventTypes());
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
                            return new EvalSelectStreamNoUnderlyingMap(selectExprForgeContext, resultEventType, namedStreams, isUsingWildcard);
                        } else {
                            return new EvalSelectStreamNoUndWEventBeanToObj(selectExprForgeContext, resultEventType, namedStreams, isUsingWildcard, propertiesToUnwrap);
                        }
                    } else if (insertIntoTargetType instanceof ObjectArrayEventType) {
                        Set<String> propertiesToUnwrap = getEventBeanToObjectProps(selPropertyTypes, insertIntoTargetType);
                        if (propertiesToUnwrap.isEmpty()) {
                            return new EvalSelectStreamNoUnderlyingObjectArray(selectExprForgeContext, insertIntoTargetType, namedStreams, isUsingWildcard);
                        } else {
                            return new EvalSelectStreamNoUndWEventBeanToObjObjArray(selectExprForgeContext, insertIntoTargetType, namedStreams, isUsingWildcard, propertiesToUnwrap);
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
                        SelectExprProcessorForge existingTypeProcessor = SelectExprInsertEventBeanFactory.getInsertUnderlyingNonJoin(eventAdapterService, insertIntoTargetType, isUsingWildcard, typeService, exprForges, columnNames, expressionReturnTypes, engineImportService, insertIntoDesc, columnNamesAsProvided, true, statementName);
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
                        return new EvalInsertWildcardSSWrapper(selectExprForgeContext, resultEventType);
                    } else {
                        return new EvalInsertWildcardSSWrapperRevision(selectExprForgeContext, resultEventType, vaeProcessor);
                    }
                }
                if (joinWildcardProcessor == null) {
                    if (!isRevisionEvent) {
                        if (resultEventType instanceof WrapperEventType) {
                            return new EvalInsertWildcardWrapper(selectExprForgeContext, resultEventType);
                        } else {
                            return new EvalInsertWildcardBean(selectExprForgeContext, resultEventType);
                        }
                    } else {
                        if (exprForges.length == 0) {
                            return new EvalInsertWildcardRevision(selectExprForgeContext, resultEventType, vaeProcessor);
                        } else {
                            EventType wrappingEventType = eventAdapterService.addWrapperType(insertIntoDesc.getEventTypeName() + "_wrapped", eventType, selPropertyTypes, false, true);
                            return new EvalInsertWildcardRevisionWrapper(selectExprForgeContext, resultEventType, vaeProcessor, wrappingEventType);
                        }
                    }
                } else {
                    if (!isRevisionEvent) {
                        return new EvalInsertWildcardJoin(selectExprForgeContext, resultEventType, joinWildcardProcessor);
                    } else {
                        return new EvalInsertWildcardJoinRevision(selectExprForgeContext, resultEventType, joinWildcardProcessor, vaeProcessor);
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
                            return new EvalInsertNoWildcardSingleColCoercionMapWrap(selectExprForgeContext, wrapper);
                        } else if (wrapper.getUnderlyingEventType() instanceof ObjectArrayEventType) {
                            return new EvalInsertNoWildcardSingleColCoercionObjectArrayWrap(selectExprForgeContext, wrapper);
                        } else if (wrapper.getUnderlyingEventType() instanceof AvroSchemaEventType) {
                            return new EvalInsertNoWildcardSingleColCoercionAvroWrap(selectExprForgeContext, wrapper);
                        } else if (wrapper.getUnderlyingEventType() instanceof VariantEventType) {
                            VariantEventType variantEventType = (VariantEventType) wrapper.getUnderlyingEventType();
                            vaeProcessor = valueAddEventService.getValueAddProcessor(variantEventType.getName());
                            return new EvalInsertNoWildcardSingleColCoercionBeanWrapVariant(selectExprForgeContext, wrapper, vaeProcessor);
                        } else {
                            return new EvalInsertNoWildcardSingleColCoercionBeanWrap(selectExprForgeContext, wrapper);
                        }
                    } else {
                        if (resultEventType instanceof BeanEventType) {
                            return new EvalInsertNoWildcardSingleColCoercionBean(selectExprForgeContext, resultEventType);
                        }
                    }
                } else {
                    if (resultEventType instanceof BeanEventType) {
                        return new EvalInsertNoWildcardSingleColCoercionRevisionBean(selectExprForgeContext, resultEventType, vaeProcessor, vaeInnerEventType);
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
                        return new EvalInsertNoWildcardSingleColCoercionRevisionFunc(selectExprForgeContext, resultEventType, vaeProcessor, vaeInnerEventType, func);
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

                    SelectExprProcessorForge selectExprInsertEventBean = null;
                    if (existingType != null) {
                        selectExprInsertEventBean = SelectExprInsertEventBeanFactory.getInsertUnderlyingNonJoin(eventAdapterService, existingType, isUsingWildcard, typeService, exprForges, columnNames, expressionReturnTypes, engineImportService, insertIntoDesc, columnNamesAsProvided, false, statementName);
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
                    return new EvalInsertNoWildcardMap(selectExprForgeContext, resultEventType);
                } else if (resultEventType instanceof ObjectArrayEventType) {
                    return makeObjectArrayConsiderReorder(selectExprForgeContext, (ObjectArrayEventType) resultEventType, statementName, typeService.getEngineURIQualifier(), exprForges);
                } else if (resultEventType instanceof AvroSchemaEventType) {
                    return eventAdapterService.getEventAdapterAvroHandler().getOutputFactory().makeSelectNoWildcard(selectExprForgeContext, exprForges, resultEventType, tableService, statementName, typeService.getEngineURIQualifier());
                } else {
                    throw new IllegalStateException("Unrecognized output type " + resultEventType);
                }
            } else {
                return new EvalInsertNoWildcardRevision(selectExprForgeContext, resultEventType, vaeProcessor, vaeInnerEventType);
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
                if (ExprNodeUtilityCore.deepEquals(groupExpression, expr, false)) {
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

    private SelectExprProcessorForge makeObjectArrayConsiderReorder(SelectExprForgeContext selectExprForgeContext, ObjectArrayEventType resultEventType, String statementName, String engineURI, ExprForge[] exprForges)
            throws ExprValidationException {
        TypeWidener[] wideners = new TypeWidener[selectExprForgeContext.getColumnNames().length];
        int[] remapped = new int[selectExprForgeContext.getColumnNames().length];
        boolean needRemap = false;
        for (int i = 0; i < selectExprForgeContext.getColumnNames().length; i++) {
            String colName = selectExprForgeContext.getColumnNames()[i];
            int index = CollectionUtil.findItem(resultEventType.getPropertyNames(), colName);
            if (index == -1) {
                throw new ExprValidationException("Could not find property '" + colName + "' in " + getTypeNameConsiderTable(resultEventType, tableService));
            }
            remapped[i] = index;
            if (index != i) {
                needRemap = true;
            }

            ExprForge forge = exprForges[i];
            Class sourceColumnType;
            if (forge instanceof SelectExprProcessorTypableForge) {
                sourceColumnType = ((SelectExprProcessorTypableForge) forge).getUnderlyingEvaluationType();
            } else if (forge instanceof SelectExprProcessorEvalStreamInsertUnd) {
                sourceColumnType = ((SelectExprProcessorEvalStreamInsertUnd) forge).getUnderlyingReturnType();
            } else {
                sourceColumnType = forge.getEvaluationType();
            }

            Class targetPropType = resultEventType.getPropertyType(colName);
            try {
                wideners[i] = TypeWidenerFactory.getCheckPropertyAssignType(colName, sourceColumnType, targetPropType, colName, false, eventAdapterService.getTypeWidenerCustomizer(resultEventType), statementName, engineURI);
            } catch (TypeWidenerException ex) {
                throw new ExprValidationException(ex.getMessage(), ex);
            }
        }

        if (!needRemap) {
            return new EvalInsertNoWildcardObjectArray(selectExprForgeContext, resultEventType);
        }
        if (CollectionUtil.isAllNullArray(wideners)) {
            return new EvalInsertNoWildcardObjectArrayRemap(selectExprForgeContext, resultEventType, remapped);
        }
        return new EvalInsertNoWildcardObjectArrayRemapWWiden(selectExprForgeContext, resultEventType, remapped, wideners);
    }

    private String getTypeNameConsiderTable(ObjectArrayEventType resultEventType, TableService tableService) {
        TableMetadata metadata = tableService.getTableMetadataFromEventType(resultEventType);
        if (metadata != null) {
            return "table '" + metadata.getTableName() + "'";
        }
        return "type '" + resultEventType.getName() + "'";
    }

    private Pair<ExprForge, Object> handleUnderlyingStreamInsert(ExprForge exprEvaluator, NamedWindowMgmtService namedWindowMgmtService, final EventAdapterService eventAdapterService) {
        if (!(exprEvaluator instanceof ExprStreamUnderlyingNode)) {
            return null;
        }
        final ExprStreamUnderlyingNode undNode = (ExprStreamUnderlyingNode) exprEvaluator;
        final int streamNum = undNode.getStreamId();
        final Class returnType = undNode.getForge().getEvaluationType();
        final EventType namedWindowAsType = getNamedWindowUnderlyingType(namedWindowMgmtService, eventAdapterService, typeService.getEventTypes()[streamNum]);
        final TableMetadata tableMetadata = tableService.getTableMetadataFromEventType(typeService.getEventTypes()[streamNum]);

        EventType eventTypeStream;
        ExprForge forge;
        if (tableMetadata != null) {
            eventTypeStream = tableMetadata.getPublicEventType();
            forge = new SelectExprProcessorEvalStreamInsertTable(streamNum, undNode, tableMetadata, returnType);
        } else if (namedWindowAsType == null) {
            eventTypeStream = typeService.getEventTypes()[streamNum];
            forge = new SelectExprProcessorEvalStreamInsertUnd(undNode, streamNum, returnType);
        } else {
            eventTypeStream = namedWindowAsType;
            forge = new SelectExprProcessorEvalStreamInsertNamedWindow(streamNum, namedWindowAsType, returnType, eventAdapterService);
        }

        return new Pair<ExprForge, Object>(forge, eventTypeStream);
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

    private TypeAndForgePair handleTypableExpression(ExprForge forge, int expressionNum)
            throws ExprValidationException {
        if (!(forge instanceof ExprTypableReturnForge)) {
            return null;
        }

        ExprTypableReturnForge typable = (ExprTypableReturnForge) forge;
        LinkedHashMap<String, Object> eventTypeExpr = typable.getRowProperties();
        if (eventTypeExpr == null) {
            return null;
        }

        EventType mapType = eventAdapterService.createAnonymousMapType(statementId + "_innereval_" + CollectionUtil.toString(assignedTypeNumberStack, "_") + "_" + expressionNum, eventTypeExpr, true);
        ExprForge newForge = new SelectExprProcessorTypableMapForge(mapType, forge, eventAdapterService);
        return new TypeAndForgePair(mapType, newForge);
    }

    private TypeAndForgePair handleInsertIntoEnumeration(String insertIntoColName, EPType insertIntoTarget, ExprForge forge)
            throws ExprValidationException {
        if (!(forge instanceof ExprEnumerationForge) || insertIntoTarget == null
                || (!EPTypeHelper.isCarryEvent(insertIntoTarget))) {
            return null;
        }

        final ExprEnumerationForge enumeration = (ExprEnumerationForge) forge;
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
                SelectExprProcessorEnumerationCollForge enumerationCollForge = new SelectExprProcessorEnumerationCollForge(enumeration, targetType, false);
                return new TypeAndForgePair(new EventType[]{targetType}, enumerationCollForge);
            }
            SelectExprProcessorEnumerationSingleToCollForge singleToCollForge = new SelectExprProcessorEnumerationSingleToCollForge(enumeration, targetType);
            return new TypeAndForgePair(new EventType[]{targetType}, singleToCollForge);
        }

        // handle single-bean target
        // handle single-source
        if (eventTypeSingle != null) {
            SelectExprProcessorEnumerationAtBeanSingleForge singleForge = new SelectExprProcessorEnumerationAtBeanSingleForge(enumeration, targetType);
            return new TypeAndForgePair(targetType, singleForge);
        }

        SelectExprProcessorEnumerationCollForge enumerationCollForge = new SelectExprProcessorEnumerationCollForge(enumeration, targetType, true);
        return new TypeAndForgePair(targetType, enumerationCollForge);
    }

    private void checkTypeCompatible(String insertIntoCol, EventType targetType, EventType selectedType)
            throws ExprValidationException {
        if (!EventTypeUtility.isTypeOrSubTypeOf(targetType, selectedType)) {
            throw new ExprValidationException(
                    "Incompatible type detected attempting to insert into column '" +
                            insertIntoCol + "' type '" + targetType.getName() + "' compared to selected type '" + selectedType.getName() + "'");
        }
    }

    private TypeAndForgePair handleInsertIntoTypableExpression(EPType insertIntoTarget, ExprForge forge, EngineImportService engineImportService)
            throws ExprValidationException {
        if (!(forge instanceof ExprTypableReturnForge)
                || insertIntoTarget == null
                || (!EPTypeHelper.isCarryEvent(insertIntoTarget))) {
            return null;
        }

        final EventType targetType = EPTypeHelper.getEventType(insertIntoTarget);
        final ExprTypableReturnForge typable = (ExprTypableReturnForge) forge;
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
                try {
                    wideners[i] = TypeWidenerFactory.getCheckPropertyAssignType(provided.getKey(), (Class) provided.getValue(),
                            expected, written.get(i).getPropertyName(), false, typeWidenerCustomizer, statementName, typeService.getEngineURIQualifier());
                } catch (TypeWidenerException ex) {
                    throw new ExprValidationException(ex.getMessage(), ex);
                }
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
            SelectExprProcessorTypableMultiForge typableMultiForge = new SelectExprProcessorTypableMultiForge(typable, hasWideners, wideners, factory, targetType, false);
            return new TypeAndForgePair(new EventType[]{targetType}, typableMultiForge);
        } else if (insertIntoTarget instanceof EventMultiValuedEPType && !typable.isMultirow()) {
            SelectExprProcessorTypableSingleForge typableSingleForge = new SelectExprProcessorTypableSingleForge(typable, hasWideners, wideners, factory, targetType, false);
            return new TypeAndForgePair(new EventType[]{targetType}, typableSingleForge);
        } else if (insertIntoTarget instanceof EventEPType && !typable.isMultirow()) {
            SelectExprProcessorTypableSingleForge typableSingleForge = new SelectExprProcessorTypableSingleForge(typable, hasWideners, wideners, factory, targetType, true);
            return new TypeAndForgePair(targetType, typableSingleForge);
        }

        // we are discarding all but the first row
        SelectExprProcessorTypableMultiForge typableMultiForge = new SelectExprProcessorTypableMultiForge(typable, hasWideners, wideners, factory, targetType, true);
        return new TypeAndForgePair(targetType, typableMultiForge);
    }

    protected static void applyWideners(Object[] row, TypeWidener[] wideners) {
        for (int i = 0; i < wideners.length; i++) {
            if (wideners[i] != null) {
                row[i] = wideners[i].widen(row[i]);
            }
        }
    }

    protected static CodegenExpression applyWidenersCodegen(CodegenExpressionRef row, TypeWidener[] wideners, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        CodegenBlock block = codegenMethodScope.makeChild(void.class, SelectExprProcessorHelper.class, codegenClassScope).addParam(Object[].class, "row").getBlock();
        for (int i = 0; i < wideners.length; i++) {
            if (wideners[i] != null) {
                block.assignArrayElement("row", constant(i), wideners[i].widenCodegen(arrayAtIndex(ref("row"), constant(i)), codegenMethodScope, codegenClassScope));
            }
        }
        return localMethodBuild(block.methodEnd()).pass(row).call();
    }

    protected static void applyWideners(Object[][] rows, TypeWidener[] wideners) {
        for (Object[] row : rows) {
            applyWideners(row, wideners);
        }
    }

    protected static CodegenExpression applyWidenersCodegenMultirow(CodegenExpressionRef rows, TypeWidener[] wideners, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        CodegenMethodNode method = codegenMethodScope.makeChild(void.class, SelectExprProcessorHelper.class, codegenClassScope).addParam(Object[][].class, "rows").getBlock()
                .forEach(Object[].class, "row", rows)
                .expression(applyWidenersCodegen(ref("row"), wideners, codegenMethodScope, codegenClassScope))
                .blockEnd()
                .methodEnd();
        return localMethodBuild(method).pass(rows).call();
    }

    private TypeAndForgePair handleAtEventbeanEnumeration(boolean isEventBeans, ExprForge forge)
            throws ExprValidationException {
        if (!(forge instanceof ExprEnumerationForge) || !isEventBeans) {
            return null;
        }

        final ExprEnumerationForge enumEval = (ExprEnumerationForge) forge;
        final EventType eventTypeSingle = enumEval.getEventTypeSingle(eventAdapterService, statementId);
        if (eventTypeSingle != null) {
            final TableMetadata tableMetadata = tableService.getTableMetadataFromEventType(eventTypeSingle);
            if (tableMetadata == null) {
                SelectExprProcessorEnumerationAtBeanSingleForge beanForge = new SelectExprProcessorEnumerationAtBeanSingleForge(enumEval, eventTypeSingle);
                return new TypeAndForgePair(eventTypeSingle, beanForge);
            }
            throw new IllegalStateException("Unrecognized enumeration source returning table row-typed values");
        }

        final EventType eventTypeColl = enumEval.getEventTypeCollection(eventAdapterService, statementId);
        if (eventTypeColl != null) {
            final TableMetadata tableMetadata = tableService.getTableMetadataFromEventType(eventTypeColl);
            if (tableMetadata == null) {
                SelectExprProcessorEnumerationAtBeanCollForge collForge = new SelectExprProcessorEnumerationAtBeanCollForge(enumEval, eventTypeColl);
                return new TypeAndForgePair(new EventType[]{eventTypeColl}, collForge);
            }
            SelectExprProcessorEnumerationAtBeanCollTableForge tableForge = new SelectExprProcessorEnumerationAtBeanCollTableForge(enumEval, tableMetadata);
            return new TypeAndForgePair(new EventType[]{tableMetadata.getPublicEventType()}, tableForge);
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

    private static class TypeAndForgePair {
        private final Object type;
        private final ExprForge forge;

        private TypeAndForgePair(Object type, ExprForge forge) {
            this.type = type;
            this.forge = forge;
        }

        public Object getType() {
            return type;
        }

        public ExprForge getForge() {
            return forge;
        }
    }
}
