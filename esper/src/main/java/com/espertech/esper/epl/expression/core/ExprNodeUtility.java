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
package com.espertech.esper.epl.expression.core;

import com.espertech.esper.client.EPException;
import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.EventType;
import com.espertech.esper.client.hook.AggregationFunctionFactory;
import com.espertech.esper.client.hook.EPLMethodInvocationContext;
import com.espertech.esper.client.util.TimePeriod;
import com.espertech.esper.codegen.base.CodegenBlock;
import com.espertech.esper.codegen.base.CodegenClassScope;
import com.espertech.esper.codegen.base.CodegenMethodScope;
import com.espertech.esper.codegen.model.expression.CodegenExpression;
import com.espertech.esper.collection.Pair;
import com.espertech.esper.core.context.util.ContextPropertyRegistry;
import com.espertech.esper.core.service.ExprEvaluatorContextStatement;
import com.espertech.esper.core.service.StatementContext;
import com.espertech.esper.core.start.EPStatementStartMethodHelperSubselect;
import com.espertech.esper.epl.core.engineimport.EngineImportException;
import com.espertech.esper.epl.core.engineimport.EngineImportService;
import com.espertech.esper.epl.core.engineimport.EngineImportSingleRowDesc;
import com.espertech.esper.epl.core.engineimport.EngineImportUndefinedException;
import com.espertech.esper.epl.core.select.BindProcessorEvaluatorStreamTable;
import com.espertech.esper.epl.core.streamtype.StreamTypeService;
import com.espertech.esper.epl.core.streamtype.StreamTypeServiceImpl;
import com.espertech.esper.epl.declexpr.ExprDeclaredNode;
import com.espertech.esper.epl.enummethod.dot.EnumMethodEnum;
import com.espertech.esper.epl.enummethod.dot.ExprDeclaredOrLambdaNode;
import com.espertech.esper.epl.enummethod.dot.ExprLambdaGoesNode;
import com.espertech.esper.epl.expression.baseagg.ExprAggregateNode;
import com.espertech.esper.epl.expression.baseagg.ExprAggregateNodeUtil;
import com.espertech.esper.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.codegen.base.CodegenMethodNode;
import com.espertech.esper.epl.expression.codegen.ExprNodeCompiler;
import com.espertech.esper.epl.expression.dot.ExprDotNodeImpl;
import com.espertech.esper.epl.expression.funcs.ExprPlugInSingleRowNode;
import com.espertech.esper.epl.expression.methodagg.ExprPlugInAggNode;
import com.espertech.esper.epl.expression.ops.ExprAndNode;
import com.espertech.esper.epl.expression.ops.ExprAndNodeImpl;
import com.espertech.esper.epl.expression.ops.ExprEqualsNode;
import com.espertech.esper.epl.expression.subquery.ExprSubselectNode;
import com.espertech.esper.epl.expression.table.ExprTableAccessNode;
import com.espertech.esper.epl.expression.time.ExprTimePeriod;
import com.espertech.esper.epl.expression.visitor.*;
import com.espertech.esper.epl.join.hint.ExcludePlanHint;
import com.espertech.esper.epl.join.plan.FilterExprAnalyzer;
import com.espertech.esper.epl.join.plan.QueryGraph;
import com.espertech.esper.epl.spec.ExpressionScriptProvided;
import com.espertech.esper.epl.spec.OnTriggerSetAssignment;
import com.espertech.esper.epl.table.mgmt.TableMetadata;
import com.espertech.esper.epl.table.mgmt.TableService;
import com.espertech.esper.event.EventAdapterService;
import com.espertech.esper.event.EventBeanUtility;
import com.espertech.esper.schedule.ScheduleParameterException;
import com.espertech.esper.schedule.ScheduleSpec;
import com.espertech.esper.schedule.ScheduleSpecUtil;
import com.espertech.esper.util.CollectionUtil;
import com.espertech.esper.util.JavaClassHelper;
import com.espertech.esper.util.SimpleNumberCoercer;
import com.espertech.esper.util.SimpleNumberCoercerFactory;
import net.sf.cglib.reflect.FastClass;
import net.sf.cglib.reflect.FastMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.StringWriter;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.util.*;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.*;

public class ExprNodeUtility {

    private static final Logger log = LoggerFactory.getLogger(ExprNodeUtility.class);

    public static final ExprNode[] EMPTY_EXPR_ARRAY = new ExprNode[0];
    public static final ExprDeclaredNode[] EMPTY_DECLARED_ARR = new ExprDeclaredNode[0];
    public static final ExpressionScriptProvided[] EMPTY_SCRIPTS = new ExpressionScriptProvided[0];

    public static Object evaluateValidationTimeNoStreams(ExprEvaluator evaluator, ExprEvaluatorContext context, String expressionName) throws ExprValidationException {
        try {
            return evaluator.evaluate(null, true, context);
        } catch (EPException ex) {
            throw new ExprValidationException("Invalid " + expressionName + " expression: " + ex.getMessage(), ex);
        } catch (RuntimeException ex) {
            log.warn("Invalid " + expressionName + " expression evaluation: {}", ex.getMessage(), ex);
            throw new ExprValidationException("Invalid " + expressionName + " expression");
        }
    }

    public static boolean deepEqualsIsSubset(ExprNode[] subset, ExprNode[] superset) {
        for (ExprNode subsetNode : subset) {
            boolean found = false;
            for (ExprNode supersetNode : superset) {
                if (deepEquals(subsetNode, supersetNode, false)) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                return false;
            }
        }
        return true;
    }

    public static boolean deepEqualsIgnoreDupAndOrder(ExprNode[] setOne, ExprNode[] setTwo) {
        if ((setOne.length == 0 && setTwo.length != 0) || (setOne.length != 0 && setTwo.length == 0)) {
            return false;
        }

        // find set-one expressions in set two
        boolean[] foundTwo = new boolean[setTwo.length];
        for (ExprNode one : setOne) {
            boolean found = false;
            for (int i = 0; i < setTwo.length; i++) {
                if (deepEquals(one, setTwo[i], false)) {
                    found = true;
                    foundTwo[i] = true;
                }
            }
            if (!found) {
                return false;
            }
        }

        // find any remaining set-two expressions in set one
        for (int i = 0; i < foundTwo.length; i++) {
            if (foundTwo[i]) {
                continue;
            }
            for (ExprNode one : setOne) {
                if (deepEquals(one, setTwo[i], false)) {
                    break;
                }
            }
            return false;
        }
        return true;
    }

    public static Map<ExprDeclaredNode, List<ExprDeclaredNode>> getDeclaredExpressionCallHierarchy(ExprDeclaredNode[] declaredExpressions) {
        ExprNodeSubselectDeclaredDotVisitor visitor = new ExprNodeSubselectDeclaredDotVisitor();
        Map<ExprDeclaredNode, List<ExprDeclaredNode>> calledToCallerMap = new HashMap<ExprDeclaredNode, List<ExprDeclaredNode>>();
        for (ExprDeclaredNode node : declaredExpressions) {
            visitor.reset();
            node.accept(visitor);
            for (ExprDeclaredNode called : visitor.getDeclaredExpressions()) {
                if (called == node) {
                    continue;
                }
                List<ExprDeclaredNode> callers = calledToCallerMap.get(called);
                if (callers == null) {
                    callers = new ArrayList<ExprDeclaredNode>(2);
                    calledToCallerMap.put(called, callers);
                }
                callers.add(node);
            }
            if (!calledToCallerMap.containsKey(node)) {
                calledToCallerMap.put(node, Collections.<ExprDeclaredNode>emptyList());
            }
        }
        return calledToCallerMap;
    }

    public static String toExpressionStringMinPrecedenceSafe(ExprNode node) {
        try {
            StringWriter writer = new StringWriter();
            node.toEPL(writer, ExprPrecedenceEnum.MINIMUM);
            return writer.toString();
        } catch (RuntimeException ex) {
            log.debug("Failed to render expression text: " + ex.getMessage(), ex);
            return "";
        }
    }

    public static String[] toExpressionStringMinPrecedenceAsArray(ExprNode[] nodes) {
        String[] expressions = new String[nodes.length];
        for (int i = 0; i < expressions.length; i++) {
            StringWriter writer = new StringWriter();
            nodes[i].toEPL(writer, ExprPrecedenceEnum.MINIMUM);
            expressions[i] = writer.toString();
        }
        return expressions;
    }

    public static String toExpressionStringMinPrecedenceAsList(ExprNode[] nodes) {
        StringWriter writer = new StringWriter();
        toExpressionStringMinPrecedenceAsList(nodes, writer);
        return writer.toString();
    }

    public static void toExpressionStringMinPrecedenceAsList(ExprNode[] nodes, StringWriter writer) {
        String delimiter = "";
        for (ExprNode node : nodes) {
            writer.append(delimiter);
            node.toEPL(writer, ExprPrecedenceEnum.MINIMUM);
            delimiter = ",";
        }
    }

    public static Pair<String, ExprNode> checkGetAssignmentToProp(ExprNode node) {
        if (!(node instanceof ExprEqualsNode)) {
            return null;
        }
        ExprEqualsNode equals = (ExprEqualsNode) node;
        if (!(equals.getChildNodes()[0] instanceof ExprIdentNode)) {
            return null;
        }
        ExprIdentNode identNode = (ExprIdentNode) equals.getChildNodes()[0];
        return new Pair<String, ExprNode>(identNode.getFullUnresolvedName(), equals.getChildNodes()[1]);
    }

