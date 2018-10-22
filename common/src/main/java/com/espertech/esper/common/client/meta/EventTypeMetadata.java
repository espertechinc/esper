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
package com.espertech.esper.common.client.meta;

import com.espertech.esper.common.client.util.EventTypeBusModifier;
import com.espertech.esper.common.client.util.NameAccessModifier;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

/**
 * Provides metadata for an event type.
 */
public class EventTypeMetadata {
    private final String name;
    private final String moduleName;
    private final EventTypeTypeClass typeClass;
    private final EventTypeApplicationType applicationType;
    private final NameAccessModifier accessModifier;
    private final EventTypeBusModifier busModifier;
    private final boolean isPropertyAgnostic;   // Type accepts any property name (i.e. no-schema XML type)
    private final EventTypeIdPair eventTypeIdPair;

    /**
     * Ctor.
     *
     * @param name               event type name
     * @param moduleName         module name that originated the event type or null if not provided or if the event type is preconfigured
     * @param typeClass          information on the originator or use of the event type
     * @param applicationType    provides the type of the underlying
     * @param accessModifier     the access modifier defining how the event type is visible to other modules
     * @param busModifier        the bus modifier defining how the event type is visible to applications calling send-event methods
     * @param isPropertyAgnostic whether the type is property-agnostic (false for most typed, true for a type that allows any property name)
     * @param eventTypeIdPair    the type id pair
     */
    public EventTypeMetadata(String name, String moduleName, EventTypeTypeClass typeClass, EventTypeApplicationType applicationType, NameAccessModifier accessModifier, EventTypeBusModifier busModifier, boolean isPropertyAgnostic, EventTypeIdPair eventTypeIdPair) {
        this.name = name;
        this.moduleName = moduleName;
        this.typeClass = typeClass;
        this.applicationType = applicationType;
        this.accessModifier = accessModifier;
        this.busModifier = busModifier;
        this.isPropertyAgnostic = isPropertyAgnostic;
        this.eventTypeIdPair = eventTypeIdPair;
    }

    /**
     * Returns information on the originator or use of the event type
     *
     * @return type class
     */
    public EventTypeTypeClass getTypeClass() {
        return typeClass;
    }

    /**
     * Returns the underlying type
     *
     * @return underling type
     */
    public EventTypeApplicationType getApplicationType() {
        return applicationType;
    }

    /**
     * Returns the access modifier
     *
     * @return access modifier
     */
    public NameAccessModifier getAccessModifier() {
        return accessModifier;
    }

    /**
     * Returns the event bus modifier.
     *
     * @return bus modifier
     */
    public EventTypeBusModifier getBusModifier() {
        return busModifier;
    }

    /**
     * Returns indicator whether the type is property-agnostic, i.e. false for types that have a list of well-defined property names and
     * true for a type that allows any property name
     *
     * @return indicator
     */
    public boolean isPropertyAgnostic() {
        return isPropertyAgnostic;
    }

    /**
     * Returns event type ids
     *
     * @return event type ids
     */
    public EventTypeIdPair getEventTypeIdPair() {
        return eventTypeIdPair;
    }

    /**
     * Returns the event type name.
     *
     * @return event type name
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the module name or null when not provided.
     *
     * @return module name
     */
    public String getModuleName() {
        return moduleName;
    }

    /**
     * Build an expression for the metadata (for internal use).
     *
     * @return exppression
     */
    public CodegenExpression toExpression() {
        return toExpressionWPublicId(constant(eventTypeIdPair.getPublicId()));
    }

    /**
     * Build an expression for the metadata (for internal use).
     *
     * @param expressionEventTypeIdPublic id pair
     * @return exppression
     */
    public CodegenExpression toExpressionWPublicId(CodegenExpression expressionEventTypeIdPublic) {
        return newInstance(EventTypeMetadata.class,
                constant(name), constant(moduleName),
                enumValue(EventTypeTypeClass.class, typeClass.name()),
                enumValue(EventTypeApplicationType.class, applicationType.name()),
                enumValue(NameAccessModifier.class, accessModifier.name()),
                enumValue(EventTypeBusModifier.class, busModifier.name()),
                constant(isPropertyAgnostic),
                newInstance(EventTypeIdPair.class, expressionEventTypeIdPublic, constant(eventTypeIdPair.getProtectedId())));
    }

    /**
     * Return metadata with the assigned ids
     *
     * @param eventTypeIdPublic    public id
     * @param eventTypeIdProtected protected id
     * @return exppression
     */
    public EventTypeMetadata withIds(long eventTypeIdPublic, long eventTypeIdProtected) {
        return new EventTypeMetadata(name, moduleName, typeClass, applicationType, accessModifier, busModifier, isPropertyAgnostic, new EventTypeIdPair(eventTypeIdPublic, eventTypeIdProtected));
    }

    public String toString() {
        return "EventTypeMetadata{" +
                "name='" + name + '\'' +
                ", typeClass=" + typeClass +
                ", applicationType=" + applicationType +
                ", accessModifier=" + accessModifier +
                ", isPropertyAgnostic=" + isPropertyAgnostic +
                ", eventTypeIdPublic=" + eventTypeIdPair.getPublicId() +
                ", eventTypeIdProtected=" + eventTypeIdPair.getProtectedId() +
                '}';
    }
}
