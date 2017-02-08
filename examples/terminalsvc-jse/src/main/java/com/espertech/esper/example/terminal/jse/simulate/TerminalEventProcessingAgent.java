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
package com.espertech.esper.example.terminal.jse.simulate;

import com.espertech.esper.client.*;
import com.espertech.esper.example.terminal.jse.event.*;
import com.espertech.esper.example.terminal.jse.listener.*;

/**
 * The master component that binds the ESP/EPL statements with the event processing agents (EPA)
 */
public class TerminalEventProcessingAgent {

    private final EPServiceProvider esperEngine;

    public TerminalEventProcessingAgent(ComplexEventListener complexEventListener) {
        // Register event class name for simplicity
        Configuration config = new Configuration();
        config.addEventType("Checkin", Checkin.class);
        config.addEventType("Cancelled", Cancelled.class);
        config.addEventType("Completed", Completed.class);
        config.addEventType("Status", Status.class);
        config.addEventType("LowPaper", LowPaper.class);
        config.addEventType("OutOfOrder", OutOfOrder.class);
        config.addEventType("BaseTerminalEvent", BaseTerminalEvent.class);

        // Get an engine instance
        esperEngine = EPServiceProviderManager.getDefaultProvider(config);
        System.out.println("Esper engine=" + esperEngine.toString());

        EPStatement statement = null;
        String stmt = null;

        stmt = "select a.terminal.id as terminal from pattern [ every a=Checkin -> " +
                "      ( OutOfOrder(terminal.id=a.terminal.id) and not (Cancelled(terminal.id=a.terminal.id) or Completed(terminal.id=a.terminal.id)) )]";
        statement = esperEngine.getEPAdministrator().createEPL(stmt);
        statement.addListener(new CheckinProblemListener(complexEventListener));

        stmt = "select * from BaseTerminalEvent where type = 'LowPaper' or type = 'OutOfOrder'";
        statement = esperEngine.getEPAdministrator().createEPL(stmt);
        statement.addListener(new TerminalEventListener(complexEventListener));

        // Note
        // this statement is illustrative as it is not going to be triggered as terminals
        // do not issue hearbeats in the simulation
        stmt = "select '1' as terminal, 'terminal is offline' as text from pattern [ every timer:interval(60 seconds) -> (timer:interval(65 seconds) and not Status(terminal.id = 'T1')) ] output first every 5 minutes";
        statement = esperEngine.getEPAdministrator().createEPL(stmt);
        statement.addListener(new TerminalStatusListener(complexEventListener));

        stmt = "insert into CountPerType " +
                "select type, count(*) as countPerType " +
                "from BaseTerminalEvent#time(10 min) " +
                "group by type " +
                "output all every 10 seconds";
        statement = esperEngine.getEPAdministrator().createEPL(stmt);
        statement.addListener(new CountPerTypeListener(complexEventListener));

        // The following demonstrates use of an "insert into ... select ..." statement capable of generating
        // virtual events - that can further be processed
        // We decide here to compute checkin latency stats over 1000 checkin event batch
        // We also use an anonymous event processor
        stmt = "insert into VirtualLatency select (b.timestamp - a.timestamp) as latency from pattern [" +
                " every a=Checkin -> b=BaseTerminalEvent(terminal.id=a.terminal.id, type in ('Completed', 'Cancelled', 'OutOfOrder'))]";
        statement = esperEngine.getEPAdministrator().createEPL(stmt);
        stmt = "select * from VirtualLatency#length_batch(1000)#uni(latency)";
        statement = esperEngine.getEPAdministrator().createEPL(stmt);
        statement.addListener(new BaseTerminalListener(complexEventListener) {
            public void update(EventBean[] newEvents, EventBean[] oldEvents) {
                long count = (Long) newEvents[0].get("datapoints");
                double avg = (Double) newEvents[0].get("average");
                complexEventListener.onComplexEvent("latency is " + avg + " over " + count + " checkin");
            }
        });
    }

    public void sendEvent(Object theEvent) {
        esperEngine.getEPRuntime().sendEvent(theEvent);
    }

}