    public static Pair<String, ExprNode> checkGetAssignmentToVariableOrProp(ExprNode node)
            throws ExprValidationException {
        Pair<String, ExprNode> prop = checkGetAssignmentToProp(node);
        if (prop != null) {
            return prop;
        }
        if (!(node instanceof ExprEqualsNode)) {
            return null;
        }
        ExprEqualsNode equals = (ExprEqualsNode) node;

        if (equals.getChildNodes()[0] instanceof ExprVariableNode) {
            ExprVariableNode variableNode = (ExprVariableNode) equals.getChildNodes()[0];
            return new Pair<String, ExprNode>(variableNode.getVariableNameWithSubProp(), equals.getChildNodes()[1]);
        }
        if (equals.getChildNodes()[0] instanceof ExprTableAccessNode) {
            throw new ExprValidationException("Table access expression not allowed on the left hand side, please remove the table prefix");
        }
        return null;
    }

    public static void applyFilterExpressionsIterable(Iterable<EventBean> iterable, List<ExprNode> filterExpressions, ExprEvaluatorContext exprEvaluatorContext, Collection<EventBean> eventsInWindow) {
        ExprEvaluator[] evaluators = ExprNodeUtility.getEvaluatorsNoCompile(filterExpressions);
        EventBean[] events = new EventBean[1];
        for (EventBean theEvent : iterable) {
            events[0] = theEvent;
            boolean add = true;
            for (ExprEvaluator filter : evaluators) {
                Object result = filter.evaluate(events, true, exprEvaluatorContext);
                if ((result == null) || (!((Boolean) result))) {
                    add = false;
                    break;
                }
            }
            if (add) {
                eventsInWindow.add(events[0]);
            }
        }
    }

    public static void applyFilterExpressionIterable(Iterator<EventBean> iterator, ExprEvaluator filterExpression, ExprEvaluatorContext exprEvaluatorContext, Collection<EventBean> eventsInWindow) {
        EventBean[] events = new EventBean[1];
        for (; iterator.hasNext(); ) {
            events[0] = iterator.next();
            Object result = filterExpression.evaluate(events, true, exprEvaluatorContext);
            if ((result == null) || (!((Boolean) result))) {
                continue;
            }
            eventsInWindow.add(events[0]);
        }
    }

    public static ExprNode connectExpressionsByLogicalAnd(List<ExprNode> nodes, ExprNode optionalAdditionalFilter) {
        if (nodes.isEmpty()) {
            return optionalAdditionalFilter;
        }
        if (optionalAdditionalFilter == null) {
            if (nodes.size() == 1) {
                return nodes.get(0);
            }
            return connectExpressionsByLogicalAnd(nodes);
        }
        if (nodes.size() == 1) {
            return connectExpressionsByLogicalAnd(Arrays.asList(nodes.get(0), optionalAdditionalFilter));
        }
        ExprAndNode andNode = connectExpressionsByLogicalAnd(nodes);
        andNode.addChildNode(optionalAdditionalFilter);
        return andNode;
    }

    public static ExprAndNode connectExpressionsByLogicalAnd(Collection<ExprNode> nodes) {
        if (nodes.size() < 2) {
            throw new IllegalArgumentException("Invalid empty or 1-element list of nodes");
        }
        ExprAndNode andNode = new ExprAndNodeImpl();
        for (ExprNode node : nodes) {
            andNode.addChildNode(node);
        }
        return andNode;
    }

    public static ExprNode connectExpressionsByLogicalAndWhenNeeded(Collection<ExprNode> nodes) {
        if (nodes.isEmpty()) {
            return null;
        }
        if (nodes.size() == 1) {
            return nodes.iterator().next();
        }
        return connectExpressionsByLogicalAnd(nodes);
    }

    /**
     * Walk expression returning properties used.
     *
     * @param exprNode            to walk
     * @param visitAggregateNodes true to visit aggregation nodes
     * @return list of props
     */
    public static List<Pair<Integer, String>> getExpressionProperties(ExprNode exprNode, boolean visitAggregateNodes) {
        ExprNodeIdentifierVisitor visitor = new ExprNodeIdentifierVisitor(visitAggregateNodes);
        exprNode.accept(visitor);
        return visitor.getExprProperties();
    }

    public static boolean isConstantValueExpr(ExprNode exprNode) {
        if (!(exprNode instanceof ExprConstantNode)) {
            return false;
        }
        ExprConstantNode constantNode = (ExprConstantNode) exprNode;
        return constantNode.isConstantValue();
    }

    /**
     * Validates the expression node subtree that has this
     * node as root. Some of the nodes of the tree, including the
     * root, might be replaced in the process.
     *
     * @param origin            validate origin
     * @param exprNode          node
     * @param validationContext context
     * @return the root node of the validated subtree, possibly
     * different than the root node of the unvalidated subtree
     * @throws ExprValidationException when the validation fails
     */
    public static ExprNode getValidatedSubtree(ExprNodeOrigin origin, ExprNode exprNode, ExprValidationContext validationContext) throws ExprValidationException {
        if (exprNode instanceof ExprLambdaGoesNode) {
            return exprNode;
        }

        try {
            return getValidatedSubtreeInternal(exprNode, validationContext, true);
        } catch (ExprValidationException ex) {
            try {
                String text;
                if (exprNode instanceof ExprSubselectNode) {
                    ExprSubselectNode subselect = (ExprSubselectNode) exprNode;
                    text = EPStatementStartMethodHelperSubselect.getSubqueryInfoText(subselect.getSubselectNumber() - 1, subselect);
                } else {
                    text = ExprNodeUtility.toExpressionStringMinPrecedenceSafe(exprNode);
                    if (text.length() > 40) {
                        String shortened = text.substring(0, 35);
                        text = shortened + "...(" + text.length() + " chars)";
                    }
                    text = "'" + text + "'";
                }
                throw new ExprValidationException("Failed to validate " +
                        origin.getClauseName() +
                        " expression " +
                        text + ": " +
                        ex.getMessage(), ex);
            } catch (RuntimeException rtex) {
                log.debug("Failed to render nice validation message text: " + rtex.getMessage(), rtex);
                throw ex;
            }
        }
    }

    public static void getValidatedSubtree(ExprNodeOrigin origin, ExprNode[] exprNode, ExprValidationContext validationContext) throws ExprValidationException {
        if (exprNode == null) {
            return;
        }
        for (int i = 0; i < exprNode.length; i++) {
            exprNode[i] = getValidatedSubtree(origin, exprNode[i], validationContext);
        }
    }

    public static void getValidatedSubtree(ExprNodeOrigin origin, ExprNode[][] exprNode, ExprValidationContext validationContext) throws ExprValidationException {
        if (exprNode == null) {
            return;
        }
        for (ExprNode[] anExprNode : exprNode) {
            getValidatedSubtree(origin, anExprNode, validationContext);
        }
    }

    public static ExprNode getValidatedAssignment(OnTriggerSetAssignment assignment, ExprValidationContext validationContext) throws ExprValidationException {
        Pair<String, ExprNode> strictAssignment = checkGetAssignmentToVariableOrProp(assignment.getExpression());
        if (strictAssignment != null) {
            ExprNode validatedRightSide = getValidatedSubtreeInternal(strictAssignment.getSecond(), validationContext, true);
            assignment.getExpression().setChildNode(1, validatedRightSide);
            return assignment.getExpression();
        } else {
            return getValidatedSubtreeInternal(assignment.getExpression(), validationContext, true);
        }
    }

    private static ExprNode getValidatedSubtreeInternal(ExprNode exprNode, ExprValidationContext validationContext, boolean isTopLevel) throws ExprValidationException {
        ExprNode result = exprNode;
        if (exprNode instanceof ExprLambdaGoesNode) {
            return exprNode;
        }

        for (int i = 0; i < exprNode.getChildNodes().length; i++) {
            ExprNode childNode = exprNode.getChildNodes()[i];
            if (childNode instanceof ExprDeclaredOrLambdaNode) {
                ExprDeclaredOrLambdaNode node = (ExprDeclaredOrLambdaNode) childNode;
                if (node.validated()) {
                    continue;
                }
            }
            ExprNode childNodeValidated = getValidatedSubtreeInternal(childNode, validationContext, false);
            exprNode.setChildNode(i, childNodeValidated);
        }

        try {
            ExprNode optionalReplacement = exprNode.validate(validationContext);
            if (optionalReplacement != null) {
                return getValidatedSubtreeInternal(optionalReplacement, validationContext, isTopLevel);
            }
        } catch (ExprValidationException e) {
            if (exprNode instanceof ExprIdentNode) {
                ExprIdentNode identNode = (ExprIdentNode) exprNode;
                try {
                    result = resolveStaticMethodOrField(identNode, e, validationContext);
                } catch (ExprValidationException ex) {
                    e = ex;
                    result = resolveAsStreamName(identNode, e, validationContext);
                }
            } else {
                throw e;
            }
        }

        // For top-level expressions check if we perform audit
        if (isTopLevel) {
            if (validationContext.isExpressionAudit()) {
                return (ExprNode) ExprNodeProxy.newInstance(validationContext.getStreamTypeService().getEngineURIQualifier(), validationContext.getStatementName(), result);
            }
        } else {
            if (validationContext.isExpressionNestedAudit() && !(result instanceof ExprIdentNode) && !(ExprNodeUtility.isConstantValueExpr(result))) {
                return (ExprNode) ExprNodeProxy.newInstance(validationContext.getStreamTypeService().getEngineURIQualifier(), validationContext.getStatementName(), result);
            }
        }

        return result;
    }

    private static ExprNode resolveAsStreamName(ExprIdentNode identNode, ExprValidationException existingException, ExprValidationContext validationContext)
            throws ExprValidationException {
        ExprStreamUnderlyingNode exprStream = new ExprStreamUnderlyingNodeImpl(identNode.getUnresolvedPropertyName(), false);

        try {
            exprStream.validate(validationContext);
        } catch (ExprValidationException ex) {
            throw existingException;
        }

        return exprStream;
    }

