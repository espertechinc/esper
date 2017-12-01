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
package com.espertech.esper.epl.parse;

import com.espertech.esper.client.ConfigurationPlugInAggregationMultiFunction;
import com.espertech.esper.collection.Pair;
import com.espertech.esper.epl.core.engineimport.EngineImportService;
import com.espertech.esper.epl.expression.baseagg.ExprAggregateNodeBase;
import com.espertech.esper.epl.expression.core.ExprChainedSpec;
import com.espertech.esper.epl.expression.core.ExprNode;
import com.espertech.esper.epl.expression.dot.ExprDotNode;
import com.espertech.esper.epl.expression.dot.ExprDotNodeImpl;
import com.espertech.esper.epl.expression.table.*;
import com.espertech.esper.epl.spec.StatementSpecRaw;
import com.espertech.esper.epl.table.mgmt.TableMetadata;
import com.espertech.esper.epl.table.mgmt.TableService;
import com.espertech.esper.plugin.PlugInAggregationMultiFunctionFactory;
import com.espertech.esper.util.LazyAllocatedMap;
import com.espertech.esper.util.StringValue;

import java.util.*;

public class ASTTableExprHelper {

    public static void addTableExpressionReference(StatementSpecRaw statementSpec, ExprTableAccessNode tableNode) {
        if (statementSpec.getTableExpressions() == null) {
            statementSpec.setTableExpressions(new HashSet<ExprTableAccessNode>());
        }
        statementSpec.getTableExpressions().add(tableNode);
    }

    public static void addTableExpressionReference(StatementSpecRaw statementSpec, Set<ExprTableAccessNode> tableNodes) {
        if (tableNodes == null || tableNodes.isEmpty()) {
            return;
        }
        if (statementSpec.getTableExpressions() == null) {
            statementSpec.setTableExpressions(new HashSet<ExprTableAccessNode>());
        }
        statementSpec.getTableExpressions().addAll(tableNodes);
    }

    public static Pair<ExprTableAccessNode, ExprDotNode> checkTableNameGetExprForSubproperty(TableService tableService, String tableName, String subproperty) {
        TableMetadata metadata = tableService.getTableMetadata(tableName);
        if (metadata == null) {
            return null;
        }

        int index = StringValue.unescapedIndexOfDot(subproperty);
        if (index == -1) {
            if (metadata.getKeyTypes().length > 0) {
                return null;
            }
            ExprTableAccessNodeSubprop tableNode = new ExprTableAccessNodeSubprop(tableName, subproperty);
            return new Pair<ExprTableAccessNode, ExprDotNode>(tableNode, null);
        }

        // we have a nested subproperty such as "tablename.subproperty.abc"
        List<ExprChainedSpec> chainedSpecs = new ArrayList<ExprChainedSpec>(1);
        chainedSpecs.add(new ExprChainedSpec(subproperty.substring(index + 1), Collections.<ExprNode>emptyList(), true));
        ExprTableAccessNodeSubprop tableNode = new ExprTableAccessNodeSubprop(tableName, subproperty.substring(0, index));
        ExprDotNode dotNode = new ExprDotNodeImpl(chainedSpecs, false, false);
        dotNode.addChildNode(tableNode);
        return new Pair<ExprTableAccessNode, ExprDotNode>(tableNode, dotNode);
    }

    /**
     * Resolve "table" and "table.property" when nested-property, not chainable
     *
     * @param tableService tables
     * @param propertyName property name
     * @return table access node
     */
    public static ExprTableAccessNode checkTableNameGetExprForProperty(TableService tableService, String propertyName) {

        // handle "var_name" alone, without chained, like an simple event property
        int index = StringValue.unescapedIndexOfDot(propertyName);
        if (index == -1) {
            if (tableService.getTableMetadata(propertyName) != null) {
                return new ExprTableAccessNodeTopLevel(propertyName);
            }
            return null;
        }

        // handle "var_name.column", without chained, like a nested event property
        String tableName = ASTUtil.unescapeDot(propertyName.substring(0, index));
        if (tableService.getTableMetadata(tableName) == null) {
            return null;
        }

        // it is a tables's subproperty
        String sub = propertyName.substring(index + 1, propertyName.length());
        return new ExprTableAccessNodeSubprop(tableName, sub);
    }

