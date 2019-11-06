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
package com.espertech.esper.common.internal.epl.resultset.select.core;

import com.espertech.esper.common.client.EventPropertyDescriptor;
import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.client.FragmentEventType;
import com.espertech.esper.common.client.meta.EventTypeApplicationType;
import com.espertech.esper.common.client.meta.EventTypeIdPair;
import com.espertech.esper.common.client.meta.EventTypeMetadata;
import com.espertech.esper.common.client.meta.EventTypeTypeClass;
import com.espertech.esper.common.client.util.EventTypeBusModifier;
import com.espertech.esper.common.client.util.EventUnderlyingType;
import com.espertech.esper.common.client.util.NameAccessModifier;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenBlock;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionRef;
import com.espertech.esper.common.internal.collection.Pair;
import com.espertech.esper.common.internal.compile.stage1.spec.CreateSchemaDesc;
import com.espertech.esper.common.internal.compile.stage1.spec.InsertIntoDesc;
import com.espertech.esper.common.internal.compile.stage2.SelectClauseExprCompiledSpec;
import com.espertech.esper.common.internal.compile.stage2.StatementRawInfo;
import com.espertech.esper.common.internal.compile.stage3.StatementCompileTimeServices;
import com.espertech.esper.common.internal.compile.stage3.StmtClassForgeableFactory;
import com.espertech.esper.common.internal.epl.agg.core.AggregationGroupByRollupLevelForge;
import com.espertech.esper.common.internal.epl.expression.core.*;
import com.espertech.esper.common.internal.epl.expression.etc.*;
import com.espertech.esper.common.internal.epl.namedwindow.path.NamedWindowCompileTimeResolver;
import com.espertech.esper.common.internal.epl.namedwindow.path.NamedWindowMetaData;
import com.espertech.esper.common.internal.epl.resultset.core.GroupByRollupInfo;
import com.espertech.esper.common.internal.epl.resultset.select.eval.*;
import com.espertech.esper.common.internal.epl.resultset.select.typable.SelectExprProcessorTypableForge;
import com.espertech.esper.common.internal.epl.resultset.select.typable.SelectExprProcessorTypableMapForge;
import com.espertech.esper.common.internal.epl.resultset.select.typable.SelectExprProcessorTypableMultiForge;
import com.espertech.esper.common.internal.epl.resultset.select.typable.SelectExprProcessorTypableSingleForge;
import com.espertech.esper.common.internal.epl.streamtype.StreamTypeService;
import com.espertech.esper.common.internal.epl.table.compiletime.TableCompileTimeResolver;
import com.espertech.esper.common.internal.epl.table.compiletime.TableMetaData;
import com.espertech.esper.common.internal.event.arr.ObjectArrayEventType;
import com.espertech.esper.common.internal.event.avro.AvroSchemaEventType;
import com.espertech.esper.common.internal.event.bean.core.BeanEventType;
import com.espertech.esper.common.internal.event.bean.introspect.BeanEventTypeStem;
import com.espertech.esper.common.internal.event.bean.service.BeanEventTypeFactory;
import com.espertech.esper.common.internal.event.core.*;
import com.espertech.esper.common.internal.event.json.compiletime.JsonEventTypeUtility;
import com.espertech.esper.common.internal.event.json.core.JsonEventType;
import com.espertech.esper.common.internal.event.map.MapEventType;
import com.espertech.esper.common.internal.event.variant.VariantEventType;
import com.espertech.esper.common.internal.rettype.EPType;
import com.espertech.esper.common.internal.rettype.EPTypeHelper;
import com.espertech.esper.common.internal.rettype.EventMultiValuedEPType;
import com.espertech.esper.common.internal.settings.ClasspathImportException;
import com.espertech.esper.common.internal.settings.ClasspathImportServiceCompileTime;
import com.espertech.esper.common.internal.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.function.Function;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

/**
 * Processor for select-clause expressions that handles a list of selection items represented by
 * expression nodes. Computes results based on matching events.
 */
public class SelectExprProcessorHelper {
    private static final Logger log = LoggerFactory.getLogger(SelectExprProcessorHelper.class);

    private final List<SelectClauseExprCompiledSpec> selectionList;
    private final List<SelectExprStreamDesc> selectedStreams;
    private final SelectProcessorArgs args;
    private final InsertIntoDesc insertIntoDesc;

    public SelectExprProcessorHelper(List<SelectClauseExprCompiledSpec> selectionList, List<SelectExprStreamDesc> selectedStreams, SelectProcessorArgs args, InsertIntoDesc insertIntoDesc) {
        this.selectionList = selectionList;
        this.selectedStreams = selectedStreams;
        this.args = args;
        this.insertIntoDesc = insertIntoDesc;
    }

    public SelectExprProcessorWInsertTarget getForge() throws ExprValidationException {
        boolean isUsingWildcard = args.isUsingWildcard();
        StreamTypeService typeService = args.getTypeService();
        ClasspathImportServiceCompileTime classpathImportService = args.getClasspathImportService();
        BeanEventTypeFactory beanEventTypeFactoryProtected = args.getBeanEventTypeFactoryPrivate();
        EventTypeNameGeneratorStatement eventTypeNameGeneratorStatement = args.getCompileTimeServices().getEventTypeNameGeneratorStatement();
        String moduleName = args.getModuleName();
        List<StmtClassForgeableFactory> additionalForgeables = new ArrayList<>();

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
            SelectExprProcessorForgeWForgables pair = SelectExprJoinWildcardProcessorFactory.create(args, null, eventTypeName -> eventTypeName + "_join");
            joinWildcardProcessor = pair.getForge();
            additionalForgeables.addAll(pair.getAdditionalForgeables());
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
            if (args.getOptionalInsertIntoEventType() != null) {
                insertIntoTargetType = args.getOptionalInsertIntoEventType();
            } else {
                insertIntoTargetType = args.getEventTypeCompileTimeResolver().getTypeByName(insertIntoDesc.getEventTypeName());
                if (insertIntoTargetType == null) {
                    TableMetaData table = args.getTableCompileTimeResolver().resolve(insertIntoDesc.getEventTypeName());
                    if (table != null) {
                        insertIntoTargetType = table.getInternalEventType();
                        args.setOptionalInsertIntoEventType(insertIntoTargetType);
                    }
                }
            }
        }

        // Obtain insert-into per-column type information, when available
        EPTypesAndPropertyDescPair insertInfo = determineInsertedEventTypeTargets(insertIntoTargetType, selectionList, insertIntoDesc);
        EPType[] insertIntoTargetsPerCol = insertInfo.insertIntoTargetsPerCol;
        EventPropertyDescriptor[] insertIntoPropertyDescriptors = insertInfo.propertyDescriptors;

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
                TypeAndForgePair pair = handleInsertIntoEnumeration(spec.getProvidedName(), insertIntoTargetsPerCol[i], expr, forge);
                if (pair != null) {
                    expressionReturnTypes[i] = pair.getType();
                    exprForges[i] = pair.getForge();
                    continue;
                }

                // handle insert-into with well-defined target event-typed column, and typable expression
                pair = handleInsertIntoTypableExpression(insertIntoTargetsPerCol[i], forge, args);
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
            pair = handleTypableExpression(forge, i, eventTypeNameGeneratorStatement);
            if (pair != null) {
                expressionReturnTypes[i] = pair.getType();
                exprForges[i] = pair.getForge();
                continue;
            }

