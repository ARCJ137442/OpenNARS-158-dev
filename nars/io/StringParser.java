/*
 * StringParser.java
 *
 * Copyright (C) 2008  Pei Wang
 *
 * This file is part of Open-NARS.
 *
 * Open-NARS is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * Open-NARS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Open-NARS.  If not, see <http://www.gnu.org/licenses/>.
 */
package nars.io;

import java.util.ArrayList;

import nars.entity.*;
import nars.inference.*;
import nars.language.*;
import nars.main.*;

/**
 * Parse input String into Task.
 */
public abstract class StringParser extends Symbols {

    /**
     * All kinds of invalid input lines
     */
    private static class InvalidInputException extends Exception {

        /**
         * An invalid input line.
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
     * @param buffer The line to be parsed
     */
    public static void parseExperience(StringBuffer buffer) {
        int i = buffer.indexOf(PREFIX_MARK + "");
        if (i > 0) {
            String prefix = buffer.substring(0, i).trim();
            if (prefix.equals(OUTPUT_LINE)) {
                return;
            } else if (prefix.equals(INPUT_LINE)) {
                buffer.delete(0, i + 1);
            }
        }
        char c = buffer.charAt(buffer.length() - 1);
        if (c == STAMP_CLOSER) {
            int j = buffer.lastIndexOf(STAMP_OPENER + "");
            buffer.delete(j - 1, buffer.length());
        }
        parseTask(buffer.toString().trim());
    }

    /**
     * Enter a new Task in String into the memory, called from InputWindow or locally.
     * @param s the single-line input String
     */
    public static void parseTask(String s) {
        StringBuffer buffer = new StringBuffer(s);
        try {
            String budgetString = getBudgetString(buffer);
            String truthString = getTruthString(buffer);
            String tense = parseTense(buffer);
            String str = buffer.toString().trim();
            int last = str.length() - 1;
            char punc = str.charAt(last);
            Stamp stamp;
            if (tense.length() > 0) {
                stamp = new Stamp(tense);
            } else {
                stamp = new Stamp();
            }
            TruthValue truth = parseTruth(truthString, punc);
            Term content = parseTerm(str.substring(0, last));
            Sentence sentence = Sentence.make(content, punc, truth, stamp, null, null);
            if (sentence == null) {
                throw new InvalidInputException("invalid sentence");
            }
            sentence.setInput();
            BudgetValue budget = parseBudget(budgetString, punc, truth);
            Task task = new Task(sentence, budget);
            if (task != null) {
                Memory.inputTask(task);
            }
        } catch (InvalidInputException e) {
            System.out.println(" !!! INVALID INPUT: " + buffer + " --- " + e.getMessage());
        }
    }

