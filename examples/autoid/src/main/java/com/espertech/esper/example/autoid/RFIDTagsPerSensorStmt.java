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
package com.espertech.esper.example.autoid;

import com.espertech.esper.client.EPAdministrator;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.UpdateListener;

public class RFIDTagsPerSensorStmt {
    private EPStatement statement;

    public RFIDTagsPerSensorStmt(EPAdministrator admin) {
        String stmt = "select ID as sensorId, coalesce(sum(countTags), 0) as numTagsPerSensor " +
                "from AutoIdRFIDExample#time(60 sec) " +
                "where Observation[0].Command = 'READ_PALLET_TAGS_ONLY' " +
                "group by ID";

        statement = admin.createEPL(stmt);
    }

    public void addListener(UpdateListener listener) {
        statement.addListener(listener);
    }
}