    public static Pair<ExprTableAccessNode, List<ExprChainedSpec>> checkTableNameGetLibFunc(
            TableService tableService,
            EngineImportService engineImportService,
            LazyAllocatedMap<ConfigurationPlugInAggregationMultiFunction, PlugInAggregationMultiFunctionFactory> plugInAggregations,
            String engineURI,
            String classIdent,
            List<ExprChainedSpec> chain) {

        int index = StringValue.unescapedIndexOfDot(classIdent);

        // handle special case "table.keys()" function
        if (index == -1) {
            if (tableService.getTableMetadata(classIdent) == null) {
                return null; // not a table
            }
            String funcName = chain.get(1).getName();
            if (funcName.toLowerCase(Locale.ENGLISH).equals("keys")) {
                List<ExprChainedSpec> subchain = chain.subList(2, chain.size());
                ExprTableAccessNodeKeys node = new ExprTableAccessNodeKeys(classIdent);
                return new Pair<ExprTableAccessNode, List<ExprChainedSpec>>(node, subchain);
            } else {
                throw ASTWalkException.from("Invalid use of variable '" + classIdent + "', unrecognized use of function '" + funcName + "', expected 'keys()'");
            }
        }

        // Handle "table.property" (without the variable[...] syntax since this is ungrouped use)
        String tableName = ASTUtil.unescapeDot(classIdent.substring(0, index));
        if (tableService.getTableMetadata(tableName) == null) {
            return null;
        }

        // this is a table access expression
        String sub = classIdent.substring(index + 1, classIdent.length());
        return handleTable(engineImportService, plugInAggregations, engineURI, tableName, sub, chain);
    }

    public static Pair<ExprTableAccessNode, List<ExprChainedSpec>> getTableExprChainable(
            EngineImportService engineImportService,
            LazyAllocatedMap<ConfigurationPlugInAggregationMultiFunction, PlugInAggregationMultiFunctionFactory> plugInAggregations,
            String engineURI,
            String tableName,
            List<ExprChainedSpec> chain) {

        // handle just "variable[...].sub"
        String subpropName = chain.get(0).getName();
        if (chain.size() == 1) {
            chain.remove(0);
            ExprTableAccessNodeSubprop tableNode = new ExprTableAccessNodeSubprop(tableName, subpropName);
            return new Pair<ExprTableAccessNode, List<ExprChainedSpec>>(tableNode, chain);
        }

        // we have a chain "variable[...].sub.xyz"
        return handleTable(engineImportService, plugInAggregations, engineURI,
                tableName, subpropName, chain);
    }

    private static Pair<ExprTableAccessNode, List<ExprChainedSpec>> handleTable(
            EngineImportService engineImportService,
            LazyAllocatedMap<ConfigurationPlugInAggregationMultiFunction, PlugInAggregationMultiFunctionFactory> plugInAggregations,
            String engineURI,
            String tableName,
            String sub,
            List<ExprChainedSpec> chain) {

        ExprTableAccessNode node = new ExprTableAccessNodeSubprop(tableName, sub);
        List<ExprChainedSpec> subchain = chain.subList(1, chain.size());

        String candidateAccessor = subchain.get(0).getName();
        ExprAggregateNodeBase exprNode = (ExprAggregateNodeBase) ASTAggregationHelper.tryResolveAsAggregation(engineImportService, false, candidateAccessor, plugInAggregations, engineURI);
        if (exprNode != null) {
            node = new ExprTableAccessNodeSubpropAccessor(tableName, sub, exprNode);
            exprNode.addChildNodes(subchain.get(0).getParameters());
            subchain.remove(0);
        }

        return new Pair<ExprTableAccessNode, List<ExprChainedSpec>>(node, subchain);

    }
}