            // handle select-clause expressions that match group-by expressions with rollup and therefore should be boxed types as rollup can produce a null value
            if (args.getGroupByRollupInfo() != null && args.getGroupByRollupInfo().getRollupDesc() != null) {
                Class returnType = forge.getEvaluationType();
                Class returnTypeBoxed = JavaClassHelper.getBoxedType(returnType);
                if (returnType != returnTypeBoxed && isGroupByRollupNullableExpression(expr, args.getGroupByRollupInfo())) {
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
                exprForges[i] = new ExprEvalByGetter(streamNum, getter, returnType);
            } else if ((insertIntoTargetType != null) && expressionReturnTypes[i] instanceof Class &&
                (fragmentType.getFragmentType().getUnderlyingType() == ((Class) expressionReturnTypes[i]).getComponentType()) &&
                ((targetFragment == null) || (targetFragment != null && targetFragment.isNative()))) {
                // same for arrays: may need to unwrap the fragment if the target type has this underlying type
                EventPropertyGetterSPI getter = ((EventTypeSPI) eventTypeStream).getGetterSPI(propertyName);
                Class returnType = eventTypeStream.getPropertyType(propertyName);
                exprForges[i] = new ExprEvalByGetter(streamNum, getter, returnType);
            } else {
                EventPropertyGetterSPI getter = ((EventTypeSPI) eventTypeStream).getGetterSPI(propertyName);
                FragmentEventType fragType = eventTypeStream.getFragmentType(propertyName);
                Class undType = fragType.getFragmentType().getUnderlyingType();
                Class returnType = fragType.isIndexed() ? JavaClassHelper.getArrayType(undType) : undType;
                exprForges[i] = new ExprEvalByGetterFragment(streamNum, getter, returnType, fragmentType);
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
            Pair<ExprForge, Object> pair = handleUnderlyingStreamInsert(exprForges[i], insertIntoPropertyDescriptors[i], insertIntoTargetsPerCol[i]);
            if (pair != null) {
                exprForges[i] = pair.getFirst();
                expressionReturnTypes[i] = pair.getSecond();
            }
        }

        // Build event type that reflects all selected properties
        LinkedHashMap<String, Object> selPropertyTypes = new LinkedHashMap<>();
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
                            BeanEventTypeStem stem = args.getCompileTimeServices().getBeanEventTypeStemService().getCreateStem(propertyType, null);
                            NameAccessModifier visibility = getVisibility(propertyType.getName());
                            EventTypeMetadata metadata = new EventTypeMetadata(propertyType.getName(), moduleName, EventTypeTypeClass.STREAM, EventTypeApplicationType.CLASS, visibility, EventTypeBusModifier.NONBUS, false, EventTypeIdPair.unassigned());
                            underlyingEventType = new BeanEventType(stem, metadata, beanEventTypeFactoryProtected, null, null, null, null);
                            args.getEventTypeCompileTimeRegistry().newType(underlyingEventType);
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
                            underlyingEventType = allocateBeanTransposeUnderlyingType(returnType, moduleName, beanEventTypeFactoryProtected);
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
        SelectExprForgeContext selectExprForgeContext = new SelectExprForgeContext(exprForges, columnNames, null, typeService.getEventTypes(), args.getEventTypeAvroHandler());

        if (insertIntoDesc == null) {
            if (!selectedStreams.isEmpty()) {
                EventType resultEventType;
                String eventTypeName = eventTypeNameGeneratorStatement.getAnonymousTypeName();
                if (underlyingEventType != null) {
                    TableMetaData table = args.getTableCompileTimeResolver().resolveTableFromEventType(underlyingEventType);
                    if (table != null) {
                        underlyingEventType = table.getPublicEventType();
                    }

                    EventTypeMetadata metadata = new EventTypeMetadata(eventTypeName, moduleName, EventTypeTypeClass.STATEMENTOUT, EventTypeApplicationType.WRAPPER, NameAccessModifier.TRANSIENT, EventTypeBusModifier.NONBUS, false, EventTypeIdPair.unassigned());
                    resultEventType = WrapperEventTypeUtil.makeWrapper(metadata, underlyingEventType, selPropertyTypes, null, beanEventTypeFactoryProtected, args.getEventTypeCompileTimeResolver());
                    args.getEventTypeCompileTimeRegistry().newType(resultEventType);

                    SelectEvalStreamWUnderlying forge = new SelectEvalStreamWUnderlying(selectExprForgeContext, resultEventType, namedStreams, isUsingWildcard,
                        unnamedStreams, singleStreamWrapper, underlyingIsFragmentEvent, underlyingStreamNumber, underlyingPropertyEventGetter, underlyingExprForge, table, typeService.getEventTypes());
                    return new SelectExprProcessorWInsertTarget(forge, insertIntoTargetType, additionalForgeables);
                } else {
                    EventTypeMetadata metadata = new EventTypeMetadata(eventTypeName, moduleName, EventTypeTypeClass.STATEMENTOUT, EventTypeApplicationType.MAP, NameAccessModifier.TRANSIENT, EventTypeBusModifier.NONBUS, false, EventTypeIdPair.unassigned());
                    resultEventType = BaseNestableEventUtil.makeMapTypeCompileTime(metadata, selPropertyTypes, null, null, null, null, beanEventTypeFactoryProtected, args.getEventTypeCompileTimeResolver());
                    args.getEventTypeCompileTimeRegistry().newType(resultEventType);
                    SelectEvalStreamNoUnderlyingMap forge = new SelectEvalStreamNoUnderlyingMap(selectExprForgeContext, resultEventType, namedStreams, isUsingWildcard);
                    return new SelectExprProcessorWInsertTarget(forge, insertIntoTargetType, additionalForgeables);
                }
            }

            if (isUsingWildcard) {
                String eventTypeName = eventTypeNameGeneratorStatement.getAnonymousTypeName();
                EventTypeMetadata metadata = new EventTypeMetadata(eventTypeName, moduleName, EventTypeTypeClass.STATEMENTOUT, EventTypeApplicationType.WRAPPER, NameAccessModifier.TRANSIENT, EventTypeBusModifier.NONBUS, false, EventTypeIdPair.unassigned());
                EventType resultEventType = WrapperEventTypeUtil.makeWrapper(metadata, eventType, selPropertyTypes, null, beanEventTypeFactoryProtected, args.getEventTypeCompileTimeResolver());
                args.getEventTypeCompileTimeRegistry().newType(resultEventType);
                SelectExprProcessorForge forge;
                if (singleStreamWrapper) {
                    forge = new SelectEvalInsertWildcardSSWrapper(selectExprForgeContext, resultEventType);
                } else if (joinWildcardProcessor == null) {
                    forge = new SelectEvalWildcard(selectExprForgeContext, resultEventType);
                } else {
                    forge = new SelectEvalWildcardJoin(selectExprForgeContext, resultEventType, joinWildcardProcessor);
                }
                return new SelectExprProcessorWInsertTarget(forge, insertIntoTargetType, additionalForgeables);
            }

            EventType resultEventType;
            EventUnderlyingType representation = EventRepresentationUtil.getRepresentation(args.getAnnotations(), args.getConfiguration(), CreateSchemaDesc.AssignedType.NONE);
            String eventTypeName = eventTypeNameGeneratorStatement.getAnonymousTypeName();
            if (representation == EventUnderlyingType.OBJECTARRAY) {
                EventTypeMetadata metadata = new EventTypeMetadata(eventTypeName, moduleName, EventTypeTypeClass.STATEMENTOUT, EventTypeApplicationType.OBJECTARR, NameAccessModifier.TRANSIENT, EventTypeBusModifier.NONBUS, false, EventTypeIdPair.unassigned());
                resultEventType = BaseNestableEventUtil.makeOATypeCompileTime(metadata, selPropertyTypes, null, null, null, null, beanEventTypeFactoryProtected, args.getEventTypeCompileTimeResolver());
            } else if (representation == EventUnderlyingType.AVRO) {
                EventTypeMetadata metadata = new EventTypeMetadata(eventTypeName, moduleName, EventTypeTypeClass.STATEMENTOUT, EventTypeApplicationType.AVRO, NameAccessModifier.TRANSIENT, EventTypeBusModifier.NONBUS, false, EventTypeIdPair.unassigned());
                resultEventType = args.getEventTypeAvroHandler().newEventTypeFromNormalized(metadata, args.getEventTypeCompileTimeResolver(), args.getBeanEventTypeFactoryPrivate().getEventBeanTypedEventFactory(), selPropertyTypes, args.getAnnotations(), null, null, null, args.getStatementName());
            } else if (representation == EventUnderlyingType.JSON) {
                EventTypeMetadata metadata = new EventTypeMetadata(eventTypeName, moduleName, EventTypeTypeClass.STATEMENTOUT, EventTypeApplicationType.JSON, NameAccessModifier.TRANSIENT, EventTypeBusModifier.NONBUS, false, EventTypeIdPair.unassigned());
                EventTypeForgablesPair pair = JsonEventTypeUtility.makeJsonTypeCompileTimeNewType(metadata, selPropertyTypes, null, null, args.getStatementRawInfo(), args.getCompileTimeServices());
                resultEventType = pair.getEventType();
                additionalForgeables.addAll(pair.getAdditionalForgeables());
            } else {
                EventTypeMetadata metadata = new EventTypeMetadata(eventTypeName, moduleName, EventTypeTypeClass.STATEMENTOUT, EventTypeApplicationType.MAP, NameAccessModifier.TRANSIENT, EventTypeBusModifier.NONBUS, false, EventTypeIdPair.unassigned());
                Map<String, Object> propertyTypes = EventTypeUtility.getPropertyTypesNonPrimitive(selPropertyTypes);
                resultEventType = BaseNestableEventUtil.makeMapTypeCompileTime(metadata, propertyTypes, null, null, null, null, beanEventTypeFactoryProtected, args.getEventTypeCompileTimeResolver());
            }
            args.getEventTypeCompileTimeRegistry().newType(resultEventType);

            SelectExprProcessorForge forge;
            if (selectExprForgeContext.getExprForges().length == 0) {
                forge = new SelectEvalNoWildcardEmptyProps(selectExprForgeContext, resultEventType);
            } else {
                if (representation == EventUnderlyingType.OBJECTARRAY) {
                    forge = new SelectEvalNoWildcardObjectArray(selectExprForgeContext, resultEventType);
                } else if (representation == EventUnderlyingType.AVRO) {
                    forge = args.getCompileTimeServices().getEventTypeAvroHandler().getOutputFactory().makeSelectNoWildcard(selectExprForgeContext, exprForges, resultEventType, args.getTableCompileTimeResolver(), args.getStatementName());
                } else if (representation == EventUnderlyingType.JSON) {
                    forge = new SelectEvalNoWildcardJson(selectExprForgeContext, (JsonEventType) resultEventType);
                } else {
                    forge = new SelectEvalNoWildcardMap(selectExprForgeContext, resultEventType);
                }
            }
            return new SelectExprProcessorWInsertTarget(forge, insertIntoTargetType, additionalForgeables);
        }

        boolean singleColumnWrapOrBeanCoercion = false;       // Additional single-column coercion for non-wrapped type done by SelectExprInsertEventBeanFactory
        boolean isVariantEvent = false;

        try {
            if (!selectedStreams.isEmpty()) {
                EventType resultEventType;

                // handle "transpose" special function with predefined target type
                if (insertIntoTargetType != null && selectedStreams.get(0).getExpressionSelectedAsStream() != null) {
                    if (exprForges.length != 0) {
                        throw new ExprValidationException("Cannot transpose additional properties in the select-clause to target event type '" +
                            insertIntoTargetType.getName() +
                            "' with underlying type '" + insertIntoTargetType.getUnderlyingType().getName() + "', the " + ClasspathImportServiceCompileTime.EXT_SINGLEROW_FUNCTION_TRANSPOSE + " function must occur alone in the select clause");
                    }
                    ExprNode expression = unnamedStreams.get(0).getExpressionSelectedAsStream().getSelectExpression();
                    Class returnType = expression.getForge().getEvaluationType();
                    if (insertIntoTargetType instanceof ObjectArrayEventType && returnType == Object[].class) {
                        SelectExprProcessorForge forge = new SelectExprInsertEventBeanFactory.SelectExprInsertNativeExpressionCoerceObjectArray(insertIntoTargetType, expression.getForge());
                        return new SelectExprProcessorWInsertTarget(forge, insertIntoTargetType, additionalForgeables);
                    } else if (insertIntoTargetType instanceof MapEventType && JavaClassHelper.isImplementsInterface(returnType, Map.class)) {
                        SelectExprProcessorForge forge = new SelectExprInsertEventBeanFactory.SelectExprInsertNativeExpressionCoerceMap(insertIntoTargetType, expression.getForge());
                        return new SelectExprProcessorWInsertTarget(forge, insertIntoTargetType, additionalForgeables);
                    } else if (insertIntoTargetType instanceof BeanEventType && JavaClassHelper.isSubclassOrImplementsInterface(returnType, insertIntoTargetType.getUnderlyingType())) {
                        SelectExprProcessorForge forge = new SelectExprInsertEventBeanFactory.SelectExprInsertNativeExpressionCoerceNative(insertIntoTargetType, expression.getForge());
                        return new SelectExprProcessorWInsertTarget(forge, insertIntoTargetType, additionalForgeables);
                    } else if (insertIntoTargetType instanceof AvroSchemaEventType && returnType.getName().equals(JavaClassHelper.APACHE_AVRO_GENERIC_RECORD_CLASSNAME)) {
                        SelectExprProcessorForge forge = new SelectExprInsertEventBeanFactory.SelectExprInsertNativeExpressionCoerceAvro(insertIntoTargetType, expression.getForge());
                        return new SelectExprProcessorWInsertTarget(forge, insertIntoTargetType, additionalForgeables);
                    } else if (insertIntoTargetType instanceof JsonEventType && returnType == String.class) {
                        SelectExprProcessorForge forge = new SelectExprInsertEventBeanFactory.SelectExprInsertNativeExpressionCoerceJson(insertIntoTargetType, expression.getForge());
                        return new SelectExprProcessorWInsertTarget(forge, insertIntoTargetType, additionalForgeables);
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
                            SelectExprProcessorForge forge = new SelectEvalStreamWUnderlying(selectExprForgeContext, insertIntoTargetType, namedStreams, isUsingWildcard,
                                unnamedStreams, false, false, underlyingStreamNumber, null, evalExprForge, null, typeService.getEventTypes());
                            return new SelectExprProcessorWInsertTarget(forge, insertIntoTargetType, additionalForgeables);
                        }
                    }
                    throw SelectEvalInsertUtil.makeEventTypeCastException(returnType, insertIntoTargetType);
                }

                if (underlyingEventType != null) {
                    // a single stream was selected via "stream.*" and there is no column name
                    // recast as a Map-type
                    if (underlyingEventType instanceof MapEventType && insertIntoTargetType instanceof MapEventType) {
                        SelectExprProcessorForge forge = SelectEvalStreamWUndRecastMapFactory.make(typeService.getEventTypes(), selectExprForgeContext, selectedStreams.get(0).getStreamSelected().getStreamNumber(), insertIntoTargetType, exprNodes, classpathImportService, args.getStatementName());
                        return new SelectExprProcessorWInsertTarget(forge, insertIntoTargetType, additionalForgeables);
                    }

                    // recast as a Object-array-type
                    if (underlyingEventType instanceof ObjectArrayEventType && insertIntoTargetType instanceof ObjectArrayEventType) {
                        SelectExprProcessorForge forge = SelectEvalStreamWUndRecastObjectArrayFactory.make(typeService.getEventTypes(), selectExprForgeContext, selectedStreams.get(0).getStreamSelected().getStreamNumber(), insertIntoTargetType, exprNodes, classpathImportService, args.getStatementName());
                        return new SelectExprProcessorWInsertTarget(forge, insertIntoTargetType, additionalForgeables);
                    }

                    // recast as a Avro-type
                    if (underlyingEventType instanceof AvroSchemaEventType && insertIntoTargetType instanceof AvroSchemaEventType) {
                        SelectExprProcessorForge forge = args.getEventTypeAvroHandler().getOutputFactory().makeRecast(typeService.getEventTypes(), selectExprForgeContext, selectedStreams.get(0).getStreamSelected().getStreamNumber(), (AvroSchemaEventType) insertIntoTargetType, exprNodes, args.getStatementName());
                        return new SelectExprProcessorWInsertTarget(forge, insertIntoTargetType, additionalForgeables);
                    }

                    // recast as a Bean-type
                    if (underlyingEventType instanceof BeanEventType && insertIntoTargetType instanceof BeanEventType) {
                        SelectExprProcessorForge forge = new SelectEvalInsertBeanRecast(insertIntoTargetType, selectedStreams.get(0).getStreamSelected().getStreamNumber(), typeService.getEventTypes());
                        return new SelectExprProcessorWInsertTarget(forge, insertIntoTargetType, additionalForgeables);
                    }

                    if (underlyingEventType instanceof JsonEventType && insertIntoTargetType instanceof JsonEventType) {
                        SelectExprProcessorForge forge = SelectEvalStreamWUndRecastJsonFactory.make(typeService.getEventTypes(), selectExprForgeContext, selectedStreams.get(0).getStreamSelected().getStreamNumber(), insertIntoTargetType, exprNodes, classpathImportService, args.getStatementName());
                        return new SelectExprProcessorWInsertTarget(forge, insertIntoTargetType, additionalForgeables);
                    }

                    // wrap if no recast possible
                    TableMetaData table = args.getTableCompileTimeResolver().resolveTableFromEventType(underlyingEventType);
                    if (table != null) {
                        underlyingEventType = table.getPublicEventType();
                    }

                    if (insertIntoTargetType == null || (!(insertIntoTargetType instanceof WrapperEventType))) {
                        NameAccessModifier visibility = getVisibility(insertIntoDesc.getEventTypeName());
                        EventTypeMetadata metadata = new EventTypeMetadata(insertIntoDesc.getEventTypeName(), moduleName, EventTypeTypeClass.STREAM, EventTypeApplicationType.WRAPPER, visibility, EventTypeBusModifier.NONBUS, false, EventTypeIdPair.unassigned());
                        resultEventType = WrapperEventTypeUtil.makeWrapper(metadata, underlyingEventType, selPropertyTypes, null, beanEventTypeFactoryProtected, args.getEventTypeCompileTimeResolver());
                        args.getEventTypeCompileTimeRegistry().newType(resultEventType);
                    } else {
                        resultEventType = insertIntoTargetType;
                    }

                    SelectEvalStreamWUnderlying forge = new SelectEvalStreamWUnderlying(selectExprForgeContext, resultEventType, namedStreams, isUsingWildcard,
                        unnamedStreams, singleStreamWrapper, underlyingIsFragmentEvent, underlyingStreamNumber, underlyingPropertyEventGetter, underlyingExprForge, table, typeService.getEventTypes());
                    return new SelectExprProcessorWInsertTarget(forge, insertIntoTargetType, additionalForgeables);
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
                        NameAccessModifier visibility = getVisibility(insertIntoDesc.getEventTypeName());
                        EventTypeMetadata metadata = new EventTypeMetadata(insertIntoDesc.getEventTypeName(), moduleName, EventTypeTypeClass.STREAM, EventTypeApplicationType.MAP, visibility, EventTypeBusModifier.NONBUS, false, EventTypeIdPair.unassigned());
                        Map<String, Object> propertyTypes = EventTypeUtility.getPropertyTypesNonPrimitive(selPropertyTypes);
                        MapEventType proposed = BaseNestableEventUtil.makeMapTypeCompileTime(metadata, propertyTypes, null, null, null, null, args.getBeanEventTypeFactoryPrivate(), args.getEventTypeCompileTimeResolver());
                        if (insertIntoTargetType != null) {
                            EventTypeUtility.compareExistingType(proposed, insertIntoTargetType);
                        } else {
                            insertIntoTargetType = proposed;
                            args.getEventTypeCompileTimeRegistry().newType(proposed);
                        }
                        Set<String> propertiesToUnwrap = getEventBeanToObjectProps(selPropertyTypes, insertIntoTargetType);

                        SelectExprProcessorForge forge;
                        if (propertiesToUnwrap.isEmpty()) {
                            forge = new SelectEvalStreamNoUnderlyingMap(selectExprForgeContext, insertIntoTargetType, namedStreams, isUsingWildcard);
                        } else {
                            forge = new SelectEvalStreamNoUndWEventBeanToObj(selectExprForgeContext, insertIntoTargetType, namedStreams, isUsingWildcard, propertiesToUnwrap);
                        }
                        return new SelectExprProcessorWInsertTarget(forge, insertIntoTargetType, additionalForgeables);
                    } else if (insertIntoTargetType instanceof ObjectArrayEventType) {
                        Set<String> propertiesToUnwrap = getEventBeanToObjectProps(selPropertyTypes, insertIntoTargetType);
                        SelectExprProcessorForge forge;
                        if (propertiesToUnwrap.isEmpty()) {
                            forge = new SelectEvalStreamNoUnderlyingObjectArray(selectExprForgeContext, insertIntoTargetType, namedStreams, isUsingWildcard);
                        } else {
                            forge = new SelectEvalStreamNoUndWEventBeanToObjObjArray(selectExprForgeContext, insertIntoTargetType, namedStreams, isUsingWildcard, propertiesToUnwrap);
                        }
                        return new SelectExprProcessorWInsertTarget(forge, insertIntoTargetType, additionalForgeables);
                    } else if (insertIntoTargetType instanceof AvroSchemaEventType) {
                        throw new ExprValidationException("Avro event type does not allow contained beans");
                    } else {
                        throw new IllegalStateException("Unrecognized event type " + insertIntoTargetType);
                    }
                }
            }

