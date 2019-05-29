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
package com.espertech.esper.common.internal.epl.contained;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.client.FragmentEventType;
import com.espertech.esper.common.internal.compile.stage1.spec.*;
import com.espertech.esper.common.internal.compile.stage2.SelectClauseElementCompiled;
import com.espertech.esper.common.internal.compile.stage2.SelectClauseExprCompiledSpec;
import com.espertech.esper.common.internal.compile.stage2.StatementRawInfo;
import com.espertech.esper.common.internal.compile.stage3.StatementCompileTimeServices;
import com.espertech.esper.common.internal.epl.expression.core.*;
import com.espertech.esper.common.internal.epl.resultset.select.core.SelectClauseStreamCompiledSpec;
import com.espertech.esper.common.internal.epl.resultset.select.core.SelectExprProcessorDescriptor;
import com.espertech.esper.common.internal.epl.resultset.select.core.SelectExprProcessorFactory;
import com.espertech.esper.common.internal.epl.resultset.select.core.SelectProcessorArgs;
import com.espertech.esper.common.internal.epl.streamtype.StreamTypeService;
import com.espertech.esper.common.internal.epl.streamtype.StreamTypeServiceImpl;
import com.espertech.esper.common.internal.event.core.*;
import com.espertech.esper.common.internal.event.json.core.JsonEventType;
import com.espertech.esper.common.internal.util.JavaClassHelper;
import com.espertech.esper.common.internal.util.UuidGenerator;

import java.util.*;

/**
 * Factory for property evaluators.
 */
