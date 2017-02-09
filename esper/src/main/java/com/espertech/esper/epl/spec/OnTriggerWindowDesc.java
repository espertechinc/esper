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

/**
 * Specification for the on-select and on-delete and on-update (via subclass) (no split-stream) statement.
 */
public class OnTriggerWindowDesc extends OnTriggerDesc {
    private String windowName;
    private String optionalAsName;
    private boolean deleteAndSelect;
    private static final long serialVersionUID = 4146264160256741899L;

    /**
     * Ctor.
     *
     * @param windowName      the window name
     * @param optionalAsName  the optional name
     * @param onTriggerType   for indicationg on-delete, on-select or on-update
     * @param deleteAndSelect indicator whether delete-and-select
     */
    public OnTriggerWindowDesc(String windowName, String optionalAsName, OnTriggerType onTriggerType, boolean deleteAndSelect) {
        super(onTriggerType);
        this.windowName = windowName;
        this.optionalAsName = optionalAsName;
        this.deleteAndSelect = deleteAndSelect;
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
     * Returns the name, or null if none defined.
     *
     * @return name
     */
    public String getOptionalAsName() {
        return optionalAsName;
    }

    public boolean isDeleteAndSelect() {
        return deleteAndSelect;
    }
}
