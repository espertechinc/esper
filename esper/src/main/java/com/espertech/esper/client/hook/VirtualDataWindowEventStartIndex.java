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
package com.espertech.esper.client.hook;

import com.espertech.esper.epl.expression.core.ExprNode;

import java.util.List;

/**
 * Event raised when an index gets created or started via the "create index" syntax.
 */
public class VirtualDataWindowEventStartIndex extends VirtualDataWindowEvent {

    private final String namedWindowName;
    private final String indexName;
    private final List<VDWCreateIndexField> fields;
    private final boolean unique;

    /**
     * Ctor.
     *
     * @param namedWindowName named window name
     * @param indexName       index name
     * @param fields          index fields
     * @param unique          for unique indexes
     */
    public VirtualDataWindowEventStartIndex(String namedWindowName, String indexName, List<VDWCreateIndexField> fields, boolean unique) {
        this.namedWindowName = namedWindowName;
        this.indexName = indexName;
        this.fields = fields;
        this.unique = unique;
    }

    /**
     * Returns the index name.
     *
     * @return index name
     */
    public String getIndexName() {
        return indexName;
    }

    /**
     * Returns a list of fields that are part of the index.
     *
     * @return list of index fields
     */
    public List<VDWCreateIndexField> getFields() {
        return fields;
    }

    /**
     * Returns the named window name.
     *
     * @return named window name
     */
    public String getNamedWindowName() {
        return namedWindowName;
    }

    /**
     * Returns indictor for unique index
     *
     * @return unique index indicator
     */
    public boolean isUnique() {
        return unique;
    }

    /**
     * Captures virtual data window indexed field informaion.
     */
    public static class VDWCreateIndexField {
        private List<ExprNode> expressions;
        private String type;
        private List<ExprNode> parameters;

        /**
         * Ctor.
         * @param expressions expressions
         * @param type field type
         * @param parameters parameters
         */
        public VDWCreateIndexField(List<ExprNode> expressions, String type, List<ExprNode> parameters) {
            this.expressions = expressions;
            this.type = type;
            this.parameters = parameters;
        }

        /**
         * Returns index expressions
         * @return index expressions
         */
        public List<ExprNode> getExpressions() {
            return expressions;
        }

        /**
         * Returns index type name
         * @return type name of index
         */
        public String getType() {
            return type;
        }

        /**
         * Returns index field parameters if any
         * @return index field parameters
         */
        public List<ExprNode> getParameters() {
            return parameters;
        }
    }
}
