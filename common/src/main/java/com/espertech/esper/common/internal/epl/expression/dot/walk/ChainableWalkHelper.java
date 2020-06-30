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
package com.espertech.esper.common.internal.epl.expression.dot.walk;

import com.espertech.esper.common.client.type.EPType;
import com.espertech.esper.common.client.type.EPTypeClass;
import com.espertech.esper.common.client.type.EPTypeNull;
import com.espertech.esper.common.internal.collection.Pair;
import com.espertech.esper.common.internal.compile.stage1.specmapper.ASTAggregationHelper;
import com.espertech.esper.common.internal.compile.stage1.specmapper.StatementSpecMapContext;
import com.espertech.esper.common.internal.epl.expression.agg.base.ExprAggregateNodeUtil;
import com.espertech.esper.common.internal.epl.expression.agg.method.ExprMinMaxAggrNode;
import com.espertech.esper.common.internal.epl.expression.chain.Chainable;
import com.espertech.esper.common.internal.epl.expression.chain.ChainableArray;
import com.espertech.esper.common.internal.epl.expression.chain.ChainableCall;
import com.espertech.esper.common.internal.epl.expression.chain.ChainableName;
import com.espertech.esper.common.internal.epl.expression.core.*;
import com.espertech.esper.common.internal.epl.expression.declared.compiletime.ExprDeclaredHelper;
import com.espertech.esper.common.internal.epl.expression.declared.compiletime.ExprDeclaredNodeImpl;
import com.espertech.esper.common.internal.epl.expression.dot.core.ExprDotNode;
import com.espertech.esper.common.internal.epl.expression.dot.core.ExprDotNodeImpl;
import com.espertech.esper.common.internal.epl.expression.funcs.ExprMinMaxRowNode;
import com.espertech.esper.common.internal.epl.expression.funcs.ExprPlugInSingleRowNode;
import com.espertech.esper.common.internal.epl.expression.table.ExprTableAccessNode;
import com.espertech.esper.common.internal.epl.expression.table.ExprTableAccessNodeKeys;
import com.espertech.esper.common.internal.epl.expression.table.ExprTableAccessNodeSubprop;
import com.espertech.esper.common.internal.epl.expression.table.ExprTableAccessNodeTopLevel;
import com.espertech.esper.common.internal.epl.expression.variable.ExprVariableNodeImpl;
import com.espertech.esper.common.internal.epl.script.core.ExprNodeScript;
import com.espertech.esper.common.internal.epl.table.compiletime.TableMetaData;
import com.espertech.esper.common.internal.epl.table.compiletime.TableMetadataColumn;
import com.espertech.esper.common.internal.epl.variable.compiletime.VariableMetaData;
import com.espertech.esper.common.internal.epl.variable.core.VariableUtil;
import com.espertech.esper.common.internal.settings.ClasspathImportException;
import com.espertech.esper.common.internal.settings.ClasspathImportSingleRowDesc;
import com.espertech.esper.common.internal.settings.ClasspathImportUndefinedException;
import com.espertech.esper.common.internal.util.JavaClassHelper;
import com.espertech.esper.common.internal.util.ValidationException;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;

import static com.espertech.esper.common.internal.epl.expression.core.ExprNodeUtilityPrint.toExpressionStringMinPrecedenceSafe;

public class ChainableWalkHelper {
    public static ExprNode processDot(boolean useChainAsIs, boolean resolveObjects, List<Chainable> chain, StatementSpecMapContext mapContext) {
        if (chain.isEmpty()) {
            throw new IllegalArgumentException("Empty chain");
        }
        Function<List<Chainable>, ExprDotNodeImpl> dotNodeFunction = chainSpec -> {
            ExprDotNodeImpl dotNode = new ExprDotNodeImpl(chainSpec,
                mapContext.getConfiguration().getCompiler().getExpression().isDuckTyping(),
                mapContext.getConfiguration().getCompiler().getExpression().isUdfCache());
            // add any variables that are referenced
            VariableMetaData variable = dotNode.isVariableOpGetName(mapContext.getVariableCompileTimeResolver());
            if (variable != null) {
                mapContext.getVariableNames().add(variable.getVariableName());
            }
            return dotNode;
        };

        // Resolve objects if required
        if (resolveObjects) {
            ExprNode resolved = resolveObject(chain, mapContext, dotNodeFunction);
            if (resolved != null) {
                return resolved;
            }
        }

        // Check if we are dealing with a plain event property expression, i.e. one without any eventstream-dependent expression
        boolean plain = determinePlainProperty(chain);
        if (plain) {
            return handlePlain(chain, dotNodeFunction, useChainAsIs);
        }
        return handleNonPlain(chain, dotNodeFunction);
    }

