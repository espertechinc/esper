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
package com.espertech.esper.epl.enummethod.dot;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.codegen.base.CodegenClassScope;
import com.espertech.esper.codegen.base.CodegenMember;
import com.espertech.esper.codegen.base.CodegenMethodScope;
import com.espertech.esper.codegen.model.expression.CodegenExpression;
import com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder;
import com.espertech.esper.epl.rettype.EPType;
import com.espertech.esper.epl.rettype.EPTypeHelper;
import com.espertech.esper.event.EventAdapterService;
import com.espertech.esper.event.bean.BeanEventType;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.newInstance;

public class ExprDotStaticMethodWrapArrayEvents implements ExprDotStaticMethodWrap {
    private EventAdapterService eventAdapterService;
    private BeanEventType type;

    public ExprDotStaticMethodWrapArrayEvents(EventAdapterService eventAdapterService, BeanEventType type) {
        this.eventAdapterService = eventAdapterService;
        this.type = type;
    }

    public EPType getTypeInfo() {
        return EPTypeHelper.collectionOfEvents(type);
    }

    public Collection convertNonNull(Object result) {
        if (!result.getClass().isArray()) {
            return null;
        }
        return new WrappingCollection(eventAdapterService, type, result);
    }

    public CodegenExpression codegenConvertNonNull(CodegenExpression result, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        CodegenMember eventSvcMember = codegenClassScope.makeAddMember(EventAdapterService.class, eventAdapterService);
        CodegenMember typeMember = codegenClassScope.makeAddMember(BeanEventType.class, type);
        return newInstance(ExprDotStaticMethodWrapArrayEvents.WrappingCollection.class, CodegenExpressionBuilder.member(eventSvcMember.getMemberId()), CodegenExpressionBuilder.member(typeMember.getMemberId()), result);
    }

    public static class WrappingCollection implements Collection<EventBean> {

        private EventAdapterService eventAdapterService;
        private BeanEventType type;
        private Object array;

        public WrappingCollection(EventAdapterService eventAdapterService, BeanEventType type, Object array) {
            this.eventAdapterService = eventAdapterService;
            this.type = type;
            this.array = array;
        }

        public int size() {
            return Array.getLength(array);
        }

        public boolean isEmpty() {
            return size() == 0;
        }

        public Iterator<EventBean> iterator() {
            return new WrappingIterator(eventAdapterService, type, array);
        }

        public boolean contains(Object o) {
            throw new UnsupportedOperationException("Partial implementation");
        }

        public Object[] toArray() {
            throw new UnsupportedOperationException("Partial implementation");
        }

        public <T> T[] toArray(T[] a) {
            throw new UnsupportedOperationException("Partial implementation");
        }

        public boolean add(EventBean eventBean) {
            throw new UnsupportedOperationException("Read-only implementation");
        }

        public boolean remove(Object o) {
            throw new UnsupportedOperationException("Read-only implementation");
        }

        public boolean containsAll(Collection<?> c) {
            throw new UnsupportedOperationException("Read-only implementation");
        }

        public boolean addAll(Collection<? extends EventBean> c) {
            throw new UnsupportedOperationException("Read-only implementation");
        }

        public boolean removeAll(Collection<?> c) {
            throw new UnsupportedOperationException("Read-only implementation");
        }

        public boolean retainAll(Collection<?> c) {
            throw new UnsupportedOperationException("Read-only implementation");
        }

        public void clear() {
            throw new UnsupportedOperationException("Read-only implementation");
        }
    }

    public static class WrappingIterator implements Iterator<EventBean> {
        private EventAdapterService eventAdapterService;
        private BeanEventType type;
        private Object array;
        private int count;

        public WrappingIterator(EventAdapterService eventAdapterService, BeanEventType type, Object array) {
            this.eventAdapterService = eventAdapterService;
            this.type = type;
            this.array = array;
        }

        public boolean hasNext() {
            if (Array.getLength(array) > count) {
                return true;
            }
            return false;
        }

        public EventBean next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            Object next = Array.get(array, count++);
            if (next == null) {
                return null;
            }
            return eventAdapterService.adapterForTypedBean(next, type);
        }

        public void remove() {
            throw new UnsupportedOperationException();
        }
    }
}
