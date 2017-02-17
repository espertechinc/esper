/*
 * *************************************************************************************
 *  Copyright (C) 2006-2015 EsperTech, Inc. All rights reserved.                       *
 *  http://www.espertech.com/esper                                                     *
 *  http://www.espertech.com                                                           *
 *  ---------------------------------------------------------------------------------- *
 *  The software in this package is published under the terms of the GPL license       *
 *  a copy of which has been included with this distribution in the license.txt file.  *
 * *************************************************************************************
 */

package com.espertech.esper.entities;

import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.EventType;
import com.espertech.esper.client.annotation.Description;
import com.espertech.esper.client.annotation.Name;
import com.espertech.esper.client.soda.EPStatementObjectModel;
import com.espertech.esper.client.util.DateTime;
import com.espertech.esper.core.service.EPStatementSPI;
import com.espertech.esper.event.EventTypeSPI;

import java.util.Calendar;

public class StatementDetailFactory {
    public static StatementDetail getDetail(EPServiceProvider engine, EPStatement stmt, boolean includeSoda, PropertySerializerClassNameProvider serializerClassNameProvider) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(stmt.getTimeLastStateChange());
        String lastStateChange = DateTime.print(cal);

        EPStatementSPI spi = (EPStatementSPI) stmt;
        String exprShort = null;
        if (spi.getExpressionNoAnnotations() != null) {
            exprShort = spi.getExpressionNoAnnotations();
        }

        String description = getDescriptionFromAnnotations(stmt.getAnnotations());
        String annotations = toStringAllocations(stmt.getAnnotations(), false);

        EventType eventType = stmt.getEventType();
        PropertyDetail[] properties = PropertyDetailFactory.getProperties((EventTypeSPI) eventType, serializerClassNameProvider);

        EPStatementObjectModel model = null;
        if (includeSoda) {
            model = engine.getEPAdministrator().compileEPL(stmt.getText());
        }

        return new StatementDetail(engine.getURI(),
                stmt.getName(), stmt.getText(), exprShort,
                stmt.getState().toString(), lastStateChange,
                stmt.isPattern(), description, spi.getStatementMetadata().getStatementType().toString(),
                annotations, properties, model);
    }

    private static String getDescriptionFromAnnotations(java.lang.annotation.Annotation[] annotations) {
        if ((annotations != null) && (annotations.length != 0)) {
            for (int j = 0; j < annotations.length; j++) {
                java.lang.annotation.Annotation anno = annotations[j];
                if (anno instanceof Description) {
                    return ((Description) anno).value();
                }
            }
        }
        return null;
    }

    private static String toStringAllocations(java.lang.annotation.Annotation[] annotations, boolean includeNameAndDesc) {
        StringBuilder buf = new StringBuilder();
        String delimiter = "";
        for (java.lang.annotation.Annotation anno : annotations) {
            if (!includeNameAndDesc) {
                if (anno instanceof Name) {
                    continue;
                }
                if (anno instanceof Description) {
                    continue;
                }
            }

            buf.append(delimiter);
            buf.append(anno.toString());
            delimiter = " ";
        }
        String out = buf.toString();
        if (out.trim().isEmpty()) {
            return null;
        }
        return out;
    }
}