    // Since static method calls such as "Class.method('a')" and mapped properties "Stream.property('key')"
    // look the same, however as the validation could not resolve "Stream.property('key')" before calling this method,
    // this method tries to resolve the mapped property as a static method.
    // Assumes that this is an ExprIdentNode.
    private static ExprNode resolveStaticMethodOrField(ExprIdentNode identNode, ExprValidationException propertyException, ExprValidationContext validationContext)
            throws ExprValidationException {
        // Reconstruct the original string
        StringBuilder mappedProperty = new StringBuilder(identNode.getUnresolvedPropertyName());
        if (identNode.getStreamOrPropertyName() != null) {
            mappedProperty.insert(0, identNode.getStreamOrPropertyName() + '.');
        }

        // Parse the mapped property format into a class name, method and single string parameter
        MappedPropertyParseResult parse = parseMappedProperty(mappedProperty.toString());
        if (parse == null) {
            ExprConstantNode constNode = resolveIdentAsEnumConst(mappedProperty.toString(), validationContext.getEngineImportService());
            if (constNode == null) {
                throw propertyException;
            } else {
                return constNode;
            }
        }

        // If there is a class name, assume a static method is possible.
        if (parse.getClassName() != null) {
            List<ExprNode> parameters = Collections.singletonList((ExprNode) new ExprConstantNodeImpl(parse.getArgString()));
            List<ExprChainedSpec> chain = new ArrayList<ExprChainedSpec>();
            chain.add(new ExprChainedSpec(parse.getClassName(), Collections.<ExprNode>emptyList(), false));
            chain.add(new ExprChainedSpec(parse.getMethodName(), parameters, false));
            ExprNode result = new ExprDotNodeImpl(chain, validationContext.getEngineImportService().isDuckType(), validationContext.getEngineImportService().isUdfCache());

            // Validate
            try {
                result.validate(validationContext);
            } catch (ExprValidationException e) {
                throw new ExprValidationException("Failed to resolve enumeration method, date-time method or mapped property '" + mappedProperty + "': " + e.getMessage());
            }

            return result;
        }

        // There is no class name, try a single-row function
        String functionName = parse.getMethodName();
        try {
            Pair<Class, EngineImportSingleRowDesc> classMethodPair = validationContext.getEngineImportService().resolveSingleRow(functionName);
            List<ExprNode> parameters = Collections.singletonList((ExprNode) new ExprConstantNodeImpl(parse.getArgString()));
            List<ExprChainedSpec> chain = Collections.singletonList(new ExprChainedSpec(classMethodPair.getSecond().getMethodName(), parameters, false));
            ExprNode result = new ExprPlugInSingleRowNode(functionName, classMethodPair.getFirst(), chain, classMethodPair.getSecond());

            // Validate
            try {
                result.validate(validationContext);
            } catch (RuntimeException e) {
                throw new ExprValidationException("Plug-in aggregation function '" + parse.getMethodName() + "' failed validation: " + e.getMessage());
            }

            return result;
        } catch (EngineImportUndefinedException e) {
            // Not an single-row function
        } catch (EngineImportException e) {
            throw new IllegalStateException("Error resolving single-row function: " + e.getMessage(), e);
        }

        // Try an aggregation function factory
        try {
            AggregationFunctionFactory aggregationFactory = validationContext.getEngineImportService().resolveAggregationFactory(parse.getMethodName());
            ExprNode result = new ExprPlugInAggNode(false, aggregationFactory, parse.getMethodName());
            result.addChildNode(new ExprConstantNodeImpl(parse.getArgString()));

            // Validate
            try {
                result.validate(validationContext);
            } catch (RuntimeException e) {
                throw new ExprValidationException("Plug-in aggregation function '" + parse.getMethodName() + "' failed validation: " + e.getMessage());
            }

            return result;
        } catch (EngineImportUndefinedException e) {
            // Not an aggregation function
        } catch (EngineImportException e) {
            throw new IllegalStateException("Error resolving aggregation: " + e.getMessage(), e);
        }

        // absolutely cannot be resolved
        throw propertyException;
    }

    private static ExprConstantNode resolveIdentAsEnumConst(String constant, EngineImportService engineImportService)
            throws ExprValidationException {
        Object enumValue = JavaClassHelper.resolveIdentAsEnumConst(constant, engineImportService, false);
        if (enumValue != null) {
            return new ExprConstantNodeImpl(enumValue);
        }
        return null;
    }

    /**
     * Parse the mapped property into classname, method and string argument.
     * Mind this has been parsed already and is a valid mapped property.
     *
     * @param property is the string property to be passed as a static method invocation
     * @return descriptor object
     */
    public static MappedPropertyParseResult parseMappedProperty(String property) {
        // get argument
        int indexFirstDoubleQuote = property.indexOf("\"");
        int indexFirstSingleQuote = property.indexOf("'");
        int startArg;
        if ((indexFirstSingleQuote == -1) && (indexFirstDoubleQuote == -1)) {
            return null;
        }
        if ((indexFirstSingleQuote != -1) && (indexFirstDoubleQuote != -1)) {
            if (indexFirstSingleQuote < indexFirstDoubleQuote) {
                startArg = indexFirstSingleQuote;
            } else {
                startArg = indexFirstDoubleQuote;
            }
        } else if (indexFirstSingleQuote != -1) {
            startArg = indexFirstSingleQuote;
        } else {
            startArg = indexFirstDoubleQuote;
        }

        int indexLastDoubleQuote = property.lastIndexOf("\"");
        int indexLastSingleQuote = property.lastIndexOf("'");
        int endArg;
        if ((indexLastSingleQuote == -1) && (indexLastDoubleQuote == -1)) {
            return null;
        }
        if ((indexLastSingleQuote != -1) && (indexLastDoubleQuote != -1)) {
            if (indexLastSingleQuote > indexLastDoubleQuote) {
                endArg = indexLastSingleQuote;
            } else {
                endArg = indexLastDoubleQuote;
            }
        } else if (indexLastSingleQuote != -1) {
            if (indexLastSingleQuote == indexFirstSingleQuote) {
                return null;
            }
            endArg = indexLastSingleQuote;
        } else {
            if (indexLastDoubleQuote == indexFirstDoubleQuote) {
                return null;
            }
            endArg = indexLastDoubleQuote;
        }
        String argument = property.substring(startArg + 1, endArg);

        // get method
        String[] splitDots = property.split("[\\.]");
        if (splitDots.length == 0) {
            return null;
        }

        // find which element represents the method, its the element with the parenthesis
        int indexMethod = -1;
        for (int i = 0; i < splitDots.length; i++) {
            if (splitDots[i].contains("(")) {
                indexMethod = i;
                break;
            }
        }
        if (indexMethod == -1) {
            return null;
        }

        String method = splitDots[indexMethod];
        int indexParan = method.indexOf("(");
        method = method.substring(0, indexParan);
        if (method.length() == 0) {
            return null;
        }

        if (splitDots.length == 1) {
            // no class name
            return new MappedPropertyParseResult(null, method, argument);
        }


        // get class
        StringBuilder clazz = new StringBuilder();
        for (int i = 0; i < indexMethod; i++) {
            if (i > 0) {
                clazz.append('.');
            }
            clazz.append(splitDots[i]);
        }

        return new MappedPropertyParseResult(clazz.toString(), method, argument);
    }

    public static boolean isAllConstants(List<ExprNode> parameters) {
        for (ExprNode node : parameters) {
            if (!node.isConstantResult()) {
                return false;
            }
        }
        return true;
    }

    public static ExprIdentNode getExprIdentNode(EventType[] typesPerStream, int streamId, String property) {
        return new ExprIdentNodeImpl(typesPerStream[streamId], property, streamId);
    }

    public static Class[] getExprResultTypes(List<ExprNode> expressions) {
        Class[] returnTypes = new Class[expressions.size()];
        for (int i = 0; i < expressions.size(); i++) {
            returnTypes[i] = expressions.get(i).getForge().getEvaluationType();
        }
        return returnTypes;
    }

