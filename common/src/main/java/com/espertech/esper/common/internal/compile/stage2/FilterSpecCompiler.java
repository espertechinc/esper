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

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.client.annotation.HookType;
import com.espertech.esper.common.internal.collection.Pair;
import com.espertech.esper.common.internal.compile.stage1.spec.PropertyEvalSpec;
import com.espertech.esper.common.internal.compile.stage3.StatementCompileTimeServices;
import com.espertech.esper.common.internal.compile.stage3.StmtClassForgeableFactory;
import com.espertech.esper.common.internal.epl.contained.PropertyEvaluatorForge;
import com.espertech.esper.common.internal.epl.contained.PropertyEvaluatorForgeFactory;
import com.espertech.esper.common.internal.epl.expression.core.*;
import com.espertech.esper.common.internal.epl.expression.subquery.ExprSubselectNode;
import com.espertech.esper.common.internal.epl.expression.visitor.ExprNodeSubselectDeclaredDotVisitor;
import com.espertech.esper.common.internal.epl.streamtype.StreamTypeService;
import com.espertech.esper.common.internal.epl.subselect.SubSelectHelperFilters;
import com.espertech.esper.common.internal.filterspec.FilterSpecParamForge;
import com.espertech.esper.common.internal.settings.ClasspathImportUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * Helper to compile (validate and optimize) filter expressions as used in pattern and filter-based streams.
 */
public final class FilterSpecCompiler {
    private static final Logger log = LoggerFactory.getLogger(FilterSpecCompiler.class);
    public final static String NEWLINE = System.getProperty("line.separator");

    public static FilterSpecCompiledDesc makeFilterSpec(EventType eventType,
                                                        String eventTypeName,
                                                        List<ExprNode> filterExpessions,
                                                        PropertyEvalSpec optionalPropertyEvalSpec,
                                                        LinkedHashMap<String, Pair<EventType, String>> taggedEventTypes,
                                                        LinkedHashMap<String, Pair<EventType, String>> arrayEventTypes,
                                                        LinkedHashSet<String> allTagNamesOrdered,
                                                        StreamTypeService streamTypeService,
                                                        String optionalStreamName,
                                                        StatementRawInfo statementRawInfo,
                                                        StatementCompileTimeServices services)
        throws ExprValidationException {
        // Validate all nodes, make sure each returns a boolean and types are good;
        // Also decompose all AND super nodes into individual expressions
        FilterSpecValidatedDesc validatedDesc = validateAllowSubquery(ExprNodeOrigin.FILTER, filterExpessions, streamTypeService, taggedEventTypes, arrayEventTypes, statementRawInfo, services);
        return build(validatedDesc, eventType, eventTypeName, optionalPropertyEvalSpec, taggedEventTypes, arrayEventTypes, allTagNamesOrdered, streamTypeService, optionalStreamName, statementRawInfo, services);
    }

    public static FilterSpecCompiledDesc build(FilterSpecValidatedDesc validatedDesc,
                                               EventType eventType,
                                               String eventTypeName,
                                               PropertyEvalSpec optionalPropertyEvalSpec,
                                               LinkedHashMap<String, Pair<EventType, String>> taggedEventTypes,
                                               LinkedHashMap<String, Pair<EventType, String>> arrayEventTypes,
                                               LinkedHashSet<String> allTagNamesOrdered,
                                               StreamTypeService streamTypeService,
                                               String optionalStreamName,
                                               StatementRawInfo statementRawInfo,
                                               StatementCompileTimeServices compileTimeServices) throws ExprValidationException {

        FilterSpecCompiled compiled = buildNoStmtCtx(validatedDesc.getExpressions(), eventType, eventTypeName, optionalStreamName, optionalPropertyEvalSpec, taggedEventTypes, arrayEventTypes, allTagNamesOrdered, streamTypeService, statementRawInfo, compileTimeServices);
        return new FilterSpecCompiledDesc(compiled, validatedDesc.getAdditionalForgeables());
    }

    public static FilterSpecCompiled buildNoStmtCtx(List<ExprNode> validatedNodes,
                                                    EventType eventType,
                                                    String eventTypeName,
                                                    String optionalStreamName,
                                                    PropertyEvalSpec optionalPropertyEvalSpec,
                                                    LinkedHashMap<String, Pair<EventType, String>> taggedEventTypes,
                                                    LinkedHashMap<String, Pair<EventType, String>> arrayEventTypes,
                                                    LinkedHashSet<String> allTagNamesOrdered,
                                                    StreamTypeService streamTypeService,
                                                    StatementRawInfo statementRawInfo,
                                                    StatementCompileTimeServices compileTimeServices
    ) throws ExprValidationException {

        PropertyEvaluatorForge optionalPropertyEvaluator = null;
        if (optionalPropertyEvalSpec != null) {
            optionalPropertyEvaluator = PropertyEvaluatorForgeFactory.makeEvaluator(optionalPropertyEvalSpec, eventType, optionalStreamName, statementRawInfo, compileTimeServices);
        }

        FilterSpecCompilerArgs args = new FilterSpecCompilerArgs(taggedEventTypes, arrayEventTypes, allTagNamesOrdered, streamTypeService, null, statementRawInfo, compileTimeServices);
        List<FilterSpecParamForge>[] spec = FilterSpecCompilerPlanner.planFilterParameters(validatedNodes, args);

        FilterSpecCompileHook hook = (FilterSpecCompileHook) ClasspathImportUtil.getAnnotationHook(statementRawInfo.getAnnotations(), HookType.INTERNAL_FILTERSPEC, FilterSpecCompileHook.class, compileTimeServices.getClasspathImportServiceCompileTime());
        if (hook != null) {
            hook.filterSpec(eventType, spec);
        }

        if (compileTimeServices.getConfiguration().getCompiler().getLogging().isEnableFilterPlan()) {
            logFilterPlans(validatedNodes, spec, eventType, optionalStreamName, statementRawInfo);
        }

        if (log.isDebugEnabled()) {
            log.debug(".makeFilterSpec spec=" + spec);
        }
        return new FilterSpecCompiled(eventType, eventTypeName, spec, optionalPropertyEvaluator);
    }

