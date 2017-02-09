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
package com.espertech.esper.client.soda;

import java.io.Serializable;
import java.io.StringWriter;
import java.util.List;
import java.util.Set;

/**
 * Represents a create-schema syntax for creating a new event type.
 */
public class CreateSchemaClause implements Serializable {
    private static final long serialVersionUID = 0L;

    private String schemaName;
    private Set<String> types;
    private List<SchemaColumnDesc> columns;
    private Set<String> inherits;
    private CreateSchemaClauseTypeDef typeDefinition;
    private String startTimestampPropertyName;
    private String endTimestampPropertyName;
    private Set<String> copyFrom;
    private String treeObjectName;


    /**
     * Ctor.
     */
    public CreateSchemaClause() {
    }

    /**
     * Ctor.
     *
     * @param schemaName     name of type
     * @param types          are for model-after, could be multiple when declaring a variant stream, or a single fully-qualified class name
     * @param typeDefinition type definition
     */
    public CreateSchemaClause(String schemaName, Set<String> types, CreateSchemaClauseTypeDef typeDefinition) {
        this.schemaName = schemaName;
        this.types = types;
        this.typeDefinition = typeDefinition;
    }

    /**
     * Ctor.
     *
     * @param schemaName name of type
     * @param columns    column definition
     * @param inherits   inherited types, if any
     */
    public CreateSchemaClause(String schemaName, List<SchemaColumnDesc> columns, Set<String> inherits) {
        this.schemaName = schemaName;
        this.columns = columns;
        this.inherits = inherits;
    }

    /**
     * Ctor.
     *
     * @param schemaName     name of type
     * @param types          are for model-after, could be multiple when declaring a variant stream, or a single fully-qualified class name
     * @param typeDefinition for variant streams, map or object array
     * @param columns        column definition
     * @param inherits       inherited types, if any
     */
    public CreateSchemaClause(String schemaName, Set<String> types, List<SchemaColumnDesc> columns, Set<String> inherits, CreateSchemaClauseTypeDef typeDefinition) {
        this.schemaName = schemaName;
        this.types = types;
        this.columns = columns;
        this.inherits = inherits;
        this.typeDefinition = typeDefinition;
    }

    /**
     * Returns id of expression assigned by tools.
     *
     * @return id
     */
    public String getTreeObjectName() {
        return treeObjectName;
    }

    /**
     * Sets id of expression assigned by tools.
     *
     * @param treeObjectName to set
     */
    public void setTreeObjectName(String treeObjectName) {
        this.treeObjectName = treeObjectName;
    }

    /**
     * Returns the type name, aka. schema name.
     *
     * @return type name
     */
    public String getSchemaName() {
        return schemaName;
    }

    /**
     * Sets the type name.
     *
     * @param schemaName to set
     */
    public void setSchemaName(String schemaName) {
        this.schemaName = schemaName;
    }

    /**
     * Returns model-after types, i.e. (fully-qualified) class name or event type name(s), multiple for variant types.
     *
     * @return type names or class names
     */
    public Set<String> getTypes() {
        return types;
    }

    /**
     * Sets model-after types, i.e. (fully-qualified) class name or event type name(s), multiple for variant types.
     *
     * @param types type names or class names
     */
    public void setTypes(Set<String> types) {
        this.types = types;
    }

    /**
     * Returns the column definition.
     *
     * @return column def
     */
    public List<SchemaColumnDesc> getColumns() {
        return columns;
    }

    /**
     * Sets the column definition.
     *
     * @param columns column def
     */
    public void setColumns(List<SchemaColumnDesc> columns) {
        this.columns = columns;
    }

    /**
     * Returns the names of event types inherited from, if any
     *
     * @return types inherited
     */
    public Set<String> getInherits() {
        return inherits;
    }

    /**
     * Sets the names of event types inherited from, if any
     *
     * @param inherits types inherited
     */
    public void setInherits(Set<String> inherits) {
        this.inherits = inherits;
    }

    /**
     * returns the type definition.
     *
     * @return type definition
     */
    public CreateSchemaClauseTypeDef getTypeDefinition() {
        return typeDefinition;
    }

    /**
     * Sets the type definition.
     *
     * @param typeDefinition type definition to set
     */
    public void setTypeDefinition(CreateSchemaClauseTypeDef typeDefinition) {
        this.typeDefinition = typeDefinition;
    }

    /**
     * Returns the property name of the property providing the start timestamp value.
     *
     * @return start timestamp property name
     */
    public String getStartTimestampPropertyName() {
        return startTimestampPropertyName;
    }

    /**
     * Sets the property name of the property providing the start timestamp value.
     *
     * @param startTimestampPropertyName start timestamp property name
     */
    public void setStartTimestampPropertyName(String startTimestampPropertyName) {
        this.startTimestampPropertyName = startTimestampPropertyName;
    }

    /**
     * Returns the property name of the property providing the end timestamp value.
     *
     * @return end timestamp property name
     */
    public String getEndTimestampPropertyName() {
        return endTimestampPropertyName;
    }

    /**
     * Returns the optional set of event type names that properties are copied from.
     *
     * @return copy-from event types
     */
    public Set<String> getCopyFrom() {
        return copyFrom;
    }

    /**
     * Sets the optional set of event type names that properties are copied from.
     *
     * @param copyFrom event types
     */
    public void setCopyFrom(Set<String> copyFrom) {
        this.copyFrom = copyFrom;
    }

    /**
     * Sets the property name of the property providing the end timestamp value.
     *
     * @param endTimestampPropertyName start timestamp property name
     */
    public void setEndTimestampPropertyName(String endTimestampPropertyName) {
        this.endTimestampPropertyName = endTimestampPropertyName;
    }

    /**
     * Render as EPL.
     *
     * @param writer to output to
     */
    public void toEPL(StringWriter writer) {
        writer.append("create");
        if (typeDefinition != null) {
            typeDefinition.write(writer);
        }
        writer.append(" schema ");
        writer.append(schemaName);
        writer.append(" as ");
        if ((types != null) && (!types.isEmpty())) {
            String delimiter = "";
            for (String type : types) {
                writer.append(delimiter);
                writer.append(type);
                delimiter = ", ";
            }
        } else {
            writer.append("(");
            String delimiter = "";
            for (SchemaColumnDesc col : columns) {
                writer.append(delimiter);
                col.toEPL(writer);
                delimiter = ", ";
            }
            writer.append(")");
        }

        if ((inherits != null) && (!inherits.isEmpty())) {
            writer.append(" inherits ");
            String delimiter = "";
            for (String name : inherits) {
                writer.append(delimiter);
                writer.append(name);
                delimiter = ", ";
            }
        }

        if (startTimestampPropertyName != null) {
            writer.append(" starttimestamp ");
            writer.append(startTimestampPropertyName);
        }
        if (endTimestampPropertyName != null) {
            writer.append(" endtimestamp ");
            writer.append(endTimestampPropertyName);
        }

        if ((copyFrom != null) && (!copyFrom.isEmpty())) {
            writer.append(" copyFrom ");
            String delimiter = "";
            for (String name : copyFrom) {
                writer.append(delimiter);
                writer.append(name);
                delimiter = ", ";
            }
        }
    }
}
