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
package com.espertech.esper.compiler.internal.parse;

import com.espertech.esper.common.client.configuration.compiler.ConfigurationCompilerPlugInAggregationMultiFunction;
import com.espertech.esper.common.client.hook.aggmultifunc.AggregationMultiFunctionForge;
import com.espertech.esper.common.internal.collection.Pair;
import com.espertech.esper.common.internal.epl.expression.core.ExprChainedSpec;
import com.espertech.esper.common.internal.epl.expression.table.ExprTableAccessNode;
import com.espertech.esper.common.internal.epl.expression.table.ExprTableAccessNodeSubprop;
import com.espertech.esper.common.internal.epl.expression.table.ExprTableAccessNodeTopLevel;
import com.espertech.esper.common.internal.epl.table.compiletime.TableCompileTimeResolver;
import com.espertech.esper.common.internal.epl.table.compiletime.TableCompileTimeUtil;
import com.espertech.esper.common.internal.epl.table.compiletime.TableMetaData;
import com.espertech.esper.common.internal.settings.ClasspathImportServiceCompileTime;
import com.espertech.esper.common.internal.util.LazyAllocatedMap;
import com.espertech.esper.common.internal.util.StringValue;

import java.util.List;

public class ASTTableExprHelper {

    public static ExprTableAccessNode checkTableNameGetExprForProperty(TableCompileTimeResolver tableCompileTimeResolver, String propertyName) {

        // handle "var_name" alone, without chained, like an simple event property
        int index = StringValue.unescapedIndexOfDot(propertyName);
        if (index == -1) {
            TableMetaData table = tableCompileTimeResolver.resolve(propertyName);
            return table == null ? null : new ExprTableAccessNodeTopLevel(table.getTableName());
        }

        // handle "var_name.column", without chained, like a nested event property
        String tableName = StringValue.unescapeDot(propertyName.substring(0, index));
        TableMetaData table = tableCompileTimeResolver.resolve(tableName);
        if (table == null) {
            return null;
        }

        // it is a tables's subproperty
        String sub = propertyName.substring(index + 1, propertyName.length());
        return new ExprTableAccessNodeSubprop(table.getTableName(), sub);
    }

    public static Pair<ExprTableAccessNode, List<ExprChainedSpec>> getTableExprChainable(
            ClasspathImportServiceCompileTime classpathImportService,
            LazyAllocatedMap<ConfigurationCompilerPlugInAggregationMultiFunction, AggregationMultiFunctionForge> plugInAggregations,
            String tableName,
            List<ExprChainedSpec> chain) {

        // handle just "variable[...].sub"
        String subpropName = chain.get(0).getName();
        if (chain.size() == 1) {
            chain.remove(0);
            ExprTableAccessNodeSubprop tableNode = new ExprTableAccessNodeSubprop(tableName, subpropName);
            return new Pair<>(tableNode, chain);
        }

        // we have a chain "variable[...].sub.xyz"
        return TableCompileTimeUtil.handleTableAccessNode(plugInAggregations, tableName, subpropName, chain);
    }
}