    private static ExprNode resolveObject(List<Chainable> chain, StatementSpecMapContext mapContext, Function<List<Chainable>, ExprDotNodeImpl> dotNodeFunction) {
        Chainable chainFirst = chain.get(0);
        String chainFirstName = chainFirst.getRootNameOrEmptyString();
        List<ExprNode> chainFirstParams = chainFirst.getParametersOrEmpty();

        // Handle script
        ExprNodeScript scriptNode = ExprDeclaredHelper.getExistsScript(mapContext.getConfiguration().getCompiler().getScripts().getDefaultDialect(), chainFirstName, chainFirstParams, mapContext.getScripts(), mapContext.getMapEnv());
        if (scriptNode != null) {
            return handleScript(scriptNode, chain, dotNodeFunction);
        }

        // Handle Table-related exceptions
        // A table will be "table.more" or "table[x, ...].more"
        TableMetaData table = mapContext.getTableCompileTimeResolver().resolve(chainFirstName);
        if (table != null) {
            Pair<ExprNode, ExprTableAccessNode> nodes = handleTable(chain, table, dotNodeFunction);
            if (nodes != null) {
                mapContext.getTableExpressions().add(nodes.getSecond());
                return nodes.getFirst();
            }
        }

        // Handle Variable-related exceptions
        // A variable will be "variable.more" or "variable[x, ...].more"
        VariableMetaData variable = mapContext.getVariableCompileTimeResolver().resolve(chainFirstName);
        if (variable != null) {
            mapContext.getVariableNames().add(variable.getVariableName());
            return handleVariable(chain, variable, mapContext, dotNodeFunction);
        }

        // Handle plug-in single-row functions
        Pair<Class, ClasspathImportSingleRowDesc> singleRow = trySingleRow(mapContext, chainFirstName);
        if (singleRow != null) {
            return handleSingleRow(singleRow, chain);
        }

        // try additional built-in single-row function
        ExprNode singleRowExtNode = mapContext.getClasspathImportService().resolveSingleRowExtendedBuiltin(chainFirstName);
        if (singleRowExtNode != null) {
            return handleSingleRowExt(singleRowExtNode, chain, dotNodeFunction);
        }

        // Handle declared-expression
        Pair<ExprDeclaredNodeImpl, StatementSpecMapContext> declaredExpr = ExprDeclaredHelper.getExistsDeclaredExpr(chainFirstName, chainFirstParams, mapContext.getExpressionDeclarations().values(), mapContext.getContextCompileTimeDescriptor(), mapContext.getMapEnv(), mapContext.getPlugInAggregations(), mapContext.getScripts());
        if (declaredExpr != null) {
            mapContext.add(declaredExpr.getSecond());
            return handleDeclaredExpr(declaredExpr.getFirst(), chain, dotNodeFunction);
        }

        // Handle aggregation function
        ExprNode aggregationNode = (chainFirst instanceof ChainableName) ? null : ASTAggregationHelper.tryResolveAsAggregation(mapContext.getClasspathImportService(), chainFirst.isDistinct(), chainFirstName, mapContext.getPlugInAggregations(), mapContext.getClassProvidedClasspathExtension());
        if (aggregationNode != null) {
            return handleAggregation(aggregationNode, chain, dotNodeFunction);
        }

        // Handle context property
        if (mapContext.getContextCompileTimeDescriptor() != null && mapContext.getContextCompileTimeDescriptor().getContextPropertyRegistry().isContextPropertyPrefix(chainFirstName)) {
            String subproperty = toPlainPropertyString(chain, 1);
            return new ExprContextPropertyNodeImpl(subproperty);
        }

        // Handle min-max case
        String chainFirstLowerCase = chainFirstName.toLowerCase(Locale.ENGLISH);
        if (!(chainFirst instanceof ChainableName) && (chainFirstLowerCase.equals("max") || chainFirstLowerCase.equals("min") ||
            chainFirstLowerCase.equals("fmax") || chainFirstLowerCase.equals("fmin"))) {
            return handleMinMax(chainFirstLowerCase, chain, dotNodeFunction);
        }

        // Handle class name
        List<Chainable> classChain = handleClassPrefixedNonProp(mapContext, chain);
        if (classChain != null) {
            return dotNodeFunction.apply(classChain);
        }

        return null;
    }

