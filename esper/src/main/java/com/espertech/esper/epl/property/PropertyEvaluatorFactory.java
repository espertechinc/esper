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
package com.espertech.esper.epl.property;

import com.espertech.esper.client.*;
import com.espertech.esper.core.service.StatementExtensionSvcContext;
import com.espertech.esper.core.service.speccompiled.SelectClauseStreamCompiledSpec;
import com.espertech.esper.epl.core.engineimport.EngineImportService;
import com.espertech.esper.epl.core.select.SelectExprProcessor;
import com.espertech.esper.epl.core.select.SelectExprProcessorCompiler;
import com.espertech.esper.epl.core.select.SelectExprProcessorFactory;
import com.espertech.esper.epl.core.select.SelectExprProcessorForge;
import com.espertech.esper.epl.core.streamtype.StreamTypeService;
import com.espertech.esper.epl.core.streamtype.StreamTypeServiceImpl;
import com.espertech.esper.epl.expression.codegen.ExprNodeCompiler;
import com.espertech.esper.epl.expression.core.*;
import com.espertech.esper.epl.named.NamedWindowMgmtService;
import com.espertech.esper.epl.spec.*;
import com.espertech.esper.epl.table.mgmt.TableService;
import com.espertech.esper.epl.util.ExprNodeUtilityRich;
import com.espertech.esper.epl.variable.VariableService;
import com.espertech.esper.event.*;
import com.espertech.esper.filterspec.PropertyEvaluator;
import com.espertech.esper.schedule.TimeProvider;
import com.espertech.esper.util.JavaClassHelper;
import com.espertech.esper.util.UuidGenerator;

import java.lang.annotation.Annotation;
import java.util.*;

/**
 * Factory for property evaluators.
 */
