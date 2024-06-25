package nars.io;

import java.util.*;

import nars.control.Parameters;
import nars.entity.*;
import nars.inference.*;
import nars.language.*;
import static nars.language.MakeTerm.*;

import nars.storage.Memory;

/**
 * Parse input String into Task or Term. Abstract class with static methods
 * only.
 */
public abstract class StringParser extends Symbols {

    /**
     * All kinds of invalid input lines
     */
    private static class InvalidInputException extends Exception {

        /**
         * An invalid input line.
         *
         * @param s type of error
         */
        InvalidInputException(String s) {
            super(s);
        }
    }

    /**
     * Parse a line of input experience
     * <p>
     * called from ExperienceIO.loadLine
     *
     * @param buffer The line to be parsed
     * @param memory Reference to the memory
     * @param time   The current time
     * @return An experienced task
     */
    public static Task parseExperience(StringBuffer buffer, Memory memory, long stampCurrentSerial, long time) {
        int i = buffer.indexOf(PREFIX_MARK + "");
        if (i > 0) {
            String prefix = buffer.substring(0, i).trim();
            switch (prefix) {
                case INPUT_LINE:
                    buffer.delete(0, i + 1);
                    break;
                default:
                    // 默认⇒返回（只有「IN」会被输入）
                    return null;
            }
        }
        char c = buffer.charAt(buffer.length() - 1);
        if (c == STAMP_CLOSER) {
            int j = buffer.lastIndexOf(STAMP_OPENER + "");
            buffer.delete(j - 1, buffer.length());
        }
        try {
            return parseTask(buffer.toString().trim(), memory, stampCurrentSerial, time);
        } catch (Exception e) {
            final String message = "ERR: !!! INVALID INPUT: parseExperience: " + buffer + " --- " + e.getMessage();
            System.out.println(message);
            // showWarning(message);
            return null;
        }
    }

    /**
     * Enter a new Task in String into the memory, called from InputWindow or
     * locally.
     *
     * @param s      the single-line input String
     * @param memory Reference to the memory
     * @param time   The current time
     * @return An experienced task
     */
    private static Task parseTask(String s, Memory memory, long stampCurrentSerial, long time) {
        final StringBuffer buffer = new StringBuffer(s);
        try {
            final String budgetString = getBudgetString(buffer);
            final String truthString = getTruthString(buffer);
            final String str = buffer.toString().trim();
            final int last = str.length() - 1;
            final char punctuation = str.charAt(last);
            final Stamp stamp = new Stamp(stampCurrentSerial, time);
            final TruthValue truth = parseTruth(truthString, punctuation);
            final Term content = parseTerm(str.substring(0, last));
            if (content == null)
                throw new InvalidInputException("missing valid term");
            final boolean revisable = !(content instanceof Conjunction && Variable.containVarD(content));
            final Sentence sentence = SentenceV1.newSentenceFromPunctuation(
                    content, punctuation, truth, stamp,
                    revisable);
            final BudgetValue budget = parseBudget(budgetString, punctuation, truth);
            return new Task(sentence, budget);
        } catch (final InvalidInputException e) {
            final String message = "ERR: !!! INVALID INPUT: parseTask: " + buffer + " --- " + e.getMessage();
            System.out.println(message);
            // showWarning(message);
            return null;
        }
    }

    /* ---------- parse values ---------- */
    /**
     * Return the prefix of a task string that contains a BudgetValue
     *
     * @param s the input in a StringBuffer
     * @return a String containing a BudgetValue
     * @throws nars.io.StringParser.InvalidInputException if the input cannot be
     *                                                    parsed into a BudgetValue
     */
    private static String getBudgetString(StringBuffer s) throws InvalidInputException {
        if (s.charAt(0) != BUDGET_VALUE_MARK) {
            return null;
        }
        int i = s.indexOf(BUDGET_VALUE_MARK + "", 1); // looking for the end
        if (i < 0) {
            throw new InvalidInputException("missing budget closer");
        }
        String budgetString = s.substring(1, i).trim();
        if (budgetString.length() == 0) {
            throw new InvalidInputException("empty budget");
        }
        s.delete(0, i + 1);
        return budgetString;
    }

    /**
     * Return the postfix of a task string that contains a TruthValue
     *
     * @return a String containing a TruthValue
     * @param s the input in a StringBuffer
     * @throws nars.io.StringParser.InvalidInputException if the input cannot be
     *                                                    parsed into a TruthValue
     */
    private static String getTruthString(StringBuffer s) throws InvalidInputException {
        int last = s.length() - 1;
        if (s.charAt(last) != TRUTH_VALUE_MARK) { // use default
            return null;
        }
        int first = s.indexOf(TRUTH_VALUE_MARK + ""); // looking for the beginning
        if (first == last) { // no matching closer
            throw new InvalidInputException("missing truth mark");
        }
        String truthString = s.substring(first + 1, last).trim();
        if (truthString.length() == 0) { // empty usage
            throw new InvalidInputException("empty truth");
        }
        s.delete(first, last + 1); // remaining input to be processed outside
        s.trimToSize();
        return truthString;
    }

