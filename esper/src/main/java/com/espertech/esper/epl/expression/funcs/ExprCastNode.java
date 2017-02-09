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
package com.espertech.esper.epl.expression.funcs;

import com.espertech.esper.client.EPException;
import com.espertech.esper.client.EventBean;
import com.espertech.esper.epl.expression.core.*;
import com.espertech.esper.pattern.observer.TimerScheduleISO8601Parser;
import com.espertech.esper.schedule.ScheduleParameterException;
import com.espertech.esper.util.*;

import java.io.StringWriter;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

/**
 * Represents the CAST(expression, type) function is an expression tree.
 */
public class ExprCastNode extends ExprNodeBase {
    private final String classIdentifier;
    private Class targetType;
    private boolean isConstant;
    private transient ExprEvaluator exprEvaluator;
    private static final long serialVersionUID = 7448449031028156455L;

    /**
     * Ctor.
     *
     * @param classIdentifier the the name of the type to cast to
     */
    public ExprCastNode(String classIdentifier) {
        this.classIdentifier = classIdentifier;
    }

    public ExprEvaluator getExprEvaluator() {
        return exprEvaluator;
    }

    /**
     * Returns the name of the type of cast to.
     *
     * @return type name
     */
    public String getClassIdentifier() {
        return classIdentifier;
    }

