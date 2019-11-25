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
package com.espertech.esper.common.internal.epl.table.compiletime;

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.client.PropertyAccessException;
import com.espertech.esper.common.client.configuration.compiler.ConfigurationCompilerPlugInAggregationMultiFunction;
import com.espertech.esper.common.client.hook.aggmultifunc.AggregationMultiFunctionForge;
import com.espertech.esper.common.internal.collection.Pair;
import com.espertech.esper.common.internal.epl.expression.core.ExprChainedSpec;
import com.espertech.esper.common.internal.epl.expression.core.ExprConstantNodeImpl;
import com.espertech.esper.common.internal.epl.expression.core.ExprNode;
import com.espertech.esper.common.internal.epl.expression.core.ExprValidationException;
import com.espertech.esper.common.internal.epl.expression.dot.core.ExprDotNode;
import com.espertech.esper.common.internal.epl.expression.dot.core.ExprDotNodeImpl;
import com.espertech.esper.common.internal.epl.expression.table.*;
import com.espertech.esper.common.internal.epl.streamtype.StreamTypeService;
import com.espertech.esper.common.internal.epl.streamtype.StreamTypeServiceImpl;
import com.espertech.esper.common.internal.event.property.IndexedProperty;
import com.espertech.esper.common.internal.event.property.Property;
import com.espertech.esper.common.internal.event.property.PropertyParser;
import com.espertech.esper.common.internal.util.LazyAllocatedMap;
import com.espertech.esper.common.internal.util.StringValue;
import com.espertech.esper.common.internal.util.ValidationException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import static com.espertech.esper.common.internal.epl.agg.core.AggregationPortableValidationBase.INVALID_TABLE_AGG_RESET;
import static com.espertech.esper.common.internal.epl.agg.core.AggregationPortableValidationBase.INVALID_TABLE_AGG_RESET_PARAMS;

public class TableCompileTimeUtil {

    public static StreamTypeServiceImpl streamTypeFromTableColumn(EventType containedEventType) {
        return new StreamTypeServiceImpl(containedEventType, containedEventType.getName(), false);
    }

    public static Pair<ExprTableAccessNode, List<ExprChainedSpec>> checkTableNameGetLibFunc(
        TableCompileTimeResolver tableService,
        LazyAllocatedMap<ConfigurationCompilerPlugInAggregationMultiFunction, AggregationMultiFunctionForge> plugInAggregations,
        String classIdent,
        List<ExprChainedSpec> chain) {

        int index = StringValue.unescapedIndexOfDot(classIdent);

        // handle special case "table.keys()" function
        if (index == -1) {
            TableMetaData table = tableService.resolve(classIdent);
            if (table == null) {
                return null; // not a table
            }
            String funcName = chain.get(1).getName();
            if (funcName.toLowerCase(Locale.ENGLISH).equals("keys")) {
                List<ExprChainedSpec> subchain = chain.subList(2, chain.size());
                ExprTableAccessNodeKeys node = new ExprTableAccessNodeKeys(table.getTableName());
                return new Pair<>(node, subchain);
            } else {
                throw new ValidationException("Invalid use of table '" + classIdent + "', unrecognized use of function '" + funcName + "', expected 'keys()'");
            }
        }

        // Handle "table.property" (without the variable[...] syntax since this is ungrouped use)
        String tableName = StringValue.unescapeDot(classIdent.substring(0, index));
        TableMetaData table = tableService.resolve(tableName);
        if (table == null) {
            return null;
        }

        // this is a table access expression
        String sub = classIdent.substring(index + 1, classIdent.length());
        return handleTableAccessNode(plugInAggregations, table.getTableName(), sub, chain);
    }