    private static ExprNode handleSingleRowExt(ExprNode singleRowExtNode, List<Chainable> chain, Function<List<Chainable>, ExprDotNodeImpl> dotNodeFunction) {
        singleRowExtNode.addChildNodes(chain.get(0).getParametersOrEmpty());
        if (chain.size() == 1) {
            return singleRowExtNode;
        }
        List<Chainable> spec = new ArrayList<>(chain.subList(1, chain.size()));
        ExprNode dot = dotNodeFunction.apply(spec);
        dot.addChildNode(singleRowExtNode);
        return dot;
    }

    private static ExprNode handleDeclaredExpr(ExprDeclaredNodeImpl node, List<Chainable> chain, Function<List<Chainable>, ExprDotNodeImpl> dotNodeFunction) {
        if (chain.size() == 1) {
            return node;
        }
        List<Chainable> spec = new ArrayList<>(chain.subList(1, chain.size()));
        ExprNode dot = dotNodeFunction.apply(spec);
        dot.addChildNode(node);
        return dot;
    }

    private static ExprNode handleMinMax(String chainFirstLowerCase, List<Chainable> chain, Function<List<Chainable>, ExprDotNodeImpl> dotNodeFunction) {
        ExprNode node = handleMinMaxNode(chainFirstLowerCase, chain.get(0));
        if (chain.size() == 1) {
            return node;
        }
        List<Chainable> spec = new ArrayList<>(chain.subList(1, chain.size()));
        ExprNode dot = dotNodeFunction.apply(spec);
        dot.addChildNode(node);
        return dot;
    }

    private static ExprNode handleMinMaxNode(String chainFirstLowerCase, Chainable spec) {
        MinMaxTypeEnum minMaxTypeEnum;
        boolean filtered = chainFirstLowerCase.startsWith("f");
        if (chainFirstLowerCase.equals("min") || chainFirstLowerCase.equals("fmin")) {
            minMaxTypeEnum = MinMaxTypeEnum.MIN;
        } else if (chainFirstLowerCase.equals("max") || chainFirstLowerCase.equals("fmax")) {
            minMaxTypeEnum = MinMaxTypeEnum.MAX;
        } else {
            throw new ValidationException("Uncountered unrecognized min or max node '" + spec.getRootNameOrEmptyString() + "'");
        }

        List<ExprNode> args = spec.getParametersOrEmpty();
        boolean distinct = spec.isDistinct();
        int numArgsPositional = ExprAggregateNodeUtil.countPositionalArgs(args);
        if (numArgsPositional > 1 && spec.isDistinct() && !filtered) {
            throw new ValidationException("The distinct keyword is not valid in per-row min and max " +
                "functions with multiple sub-expressions");
        }

        ExprNode minMaxNode;
        if (!distinct && numArgsPositional > 1 && !filtered) {
            // use the row function
            minMaxNode = new ExprMinMaxRowNode(minMaxTypeEnum);
        } else {
            // use the aggregation function
            minMaxNode = new ExprMinMaxAggrNode(distinct, minMaxTypeEnum, filtered, false);
        }
        minMaxNode.addChildNodes(args);
        return minMaxNode;
    }

    private static ExprNode handleSingleRow(Pair<Class, ClasspathImportSingleRowDesc> singleRow, List<Chainable> chain) {
        List<Chainable> spec = new ArrayList<>();
        String methodName = singleRow.getSecond().getMethodName();
        String nameUsed = chain.get(0).getRootNameOrEmptyString();
        ChainableCall call = new ChainableCall(methodName, chain.get(0).getParametersOrEmpty());
        spec.add(call);
        spec.addAll(chain.subList(1, chain.size()));
        return new ExprPlugInSingleRowNode(nameUsed, singleRow.getFirst(), spec, singleRow.getSecond());
    }

    private static Pair<Class, ClasspathImportSingleRowDesc> trySingleRow(StatementSpecMapContext mapContext, String chainFirstName) {
        try {
            return mapContext.getClasspathImportService().resolveSingleRow(chainFirstName, mapContext.getClassProvidedClasspathExtension());
        } catch (ClasspathImportException | ClasspathImportUndefinedException ex) {
            return null;
        }
    }