public class PropertyEvaluatorForgeFactory {
    public static PropertyEvaluatorForge makeEvaluator(PropertyEvalSpec spec,
                                                       EventType sourceEventType,
                                                       String optionalSourceStreamName,
                                                       StatementRawInfo rawInfo,
                                                       StatementCompileTimeServices services)
        throws ExprValidationException {
        int length = spec.getAtoms().size();
        ContainedEventEvalForge[] containedEventForges = new ContainedEventEvalForge[length];
        FragmentEventType[] fragmentEventTypes = new FragmentEventType[length];
        EventType currentEventType = sourceEventType;
        ExprForge[] whereClauses = new ExprForge[length];

        List<EventType> streamEventTypes = new ArrayList<EventType>();
        List<String> streamNames = new ArrayList<String>();
        Map<String, Integer> streamNameAndNumber = new HashMap<String, Integer>();
        List<String> expressionTexts = new ArrayList<String>();

        streamEventTypes.add(sourceEventType);
        streamNames.add(optionalSourceStreamName);
        streamNameAndNumber.put(optionalSourceStreamName, 0);
        expressionTexts.add(sourceEventType.getName());

        List<SelectClauseElementCompiled> cumulativeSelectClause = new ArrayList<SelectClauseElementCompiled>();
        for (int i = 0; i < length; i++) {
            PropertyEvalAtom atom = spec.getAtoms().get(i);
            ContainedEventEvalForge containedEventEval = null;
            String expressionText = null;
            EventType streamEventType = null;
            FragmentEventType fragmentEventType = null;

            // Resolve directly as fragment event type if possible
            if (atom.getSplitterExpression() instanceof ExprIdentNode) {
                String propertyName = ((ExprIdentNode) atom.getSplitterExpression()).getFullUnresolvedName();
                fragmentEventType = currentEventType.getFragmentType(propertyName);
                if (fragmentEventType != null) {
                    EventPropertyGetterSPI getter = ((EventTypeSPI) currentEventType).getGetterSPI(propertyName);
                    if (getter != null) {
                        containedEventEval = new ContainedEventEvalGetterForge(getter);
                        expressionText = propertyName;
                        streamEventType = fragmentEventType.getFragmentType();
                    }
                }
            }

            // evaluate splitter expression
            if (containedEventEval == null) {
                ExprNodeUtilityValidate.validatePlainExpression(ExprNodeOrigin.CONTAINEDEVENT, atom.getSplitterExpression());

                EventType[] availableTypes = streamEventTypes.toArray(new EventType[streamEventTypes.size()]);
                String[] availableStreamNames = streamNames.toArray(new String[streamNames.size()]);
                boolean[] isIStreamOnly = new boolean[streamNames.size()];
                Arrays.fill(isIStreamOnly, true);
                StreamTypeService streamTypeService = new StreamTypeServiceImpl(availableTypes, availableStreamNames, isIStreamOnly, false, false);
                ExprValidationContext validationContext = new ExprValidationContextBuilder(streamTypeService, rawInfo, services).withAllowBindingConsumption(true).build();
                ExprNode validatedExprNode = ExprNodeUtilityValidate.getValidatedSubtree(ExprNodeOrigin.CONTAINEDEVENT, atom.getSplitterExpression(), validationContext);

                // determine result type
                if (atom.getOptionalResultEventType() == null) {
                    throw new ExprValidationException("Missing @type(name) declaration providing the event type name of the return type for expression '" +
                        ExprNodeUtilityPrint.toExpressionStringMinPrecedenceSafe(atom.getSplitterExpression()) + "'");
                }
                streamEventType = services.getEventTypeCompileTimeResolver().getTypeByName(atom.getOptionalResultEventType());
                if (streamEventType == null) {
                    throw new ExprValidationException("Event type by name '" + atom.getOptionalResultEventType() + "' could not be found");
                }
                Class returnType = validatedExprNode.getForge().getEvaluationType();

                // when the expression returns an array, allow array values to become the column of the single-column event type
                if (returnType.isArray() &&
                    streamEventType.getPropertyNames().length == 1 &&
                    !(streamEventType instanceof JsonEventType) && // since json string-array should not become itself the property
                    JavaClassHelper.isSubclassOrImplementsInterface(JavaClassHelper.getBoxedType(returnType.getComponentType()), JavaClassHelper.getBoxedType(streamEventType.getPropertyType(streamEventType.getPropertyNames()[0])))) {
                    Set<WriteablePropertyDescriptor> writables = EventTypeUtility.getWriteableProperties(streamEventType, false, false);
                    if (writables != null && !writables.isEmpty()) {
                        try {
                            EventBeanManufacturerForge manufacturer = EventTypeUtility.getManufacturer(streamEventType, new WriteablePropertyDescriptor[]{writables.iterator().next()}, services.getClasspathImportServiceCompileTime(), false, services.getEventTypeAvroHandler());
                            containedEventEval = new ContainedEventEvalArrayToEventForge(validatedExprNode.getForge(), manufacturer);
                        } catch (EventBeanManufactureException e) {
                            throw new ExprValidationException("Event type '" + streamEventType.getName() + "' cannot be populated: " + e.getMessage(), e);
                        }
                    } else {
                        throw new ExprValidationException("Event type '" + streamEventType.getName() + "' cannot be written to");
                    }
                } else if (returnType.isArray() &&
                    returnType.getComponentType() == EventBean.class) {
                    containedEventEval = new ContainedEventEvalEventBeanArrayForge(validatedExprNode.getForge());
                } else {
                    // check expression result type against eventtype expected underlying type
                    if (returnType.isArray()) {
                        if (!(streamEventType instanceof JsonEventType)) {
                            if (!JavaClassHelper.isSubclassOrImplementsInterface(returnType.getComponentType(), streamEventType.getUnderlyingType())) {
                                throw new ExprValidationException("Event type '" + streamEventType.getName() + "' underlying type " + streamEventType.getUnderlyingType().getName() +
                                    " cannot be assigned a value of type " + JavaClassHelper.getClassNameFullyQualPretty(returnType));
                            }
                        } else {
                            if (returnType.getComponentType() != String.class) {
                                throw new ExprValidationException("Event type '" + streamEventType.getName() + "' requires string-type array and cannot be assigned from value of type " + JavaClassHelper.getClassNameFullyQualPretty(returnType));
                            }
                        }
                    } else if (JavaClassHelper.isImplementsInterface(returnType, Iterable.class)) {
                        // fine, assumed to return the right type
                    } else {
                        throw new ExprValidationException("Return type of expression '" + ExprNodeUtilityPrint.toExpressionStringMinPrecedenceSafe(atom.getSplitterExpression()) + "' is '" + returnType.getName() + "', expected an Iterable or array result");
                    }
                    containedEventEval = new ContainedEventEvalExprNodeForge(validatedExprNode.getForge(), streamEventType);
                }
                expressionText = ExprNodeUtilityPrint.toExpressionStringMinPrecedenceSafe(validatedExprNode);
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
                StreamTypeService streamTypeService = new StreamTypeServiceImpl(whereTypes, whereStreamNames, isIStreamOnly, false, false);
                ExprValidationContext validationContext = new ExprValidationContextBuilder(streamTypeService, rawInfo, services).withAllowBindingConsumption(true).build();
                ExprNode whereClause = ExprNodeUtilityValidate.getValidatedSubtree(ExprNodeOrigin.CONTAINEDEVENT, atom.getOptionalWhereClause(), validationContext);
                whereClauses[i] = whereClause.getForge();
            }

            // validate select clause
            if (atom.getOptionalSelectClause() != null && !atom.getOptionalSelectClause().getSelectExprList().isEmpty()) {
                EventType[] whereTypes = streamEventTypes.toArray(new EventType[streamEventTypes.size()]);
                String[] whereStreamNames = streamNames.toArray(new String[streamNames.size()]);
                boolean[] isIStreamOnly = new boolean[streamNames.size()];
                Arrays.fill(isIStreamOnly, true);
                StreamTypeService streamTypeService = new StreamTypeServiceImpl(whereTypes, whereStreamNames, isIStreamOnly, false, false);
                ExprValidationContext validationContext = new ExprValidationContextBuilder(streamTypeService, rawInfo, services).withAllowBindingConsumption(true).build();

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
                        ExprNode exprCompiled = ExprNodeUtilityValidate.getValidatedSubtree(ExprNodeOrigin.CONTAINEDEVENT, exprSpec.getSelectExpression(), validationContext);
                        String resultName = exprSpec.getOptionalAsName();
                        if (resultName == null) {
                            resultName = ExprNodeUtilityPrint.toExpressionStringMinPrecedenceSafe(exprCompiled);
                        }
                        cumulativeSelectClause.add(new SelectClauseExprCompiledSpec(exprCompiled, resultName, exprSpec.getOptionalAsName(), exprSpec.isEvents()));

                        String isMinimal = ExprNodeUtilityValidate.isMinimalExpression(exprCompiled);
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
            containedEventForges[i] = containedEventEval;
        }

        if (cumulativeSelectClause.isEmpty()) {
            if (length == 1) {
                return new PropertyEvaluatorSimpleForge(containedEventForges[0], fragmentEventTypes[0], whereClauses[0], expressionTexts.get(0));
            } else {
                return new PropertyEvaluatorNestedForge(containedEventForges, fragmentEventTypes, whereClauses, expressionTexts.toArray(new String[expressionTexts.size()]));
            }
        } else {
            boolean[] fragmentEventTypeIsIndexed = new boolean[fragmentEventTypes.length];
            for (int i = 0; i < fragmentEventTypes.length; i++) {
                fragmentEventTypeIsIndexed[i] = fragmentEventTypes[i].isIndexed();
            }
            PropertyEvaluatorAccumulativeForge accumulative = new PropertyEvaluatorAccumulativeForge(containedEventForges, fragmentEventTypeIsIndexed, whereClauses, expressionTexts);

            EventType[] whereTypes = streamEventTypes.toArray(new EventType[streamEventTypes.size()]);
            String[] whereStreamNames = streamNames.toArray(new String[streamNames.size()]);
            boolean[] isIStreamOnly = new boolean[streamNames.size()];
            Arrays.fill(isIStreamOnly, true);
            StreamTypeService streamTypeService = new StreamTypeServiceImpl(whereTypes, whereStreamNames, isIStreamOnly, false, false);

            SelectClauseElementCompiled[] cumulativeSelectArr = cumulativeSelectClause.toArray(new SelectClauseElementCompiled[cumulativeSelectClause.size()]);
            SelectProcessorArgs args = new SelectProcessorArgs(cumulativeSelectArr, null, false, null, null, streamTypeService,
                null, false, rawInfo.getAnnotations(), rawInfo, services);
            SelectExprProcessorDescriptor selectExprDesc = SelectExprProcessorFactory.getProcessor(args, null, false);

            return new PropertyEvaluatorSelectForge(selectExprDesc, accumulative);
        }
    }
}