public class PropertyEvaluatorFactory {
    public static PropertyEvaluator makeEvaluator(PropertyEvalSpec spec,
                                                  EventType sourceEventType,
                                                  String optionalSourceStreamName,
                                                  EventAdapterService eventAdapterService,
                                                  EngineImportService engineImportService,
                                                  final TimeProvider timeProvider,
                                                  VariableService variableService,
                                                  TableService tableService,
                                                  String engineURI,
                                                  int statementId,
                                                  String statementName,
                                                  Annotation[] annotations,
                                                  Collection<Integer> assignedTypeNumberStack,
                                                  ConfigurationInformation configuration,
                                                  NamedWindowMgmtService namedWindowMgmtService,
                                                  StatementExtensionSvcContext statementExtensionSvcContext)
            throws ExprValidationException {
        int length = spec.getAtoms().size();
        ContainedEventEval[] containedEventEvals = new ContainedEventEval[length];
        FragmentEventType[] fragmentEventTypes = new FragmentEventType[length];
        EventType currentEventType = sourceEventType;
        ExprEvaluator[] whereClauses = new ExprEvaluator[length];

        List<EventType> streamEventTypes = new ArrayList<EventType>();
        List<String> streamNames = new ArrayList<String>();
        Map<String, Integer> streamNameAndNumber = new HashMap<String, Integer>();
        List<String> expressionTexts = new ArrayList<String>();
        ExprEvaluatorContext validateContext = new ExprEvaluatorContextTimeOnly(timeProvider);

        streamEventTypes.add(sourceEventType);
        streamNames.add(optionalSourceStreamName);
        streamNameAndNumber.put(optionalSourceStreamName, 0);
        expressionTexts.add(sourceEventType.getName());

        List<SelectClauseElementCompiled> cumulativeSelectClause = new ArrayList<SelectClauseElementCompiled>();
        for (int i = 0; i < length; i++) {
            PropertyEvalAtom atom = spec.getAtoms().get(i);
            ContainedEventEval containedEventEval = null;
            String expressionText = null;
            EventType streamEventType = null;
            FragmentEventType fragmentEventType = null;

            // Resolve directly as fragment event type if possible
            if (atom.getSplitterExpression() instanceof ExprIdentNode) {
                String propertyName = ((ExprIdentNode) atom.getSplitterExpression()).getFullUnresolvedName();
                fragmentEventType = currentEventType.getFragmentType(propertyName);
                if (fragmentEventType != null) {
                    EventPropertyGetter getter = currentEventType.getGetter(propertyName);
                    if (getter != null) {
                        containedEventEval = new ContainedEventEvalGetter(getter);
                        expressionText = propertyName;
                        streamEventType = fragmentEventType.getFragmentType();
                    }
                }
            }

            // evaluate splitter expression
            if (containedEventEval == null) {
                ExprNodeUtilityRich.validatePlainExpression(ExprNodeOrigin.CONTAINEDEVENT, atom.getSplitterExpression());

                EventType[] availableTypes = streamEventTypes.toArray(new EventType[streamEventTypes.size()]);
                String[] availableStreamNames = streamNames.toArray(new String[streamNames.size()]);
                boolean[] isIStreamOnly = new boolean[streamNames.size()];
                Arrays.fill(isIStreamOnly, true);
                StreamTypeService streamTypeService = new StreamTypeServiceImpl(availableTypes, availableStreamNames, isIStreamOnly, engineURI, false, false);
                ExprValidationContext validationContext = new ExprValidationContext(streamTypeService, engineImportService, statementExtensionSvcContext, null, timeProvider, variableService, tableService, validateContext, eventAdapterService, statementName, statementId, annotations, null, false, false, true, false, null, false);
                ExprNode validatedExprNode = ExprNodeUtilityRich.getValidatedSubtree(ExprNodeOrigin.CONTAINEDEVENT, atom.getSplitterExpression(), validationContext);
                ExprEvaluator evaluator = ExprNodeCompiler.allocateEvaluator(validatedExprNode.getForge(), engineImportService, PropertyEvaluatorFactory.class, false, statementName);

                // determine result type
                if (atom.getOptionalResultEventType() == null) {
                    throw new ExprValidationException("Missing @type(name) declaration providing the event type name of the return type for expression '" +
                            ExprNodeUtilityCore.toExpressionStringMinPrecedenceSafe(atom.getSplitterExpression()) + "'");
                }
                streamEventType = eventAdapterService.getExistsTypeByName(atom.getOptionalResultEventType());
                if (streamEventType == null) {
                    throw new ExprValidationException("Event type by name '" + atom.getOptionalResultEventType() + "' could not be found");
                }
                Class returnType = validatedExprNode.getForge().getEvaluationType();

                // when the expression returns an array, allow array values to become the column of the single-column event type
                if (returnType.isArray() &&
                        streamEventType.getPropertyNames().length == 1 &&
                        JavaClassHelper.isSubclassOrImplementsInterface(JavaClassHelper.getBoxedType(returnType.getComponentType()), JavaClassHelper.getBoxedType(streamEventType.getPropertyType(streamEventType.getPropertyNames()[0])))) {
                    Set<WriteablePropertyDescriptor> writables = eventAdapterService.getWriteableProperties(streamEventType, false);
                    if (!writables.isEmpty()) {
                        try {
                            EventBeanManufacturer manufacturer = EventAdapterServiceHelper.getManufacturer(eventAdapterService, streamEventType, new WriteablePropertyDescriptor[]{writables.iterator().next()}, engineImportService, false, eventAdapterService.getEventAdapterAvroHandler());
                            containedEventEval = new ContainedEventEvalArrayToEvent(evaluator, manufacturer);
                        } catch (EventBeanManufactureException e) {
                            throw new ExprValidationException("Event type '" + streamEventType.getName() + "' cannot be populated: " + e.getMessage(), e);
                        }
                    } else {
                        throw new ExprValidationException("Event type '" + streamEventType.getName() + "' cannot be written to");
                    }
                } else if (returnType.isArray() &&
                        returnType.getComponentType() == EventBean.class) {
                    containedEventEval = new ContainedEventEvalEventBeanArray(evaluator);
                } else {
                    EventBeanFactory eventBeanFactory = EventAdapterServiceHelper.getFactoryForType(streamEventType, eventAdapterService);
                    // check expression result type against eventtype expected underlying type
                    if (returnType.isArray()) {
                        if (!JavaClassHelper.isSubclassOrImplementsInterface(returnType.getComponentType(), streamEventType.getUnderlyingType())) {
                            throw new ExprValidationException("Event type '" + streamEventType.getName() + "' underlying type " + streamEventType.getUnderlyingType().getName() +
                                    " cannot be assigned a value of type " + JavaClassHelper.getClassNameFullyQualPretty(returnType));
                        }
                    } else if (JavaClassHelper.isImplementsInterface(returnType, Iterable.class)) {
                        // fine, assumed to return the right type
                    } else {
                        throw new ExprValidationException("Return type of expression '" + ExprNodeUtilityCore.toExpressionStringMinPrecedenceSafe(atom.getSplitterExpression()) + "' is '" + returnType.getName() + "', expected an Iterable or array result");
                    }
                    containedEventEval = new ContainedEventEvalExprNode(evaluator, eventBeanFactory);
                }
                expressionText = ExprNodeUtilityCore.toExpressionStringMinPrecedenceSafe(validatedExprNode);
                fragmentEventType = new FragmentEventType(streamEventType, true, false);
            }

            // validate where clause, if any
            streamEventTypes.add(streamEventType);
            streamNames.add(atom.getOptionalAsName());
            streamNameAndNumber.put(atom.getOptionalAsName(), i + 1);
            expressionTexts.add(expressionText);

            if (atom.getOptionalWhereClause() != null) {
                EventType[] whereTypes = streamEventTypes.toArray(new EventType[streamEventTypes.size()]);
                String[] whereStreamNames = streamNames.toArray(new String[streamNames.size()]);
                boolean[] isIStreamOnly = new boolean[streamNames.size()];
                Arrays.fill(isIStreamOnly, true);
                StreamTypeService streamTypeService = new StreamTypeServiceImpl(whereTypes, whereStreamNames, isIStreamOnly, engineURI, false, false);
                ExprValidationContext validationContext = new ExprValidationContext(streamTypeService, engineImportService, statementExtensionSvcContext, null, timeProvider, variableService, tableService, validateContext, eventAdapterService, statementName, statementId, annotations, null, false, false, true, false, null, false);
                ExprNode whereClause = ExprNodeUtilityRich.getValidatedSubtree(ExprNodeOrigin.CONTAINEDEVENT, atom.getOptionalWhereClause(), validationContext);
                whereClauses[i] = ExprNodeCompiler.allocateEvaluator(whereClause.getForge(), engineImportService, PropertyEvaluatorFactory.class, false, statementName);
            }

            // validate select clause
            if (atom.getOptionalSelectClause() != null) {
                EventType[] whereTypes = streamEventTypes.toArray(new EventType[streamEventTypes.size()]);
                String[] whereStreamNames = streamNames.toArray(new String[streamNames.size()]);
                boolean[] isIStreamOnly = new boolean[streamNames.size()];
                Arrays.fill(isIStreamOnly, true);
                StreamTypeService streamTypeService = new StreamTypeServiceImpl(whereTypes, whereStreamNames, isIStreamOnly, engineURI, false, false);
                ExprValidationContext validationContext = new ExprValidationContext(streamTypeService, engineImportService, statementExtensionSvcContext, null, timeProvider, variableService, tableService, validateContext, eventAdapterService, statementName, statementId, annotations, null, false, false, true, false, null, false);

                for (SelectClauseElementRaw raw : atom.getOptionalSelectClause().getSelectExprList()) {
                    if (raw instanceof SelectClauseStreamRawSpec) {
                        SelectClauseStreamRawSpec rawStreamSpec = (SelectClauseStreamRawSpec) raw;
                        if (!streamNames.contains(rawStreamSpec.getStreamName())) {
                            throw new ExprValidationException("Property rename '" + rawStreamSpec.getStreamName() + "' not found in path");
                        }
                        SelectClauseStreamCompiledSpec streamSpec = new SelectClauseStreamCompiledSpec(rawStreamSpec.getStreamName(), rawStreamSpec.getOptionalAsName());
                        int streamNumber = streamNameAndNumber.get(rawStreamSpec.getStreamName());
                        streamSpec.setStreamNumber(streamNumber);
                        cumulativeSelectClause.add(streamSpec);
                    } else if (raw instanceof SelectClauseExprRawSpec) {
                        SelectClauseExprRawSpec exprSpec = (SelectClauseExprRawSpec) raw;
                        ExprNode exprCompiled = ExprNodeUtilityRich.getValidatedSubtree(ExprNodeOrigin.CONTAINEDEVENT, exprSpec.getSelectExpression(), validationContext);
                        String resultName = exprSpec.getOptionalAsName();
                        if (resultName == null) {
                            resultName = ExprNodeUtilityCore.toExpressionStringMinPrecedenceSafe(exprCompiled);
                        }
                        cumulativeSelectClause.add(new SelectClauseExprCompiledSpec(exprCompiled, resultName, exprSpec.getOptionalAsName(), exprSpec.isEvents()));

                        String isMinimal = ExprNodeUtilityRich.isMinimalExpression(exprCompiled);
                        if (isMinimal != null) {
                            throw new ExprValidationException("Expression in a property-selection may not utilize " + isMinimal);
                        }
                    } else if (raw instanceof SelectClauseElementWildcard) {
                        // wildcards are stream selects: we assign a stream name (any) and add a stream wildcard select
                        String streamNameAtom = atom.getOptionalAsName();
                        if (streamNameAtom == null) {
                            streamNameAtom = UuidGenerator.generate();
                        }

                        SelectClauseStreamCompiledSpec streamSpec = new SelectClauseStreamCompiledSpec(streamNameAtom, atom.getOptionalAsName());
                        int streamNumber = i + 1;
                        streamSpec.setStreamNumber(streamNumber);
                        cumulativeSelectClause.add(streamSpec);
                    } else {
                        throw new IllegalStateException("Unknown select clause item:" + raw);
                    }
                }
            }

            currentEventType = fragmentEventType.getFragmentType();
            fragmentEventTypes[i] = fragmentEventType;
            containedEventEvals[i] = containedEventEval;
        }

        if (cumulativeSelectClause.isEmpty()) {
            if (length == 1) {
                return new PropertyEvaluatorSimple(containedEventEvals[0], fragmentEventTypes[0], whereClauses[0], expressionTexts.get(0));
            } else {
                return new PropertyEvaluatorNested(containedEventEvals, fragmentEventTypes, whereClauses, expressionTexts);
            }
        } else {
            PropertyEvaluatorAccumulative accumulative = new PropertyEvaluatorAccumulative(containedEventEvals, fragmentEventTypes, whereClauses, expressionTexts);

            EventType[] whereTypes = streamEventTypes.toArray(new EventType[streamEventTypes.size()]);
            String[] whereStreamNames = streamNames.toArray(new String[streamNames.size()]);
            boolean[] isIStreamOnly = new boolean[streamNames.size()];
            Arrays.fill(isIStreamOnly, true);
            StreamTypeService streamTypeService = new StreamTypeServiceImpl(whereTypes, whereStreamNames, isIStreamOnly, engineURI, false, false);

            SelectClauseElementCompiled[] cumulativeSelectArr = cumulativeSelectClause.toArray(new SelectClauseElementCompiled[cumulativeSelectClause.size()]);
            SelectExprProcessorForge selectExprForge = SelectExprProcessorFactory.getProcessor(assignedTypeNumberStack, cumulativeSelectArr, false, null, null, null, streamTypeService, eventAdapterService, null, null, null, engineImportService, validateContext, variableService, tableService, timeProvider, engineURI, statementId, statementName, annotations, null, configuration, null, namedWindowMgmtService, null, null, statementExtensionSvcContext);
            SelectExprProcessor selectExpr = SelectExprProcessorCompiler.allocateSelectExprEvaluator(eventAdapterService, selectExprForge, engineImportService, PropertyEvaluatorFactory.class, false, statementName);

            return new PropertyEvaluatorSelect(selectExprForge.getResultEventType(), selectExpr, accumulative);
        }
    }
}