    public static Pair<ExprNode, List<ExprChainedSpec>> getTableNodeChainable(StreamTypeService streamTypeService,
                                                                              List<ExprChainedSpec> chainSpec,
                                                                              boolean allowTableAggReset,
                                                                              TableCompileTimeResolver tableCompileTimeResolver)
            throws ExprValidationException {
        chainSpec = new ArrayList<>(chainSpec);

        String unresolvedPropertyName = chainSpec.get(0).getName();
        int tableStreamNum = streamTypeService.getStreamNumForStreamName(unresolvedPropertyName);
        if (chainSpec.size() == 2 && tableStreamNum != -1) {
            TableMetaData tableMetadata = tableCompileTimeResolver.resolveTableFromEventType(streamTypeService.getEventTypes()[tableStreamNum]);
            if (tableMetadata != null && chainSpec.get(1).getName().toLowerCase(Locale.ENGLISH).equals("reset")) {
                if (!allowTableAggReset) {
                    throw new ExprValidationException(INVALID_TABLE_AGG_RESET);
                }
                if (!chainSpec.get(1).getParameters().isEmpty()) {
                    throw new ExprValidationException(INVALID_TABLE_AGG_RESET_PARAMS);
                }
                ExprTableResetRowAggNode node = new ExprTableResetRowAggNode(tableMetadata, tableStreamNum);
                chainSpec.clear();
                return new Pair<>(node, chainSpec);
            }
        }

        StreamTableColWStreamName col = findTableColumnMayByPrefixed(streamTypeService, unresolvedPropertyName, tableCompileTimeResolver);
        if (col == null) {
            return null;
        }

        StreamTableColPair pair = col.getPair();
        if (pair.getColumn() instanceof TableMetadataColumnAggregation) {
            TableMetadataColumnAggregation agg = (TableMetadataColumnAggregation) pair.getColumn();
            Class returnType = pair.getTableMetadata().getPublicEventType().getPropertyType(pair.getColumn().getColumnName());
            ExprTableIdentNode node = new ExprTableIdentNode(pair.tableMetadata, null, unresolvedPropertyName, returnType, pair.getStreamNum(), agg.getColumnName(), agg.getColumn());
            chainSpec.remove(0);
            return new Pair<>(node, chainSpec);
        }
        return null;
    }

    public static ExprTableIdentNode getTableIdentNode(StreamTypeService streamTypeService, String unresolvedPropertyName, String streamOrPropertyName, TableCompileTimeResolver resolver)
            throws ExprValidationException {
        String propertyPrefixed = unresolvedPropertyName;
        if (streamOrPropertyName != null) {
            propertyPrefixed = streamOrPropertyName + "." + unresolvedPropertyName;
        }
        StreamTableColWStreamName col = findTableColumnMayByPrefixed(streamTypeService, propertyPrefixed, resolver);
        if (col == null) {
            return null;
        }
        StreamTableColPair pair = col.getPair();
        if (pair.getColumn() instanceof TableMetadataColumnAggregation) {
            TableMetadataColumnAggregation agg = (TableMetadataColumnAggregation) pair.getColumn();
            Class resultType = pair.tableMetadata.getPublicEventType().getPropertyType(agg.getColumnName());
            return new ExprTableIdentNode(pair.tableMetadata, streamOrPropertyName, unresolvedPropertyName, resultType, pair.streamNum, agg.getColumnName(), agg.getColumn());
        }
        return null;
    }

    public static Pair<ExprTableAccessNode, ExprDotNode> mapPropertyToTableNested(TableCompileTimeResolver resolver, String stream, String subproperty) {

        TableMetaData table = resolver.resolve(stream);
        Integer indexIfIndexed = null;
        if (table == null) {
            // try indexed property
            Pair<IndexedProperty, TableMetaData> pair = mapPropertyToTable(stream, resolver);
            if (pair == null) {
                return null;
            }
            table = pair.getSecond();
            indexIfIndexed = pair.getFirst().getIndex();
        }

        if (table.isKeyed() && indexIfIndexed == null) {
            return null;
        }
        if (!table.isKeyed() && indexIfIndexed != null) {
            return null;
        }

        int index = StringValue.unescapedIndexOfDot(subproperty);
        if (index == -1) {
            ExprTableAccessNodeSubprop tableNode = new ExprTableAccessNodeSubprop(table.getTableName(), subproperty);
            if (indexIfIndexed != null) {
                tableNode.addChildNode(new ExprConstantNodeImpl(indexIfIndexed));
            }
            return new Pair<>(tableNode, null);
        }

        // we have a nested subproperty such as "tablename.subproperty.abc"
        List<ExprChainedSpec> chainedSpecs = new ArrayList<ExprChainedSpec>(1);
        chainedSpecs.add(new ExprChainedSpec(subproperty.substring(index + 1), Collections.<ExprNode>emptyList(), true));
        ExprTableAccessNodeSubprop tableNode = new ExprTableAccessNodeSubprop(table.getTableName(), subproperty.substring(0, index));
        if (indexIfIndexed != null) {
            tableNode.addChildNode(new ExprConstantNodeImpl(indexIfIndexed));
        }
        ExprDotNode dotNode = new ExprDotNodeImpl(chainedSpecs, false, false);
        dotNode.addChildNode(tableNode);
        return new Pair<>(tableNode, dotNode);
    }

    public static Pair<ExprTableAccessNode, List<ExprChainedSpec>> handleTableAccessNode(
        LazyAllocatedMap<ConfigurationCompilerPlugInAggregationMultiFunction, AggregationMultiFunctionForge> plugInAggregations,
        String tableName,
        String sub,
        List<ExprChainedSpec> chain) {
        ExprTableAccessNode node = new ExprTableAccessNodeSubprop(tableName, sub);
        List<ExprChainedSpec> subchain = chain.subList(1, chain.size());
        return new Pair<>(node, subchain);
    }