    public ExprNode validate(ExprValidationContext validationContext) throws ExprValidationException {
        if (this.getChildNodes().length == 0 || this.getChildNodes().length > 2) {
            throw new ExprValidationException("Cast function node must have one or two child expressions");
        }

        ExprEvaluator valueEvaluator = this.getChildNodes()[0].getExprEvaluator();
        Class fromType = valueEvaluator.getType();

        // determine date format parameter
        Map<String, ExprNamedParameterNode> namedParams = ExprNodeUtility.getNamedExpressionsHandleDups(Arrays.asList(this.getChildNodes()));
        ExprNodeUtility.validateNamed(namedParams, new String[]{"dateformat"});
        ExprNamedParameterNode dateFormatParameter = namedParams.get("dateformat");
        if (dateFormatParameter != null) {
            ExprNodeUtility.validateNamedExpectType(dateFormatParameter, new Class[]{String.class});
        }

        // identify target type
        // try the primitive names including "string"
        SimpleTypeCaster caster;
        targetType = JavaClassHelper.getBoxedType(JavaClassHelper.getPrimitiveClassForName(classIdentifier.trim()));
        boolean numeric;
        CasterParserComputer casterParserComputer = null;
        if (dateFormatParameter != null) {
            if (fromType != String.class) {
                throw new ExprValidationException("Use of the '" + dateFormatParameter.getParameterName() + "' named parameter requires a string-type input");
            }
            if (targetType == null) {
                try {
                    targetType = JavaClassHelper.getClassForName(classIdentifier.trim(), validationContext.getEngineImportService().getClassForNameProvider());
                } catch (ClassNotFoundException e) {
                    // expected
                }
            }

            // dynamic or static date format
            numeric = false;
            caster = null;
            if (targetType == Date.class || classIdentifier.trim().toLowerCase(Locale.ENGLISH).equals("date")) {
                targetType = Date.class;
                ExprCastNodeDateDesc desc = validateDateFormat(dateFormatParameter, validationContext, false);
                if (desc.getStaticDateFormat() != null) {
                    if (desc.iso8601Format()) {
                        casterParserComputer = new StringToDateWStaticISOFormatComputer();
                    } else {
                        casterParserComputer = new StringToDateWStaticFormatComputer(desc.getStaticDateFormat());
                    }
                } else {
                    casterParserComputer = new StringToDateWDynamicFormatComputer(desc.getDynamicDateFormat());
                }
            } else if (targetType == Calendar.class || classIdentifier.trim().toLowerCase(Locale.ENGLISH).equals("calendar")) {
                targetType = Calendar.class;
                ExprCastNodeDateDesc desc = validateDateFormat(dateFormatParameter, validationContext, false);
                if (desc.getStaticDateFormat() != null) {
                    if (desc.iso8601Format()) {
                        casterParserComputer = new StringToCalendarWStaticISOFormatComputer();
                    } else {
                        casterParserComputer = new StringToCalendarWStaticFormatComputer(desc.getStaticDateFormat(), validationContext.getEngineImportService().getTimeZone());
                    }
                } else {
                    casterParserComputer = new StringToCalendarWDynamicFormatComputer(desc.getDynamicDateFormat(), validationContext.getEngineImportService().getTimeZone());
                }
            } else if (targetType == Long.class) {
                targetType = Long.class;
                ExprCastNodeDateDesc desc = validateDateFormat(dateFormatParameter, validationContext, false);
                if (desc.getStaticDateFormat() != null) {
                    if (desc.iso8601Format()) {
                        casterParserComputer = new StringToLongWStaticISOFormatComputer();
                    } else {
                        casterParserComputer = new StringToLongWStaticFormatComputer(desc.getStaticDateFormat());
                    }
                } else {
                    casterParserComputer = new StringToLongWDynamicFormatComputer(desc.getDynamicDateFormat());
                }
            } else if (targetType == LocalDateTime.class || classIdentifier.trim().toLowerCase(Locale.ENGLISH).equals("localdatetime")) {
                targetType = LocalDateTime.class;
                ExprCastNodeDateDesc desc = validateDateFormat(dateFormatParameter, validationContext, true);
                if (desc.getStaticDateFormat() != null) {
                    if (desc.iso8601Format()) {
                        casterParserComputer = new StringToLocalDateTimeWStaticFormatComputer(DateTimeFormatter.ISO_DATE_TIME);
                    } else {
                        casterParserComputer = new StringToLocalDateTimeWStaticFormatComputer(desc.getDateTimeFormatter());
                    }
                } else {
                    casterParserComputer = new StringToLocalDateTimeWDynamicFormatComputer(desc.getDynamicDateFormat());
                }
            } else if (targetType == LocalDate.class || classIdentifier.trim().toLowerCase(Locale.ENGLISH).equals("localdate")) {
                targetType = LocalDate.class;
                ExprCastNodeDateDesc desc = validateDateFormat(dateFormatParameter, validationContext, true);
                if (desc.getStaticDateFormat() != null) {
                    if (desc.iso8601Format()) {
                        casterParserComputer = new StringToLocalDateWStaticFormatComputer(DateTimeFormatter.ISO_DATE);
                    } else {
                        casterParserComputer = new StringToLocalDateWStaticFormatComputer(desc.getDateTimeFormatter());
                    }
                } else {
                    casterParserComputer = new StringToLocalDateWDynamicFormatComputer(desc.getDynamicDateFormat());
                }
            } else if (targetType == LocalTime.class || classIdentifier.trim().toLowerCase(Locale.ENGLISH).equals("localtime")) {
                targetType = LocalTime.class;
                ExprCastNodeDateDesc desc = validateDateFormat(dateFormatParameter, validationContext, true);
                if (desc.getStaticDateFormat() != null) {
                    if (desc.iso8601Format()) {
                        casterParserComputer = new StringToLocalTimeWStaticFormatComputer(DateTimeFormatter.ISO_TIME);
                    } else {
                        casterParserComputer = new StringToLocalTimeWStaticFormatComputer(desc.getDateTimeFormatter());
                    }
                } else {
                    casterParserComputer = new StringToLocalTimeWDynamicFormatComputer(desc.getDynamicDateFormat());
                }
            } else if (targetType == ZonedDateTime.class || classIdentifier.trim().toLowerCase(Locale.ENGLISH).equals("zoneddatetime")) {
                targetType = ZonedDateTime.class;
                ExprCastNodeDateDesc desc = validateDateFormat(dateFormatParameter, validationContext, true);
                if (desc.getStaticDateFormat() != null) {
                    if (desc.iso8601Format()) {
                        casterParserComputer = new StringToZonedDateTimeWStaticFormatComputer(DateTimeFormatter.ISO_ZONED_DATE_TIME);
                    } else {
                        casterParserComputer = new StringToZonedDateTimeWStaticFormatComputer(desc.getDateTimeFormatter());
                    }
                } else {
                    casterParserComputer = new StringToZonedDateTimeWDynamicFormatComputer(desc.getDynamicDateFormat());
                }
            } else {
                throw new ExprValidationException("Use of the '" + dateFormatParameter.getParameterName() + "' named parameter requires a target type of calendar, date, long, localdatetime, localdate, localtime or zoneddatetime");
            }
        } else if (targetType != null) {
            targetType = JavaClassHelper.getBoxedType(targetType);
            caster = SimpleTypeCasterFactory.getCaster(fromType, targetType);
            numeric = caster.isNumericCast();
        } else if (classIdentifier.trim().toLowerCase(Locale.ENGLISH).equals("BigInteger".toLowerCase(Locale.ENGLISH))) {
            targetType = BigInteger.class;
            caster = SimpleTypeCasterFactory.getCaster(fromType, targetType);
            numeric = true;
        } else if (classIdentifier.trim().toLowerCase(Locale.ENGLISH).equals("BigDecimal".toLowerCase(Locale.ENGLISH))) {
            targetType = BigDecimal.class;
            caster = SimpleTypeCasterFactory.getCaster(fromType, targetType);
            numeric = true;
        } else {
            try {
                targetType = JavaClassHelper.getClassForName(classIdentifier.trim(), validationContext.getEngineImportService().getClassForNameProvider());
            } catch (ClassNotFoundException e) {
                throw new ExprValidationException("Class as listed in cast function by name '" + classIdentifier + "' cannot be loaded", e);
            }
            numeric = JavaClassHelper.isNumeric(targetType);
            if (numeric) {
                caster = SimpleTypeCasterFactory.getCaster(fromType, targetType);
            } else {
                caster = new SimpleTypeCasterAnyType(targetType);
            }
        }

        // assign a computer unless already assigned
        if (casterParserComputer == null) {
            // to-string
            if (targetType == String.class) {
                casterParserComputer = new StringXFormComputer();
            } else if (fromType == String.class) {
                // parse
                SimpleTypeParser parser = SimpleTypeParserFactory.getParser(JavaClassHelper.getBoxedType(targetType));
                casterParserComputer = new StringParserComputer(parser);
            } else if (numeric) {
                // numeric cast with check
                casterParserComputer = new NumberCasterComputer(caster);
            } else {
                // non-numeric cast
                casterParserComputer = new NonnumericCasterComputer(caster);
            }
        }

        // determine constant or not
        Object theConstant = null;
        if (this.getChildNodes()[0].isConstantResult()) {
            isConstant = casterParserComputer.isConstantForConstInput();
            if (isConstant) {
                Object in = valueEvaluator.evaluate(null, true, validationContext.getExprEvaluatorContext());
                theConstant = casterParserComputer.compute(in, null, true, validationContext.getExprEvaluatorContext());
            }
        }

        // determine evaluator
        if (isConstant) {
            exprEvaluator = new ExprCastNodeConstEval(this, theConstant);
        } else {
            exprEvaluator = new ExprCastNodeNonConstEval(this, valueEvaluator, casterParserComputer);
        }
        return null;
    }