    public static ExprNodeUtilMethodDesc resolveMethodAllowWildcardAndStream(String className,
                                                                             Class optionalClass,
                                                                             String methodName,
                                                                             List<ExprNode> parameters,
                                                                             EngineImportService engineImportService,
                                                                             EventAdapterService eventAdapterService,
                                                                             int statementId,
                                                                             boolean allowWildcard,
                                                                             final EventType wildcardType,
                                                                             ExprNodeUtilResolveExceptionHandler exceptionHandler,
                                                                             String functionName,
                                                                             TableService tableService,
                                                                             String engineURI) throws ExprValidationException {
        Class[] paramTypes = new Class[parameters.size()];
        ExprForge[] childForges = new ExprForge[parameters.size()];
        int count = 0;
        boolean[] allowEventBeanType = new boolean[parameters.size()];
        boolean[] allowEventBeanCollType = new boolean[parameters.size()];
        ExprForge[] childEvalsEventBeanReturnTypesForges = new ExprForge[parameters.size()];
        boolean allConstants = true;
        for (ExprNode childNode : parameters) {
            if (!EnumMethodEnum.isEnumerationMethod(methodName) && childNode instanceof ExprLambdaGoesNode) {
                throw new ExprValidationException("Unexpected lambda-expression encountered as parameter to UDF or static method '" + methodName + "'");
            }
            if (childNode instanceof ExprWildcard) {
                if (wildcardType == null || !allowWildcard) {
                    throw new ExprValidationException("Failed to resolve wildcard parameter to a given event type");
                }
                childForges[count] = new ExprNodeUtilExprStreamNumUnd(0, wildcardType.getUnderlyingType());
                childEvalsEventBeanReturnTypesForges[count] = new ExprNodeUtilExprStreamNumEvent(0);
                paramTypes[count] = wildcardType.getUnderlyingType();
                allowEventBeanType[count] = true;
                allConstants = false;
                count++;
                continue;
            }
            if (childNode instanceof ExprStreamUnderlyingNode) {
                ExprStreamUnderlyingNode und = (ExprStreamUnderlyingNode) childNode;
                TableMetadata tableMetadata = tableService.getTableMetadataFromEventType(und.getEventType());
                if (tableMetadata == null) {
                    childForges[count] = childNode.getForge();
                    childEvalsEventBeanReturnTypesForges[count] = new ExprNodeUtilExprStreamNumEvent(und.getStreamId());
                } else {
                    childForges[count] = new BindProcessorEvaluatorStreamTable(und.getStreamId(), und.getEventType().getUnderlyingType(), tableMetadata);
                    childEvalsEventBeanReturnTypesForges[count] = new ExprNodeUtilExprStreamNumEventTable(und.getStreamId(), tableMetadata);
                }
                paramTypes[count] = childForges[count].getEvaluationType();
                allowEventBeanType[count] = true;
                allConstants = false;
                count++;
                continue;
            }
            if (childNode.getForge() instanceof ExprEnumerationForge) {
                ExprEnumerationForge enumeration = (ExprEnumerationForge) childNode.getForge();
                EventType eventType = enumeration.getEventTypeSingle(eventAdapterService, statementId);
                childForges[count] = childNode.getForge();
                paramTypes[count] = childForges[count].getEvaluationType();
                allConstants = false;
                if (eventType != null) {
                    childEvalsEventBeanReturnTypesForges[count] = new ExprNodeUtilExprStreamNumEnumSingleForge(enumeration);
                    allowEventBeanType[count] = true;
                    count++;
                    continue;
                }
                EventType eventTypeColl = enumeration.getEventTypeCollection(eventAdapterService, statementId);
                if (eventTypeColl != null) {
                    childEvalsEventBeanReturnTypesForges[count] = new ExprNodeUtilExprStreamNumEnumCollForge(enumeration);
                    allowEventBeanCollType[count] = true;
                    count++;
                    continue;
                }
            }

            paramTypes[count] = childNode.getForge().getEvaluationType();
            childForges[count] = childNode.getForge();
            count++;
            if (!(childNode.isConstantResult())) {
                allConstants = false;
            }
        }

        // Try to resolve the method
        final FastMethod staticMethod;
        Method method;
        try {
            if (optionalClass != null) {
                method = engineImportService.resolveMethod(optionalClass, methodName, paramTypes, allowEventBeanType, allowEventBeanCollType);
            } else {
                method = engineImportService.resolveMethodOverloadChecked(className, methodName, paramTypes, allowEventBeanType, allowEventBeanCollType);
            }
            FastClass declaringClass = FastClass.create(engineImportService.getFastClassClassLoader(method.getDeclaringClass()), method.getDeclaringClass());
            staticMethod = declaringClass.getMethod(method);
        } catch (Exception e) {
            throw exceptionHandler.handle(e);
        }

        // rewrite those evaluator that should return the event itself
        if (CollectionUtil.isAnySet(allowEventBeanType)) {
            for (int i = 0; i < parameters.size(); i++) {
                if (allowEventBeanType[i] && method.getParameterTypes()[i] == EventBean.class) {
                    childForges[i] = childEvalsEventBeanReturnTypesForges[i];
                }
            }
        }

        // rewrite those evaluators that should return the event collection
        if (CollectionUtil.isAnySet(allowEventBeanCollType)) {
            for (int i = 0; i < parameters.size(); i++) {
                if (allowEventBeanCollType[i] && method.getParameterTypes()[i] == Collection.class) {
                    childForges[i] = childEvalsEventBeanReturnTypesForges[i];
                }
            }
        }

        // add an evaluator if the method expects a context object
        if (!method.isVarArgs() && method.getParameterTypes().length > 0 &&
                method.getParameterTypes()[method.getParameterTypes().length - 1] == EPLMethodInvocationContext.class) {
            ExprNodeUtilExprMethodContext node = new ExprNodeUtilExprMethodContext(engineURI, functionName, eventAdapterService);
            childForges = (ExprForge[]) CollectionUtil.arrayExpandAddSingle(childForges, node);
        }

        // handle varargs
        if (method.isVarArgs()) {
            // handle context parameter
            int numMethodParams = method.getParameterTypes().length;
            if (numMethodParams > 1 && method.getParameterTypes()[numMethodParams - 2] == EPLMethodInvocationContext.class) {
                ExprForge[] rewrittenForges = new ExprForge[childForges.length + 1];
                System.arraycopy(childForges, 0, rewrittenForges, 0, numMethodParams - 2);
                ExprNodeUtilExprMethodContext node = new ExprNodeUtilExprMethodContext(engineURI, functionName, eventAdapterService);
                rewrittenForges[numMethodParams - 2] = node;
                System.arraycopy(childForges, numMethodParams - 2, rewrittenForges, numMethodParams - 1, childForges.length - (numMethodParams - 2));
                childForges = rewrittenForges;
            }

            Pair<ExprForge[], ExprEvaluator[]> pair = makeVarargArrayEval(method, childForges);
            childForges = pair.getFirst();
        }

        return new ExprNodeUtilMethodDesc(allConstants, childForges, method, staticMethod);
    }

    public static void validatePlainExpression(ExprNodeOrigin origin, ExprNode expression) throws ExprValidationException {
        ExprNodeSummaryVisitor summaryVisitor = new ExprNodeSummaryVisitor();
        validatePlainExpression(origin, expression, summaryVisitor);
    }

    public static void validatePlainExpression(ExprNodeOrigin origin, ExprNode[] expressions) throws ExprValidationException {
        ExprNodeSummaryVisitor summaryVisitor = new ExprNodeSummaryVisitor();
        for (ExprNode expression : expressions) {
            validatePlainExpression(origin, expression, summaryVisitor);
        }
    }

    private static void validatePlainExpression(ExprNodeOrigin origin, ExprNode expression, ExprNodeSummaryVisitor summaryVisitor) throws ExprValidationException {
        expression.accept(summaryVisitor);
        if (summaryVisitor.isHasAggregation() || summaryVisitor.isHasSubselect() || summaryVisitor.isHasStreamSelect() || summaryVisitor.isHasPreviousPrior()) {
            String text = toExpressionStringMinPrecedenceSafe(expression);
            throw new ExprValidationException("Invalid " + origin.getClauseName() + " expression '" + text + "': Aggregation, sub-select, previous or prior functions are not supported in this context");
        }
    }

    public static ExprNode validateSimpleGetSubtree(ExprNodeOrigin origin, ExprNode expression, StatementContext statementContext, EventType optionalEventType, boolean allowBindingConsumption)
            throws ExprValidationException {

        ExprNodeUtility.validatePlainExpression(origin, expression);

        StreamTypeServiceImpl streamTypes;
        if (optionalEventType != null) {
            streamTypes = new StreamTypeServiceImpl(optionalEventType, null, true, statementContext.getEngineURI());
        } else {
            streamTypes = new StreamTypeServiceImpl(statementContext.getEngineURI(), false);
        }

        ExprValidationContext validationContext = new ExprValidationContext(streamTypes, statementContext.getEngineImportService(), statementContext.getStatementExtensionServicesContext(), null, statementContext.getSchedulingService(), statementContext.getVariableService(), statementContext.getTableService(), new ExprEvaluatorContextStatement(statementContext, false), statementContext.getEventAdapterService(), statementContext.getStatementName(), statementContext.getStatementId(), statementContext.getAnnotations(), statementContext.getContextDescriptor(), false, false, allowBindingConsumption, false, null, false);
        return ExprNodeUtility.getValidatedSubtree(origin, expression, validationContext);
    }

    public static ExprValidationContext getExprValidationContextStatementOnly(StatementContext statementContext) {
        return new ExprValidationContext(new StreamTypeServiceImpl(statementContext.getEngineURI(), false), statementContext.getEngineImportService(), statementContext.getStatementExtensionServicesContext(), null, statementContext.getSchedulingService(), statementContext.getVariableService(), statementContext.getTableService(), new ExprEvaluatorContextStatement(statementContext, false), statementContext.getEventAdapterService(), statementContext.getStatementName(), statementContext.getStatementId(), statementContext.getAnnotations(), statementContext.getContextDescriptor(), false, false, false, false, null, false);
    }

    public static Set<String> getPropertyNamesIfAllProps(ExprNode[] expressions) {
        for (ExprNode expression : expressions) {
            if (!(expression instanceof ExprIdentNode)) {
                return null;
            }
        }
        Set<String> uniquePropertyNames = new HashSet<String>();
        for (ExprNode expression : expressions) {
            ExprIdentNode identNode = (ExprIdentNode) expression;
            uniquePropertyNames.add(identNode.getUnresolvedPropertyName());
        }
        return uniquePropertyNames;
    }

    public static String[] toExpressionStringsMinPrecedence(ExprNode[] expressions) {
        String[] texts = new String[expressions.length];
        for (int i = 0; i < expressions.length; i++) {
            texts[i] = toExpressionStringMinPrecedenceSafe(expressions[i]);
        }
        return texts;
    }

    public static List<Pair<ExprNode, ExprNode>> findExpression(ExprNode selectExpression, ExprNode searchExpression) {
        List<Pair<ExprNode, ExprNode>> pairs = new ArrayList<Pair<ExprNode, ExprNode>>();
        if (deepEquals(selectExpression, searchExpression, false)) {
            pairs.add(new Pair<ExprNode, ExprNode>(null, selectExpression));
            return pairs;
        }
        findExpressionChildRecursive(selectExpression, searchExpression, pairs);
        return pairs;
    }