    private static ExprNode handleScript(ExprNodeScript scriptNode, List<Chainable> chain, Function<List<Chainable>, ExprDotNodeImpl> dotNodeFunction) {
        if (chain.size() == 1) {
            return scriptNode;
        }
        List<Chainable> subchain = chain.subList(1, chain.size());
        ExprDotNode dot = dotNodeFunction.apply(subchain);
        dot.addChildNode(scriptNode);
        return dot;
    }

    private static ExprNode handleVariable(List<Chainable> chain, VariableMetaData variable, StatementSpecMapContext mapContext, Function<List<Chainable>, ExprDotNodeImpl> dotNodeFunction) {
        String message = VariableUtil.checkVariableContextName(mapContext.getContextName(), variable);
        if (message != null) {
            throw new ValidationException(message);
        }

        ExprNode rootNode = new ExprVariableNodeImpl(variable, null);
        if (chain.size() == 1) {
            return rootNode;
        }

        // Handle simple-subproperty by means of variable node
        if (chain.size() == 2 && chain.get(1) instanceof ChainableName) {
            return new ExprVariableNodeImpl(variable, chain.get(1).getRootNameOrEmptyString());
        }

        List<Chainable> subchain = chain.subList(1, chain.size());
        ExprDotNode dot = dotNodeFunction.apply(subchain);
        dot.addChildNode(rootNode);
        return dot;
    }

    private static List<Chainable> handleClassPrefixedNonProp(StatementSpecMapContext mapContext, List<Chainable> chain) {
        int indexOfLastProp = getClassIndexOfLastProp(chain);
        if (indexOfLastProp == -1 || indexOfLastProp == chain.size() - 1) {
            return null;
        }
        int depth = indexOfLastProp;
        int depthFound = -1;
        while (depth > 0) {
            String classNameCandidate = buildClassName(chain, depth);
            try {
                mapContext.getClasspathImportService().resolveClass(classNameCandidate, false, mapContext.getClassProvidedClasspathExtension());
                depthFound = depth;
                break;
            } catch (Throwable ex) {
                // expected, handled later when expression validation takes place
            }
            depth--;
        }
        if (depthFound == -1) {
            return null;
        }
        if (depth == indexOfLastProp) {
            String classNameCandidate = buildClassName(chain, depth);
            return buildSubchainWClassname(classNameCandidate, depth + 1, chain);
        }
        // include the next identifier, i.e. ENUM or CONSTANT etc.
        String classNameCandidate = buildClassName(chain, depth + 1);
        return buildSubchainWClassname(classNameCandidate, depth + 2, chain);
    }

    private static List<Chainable> buildSubchainWClassname(String classNameCandidate, int depth, List<Chainable> chain) {
        List<Chainable> newChain = new ArrayList<>(2);
        newChain.add(new ChainableName(classNameCandidate));
        newChain.addAll(chain.subList(depth, chain.size()));
        return newChain;
    }

    private static int getClassIndexOfLastProp(List<Chainable> chain) {
        int indexOfLastProp = -1;
        for (int i = 0; i < chain.size(); i++) {
            Chainable spec = chain.get(i);
            if (!(spec instanceof ChainableName) || spec.isOptional()) {
                return indexOfLastProp;
            }
            if (chain.size() > i + 1 && chain.get(i + 1) instanceof ChainableArray) {
                return indexOfLastProp;
            }
            indexOfLastProp = i;
        }
        return indexOfLastProp;
    }

    private static String buildClassName(List<Chainable> chain, int depthInclusive) {
        StringBuilder builder = new StringBuilder();
        String delimiter = "";
        for (int i = 0; i < depthInclusive + 1; i++) {
            builder.append(delimiter);
            builder.append(chain.get(i).getRootNameOrEmptyString());
            delimiter = ".";
        }
        return builder.toString();
    }

