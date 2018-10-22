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
package com.espertech.esper.common.internal.epl.resultset.select.core;

import com.espertech.esper.common.internal.compile.stage2.SelectClauseElementCompiled;
import com.espertech.esper.common.internal.epl.table.compiletime.TableMetaData;

public class SelectClauseStreamCompiledSpec implements SelectClauseElementCompiled {
    private final String streamName;
    private final String optionalColumnName;
    private int streamNumber = -1;
    private boolean isFragmentEvent = false;
    private boolean isProperty = false;
    private Class propertyType;
    private TableMetaData tableMetadata;

    /**
     * Ctor.
     *
     * @param streamName         is the stream name of the stream to select
     * @param optionalColumnName is the column name
     */
    public SelectClauseStreamCompiledSpec(String streamName, String optionalColumnName) {
        this.streamName = streamName;
        this.optionalColumnName = optionalColumnName;
    }

    /**
     * Returns the stream name (e.g. select streamName from MyEvent as streamName).
     *
     * @return name
     */
    public String getStreamName() {
        return streamName;
    }

    /**
     * Returns the column name.
     *
     * @return name
     */
    public String getOptionalName() {
        return optionalColumnName;
    }

    /**
     * Returns the stream number of the stream for the stream name.
     *
     * @return stream number
     */
    public int getStreamNumber() {
        if (streamNumber == -1) {
            throw new IllegalStateException("Not initialized for stream number and tagged event");
        }
        return streamNumber;
    }

    /**
     * Returns true to indicate that we are meaning to select a tagged event in a pattern, or false if
     * selecting an event from a stream.
     *
     * @return true for tagged event in pattern, false for stream
     */
    public boolean isFragmentEvent() {
        if (streamNumber == -1) {
            throw new IllegalStateException("Not initialized for stream number and tagged event");
        }
        return isFragmentEvent;
    }

    /**
     * Sets the stream number of the selected stream within the context of the from-clause.
     *
     * @param streamNumber to set
     */
    public void setStreamNumber(int streamNumber) {
        this.streamNumber = streamNumber;
    }

    /**
     * Sets a flag indicating whether the stream wildcard is for a tagged event in a pattern.
     *
     * @param taggedEvent in pattern
     */
    public void setFragmentEvent(boolean taggedEvent) {
        isFragmentEvent = taggedEvent;
    }

    /**
     * Sets an indicate that a property was selected with wildcard.
     *
     * @param property     selected
     * @param propertyType the return type
     */
    public void setProperty(boolean property, Class propertyType) {
        this.isProperty = property;
        this.propertyType = propertyType;
    }

    /**
     * True if selecting from a property, false if not
     *
     * @return indicator whether property or not
     */
    public boolean isProperty() {
        return isProperty;
    }

    /**
     * Returns property type.
     *
     * @return property type
     */
    public Class getPropertyType() {
        return propertyType;
    }

    public TableMetaData getTableMetadata() {
        return tableMetadata;
    }

    public void setTableMetadata(TableMetaData tableMetadata) {
        this.tableMetadata = tableMetadata;
    }
}