    private static void findExpressionChildRecursive(ExprNode parent, ExprNode searchExpression, List<Pair<ExprNode, ExprNode>> pairs) {
        for (ExprNode child : parent.getChildNodes()) {
            if (deepEquals(child, searchExpression, false)) {
                pairs.add(new Pair<ExprNode, ExprNode>(parent, child));
                continue;
            }
            findExpressionChildRecursive(child, searchExpression, pairs);
        }
    }

    public static void toExpressionStringParameterList(ExprNode[] childNodes, StringWriter buffer) {
        String delimiter = "";
        for (ExprNode childNode : childNodes) {
            buffer.append(delimiter);
            buffer.append(ExprNodeUtility.toExpressionStringMinPrecedenceSafe(childNode));
            delimiter = ",";
        }
    }

    public static void toExpressionStringWFunctionName(String functionName, ExprNode[] childNodes, StringWriter writer) {
        writer.append(functionName);
        writer.append("(");
        toExpressionStringParameterList(childNodes, writer);
        writer.append(')');
    }

    public static String[] getIdentResolvedPropertyNames(ExprNode[] nodes) {
        String[] propertyNames = new String[nodes.length];
        for (int i = 0; i < propertyNames.length; i++) {
            if (!(nodes[i] instanceof ExprIdentNode)) {
                throw new IllegalArgumentException("Expressions are not ident nodes");
            }
            propertyNames[i] = ((ExprIdentNode) nodes[i]).getResolvedPropertyName();
        }
        return propertyNames;
    }

    public static Class[] getExprResultTypes(ExprNode[] nodes) {
        Class[] types = new Class[nodes.length];
        for (int i = 0; i < types.length; i++) {
            types[i] = nodes[i].getForge().getEvaluationType();
        }
        return types;
    }

    public static ExprEvaluator makeUnderlyingEvaluator(final int streamNum, final Class resultType, TableMetadata tableMetadata) {
        if (tableMetadata != null) {
            return new ExprNodeUtilUnderlyingEvaluatorTable(streamNum, resultType, tableMetadata);
        }
        return new ExprNodeUtilUnderlyingEvaluator(streamNum, resultType);
    }

    public static boolean hasStreamSelect(List<ExprNode> exprNodes) {
        ExprNodeStreamSelectVisitor visitor = new ExprNodeStreamSelectVisitor(false);
        for (ExprNode node : exprNodes) {
            node.accept(visitor);
            if (visitor.hasStreamSelect()) {
                return true;
            }
        }
        return false;
    }

    public static void validateNoSpecialsGroupByExpressions(ExprNode[] groupByNodes) throws ExprValidationException {
        ExprNodeSubselectDeclaredDotVisitor visitorSubselects = new ExprNodeSubselectDeclaredDotVisitor();
        ExprNodeGroupingVisitorWParent visitorGrouping = new ExprNodeGroupingVisitorWParent();
        List<ExprAggregateNode> aggNodesInGroupBy = new ArrayList<ExprAggregateNode>(1);

        for (ExprNode groupByNode : groupByNodes) {

            // no subselects
            groupByNode.accept(visitorSubselects);
            if (visitorSubselects.getSubselects().size() > 0) {
                throw new ExprValidationException("Subselects not allowed within group-by");
            }

            // no special grouping-clauses
            groupByNode.accept(visitorGrouping);
            if (!visitorGrouping.getGroupingIdNodes().isEmpty()) {
                throw ExprGroupingIdNode.makeException("grouping_id");
            }
            if (!visitorGrouping.getGroupingNodes().isEmpty()) {
                throw ExprGroupingIdNode.makeException("grouping");
            }

            // no aggregations allowed
            ExprAggregateNodeUtil.getAggregatesBottomUp(groupByNode, aggNodesInGroupBy);
            if (!aggNodesInGroupBy.isEmpty()) {
                throw new ExprValidationException("Group-by expressions cannot contain aggregate functions");
            }
        }
    }

    public static Map<String, ExprNamedParameterNode> getNamedExpressionsHandleDups(List<ExprNode> parameters) throws ExprValidationException {
        Map<String, ExprNamedParameterNode> nameds = null;

        for (ExprNode node : parameters) {
            if (node instanceof ExprNamedParameterNode) {
                ExprNamedParameterNode named = (ExprNamedParameterNode) node;
                if (nameds == null) {
                    nameds = new HashMap<String, ExprNamedParameterNode>();
                }
                String lowerCaseName = named.getParameterName().toLowerCase(Locale.ENGLISH);
                if (nameds.containsKey(lowerCaseName)) {
                    throw new ExprValidationException("Duplicate parameter '" + lowerCaseName + "'");
                }
                nameds.put(lowerCaseName, named);
            }
        }
        if (nameds == null) {
            return Collections.emptyMap();
        }
        return nameds;
    }

    public static void validateNamed(Map<String, ExprNamedParameterNode> namedExpressions, String[] namedParameters) throws ExprValidationException {
        for (Map.Entry<String, ExprNamedParameterNode> entry : namedExpressions.entrySet()) {
            boolean found = false;
            for (String named : namedParameters) {
                if (named.equals(entry.getKey())) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                throw new ExprValidationException("Unexpected named parameter '" + entry.getKey() + "', expecting any of the following: " + CollectionUtil.toStringArray(namedParameters));
            }
        }
    }

    public static boolean validateNamedExpectType(ExprNamedParameterNode namedParameterNode, Class[] expectedTypes) throws ExprValidationException {
        if (namedParameterNode.getChildNodes().length != 1) {
            throw getNamedValidationException(namedParameterNode.getParameterName(), expectedTypes);
        }

        ExprNode childNode = namedParameterNode.getChildNodes()[0];
        Class returnType = JavaClassHelper.getBoxedType(childNode.getForge().getEvaluationType());

        boolean found = false;
        for (Class expectedType : expectedTypes) {
            if (expectedType == TimePeriod.class && childNode instanceof ExprTimePeriod) {
                found = true;
                break;
            }
            if (returnType == JavaClassHelper.getBoxedType(expectedType)) {
                found = true;
                break;
            }
        }

        if (found) {
            return namedParameterNode.getChildNodes()[0].isConstantResult();
        }
        throw getNamedValidationException(namedParameterNode.getParameterName(), expectedTypes);
    }

    private static ExprValidationException getNamedValidationException(String parameterName, Class[] expected) {
        String expectedType;
        if (expected.length == 1) {
            expectedType = "a " + JavaClassHelper.getSimpleNameForClass(expected[0]) + "-typed value";
        } else {
            StringWriter buf = new StringWriter();
            buf.append("any of the following types: ");
            String delimiter = "";
            for (Class clazz : expected) {
                buf.append(delimiter);
                buf.append(JavaClassHelper.getSimpleNameForClass(clazz));
                delimiter = ",";
            }
            expectedType = buf.toString();
        }
        String message = "Failed to validate named parameter '" + parameterName + "', expected a single expression returning " + expectedType;
        return new ExprValidationException(message);
    }

    public static ExprNode[] addExpression(ExprNode[] expressions, ExprNode expression) {
        ExprNode[] target = new ExprNode[expressions.length + 1];
        System.arraycopy(expressions, 0, target, 0, expressions.length);
        target[expressions.length] = expression;
        return target;
    }

    public static QueryGraph validateFilterGetQueryGraphSafe(ExprNode filterExpression, StatementContext statementContext, StreamTypeServiceImpl typeService) {
        ExcludePlanHint excludePlanHint = null;
        try {
            excludePlanHint = ExcludePlanHint.getHint(typeService.getStreamNames(), statementContext);
        } catch (ExprValidationException ex) {
            log.warn("Failed to consider exclude-plan hint: " + ex.getMessage(), ex);
        }

        QueryGraph queryGraph = new QueryGraph(1, excludePlanHint, false);
        validateFilterWQueryGraphSafe(queryGraph, filterExpression, statementContext, typeService);
        return queryGraph;
    }

    public static void validateFilterWQueryGraphSafe(QueryGraph queryGraph, ExprNode filterExpression, StatementContext statementContext, StreamTypeServiceImpl typeService) {
        try {
            ExprEvaluatorContextStatement evaluatorContextStmt = new ExprEvaluatorContextStatement(statementContext, false);
            ExprValidationContext validationContext = new ExprValidationContext(typeService, statementContext.getEngineImportService(), statementContext.getStatementExtensionServicesContext(), null, statementContext.getTimeProvider(), statementContext.getVariableService(), statementContext.getTableService(), evaluatorContextStmt, statementContext.getEventAdapterService(), statementContext.getStatementName(), statementContext.getStatementId(), statementContext.getAnnotations(), statementContext.getContextDescriptor(), false, false, true, false, null, true);
            ExprNode validated = ExprNodeUtility.getValidatedSubtree(ExprNodeOrigin.FILTER, filterExpression, validationContext);
            FilterExprAnalyzer.analyze(validated, queryGraph, false);
        } catch (Exception ex) {
            log.warn("Unexpected exception analyzing filterable expression '" + toExpressionStringMinPrecedenceSafe(filterExpression) + "': " + ex.getMessage(), ex);
        }
    }

    public static void checkValidated(ExprForge forge) {
        if (forge == null) {
            throw checkValidatedException();
        }
    }

    public static IllegalStateException checkValidatedException() {
        return new IllegalStateException("Expression has not been validated");
    }

    /**
     * Encapsulates the parse result parsing a mapped property as a class and method name with args.
     */
    public static class MappedPropertyParseResult {
        private String className;
        private String methodName;
        private String argString;

