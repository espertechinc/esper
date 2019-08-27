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
package com.espertech.esper.common.internal.epl.datetime.eval;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.hook.datetimemethod.DateTimeMethodOps;
import com.espertech.esper.common.client.hook.datetimemethod.DateTimeMethodOpsModify;
import com.espertech.esper.common.client.hook.datetimemethod.DateTimeMethodOpsReformat;
import com.espertech.esper.common.client.hook.datetimemethod.DateTimeMethodValidateContext;
import com.espertech.esper.common.client.util.TimePeriod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.compile.stage2.StatementRawInfo;
import com.espertech.esper.common.internal.epl.datetime.calop.CalendarForge;
import com.espertech.esper.common.internal.epl.datetime.calop.CalendarForgeFactory;
import com.espertech.esper.common.internal.epl.datetime.interval.IntervalForge;
import com.espertech.esper.common.internal.epl.datetime.interval.IntervalForgeFactory;
import com.espertech.esper.common.internal.epl.datetime.plugin.DTMPluginForgeFactory;
import com.espertech.esper.common.internal.epl.datetime.plugin.DTMPluginReformatForge;
import com.espertech.esper.common.internal.epl.datetime.plugin.DTMPluginValueChangeForge;
import com.espertech.esper.common.internal.epl.datetime.reformatop.ReformatForge;
import com.espertech.esper.common.internal.epl.datetime.reformatop.ReformatForgeFactory;
import com.espertech.esper.common.internal.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.common.internal.epl.expression.core.*;
import com.espertech.esper.common.internal.epl.expression.dot.core.ExprDotForge;
import com.espertech.esper.common.internal.epl.expression.dot.core.ExprDotNodeFilterAnalyzerInput;
import com.espertech.esper.common.internal.epl.expression.time.abacus.TimeAbacus;
import com.espertech.esper.common.internal.epl.expression.time.node.ExprTimePeriod;
import com.espertech.esper.common.internal.epl.join.analyze.FilterExprAnalyzerAffector;
import com.espertech.esper.common.internal.epl.methodbase.*;
import com.espertech.esper.common.internal.epl.streamtype.StreamTypeService;
import com.espertech.esper.common.internal.epl.table.compiletime.TableCompileTimeResolver;
import com.espertech.esper.common.internal.rettype.*;
import com.espertech.esper.common.internal.settings.ClasspathImportServiceCompileTime;
import com.espertech.esper.common.internal.util.JavaClassHelper;

import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

public class ExprDotDTFactory {