    /**
     * parse the input String into a TruthValue (or DesireValue)
     *
     * @param s    input String
     * @param type Task type
     * @return the input TruthValue
     */
    private static TruthValue parseTruth(String s, char type) {
        if (type == QUESTION_MARK) {
            return null;
        }
        float frequency = 1.0f;
        float confidence = Parameters.DEFAULT_JUDGMENT_CONFIDENCE;
        if (s != null) {
            int i = s.indexOf(VALUE_SEPARATOR);
            if (i < 0) {
                frequency = Float.parseFloat(s);
            } else {
                frequency = Float.parseFloat(s.substring(0, i));
                confidence = Float.parseFloat(s.substring(i + 1));
            }
        }
        return new TruthValue(frequency, confidence);
    }

    /**
     * parse the input String into a BudgetValue
     * * 📝【2024-05-13 11:33:08】传参示例
     * * `s`: "1.0"、"1.0;0.9;0.9"
     *
     * @param truth       the TruthValue of the task
     * @param s           input String
     * @param punctuation Task punctuation
     * @return the input BudgetValue
     * @throws nars.io.StringParser.InvalidInputException If the String cannot
     *                                                    be parsed into a
     *                                                    BudgetValue
     */
    private static BudgetValue parseBudget(String s, char punctuation, TruthValue truth) throws InvalidInputException {
        float priority, durability, quality;
        switch (punctuation) {
            case JUDGMENT_MARK:
                priority = Parameters.DEFAULT_JUDGMENT_PRIORITY;
                durability = Parameters.DEFAULT_JUDGMENT_DURABILITY;
                quality = BudgetFunctions.truthToQuality(truth);
                break;
            case QUESTION_MARK:
                priority = Parameters.DEFAULT_QUESTION_PRIORITY;
                durability = Parameters.DEFAULT_QUESTION_DURABILITY;
                quality = 1; // * 📝「问题」没有真值
                break;
            default:
                throw new InvalidInputException("unknown punctuation: '" + punctuation + "'");
        }
        // * 🚩覆盖性默认值（从字符串）
        if (s != null) { // override default
            // * 🚩【2024-05-13 11:32:19】使用`String.split`多次拆分
            String[] floatStrings = s.split(String.valueOf(VALUE_SEPARATOR));
            switch (floatStrings.length) {
                case 3: // full budget-value
                    quality = Float.parseFloat(floatStrings[2]);
                case 2: // default quality
                    durability = Float.parseFloat(floatStrings[1]);
                case 1: // default durability
                    priority = Float.parseFloat(floatStrings[0]);
            }
        }
        return new BudgetValue(priority, durability, quality);
    }

    /* ---------- parse String into term ---------- */
    /**
     * Top-level method that parse a Term in general, which may recursively call
     * itself.
     * <p>
     * There are 5 valid cases: 1. (Op, A1, ..., An) is a CompoundTerm if Op is
     * a built-in operator 2. {A1, ..., An} is an SetExt; 3. [A1, ..., An] is an
     * SetInt; 4. <T1 Re T2> is a Statement (including higher-order Statement);
     * 5. otherwise it is a simple term.
     *
     * @param s0     the String to be parsed
     * @param memory Reference to the memory
     * @return the Term generated from the String
     */
    private static Term parseTerm(String s0) {
        final String s = s0.trim();
        try {
            if (s.length() == 0) {
                throw new InvalidInputException("missing content");
            }
            final int index = s.length() - 1;
            final char first = s.charAt(0);
            final char last = s.charAt(index);
            switch (first) {
                case COMPOUND_TERM_OPENER:
                    if (last == COMPOUND_TERM_CLOSER) {
                        return parseCompoundTerm(s.substring(1, index));
                    } else {
                        throw new InvalidInputException("missing CompoundTerm closer");
                    }
                case SET_EXT_OPENER:
                    if (last == SET_EXT_CLOSER) {
                        return makeSetExt(parseArguments(s.substring(1, index) + ARGUMENT_SEPARATOR));
                    } else {
                        throw new InvalidInputException("missing ExtensionSet closer");
                    }
                case SET_INT_OPENER:
                    if (last == SET_INT_CLOSER) {
                        return makeSetInt(parseArguments(s.substring(1, index) + ARGUMENT_SEPARATOR));
                    } else {
                        throw new InvalidInputException("missing IntensionSet closer");
                    }
                case STATEMENT_OPENER:
                    if (last == STATEMENT_CLOSER) {
                        return parseStatement(s.substring(1, index));
                    } else {
                        throw new InvalidInputException("missing Statement closer");
                    }
                default:
                    return parseAtomicTerm(s);
            }
        } catch (InvalidInputException e) {
            String message = "ERR: !!! INVALID INPUT: parseTerm: " + s + " --- " + e.getMessage();
            System.out.println(message);
            // showWarning(message);
        }
        return null;
    }