        /**
         * Returns class name.
         *
         * @return name of class
         */
        public String getClassName() {
            return className;
        }

        /**
         * Returns the method name.
         *
         * @return method name
         */
        public String getMethodName() {
            return methodName;
        }

        /**
         * Returns the method argument.
         *
         * @return arg
         */
        public String getArgString() {
            return argString;
        }

        /**
         * Returns the parse result of the mapped property.
         *
         * @param className  is the class name, or null if there isn't one
         * @param methodName is the method name
         * @param argString  is the argument
         */
        public MappedPropertyParseResult(String className, String methodName, String argString) {
            this.className = className;
            this.methodName = methodName;
            this.argString = argString;
        }
    }

    public static void acceptChain(ExprNodeVisitor visitor, List<ExprChainedSpec> chainSpec) {
        for (ExprChainedSpec chain : chainSpec) {
            for (ExprNode param : chain.getParameters()) {
                param.accept(visitor);
            }
        }
    }

    public static void acceptChain(ExprNodeVisitorWithParent visitor, List<ExprChainedSpec> chainSpec) {
        for (ExprChainedSpec chain : chainSpec) {
            for (ExprNode param : chain.getParameters()) {
                param.accept(visitor);
            }
        }
    }

    public static void acceptChain(ExprNodeVisitorWithParent visitor, List<ExprChainedSpec> chainSpec, ExprNode parent) {
        for (ExprChainedSpec chain : chainSpec) {
            for (ExprNode param : chain.getParameters()) {
                param.acceptChildnodes(visitor, parent);
            }
        }
    }

    public static void replaceChildNode(ExprNode parentNode, ExprNode nodeToReplace, ExprNode newNode) {
        int index = ExprNodeUtility.findChildNode(parentNode, nodeToReplace);
        if (index == -1) {
            parentNode.replaceUnlistedChildNode(nodeToReplace, newNode);
        } else {
            parentNode.setChildNode(index, newNode);
        }
    }

    private static int findChildNode(ExprNode parentNode, ExprNode childNode) {
        for (int i = 0; i < parentNode.getChildNodes().length; i++) {
            if (parentNode.getChildNodes()[i] == childNode) {
                return i;
            }
        }
        return -1;
    }

    public static void replaceChainChildNode(ExprNode nodeToReplace, ExprNode newNode, List<ExprChainedSpec> chainSpec) {
        for (ExprChainedSpec chained : chainSpec) {
            int index = chained.getParameters().indexOf(nodeToReplace);
            if (index != -1) {
                chained.getParameters().set(index, newNode);
            }
        }
    }

    public static ExprNodePropOrStreamSet getNonAggregatedProps(EventType[] types, List<ExprNode> exprNodes, ContextPropertyRegistry contextPropertyRegistry) {
        // Determine all event properties in the clause
        ExprNodePropOrStreamSet nonAggProps = new ExprNodePropOrStreamSet();
        ExprNodeIdentifierAndStreamRefVisitor visitor = new ExprNodeIdentifierAndStreamRefVisitor(false);
        for (ExprNode node : exprNodes) {
            visitor.reset();
            node.accept(visitor);
            addNonAggregatedProps(nonAggProps, visitor.getRefs(), types, contextPropertyRegistry);
        }

        return nonAggProps;
    }

    private static void addNonAggregatedProps(ExprNodePropOrStreamSet nonAggProps, List<ExprNodePropOrStreamDesc> refs, EventType[] types, ContextPropertyRegistry contextPropertyRegistry) {
        for (ExprNodePropOrStreamDesc pair : refs) {
            if (pair instanceof ExprNodePropOrStreamPropDesc) {
                ExprNodePropOrStreamPropDesc propDesc = (ExprNodePropOrStreamPropDesc) pair;
                EventType originType = types.length > pair.getStreamNum() ? types[pair.getStreamNum()] : null;
                if (originType == null || contextPropertyRegistry == null || !contextPropertyRegistry.isPartitionProperty(originType, propDesc.getPropertyName())) {
                    nonAggProps.add(pair);
                }
            } else {
                nonAggProps.add(pair);
            }
        }
    }

    public static void addNonAggregatedProps(ExprNode exprNode, ExprNodePropOrStreamSet set, EventType[] types, ContextPropertyRegistry contextPropertyRegistry) {
        ExprNodeIdentifierAndStreamRefVisitor visitor = new ExprNodeIdentifierAndStreamRefVisitor(false);
        exprNode.accept(visitor);
        addNonAggregatedProps(set, visitor.getRefs(), types, contextPropertyRegistry);
    }

    public static ExprNodePropOrStreamSet getAggregatedProperties(List<ExprAggregateNode> aggregateNodes) {
        // Get a list of properties being aggregated in the clause.
        ExprNodePropOrStreamSet propertiesAggregated = new ExprNodePropOrStreamSet();
        ExprNodeIdentifierAndStreamRefVisitor visitor = new ExprNodeIdentifierAndStreamRefVisitor(true);
        for (ExprNode selectAggExprNode : aggregateNodes) {
            visitor.reset();
            selectAggExprNode.accept(visitor);
            List<ExprNodePropOrStreamDesc> properties = visitor.getRefs();
            propertiesAggregated.addAll(properties);
        }

        return propertiesAggregated;
    }

    public static ExprEvaluator[] getEvaluatorsMayCompile(ExprNode[] exprNodes, EngineImportService engineImportService, Class requestor, boolean isFireAndForget, String statementName) {
        if (exprNodes == null) {
            return null;
        }
        ExprEvaluator[] eval = new ExprEvaluator[exprNodes.length];
        for (int i = 0; i < exprNodes.length; i++) {
            ExprNode node = exprNodes[i];
            if (node != null) {
                eval[i] = ExprNodeCompiler.allocateEvaluator(node.getForge(), engineImportService, requestor, isFireAndForget, statementName);
            }
        }
        return eval;
    }

    public static ExprEvaluator[] getEvaluatorsMayCompile(List<ExprNode> exprNodes, EngineImportService engineImportService, Class requestor, boolean isFireAndForget, String statementName) {
        if (exprNodes == null) {
            return null;
        }
        ExprEvaluator[] eval = new ExprEvaluator[exprNodes.size()];
        for (int i = 0; i < exprNodes.size(); i++) {
            ExprNode node = exprNodes.get(i);
            if (node != null) {
                eval[i] = ExprNodeCompiler.allocateEvaluator(node.getForge(), engineImportService, requestor, isFireAndForget, statementName);
            }
        }
        return eval;
    }

    public static ExprEvaluator[] getEvaluatorsMayCompile(ExprForge[] forges, EngineImportService engineImportService, Class requestor, boolean isFireAndForget, String statementName) {
        if (forges == null) {
            return null;
        }
        ExprEvaluator[] eval = new ExprEvaluator[forges.length];
        for (int i = 0; i < forges.length; i++) {
            if (forges[i] != null) {
                eval[i] = ExprNodeCompiler.allocateEvaluator(forges[i], engineImportService, requestor, isFireAndForget, statementName);
            }
        }
        return eval;
    }

    public static ExprEvaluator[] getEvaluatorsNoCompile(ExprNode[] exprNodes) {
        if (exprNodes == null) {
            return null;
        }
        ExprEvaluator[] eval = new ExprEvaluator[exprNodes.length];
        for (int i = 0; i < exprNodes.length; i++) {
            ExprNode node = exprNodes[i];
            if (node != null) {
                eval[i] = node.getForge().getExprEvaluator();
            }
        }
        return eval;
    }

    public static ExprForge[] getForges(ExprNode[] exprNodes) {
        if (exprNodes == null) {
            return null;
        }
        ExprForge[] forge = new ExprForge[exprNodes.length];
        for (int i = 0; i < exprNodes.length; i++) {
            ExprNode node = exprNodes[i];
            if (node != null) {
                forge[i] = node.getForge();
            }
        }
        return forge;
    }

    public static ExprEvaluator[] getEvaluatorsNoCompile(ExprForge[] forges) {
        if (forges == null) {
            return null;
        }
        ExprEvaluator[] eval = new ExprEvaluator[forges.length];
        for (int i = 0; i < forges.length; i++) {
            ExprForge forge = forges[i];
            if (forge != null) {
                eval[i] = forge.getExprEvaluator();
            }
        }
        return eval;
    }

    public static ExprEvaluator[] getEvaluatorsNoCompile(List<ExprNode> childNodes) {
        ExprEvaluator[] eval = new ExprEvaluator[childNodes.size()];
        for (int i = 0; i < childNodes.size(); i++) {
            eval[i] = childNodes.get(i).getForge().getExprEvaluator();
        }
        return eval;
    }

    public static Set<Integer> getIdentStreamNumbers(ExprNode child) {

        Set<Integer> streams = new HashSet<Integer>();
        ExprNodeIdentifierCollectVisitor visitor = new ExprNodeIdentifierCollectVisitor();
        child.accept(visitor);
        for (ExprIdentNode node : visitor.getExprProperties()) {
            streams.add(node.getStreamId());
        }
        return streams;
    }