    public static ExprDotDTMethodDesc validateMake(StreamTypeService streamTypeService, Deque<ExprChainedSpec> chainSpecStack, DatetimeMethodDesc dtMethod, String dtMethodName, EPType inputType, List<ExprNode> parameters, ExprDotNodeFilterAnalyzerInput inputDesc, TimeAbacus timeAbacus, TableCompileTimeResolver tableCompileTimeResolver, ClasspathImportServiceCompileTime classpathImportService, StatementRawInfo statementRawInfo)
        throws ExprValidationException {
        // verify input
        String message = "Date-time enumeration method '" + dtMethodName + "' requires either a Calendar, Date, long, LocalDateTime or ZonedDateTime value as input or events of an event type that declares a timestamp property";
        if (inputType instanceof EventEPType) {
            if (((EventEPType) inputType).getType().getStartTimestampPropertyName() == null) {
                throw new ExprValidationException(message);
            }
        } else {
            if (!(inputType instanceof ClassEPType || inputType instanceof NullEPType)) {
                throw new ExprValidationException(message + " but received " + EPTypeHelper.toTypeDescriptive(inputType));
            }
            if (inputType instanceof ClassEPType) {
                ClassEPType classEPType = (ClassEPType) inputType;
                if (!JavaClassHelper.isDatetimeClass(classEPType.getType())) {
                    throw new ExprValidationException(message + " but received " + JavaClassHelper.getClassNameFullyQualPretty(classEPType.getType()));
                }
            }
        }

        List<CalendarForge> calendarForges = new ArrayList<>();
        ReformatForge reformatForge = null;
        IntervalForge intervalForge = null;
        DatetimeMethodDesc currentMethod = dtMethod;
        List<ExprNode> currentParameters = parameters;
        String currentMethodName = dtMethodName;

        // drain all calendar op
        FilterExprAnalyzerAffector filterAnalyzerDesc = null;
        while (true) {

            // handle the first one only if its a calendar op
            ExprForge[] forges = getForges(currentParameters);
            DatetimeMethodProviderForgeFactory opFactory = currentMethod.getForgeFactory();

            // compile parameter abstract for validation against available footprints
            DotMethodFPProvided footprintProvided = DotMethodUtil.getProvidedFootprint(currentParameters);

            // validate parameters
            DotMethodFP footprintFound = DotMethodUtil.validateParametersDetermineFootprint(currentMethod.getFootprints(), DotMethodTypeEnum.DATETIME, currentMethodName, footprintProvided, DotMethodInputTypeMatcher.DEFAULT_ALL);

            if (opFactory instanceof CalendarForgeFactory) {
                CalendarForge calendarForge = ((CalendarForgeFactory) currentMethod.getForgeFactory()).getOp(currentMethod, currentMethodName, currentParameters, forges);
                calendarForges.add(calendarForge);
            } else if (opFactory instanceof ReformatForgeFactory) {
                reformatForge = ((ReformatForgeFactory) opFactory).getForge(inputType, timeAbacus, currentMethod, currentMethodName, currentParameters);

                // compile filter analyzer information if there are no calendar op in the chain
                if (calendarForges.isEmpty()) {
                    filterAnalyzerDesc = reformatForge.getFilterDesc(streamTypeService.getEventTypes(), currentMethod, currentParameters, inputDesc);
                } else {
                    filterAnalyzerDesc = null;
                }
            } else if (opFactory instanceof IntervalForgeFactory) {
                intervalForge = ((IntervalForgeFactory) opFactory).getForge(streamTypeService, currentMethod, currentMethodName, currentParameters, timeAbacus, tableCompileTimeResolver);

                // compile filter analyzer information if there are no calendar op in the chain
                if (calendarForges.isEmpty()) {
                    filterAnalyzerDesc = intervalForge.getFilterDesc(streamTypeService.getEventTypes(), currentMethod, currentParameters, inputDesc);
                } else {
                    filterAnalyzerDesc = null;
                }
            } else if (opFactory instanceof DTMPluginForgeFactory) {
                DTMPluginForgeFactory plugIn = (DTMPluginForgeFactory) opFactory;
                DateTimeMethodValidateContext usageDesc = new DateTimeMethodValidateContext(footprintFound, streamTypeService, currentMethod, currentParameters, statementRawInfo);
                DateTimeMethodOps ops = plugIn.validate(usageDesc);
                if (ops == null) {
                    throw new ExprValidationException("Plug-in datetime method provider " + plugIn.getClass() + " returned a null-value for the operations");
                }
                Class input = EPTypeHelper.getClassSingleValued(inputType);
                if (ops instanceof DateTimeMethodOpsModify) {
                    calendarForges.add(new DTMPluginValueChangeForge(input, (DateTimeMethodOpsModify) ops, usageDesc.getCurrentParameters()));
                } else if (ops instanceof DateTimeMethodOpsReformat) {
                    reformatForge = new DTMPluginReformatForge(input, (DateTimeMethodOpsReformat) ops, usageDesc.getCurrentParameters());
                } else {
                    throw new ExprValidationException("Plug-in datetime method ops " + ops.getClass() + " is not recognized");
                }
                // no action
            } else {
                throw new IllegalStateException("Invalid op factory class " + opFactory);
            }

            // see if there is more
            if (chainSpecStack.isEmpty() || !DatetimeMethodResolver.isDateTimeMethod(chainSpecStack.getFirst().getName(), classpathImportService)) {
                break;
            }

            // pull next
            ExprChainedSpec next = chainSpecStack.removeFirst();
            currentMethod = DatetimeMethodResolver.fromName(next.getName(), classpathImportService);
            currentParameters = next.getParameters();
            currentMethodName = next.getName();

            if (reformatForge != null || intervalForge != null) {
                throw new ExprValidationException("Invalid input for date-time method '" + next.getName() + "'");
            }
        }

        ExprDotForge dotForge;
        EPType returnType;

        dotForge = new ExprDotDTForge(calendarForges, timeAbacus, reformatForge, intervalForge, EPTypeHelper.getClassSingleValued(inputType), EPTypeHelper.getEventTypeSingleValued(inputType));
        returnType = dotForge.getTypeInfo();
        return new ExprDotDTMethodDesc(dotForge, returnType, filterAnalyzerDesc);
    }

    private static ExprForge[] getForges(List<ExprNode> parameters) {

        ExprForge[] inputExpr = new ExprForge[parameters.size()];
        for (int i = 0; i < parameters.size(); i++) {

            ExprNode innerExpr = parameters.get(i);
            final ExprForge inner = innerExpr.getForge();

            // Time periods get special attention
            if (innerExpr instanceof ExprTimePeriod) {

                final ExprTimePeriod timePeriod = (ExprTimePeriod) innerExpr;
                inputExpr[i] = new ExprForge() {
                    public ExprEvaluator getExprEvaluator() {
                        return new ExprEvaluator() {
                            public Object evaluate(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
                                return timePeriod.evaluateGetTimePeriod(eventsPerStream, isNewData, context);
                            }
                        };
                    }

                    public ExprForgeConstantType getForgeConstantType() {
                        return ExprForgeConstantType.NONCONST;
                    }

                    public CodegenExpression evaluateCodegen(Class requiredType, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
                        return timePeriod.evaluateGetTimePeriodCodegen(codegenMethodScope, exprSymbol, codegenClassScope);
                    }

                    public Class getEvaluationType() {
                        return TimePeriod.class;
                    }

                    public ExprNode getForgeRenderable() {
                        return timePeriod;
                    }
                };
            } else {
                inputExpr[i] = inner;
            }
        }
        return inputExpr;
    }
}