    public boolean isConstantResult() {
        return isConstant;
    }

    public Class getTargetType() {
        return targetType;
    }

    public void toPrecedenceFreeEPL(StringWriter writer) {
        writer.append("cast(");
        this.getChildNodes()[0].toEPL(writer, ExprPrecedenceEnum.MINIMUM);
        writer.append(",");
        writer.append(classIdentifier);
        for (int i = 1; i < this.getChildNodes().length; i++) {
            writer.write(",");
            this.getChildNodes()[i].toEPL(writer, ExprPrecedenceEnum.MINIMUM);
        }
        writer.append(')');
    }

    public ExprPrecedenceEnum getPrecedence() {
        return ExprPrecedenceEnum.UNARY;
    }

    public boolean equalsNode(ExprNode node) {
        if (!(node instanceof ExprCastNode)) {
            return false;
        }
        ExprCastNode other = (ExprCastNode) node;
        return other.classIdentifier.equals(this.classIdentifier);
    }

    /**
     * Casting and parsing computer.
     */
    public interface CasterParserComputer {
        /**
         * Compute an result performing casting and parsing.
         *
         * @param input                to process
         * @param eventsPerStream      events per stream
         * @param newData              new data indicator
         * @param exprEvaluatorContext evaluation context
         * @return cast or parse result
         */
        public Object compute(Object input, EventBean[] eventsPerStream, boolean newData, ExprEvaluatorContext exprEvaluatorContext);