    // Event properties can be plain properties or complex chains including function chain.
    //
    // Plain properties:
    // - have just constants, i.e. just array[0] and map('x')
    // - don't have inner expressions, i.e. don't have array[index_expr] or map(key_expr)
    // - they are handled by ExprIdentNode and completely by each event type
    // - this allows chains such as a.array[0].map('x') to evaluate directly within the underlying itself
    //   and without EventBean instance allocation and with eliminating casting the underlying
    //
    // Complex chain:
    // - always have an expression such as "array[index_indexexpr]"
    // - are handled by ExprDotNode
    // - evaluated as chain, using fragment event type i.e. EventBean instance allocation when required
    //
    private static ExprNode handlePlain(List<Chainable> chain, Function<List<Chainable>, ExprDotNodeImpl> dotNodeFunction, boolean useChainAsIs) {
        // Handle properties that are not prefixed by a stream name
        Chainable first = chain.get(0);
        if (chain.size() == 1 || isArrayProperty(first, chain.get(1)) || first.isOptional() || isMappedProperty(first)) {
            if (useChainAsIs) {
                return dotNodeFunction.apply(chain);
            }
            String propertyName = toPlainPropertyString(chain, 0);
            return new ExprIdentNodeImpl(propertyName);
        }

        // Handle properties that can be prefixed by a stream name
        String leadingIdentifier = chain.get(0).getRootNameOrEmptyString();
        String streamOrNestedPropertyName = DotEscaper.escapeDot(leadingIdentifier);
        String propertyName = toPlainPropertyString(chain, 1);
        return new ExprIdentNodeImpl(propertyName, streamOrNestedPropertyName);
    }

    private static boolean isArrayProperty(Chainable chainable, Chainable next) {
        if (!(next instanceof ChainableArray)) {
            return false;
        }
        ChainableArray array = (ChainableArray) next;
        return chainable instanceof ChainableName && isSingleParameterConstantOfType(array.getIndexes(), Integer.class);
    }

    private static boolean isMappedProperty(Chainable chainable) {
        if (!(chainable instanceof ChainableCall)) {
            return false;
        }
        ChainableCall call = (ChainableCall) chainable;
        return isSingleParameterConstantOfType(call.getParameters(), String.class);
    }

    private static ExprNode handleAggregation(ExprNode aggregationNode, List<Chainable> chain, Function<List<Chainable>, ExprDotNodeImpl> dotNodeFunction) {
        Chainable firstSpec = chain.remove(0);
        aggregationNode.addChildNodes(firstSpec.getParametersOrEmpty());
        ExprNode exprNode;
        if (chain.isEmpty()) {
            exprNode = aggregationNode;
        } else {
            exprNode = dotNodeFunction.apply(chain);
            exprNode.addChildNode(aggregationNode);
        }
        return exprNode;
    }

    private static ExprNode handleNonPlain(List<Chainable> chain, Function<List<Chainable>, ExprDotNodeImpl> dotNodeFunction) {
        if (chain.size() == 1) {
            return dotNodeFunction.apply(chain);
        }

        // We know that this is not a plain event property.
        // Build a class name from the prefix.
        int indexOfLastProp = getClassIndexOfLastProp(chain);
        if (indexOfLastProp != -1 && indexOfLastProp < chain.size() - 1) {
            String classNameCandidate = buildClassName(chain, indexOfLastProp);
            chain = buildSubchainWClassname(classNameCandidate, indexOfLastProp + 1, chain);
            return dotNodeFunction.apply(chain);
        }

        return dotNodeFunction.apply(chain);
    }

    private static boolean determinePlainProperty(List<Chainable> chain) {
        Chainable previous = null;
        for (Chainable spec : chain) {
            if (spec instanceof ChainableArray) {
                // must be "[index]" with index being an integer constant
                ChainableArray array = (ChainableArray) spec;
                if (!isSingleParameterConstantOfType(array.getIndexes(), Integer.class)) {
                    return false;
                }
                if (previous instanceof ChainableArray) {
                    // plain property expressions don't allow two-dimensional array
                    return false;
                }
            }
            if (spec instanceof ChainableCall) {
                // must be "x(key)" with key being a string constant
                ChainableCall call = (ChainableCall) spec;
                if (!isSingleParameterConstantOfType(call.getParameters(), String.class)) {
                    return false;
                }
            }
            previous = spec;
        }

        return true;
    }

    private static Pair<ExprNode, ExprTableAccessNode> handleTable(List<Chainable> chain, TableMetaData table, Function<List<Chainable>, ExprDotNodeImpl> dotNodeFunction) {
        if (chain.size() == 1) {
            ExprTableAccessNodeTopLevel node = new ExprTableAccessNodeTopLevel(table.getTableName());
            return new Pair<>(node, node);
        }

        if (chain.get(1) instanceof ChainableArray) {
            List<ExprNode> tableKeys = ((ChainableArray) chain.get(1)).getIndexes();
            return handleTableSubchain(tableKeys, chain.subList(2, chain.size()), table, dotNodeFunction);
        } else {
            return handleTableSubchain(Collections.emptyList(), chain.subList(1, chain.size()), table, dotNodeFunction);
        }
    }

