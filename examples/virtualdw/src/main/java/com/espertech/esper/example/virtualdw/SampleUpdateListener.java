package com.espertech.esper.example.virtualdw;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.UpdateListener;

public class SampleUpdateListener implements UpdateListener {

    private EventBean lastEvent;

    public void update(EventBean[] newEvents, EventBean[] oldEvents) {
        lastEvent = newEvents[0];
    }

    public EventBean getLastEvent() {
        return lastEvent;
    }
}