    // private static void showWarning(String message) {
    // new TemporaryFrame( message + "\n( the faulty line has been kept in the input
    // window )",
    // 40000, TemporaryFrame.WARNING );
    // }
    /**
     * Parse a Term that has no internal structure.
     * <p>
     * The Term can be a constant or a variable.
     *
     * @param s0 the String to be parsed
     * @throws nars.io.StringParser.InvalidInputException the String cannot be
     *                                                    parsed into a Term
     * @return the Term generated from the String
     */
    private static Term parseAtomicTerm(String s0) throws InvalidInputException {
        String s = s0.trim();
        if (s.length() == 0) {
            throw new InvalidInputException("missing term");
        }
        if (s.contains(" ")) { // invalid characters in a name
            throw new InvalidInputException("invalid term");
        }
        final Variable variableTerm = tryParseVariable(s);
        if (variableTerm != null) {
            return variableTerm;
        } else {
            return makeWord(s);
        }
    }

    /** 🆕解析变量词项 */
    private static Variable tryParseVariable(String fullName) throws InvalidInputException {
        final char type = fullName.charAt(0);
        final String name = fullName.substring(1);
        switch (type) {
            // * ✅【2024-06-15 12:59:18】基本验证成功：通过散列码的方式，基本不会发生「变量重名」的情况
            case Symbols.VAR_INDEPENDENT:
                return makeVarI(name.hashCode());
            case Symbols.VAR_DEPENDENT:
                return makeVarD(name.hashCode());
            case Symbols.VAR_QUERY:
                return makeVarQ(name.hashCode());
            default:
                // throw new InvalidInputException("invalid variable");
                return null;
        }
    }

    /**
     * Parse a String to create a Statement.
     *
     * @return the Statement generated from the String
     * @param s0 The input String to be parsed
     * @throws nars.io.StringParser.InvalidInputException the String cannot be
     *                                                    parsed into a Term
     */
    private static Statement parseStatement(String s0) throws InvalidInputException {
        String s = s0.trim();
        int i = topRelation(s);
        if (i < 0)
            throw new InvalidInputException("invalid statement relation");
        String relation = s.substring(i, i + 3);
        Term subject = parseTerm(s.substring(0, i));
        if (subject == null) // * 🚩拒绝以null构造词项
            throw new InvalidInputException("invalid statement subject");
        Term predicate = parseTerm(s.substring(i + 3));
        if (predicate == null) // * 🚩拒绝以null构造词项
            throw new InvalidInputException("invalid statement predicate");
        Statement t = makeStatementFromParse(relation, subject, predicate);
        if (t == null)
            throw new InvalidInputException("invalid statement");
        return t;
    }

    /**
     * Check Statement relation symbol, called in StringParser
     *
     * @param s0 The String to be checked
     * @return if the given String is a relation symbol
     */
    public static boolean isRelation(String s0) {
        final String s = s0.trim();
        if (s.length() != 3) {
            return false;
        }
        return (s.equals(Symbols.INHERITANCE_RELATION)
                || s.equals(Symbols.SIMILARITY_RELATION)
                || s.equals(Symbols.INSTANCE_RELATION)
                || s.equals(Symbols.PROPERTY_RELATION)
                || s.equals(Symbols.INSTANCE_PROPERTY_RELATION)
                || s.equals(Symbols.IMPLICATION_RELATION)
                || s.equals(Symbols.EQUIVALENCE_RELATION));
    }

    /**
     * Parse a String to create a CompoundTerm.
     *
     * @return the Term generated from the String
     * @param s0 The String to be parsed
     * @throws nars.io.StringParser.InvalidInputException the String cannot be
     *                                                    parsed into a Term
     */
    private static Term parseCompoundTerm(String s0) throws InvalidInputException {
        String s = s0.trim();
        int firstSeparator = s.indexOf(ARGUMENT_SEPARATOR);
        String op = s.substring(0, firstSeparator).trim();
        if (!isCompoundOperator(op)) {
            throw new InvalidInputException("unknown operator: " + op);
        }
        ArrayList<Term> arg = parseArguments(s.substring(firstSeparator + 1) + ARGUMENT_SEPARATOR);
        Term t = makeCompoundTerm(op, arg);
        if (t == null) {
            throw new InvalidInputException("invalid compound term");
        }
        return t;
    }

