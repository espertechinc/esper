package com.espertech.esper.example.virtualdw;

public class SampleTriggerEvent {
    private String triggerKey;

    public SampleTriggerEvent(String triggerKey) {
        this.triggerKey = triggerKey;
    }

    public String getTriggerKey() {
        return triggerKey;
    }
}