            VariantEventType variantEventType = null;
            if (insertIntoTargetType instanceof VariantEventType) {
                variantEventType = (VariantEventType) insertIntoTargetType;
                isVariantEvent = true;
                variantEventType.validateInsertedIntoEventType(eventType);
            }

            EventType resultEventType;
            if (isUsingWildcard) {
                if (variantEventType != null) {
                    resultEventType = variantEventType;
                } else {
                    if (insertIntoTargetType != null) {

                        // handle insert-into with fast coercion (no additional properties selected)
                        if (selPropertyTypes.isEmpty()) {
                            if (insertIntoTargetType instanceof BeanEventType && eventType instanceof BeanEventType) {
                                SelectExprProcessorForge forge = new SelectEvalInsertBeanRecast(insertIntoTargetType, 0, typeService.getEventTypes());
                                return new SelectExprProcessorWInsertTarget(forge, insertIntoTargetType, additionalForgeables);
                            }
                            if (insertIntoTargetType instanceof ObjectArrayEventType && eventType instanceof ObjectArrayEventType) {
                                ObjectArrayEventType target = (ObjectArrayEventType) insertIntoTargetType;
                                ObjectArrayEventType source = (ObjectArrayEventType) eventType;
                                ExprValidationException msg = BaseNestableEventType.isDeepEqualsProperties(eventType.getName(), source.getTypes(), target.getTypes());
                                if (msg == null) {
                                    SelectExprProcessorForge forge = new SelectEvalInsertCoercionObjectArray(insertIntoTargetType);
                                    return new SelectExprProcessorWInsertTarget(forge, insertIntoTargetType, additionalForgeables);
                                }
                            }
                            if (insertIntoTargetType instanceof MapEventType && eventType instanceof MapEventType) {
                                SelectExprProcessorForge forge = new SelectEvalInsertCoercionMap(insertIntoTargetType);
                                return new SelectExprProcessorWInsertTarget(forge, insertIntoTargetType, additionalForgeables);
                            }
                            if (insertIntoTargetType instanceof AvroSchemaEventType && eventType instanceof AvroSchemaEventType) {
                                SelectExprProcessorForge forge = new SelectEvalInsertCoercionAvro(insertIntoTargetType);
                                return new SelectExprProcessorWInsertTarget(forge, insertIntoTargetType, additionalForgeables);
                            }
                            if (insertIntoTargetType instanceof JsonEventType && eventType instanceof JsonEventType) {
                                JsonEventType source = (JsonEventType) eventType;
                                JsonEventType target = (JsonEventType) insertIntoTargetType;
                                ExprValidationException msg = BaseNestableEventType.isDeepEqualsProperties(eventType.getName(), source.getTypes(), target.getTypes());
                                if (msg == null) {
                                    SelectExprProcessorForge forge = new SelectEvalInsertCoercionJson(source, target);
                                    return new SelectExprProcessorWInsertTarget(forge, insertIntoTargetType, additionalForgeables);
                                }
                            }
                            if (insertIntoTargetType instanceof WrapperEventType && eventType instanceof BeanEventType) {
                                WrapperEventType wrapperType = (WrapperEventType) insertIntoTargetType;
                                if (wrapperType.getUnderlyingEventType() instanceof BeanEventType) {
                                    SelectExprProcessorForge forge = new SelectEvalInsertBeanWrapRecast(wrapperType, 0, typeService.getEventTypes());
                                    return new SelectExprProcessorWInsertTarget(forge, insertIntoTargetType, additionalForgeables);
                                }
                            }
                            if (insertIntoTargetType instanceof WrapperEventType) {
                                WrapperEventType wrapperEventType = (WrapperEventType) insertIntoTargetType;
                                if (EventTypeUtility.isTypeOrSubTypeOf(eventType, wrapperEventType.getUnderlyingEventType())) {
                                    SelectExprProcessorForge forge = new SelectEvalInsertWildcardWrapper(selectExprForgeContext, insertIntoTargetType);
                                    return new SelectExprProcessorWInsertTarget(forge, insertIntoTargetType, additionalForgeables);
                                }
                                if (wrapperEventType.getUnderlyingEventType() instanceof WrapperEventType) {
                                    WrapperEventType nestedWrapper = (WrapperEventType) wrapperEventType.getUnderlyingEventType();
                                    if (EventTypeUtility.isTypeOrSubTypeOf(eventType, nestedWrapper.getUnderlyingEventType())) {
                                        SelectExprProcessorForge forge = new SelectEvalInsertWildcardWrapperNested(selectExprForgeContext, insertIntoTargetType, nestedWrapper);
                                        return new SelectExprProcessorWInsertTarget(forge, insertIntoTargetType, additionalForgeables);
                                    }
                                }
                            }
                        }

                        // handle insert-into by generating the writer with possible additional properties
                        SelectExprProcessorForge existingTypeProcessor = SelectExprInsertEventBeanFactory.getInsertUnderlyingNonJoin(insertIntoTargetType, isUsingWildcard, typeService, exprForges, columnNames, expressionReturnTypes, insertIntoDesc, columnNamesAsProvided, true, args.getStatementName(), args.getClasspathImportService(), args.getEventTypeAvroHandler());
                        if (existingTypeProcessor != null) {
                            return new SelectExprProcessorWInsertTarget(existingTypeProcessor, insertIntoTargetType, additionalForgeables);
                        }
                    }

                    NameAccessModifier visibility = getVisibility(insertIntoDesc.getEventTypeName());
                    if (selPropertyTypes.isEmpty() && eventType instanceof BeanEventType) {
                        BeanEventType beanEventType = (BeanEventType) eventType;
                        EventTypeMetadata metadata = new EventTypeMetadata(insertIntoDesc.getEventTypeName(), moduleName, EventTypeTypeClass.STREAM, EventTypeApplicationType.CLASS, visibility, EventTypeBusModifier.NONBUS, false, EventTypeIdPair.unassigned());
                        BeanEventType newBeanType = new BeanEventType(beanEventType.getStem(), metadata, beanEventTypeFactoryProtected, null, null, null, null);
                        resultEventType = newBeanType;
                        if (insertIntoTargetType != null) {
                            EventTypeUtility.compareExistingType(insertIntoTargetType, newBeanType);
                        } else {
                            args.getEventTypeCompileTimeRegistry().newType(resultEventType);
                        }
                    } else {
                        EventTypeMetadata metadata = new EventTypeMetadata(insertIntoDesc.getEventTypeName(), moduleName, EventTypeTypeClass.STREAM, EventTypeApplicationType.WRAPPER, visibility, EventTypeBusModifier.NONBUS, false, EventTypeIdPair.unassigned());
                        WrapperEventType wrapperEventType = WrapperEventTypeUtil.makeWrapper(metadata, eventType, selPropertyTypes, null, beanEventTypeFactoryProtected, args.getEventTypeCompileTimeResolver());
                        resultEventType = wrapperEventType;
                        if (insertIntoTargetType != null) {
                            EventTypeUtility.compareExistingType(insertIntoTargetType, wrapperEventType);
                        } else {
                            args.getEventTypeCompileTimeRegistry().newType(resultEventType);
                        }
                    }
                }

                if (singleStreamWrapper) {
                    if (!isVariantEvent) {
                        SelectExprProcessorForge forge = new SelectEvalInsertWildcardSSWrapper(selectExprForgeContext, resultEventType);
                        return new SelectExprProcessorWInsertTarget(forge, insertIntoTargetType, additionalForgeables);
                    } else {
                        SelectExprProcessorForge forge = new SelectEvalInsertWildcardSSWrapperRevision(selectExprForgeContext, resultEventType, variantEventType);
                        return new SelectExprProcessorWInsertTarget(forge, insertIntoTargetType, additionalForgeables);
                    }
                }
                if (joinWildcardProcessor == null) {
                    if (!isVariantEvent) {
                        if (resultEventType instanceof WrapperEventType) {
                            SelectExprProcessorForge forge = new SelectEvalInsertWildcardWrapper(selectExprForgeContext, resultEventType);
                            return new SelectExprProcessorWInsertTarget(forge, insertIntoTargetType, additionalForgeables);
                        } else {
                            SelectExprProcessorForge forge = new SelectEvalInsertWildcardBean(selectExprForgeContext, resultEventType);
                            return new SelectExprProcessorWInsertTarget(forge, insertIntoTargetType, additionalForgeables);
                        }
                    } else {
                        if (exprForges.length == 0) {
                            SelectExprProcessorForge forge = new SelectEvalInsertWildcardVariant(selectExprForgeContext, resultEventType, variantEventType);
                            return new SelectExprProcessorWInsertTarget(forge, insertIntoTargetType, additionalForgeables);
                        } else {
                            String eventTypeName = eventTypeNameGeneratorStatement.getAnonymousTypeName();
                            EventTypeMetadata metadata = new EventTypeMetadata(eventTypeName, moduleName, EventTypeTypeClass.STATEMENTOUT, EventTypeApplicationType.WRAPPER, NameAccessModifier.TRANSIENT, EventTypeBusModifier.NONBUS, false, EventTypeIdPair.unassigned());
                            resultEventType = WrapperEventTypeUtil.makeWrapper(metadata, eventType, selPropertyTypes, null, beanEventTypeFactoryProtected, args.getEventTypeCompileTimeResolver());
                            args.getEventTypeCompileTimeRegistry().newType(resultEventType);
                            SelectExprProcessorForge forge = new SelectEvalInsertWildcardVariantWrapper(selectExprForgeContext, resultEventType, variantEventType, resultEventType);
                            return new SelectExprProcessorWInsertTarget(forge, insertIntoTargetType, additionalForgeables);
                        }
                    }
                } else {
                    SelectExprProcessorForge forge;
                    if (!isVariantEvent) {
                        forge = new SelectEvalInsertWildcardJoin(selectExprForgeContext, resultEventType, joinWildcardProcessor);
                    } else {
                        forge = new SelectEvalInsertWildcardJoinVariant(selectExprForgeContext, resultEventType, joinWildcardProcessor, variantEventType);
                    }
                    return new SelectExprProcessorWInsertTarget(forge, insertIntoTargetType, additionalForgeables);
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
                        if ((wrapperType.getUnderlyingEventType().getUnderlyingType() == columnOneType) ||
                            (wrapperType.getUnderlyingEventType() instanceof JsonEventType && columnOneType == String.class)) {
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
                if (!isVariantEvent) {
                    if (resultEventType instanceof WrapperEventType) {
                        WrapperEventType wrapper = (WrapperEventType) resultEventType;
                        if (wrapper.getUnderlyingEventType() instanceof MapEventType) {
                            SelectExprProcessorForge forge = new SelectEvalInsertNoWildcardSingleColCoercionMapWrap(selectExprForgeContext, wrapper);
                            return new SelectExprProcessorWInsertTarget(forge, insertIntoTargetType, additionalForgeables);
                        } else if (wrapper.getUnderlyingEventType() instanceof ObjectArrayEventType) {
                            SelectExprProcessorForge forge = new SelectEvalInsertNoWildcardSingleColCoercionObjectArrayWrap(selectExprForgeContext, wrapper);
                            return new SelectExprProcessorWInsertTarget(forge, insertIntoTargetType, additionalForgeables);
                        } else if (wrapper.getUnderlyingEventType() instanceof JsonEventType) {
                            SelectExprProcessorForge forge = new SelectEvalInsertNoWildcardSingleColCoercionJsonWrap(selectExprForgeContext, wrapper);
                            return new SelectExprProcessorWInsertTarget(forge, insertIntoTargetType, additionalForgeables);
                        } else if (wrapper.getUnderlyingEventType() instanceof AvroSchemaEventType) {
                            SelectExprProcessorForge forge = new SelectEvalInsertNoWildcardSingleColCoercionAvroWrap(selectExprForgeContext, wrapper);
                            return new SelectExprProcessorWInsertTarget(forge, insertIntoTargetType, additionalForgeables);
                        } else if (wrapper.getUnderlyingEventType() instanceof VariantEventType) {
                            variantEventType = (VariantEventType) wrapper.getUnderlyingEventType();
                            SelectExprProcessorForge forge = new SelectEvalInsertNoWildcardSingleColCoercionBeanWrapVariant(selectExprForgeContext, wrapper, variantEventType);
                            return new SelectExprProcessorWInsertTarget(forge, insertIntoTargetType, additionalForgeables);
                        } else {
                            SelectExprProcessorForge forge = new SelectEvalInsertNoWildcardSingleColCoercionBeanWrap(selectExprForgeContext, wrapper);
                            return new SelectExprProcessorWInsertTarget(forge, insertIntoTargetType, additionalForgeables);
                        }
                    } else {
                        if (resultEventType instanceof BeanEventType) {
                            SelectExprProcessorForge forge = new SelectEvalInsertNoWildcardSingleColCoercionBean(selectExprForgeContext, resultEventType);
                            return new SelectExprProcessorWInsertTarget(forge, insertIntoTargetType, additionalForgeables);
                        }
                    }
                } else {
                    throw new UnsupportedOperationException("Single-column wrap conversion to variant type is not supported");
                }
            }
            if (resultEventType == null) {
                if (variantEventType != null) {
                    String eventTypeName = eventTypeNameGeneratorStatement.getAnonymousTypeName();
                    EventTypeMetadata metadata = new EventTypeMetadata(eventTypeName, moduleName, EventTypeTypeClass.STREAM, EventTypeApplicationType.MAP, NameAccessModifier.TRANSIENT, EventTypeBusModifier.NONBUS, false, EventTypeIdPair.unassigned());
                    resultEventType = BaseNestableEventUtil.makeMapTypeCompileTime(metadata, selPropertyTypes, null, null, null, null, args.getBeanEventTypeFactoryPrivate(), args.getEventTypeCompileTimeResolver());
                    args.getEventTypeCompileTimeRegistry().newType(resultEventType);
                } else {
                    EventType existingType = insertIntoTargetType;
                    if (existingType == null) {
                        // The type may however be an auto-import or fully-qualified class name
                        Class clazz = null;
                        try {
                            clazz = classpathImportService.resolveClass(insertIntoDesc.getEventTypeName(), false);
                        } catch (ClasspathImportException e) {
                            log.debug("Target stream name '" + insertIntoDesc.getEventTypeName() + "' is not resolved as a class name");
                        }
                        if (clazz != null) {
                            NameAccessModifier nameVisibility = getVisibility(insertIntoDesc.getEventTypeName());
                            BeanEventTypeStem stem = args.getCompileTimeServices().getBeanEventTypeStemService().getCreateStem(clazz, null);
                            EventTypeMetadata metadata = new EventTypeMetadata(insertIntoDesc.getEventTypeName(), moduleName, EventTypeTypeClass.STREAM, EventTypeApplicationType.CLASS, nameVisibility, EventTypeBusModifier.NONBUS, false, EventTypeIdPair.unassigned());
                            existingType = new BeanEventType(stem, metadata, beanEventTypeFactoryProtected, null, null, null, null);
                            args.getEventTypeCompileTimeRegistry().newType(existingType);
                        }
                    }

                    SelectExprProcessorForge selectExprInsertEventBean = null;
                    if (existingType != null) {
                        selectExprInsertEventBean = SelectExprInsertEventBeanFactory.getInsertUnderlyingNonJoin(existingType, isUsingWildcard, typeService, exprForges, columnNames, expressionReturnTypes, insertIntoDesc, columnNamesAsProvided, false, args.getStatementName(), args.getClasspathImportService(), args.getEventTypeAvroHandler());
                    }
                    if (selectExprInsertEventBean != null) {
                        return new SelectExprProcessorWInsertTarget(selectExprInsertEventBean, insertIntoTargetType, additionalForgeables);
                    } else {
                        // use the provided override-type if there is one
                        if (args.getOptionalInsertIntoEventType() != null) {
                            resultEventType = insertIntoTargetType;
                        } else if (existingType instanceof AvroSchemaEventType) {
                            args.getEventTypeAvroHandler().avroCompat(existingType, selPropertyTypes);
                            resultEventType = existingType;
                        } else {
                            NameAccessModifier visibility = getVisibility(insertIntoDesc.getEventTypeName());
                            EventUnderlyingType out = EventRepresentationUtil.getRepresentation(args.getAnnotations(), args.getConfiguration(), CreateSchemaDesc.AssignedType.NONE);
                            Function<EventTypeApplicationType, EventTypeMetadata> metadata = appType -> new EventTypeMetadata(insertIntoDesc.getEventTypeName(), moduleName, EventTypeTypeClass.STREAM, appType, visibility, EventTypeBusModifier.NONBUS, false, EventTypeIdPair.unassigned());
                            Map<String, Object> propertyTypes = EventTypeUtility.getPropertyTypesNonPrimitive(selPropertyTypes);
                            if (out == EventUnderlyingType.MAP) {
                                MapEventType proposed = BaseNestableEventUtil.makeMapTypeCompileTime(metadata.apply(EventTypeApplicationType.MAP), propertyTypes, null, null, null, null, args.getBeanEventTypeFactoryPrivate(), args.getEventTypeCompileTimeResolver());
                                if (insertIntoTargetType != null) {
                                    EventTypeUtility.compareExistingType(proposed, insertIntoTargetType);
                                    resultEventType = insertIntoTargetType;
                                } else {
                                    args.getEventTypeCompileTimeRegistry().newType(proposed);
                                    resultEventType = proposed;
                                }
                            } else if (out == EventUnderlyingType.OBJECTARRAY) {
                                ObjectArrayEventType proposed = BaseNestableEventUtil.makeOATypeCompileTime(metadata.apply(EventTypeApplicationType.OBJECTARR), propertyTypes, null, null, null, null, args.getBeanEventTypeFactoryPrivate(), args.getEventTypeCompileTimeResolver());
                                if (insertIntoTargetType != null) {
                                    EventTypeUtility.compareExistingType(proposed, insertIntoTargetType);
                                    resultEventType = insertIntoTargetType;
                                } else {
                                    args.getEventTypeCompileTimeRegistry().newType(proposed);
                                    resultEventType = proposed;
                                }
                            } else if (out == EventUnderlyingType.JSON) {
                                EventTypeForgablesPair pair = JsonEventTypeUtility.makeJsonTypeCompileTimeNewType(metadata.apply(EventTypeApplicationType.JSON), propertyTypes, null, null, args.getStatementRawInfo(), args.getCompileTimeServices());
                                EventType proposed = pair.getEventType();
                                if (insertIntoTargetType != null) {
                                    EventTypeUtility.compareExistingType(proposed, insertIntoTargetType);
                                    resultEventType = insertIntoTargetType;
                                } else {
                                    args.getEventTypeCompileTimeRegistry().newType(proposed);
                                    resultEventType = proposed;
                                    additionalForgeables.addAll(pair.getAdditionalForgeables());
                                }
                            } else if (out == EventUnderlyingType.AVRO) {
                                AvroSchemaEventType proposed = args.getEventTypeAvroHandler().newEventTypeFromNormalized(metadata.apply(EventTypeApplicationType.AVRO), null, args.getBeanEventTypeFactoryPrivate().getEventBeanTypedEventFactory(), propertyTypes, args.getAnnotations(), null, null, null, args.getStatementName());
                                if (insertIntoTargetType != null) {
                                    EventTypeUtility.compareExistingType(proposed, insertIntoTargetType);
                                    resultEventType = insertIntoTargetType;
                                } else {
                                    args.getEventTypeCompileTimeRegistry().newType(proposed);
                                    resultEventType = proposed;
                                }
                            } else {
                                throw new IllegalStateException("Unrecognized code " + out);
                            }
                        }
                    }
                }
            }

            if (variantEventType != null) {
                variantEventType.validateInsertedIntoEventType(resultEventType);
                isVariantEvent = true;
            }

            SelectExprProcessorForge forge;
            if (!isVariantEvent) {
                if (resultEventType instanceof MapEventType) {
                    forge = new SelectEvalNoWildcardMap(selectExprForgeContext, resultEventType);
                } else if (resultEventType instanceof ObjectArrayEventType) {
                    forge = makeObjectArrayConsiderReorder(selectExprForgeContext, (ObjectArrayEventType) resultEventType, exprForges, args.getStatementRawInfo(), args.getCompileTimeServices());
                } else if (resultEventType instanceof AvroSchemaEventType) {
                    forge = args.getEventTypeAvroHandler().getOutputFactory().makeSelectNoWildcard(selectExprForgeContext, exprForges, resultEventType, args.getTableCompileTimeResolver(), args.getStatementName());
                } else if (resultEventType instanceof JsonEventType) {
                    forge = new SelectEvalNoWildcardJson(selectExprForgeContext, (JsonEventType) resultEventType);
                } else {
                    throw new IllegalStateException("Unrecognized output type " + resultEventType);
                }
            } else {
                forge = new SelectEvalInsertNoWildcardVariant(selectExprForgeContext, resultEventType, variantEventType, resultEventType);
            }
            return new SelectExprProcessorWInsertTarget(forge, insertIntoTargetType, additionalForgeables);
        } catch (EventAdapterException ex) {
            log.debug("Exception provided by event adapter: " + ex.getMessage(), ex);
            throw new ExprValidationException(ex.getMessage(), ex);
        }
    }