        public boolean isConstantForConstInput();
    }

    /**
     * Casting and parsing computer.
     */
    public static class StringXFormComputer implements CasterParserComputer {
        public Object compute(Object input, EventBean[] eventsPerStream, boolean newData, ExprEvaluatorContext exprEvaluatorContext) {
            return input.toString();
        }

        public boolean isConstantForConstInput() {
            return true;
        }
    }

    /**
     * Casting and parsing computer.
     */
    public static class NumberCasterComputer implements CasterParserComputer {
        private final SimpleTypeCaster numericTypeCaster;

        public NumberCasterComputer(SimpleTypeCaster numericTypeCaster) {
            this.numericTypeCaster = numericTypeCaster;
        }

        public Object compute(Object input, EventBean[] eventsPerStream, boolean newData, ExprEvaluatorContext exprEvaluatorContext) {
            if (input instanceof Number) {
                return numericTypeCaster.cast(input);
            }
            return null;
        }

        public boolean isConstantForConstInput() {
            return true;
        }
    }

    /**
     * Casting and parsing computer.
     */
    public static class StringParserComputer implements CasterParserComputer {
        private final SimpleTypeParser parser;

        public StringParserComputer(SimpleTypeParser parser) {
            this.parser = parser;
        }

        public Object compute(Object input, EventBean[] eventsPerStream, boolean newData, ExprEvaluatorContext exprEvaluatorContext) {
            return parser.parse(input.toString());
        }

        public boolean isConstantForConstInput() {
            return true;
        }
    }

    /**
     * Casting and parsing computer.
     */
    public static class NonnumericCasterComputer implements CasterParserComputer {
        private final SimpleTypeCaster caster;

        public NonnumericCasterComputer(SimpleTypeCaster numericTypeCaster) {
            this.caster = numericTypeCaster;
        }

        public Object compute(Object input, EventBean[] eventsPerStream, boolean newData, ExprEvaluatorContext exprEvaluatorContext) {
            return caster.cast(input);
        }

        public boolean isConstantForConstInput() {
            return true;
        }
    }

    public static EPException handleParseException(String formatString, String date, Exception ex) {
        return new EPException("Exception parsing date '" + date + "' format '" + formatString + "': " + ex.getMessage(), ex);
    }

    public static EPException handleParseISOException(String date, ScheduleParameterException ex) {
        return new EPException("Exception parsing iso8601 date '" + date + "': " + ex.getMessage(), ex);
    }

    public static abstract class StringToDateLongWStaticFormat implements CasterParserComputer {
        protected final String dateFormat;
        protected final ThreadLocal<SimpleDateFormat> formats = new ThreadLocal<SimpleDateFormat>() {
            protected synchronized SimpleDateFormat initialValue() {
                return new SimpleDateFormat(dateFormat);
            }
        };

        protected StringToDateLongWStaticFormat(String dateFormat) {
            this.dateFormat = dateFormat;
        }

        public boolean isConstantForConstInput() {
            return true;
        }
    }

    public static abstract class StringToDateLongWDynamicFormat implements CasterParserComputer {
        private final ExprEvaluator dateFormatEval;

        protected StringToDateLongWDynamicFormat(ExprEvaluator dateFormatEval) {
            this.dateFormatEval = dateFormatEval;
        }

        protected abstract Object computeFromFormat(String dateFormat, SimpleDateFormat format, Object input) throws ParseException;

        public boolean isConstantForConstInput() {
            return false;
        }

