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
package com.espertech.esper.common.client.type;

import com.espertech.esper.common.internal.collection.Pair;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.namespace.QName;
import java.io.DataInput;
import java.io.DataOutput;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Provides {@link EPType} instances for common types.
 */
public enum EPTypePremade {
    /**
     * boolean
     */
    BOOLEANPRIMITIVE(boolean.class),
    /**
     * byte
     **/
    BYTEPRIMITIVE(byte.class),
    /**
     * char
     **/
    CHARPRIMITIVE(char.class),
    /**
     * short
     **/
    SHORTPRIMITIVE(short.class),
    /**
     * int
     **/
    INTEGERPRIMITIVE(int.class),
    /**
     * long
     **/
    LONGPRIMITIVE(long.class),
    /**
     * double
     **/
    DOUBLEPRIMITIVE(double.class),
    /**
     * float
     **/
    FLOATPRIMITIVE(float.class),
    /**
     * Boolean
     **/
    BOOLEANBOXED(Boolean.class),
    /**
     * Byte
     **/
    BYTEBOXED(Byte.class),
    /**
     * Character
     **/
    CHARBOXED(Character.class),
    /**
     * Short
     **/
    SHORTBOXED(Short.class),
    /**
     * Integer
     **/
    INTEGERBOXED(Integer.class),
    /**
     * Long
     **/
    LONGBOXED(Long.class),
    /**
     * Double
     **/
    DOUBLEBOXED(Double.class),
    /**
     * Float
     **/
    FLOATBOXED(Float.class),
    /**
     * BigInteger
     **/
    BIGINTEGER(BigInteger.class),
    /**
     * BigDecimal
     **/
    BIGDECIMAL(BigDecimal.class),
    /**
     * BigInteger[]
     **/
    BIGINTEGERARRAY(BigInteger[].class),
    /**
     * BigDecimal[]
     **/
    BIGDECIMALARRAY(BigDecimal[].class),
    /**
     * BigInteger[][]
     **/
    BIGINTEGERARRAYARRAY(BigInteger[][].class),
    /**
     * BigDecimal[][]
     **/
    BIGDECIMALARRAYARRAY(BigDecimal[][].class),
    /**
     * String
     **/
    STRING(String.class),
    /**
     * Number
     **/
    NUMBER(Number.class),
    /**
     * Object
     **/
    OBJECT(Object.class),
    /**
     * void
     **/
    VOID(void.class),
    /**
     * CharSequence
     **/
    CHARSEQUENCE(CharSequence.class),
    /**
     * ZonedDateTime
     **/
    ZONEDDATETIME(ZonedDateTime.class),
    /**
     * ZonedDateTime[]
     **/
    ZONEDDATETIMEARRAY(ZonedDateTime[].class),
    /**
     * ZonedDateTime[][]
     **/
    ZONEDDATETIMEARRAYARRAY(ZonedDateTime[][].class),
    /**
     * LocalDateTime
     **/
    LOCALDATETIME(LocalDateTime.class),
    /**
     * LocalDateTime[]
     **/
    LOCALDATETIMEARRAY(LocalDateTime[].class),
    /**
     * LocalDateTime[][]
     **/
    LOCALDATETIMEARRAYARRAY(LocalDateTime[][].class),
    /**
     * LocalDate
     **/
    LOCALDATE(LocalDate.class),
    /**
     * LocalDate[]
     **/
    LOCALDATEARRAY(LocalDate[].class),
    /**
     * LocalDate[][]
     **/
    LOCALDATEARRAYARRAY(LocalDate[][].class),
    /**
     * LocalTime
     **/
    LOCALTIME(LocalTime.class),
    /**
     * Date
     **/
    DATE(Date.class),
    /**
     * Calendar
     **/
    CALENDAR(Calendar.class),
    /**
     * Collection
     **/
    COLLECTION(Collection.class),
    /**
     * List
     **/
    LIST(List.class),
    /**
     * List[]
     **/
    LISTARRAY(List[].class),
    /**
     * ArrayList
     **/
    ARRAYLIST(ArrayList.class),
    /**
     * LinkedList
     **/
    LINKEDLIST(LinkedList.class),
    /**
     * Deque
     **/
    DEQUE(Deque.class),
    /**
     * ArrayDeque
     **/
    ARRAYDEQUE(ArrayDeque.class),
    /**
     * Set
     **/
    SET(Set.class),
    /**
     * HashSet
     **/
    HASHSET(HashSet.class),
    /**
     * Iterator
     **/
    ITERATOR(Iterator.class),
    /**
     * Iterable
     **/
    ITERABLE(Iterable.class),
    /**
     * Object[]
     **/
    OBJECTARRAY(Object[].class),
    /**
     * Object[][]
     **/
    OBJECTARRAYARRAY(Object[][].class),
    /**
     * Pair
     **/
    PAIR(Pair.class),
    /**
     * Map
     **/
    MAP(Map.class),
    /**
     * Map[]
     **/
    MAPARRAY(Map[].class),
    /**
     * TreeMap
     **/
    TREEMAP(TreeMap.class),
    /**
     * TreeSet
     **/
    TREESET(TreeSet.class),
    /**
     * NavigableMap
     **/
    NAVIGABLEMAP(NavigableMap.class),
    /**
     * HashMap
     **/
    HASHMAP(HashMap.class),
    /**
     * LinkedHashMap
     **/
    LINKEDHASHMAP(LinkedHashMap.class),
    /**
     * LinkedHashMap[]
     **/
    LINKEDHASHMAPARRAY(LinkedHashMap[].class),
    /**
     * LinkedHashSet
     **/
    LINKEDHASHSET(LinkedHashSet.class),
    /**
     * Node
     **/
    NODE(Node.class),
    /**
     * NodeList
     **/
    NODELIST(NodeList.class),
    /**
     * QName
     **/
    QNAME(QName.class),
    /**
     * Node[]
     **/
    NODEARRAY(Node[].class),
    /**
     * String[]
     **/
    STRINGARRAY(String[].class),
    /**
     * String[][]
     **/
    STRINGARRAYARRAY(String[][].class),
    /**
     * Boolean[]
     **/
    BOOLEANBOXEDARRAY(Boolean[].class),
    /**
     * Boolean[][]
     **/
    BOOLEANBOXEDARRAYARRAY(Boolean[][].class),
    /**
     * Byte[]
     **/
    BYTEBOXEDARRAY(Byte[].class),
    /**
     * Byte[][]
     **/
    BYTEBOXEDARRAYARRAY(Byte[][].class),
    /**
     * Character[]
     **/
    CHARBOXEDARRAY(Character[].class),
    /**
     * Character[][]
     **/
    CHARBOXEDARRAYARRAY(Character[][].class),
    /**
     * Short[]
     **/
    SHORTBOXEDARRAY(Short[].class),
    /**
     * Short[][]
     **/
    SHORTBOXEDARRAYARRAY(Short[][].class),
    /**
     * Integer[]
     **/
    INTEGERBOXEDARRAY(Integer[].class),
    /**
     * Integer[][]
     **/
    INTEGERBOXEDARRAYARRAY(Integer[][].class),
    /**
     * Long[]
     **/
    LONGBOXEDARRAY(Long[].class),
    /**
     * Long[][]
     **/
    LONGBOXEDARRAYARRAY(Long[][].class),
    /**
     * Double[]
     **/
    DOUBLEBOXEDARRAY(Double[].class),
    /**
     * Double[][]
     **/
    DOUBLEBOXEDARRAYARRAY(Double[][].class),
    /**
     * Float[]
     **/
    FLOATBOXEDARRAY(Float[].class),
    /**
     * Float[][]
     **/
    FLOATBOXEDARRAYARRAY(Float[][].class),
    /**
     * boolean[]
     **/
    BOOLEANPRIMITIVEARRAY(boolean[].class),
    /**
     * boolean[][]
     **/
    BOOLEANPRIMITIVEARRAYARRAY(boolean[][].class),
    /**
     * byte[]
     **/
    BYTEPRIMITIVEARRAY(byte[].class),
    /**
     * byte[][]
     **/
    BYTEPRIMITIVEARRAYARRAY(byte[][].class),
    /**
     * char[]
     **/
    CHARPRIMITIVEARRAY(char[].class),
    /**
     * char[][]
     **/
    CHARPRIMITIVEARRAYARRAY(char[][].class),
    /**
     * short[]
     **/
    SHORTPRIMITIVEARRAY(short[].class),
    /**
     * short[][]
     **/
    SHORTPRIMITIVEARRAYARRAY(short[][].class),
    /**
     * int[]
     **/
    INTEGERPRIMITIVEARRAY(int[].class),
    /**
     * int[][]
     **/
    INTEGERPRIMITIVEARRAYARRAY(int[][].class),
    /**
     * long[]
     **/
    LONGPRIMITIVEARRAY(long[].class),
    /**
     * long[][]
     **/
    LONGPRIMITIVEARRAYARRAY(long[][].class),
    /**
     * double[]
     **/
    DOUBLEPRIMITIVEARRAY(double[].class),
    /**
     * double[][]
     **/
    DOUBLEPRIMITIVEARRAYARRAY(double[][].class),
    /**
     * float[]
     **/
    FLOATPRIMITIVEARRAY(float[].class),
    /**
     * float[][]
     **/
    FLOATPRIMITIVEARRAYARRAY(float[][].class),
    /**
     * CopyOnWriteArrayList
     **/
    COPYONWRITEARRAYLIST(CopyOnWriteArrayList.class),
    /**
     * CopyOnWriteArraySet
     **/
    COPYONWRITEARRAYSET(CopyOnWriteArraySet.class),
    /**
     * Comparator
     **/
    COMPARATOR(Comparator.class),
    /**
     * Comparable
     **/
    COMPARABLE(Comparable.class),
    /**
     * SimpleDateFormat
     **/
    SIMPLEDATEFORMAT(SimpleDateFormat.class),
    /**
     * DateTimeFormatter
     **/
    DATETIMEFORMATTER(DateTimeFormatter.class),
    /**
     * DateFormat
     **/
    DATEFORMAT(DateFormat.class),
    /**
     * java.lang.instrument.Instrumentation
     **/
    INSTRUMENTATION(java.lang.instrument.Instrumentation.class),
    /**
     * java.util.regex.Pattern
     **/
    PATTERN(java.util.regex.Pattern.class),
    /**
     * Class
     **/
    CLASS(Class.class),
    /**
     * Method
     **/
    METHOD(Method.class),
    /**
     * Field
     **/
    FIELD(Field.class),
    /**
     * TimeZone
     **/
    TIMEZONE(TimeZone.class),
    /**
     * AtomicLong
     **/
    ATOMICLONG(AtomicLong.class),
    /**
     * java.math.MathContext
     **/
    MATHCONTEXT(java.math.MathContext.class),
    /**
     * Annotation
     **/
    ANNOTATION(Annotation.class),
    /**
     * Annotation[]
     **/
    ANNOTATIONARRAY(Annotation[].class),
    /**
     * DataOutput
     **/
    DATAOUTPUT(DataOutput.class),
    /**
     * DataInput
     **/
    DATAINPUT(DataInput.class),
    /**
     * ByteBuffer
     **/
    BYTEBUFFER(ByteBuffer.class),
    /**
     * Map.Entry
     **/
    MAPENTRY(Map.Entry.class),
    /**
     * AbstractMap.SimpleEntry
     **/
    ABSTRACTMAPSIMPLEENTRY(AbstractMap.SimpleEntry.class),
    /**
     * StringBuffer
     **/
    STRINGBUFFER(StringBuffer.class),
    /**
     * StringBuilder
     **/
    STRINGBUILDER(StringBuilder.class),
    /**
     * UnsupportedOperationException
     **/
    UNSUPPORTEDOPERATIONEXCEPTION(UnsupportedOperationException.class),
    /**
     * IllegalStateException
     **/
    ILLEGALSTATEEXCEPTION(IllegalStateException.class),
    /**
     * NoSuchElementException
     **/
    NOSUCHELEMENTEXCEPTION(NoSuchElementException.class),
    /**
     * java.io.IOException
     **/
    IOEXCEPTION(java.io.IOException.class),
    /**
     * Throwable
     **/
    THROWABLE(Throwable.class),
    /**
     * java.util.function.Supplier
     **/
    SUPPLIER(java.util.function.Supplier.class),
    /**
     * java.sql.Date
     **/
    SQLDATE(java.sql.Date.class),
    /**
     * java.sql.Time
     **/
    SQLTIME(java.sql.Time.class),
    /**
     * java.sql.Clob
     **/
    SQLCLOB(java.sql.Clob.class),
    /**
     * java.sql.Blob
     **/
    SQLBLOB(java.sql.Blob.class),
    /**
     * java.sql.Array
     **/
    SQLARRAY(java.sql.Array.class),
    /**
     * java.sql.Struct
     **/
    SQLSTRUCT(java.sql.Struct.class),
    /**
     * java.sql.Ref
     **/
    SQLREF(java.sql.Ref.class),
    /**
     * java.sql.Timestamp
     **/
    SQLTIMESTAMP(java.sql.Timestamp.class),
    /**
     * java.net.URL
     **/
    NETURL(java.net.URL.class),
    /**
     * java.net.URL[]
     **/
    NETURLARRAY(java.net.URL[].class),
    /**
     * java.net.URL[][]
     **/
    NETURLARRAYARRAY(java.net.URL[][].class),
    /**
     * java.net.URI
     **/
    NETURI(java.net.URI.class),
    /**
     * java.net.URI[]
     **/
    NETURIARRAY(java.net.URI[].class),
    /**
     * java.net.URI[][]
     **/
    NETURIARRAYARRAY(java.net.URI[][].class),
    /**
     * java.util.UUID
     **/
    UUID(java.util.UUID.class),
    /**
     * java.util.UUID[]
     **/
    UUIDARRAY(java.util.UUID[].class),
    /**
     * java.util.UUID[][]
     **/
    UUIDARRAYARRAY(java.util.UUID[][].class),
    /**
     * OffsetDateTime
     **/
    OFFSETDATETIME(OffsetDateTime.class),
    /**
     * OffsetDateTime[]
     **/
    OFFSETDATETIMEARRAY(OffsetDateTime[].class),
    /**
     * OffsetDateTime[][]
     **/
    OFFSETDATETIMEARRAYARRAY(OffsetDateTime[][].class);