    private EventType allocateBeanTransposeUnderlyingType(Class returnType, String moduleName, BeanEventTypeFactory beanEventTypeFactoryProtected) {
        // check if the module has already registered the same bean type.
        // since private bean-types are registered by fully-qualified class name this prevents name-duplicate.
        for (EventType eventType : args.getEventTypeCompileTimeRegistry().getNewTypesAdded()) {
            if (!(eventType instanceof BeanEventType)) {
                continue;
            }
            BeanEventType beanEventType = (BeanEventType) eventType;
            if (beanEventType.getUnderlyingType() == returnType) {
                return beanEventType;
            }
        }

        // the bean-type have not been allocated
        BeanEventTypeStem stem = args.getCompileTimeServices().getBeanEventTypeStemService().getCreateStem(returnType, null);
        NameAccessModifier visibility = getVisibility(returnType.getName());
        EventTypeMetadata metadata = new EventTypeMetadata(returnType.getName(), moduleName, EventTypeTypeClass.STREAM, EventTypeApplicationType.CLASS, visibility, EventTypeBusModifier.NONBUS, false, EventTypeIdPair.unassigned());
        BeanEventType type = new BeanEventType(stem, metadata, beanEventTypeFactoryProtected, null, null, null, null);
        args.getEventTypeCompileTimeRegistry().newType(type);
        return type;
    }