    /**
     * Check CompoundTerm operator symbol
     *
     * @return if the given String is an operator symbol
     * @param s The String to be checked
     */
    private static boolean isCompoundOperator(String s) {
        if (s.length() == 1) {
            return (s.equals(Symbols.INTERSECTION_EXT_OPERATOR)
                    || s.equals(Symbols.INTERSECTION_INT_OPERATOR)
                    || s.equals(Symbols.DIFFERENCE_EXT_OPERATOR)
                    || s.equals(Symbols.DIFFERENCE_INT_OPERATOR)
                    || s.equals(Symbols.PRODUCT_OPERATOR)
                    || s.equals(Symbols.IMAGE_EXT_OPERATOR)
                    || s.equals(Symbols.IMAGE_INT_OPERATOR));
        }
        if (s.length() == 2) {
            return (s.equals(Symbols.NEGATION_OPERATOR)
                    || s.equals(Symbols.DISJUNCTION_OPERATOR)
                    || s.equals(Symbols.CONJUNCTION_OPERATOR));
        }
        return false;
    }

    /**
     * Parse a String into the argument get of a CompoundTerm.
     *
     * @return the arguments in an ArrayList
     * @param s0 The String to be parsed
     * @throws nars.io.StringParser.InvalidInputException the String cannot be
     *                                                    parsed into an argument
     *                                                    get
     */
    private static ArrayList<Term> parseArguments(String s0) throws InvalidInputException {
        String s = s0.trim();
        ArrayList<Term> list = new ArrayList<>();
        int start = 0;
        int end = 0;
        Term t;
        while (end < s.length() - 1) {
            end = nextSeparator(s, start);
            t = parseTerm(s.substring(start, end)); // recursive call
            list.add(t);
            start = end + 1;
        }
        if (list.isEmpty()) {
            throw new InvalidInputException("null argument");
        }
        return list;
    }

    /* ---------- locate top-level substring ---------- */
    /**
     * Locate the first top-level separator in a CompoundTerm
     *
     * @return the index of the next separator in a String
     * @param s     The String to be parsed
     * @param first The starting index
     */
    private static int nextSeparator(String s, int first) {
        int levelCounter = 0;
        int i = first;
        while (i < s.length() - 1) {
            if (isOpener(s, i)) {
                levelCounter++;
            } else if (isCloser(s, i)) {
                levelCounter--;
            } else if (s.charAt(i) == ARGUMENT_SEPARATOR) {
                if (levelCounter == 0) {
                    break;
                }
            }
            i++;
        }
        return i;
    }

    /**
     * locate the top-level relation in a statement
     *
     * @return the index of the top-level relation
     * @param s The String to be parsed
     */
    private static int topRelation(String s) { // need efficiency improvement
        int levelCounter = 0;
        int i = 0;
        while (i < s.length() - 3) { // don't need to check the last 3 characters
            if ((levelCounter == 0) && (isRelation(s.substring(i, i + 3)))) {
                return i;
            }
            if (isOpener(s, i)) {
                levelCounter++;
            } else if (isCloser(s, i)) {
                levelCounter--;
            }
            i++;
        }
        return -1;
    }

    /* ---------- recognize symbols ---------- */
    /**
     * Check CompoundTerm opener symbol
     *
     * @return if the given String is an opener symbol
     * @param s The String to be checked
     * @param i The starting index
     */
    private static boolean isOpener(String s, int i) {
        char c = s.charAt(i);
        boolean b = (c == COMPOUND_TERM_OPENER)
                || (c == SET_EXT_OPENER)
                || (c == SET_INT_OPENER)
                || (c == STATEMENT_OPENER);
        if (!b) {
            return false;
        }
        if (i + 3 <= s.length() && isRelation(s.substring(i, i + 3))) {
            return false;
        }
        return true;
    }

    /**
     * Check CompoundTerm closer symbol
     *
     * @return if the given String is a closer symbol
     * @param s The String to be checked
     * @param i The starting index
     */
    private static boolean isCloser(String s, int i) {
        char c = s.charAt(i);
        boolean b = (c == COMPOUND_TERM_CLOSER)
                || (c == SET_EXT_CLOSER)
                || (c == SET_INT_CLOSER)
                || (c == STATEMENT_CLOSER);
        if (!b) {
            return false;
        }
        if (i >= 2 && isRelation(s.substring(i - 2, i + 1))) {
            return false;
        }
        return true;
    }
}