    private static Map<Class, EPTypePremade> classToPremade = new HashMap<>();

    static {
        for (EPTypePremade premade : EPTypePremade.values()) {
            if (classToPremade.containsKey(premade.epType.typeClass)) {
                throw new IllegalStateException("Class '" + premade.epType.typeClass + "' already among premades");
            }
            classToPremade.put(premade.getEPType().getType(), premade);
        }
    }

    private final EPTypeClass epType;

    EPTypePremade(Class<?> clazz) {
        this.epType = new EPTypeClass(clazz);
    }

    /**
     * Returns the pre-allocated EPTypeClass for a given class, or allocates a new EPTypeClass when there is no pre-allocated EPTypeClass for this class
     * @param clazz class
     * @return type class
     */
    public static EPTypeClass getOrCreate(Class<?> clazz) {
        if (clazz == null) {
            throw new IllegalArgumentException("Not available for null clazz");
        }
        EPTypePremade found = classToPremade.get(clazz);
        if (found != null) {
            return found.getEPType();
        }
        return new EPTypeClass(clazz);
    }

    /**
     * Returns the pre-allocated holder for a given class, or null if there is no pre-allocated holder for this class
     * @param clazz class
     * @return type class or null
     */
    public static EPTypePremade getExisting(Class<?> clazz) {
        if (clazz == null) {
            throw new IllegalArgumentException("Not available for null clazz");
        }
        return classToPremade.get(clazz);
    }

    /**
     * Returns the type class.
     * @return type class
     */
    public EPTypeClass getEPType() {
        return epType;
    }
}