        public Object compute(Object input, EventBean[] eventsPerStream, boolean newData, ExprEvaluatorContext exprEvaluatorContext) {
            Object format = dateFormatEval.evaluate(eventsPerStream, newData, exprEvaluatorContext);
            if (format == null) {
                throw new EPException("Null date format returned by 'dateformat' expression");
            }
            SimpleDateFormat dateFormat;
            try {
                dateFormat = new SimpleDateFormat(format.toString());
            } catch (RuntimeException ex) {
                throw new EPException("Invalid date format '" + format.toString() + "': " + ex.getMessage(), ex);
            }
            try {
                return computeFromFormat(format.toString(), dateFormat, input);
            } catch (ParseException ex) {
                throw handleParseException(format.toString(), input.toString(), ex);
            }
        }
    }

    public static class StringToDateWStaticFormatComputer extends StringToDateLongWStaticFormat {
        public StringToDateWStaticFormatComputer(String dateFormat) {
            super(dateFormat);
        }

        public Object compute(Object input, EventBean[] eventsPerStream, boolean newData, ExprEvaluatorContext exprEvaluatorContext) {
            return parseSafe(dateFormat, formats.get(), input);
        }

        protected static Object parseSafe(String formatString, SimpleDateFormat format, Object input) {
            try {
                return format.parse(input.toString());
            } catch (ParseException e) {
                throw handleParseException(formatString, input.toString(), e);
            }
        }
    }

    public abstract static class StringToJava8WStaticFormatComputer implements CasterParserComputer {
        protected final DateTimeFormatter formatter;

        public StringToJava8WStaticFormatComputer(DateTimeFormatter formatter) {
            this.formatter = formatter;
        }

        public boolean isConstantForConstInput() {
            return true;
        }

        public abstract Object parse(String input);

        public Object compute(Object input, EventBean[] eventsPerStream, boolean newData, ExprEvaluatorContext exprEvaluatorContext) {
            try {
                return parse(input.toString());
            } catch (DateTimeParseException e) {
                throw handleParseException(formatter.toString(), input.toString(), e);
            }
        }
    }

    public static class StringToLocalDateTimeWStaticFormatComputer extends StringToJava8WStaticFormatComputer {
        public StringToLocalDateTimeWStaticFormatComputer(DateTimeFormatter formatter) {
            super(formatter);
        }

        public Object parse(String input) {
            return LocalDateTime.parse(input, formatter);
        }
    }

    public static class StringToLocalDateWStaticFormatComputer extends StringToJava8WStaticFormatComputer {
        public StringToLocalDateWStaticFormatComputer(DateTimeFormatter formatter) {
            super(formatter);
        }

        public Object parse(String input) {
            return LocalDate.parse(input, formatter);
        }
    }

    public static class StringToLocalTimeWStaticFormatComputer extends StringToJava8WStaticFormatComputer {
        public StringToLocalTimeWStaticFormatComputer(DateTimeFormatter formatter) {
            super(formatter);
        }

        public Object parse(String input) {
            return LocalTime.parse(input, formatter);
        }
    }

    public static class StringToZonedDateTimeWStaticFormatComputer extends StringToJava8WStaticFormatComputer {
        public StringToZonedDateTimeWStaticFormatComputer(DateTimeFormatter formatter) {
            super(formatter);
        }

        public Object parse(String input) {
            return ZonedDateTime.parse(input, formatter);
        }
    }

    public abstract static class StringToJava8WDynamicFormatComputer implements CasterParserComputer {
        private final ExprEvaluator dateFormatEval;

        public StringToJava8WDynamicFormatComputer(ExprEvaluator dateFormatEval) {
            this.dateFormatEval = dateFormatEval;
        }

        public boolean isConstantForConstInput() {
            return false;
        }

        public abstract Object parse(String input, DateTimeFormatter formatter);

