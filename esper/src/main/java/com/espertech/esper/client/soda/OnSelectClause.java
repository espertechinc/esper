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

import java.io.StringWriter;

/**
 * A clause to delete from a named window based on a triggering event arriving and correlated to the named window events to be deleted.
 */
public class OnSelectClause extends OnClause {
    private static final long serialVersionUID = 0L;

    private String windowName;
    private String optionalAsName;
    private boolean deleteAndSelect;

    /**
     * Ctor.
     */
    public OnSelectClause() {
    }

    /**
     * Creates an on-select clause.
     *
     * @param windowName     is the named window name
     * @param optionalAsName is the optional name
     * @return on-select clause
     */
    public static OnSelectClause create(String windowName, String optionalAsName) {
        return new OnSelectClause(windowName, optionalAsName);
    }

    /**
     * Ctor.
     *
     * @param windowName     is the named window name
     * @param optionalAsName is the name of the named window
     */
    public OnSelectClause(String windowName, String optionalAsName) {
        this.windowName = windowName;
        this.optionalAsName = optionalAsName;
    }

    /**
     * Renders the clause in textual representation.
     *
     * @param writer to output to
     */
    public void toEPL(StringWriter writer) {
        writer.write(windowName);
        if (optionalAsName != null) {
            writer.write(" as ");
            writer.write(optionalAsName);
        }
    }

    /**
     * Returns the name of the named window to delete from.
     *
     * @return named window name
     */
    public String getWindowName() {
        return windowName;
    }

    /**
     * Sets the name of the named window.
     *
     * @param windowName window name
     */
    public void setWindowName(String windowName) {
        this.windowName = windowName;
    }

    /**
     * Returns the as-provided name for the named window.
     *
     * @return name
     */
    public String getOptionalAsName() {
        return optionalAsName;
    }

    /**
     * Sets the as-provided name for the named window.
     *
     * @param optionalAsName name to set for window
     */
    public void setOptionalAsName(String optionalAsName) {
        this.optionalAsName = optionalAsName;
    }

    /**
     * Returns indicator whether select-and-delete or just select
     *
     * @return indicator
     */
    public boolean isDeleteAndSelect() {
        return deleteAndSelect;
    }

    /**
     * Sets indicator whether select-and-delete or just select
     *
     * @param deleteAndSelect indicator
     */
    public void setDeleteAndSelect(boolean deleteAndSelect) {
        this.deleteAndSelect = deleteAndSelect;
    }
}
