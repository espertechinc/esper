package com.espertech.esperio.socket.core;

import com.espertech.esper.event.EventBeanManufacturer;
import com.espertech.esper.event.WriteablePropertyDescriptor;
import com.espertech.esper.util.SimpleTypeParser;

public class WriterCacheEntry {
    private final EventBeanManufacturer eventBeanManufacturer;
    private final WriteablePropertyDescriptor[] writableProperties;
    private final SimpleTypeParser[] parsers;

    public WriterCacheEntry(EventBeanManufacturer eventBeanManufacturer, WriteablePropertyDescriptor[] writableProperties, SimpleTypeParser[] parsers) {
        this.eventBeanManufacturer = eventBeanManufacturer;
        this.writableProperties = writableProperties;
        this.parsers = parsers;
    }

    public EventBeanManufacturer getEventBeanManufacturer() {
        return eventBeanManufacturer;
    }

    public WriteablePropertyDescriptor[] getWritableProperties() {
        return writableProperties;
    }

    public SimpleTypeParser[] getParsers() {
        return parsers;
    }
}
