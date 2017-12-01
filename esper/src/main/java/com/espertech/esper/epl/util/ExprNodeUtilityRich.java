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
package com.espertech.esper.epl.util;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.EventType;
import com.espertech.esper.client.hook.AggregationFunctionFactory;
import com.espertech.esper.client.hook.EPLMethodInvocationContext;
import com.espertech.esper.client.util.TimePeriod;
import com.espertech.esper.codegen.base.CodegenBlock;
import com.espertech.esper.codegen.base.CodegenClassScope;
import com.espertech.esper.codegen.base.CodegenMethodNode;
import com.espertech.esper.codegen.base.CodegenMethodScope;
import com.espertech.esper.codegen.model.expression.CodegenExpression;
import com.espertech.esper.collection.Pair;
import com.espertech.esper.core.context.util.ContextPropertyRegistry;
import com.espertech.esper.epl.core.engineimport.*;
import com.espertech.esper.epl.core.select.BindProcessorEvaluatorStreamTable;
import com.espertech.esper.epl.core.streamtype.StreamTypeService;
import com.espertech.esper.epl.declexpr.ExprDeclaredNode;
import com.espertech.esper.epl.enummethod.dot.EnumMethodEnum;
import com.espertech.esper.epl.enummethod.dot.ExprDeclaredOrLambdaNode;
import com.espertech.esper.epl.enummethod.dot.ExprLambdaGoesNode;
import com.espertech.esper.epl.expression.baseagg.ExprAggregateNode;
import com.espertech.esper.epl.expression.baseagg.ExprAggregateNodeUtil;
import com.espertech.esper.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.epl.expression.codegen.ExprNodeCompiler;
import com.espertech.esper.epl.expression.core.*;
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
import com.espertech.esper.epl.spec.ExpressionScriptProvided;
import com.espertech.esper.epl.spec.FilterStreamSpecRaw;
import com.espertech.esper.epl.spec.OnTriggerSetAssignment;
import com.espertech.esper.epl.spec.StreamSpecRaw;
import com.espertech.esper.epl.table.mgmt.TableMetadata;
import com.espertech.esper.epl.table.mgmt.TableService;
import com.espertech.esper.event.EventAdapterService;
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
import static com.espertech.esper.epl.expression.core.ExprNodeUtilityCore.*;

public class ExprNodeUtilityRich {

    private static final Logger log = LoggerFactory.getLogger(ExprNodeUtilityCore.class);

    public static final ExprDeclaredNode[] EMPTY_DECLARED_ARR = new ExprDeclaredNode[0];
    public static final ExpressionScriptProvided[] EMPTY_SCRIPTS = new ExpressionScriptProvided[0];

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
                    text = getSubqueryInfoText(subselect.getSubselectNumber() - 1, subselect);
                } else {
                    text = toExpressionStringMinPrecedenceSafe(exprNode);
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

    public static String getSubqueryInfoText(int subqueryNum, ExprSubselectNode subselect) {
        String text = "subquery number " + (subqueryNum + 1);
        StreamSpecRaw streamRaw = subselect.getStatementSpecRaw().getStreamSpecs().get(0);
        if (streamRaw instanceof FilterStreamSpecRaw) {
            text += " querying " + ((FilterStreamSpecRaw) streamRaw).getRawFilterSpec().getEventTypeName();
        }
        return text;
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
            if (validationContext.isExpressionNestedAudit() && !(result instanceof ExprIdentNode) && !(ExprNodeUtilityCore.isConstantValueExpr(result))) {
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
        Object enumValue = EngineImportUtil.resolveIdentAsEnumConst(constant, engineImportService, false);
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

    public static ExprForge makeUnderlyingForge(final int streamNum, final Class resultType, TableMetadata tableMetadata) {
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
            ExprNodeUtilityCore.acceptParams(visitor, chain.getParameters());
        }
    }

    public static void acceptChain(ExprNodeVisitorWithParent visitor, List<ExprChainedSpec> chainSpec) {
        for (ExprChainedSpec chain : chainSpec) {
            ExprNodeUtilityCore.acceptParams(visitor, chain.getParameters());
        }
    }

    public static void acceptChain(ExprNodeVisitorWithParent visitor, List<ExprChainedSpec> chainSpec, ExprNode parent) {
        for (ExprChainedSpec chain : chainSpec) {
            ExprNodeUtilityCore.acceptParams(visitor, chain.getParameters(), parent);
        }
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

    public static ExprEvaluator[] getEvaluatorsMayCompileWMultiValue(ExprForge[][] forges, EngineImportService engineImportService, Class requestor, boolean isFireAndForget, String statementName) {
        if (forges == null) {
            return null;
        }
        ExprEvaluator[] eval = new ExprEvaluator[forges.length];
        for (int i = 0; i < forges.length; i++) {
            eval[i] = getEvaluatorMayCompileWMultiValue(forges[i], engineImportService, requestor, isFireAndForget, statementName);
        }
        return eval;
    }

    public static ExprEvaluator getEvaluatorMayCompileWMultiValue(ExprForge[] forges, EngineImportService engineImportService, Class requestor, boolean isFireAndForget, String statementName) {
        if (forges.length == 1) {
            return ExprNodeCompiler.allocateEvaluator(forges[0], engineImportService, requestor, isFireAndForget, statementName);
        }
        ExprEvaluator[] evals = getEvaluatorsMayCompile(forges, engineImportService, requestor, isFireAndForget, statementName);
        return getEvaluatorMultiValue(evals);
    }

    public static ExprEvaluator getEvaluatorMultiValue(ExprEvaluator[] evaluators) {
        return new ExprEvaluator() {
            public Object evaluate(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
                Object[] values = new Object[evaluators.length];
                for (int i = 0; i < evaluators.length; i++) {
                    values[i] = evaluators[i].evaluate(eventsPerStream, isNewData, exprEvaluatorContext);
                }
                return values;
            }
        };
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

    public static void validate(ExprNodeOrigin origin, List<ExprChainedSpec> chainSpec, ExprValidationContext validationContext) throws ExprValidationException {

        // validate all parameters
        for (ExprChainedSpec chainElement : chainSpec) {
            List<ExprNode> validated = new ArrayList<ExprNode>();
            for (ExprNode expr : chainElement.getParameters()) {
                validated.add(ExprNodeUtilityRich.getValidatedSubtree(origin, expr, validationContext));
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
                return new VarargOnlyArrayEvalNoCoerce(this, getEvaluatorsNoCompile(forges));
            }
            return new VarargOnlyArrayForgeWithCoerce(this, getEvaluatorsNoCompile(forges));
        }

        public CodegenExpression evaluateCodegen(Class requiredType, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
            Class arrayType = JavaClassHelper.getArrayType(varargClass);
            CodegenMethodNode methodNode = codegenMethodScope.makeChild(arrayType, VarargOnlyArrayForge.class, codegenClassScope);


            CodegenBlock block = methodNode.getBlock()
                    .declareVar(arrayType, "array", newArrayByLength(varargClass, constant(forges.length)));
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