    /* ---------- parse values ---------- */
    /**
     * Return the prefex of a task string that contains a BudgetValue
     * @param s the input in a StringBuffer
     * @return a String containing a BudgetValue
     * @throws nars.io.StringParser.InvalidInputException if the input cannot be parsed into a BudgetValue
     */
    private static String getBudgetString(StringBuffer s) throws InvalidInputException {
        if (s.charAt(0) != BUDGET_VALUE_MARK) {
            return null;
        }
        int i = s.indexOf(BUDGET_VALUE_MARK + "", 1);    // looking for the end
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
     * Recognize the tense of an input sentence
     * @param s the input in a StringBuffer
     * @return a tense value
     */
    private static String parseTense(StringBuffer s) {
        int i = s.indexOf(Symbols.TENSE_MARK);
        String t = "";
        if (i > 0) {
            t = s.substring(i).trim();
            s.delete(i, s.length());
        }
        return t;
    }

    /**
     * Return the postfix of a task string that contains a TruthValue
     * @return a String containing a TruthValue
     * @param s the input in a StringBuffer
     * @throws nars.io.StringParser.InvalidInputException if the input cannot be parsed into a TruthValue
     */
    private static String getTruthString(StringBuffer s) throws InvalidInputException {
        int last = s.length() - 1;
        if (s.charAt(last) != TRUTH_VALUE_MARK) {       // use default
            return null;
        }
        int first = s.indexOf(TRUTH_VALUE_MARK + "");    // looking for the beginning
        if (first == last) { // no matching closer
            throw new InvalidInputException("missing truth mark");
        }
        String truthString = s.substring(first + 1, last).trim();
        if (truthString.length() == 0) {                // empty usage
            throw new InvalidInputException("empty truth");
        }
        s.delete(first, last + 1);                 // remaining input to be processed outside
        s.trimToSize();
        return truthString;
    }

    /**
     * parse the input String into a TruthValue (or DesireValue)
     * @param s input String
     * @param type Task type
     * @return the input TruthValue
     */
    private static TruthValue parseTruth(String s, char type) {
        if ((type == QUESTION_MARK) || (type == QUEST_MARK)) {
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
     * 
     * @param truth the TruthValue of the task
     * @param s input String
     * @param punctuation Task punctuation
     * @return the input BudgetValue
     * @throws nars.io.StringParser.InvalidInputException If the String cannot be parsed into a BudgetValue
     */
    private static BudgetValue parseBudget(String s, char punctuation, TruthValue truth) throws InvalidInputException {
        float priority, durability;
        switch (punctuation) {
            case JUDGMENT_MARK:
                priority = Parameters.DEFAULT_JUDGMENT_PRIORITY;
                durability = Parameters.DEFAULT_JUDGMENT_DURABILITY;
                break;
            case GOAL_MARK:
                priority = Parameters.DEFAULT_GOAL_PRIORITY;
                durability = Parameters.DEFAULT_GOAL_DURABILITY;
                break;
            case QUESTION_MARK:
            case QUEST_MARK:
                priority = Parameters.DEFAULT_QUESTION_PRIORITY;
                durability = Parameters.DEFAULT_QUESTION_DURABILITY;
                break;
            default:
                throw new InvalidInputException("unknown punctuation: '" + punctuation + "'");
        }
        if (s != null) { // overrite default
            int i = s.indexOf(VALUE_SEPARATOR);
            if (i < 0) {        // default durability
                priority = Float.parseFloat(s);
            } else {
                priority = Float.parseFloat(s.substring(0, i));
                durability = Float.parseFloat(s.substring(i + 1));
            }
        }
        float quality = (truth == null) ? 1 : BudgetFunctions.truthToQuality(truth);
        return new BudgetValue(priority, durability, quality);
    }

    /* ---------- parse String into term ---------- */
    /**
     * Top-level method that parse a Term in general, which may recursively call itself.
     * <p>
     * There are 5 valid cases:
     * 1. (Op, A1, ..., An) is a CompoundTerm if Op is a built-in operator
     * 2. {A1, ..., An} is an SetExt;
     * 3. [A1, ..., An] is an SetInt;
     * 4. <T1 Re T2> is a Statement (including higher-order Statement);
     * 5. otherwise it is a simple term.
     * @param s0 the String to be parsed
     * @return the Term generated from the String
     */
    public static Term parseTerm(String s0) {
        String s = s0.trim();
        try {
            if (s.length() == 0) {
                throw new InvalidInputException("missing content");
            }
            Term t = Memory.nameToListedTerm(s);    // existing constant or operator
            if (t != null) {
                return t;
            }                           // existing Term
            int index = s.length() - 1;
            char first = s.charAt(0);
            char last = s.charAt(index);
            switch (first) {
                case COMPOUND_TERM_OPENER:
                    if (last == COMPOUND_TERM_CLOSER) {
                        return parseCompoundTerm(s.substring(1, index));
                    } else {
                        throw new InvalidInputException("missing CompoundTerm closer");
                    }
                case SET_EXT_OPENER:
                    if (last == SET_EXT_CLOSER) {
                        return SetExt.make(parseArguments(s.substring(1, index) + ARGUMENT_SEPARATOR));
                    } else {
                        throw new InvalidInputException("missing ExtensionSet closer");
                    }
                case SET_INT_OPENER:
                    if (last == SET_INT_CLOSER) {
                        return SetInt.make(parseArguments(s.substring(1, index) + ARGUMENT_SEPARATOR));
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
                    return parseSimpleTerm(s);
            }
        } catch (InvalidInputException e) {
            System.out.println(" !!! INVALID INPUT: " + s + " --- " + e.getMessage());
        }
        return null;
    }

    /**
     * Parse a Term that has no internal structure.
     * <p>
     * The Term can be a constant or a variable.
     * @param s0 the String to be parsed
     * @throws nars.io.StringParser.InvalidInputException the String cannot be parsed into a Term
     * @return the Term generated from the String
     */
    private static Term parseSimpleTerm(String s0) throws InvalidInputException {
        String s = s0.trim();
        if (s.length() == 0) {
            throw new InvalidInputException("missing term");
        }
        if (s.contains(" ")) // invalid characters in a name
        {
            throw new InvalidInputException("invalid term");
        }
        Term term;
        char prefix = s.charAt(0);
        if ((prefix == VARIABLE_TAG) || (prefix == QUERY_VARIABLE_TAG)) {
            term = new Variable(s);         // the only place to directly call this constructor
        } else {
            term = new Term(s);             // the only place to directly call this constructor
        }             
        return term;
    }

    /**
     * Parse a String to create a Statement.
     * @return the Statement generated from the String
     * @param s0 The input String to be parsed
     * @throws nars.io.StringParser.InvalidInputException the String cannot be parsed into a Term
     */
    private static Statement parseStatement(String s0) throws InvalidInputException {
        String s = s0.trim();
        int i = topRelation(s);
        if (i < 0) {
            throw new InvalidInputException("invalid statement");
        }
        String relation = s.substring(i, i + 3);
        Term subject = parseTerm(s.substring(0, i));
        Term predicate = parseTerm(s.substring(i + 3));
        Statement t = Statement.make(relation, subject, predicate);
        if (t == null) {
            throw new InvalidInputException("invalid statement");
        }
        return t;
    }

    /**
     * Parse a String to create a CompoundTerm.
     * @return the Term generated from the String
     * @param s0 The String to be parsed
     * @throws nars.io.StringParser.InvalidInputException the String cannot be parsed into a Term
     */
    private static Term parseCompoundTerm(String s0) throws InvalidInputException {
        String s = s0.trim();
        int firstSeparator = s.indexOf(ARGUMENT_SEPARATOR);
        String op = s.substring(0, firstSeparator).trim();
        if (!CompoundTerm.isOperator(op)) {
            throw new InvalidInputException("unknown operator: " + op);
        }
        ArrayList<Term> arg = parseArguments(s.substring(firstSeparator + 1) + ARGUMENT_SEPARATOR);
        Term t = CompoundTerm.make(op, arg);
        if (t == null) {
            throw new InvalidInputException("invalid compound term");
        }
        return t;
    }

    /**
     * Parse a String into the argument get of a CompoundTerm.
     * @return the arguments in an ArrayList
     * @param s0 The String to be parsed
     * @throws nars.io.StringParser.InvalidInputException the String cannot be parsed into an argument get
     */
    private static ArrayList<Term> parseArguments(String s0) throws InvalidInputException {
        String s = s0.trim();
        ArrayList<Term> list = new ArrayList<Term>();
        int start = 0;
        int end = 0;
        Term t;
        while (end < s.length() - 1) {
            end = nextSeparator(s, start);
            t = parseTerm(s.substring(start, end));     // recursive call
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
     * @return the index of the next seperator in a String
     * @param s The String to be parsed
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
     * @return the index of the top-level relation
     * @param s The String to be parsed
     */
    private static int topRelation(String s) {      // need efficiency improvement
        int levelCounter = 0;
        int i = 0;
        while (i < s.length() - 3) {    // don't need to check the last 3 characters
            if ((levelCounter == 0) && (Statement.isRelation(s.substring(i, i + 3)))) {
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
     * @return if the given String is an opener symbol
     * @param s The String to be checked
     * @param i The starting index
     */
    private static boolean isOpener(String s, int i) {
        char c = s.charAt(i);
        boolean b = (c == COMPOUND_TERM_OPENER) ||
                (c == SET_EXT_OPENER) ||
                (c == SET_INT_OPENER) ||
                (c == STATEMENT_OPENER);
        if (!b) {
            return false;
        }
        if (i + 3 <= s.length() && Statement.isRelation(s.substring(i, i + 3))) {
            return false;
        }
        return true;
    }

    /**
     * Check CompoundTerm closer symbol
     * @return if the given String is a closer symbol
     * @param s The String to be checked
     * @param i The starting index
     */
    private static boolean isCloser(String s, int i) {
        char c = s.charAt(i);
        boolean b = (c == COMPOUND_TERM_CLOSER) ||
                (c == SET_EXT_CLOSER) ||
                (c == SET_INT_CLOSER) ||
                (c == STATEMENT_CLOSER);
        if (!b) {
            return false;
        }
        if (i >= 2 && Statement.isRelation(s.substring(i - 2, i + 1))) {
            return false;
        }
        return true;
    }
}