    public static FilterSpecValidatedDesc validateAllowSubquery(ExprNodeOrigin exprNodeOrigin,
                                                                List<ExprNode> exprNodes,
                                                                StreamTypeService streamTypeService,
                                                                LinkedHashMap<String, Pair<EventType, String>> taggedEventTypes,
                                                                LinkedHashMap<String, Pair<EventType, String>> arrayEventTypes,
                                                                StatementRawInfo statementRawInfo,
                                                                StatementCompileTimeServices services)
        throws ExprValidationException {
        List<ExprNode> validatedNodes = new ArrayList<ExprNode>();
        List<StmtClassForgeableFactory> additionalForgeables = new ArrayList<>(2);

        ExprValidationContext validationContext = new ExprValidationContextBuilder(streamTypeService, statementRawInfo, services)
            .withAllowBindingConsumption(true).withIsFilterExpression(true).build();
        for (ExprNode node : exprNodes) {
            // Determine subselects
            ExprNodeSubselectDeclaredDotVisitor visitor = new ExprNodeSubselectDeclaredDotVisitor();
            node.accept(visitor);

            // Compile subselects
            if (!visitor.getSubselects().isEmpty()) {

                // The outer event type is the filtered-type itself
                for (ExprSubselectNode subselect : visitor.getSubselects()) {
                    try {
                        List<StmtClassForgeableFactory> subselectAdditionalForgeables = SubSelectHelperFilters.handleSubselectSelectClauses(subselect,
                            streamTypeService.getEventTypes()[0], streamTypeService.getStreamNames()[0], streamTypeService.getStreamNames()[0],
                            taggedEventTypes, arrayEventTypes, statementRawInfo, services);
                        additionalForgeables.addAll(subselectAdditionalForgeables);
                    } catch (ExprValidationException ex) {
                        throw new ExprValidationException("Failed to validate " + ExprNodeUtilityMake.getSubqueryInfoText(subselect) + ": " + ex.getMessage(), ex);
                    }
                }
            }

            ExprNode validated = ExprNodeUtilityValidate.getValidatedSubtree(exprNodeOrigin, node, validationContext);
            validatedNodes.add(validated);

            if ((validated.getForge().getEvaluationType() != Boolean.class) && ((validated.getForge().getEvaluationType() != boolean.class))) {
                throw new ExprValidationException("Filter expression not returning a boolean value: '" + ExprNodeUtilityPrint.toExpressionStringMinPrecedenceSafe(validated) + "'");
            }
        }

        return new FilterSpecValidatedDesc(validatedNodes, additionalForgeables);
    }

    private static void logFilterPlans(List<ExprNode> validatedNodes, List<FilterSpecParamForge>[] spec, EventType eventType, String optionalStreamName, StatementRawInfo statementRawInfo) {
        StringBuilder buf = new StringBuilder();
        buf.append("Filter plan for statement '").append(statementRawInfo.getStatementName()).append("' filtering event type '").append(eventType.getName() + "'");
        if (optionalStreamName != null) {
            buf.append(" alias '" + optionalStreamName + "'");
        }
        if (validatedNodes.isEmpty()) {
            buf.append(" empty");
        } else {
            ExprNode andNode = ExprNodeUtilityMake.connectExpressionsByLogicalAndWhenNeeded(validatedNodes);
            String expression = ExprNodeUtilityPrint.toExpressionStringMinPrecedenceSafe(andNode);
            buf.append(" expression '").append(expression).append("' for ").append(spec.length).append(" path");
        }

        for (int i = 0; i < spec.length; i++) {
            logFilterPlanPath(i, spec[i], buf);
        }

        log.info(buf.toString());
    }

    private static void logFilterPlanPath(int indexPath, List<FilterSpecParamForge> forges, StringBuilder buf) {
        buf.append("  path #").append(indexPath).append(" there are ").append(forges.size()).append(" tuplets").append(NEWLINE);

        int indexForge = 0;
        for (FilterSpecParamForge forge : forges) {
            logFilterPlanForge(indexForge, forge, buf);
            indexForge++;
        }
    }

    private static void logFilterPlanForge(int indexForge, FilterSpecParamForge forge, StringBuilder buf) {
        buf.append("    tuplet #").append(indexForge).append(NEWLINE);
        buf.append("      operator: ").append(forge.getFilterOperator().getTextualOp()).append(NEWLINE);
        buf.append("      lookupable: ").append(forge.getLookupable().getExpression()).append(NEWLINE);
        buf.append("      value-expression: ");
        forge.valueExprToString(buf, 8);
    }
}
