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
import com.espertech.esper.codegen.base.CodegenClassScope;
import com.espertech.esper.codegen.base.CodegenMember;
import com.espertech.esper.codegen.base.CodegenMethodScope;
import com.espertech.esper.codegen.model.expression.CodegenExpression;
import com.espertech.esper.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.codegen.base.CodegenMethodNode;
import com.espertech.esper.epl.expression.core.*;
import com.espertech.esper.epl.util.ExprNodeUtilityRich;
import com.espertech.esper.pattern.observer.TimerScheduleISO8601Parser;
import com.espertech.esper.schedule.ScheduleParameterException;
import com.espertech.esper.util.*;

import java.io.StringWriter;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.*;

/**
 * Represents the CAST(expression, type) function is an expression tree.
 */
public class ExprCastNode extends ExprNodeBase {
    private static final long serialVersionUID = 7448449031028156455L;

    private final String classIdentifier;

    private transient ExprCastNodeForge forge;

    /**
     * Ctor.
     *
     * @param classIdentifier the the name of the type to cast to
     */
    public ExprCastNode(String classIdentifier) {
        this.classIdentifier = classIdentifier;
    }

    public ExprEvaluator getExprEvaluator() {
        checkValidated(forge);
        return forge.getExprEvaluator();
    }

    public ExprForge getForge() {
        checkValidated(forge);
        return forge;
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

        Class fromType = this.getChildNodes()[0].getForge().getEvaluationType();

        // determine date format parameter
        Map<String, ExprNamedParameterNode> namedParams = ExprNodeUtilityRich.getNamedExpressionsHandleDups(Arrays.asList(this.getChildNodes()));
        ExprNodeUtilityRich.validateNamed(namedParams, new String[]{"dateformat"});
        ExprNamedParameterNode dateFormatParameter = namedParams.get("dateformat");
        if (dateFormatParameter != null) {
            ExprNodeUtilityRich.validateNamedExpectType(dateFormatParameter, new Class[]{String.class, DateFormat.class, DateTimeFormatter.class});
        }

        // identify target type
        // try the primitive names including "string"
        SimpleTypeCaster caster;
        Class targetType = JavaClassHelper.getBoxedType(JavaClassHelper.getPrimitiveClassForName(classIdentifier.trim()));
        boolean numeric;
        CasterParserComputerForge casterParserComputerForge = null;
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
                if (desc.isIso8601Format()) {
                    casterParserComputerForge = new StringToDateWStaticISOFormatComputer();
                } else if (desc.getDateFormat() != null) {
                    casterParserComputerForge = new StringToDateWStaticFormatComputer(desc.getStaticDateFormatString(), desc.getDateFormat());
                } else {
                    casterParserComputerForge = new StringToDateWDynamicFormatComputerForge(desc.getDynamicDateFormat());
                }
            } else if (targetType == Calendar.class || classIdentifier.trim().toLowerCase(Locale.ENGLISH).equals("calendar")) {
                targetType = Calendar.class;
                ExprCastNodeDateDesc desc = validateDateFormat(dateFormatParameter, validationContext, false);
                if (desc.isIso8601Format()) {
                    casterParserComputerForge = new StringToCalendarWStaticISOFormatComputer();
                } else if (desc.getDateFormat() != null) {
                    casterParserComputerForge = new StringToCalendarWStaticFormatComputer(desc.getStaticDateFormatString(), desc.getDateFormat(), validationContext.getEngineImportService().getTimeZone());
                } else {
                    casterParserComputerForge = new StringToCalendarWDynamicFormatComputer(desc.getDynamicDateFormat(), validationContext.getEngineImportService().getTimeZone());
                }
            } else if (targetType == Long.class) {
                targetType = Long.class;
                ExprCastNodeDateDesc desc = validateDateFormat(dateFormatParameter, validationContext, false);
                if (desc.isIso8601Format()) {
                    casterParserComputerForge = new StringToLongWStaticISOFormatComputer();
                } else if (desc.getDateFormat() != null) {
                    casterParserComputerForge = new StringToLongWStaticFormatComputer(desc.getStaticDateFormatString(), desc.getDateFormat());
                } else {
                    casterParserComputerForge = new StringToLongWDynamicFormatComputerForge(desc.getDynamicDateFormat());
                }
            } else if (targetType == LocalDateTime.class || classIdentifier.trim().toLowerCase(Locale.ENGLISH).equals("localdatetime")) {
                targetType = LocalDateTime.class;
                ExprCastNodeDateDesc desc = validateDateFormat(dateFormatParameter, validationContext, true);
                if (desc.isIso8601Format()) {
                    casterParserComputerForge = new StringToLocalDateTimeWStaticFormatComputer(DateTimeFormatter.ISO_DATE_TIME);
                } else if (desc.getDateTimeFormatter() != null) {
                    casterParserComputerForge = new StringToLocalDateTimeWStaticFormatComputer(desc.getDateTimeFormatter());
                } else {
                    casterParserComputerForge = new StringToLocalDateTimeWDynamicFormatComputerForge(desc.getDynamicDateFormat());
                }
            } else if (targetType == LocalDate.class || classIdentifier.trim().toLowerCase(Locale.ENGLISH).equals("localdate")) {
                targetType = LocalDate.class;
                ExprCastNodeDateDesc desc = validateDateFormat(dateFormatParameter, validationContext, true);
                if (desc.isIso8601Format()) {
                    casterParserComputerForge = new StringToLocalDateWStaticFormatComputer(DateTimeFormatter.ISO_DATE);
                } else if (desc.getDateTimeFormatter() != null) {
                    casterParserComputerForge = new StringToLocalDateWStaticFormatComputer(desc.getDateTimeFormatter());
                } else {
                    casterParserComputerForge = new StringToLocalDateWDynamicFormatComputerForge(desc.getDynamicDateFormat());
                }
            } else if (targetType == LocalTime.class || classIdentifier.trim().toLowerCase(Locale.ENGLISH).equals("localtime")) {
                targetType = LocalTime.class;
                ExprCastNodeDateDesc desc = validateDateFormat(dateFormatParameter, validationContext, true);
                if (desc.isIso8601Format()) {
                    casterParserComputerForge = new StringToLocalTimeWStaticFormatComputer(DateTimeFormatter.ISO_TIME);
                } else if (desc.getDateTimeFormatter() != null) {
                    casterParserComputerForge = new StringToLocalTimeWStaticFormatComputer(desc.getDateTimeFormatter());
                } else {
                    casterParserComputerForge = new StringToLocalTimeWDynamicFormatComputerForge(desc.getDynamicDateFormat());
                }
            } else if (targetType == ZonedDateTime.class || classIdentifier.trim().toLowerCase(Locale.ENGLISH).equals("zoneddatetime")) {
                targetType = ZonedDateTime.class;
                ExprCastNodeDateDesc desc = validateDateFormat(dateFormatParameter, validationContext, true);
                if (desc.isIso8601Format()) {
                    casterParserComputerForge = new StringToZonedDateTimeWStaticFormatComputer(DateTimeFormatter.ISO_ZONED_DATE_TIME);
                } else if (desc.getDateTimeFormatter() != null) {
                    casterParserComputerForge = new StringToZonedDateTimeWStaticFormatComputer(desc.getDateTimeFormatter());
                } else {
                    casterParserComputerForge = new StringToZonedDateTimeWDynamicFormatComputerForge(desc.getDynamicDateFormat());
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
        if (casterParserComputerForge == null) {
            // to-string
            if (targetType == String.class) {
                casterParserComputerForge = new StringXFormComputer();
            } else if (fromType == String.class && targetType != Character.class) {
                // parse
                SimpleTypeParser parser = SimpleTypeParserFactory.getParser(JavaClassHelper.getBoxedType(targetType));
                casterParserComputerForge = new StringParserComputer(parser);
            } else if (numeric) {
                // numeric cast with check
                casterParserComputerForge = new NumberCasterComputer(caster);
            } else {
                // non-numeric cast
                casterParserComputerForge = new NonnumericCasterComputer(caster);
            }
        }

        // determine constant or not
        Object theConstant = null;
        boolean isConstant = false;
        if (this.getChildNodes()[0].isConstantResult()) {
            isConstant = casterParserComputerForge.isConstantForConstInput();
            if (isConstant) {
                Object in = this.getChildNodes()[0].getForge().getExprEvaluator().evaluate(null, true, validationContext.getExprEvaluatorContext());
                theConstant = in == null ? null : casterParserComputerForge.getEvaluatorComputer().compute(in, null, true, validationContext.getExprEvaluatorContext());
            }
        }

        forge = new ExprCastNodeForge(this, casterParserComputerForge, targetType, isConstant, theConstant);
        return null;
    }

    public boolean isConstantResult() {
        checkValidated(forge);
        return forge.isConstant();
    }

    public Class getTargetType() {
        checkValidated(forge);
        return forge.getEvaluationType();
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

    public boolean equalsNode(ExprNode node, boolean ignoreStreamPrefix) {
        if (!(node instanceof ExprCastNode)) {
            return false;
        }
        ExprCastNode other = (ExprCastNode) node;
        return other.classIdentifier.equals(this.classIdentifier);
    }

    /**
     * Casting and parsing computer.
     */
    public interface CasterParserComputerForge {
        public boolean isConstantForConstInput();

        public CodegenExpression codegenPremade(Class evaluationType, CodegenExpression input, Class inputType, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope);

        public CasterParserComputer getEvaluatorComputer();
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
    }

    /**
     * Casting and parsing computer.
     */
    public static class StringXFormComputer implements CasterParserComputerForge, CasterParserComputer {
        public Object compute(Object input, EventBean[] eventsPerStream, boolean newData, ExprEvaluatorContext exprEvaluatorContext) {
            return input.toString();
        }

        public boolean isConstantForConstInput() {
            return true;
        }

        public CodegenExpression codegenPremade(Class evaluationType, CodegenExpression input, Class inputType, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
            return exprDotMethod(input, "toString");
        }

        public CasterParserComputer getEvaluatorComputer() {
            return this;
        }
    }

    /**
     * Casting and parsing computer.
     */
    public static class NumberCasterComputer implements CasterParserComputerForge, CasterParserComputer {
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

        public CodegenExpression codegenPremade(Class evaluationType, CodegenExpression input, Class inputType, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
            if (inputType.isPrimitive() || JavaClassHelper.isSubclassOrImplementsInterface(inputType, Number.class)) {
                return numericTypeCaster.codegen(input, inputType, codegenMethodScope, codegenClassScope);
            }
            CodegenMethodNode methodNode = codegenMethodScope.makeChild(evaluationType, NumberCasterComputer.class, codegenClassScope).addParam(inputType, "input");

            methodNode.getBlock()
                    .ifInstanceOf("input", Number.class)
                    .blockReturn(numericTypeCaster.codegen(ref("input"), inputType, methodNode, codegenClassScope))
                    .methodReturn(constantNull());
            return localMethod(methodNode, input);
        }

        public CasterParserComputer getEvaluatorComputer() {
            return this;
        }
    }

    /**
     * Casting and parsing computer.
     */
    public static class StringParserComputer implements CasterParserComputer, CasterParserComputerForge {
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

        public CodegenExpression codegenPremade(Class evaluationType, CodegenExpression input, Class inputType, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
            return parser.codegen(input);
        }

        public CasterParserComputer getEvaluatorComputer() {
            return this;
        }
    }

    /**
     * Casting and parsing computer.
     */
    public static class NonnumericCasterComputer implements CasterParserComputerForge, CasterParserComputer {
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

        public CodegenExpression codegenPremade(Class evaluationType, CodegenExpression input, Class inputType, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
            return caster.codegen(input, inputType, codegenMethodScope, codegenClassScope);
        }

        public CasterParserComputer getEvaluatorComputer() {
            return this;
        }
    }

    public static EPException handleParseException(String formatString, String date, Exception ex) {
        return new EPException("Exception parsing date '" + date + "' format '" + formatString + "': " + ex.getMessage(), ex);
    }

    public static EPException handleParseISOException(String date, ScheduleParameterException ex) {
        return new EPException("Exception parsing iso8601 date '" + date + "': " + ex.getMessage(), ex);
    }

    public static abstract class StringToDateLongWStaticFormat implements CasterParserComputerForge, CasterParserComputer {
        protected final String dateFormatString;
        protected final DateFormat dateFormat;

        protected final ThreadLocal<DateFormat> formats = new ThreadLocal<DateFormat>() {
            protected synchronized DateFormat initialValue() {
                return (DateFormat) dateFormat.clone();
            }
        };

        protected StringToDateLongWStaticFormat(String dateFormatString, DateFormat dateFormat) {
            this.dateFormatString = dateFormatString;
            this.dateFormat = dateFormat;
        }

        public CasterParserComputer getEvaluatorComputer() {
            return this;
        }

        public boolean isConstantForConstInput() {
            return true;
        }

        protected CodegenExpression codegenAddFormat(CodegenClassScope codegenClassScope) {
            return member(codegenClassScope.makeAddMember(DateFormat.class, dateFormat).getMemberId());
        }

        protected CodegenExpression codegenAddFormatString(CodegenClassScope codegenClassScope) {
            return member(codegenClassScope.makeAddMember(String.class, dateFormatString).getMemberId());
        }
    }

    public static abstract class StringToDateLongWDynamicFormatForge implements CasterParserComputerForge {
        protected final ExprForge dateFormatForge;

        protected StringToDateLongWDynamicFormatForge(ExprForge dateFormatForge) {
            this.dateFormatForge = dateFormatForge;
        }

        public boolean isConstantForConstInput() {
            return false;
        }
    }

    public static abstract class StringToDateLongWDynamicFormatEval implements CasterParserComputer {
        protected final ExprEvaluator dateFormatEval;

        public StringToDateLongWDynamicFormatEval(ExprEvaluator dateFormatEval) {
            this.dateFormatEval = dateFormatEval;
        }
    }

    public static class StringToDateWStaticFormatComputer extends StringToDateLongWStaticFormat {
        public StringToDateWStaticFormatComputer(String dateFormatString, DateFormat dateFormat) {
            super(dateFormatString, dateFormat);
        }

        public Object compute(Object input, EventBean[] eventsPerStream, boolean newData, ExprEvaluatorContext exprEvaluatorContext) {
            return stringToDateWStaticFormatParseSafe(dateFormatString, formats.get(), input);
        }

        /**
         * NOTE: Code-generation-invoked method, method name and parameter order matters
         *
         * @param formatString format text
         * @param format       format
         * @param input        input
         * @return date
         */
        public static Date stringToDateWStaticFormatParseSafe(String formatString, DateFormat format, Object input) {
            try {
                return format.parse(input.toString());
            } catch (ParseException e) {
                throw handleParseException(formatString, input.toString(), e);
            }
        }

        public CodegenExpression codegenPremade(Class evaluationType, CodegenExpression input, Class inputType, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
            return staticMethod(StringToDateWStaticFormatComputer.class, "stringToDateWStaticFormatParseSafe", codegenAddFormatString(codegenClassScope), codegenAddFormat(codegenClassScope), input);
        }
    }

    public abstract static class StringToJava8WStaticFormatComputer implements CasterParserComputerForge, CasterParserComputer {
        protected final DateTimeFormatter formatter;

        public StringToJava8WStaticFormatComputer(DateTimeFormatter formatter) {
            this.formatter = formatter;
        }

        public boolean isConstantForConstInput() {
            return true;
        }

        public abstract Object parse(String input);

        public Object compute(Object input, EventBean[] eventsPerStream, boolean newData, ExprEvaluatorContext exprEvaluatorContext) {
            return parse(input.toString());
        }

        protected CodegenExpression codegenFormatter(CodegenClassScope codegenClassScope) {
            return member(codegenClassScope.makeAddMember(DateTimeFormatter.class, formatter).getMemberId());
        }

        public CasterParserComputer getEvaluatorComputer() {
            return this;
        }
    }

    public static class StringToLocalDateTimeWStaticFormatComputer extends StringToJava8WStaticFormatComputer {
        public StringToLocalDateTimeWStaticFormatComputer(DateTimeFormatter formatter) {
            super(formatter);
        }

        /**
         * NOTE: Code-generation-invoked method, method name and parameter order matters
         *
         * @param input     string
         * @param formatter formatter
         * @return ldt
         */
        public static LocalDateTime stringToLocalDateTimeWStaticFormatParse(String input, DateTimeFormatter formatter) {
            try {
                return LocalDateTime.parse(input, formatter);
            } catch (DateTimeParseException e) {
                throw handleParseException(formatter.toString(), input, e);
            }
        }

        public Object parse(String input) {
            return stringToLocalDateTimeWStaticFormatParse(input, formatter);
        }

        public CodegenExpression codegenPremade(Class evaluationType, CodegenExpression input, Class inputType, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
            return staticMethod(StringToLocalDateTimeWStaticFormatComputer.class, "stringToLocalDateTimeWStaticFormatParse", input, codegenFormatter(codegenClassScope));
        }
    }

    public static class StringToLocalDateWStaticFormatComputer extends StringToJava8WStaticFormatComputer {
        public StringToLocalDateWStaticFormatComputer(DateTimeFormatter formatter) {
            super(formatter);
        }

        /**
         * NOTE: Code-generation-invoked method, method name and parameter order matters
         *
         * @param input     string
         * @param formatter formatter
         * @return ld
         */
        public static LocalDate stringToLocalDateWStaticFormatParse(String input, DateTimeFormatter formatter) {
            try {
                return LocalDate.parse(input, formatter);
            } catch (DateTimeParseException e) {
                throw handleParseException(formatter.toString(), input, e);
            }
        }

        public Object parse(String input) {
            return stringToLocalDateWStaticFormatParse(input, formatter);
        }

        public CodegenExpression codegenPremade(Class evaluationType, CodegenExpression input, Class inputType, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
            return staticMethod(StringToLocalDateWStaticFormatComputer.class, "stringToLocalDateWStaticFormatParse", input, codegenFormatter(codegenClassScope));
        }
    }

    public static class StringToLocalTimeWStaticFormatComputer extends StringToJava8WStaticFormatComputer {
        public StringToLocalTimeWStaticFormatComputer(DateTimeFormatter formatter) {
            super(formatter);
        }

        /**
         * NOTE: Code-generation-invoked method, method name and parameter order matters
         *
         * @param input     string
         * @param formatter formatter
         * @return lt
         */
        public static LocalTime stringToLocalTimeWStaticFormatParse(String input, DateTimeFormatter formatter) {
            try {
                return LocalTime.parse(input, formatter);
            } catch (DateTimeParseException e) {
                throw handleParseException(formatter.toString(), input, e);
            }
        }

        public Object parse(String input) {
            return stringToLocalTimeWStaticFormatParse(input, formatter);
        }

        public CodegenExpression codegenPremade(Class evaluationType, CodegenExpression input, Class inputType, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
            return staticMethod(StringToLocalTimeWStaticFormatComputer.class, "stringToLocalTimeWStaticFormatParse", input, codegenFormatter(codegenClassScope));
        }
    }

    public static class StringToZonedDateTimeWStaticFormatComputer extends StringToJava8WStaticFormatComputer {
        public StringToZonedDateTimeWStaticFormatComputer(DateTimeFormatter formatter) {
            super(formatter);
        }

        /**
         * NOTE: Code-generation-invoked method, method name and parameter order matters
         *
         * @param input     string
         * @param formatter formatter
         * @return lt
         */
        public static ZonedDateTime stringZonedDateTimeWStaticFormatParse(String input, DateTimeFormatter formatter) {
            try {
                return ZonedDateTime.parse(input, formatter);
            } catch (DateTimeParseException e) {
                throw handleParseException(formatter.toString(), input, e);
            }
        }

        public Object parse(String input) {
            return stringZonedDateTimeWStaticFormatParse(input, formatter);
        }

        public CodegenExpression codegenPremade(Class evaluationType, CodegenExpression input, Class inputType, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
            return staticMethod(StringToZonedDateTimeWStaticFormatComputer.class, "stringZonedDateTimeWStaticFormatParse", input, codegenFormatter(codegenClassScope));
        }
    }

    public abstract static class StringToJava8WDynamicFormatComputerForge implements CasterParserComputerForge {
        protected final ExprForge dateFormatForge;

        public StringToJava8WDynamicFormatComputerForge(ExprForge dateFormatForge) {
            this.dateFormatForge = dateFormatForge;
        }

        public boolean isConstantForConstInput() {
            return false;
        }
    }

    public abstract static class StringToJava8WDynamicFormatComputerEval implements CasterParserComputer {
        protected final ExprEvaluator dateFormatEval;

        public StringToJava8WDynamicFormatComputerEval(ExprEvaluator dateFormatEval) {
            this.dateFormatEval = dateFormatEval;
        }
    }

    public static class StringToLocalDateTimeWDynamicFormatComputerForge extends StringToJava8WDynamicFormatComputerForge {
        public StringToLocalDateTimeWDynamicFormatComputerForge(ExprForge dateFormatForge) {
            super(dateFormatForge);
        }

        public CasterParserComputer getEvaluatorComputer() {
            return new StringToLocalDateTimeWDynamicFormatComputerEval(dateFormatForge.getExprEvaluator());
        }

        public CodegenExpression codegenPremade(Class evaluationType, CodegenExpression input, Class inputType, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
            return StringToLocalDateTimeWDynamicFormatComputerEval.codegen(input, dateFormatForge, codegenMethodScope, exprSymbol, codegenClassScope);
        }
    }

    public static class StringToLocalDateTimeWDynamicFormatComputerEval extends StringToJava8WDynamicFormatComputerEval {
        public StringToLocalDateTimeWDynamicFormatComputerEval(ExprEvaluator dateFormatEval) {
            super(dateFormatEval);
        }

        public Object compute(Object input, EventBean[] eventsPerStream, boolean newData, ExprEvaluatorContext exprEvaluatorContext) {
            Object format = dateFormatEval.evaluate(eventsPerStream, newData, exprEvaluatorContext);
            DateTimeFormatter formatter = stringToDateTimeFormatterSafe(format);
            return StringToLocalDateTimeWStaticFormatComputer.stringToLocalDateTimeWStaticFormatParse(input.toString(), formatter);
        }

        public static CodegenExpression codegen(CodegenExpression input, ExprForge dateFormatForge, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
            CodegenMethodNode method = codegenMethodScope.makeChild(LocalDateTime.class, StringToLocalDateTimeWDynamicFormatComputerEval.class, codegenClassScope).addParam(String.class, "input");
            method.getBlock()
                    .declareVar(DateTimeFormatter.class, "formatter", staticMethod(ExprCastNode.class, "stringToDateTimeFormatterSafe", dateFormatForge.evaluateCodegen(Object.class, method, exprSymbol, codegenClassScope)))
                    .methodReturn(staticMethod(StringToLocalDateTimeWStaticFormatComputer.class, "stringToLocalDateTimeWStaticFormatParse", ref("input"), ref("formatter")));
            return localMethod(method, input);
        }
    }

    public static class StringToLocalDateWDynamicFormatComputerForge extends StringToJava8WDynamicFormatComputerForge {
        public StringToLocalDateWDynamicFormatComputerForge(ExprForge dateFormatForge) {
            super(dateFormatForge);
        }

        public CasterParserComputer getEvaluatorComputer() {
            return new StringToLocalDateWDynamicFormatComputerEval(dateFormatForge.getExprEvaluator());
        }

        public CodegenExpression codegenPremade(Class evaluationType, CodegenExpression input, Class inputType, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
            return StringToLocalDateWDynamicFormatComputerEval.codegen(input, dateFormatForge, codegenMethodScope, exprSymbol, codegenClassScope);
        }
    }

    public static class StringToLocalDateWDynamicFormatComputerEval extends StringToJava8WDynamicFormatComputerEval {
        public StringToLocalDateWDynamicFormatComputerEval(ExprEvaluator dateFormatEval) {
            super(dateFormatEval);
        }

        public LocalDate compute(Object input, EventBean[] eventsPerStream, boolean newData, ExprEvaluatorContext exprEvaluatorContext) {
            Object format = dateFormatEval.evaluate(eventsPerStream, newData, exprEvaluatorContext);
            DateTimeFormatter formatter = stringToDateTimeFormatterSafe(format);
            return StringToLocalDateWStaticFormatComputer.stringToLocalDateWStaticFormatParse(input.toString(), formatter);
        }

        public static CodegenExpression codegen(CodegenExpression input, ExprForge dateFormatForge, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
            CodegenMethodNode method = codegenMethodScope.makeChild(LocalDate.class, StringToLocalDateWDynamicFormatComputerEval.class, codegenClassScope).addParam(String.class, "input");
            method.getBlock()
                    .declareVar(DateTimeFormatter.class, "formatter", staticMethod(ExprCastNode.class, "stringToDateTimeFormatterSafe", dateFormatForge.evaluateCodegen(Object.class, method, exprSymbol, codegenClassScope)))
                    .methodReturn(staticMethod(StringToLocalDateWStaticFormatComputer.class, "stringToLocalDateWStaticFormatParse", ref("input"), ref("formatter")));
            return localMethod(method, input);
        }
    }

    public static class StringToLocalTimeWDynamicFormatComputerForge extends StringToJava8WDynamicFormatComputerForge {
        public StringToLocalTimeWDynamicFormatComputerForge(ExprForge dateFormatForge) {
            super(dateFormatForge);
        }

        public CasterParserComputer getEvaluatorComputer() {
            return new StringToLocalTimeWDynamicFormatComputerEval(dateFormatForge.getExprEvaluator());
        }

        public CodegenExpression codegenPremade(Class evaluationType, CodegenExpression input, Class inputType, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
            return StringToLocalTimeWDynamicFormatComputerEval.codegen(input, dateFormatForge, codegenMethodScope, exprSymbol, codegenClassScope);
        }
    }

    public static class StringToLocalTimeWDynamicFormatComputerEval extends StringToJava8WDynamicFormatComputerEval {
        public StringToLocalTimeWDynamicFormatComputerEval(ExprEvaluator dateFormatEval) {
            super(dateFormatEval);
        }

        public LocalTime compute(Object input, EventBean[] eventsPerStream, boolean newData, ExprEvaluatorContext exprEvaluatorContext) {
            Object format = dateFormatEval.evaluate(eventsPerStream, newData, exprEvaluatorContext);
            DateTimeFormatter formatter = stringToDateTimeFormatterSafe(format);
            return StringToLocalTimeWStaticFormatComputer.stringToLocalTimeWStaticFormatParse(input.toString(), formatter);
        }

        public static CodegenExpression codegen(CodegenExpression input, ExprForge dateFormatForge, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
            CodegenMethodNode methodNode = codegenMethodScope.makeChild(LocalTime.class, StringToLocalTimeWDynamicFormatComputerForge.class, codegenClassScope).addParam(String.class, "input");

            methodNode.getBlock()
                    .declareVar(DateTimeFormatter.class, "formatter", staticMethod(ExprCastNode.class, "stringToDateTimeFormatterSafe", dateFormatForge.evaluateCodegen(Object.class, methodNode, exprSymbol, codegenClassScope)))
                    .methodReturn(staticMethod(StringToLocalTimeWStaticFormatComputer.class, "stringToLocalTimeWStaticFormatParse", ref("input"), ref("formatter")));
            return localMethod(methodNode, input);
        }
    }

    public static class StringToZonedDateTimeWDynamicFormatComputerForge extends StringToJava8WDynamicFormatComputerForge {
        public StringToZonedDateTimeWDynamicFormatComputerForge(ExprForge dateFormatForge) {
            super(dateFormatForge);
        }

        public CasterParserComputer getEvaluatorComputer() {
            return new StringToZonedDateTimeWDynamicFormatComputerEval(dateFormatForge.getExprEvaluator());
        }

        public CodegenExpression codegenPremade(Class evaluationType, CodegenExpression input, Class inputType, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
            return StringToZonedDateTimeWDynamicFormatComputerEval.codegen(input, dateFormatForge, codegenMethodScope, exprSymbol, codegenClassScope);
        }
    }

    public static class StringToZonedDateTimeWDynamicFormatComputerEval extends StringToJava8WDynamicFormatComputerEval {
        public StringToZonedDateTimeWDynamicFormatComputerEval(ExprEvaluator dateFormatEval) {
            super(dateFormatEval);
        }

        public ZonedDateTime compute(Object input, EventBean[] eventsPerStream, boolean newData, ExprEvaluatorContext exprEvaluatorContext) {
            Object format = dateFormatEval.evaluate(eventsPerStream, newData, exprEvaluatorContext);
            DateTimeFormatter formatter = stringToDateTimeFormatterSafe(format);
            return StringToZonedDateTimeWStaticFormatComputer.stringZonedDateTimeWStaticFormatParse(input.toString(), formatter);
        }

        public static CodegenExpression codegen(CodegenExpression input, ExprForge dateFormatForge, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
            CodegenMethodNode methodNode = codegenMethodScope.makeChild(ZonedDateTime.class, StringToZonedDateTimeWDynamicFormatComputerEval.class, codegenClassScope).addParam(String.class, "input");
            methodNode.getBlock()
                    .declareVar(DateTimeFormatter.class, "formatter", staticMethod(ExprCastNode.class, "stringToDateTimeFormatterSafe", dateFormatForge.evaluateCodegen(Object.class, methodNode, exprSymbol, codegenClassScope)))
                    .methodReturn(staticMethod(StringToZonedDateTimeWStaticFormatComputer.class, "stringZonedDateTimeWStaticFormatParse", ref("input"), ref("formatter")));
            return localMethod(methodNode, input);
        }
    }

    public static class StringToDateWStaticISOFormatComputer implements CasterParserComputerForge, CasterParserComputer {

        /**
         * NOTE: Code-generation-invoked method, method name and parameter order matters
         *
         * @param input input
         * @return date
         */
        public static Date stringToDateWStaticISOParse(String input) {
            try {
                return TimerScheduleISO8601Parser.parseDate(input).getTime();
            } catch (ScheduleParameterException e) {
                throw handleParseISOException(input, e);
            }
        }

        public Object compute(Object input, EventBean[] eventsPerStream, boolean newData, ExprEvaluatorContext exprEvaluatorContext) {
            return stringToDateWStaticISOParse(input.toString());
        }

        public boolean isConstantForConstInput() {
            return true;
        }

        public CodegenExpression codegenPremade(Class evaluationType, CodegenExpression input, Class inputType, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
            return staticMethod(StringToDateWStaticISOFormatComputer.class, "stringToDateWStaticISOParse", input);
        }

        public CasterParserComputer getEvaluatorComputer() {
            return this;
        }
    }

    public static class StringToLongWStaticFormatComputer extends StringToDateLongWStaticFormat {
        public StringToLongWStaticFormatComputer(String dateFormatString, DateFormat dateFormat) {
            super(dateFormatString, dateFormat);
        }

        public Object compute(Object input, EventBean[] eventsPerStream, boolean newData, ExprEvaluatorContext exprEvaluatorContext) {
            return stringToLongWStaticFormatParseSafe(dateFormatString, formats.get(), input);
        }

        /**
         * NOTE: Code-generation-invoked method, method name and parameter order matters
         *
         * @param dateFormatString format text
         * @param format           format
         * @param input            input
         * @return msec
         */
        public static long stringToLongWStaticFormatParseSafe(String dateFormatString, DateFormat format, Object input) {
            try {
                return format.parse(input.toString()).getTime();
            } catch (ParseException e) {
                throw handleParseException(dateFormatString, input.toString(), e);
            }
        }

        public CodegenExpression codegenPremade(Class evaluationType, CodegenExpression input, Class inputType, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
            return staticMethod(StringToLongWStaticFormatComputer.class, "stringToLongWStaticFormatParseSafe", codegenAddFormatString(codegenClassScope), codegenAddFormat(codegenClassScope), input);
        }
    }

    public static class StringToLongWStaticISOFormatComputer implements CasterParserComputerForge, CasterParserComputer {

        /**
         * NOTE: Code-generation-invoked method, method name and parameter order matters
         *
         * @param input input
         * @return msec
         */
        public static long stringToLongWStaticISOParse(String input) {
            try {
                return TimerScheduleISO8601Parser.parseDate(input).getTimeInMillis();
            } catch (ScheduleParameterException ex) {
                throw handleParseISOException(input, ex);
            }
        }

        public Object compute(Object input, EventBean[] eventsPerStream, boolean newData, ExprEvaluatorContext exprEvaluatorContext) {
            return stringToLongWStaticISOParse(input.toString());
        }

        public boolean isConstantForConstInput() {
            return true;
        }

        public CodegenExpression codegenPremade(Class evaluationType, CodegenExpression input, Class inputType, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
            return staticMethod(StringToLongWStaticISOFormatComputer.class, "stringToLongWStaticISOParse", input);
        }

        public CasterParserComputer getEvaluatorComputer() {
            return this;
        }
    }

    public static class StringToCalendarWStaticFormatComputer extends StringToDateLongWStaticFormat {

        private final TimeZone timeZone;

        public StringToCalendarWStaticFormatComputer(String dateFormatString, DateFormat dateFormat, TimeZone timeZone) {
            super(dateFormatString, dateFormat);
            this.timeZone = timeZone;
        }

        public Object compute(Object input, EventBean[] eventsPerStream, boolean newData, ExprEvaluatorContext exprEvaluatorContext) {
            return stringToCalendarWStaticFormatParse(dateFormatString, formats.get(), input, timeZone);
        }

        /**
         * NOTE: Code-generation-invoked method, method name and parameter order matters
         *
         * @param formatString format string
         * @param format       format
         * @param input        input
         * @param timeZone     time zone
         * @return cal
         */
        public static Calendar stringToCalendarWStaticFormatParse(String formatString, DateFormat format, Object input, TimeZone timeZone) {
            try {
                Calendar cal = Calendar.getInstance(timeZone);
                Date date = format.parse(input.toString());
                cal.setTime(date);
                return cal;
            } catch (ParseException ex) {
                throw handleParseException(formatString, input.toString(), ex);
            }
        }

        public CodegenExpression codegenPremade(Class evaluationType, CodegenExpression input, Class inputType, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
            CodegenMember tz = codegenClassScope.makeAddMember(TimeZone.class, timeZone);
            return staticMethod(StringToCalendarWStaticFormatComputer.class, "stringToCalendarWStaticFormatParse", codegenAddFormatString(codegenClassScope), codegenAddFormat(codegenClassScope), input, member(tz.getMemberId()));
        }
    }

    public static class StringToCalendarWStaticISOFormatComputer implements CasterParserComputerForge, CasterParserComputer {

        /**
         * NOTE: Code-generation-invoked method, method name and parameter order matters
         *
         * @param input input
         * @return cal
         */
        public static Calendar stringToCalendarWStaticISOParse(String input) {
            try {
                return TimerScheduleISO8601Parser.parseDate(input);
            } catch (ScheduleParameterException ex) {
                throw handleParseISOException(input, ex);
            }
        }

        public Object compute(Object input, EventBean[] eventsPerStream, boolean newData, ExprEvaluatorContext exprEvaluatorContext) {
            return stringToCalendarWStaticISOParse(input.toString());
        }

        public boolean isConstantForConstInput() {
            return true;
        }

        public CodegenExpression codegenPremade(Class evaluationType, CodegenExpression input, Class inputType, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
            return staticMethod(StringToCalendarWStaticISOFormatComputer.class, "stringToCalendarWStaticISOParse", input);
        }

        public CasterParserComputer getEvaluatorComputer() {
            return this;
        }
    }

    public static class StringToDateWDynamicFormatComputerForge extends StringToDateLongWDynamicFormatForge {
        public StringToDateWDynamicFormatComputerForge(ExprForge dateFormatForge) {
            super(dateFormatForge);
        }

        public CasterParserComputer getEvaluatorComputer() {
            return new StringToDateWDynamicFormatComputerEval(dateFormatForge.getExprEvaluator());
        }

        public CodegenExpression codegenPremade(Class evaluationType, CodegenExpression input, Class inputType, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
            return StringToDateWDynamicFormatComputerEval.codegen(input, dateFormatForge, codegenMethodScope, exprSymbol, codegenClassScope);
        }
    }

    public static class StringToDateWDynamicFormatComputerEval extends StringToDateLongWDynamicFormatEval {
        public StringToDateWDynamicFormatComputerEval(ExprEvaluator dateFormatEval) {
            super(dateFormatEval);
        }

        public Date compute(Object input, EventBean[] eventsPerStream, boolean newData, ExprEvaluatorContext exprEvaluatorContext) {
            Object format = dateFormatEval.evaluate(eventsPerStream, newData, exprEvaluatorContext);
            SimpleDateFormat dateFormat = stringToSimpleDateFormatSafe(format);
            return StringToDateWStaticFormatComputer.stringToDateWStaticFormatParseSafe(format.toString(), dateFormat, input);
        }

        public static CodegenExpression codegen(CodegenExpression input, ExprForge formatExpr, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
            CodegenMethodNode method = codegenMethodScope.makeChild(Date.class, StringToDateWDynamicFormatComputerEval.class, codegenClassScope).addParam(Object.class, "input");
            method.getBlock()
                    .declareVar(Object.class, "format", formatExpr.evaluateCodegen(Object.class, method, exprSymbol, codegenClassScope))
                    .declareVar(SimpleDateFormat.class, "dateFormat", staticMethod(ExprCastNode.class, "stringToSimpleDateFormatSafe", ref("format")))
                    .methodReturn(staticMethod(StringToDateWStaticFormatComputer.class, "stringToDateWStaticFormatParseSafe", exprDotMethod(ref("format"), "toString"), ref("dateFormat"), ref("input")));
            return localMethod(method, input);
        }
    }

    public static class StringToLongWDynamicFormatComputerForge extends StringToDateLongWDynamicFormatForge {

        public StringToLongWDynamicFormatComputerForge(ExprForge dateFormatForge) {
            super(dateFormatForge);
        }

        public CasterParserComputer getEvaluatorComputer() {
            return new StringToLongWDynamicFormatComputerEval(dateFormatForge.getExprEvaluator());
        }

        public CodegenExpression codegenPremade(Class evaluationType, CodegenExpression input, Class inputType, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
            return StringToLongWDynamicFormatComputerEval.codegen(input, dateFormatForge, codegenMethodScope, exprSymbol, codegenClassScope);
        }
    }

    public static class StringToLongWDynamicFormatComputerEval extends StringToDateLongWDynamicFormatEval {

        public StringToLongWDynamicFormatComputerEval(ExprEvaluator dateFormatEval) {
            super(dateFormatEval);
        }

        public Long compute(Object input, EventBean[] eventsPerStream, boolean newData, ExprEvaluatorContext exprEvaluatorContext) {
            Object format = dateFormatEval.evaluate(eventsPerStream, newData, exprEvaluatorContext);
            SimpleDateFormat dateFormat = stringToSimpleDateFormatSafe(format);
            return StringToLongWStaticFormatComputer.stringToLongWStaticFormatParseSafe(format.toString(), dateFormat, input);
        }

        public static CodegenExpression codegen(CodegenExpression input, ExprForge formatForge, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
            CodegenMethodNode method = codegenMethodScope.makeChild(Long.class, StringToLongWDynamicFormatComputerEval.class, codegenClassScope).addParam(Object.class, "input");
            method.getBlock()
                    .declareVar(Object.class, "format", formatForge.evaluateCodegen(Object.class, method, exprSymbol, codegenClassScope))
                    .declareVar(SimpleDateFormat.class, "dateFormat", staticMethod(ExprCastNode.class, "stringToSimpleDateFormatSafe", ref("format")))
                    .methodReturn(staticMethod(StringToLongWStaticFormatComputer.class, "stringToLongWStaticFormatParseSafe", exprDotMethod(ref("format"), "toString"), ref("dateFormat"), ref("input")));
            return localMethod(method, input);
        }
    }

    public static class StringToCalendarWDynamicFormatComputer extends StringToDateLongWDynamicFormatForge {

        private final TimeZone timeZone;

        public StringToCalendarWDynamicFormatComputer(ExprForge dateFormatForge, TimeZone timeZone) {
            super(dateFormatForge);
            this.timeZone = timeZone;
        }

        public CasterParserComputer getEvaluatorComputer() {
            return new StringToCalendarWDynamicFormatComputerEval(dateFormatForge.getExprEvaluator(), timeZone);
        }

        public CodegenExpression codegenPremade(Class evaluationType, CodegenExpression input, Class inputType, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
            return StringToCalendarWDynamicFormatComputerEval.codegen(input, dateFormatForge, codegenMethodScope, exprSymbol, codegenClassScope, timeZone);
        }
    }

    public static class StringToCalendarWDynamicFormatComputerEval extends StringToDateLongWDynamicFormatEval {

        private final TimeZone timeZone;

        public StringToCalendarWDynamicFormatComputerEval(ExprEvaluator dateFormatEval, TimeZone timeZone) {
            super(dateFormatEval);
            this.timeZone = timeZone;
        }

        public Calendar compute(Object input, EventBean[] eventsPerStream, boolean newData, ExprEvaluatorContext exprEvaluatorContext) {
            Object format = dateFormatEval.evaluate(eventsPerStream, newData, exprEvaluatorContext);
            SimpleDateFormat dateFormat = stringToSimpleDateFormatSafe(format);
            return StringToCalendarWStaticFormatComputer.stringToCalendarWStaticFormatParse(format.toString(), dateFormat, input, timeZone);
        }

        public static CodegenExpression codegen(CodegenExpression input, ExprForge dateFormatForge, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope, TimeZone timeZone) {
            CodegenMember timezone = codegenClassScope.makeAddMember(TimeZone.class, timeZone);
            CodegenMethodNode method = codegenMethodScope.makeChild(Calendar.class, StringToCalendarWDynamicFormatComputerEval.class, codegenClassScope).addParam(Object.class, "input");
            method.getBlock()
                    .declareVar(Object.class, "format", dateFormatForge.evaluateCodegen(Object.class, method, exprSymbol, codegenClassScope))
                    .declareVar(SimpleDateFormat.class, "dateFormat", staticMethod(ExprCastNode.class, "stringToSimpleDateFormatSafe", ref("format")))
                    .methodReturn(staticMethod(StringToCalendarWStaticFormatComputer.class, "stringToCalendarWStaticFormatParse", exprDotMethod(ref("format"), "toString"), ref("dateFormat"), ref("input"), member(timezone.getMemberId())));
            return localMethod(method, input);
        }
    }

    private ExprCastNodeDateDesc validateDateFormat(ExprNamedParameterNode dateFormatParameter, ExprValidationContext validationContext, boolean java8Formatter) throws ExprValidationException {
        String staticFormatString = null;
        DateFormat dateFormat = null;
        ExprForge dynamicDateFormat = null;
        boolean iso8601Format = false;
        DateTimeFormatter dateTimeFormatter = null;

        ExprNode formatExpr = dateFormatParameter.getChildNodes()[0];
        ExprForge formatForge = formatExpr.getForge();
        Class formatReturnType = formatExpr.getForge().getEvaluationType();

        if (formatReturnType == String.class) {
            if (!formatExpr.isConstantResult()) {
                dynamicDateFormat = formatForge;
            } else {
                staticFormatString = (String) formatForge.getExprEvaluator().evaluate(null, true, validationContext.getExprEvaluatorContext());
                if (staticFormatString.toLowerCase(Locale.ENGLISH).trim().equals("iso")) {
                    iso8601Format = true;
                } else {
                    if (!java8Formatter) {
                        try {
                            dateFormat = new SimpleDateFormat(staticFormatString);
                        } catch (RuntimeException ex) {
                            throw new ExprValidationException("Invalid date format '" + staticFormatString + "' (as obtained from new SimpleDateFormat): " + ex.getMessage(), ex);
                        }
                    } else {
                        try {
                            dateTimeFormatter = DateTimeFormatter.ofPattern(staticFormatString);
                        } catch (RuntimeException ex) {
                            throw new ExprValidationException("Invalid date format '" + staticFormatString + "' (as obtained from DateTimeFormatter.ofPattern): " + ex.getMessage(), ex);
                        }
                    }
                }
            }
        } else {
            Object dateFormatObject = ExprNodeUtilityCore.evaluateValidationTimeNoStreams(formatForge.getExprEvaluator(), validationContext.getExprEvaluatorContext(), "date format");
            if (!java8Formatter) {
                if (!(dateFormatObject instanceof DateFormat)) {
                    throw getFailedExpected(DateFormat.class, dateFormatObject);
                }
                dateFormat = (DateFormat) dateFormatObject;
            } else {
                if (!(dateFormatObject instanceof DateTimeFormatter)) {
                    throw getFailedExpected(DateTimeFormatter.class, dateFormatObject);
                }
                dateTimeFormatter = (DateTimeFormatter) dateFormatObject;
            }
        }

        return new ExprCastNodeDateDesc(iso8601Format, dynamicDateFormat, staticFormatString, dateFormat, dateTimeFormatter);
    }

    /**
     * NOTE: Code-generation-invoked method, method name and parameter order matters
     *
     * @param format format
     * @return date format
     */
    public static SimpleDateFormat stringToSimpleDateFormatSafe(Object format) {
        if (format == null) {
            throw new EPException("Null date format returned by 'dateformat' expression");
        }
        try {
            return new SimpleDateFormat(format.toString());
        } catch (RuntimeException ex) {
            throw new EPException("Invalid date format '" + format.toString() + "': " + ex.getMessage(), ex);
        }
    }

    public static DateTimeFormatter stringToDateTimeFormatterSafe(Object format) {
        if (format == null) {
            throw new EPException("Null date format returned by 'dateformat' expression");
        }
        try {
            return DateTimeFormatter.ofPattern(format.toString());
        } catch (RuntimeException ex) {
            throw new EPException("Invalid date format '" + format.toString() + "': " + ex.getMessage(), ex);
        }
    }

    private ExprValidationException getFailedExpected(Class expected, Object received) {
        return new ExprValidationException("Invalid format, expected string-format or " + expected.getSimpleName() + " but received " + JavaClassHelper.getClassNameFullyQualPretty(received.getClass()));
    }
}
