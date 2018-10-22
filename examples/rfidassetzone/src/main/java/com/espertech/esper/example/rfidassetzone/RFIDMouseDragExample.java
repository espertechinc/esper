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
package com.espertech.esper.example.rfidassetzone;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.configuration.Configuration;
import com.espertech.esper.runtime.client.EPRuntime;
import com.espertech.esper.runtime.client.EPRuntimeProvider;
import com.espertech.esper.runtime.client.EPStatement;
import com.espertech.esper.runtime.client.UpdateListener;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class RFIDMouseDragExample extends JFrame {
    private final static int WIDTH = 600;
    private final static int HEIGHT = 400;

    private DisplayCanvas canvas;

    public RFIDMouseDragExample() {
        super();

        // Setup runtime
        Configuration config = new Configuration();
        config.getCommon().addEventType("LocationReport", LocationReport.class);

        EPRuntime runtime = EPRuntimeProvider.getDefaultRuntime(config);

        LRMovingZoneStmt.createStmt(runtime, 10, new UpdateListener() {
            public void update(EventBean[] newEvents, EventBean[] oldEvents, EPStatement statement, EPRuntime runtime) {
                for (int i = 0; i < newEvents.length; i++) {
                    System.out.println("ALERT: Asset group not moving together, zone " +
                        newEvents[i].get("Part.zone"));
                }
            }
        });

        // Setup window
        Container container = getContentPane();
        canvas = new DisplayCanvas(runtime, WIDTH, HEIGHT);
        container.add(canvas);

        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                System.exit(0);
            }
        });
        setSize(WIDTH, HEIGHT);
        setVisible(true);
    }

    public static void main(String[] arg) {
        new RFIDMouseDragExample();
    }
}