    private static Pair<ExprNode, ExprTableAccessNode> handleTableSubchain(List<ExprNode> tableKeys, List<Chainable> chain, TableMetaData table, Function<List<Chainable>, ExprDotNodeImpl> dotNodeFunction) {

        if (chain.isEmpty()) {
            ExprTableAccessNodeTopLevel node = new ExprTableAccessNodeTopLevel(table.getTableName());
            node.addChildNodes(tableKeys);
            return new Pair<>(node, node);
        }

        // We make an exception when the table is keyed and the column is found and there are no table keys provided.
        // This accommodates the case "select MyTable.a from MyTable".
        String columnOrOtherName = chain.get(0).getRootNameOrEmptyString();
        TableMetadataColumn tableColumn = table.getColumns().get(columnOrOtherName);
        if (tableColumn != null && table.isKeyed() && tableKeys.isEmpty()) {
            return null; // let this be resolved as an identifier
        }
        if (chain.size() == 1) {
            if (chain.get(0) instanceof ChainableName) {
                ExprTableAccessNodeSubprop node = new ExprTableAccessNodeSubprop(table.getTableName(), columnOrOtherName);
                node.addChildNodes(tableKeys);
                return new Pair<>(node, node);
            }
            if (columnOrOtherName.toLowerCase(Locale.ENGLISH).equals("keys")) {
                ExprTableAccessNodeKeys node = new ExprTableAccessNodeKeys(table.getTableName());
                node.addChildNodes(tableKeys);
                return new Pair<>(node, node);
            } else {
                throw new ValidationException("Invalid use of table '" + table.getTableName() + "', unrecognized use of function '" + columnOrOtherName + "', expected 'keys()'");
            }
        }

        ExprTableAccessNodeSubprop node = new ExprTableAccessNodeSubprop(table.getTableName(), columnOrOtherName);
        node.addChildNodes(tableKeys);
        List<Chainable> subchain = chain.subList(1, chain.size());
        ExprNode exprNode = dotNodeFunction.apply(subchain);
        exprNode.addChildNode(node);
        return new Pair<>(exprNode, node);
    }

    private static boolean isSingleParameterConstantOfType(List<ExprNode> expressions, Class expected) {
        if (expressions.size() != 1) {
            return false;
        }
        ExprNode first = expressions.get(0);
        return isConstantExprOfType(first, expected);
    }

    private static boolean isConstantExprOfType(ExprNode node, Class expected) {
        if (!(node instanceof ExprConstantNode)) {
            return false;
        }
        ExprConstantNode constantNode = (ExprConstantNode) node;
        EPType type = constantNode.getConstantType();
        if (type == null || type == EPTypeNull.INSTANCE) {
            return expected == null;
        }
        EPTypeClass typeClass = (EPTypeClass) type;
        return JavaClassHelper.getBoxedType(typeClass).getType() == expected;
    }

    private static String toPlainPropertyString(List<Chainable> chain, int startIndex) {
        StringWriter buffer = new StringWriter();
        String delimiter = "";
        for (Chainable element : chain.subList(startIndex, chain.size())) {
            if (element instanceof ChainableName) {
                ChainableName name = (ChainableName) element;
                buffer.append(delimiter);
                buffer.append(name.getNameUnescaped());
            } else if (element instanceof ChainableArray) {
                ChainableArray array = (ChainableArray) element;
                if (array.getIndexes().size() != 1) {
                    throw new IllegalStateException("Expected plain array property but found multiple index expressions");
                }
                buffer.append("[");
                buffer.append(toExpressionStringMinPrecedenceSafe(array.getIndexes().get(0)));
                buffer.append("]");
            } else if (element instanceof ChainableCall) {
                ChainableCall call = (ChainableCall) element;
                if (call.getParameters().size() != 1) {
                    throw new IllegalStateException("Expected plain mapped property but found multiple key expressions");
                }
                buffer.append(delimiter);
                buffer.append(call.getNameUnescaped());
                buffer.append("(");
                ExprConstantNode constantNode = (ExprConstantNode) call.getParameters().get(0);
                if (constantNode.getStringConstantWhenProvided() != null) {
                    buffer.append(constantNode.getStringConstantWhenProvided());
                } else {
                    buffer.append("'");
                    buffer.append((String) constantNode.getConstantValue());
                    buffer.append("'");
                }
                buffer.append(")");
            }
            if (element.isOptional()) {
                buffer.append("?");
            }
            delimiter = ".";
        }
        return buffer.toString();
    }
}