    private boolean isGroupByRollupNullableExpression(ExprNode expr, GroupByRollupInfo groupByRollupInfo) {
        // if all levels include this key, we are fine
        for (AggregationGroupByRollupLevelForge level : groupByRollupInfo.getRollupDesc().getLevels()) {
            if (level.isAggregationTop()) {
                return true;
            }
            boolean found = false;
            for (int rollupKeyIndex : level.getRollupKeys()) {
                ExprNode groupExpression = groupByRollupInfo.getExprNodes()[rollupKeyIndex];
                if (ExprNodeUtilityCompare.deepEquals(groupExpression, expr, false)) {
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

    private SelectExprProcessorForge makeObjectArrayConsiderReorder(SelectExprForgeContext selectExprForgeContext, ObjectArrayEventType resultEventType, ExprForge[] exprForges, StatementRawInfo statementRawInfo, StatementCompileTimeServices compileTimeServices)
        throws ExprValidationException {
        TypeWidenerSPI[] wideners = new TypeWidenerSPI[selectExprForgeContext.getColumnNames().length];
        int[] remapped = new int[selectExprForgeContext.getColumnNames().length];
        boolean needRemap = false;
        for (int i = 0; i < selectExprForgeContext.getColumnNames().length; i++) {
            String colName = selectExprForgeContext.getColumnNames()[i];
            int index = CollectionUtil.findItem(resultEventType.getPropertyNames(), colName);
            if (index == -1) {
                throw new ExprValidationException("Could not find property '" + colName + "' in " + getTypeNameConsiderTable(resultEventType, compileTimeServices.getTableCompileTimeResolver()));
            }
            remapped[i] = index;
            if (index != i) {
                needRemap = true;
            }

            ExprForge forge = exprForges[i];
            Class sourceColumnType;
            if (forge instanceof SelectExprProcessorTypableForge) {
                sourceColumnType = ((SelectExprProcessorTypableForge) forge).getUnderlyingEvaluationType();
            } else if (forge instanceof ExprEvalStreamInsertBean) {
                sourceColumnType = ((ExprEvalStreamInsertBean) forge).getUnderlyingReturnType();
            } else {
                sourceColumnType = forge.getEvaluationType();
            }

            Class targetPropType = resultEventType.getPropertyType(colName);
            try {
                TypeWidenerFactory.getCheckPropertyAssignType(colName, sourceColumnType, targetPropType, colName, false, args.getEventTypeAvroHandler().getTypeWidenerCustomizer(resultEventType), statementRawInfo.getStatementName());
            } catch (TypeWidenerException ex) {
                throw new ExprValidationException(ex.getMessage(), ex);
            }
        }

        if (!needRemap) {
            return new SelectEvalInsertNoWildcardObjectArray(selectExprForgeContext, resultEventType);
        }
        if (CollectionUtil.isAllNullArray(wideners)) {
            return new SelectEvalInsertNoWildcardObjectArrayRemap(selectExprForgeContext, resultEventType, remapped);
        }
        throw new UnsupportedOperationException("Automatic widening to columns of an object-array event type is not supported");
    }

    private String getTypeNameConsiderTable(ObjectArrayEventType resultEventType, TableCompileTimeResolver tableCompileTimeResolver) {
        TableMetaData metadata = tableCompileTimeResolver.resolveTableFromEventType(resultEventType);
        if (metadata != null) {
            return "table '" + metadata.getTableName() + "'";
        }
        return "type '" + resultEventType.getName() + "'";
    }

    private Pair<ExprForge, Object> handleUnderlyingStreamInsert(ExprForge exprEvaluator, EventPropertyDescriptor optionalInsertedTargetProp, EPType optionalInsertedTargetEPType) {
        if (!(exprEvaluator instanceof ExprStreamUnderlyingNode)) {
            return null;
        }
        ExprStreamUnderlyingNode undNode = (ExprStreamUnderlyingNode) exprEvaluator;
        int streamNum = undNode.getStreamId();
        Class returnType = undNode.getForge().getEvaluationType();
        EventType namedWindowAsType = getNamedWindowUnderlyingType(args.getNamedWindowCompileTimeResolver(), args.getTypeService().getEventTypes()[streamNum]);
        TableMetaData tableMetadata = args.getTableCompileTimeResolver().resolveTableFromEventType(args.getTypeService().getEventTypes()[streamNum]);

        EventType eventTypeStream;
        ExprForge forge;
        if (tableMetadata != null) {
            eventTypeStream = tableMetadata.getPublicEventType();
            forge = new ExprEvalStreamInsertTable(streamNum, tableMetadata, returnType);
        } else if (namedWindowAsType == null) {
            eventTypeStream = args.getTypeService().getEventTypes()[streamNum];
            if (optionalInsertedTargetProp != null &&
                JavaClassHelper.isSubclassOrImplementsInterface(eventTypeStream.getUnderlyingType(), optionalInsertedTargetProp.getPropertyType()) &&
                (optionalInsertedTargetEPType == null || !EventTypeUtility.isTypeOrSubTypeOf(eventTypeStream, EPTypeHelper.getEventType(optionalInsertedTargetEPType)))) {
                return new Pair<>(new ExprEvalStreamInsertUnd(undNode, streamNum, returnType), returnType);
            } else {
                forge = new ExprEvalStreamInsertBean(undNode, streamNum, returnType);
            }
        } else {
            eventTypeStream = namedWindowAsType;
            forge = new ExprEvalStreamInsertNamedWindow(streamNum, namedWindowAsType, returnType);
        }

        return new Pair<>(forge, eventTypeStream);
    }

    private EventType getNamedWindowUnderlyingType(NamedWindowCompileTimeResolver namedWindowCompileTimeResolver, EventType eventType) {
        NamedWindowMetaData nw = namedWindowCompileTimeResolver.resolve(eventType.getName());
        if (nw == null) {
            return null;
        }
        return nw.getOptionalEventTypeAs();
    }

    private static EPTypesAndPropertyDescPair determineInsertedEventTypeTargets(EventType targetType, List<SelectClauseExprCompiledSpec> selectionList, InsertIntoDesc insertIntoDesc) {
        EPType[] targets = new EPType[selectionList.size()];
        EventPropertyDescriptor[] propertyDescriptors = new EventPropertyDescriptor[selectionList.size()];
        if (targetType == null) {
            return new EPTypesAndPropertyDescPair(targets, propertyDescriptors);
        }

        for (int i = 0; i < selectionList.size(); i++) {
            SelectClauseExprCompiledSpec expr = selectionList.get(i);

            String providedName = null;
            if (expr.getProvidedName() != null) {
                providedName = expr.getProvidedName();
            } else if (insertIntoDesc.getColumnNames().size() > i) {
                providedName = insertIntoDesc.getColumnNames().get(i);
            }

            if (providedName == null) {
                continue;
            }

            EventPropertyDescriptor desc = targetType.getPropertyDescriptor(providedName);
            propertyDescriptors[i] = desc;
            if (desc == null) {
                continue;
            }

            if (!desc.isFragment()) {
                continue;
            }

            FragmentEventType fragmentEventType = targetType.getFragmentType(providedName);
            if (fragmentEventType == null) {
                continue;
            }

            if (fragmentEventType.isIndexed()) {
                targets[i] = EPTypeHelper.collectionOfEvents(fragmentEventType.getFragmentType());
            } else {
                targets[i] = EPTypeHelper.singleEvent(fragmentEventType.getFragmentType());
            }
        }

        return new EPTypesAndPropertyDescPair(targets, propertyDescriptors);
    }

    private TypeAndForgePair handleTypableExpression(ExprForge forge, int expressionNum, EventTypeNameGeneratorStatement eventTypeNameGeneratorStatement)
        throws ExprValidationException {
        if (!(forge instanceof ExprTypableReturnForge)) {
            return null;
        }

        ExprTypableReturnForge typable = (ExprTypableReturnForge) forge;
        LinkedHashMap<String, Object> eventTypeExpr = typable.getRowProperties();
        if (eventTypeExpr == null) {
            return null;
        }

        String eventTypeName = eventTypeNameGeneratorStatement.getAnonymousTypeNameWithInner(expressionNum);
        EventTypeMetadata metadata = new EventTypeMetadata(eventTypeName, args.getModuleName(), EventTypeTypeClass.STATEMENTOUT, EventTypeApplicationType.MAP, NameAccessModifier.TRANSIENT, EventTypeBusModifier.NONBUS, false, EventTypeIdPair.unassigned());
        Map<String, Object> propertyTypes = EventTypeUtility.getPropertyTypesNonPrimitive(eventTypeExpr);
        EventType mapType = BaseNestableEventUtil.makeMapTypeCompileTime(metadata, propertyTypes, null, null, null, null, args.getBeanEventTypeFactoryPrivate(), args.getEventTypeCompileTimeResolver());
        args.getEventTypeCompileTimeRegistry().newType(mapType);

        ExprForge newForge = new SelectExprProcessorTypableMapForge(mapType, forge);
        return new TypeAndForgePair(mapType, newForge);
    }

    private TypeAndForgePair handleInsertIntoEnumeration(String insertIntoColName, EPType insertIntoTarget, ExprNode expr, ExprForge forge)
        throws ExprValidationException {
        if (insertIntoTarget == null || (!EPTypeHelper.isCarryEvent(insertIntoTarget))) {
            return null;
        }
        final EventType targetType = EPTypeHelper.getEventType(insertIntoTarget);

        final ExprEnumerationForge enumeration;
        if (forge instanceof ExprEnumerationForge) {
            enumeration = (ExprEnumerationForge) forge;
        } else if (expr instanceof ExprEnumerationForgeProvider && !(expr instanceof ExprStreamUnderlyingNode)) {
            // ExprStreamUnderlyingNode specifically is handled elsewhere
            ExprEnumerationForgeProvider provider = (ExprEnumerationForgeProvider) expr;
            ExprEnumerationForgeDesc desc = provider.getEnumerationForge(args.getTypeService(), args.getContextDescriptor());
            if (desc == null || desc.getForge() == null) {
                return null;
            }
            enumeration = desc.getForge();
            EventType sourceType = enumeration.getEventTypeSingle(args.getStatementRawInfo(), args.getCompileTimeServices());
            if (sourceType == null || !EventTypeUtility.isTypeOrSubTypeOf(sourceType, targetType)) {
                return null;
            }
        } else {
            return null;
        }

        final EventType eventTypeSingle = enumeration.getEventTypeSingle(args.getStatementRawInfo(), args.getCompileTimeServices());
        final EventType eventTypeColl = enumeration.getEventTypeCollection(args.getStatementRawInfo(), args.getCompileTimeServices());
        final EventType sourceType = eventTypeSingle != null ? eventTypeSingle : eventTypeColl;
        if (eventTypeColl == null && eventTypeSingle == null) {
            return null;    // enumeration is untyped events (select-clause provided to subquery or 'new' operator)
        }
        if (sourceType.getMetadata().getTypeClass() == EventTypeTypeClass.SUBQDERIVED) {
            return null;    // we don't allow anonymous types here, thus excluding subquery multi-column selection
        }

        // check type info
        checkTypeCompatible(insertIntoColName, targetType, sourceType);

        // handle collection target - produce EventBean[]
        if (insertIntoTarget instanceof EventMultiValuedEPType) {
            if (eventTypeColl != null) {
                ExprEvalEnumerationCollForge enumerationCollForge = new ExprEvalEnumerationCollForge(enumeration, targetType, false);
                return new TypeAndForgePair(new EventType[]{targetType}, enumerationCollForge);
            }
            ExprEvalEnumerationSingleToCollForge singleToCollForge = new ExprEvalEnumerationSingleToCollForge(enumeration, targetType);
            return new TypeAndForgePair(new EventType[]{targetType}, singleToCollForge);
        }

        // handle single-bean target
        // handle single-source
        if (eventTypeSingle != null) {
            ExprEvalEnumerationAtBeanSingleForge singleForge = new ExprEvalEnumerationAtBeanSingleForge(enumeration, targetType);
            return new TypeAndForgePair(targetType, singleForge);
        }

        ExprEvalEnumerationCollForge enumerationCollForge = new ExprEvalEnumerationCollForge(enumeration, targetType, true);
        return new TypeAndForgePair(targetType, enumerationCollForge);
    }

    private void checkTypeCompatible(String insertIntoCol, EventType targetType, EventType selectedType)
        throws ExprValidationException {
        if (selectedType instanceof BeanEventType && targetType instanceof BeanEventType) {
            BeanEventType selected = (BeanEventType) selectedType;
            BeanEventType target = (BeanEventType) targetType;
            if (JavaClassHelper.isSubclassOrImplementsInterface(selected.getUnderlyingType(), target.getUnderlyingType())) {
                return;
            }
        }
        if (!EventTypeUtility.isTypeOrSubTypeOf(targetType, selectedType)) {
            throw new ExprValidationException(
                "Incompatible type detected attempting to insert into column '" +
                    insertIntoCol + "' type '" + targetType.getName() + "' compared to selected type '" + selectedType.getName() + "'");
        }
    }

    private TypeAndForgePair handleInsertIntoTypableExpression(EPType insertIntoTarget, ExprForge forge, SelectProcessorArgs args)
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

        Set<WriteablePropertyDescriptor> writables = EventTypeUtility.getWriteableProperties(targetType, false, false);
        List<WriteablePropertyDescriptor> written = new ArrayList<>();
        List<Map.Entry<String, Object>> writtenOffered = new ArrayList<>();

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
        final TypeWidenerSPI[] wideners = new TypeWidenerSPI[written.size()];
        TypeWidenerCustomizer typeWidenerCustomizer = args.getEventTypeAvroHandler().getTypeWidenerCustomizer(targetType);
        for (int i = 0; i < written.size(); i++) {
            Class expected = written.get(i).getType();
            Map.Entry<String, Object> provided = writtenOffered.get(i);
            if (provided.getValue() instanceof Class) {
                try {
                    wideners[i] = TypeWidenerFactory.getCheckPropertyAssignType(provided.getKey(), (Class) provided.getValue(),
                        expected, written.get(i).getPropertyName(), false, typeWidenerCustomizer, args.getStatementName());
                } catch (TypeWidenerException ex) {
                    throw new ExprValidationException(ex.getMessage(), ex);
                }
            }
        }
        final boolean hasWideners = !CollectionUtil.isAllNullArray(wideners);

        // obtain factory
        WriteablePropertyDescriptor[] writtenArray = written.toArray(new WriteablePropertyDescriptor[written.size()]);
        EventBeanManufacturerForge manufacturer;
        try {
            manufacturer = EventTypeUtility.getManufacturer(targetType, writtenArray, args.getClasspathImportService(), false, args.getEventTypeAvroHandler());
        } catch (EventBeanManufactureException e) {
            throw new ExprValidationException("Failed to obtain eventbean factory: " + e.getMessage(), e);
        }

        // handle collection
        ExprForge typableForge;
        boolean targetIsMultirow = insertIntoTarget instanceof EventMultiValuedEPType;
        if (typable.isMultirow()) {
            if (targetIsMultirow) {
                typableForge = new SelectExprProcessorTypableMultiForge(typable, hasWideners, wideners, manufacturer, targetType, false);
            } else {
                typableForge = new SelectExprProcessorTypableMultiForge(typable, hasWideners, wideners, manufacturer, targetType, true);
            }
        } else {
            if (targetIsMultirow) {
                typableForge = new SelectExprProcessorTypableSingleForge(typable, hasWideners, wideners, manufacturer, targetType, false);
            } else {
                typableForge = new SelectExprProcessorTypableSingleForge(typable, hasWideners, wideners, manufacturer, targetType, true);
            }
        }

        Object type = targetIsMultirow ? new EventType[]{targetType} : targetType;
        return new TypeAndForgePair(type, typableForge);
    }

    protected static void applyWideners(Object[] row, TypeWidenerSPI[] wideners) {
        for (int i = 0; i < wideners.length; i++) {
            if (wideners[i] != null) {
                row[i] = wideners[i].widen(row[i]);
            }
        }
    }

    public static CodegenExpression applyWidenersCodegen(CodegenExpressionRef row, TypeWidenerSPI[] wideners, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        CodegenBlock block = codegenMethodScope.makeChild(void.class, SelectExprProcessorHelper.class, codegenClassScope).addParam(Object[].class, "row").getBlock();
        for (int i = 0; i < wideners.length; i++) {
            if (wideners[i] != null) {
                block.assignArrayElement("row", constant(i), wideners[i].widenCodegen(arrayAtIndex(ref("row"), constant(i)), codegenMethodScope, codegenClassScope));
            }
        }
        return localMethodBuild(block.methodEnd()).pass(row).call();
    }

    public static CodegenExpression applyWidenersCodegenMultirow(CodegenExpressionRef rows, TypeWidenerSPI[] wideners, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        CodegenMethod method = codegenMethodScope.makeChild(void.class, SelectExprProcessorHelper.class, codegenClassScope).addParam(Object[][].class, "rows").getBlock()
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

        final EventType eventTypeColl = enumEval.getEventTypeCollection(args.getStatementRawInfo(), args.getCompileTimeServices());
        if (eventTypeColl != null) {
            TableMetaData tableMetadata = args.getTableCompileTimeResolver().resolveTableFromEventType(eventTypeColl);
            if (tableMetadata == null) {
                ExprEvalEnumerationAtBeanColl collForge = new ExprEvalEnumerationAtBeanColl(enumEval, eventTypeColl);
                return new TypeAndForgePair(new EventType[]{eventTypeColl}, collForge);
            }
            ExprEvalEnumerationAtBeanCollTable tableForge = new ExprEvalEnumerationAtBeanCollTable(enumEval, tableMetadata);
            return new TypeAndForgePair(new EventType[]{tableMetadata.getPublicEventType()}, tableForge);
        }

        final EventType eventTypeSingle = enumEval.getEventTypeSingle(args.getStatementRawInfo(), args.getCompileTimeServices());
        if (eventTypeSingle != null) {
            final TableMetaData tableMetadata = args.getTableCompileTimeResolver().resolveTableFromEventType(eventTypeSingle);
            if (tableMetadata == null) {
                ExprEvalEnumerationAtBeanSingleForge beanForge = new ExprEvalEnumerationAtBeanSingleForge(enumEval, eventTypeSingle);
                return new TypeAndForgePair(eventTypeSingle, beanForge);
            }
            throw new IllegalStateException("Unrecognized enumeration source returning table row-typed values");
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

    private NameAccessModifier getVisibility(String name) {
        return args.getCompileTimeServices().getModuleVisibilityRules().getAccessModifierEventType(args.getStatementRawInfo(), name);
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


    private static class EPTypesAndPropertyDescPair {
        private final EPType[] insertIntoTargetsPerCol;
        private final EventPropertyDescriptor[] propertyDescriptors;

        public EPTypesAndPropertyDescPair(EPType[] insertIntoTargetsPerCol, EventPropertyDescriptor[] propertyDescriptors) {
            this.insertIntoTargetsPerCol = insertIntoTargetsPerCol;
            this.propertyDescriptors = propertyDescriptors;
        }

        public EPType[] getInsertIntoTargetsPerCol() {
            return insertIntoTargetsPerCol;
        }

        public EventPropertyDescriptor[] getPropertyDescriptors() {
            return propertyDescriptors;
        }
    }
}