    private static StreamTableColWStreamName findTableColumnMayByPrefixed(StreamTypeService streamTypeService, String streamAndPropName, TableCompileTimeResolver resolver)
            throws ExprValidationException {
        int indexDot = streamAndPropName.indexOf(".");
        if (indexDot == -1) {
            StreamTableColPair pair = findTableColumnAcrossStreams(streamTypeService, streamAndPropName, resolver);
            if (pair != null) {
                return new StreamTableColWStreamName(pair, null);
            }
        } else {
            String streamName = streamAndPropName.substring(0, indexDot);
            String colName = streamAndPropName.substring(indexDot + 1);
            int streamNum = streamTypeService.getStreamNumForStreamName(streamName);
            if (streamNum == -1) {
                return null;
            }
            StreamTableColPair pair = findTableColumnForType(streamNum, streamTypeService.getEventTypes()[streamNum], colName, resolver);
            if (pair != null) {
                return new StreamTableColWStreamName(pair, streamName);
            }
        }
        return null;
    }

    private static StreamTableColPair findTableColumnAcrossStreams(StreamTypeService streamTypeService, String columnName, TableCompileTimeResolver resolver)
            throws ExprValidationException {
        StreamTableColPair found = null;
        for (int i = 0; i < streamTypeService.getEventTypes().length; i++) {
            EventType type = streamTypeService.getEventTypes()[i];
            if (type == null) {
                continue;
            }
            StreamTableColPair pair = findTableColumnForType(i, type, columnName, resolver);
            if (pair == null) {
                continue;
            }
            if (found != null) {
                if (streamTypeService.isStreamZeroUnambigous() && found.getStreamNum() == 0) {
                    continue;
                }
                throw new ExprValidationException("Ambiguous table column '" + columnName + "' should be prefixed by a stream name");
            }
            found = pair;
        }
        return found;
    }

    private static StreamTableColPair findTableColumnForType(int streamNum, EventType type, String columnName, TableCompileTimeResolver resolver) {
        TableMetaData tableMetadata = resolver.resolveTableFromEventType(type);
        if (tableMetadata != null) {
            TableMetadataColumn column = tableMetadata.getColumns().get(columnName);
            if (column != null) {
                return new StreamTableColPair(streamNum, column, tableMetadata);
            }
        }
        return null;
    }

    /**
     * Handle property "table" or "table[key]" where key is an integer and therefore can be a regular property
     *
     * @param propertyName property
     * @param resolver     resolver
     * @return expression null or node
     */
    public static ExprTableAccessNode mapPropertyToTableUnnested(String propertyName, TableCompileTimeResolver resolver) {
        // try regular property
        TableMetaData table = resolver.resolve(propertyName);
        if (table != null) {
            return new ExprTableAccessNodeTopLevel(table.getTableName());
        }

        // try indexed property
        Pair<IndexedProperty, TableMetaData> pair = mapPropertyToTable(propertyName, resolver);
        if (pair == null) {
            return null;
        }

        ExprTableAccessNode tableNode = new ExprTableAccessNodeTopLevel(pair.getSecond().getTableName());
        tableNode.addChildNode(new ExprConstantNodeImpl(pair.getFirst().getIndex()));
        return tableNode;
    }

    private static Pair<IndexedProperty, TableMetaData> mapPropertyToTable(String propertyName, TableCompileTimeResolver resolver) {
        try {
            Property property = PropertyParser.parseAndWalkLaxToSimple(propertyName);
            if (!(property instanceof IndexedProperty)) {
                return null;
            }

            String name = property.getPropertyNameAtomic();
            TableMetaData table = resolver.resolve(name);
            if (table == null) {
                return null;
            }

            return new Pair<>((IndexedProperty) property, table);
        } catch (PropertyAccessException ex) {
            // possible
        }
        return null;
    }

    private static class StreamTableColPair {
        private final int streamNum;
        private final TableMetadataColumn column;
        private final TableMetaData tableMetadata;

        private StreamTableColPair(int streamNum, TableMetadataColumn column, TableMetaData tableMetadata) {
            this.streamNum = streamNum;
            this.column = column;
            this.tableMetadata = tableMetadata;
        }

        public int getStreamNum() {
            return streamNum;
        }

        public TableMetadataColumn getColumn() {
            return column;
        }

        public TableMetaData getTableMetadata() {
            return tableMetadata;
        }
    }

    private static class StreamTableColWStreamName {
        private final StreamTableColPair pair;
        private final String optionalStreamName;

        private StreamTableColWStreamName(StreamTableColPair pair, String optionalStreamName) {
            this.pair = pair;
            this.optionalStreamName = optionalStreamName;
        }

        public StreamTableColPair getPair() {
            return pair;
        }

        public String getOptionalStreamName() {
            return optionalStreamName;
        }
    }
}