    /**
     * Returns true if all properties within the expression are witin data window'd streams.
     *
     * @param child              expression to interrogate
     * @param streamTypeService  streams
     * @param unidirectionalJoin indicator unidirection join
     * @return indicator
     */
    public static boolean hasRemoveStreamForAggregations(ExprNode child, StreamTypeService streamTypeService, boolean unidirectionalJoin) {

        // Determine whether all streams are istream-only or irstream
        boolean[] isIStreamOnly = streamTypeService.getIStreamOnly();
        boolean isAllIStream = true;    // all true?
        boolean isAllIRStream = true;   // all false?
        for (boolean anIsIStreamOnly : isIStreamOnly) {
            if (!anIsIStreamOnly) {
                isAllIStream = false;
            } else {
                isAllIRStream = false;
            }
        }

        // determine if a data-window applies to this max function
        boolean hasDataWindows = true;
        if (isAllIStream) {
            hasDataWindows = false;
        } else if (!isAllIRStream) {
            if (streamTypeService.getEventTypes().length > 1) {
                if (unidirectionalJoin) {
                    return false;
                }
                // In a join we assume that a data window is present or implicit via unidirectional
            } else {
                hasDataWindows = false;
                // get all aggregated properties to determine if any is from a windowed stream
                ExprNodeIdentifierCollectVisitor visitor = new ExprNodeIdentifierCollectVisitor();
                child.accept(visitor);
                for (ExprIdentNode node : visitor.getExprProperties()) {
                    if (!isIStreamOnly[node.getStreamId()]) {
                        hasDataWindows = true;
                        break;
                    }
                }
            }
        }

        return hasDataWindows;
    }


    /**
     * Apply a filter expression.
     *
     * @param filter               expression
     * @param streamZeroEvent      the event that represents stream zero
     * @param streamOneEvents      all events thate are stream one events
     * @param exprEvaluatorContext context for expression evaluation
     * @return filtered stream one events
     */
    public static EventBean[] applyFilterExpression(ExprEvaluator filter, EventBean streamZeroEvent, EventBean[] streamOneEvents, ExprEvaluatorContext exprEvaluatorContext) {
        EventBean[] eventsPerStream = new EventBean[2];
        eventsPerStream[0] = streamZeroEvent;

        EventBean[] filtered = new EventBean[streamOneEvents.length];
        int countPass = 0;

        for (EventBean eventBean : streamOneEvents) {
            eventsPerStream[1] = eventBean;

            Boolean result = (Boolean) filter.evaluate(eventsPerStream, true, exprEvaluatorContext);
            if ((result != null) && result) {
                filtered[countPass] = eventBean;
                countPass++;
            }
        }

        if (countPass == streamOneEvents.length) {
            return streamOneEvents;
        }
        return EventBeanUtility.resizeArray(filtered, countPass);
    }

    /**
     * Apply a filter expression returning a pass indicator.
     *
     * @param filter               to apply
     * @param eventsPerStream      events per stream
     * @param exprEvaluatorContext context for expression evaluation
     * @return pass indicator
     */
    public static boolean applyFilterExpression(ExprEvaluator filter, EventBean[] eventsPerStream, ExprEvaluatorContext exprEvaluatorContext) {
        Boolean result = (Boolean) filter.evaluate(eventsPerStream, true, exprEvaluatorContext);
        return (result != null) && result;
    }