        public Object compute(Object input, EventBean[] eventsPerStream, boolean newData, ExprEvaluatorContext exprEvaluatorContext) {
            Object format = dateFormatEval.evaluate(eventsPerStream, newData, exprEvaluatorContext);
            if (format == null) {
                throw new EPException("Null date format returned by 'dateformat' expression");
            }
            DateTimeFormatter dateFormat;
            try {
                dateFormat = DateTimeFormatter.ofPattern(format.toString());
            } catch (RuntimeException ex) {
                throw new EPException("Invalid date format '" + format.toString() + "': " + ex.getMessage(), ex);
            }
            try {
                return parse(input.toString(), dateFormat);
            } catch (DateTimeParseException e) {
                throw handleParseException(dateFormat.toString(), input.toString(), e);
            }
        }
    }

    public static class StringToLocalDateTimeWDynamicFormatComputer extends StringToJava8WDynamicFormatComputer {
        public StringToLocalDateTimeWDynamicFormatComputer(ExprEvaluator dateFormatEval) {
            super(dateFormatEval);
        }

        public Object parse(String input, DateTimeFormatter formatter) {
            return LocalDateTime.parse(input, formatter);
        }
    }

    public static class StringToLocalDateWDynamicFormatComputer extends StringToJava8WDynamicFormatComputer {
        public StringToLocalDateWDynamicFormatComputer(ExprEvaluator dateFormatEval) {
            super(dateFormatEval);
        }

        public Object parse(String input, DateTimeFormatter formatter) {
            return LocalDate.parse(input, formatter);
        }
    }

    public static class StringToLocalTimeWDynamicFormatComputer extends StringToJava8WDynamicFormatComputer {
        public StringToLocalTimeWDynamicFormatComputer(ExprEvaluator dateFormatEval) {
            super(dateFormatEval);
        }

        public Object parse(String input, DateTimeFormatter formatter) {
            return LocalTime.parse(input, formatter);
        }
    }

    public static class StringToZonedDateTimeWDynamicFormatComputer extends StringToJava8WDynamicFormatComputer {
        public StringToZonedDateTimeWDynamicFormatComputer(ExprEvaluator dateFormatEval) {
            super(dateFormatEval);
        }

        public Object parse(String input, DateTimeFormatter formatter) {
            return ZonedDateTime.parse(input, formatter);
        }
    }

    public static class StringToDateWStaticISOFormatComputer implements CasterParserComputer {

        public Object compute(Object input, EventBean[] eventsPerStream, boolean newData, ExprEvaluatorContext exprEvaluatorContext) {
            try {
                return TimerScheduleISO8601Parser.parseDate(input.toString()).getTime();
            } catch (ScheduleParameterException e) {
                throw handleParseISOException(input.toString(), e);
            }
        }

        public boolean isConstantForConstInput() {
            return true;
        }
    }

    public static class StringToLongWStaticFormatComputer extends StringToDateLongWStaticFormat {
        public StringToLongWStaticFormatComputer(String dateFormat) {
            super(dateFormat);
        }

        public Object compute(Object input, EventBean[] eventsPerStream, boolean newData, ExprEvaluatorContext exprEvaluatorContext) {
            return parseSafe(dateFormat, formats.get(), input);
        }

        protected static Object parseSafe(String dateFormat, SimpleDateFormat format, Object input) {
            try {
                return format.parse(input.toString()).getTime();
            } catch (ParseException e) {
                throw handleParseException(dateFormat, input.toString(), e);
            }
        }
    }

    public static class StringToLongWStaticISOFormatComputer implements CasterParserComputer {
        public Object compute(Object input, EventBean[] eventsPerStream, boolean newData, ExprEvaluatorContext exprEvaluatorContext) {
            try {
                return TimerScheduleISO8601Parser.parseDate(input.toString()).getTimeInMillis();
            } catch (ScheduleParameterException ex) {
                throw handleParseISOException(input.toString(), ex);
            }
        }

        public boolean isConstantForConstInput() {
            return true;
        }
    }

    public static class StringToCalendarWStaticFormatComputer extends StringToDateLongWStaticFormat {

        private final TimeZone timeZone;

        public StringToCalendarWStaticFormatComputer(String dateFormat, TimeZone timeZone) {
            super(dateFormat);
            this.timeZone = timeZone;
        }

