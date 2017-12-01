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
package com.espertech.esper.epl.spec;

import com.espertech.esper.epl.expression.core.ExprNode;

import java.io.Serializable;
import java.util.List;

/**
 * Specification for creating a named window.
 */
public class CreateWindowDesc implements Serializable {
    private String windowName;
    private List<ViewSpec> viewSpecs;
    private boolean isInsert;
    private String insertFromWindow;
    private ExprNode insertFilter;
    private StreamSpecOptions streamSpecOptions;
    private List<ColumnDesc> columns;
    private String asEventTypeName;
    private static final long serialVersionUID = 3889989851649484639L;

    /**
     * Ctor.
     *
     * @param windowName        the window name
     * @param viewSpecs         the view definitions
     * @param insert            true for insert-info
     * @param insertFilter      optional filter expression
     * @param streamSpecOptions options such as retain-union etc
     * @param columns           list of columns, if using column syntax
     * @param asEventTypeName   as-type
     */
    public CreateWindowDesc(String windowName, List<ViewSpec> viewSpecs, StreamSpecOptions streamSpecOptions, boolean insert, ExprNode insertFilter, List<ColumnDesc> columns, String asEventTypeName) {
        this.windowName = windowName;
        this.viewSpecs = viewSpecs;
        this.isInsert = insert;
        this.insertFilter = insertFilter;
        this.streamSpecOptions = streamSpecOptions;
        this.columns = columns;
        this.asEventTypeName = asEventTypeName;
    }

    /**
     * Returns the window name.
     *
     * @return window name
     */
    public String getWindowName() {
        return windowName;
    }

    /**
     * Returns the view specifications.
     *
     * @return view specs
     */
    public List<ViewSpec> getViewSpecs() {
        return viewSpecs;
    }

    /**
     * Returns true for insert-from.
     *
     * @return indicator to insert from another named window
     */
    public boolean isInsert() {
        return isInsert;
    }

    /**
     * Returns the expression to filter insert-from events, or null if none supplied.
     *
     * @return insert filter expression
     */
    public ExprNode getInsertFilter() {
        return insertFilter;
    }

    /**
     * Returns the window name to insert from.
     *
     * @return window name to insert from
     */
    public String getInsertFromWindow() {
        return insertFromWindow;
    }

    /**
     * Sets the filter expression to use to apply
     *
     * @param insertFilter filter
     */
    public void setInsertFilter(ExprNode insertFilter) {
        this.insertFilter = insertFilter;
    }

    /**
     * Sets the source named window if inserting from another named window.
     *
     * @param insertFromWindow source named window
     */
    public void setInsertFromWindow(String insertFromWindow) {
        this.insertFromWindow = insertFromWindow;
    }

    /**
     * Returns the options for the stream such as unidirectional, retain-union etc.
     *
     * @return stream options
     */
    public StreamSpecOptions getStreamSpecOptions() {
        return streamSpecOptions;
    }

    /**
     * Returns column names and types.
     *
     * @return column descriptors
     */
    public List<ColumnDesc> getColumns() {
        return columns;
    }

    public String getAsEventTypeName() {
        return asEventTypeName;
    }
}
