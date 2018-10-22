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
package com.espertech.esper.common.internal.context.aifactory.select;

import com.espertech.esper.common.internal.epl.namedwindow.core.NamedWindow;
import com.espertech.esper.common.internal.epl.table.core.Table;

public class StreamJoinAnalysisResultRuntime {
    private boolean pureSelfJoin;
    private boolean[] unidirectional;
    private boolean[] unidirectionalNonDriving;
    private NamedWindow[] namedWindows;
    private Table[] tables;

    public boolean isPureSelfJoin() {
        return pureSelfJoin;
    }

    public void setPureSelfJoin(boolean pureSelfJoin) {
        this.pureSelfJoin = pureSelfJoin;
    }

    public boolean isUnidirectionalAll() {
        for (boolean ind : unidirectional) {
            if (!ind) {
                return false;
            }
        }
        return true;
    }

    public boolean isUnidirectional() {
        for (boolean ind : unidirectional) {
            if (ind) {
                return true;
            }
        }
        return false;
    }

    public int getUnidirectionalStreamNumberFirst() {
        for (int i = 0; i < unidirectional.length; i++) {
            if (unidirectional[i]) {
                return i;
            }
        }
        throw new IllegalStateException();
    }

    public boolean[] getUnidirectionalNonDriving() {
        return unidirectionalNonDriving;
    }

    public void setUnidirectionalNonDriving(boolean[] unidirectionalNonDriving) {
        this.unidirectionalNonDriving = unidirectionalNonDriving;
    }

    public NamedWindow[] getNamedWindows() {
        return namedWindows;
    }

    public void setNamedWindows(NamedWindow[] namedWindows) {
        this.namedWindows = namedWindows;
    }

    public void setTables(Table[] tables) {
        this.tables = tables;
    }

    public void setUnidirectional(boolean[] unidirectional) {
        this.unidirectional = unidirectional;
    }

    public Table[] getTables() {
        return tables;
    }
}