        public Object compute(Object input, EventBean[] eventsPerStream, boolean newData, ExprEvaluatorContext exprEvaluatorContext) {
            return parse(dateFormat, formats.get(), input, timeZone);
        }

        protected static Object parse(String formatString, SimpleDateFormat format, Object input, TimeZone timeZone) {
            try {
                Calendar cal = Calendar.getInstance(timeZone);
                Date date = format.parse(input.toString());
                cal.setTime(date);
                return cal;
            } catch (ParseException ex) {
                throw handleParseException(formatString, input.toString(), ex);
            }
        }
    }

    public static class StringToCalendarWStaticISOFormatComputer implements CasterParserComputer {

        public Object compute(Object input, EventBean[] eventsPerStream, boolean newData, ExprEvaluatorContext exprEvaluatorContext) {
            try {
                return TimerScheduleISO8601Parser.parseDate(input.toString());
            } catch (ScheduleParameterException ex) {
                throw handleParseISOException(input.toString(), ex);
            }
        }

        public boolean isConstantForConstInput() {
            return true;
        }
    }

    public static class StringToDateWDynamicFormatComputer extends StringToDateLongWDynamicFormat {
        public StringToDateWDynamicFormatComputer(ExprEvaluator dateFormatEval) {
            super(dateFormatEval);
        }

        protected Object computeFromFormat(String formatString, SimpleDateFormat format, Object input) {
            return StringToDateWStaticFormatComputer.parseSafe(formatString, format, input);
        }
    }

    public static class StringToLongWDynamicFormatComputer extends StringToDateLongWDynamicFormat {
        public StringToLongWDynamicFormatComputer(ExprEvaluator dateFormatEval) {
            super(dateFormatEval);
        }

        protected Object computeFromFormat(String dateFormat, SimpleDateFormat format, Object input) {
            return StringToLongWStaticFormatComputer.parseSafe(dateFormat, format, input);
        }
    }

    public static class StringToCalendarWDynamicFormatComputer extends StringToDateLongWDynamicFormat {

        private final TimeZone timeZone;

        public StringToCalendarWDynamicFormatComputer(ExprEvaluator dateFormatEval, TimeZone timeZone) {
            super(dateFormatEval);
            this.timeZone = timeZone;
        }

        protected Object computeFromFormat(String formatString, SimpleDateFormat format, Object input) {
            return StringToCalendarWStaticFormatComputer.parse(formatString, format, input, timeZone);
        }
    }

    private ExprCastNodeDateDesc validateDateFormat(ExprNamedParameterNode dateFormatParameter, ExprValidationContext validationContext, boolean java8Formatter) throws ExprValidationException {
        String staticDateFormat = null;
        ExprEvaluator dynamicDateFormat = null;
        boolean iso8601Format = false;
        DateTimeFormatter dateTimeFormatter = null;

        if (!dateFormatParameter.getChildNodes()[0].isConstantResult()) {
            dynamicDateFormat = dateFormatParameter.getChildNodes()[0].getExprEvaluator();
        } else {
            staticDateFormat = (String) dateFormatParameter.getChildNodes()[0].getExprEvaluator().evaluate(null, true, validationContext.getExprEvaluatorContext());
            if (staticDateFormat.toLowerCase(Locale.ENGLISH).trim().equals("iso")) {
                iso8601Format = true;
            } else {
                if (!java8Formatter) {
                    try {
                        new SimpleDateFormat(staticDateFormat);
                    } catch (RuntimeException ex) {
                        throw new ExprValidationException("Invalid date format '" + staticDateFormat + "' (as obtained from new SimpleDateFormat): " + ex.getMessage(), ex);
                    }
                } else {
                    try {
                        dateTimeFormatter = DateTimeFormatter.ofPattern(staticDateFormat);
                    } catch (RuntimeException ex) {
                        throw new ExprValidationException("Invalid date format '" + staticDateFormat + "' (as obtained from DateTimeFormatter.ofPattern): " + ex.getMessage(), ex);
                    }
                }
            }
        }
        return new ExprCastNodeDateDesc(staticDateFormat, dynamicDateFormat, iso8601Format, dateTimeFormatter);
    }
}
