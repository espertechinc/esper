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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * An insert-into clause consists of a stream name and column names and an optional stream selector.
 */
public class InsertIntoClause implements Serializable {
    private static final long serialVersionUID = 0L;

    private StreamSelector streamSelector;
    private String streamName;
    private List<String> columnNames;

    /**
     * Ctor.
     */
    public InsertIntoClause() {
    }

    /**
     * Creates the insert-into clause.
     *
     * @param streamName the name of the stream to insert into
     * @return clause
     */
    public static InsertIntoClause create(String streamName) {
        return new InsertIntoClause(streamName);
    }

    /**
     * Creates the insert-into clause.
     *
     * @param streamName the name of the stream to insert into
     * @param columns    is a list of column names
     * @return clause
     */
    public static InsertIntoClause create(String streamName, String... columns) {
        return new InsertIntoClause(streamName, columns);
    }

    /**
     * Creates the insert-into clause.
     *
     * @param streamName     the name of the stream to insert into
     * @param columns        is a list of column names
     * @param streamSelector selects the stream
     * @return clause
     */
    public static InsertIntoClause create(String streamName, String[] columns, StreamSelector streamSelector) {
        if (streamSelector == StreamSelector.RSTREAM_ISTREAM_BOTH) {
            throw new IllegalArgumentException("Insert into only allows istream or rstream selection, not both");
        }
        return new InsertIntoClause(streamName, Arrays.asList(columns), streamSelector);
    }

    /**
     * Ctor.
     *
     * @param streamName is the stream name to insert into
     */
    public InsertIntoClause(String streamName) {
        this.streamSelector = StreamSelector.ISTREAM_ONLY;
        this.streamName = streamName;
        this.columnNames = new ArrayList<String>();
    }

    /**
     * Ctor.
     *
     * @param streamName  is the stream name to insert into
     * @param columnNames column names
     */
    public InsertIntoClause(String streamName, String[] columnNames) {
        this.streamSelector = StreamSelector.ISTREAM_ONLY;
        this.streamName = streamName;
        this.columnNames = Arrays.asList(columnNames);
    }

    /**
     * Ctor.
     *
     * @param streamName     is the stream name to insert into
     * @param columnNames    column names
     * @param streamSelector selector for either insert stream (the default) or remove stream or both
     */
    public InsertIntoClause(String streamName, List<String> columnNames, StreamSelector streamSelector) {
        this.streamSelector = streamSelector;
        this.streamName = streamName;
        this.columnNames = columnNames;
    }

    /**
     * Returns the stream selector for the insert into.
     *
     * @return stream selector
     */
    public StreamSelector getStreamSelector() {
        return streamSelector;
    }

    /**
     * Sets the stream selector for the insert into.
     *
     * @param streamSelector stream selector
     */
    public void setStreamSelector(StreamSelector streamSelector) {
        this.streamSelector = streamSelector;
    }

    /**
     * Returns name of stream name to use for insert-into stream.
     *
     * @return stream name
     */
    public String getStreamName() {
        return streamName;
    }

    /**
     * Returns a list of column names specified optionally in the insert-into clause, or empty if none specified.
     *
     * @return column names or empty list if none supplied
     */
    public List<String> getColumnNames() {
        return columnNames;
    }

    /**
     * Set stream name.
     *
     * @param streamName name
     */
    public void setStreamName(String streamName) {
        this.streamName = streamName;
    }

    /**
     * Add a column name to the insert-into clause.
     *
     * @param columnName to add
     */
    public void add(String columnName) {
        columnNames.add(columnName);
    }

    /**
     * Set column names.
     *
     * @param columnNames names
     */
    public void setColumnNames(List<String> columnNames) {
        this.columnNames = columnNames;
    }

    /**
     * Renders the clause in textual representation.
     *
     * @param writer     to output to
     * @param formatter  for newline-whitespace formatting
     * @param isTopLevel to indicate if this insert-into-clause is inside other clauses.
     */
    public void toEPL(StringWriter writer, EPStatementFormatter formatter, boolean isTopLevel) {
        formatter.beginInsertInto(writer, isTopLevel);
        writer.write("insert ");
        if (streamSelector != StreamSelector.ISTREAM_ONLY) {
            writer.write(streamSelector.getEpl());
            writer.write(" ");
        }

        writer.write("into ");
        writer.write(streamName);

        if (columnNames.size() > 0) {
            writer.write("(");
            String delimiter = "";
            for (String name : columnNames) {
                writer.write(delimiter);
                writer.write(name);
                delimiter = ", ";
            }
            writer.write(")");
        }
    }
}