    /**
     * Compare two expression nodes and their children in exact child-node sequence,
     * returning true if the 2 expression nodes trees are equals, or false if they are not equals.
     * <p>
     * Recursive call since it uses this method to compare child nodes in the same exact sequence.
     * Nodes are compared using the equalsNode method.
     *
     * @param nodeOne            - first expression top node of the tree to compare
     * @param nodeTwo            - second expression top node of the tree to compare
     * @param ignoreStreamPrefix when the equals-comparison can ignore prefix of event properties
     * @return false if this or all child nodes are not equal, true if equal
     */
    public static boolean deepEquals(ExprNode nodeOne, ExprNode nodeTwo, boolean ignoreStreamPrefix) {
        if (nodeOne.getChildNodes().length != nodeTwo.getChildNodes().length) {
            return false;
        }
        if (!nodeOne.equalsNode(nodeTwo, ignoreStreamPrefix)) {
            return false;
        }
        for (int i = 0; i < nodeOne.getChildNodes().length; i++) {
            ExprNode childNodeOne = nodeOne.getChildNodes()[i];
            ExprNode childNodeTwo = nodeTwo.getChildNodes()[i];

            if (!ExprNodeUtility.deepEquals(childNodeOne, childNodeTwo, ignoreStreamPrefix)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Compares two expression nodes via deep comparison, considering all
     * child nodes of either side.
     *
     * @param one                array of expressions
     * @param two                array of expressions
     * @param ignoreStreamPrefix indicator whether we ignore stream prefixes and instead use resolved property name
     * @return true if the expressions are equal, false if not
     */
    public static boolean deepEquals(ExprNode[] one, ExprNode[] two, boolean ignoreStreamPrefix) {
        if (one.length != two.length) {
            return false;
        }
        for (int i = 0; i < one.length; i++) {
            if (!ExprNodeUtility.deepEquals(one[i], two[i], ignoreStreamPrefix)) {
                return false;
            }
        }
        return true;
    }

    public static boolean deepEquals(List<ExprNode> one, List<ExprNode> two) {
        if (one.size() != two.size()) {
            return false;
        }
        for (int i = 0; i < one.size(); i++) {
            if (!ExprNodeUtility.deepEquals(one.get(i), two.get(i), false)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Check if the expression is minimal: does not have a subselect, aggregation and does not need view resources
     *
     * @param expression to inspect
     * @return null if minimal, otherwise name of offending sub-expression
     */
    public static String isMinimalExpression(ExprNode expression) {
        ExprNodeSubselectDeclaredDotVisitor subselectVisitor = new ExprNodeSubselectDeclaredDotVisitor();
        expression.accept(subselectVisitor);
        if (subselectVisitor.getSubselects().size() > 0) {
            return "a subselect";
        }

        ExprNodeViewResourceVisitor viewResourceVisitor = new ExprNodeViewResourceVisitor();
        expression.accept(viewResourceVisitor);
        if (viewResourceVisitor.getExprNodes().size() > 0) {
            return "a function that requires view resources (prior, prev)";
        }

        List<ExprAggregateNode> aggregateNodes = new LinkedList<ExprAggregateNode>();
        ExprAggregateNodeUtil.getAggregatesBottomUp(expression, aggregateNodes);
        if (!aggregateNodes.isEmpty()) {
            return "an aggregation function";
        }
        return null;
    }

    public static void toExpressionString(List<ExprChainedSpec> chainSpec, StringWriter buffer, boolean prefixDot, String functionName) {
        String delimiterOuter = "";
        if (prefixDot) {
            delimiterOuter = ".";
        }
        boolean isFirst = true;
        for (ExprChainedSpec element : chainSpec) {
            buffer.append(delimiterOuter);
            if (functionName != null) {
                buffer.append(functionName);
            } else {
                buffer.append(element.getName());
            }

            // the first item without dot-prefix and empty parameters should not be appended with parenthesis
            if (!isFirst || prefixDot || !element.getParameters().isEmpty()) {
                toExpressionStringIncludeParen(element.getParameters(), buffer);
            }

            delimiterOuter = ".";
            isFirst = false;
        }
    }

    public static void toExpressionStringParameterList(List<ExprNode> parameters, StringWriter buffer) {
        String delimiter = "";
        for (ExprNode param : parameters) {
            buffer.append(delimiter);
            delimiter = ",";
            buffer.append(ExprNodeUtility.toExpressionStringMinPrecedenceSafe(param));
        }
    }

    public static void toExpressionString(ExprNode node, StringWriter buffer) {
        node.toEPL(buffer, ExprPrecedenceEnum.MINIMUM);
    }

    public static void toExpressionStringIncludeParen(List<ExprNode> parameters, StringWriter buffer) {
        buffer.append("(");
        toExpressionStringParameterList(parameters, buffer);
        buffer.append(")");
    }

    public static void validate(ExprNodeOrigin origin, List<ExprChainedSpec> chainSpec, ExprValidationContext validationContext) throws ExprValidationException {

        // validate all parameters
        for (ExprChainedSpec chainElement : chainSpec) {
            List<ExprNode> validated = new ArrayList<ExprNode>();
            for (ExprNode expr : chainElement.getParameters()) {
                validated.add(ExprNodeUtility.getValidatedSubtree(origin, expr, validationContext));
                if (expr instanceof ExprNamedParameterNode) {
                    throw new ExprValidationException("Named parameters are not allowed");
                }
            }
            chainElement.setParameters(validated);
        }
    }

    public static List<ExprNode> collectChainParameters(List<ExprChainedSpec> chainSpec) {
        List<ExprNode> result = new ArrayList<ExprNode>();
        for (ExprChainedSpec chainElement : chainSpec) {
            result.addAll(chainElement.getParameters());
        }
        return result;
    }

    public static void toExpressionStringParams(StringWriter writer, ExprNode[] params) {
        writer.append('(');
        String delimiter = "";
        for (ExprNode childNode : params) {
            writer.append(delimiter);
            delimiter = ",";
            writer.append(ExprNodeUtility.toExpressionStringMinPrecedenceSafe(childNode));
        }
        writer.append(')');
    }

    public static String printEvaluators(ExprEvaluator[] evaluators) {
        StringWriter writer = new StringWriter();
        String delimiter = "";
        for (ExprEvaluator evaluator : evaluators) {
            writer.append(delimiter);
            writer.append(evaluator.getClass().getSimpleName());
            delimiter = ", ";
        }
        return writer.toString();
    }

    public static ExprEvaluator[] crontabScheduleValidate(ExprNodeOrigin origin, List<ExprNode> scheduleSpecExpressionList, StatementContext context, boolean allowBindingConsumption)
            throws ExprValidationException {

        // Validate the expressions
        ExprEvaluator[] expressions = new ExprEvaluator[scheduleSpecExpressionList.size()];
        int count = 0;
        ExprEvaluatorContextStatement evaluatorContextStmt = new ExprEvaluatorContextStatement(context, false);
        for (ExprNode parameters : scheduleSpecExpressionList) {
            ExprValidationContext validationContext = new ExprValidationContext(new StreamTypeServiceImpl(context.getEngineURI(), false), context.getEngineImportService(), context.getStatementExtensionServicesContext(), null, context.getSchedulingService(), context.getVariableService(), context.getTableService(), evaluatorContextStmt, context.getEventAdapterService(), context.getStatementName(), context.getStatementId(), context.getAnnotations(), context.getContextDescriptor(), false, false, allowBindingConsumption, false, null, false);
            ExprNode node = ExprNodeUtility.getValidatedSubtree(origin, parameters, validationContext);
            expressions[count++] = ExprNodeCompiler.allocateEvaluator(node.getForge(), context.getEngineImportService(), ExprNodeUtility.class, false, context.getStatementName());
        }

        if (expressions.length <= 4 || expressions.length >= 8) {
            throw new ExprValidationException("Invalid schedule specification: " + ScheduleSpecUtil.getExpressionCountException(expressions.length));
        }

        return expressions;
    }

    public static ScheduleSpec crontabScheduleBuild(ExprEvaluator[] scheduleSpecEvaluators, ExprEvaluatorContext context) {

        // Build a schedule
        try {
            Object[] scheduleSpecParameterList = evaluateExpressions(scheduleSpecEvaluators, context);
            return ScheduleSpecUtil.computeValues(scheduleSpecParameterList);
        } catch (ScheduleParameterException e) {
            throw new EPException("Invalid schedule specification: " + e.getMessage(), e);
        }
    }

    public static Object[] evaluateExpressions(ExprEvaluator[] parameters, ExprEvaluatorContext exprEvaluatorContext) {
        Object[] results = new Object[parameters.length];
        int count = 0;
        for (ExprEvaluator expr : parameters) {
            try {
                results[count] = expr.evaluate(null, true, exprEvaluatorContext);
                count++;
            } catch (RuntimeException ex) {
                String message = "Failed expression evaluation in crontab timer-at for parameter " + count + ": " + ex.getMessage();
                log.error(message, ex);
                throw new IllegalArgumentException(message);
            }
        }
        return results;
    }

    public static ExprNode[] toArray(Collection<ExprNode> expressions) {
        if (expressions.isEmpty()) {
            return EMPTY_EXPR_ARRAY;
        }
        return expressions.toArray(new ExprNode[expressions.size()]);
    }

    public static ExprDeclaredNode[] toArray(List<ExprDeclaredNode> declaredNodes) {
        if (declaredNodes.isEmpty()) {
            return EMPTY_DECLARED_ARR;
        }
        return declaredNodes.toArray(new ExprDeclaredNode[declaredNodes.size()]);
    }

    public static ExprNodePropOrStreamSet getGroupByPropertiesValidateHasOne(ExprNode[] groupByNodes)
            throws ExprValidationException {
        // Get the set of properties refered to by all group-by expression nodes.
        ExprNodePropOrStreamSet propertiesGroupBy = new ExprNodePropOrStreamSet();
        ExprNodeIdentifierAndStreamRefVisitor visitor = new ExprNodeIdentifierAndStreamRefVisitor(true);

        for (ExprNode groupByNode : groupByNodes) {
            visitor.reset();
            groupByNode.accept(visitor);
            List<ExprNodePropOrStreamDesc> propertiesNode = visitor.getRefs();
            propertiesGroupBy.addAll(propertiesNode);

            // For each group-by expression node, require at least one property.
            if (propertiesNode.isEmpty()) {
                throw new ExprValidationException("Group-by expressions must refer to property names");
            }
        }

        return propertiesGroupBy;
    }

    private static Pair<ExprForge[], ExprEvaluator[]> makeVarargArrayEval(Method method, final ExprForge[] childForges) {
        ExprEvaluator[] evals = new ExprEvaluator[method.getParameterTypes().length];
        ExprForge[] forges = new ExprForge[method.getParameterTypes().length];
        Class varargClass = method.getParameterTypes()[method.getParameterTypes().length - 1].getComponentType();
        Class varargClassBoxed = JavaClassHelper.getBoxedType(varargClass);
        if (method.getParameterTypes().length > 1) {
            System.arraycopy(childForges, 0, forges, 0, forges.length - 1);
        }
        final int varargArrayLength = childForges.length - method.getParameterTypes().length + 1;

        // handle passing array along
        if (varargArrayLength == 1) {
            ExprForge lastForge = childForges[method.getParameterTypes().length - 1];
            Class lastReturns = lastForge.getEvaluationType();
            if (lastReturns != null && lastReturns.isArray()) {
                forges[method.getParameterTypes().length - 1] = lastForge;
                return new Pair<>(forges, evals);
            }
        }

        // handle parameter conversion to vararg parameter
        ExprForge[] varargForges = new ExprForge[varargArrayLength];
        SimpleNumberCoercer[] coercers = new SimpleNumberCoercer[varargForges.length];
        boolean needCoercion = false;
        for (int i = 0; i < varargArrayLength; i++) {
            int childIndex = i + method.getParameterTypes().length - 1;
            Class resultType = childForges[childIndex].getEvaluationType();
            varargForges[i] = childForges[childIndex];

            if (resultType == null && !varargClass.isPrimitive()) {
                continue;
            }

            if (JavaClassHelper.isSubclassOrImplementsInterface(resultType, varargClass)) {
                // no need to coerce
                continue;
            }

            if (JavaClassHelper.getBoxedType(resultType) != varargClassBoxed) {
                needCoercion = true;
                coercers[i] = SimpleNumberCoercerFactory.getCoercer(resultType, varargClassBoxed);
            }
        }

        ExprForge varargForge = new VarargOnlyArrayForge(varargForges, varargClass, needCoercion ? coercers : null);
        forges[method.getParameterTypes().length - 1] = varargForge;
        evals[method.getParameterTypes().length - 1] = varargForge.getExprEvaluator();
        return new Pair<>(forges, evals);
    }

    private static class VarargOnlyArrayForge implements ExprForge, ExprNodeRenderable {
        private final ExprForge[] forges;
        protected final Class varargClass;
        protected final SimpleNumberCoercer[] optionalCoercers;

        public VarargOnlyArrayForge(ExprForge[] forges, Class varargClass, SimpleNumberCoercer[] optionalCoercers) {
            this.forges = forges;
            this.varargClass = varargClass;
            this.optionalCoercers = optionalCoercers;
        }

        public ExprEvaluator getExprEvaluator() {
            if (optionalCoercers == null) {
                return new VarargOnlyArrayEvalNoCoerce(this, ExprNodeUtility.getEvaluatorsNoCompile(forges));
            }
            return new VarargOnlyArrayForgeWithCoerce(this, ExprNodeUtility.getEvaluatorsNoCompile(forges));
        }

        public CodegenExpression evaluateCodegen(Class requiredType, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
            Class arrayType = JavaClassHelper.getArrayType(varargClass);
            CodegenMethodNode methodNode = codegenMethodScope.makeChild(arrayType, VarargOnlyArrayForge.class, codegenClassScope);


            CodegenBlock block = methodNode.getBlock()
                    .declareVar(arrayType, "array", newArray(varargClass, constant(forges.length)));
            for (int i = 0; i < forges.length; i++) {
                CodegenExpression expression = forges[i].evaluateCodegen(requiredType, methodNode, exprSymbol, codegenClassScope);
                CodegenExpression assignment;
                if (optionalCoercers == null || optionalCoercers[i] == null) {
                    assignment = expression;
                } else {
                    Class evalType = forges[i].getEvaluationType();
                    if (evalType.isPrimitive()) {
                        assignment = optionalCoercers[i].coerceCodegen(expression, evalType);
                    } else {
                        assignment = optionalCoercers[i].coerceCodegenMayNullBoxed(expression, evalType, methodNode, codegenClassScope);
                    }
                }
                block.assignArrayElement("array", constant(i), assignment);
            }
            block.methodReturn(ref("array"));
            return localMethod(methodNode);
        }

        public ExprForgeComplexityEnum getComplexity() {
            return ExprForgeComplexityEnum.INTER;
        }

        public Class getEvaluationType() {
            return JavaClassHelper.getArrayType(varargClass);
        }

        public ExprNodeRenderable getForgeRenderable() {
            return this;
        }

        public void toEPL(StringWriter writer, ExprPrecedenceEnum parentPrecedence) {
            writer.append(this.getClass().getSimpleName());
        }
    }

    private static class VarargOnlyArrayEvalNoCoerce implements ExprEvaluator {
        private final VarargOnlyArrayForge forge;
        private final ExprEvaluator[] evals;

        public VarargOnlyArrayEvalNoCoerce(VarargOnlyArrayForge forge, ExprEvaluator[] evals) {
            this.forge = forge;
            this.evals = evals;
        }

        public Object evaluate(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
            Object array = Array.newInstance(forge.varargClass, evals.length);
            for (int i = 0; i < evals.length; i++) {
                Object value = evals[i].evaluate(eventsPerStream, isNewData, context);
                Array.set(array, i, value);
            }
            return array;
        }

    }

    private static class VarargOnlyArrayForgeWithCoerce implements ExprEvaluator {
        private final VarargOnlyArrayForge forge;
        private final ExprEvaluator[] evals;

        public VarargOnlyArrayForgeWithCoerce(VarargOnlyArrayForge forge, ExprEvaluator[] evals) {
            this.forge = forge;
            this.evals = evals;
        }

        public Object evaluate(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
            Object array = Array.newInstance(forge.varargClass, evals.length);
            for (int i = 0; i < evals.length; i++) {
                Object value = evals[i].evaluate(eventsPerStream, isNewData, context);
                if (forge.optionalCoercers[i] != null) {
                    value = forge.optionalCoercers[i].coerceBoxed((Number) value);
                }
                Array.set(array, i, value);
            }
            return array;
        }

    }
}
