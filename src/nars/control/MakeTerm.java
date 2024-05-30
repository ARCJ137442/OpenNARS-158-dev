package nars.control;

import java.util.ArrayList;
import java.util.TreeSet;

import nars.io.Symbols;
import nars.language.CompoundTerm;
import nars.language.Conjunction;
import nars.language.Statement;
import nars.language.DifferenceExt;
import nars.language.DifferenceInt;
import nars.language.Disjunction;
import nars.language.Equivalence;
import nars.language.ImageExt;
import nars.language.ImageInt;
import nars.language.Implication;
import nars.language.Inheritance;
import nars.language.IntersectionExt;
import nars.language.IntersectionInt;
import nars.language.Negation;
import nars.language.Product;
import nars.language.SetExt;
import nars.language.SetInt;
import nars.language.Similarity;
import nars.language.Term;
import nars.storage.Memory;

/**
 * 所有原`nars.language`包的{@link Term}子类中有关`make`的静态方法
 * * 🎯解耦`nars.language`与`nars.storage`
 * * 📝实际上主要有两种功能：
 * * 1. 在创建前简化词项内容：如`(&&, A, (&&, B, C))` => `(&&, A, B, C)`
 * * 2. 根据「名称」在记忆区中寻找已有缓存，记忆区已有缓存⇒直接使用
 * * 📝【2024-05-30 08:51:04】此中对「记忆区」的访问均为只读访问：只需判断「是否已有概念」
 */
public abstract class MakeTerm {

    /**
     * Try to get a term from its name in the memory
     *
     * @param <T>    The type of the term
     * @param name   The name of the term
     * @param memory The memory
     * @return The existing term or null
     */
    @SuppressWarnings("unchecked")
    public static <T extends Term> T getFromNameOrNull(String name, Memory memory) {
        // TODO: 验证：实际上「不要记忆区」也能在功能上与原版一致
        final Term t = memory.nameToListedTerm(name);
        return t == null ? null : (T) t;
    }

    /* CompoundTerm */

    /* static methods making new compounds, which may return null */
    /**
     * Try to make a compound term from a template and a list of components
     *
     * @param compound   The template
     * @param components The components
     * @param memory     Reference to the memory
     * @return A compound term or null
     */
    public static Term makeCompoundTerm(CompoundTerm compound, ArrayList<Term> components, Memory memory) {
        if (compound instanceof ImageExt)
            return makeImageExt(components, ((ImageExt) compound).getRelationIndex(), memory);
        else if (compound instanceof ImageInt)
            return makeImageInt(components, ((ImageInt) compound).getRelationIndex(), memory);
        else
            return makeCompoundTerm(compound.operator(), components, memory);
    }

    /**
     * Try to make a compound term from an operator and a list of components
     * <p>
     * Called from StringParser
     *
     * @param op     Term operator
     * @param arg    Component list
     * @param memory Reference to the memory
     * @return A compound term or null
     */
    public static Term makeCompoundTerm(String op, ArrayList<Term> arg, Memory memory) {
        switch (op.length()) {
            case 1:
                if (op.charAt(0) == Symbols.SET_EXT_OPENER)
                    return makeSetExt(arg, memory);
                if (op.charAt(0) == Symbols.SET_INT_OPENER)
                    return makeSetInt(arg, memory);
                switch (op) {
                    case Symbols.INTERSECTION_EXT_OPERATOR:
                        return makeIntersectionExt(arg, memory);
                    case Symbols.INTERSECTION_INT_OPERATOR:
                        return makeIntersectionInt(arg, memory);
                    case Symbols.DIFFERENCE_EXT_OPERATOR:
                        return makeDifferenceExt(arg, memory);
                    case Symbols.DIFFERENCE_INT_OPERATOR:
                        return makeDifferenceInt(arg, memory);
                    case Symbols.PRODUCT_OPERATOR:
                        return makeProduct(arg, memory);
                    case Symbols.IMAGE_EXT_OPERATOR:
                        return makeImageExt(arg, memory);
                    case Symbols.IMAGE_INT_OPERATOR:
                        return makeImageInt(arg, memory);
                    default:
                        return null;
                }
            case 2:
                switch (op) {
                    case Symbols.NEGATION_OPERATOR:
                        return makeNegation(arg, memory);
                    case Symbols.DISJUNCTION_OPERATOR:
                        return makeDisjunction(arg, memory);
                    case Symbols.CONJUNCTION_OPERATOR:
                        return makeConjunction(arg, memory);
                    default:
                        return null;
                }
            default:
                return null;
        }
    }

    /**
     * Try to add a component into a compound
     *
     * @param t1     The compound
     * @param t2     The component
     * @param memory Reference to the memory
     * @return The new compound
     */
    public static Term addComponents(CompoundTerm t1, Term t2, Memory memory) {
        if (t2 == null)
            return t1;
        final ArrayList<Term> list = t1.cloneComponents();
        final boolean success;
        if (t1.getClass() == t2.getClass())
            success = list.addAll(((CompoundTerm) t2).getComponents());
        else
            success = list.add(t2);
        return (success ? makeCompoundTerm(t1, list, memory) : null);
    }

    /**
     * Try to remove a component from a compound
     *
     * @param t1     The compound
     * @param t2     The component
     * @param memory Reference to the memory
     * @return The new compound
     */
    public static Term reduceComponents(CompoundTerm t1, Term t2, Memory memory) {
        final boolean success;
        final ArrayList<Term> list = t1.cloneComponents();
        if (t1.getClass() == t2.getClass())
            success = list.removeAll(((CompoundTerm) t2).getComponents());
        else
            success = list.remove(t2);
        if (success) {
            if (list.size() > 1) {
                return makeCompoundTerm(t1, list, memory);
            }
            if (list.size() == 1) {
                if ((t1 instanceof Conjunction) || (t1 instanceof Disjunction)
                        || (t1 instanceof IntersectionExt) || (t1 instanceof IntersectionInt)
                        || (t1 instanceof DifferenceExt) || (t1 instanceof DifferenceInt)) {
                    return list.get(0);
                }
            }
        }
        return null;
    }

    /**
     * Try to replace a component in a compound at a given index by another one
     *
     * @param compound The compound
     * @param index    The location of replacement
     * @param t        The new component
     * @param memory   Reference to the memory
     * @return The new compound
     */
    public static Term setComponent(CompoundTerm compound, int index, Term t, Memory memory) {
        final ArrayList<Term> list = compound.cloneComponents();
        list.remove(index);
        if (t != null) {
            if (compound.getClass() != t.getClass()) {
                list.add(index, t);
            } else {
                ArrayList<Term> list2 = ((CompoundTerm) t).cloneComponents();
                for (int i = 0; i < list2.size(); i++) {
                    list.add(index + i, list2.get(i));
                }
            }
        }
        return makeCompoundTerm(compound, list, memory);
    }

    /* Statement */

    /**
     * Make a Statement from String, called by StringParser
     *
     * @param relation  The relation String
     * @param subject   The first component
     * @param predicate The second component
     * @param memory    Reference to the memory
     * @return The Statement built
     */
    public static Statement makeStatement(String relation, Term subject, Term predicate, Memory memory) {
        if (Statement.invalidStatement(subject, predicate))
            return null;
        switch (relation) {
            case Symbols.INHERITANCE_RELATION:
                return makeInheritance(subject, predicate, memory);
            case Symbols.SIMILARITY_RELATION:
                return makeSimilarity(subject, predicate, memory);
            case Symbols.INSTANCE_RELATION:
                return makeInstance(subject, predicate, memory);
            case Symbols.PROPERTY_RELATION:
                return makeProperty(subject, predicate, memory);
            case Symbols.INSTANCE_PROPERTY_RELATION:
                return makeInstanceProperty(subject, predicate, memory);
            case Symbols.IMPLICATION_RELATION:
                return makeImplication(subject, predicate, memory);
            case Symbols.EQUIVALENCE_RELATION:
                return makeEquivalence(subject, predicate, memory);
            default:
                return null;
        }
    }

    /**
     * Make a Statement from given components, called by the rules
     *
     * @return The Statement built
     * @param subj      The first component
     * @param pred      The second component
     * @param statement A sample statement providing the class type
     * @param memory    Reference to the memory
     */
    public static Statement makeStatement(Statement statement, Term subj, Term pred, Memory memory) {
        if (statement instanceof Inheritance)
            return makeInheritance(subj, pred, memory);
        if (statement instanceof Similarity)
            return makeSimilarity(subj, pred, memory);
        if (statement instanceof Implication)
            return makeImplication(subj, pred, memory);
        if (statement instanceof Equivalence)
            return makeEquivalence(subj, pred, memory);
        return null;
    }

    /**
     * Make a symmetric Statement from given components and temporal
     * information, called by the rules
     *
     * @param statement A sample asymmetric statement providing the class type
     * @param subj      The first component
     * @param pred      The second component
     * @param memory    Reference to the memory
     * @return The Statement built
     */
    public static Statement makeStatementSym(Statement statement, Term subj, Term pred, Memory memory) {
        if (statement instanceof Inheritance)
            return makeSimilarity(subj, pred, memory);
        if (statement instanceof Implication)
            return makeEquivalence(subj, pred, memory);
        return null;
    }

    /* Conjunction */

    /**
     * Try to make a new compound from a list of components. Called by
     * StringParser.
     *
     * @return the Term generated from the arguments
     * @param argList the list of arguments
     * @param memory  Reference to the memory
     */
    public static Term makeConjunction(ArrayList<Term> argList, Memory memory) {
        final TreeSet<Term> set = new TreeSet<>(argList); // sort/merge arguments
        return makeConjunction(set, memory);
    }

    /**
     * Try to make a new Disjunction from a set of components. Called by the
     * public make methods.
     *
     * @param set    a set of Term as components
     * @param memory Reference to the memory
     * @return the Term generated from the arguments
     */
    private static Term makeConjunction(TreeSet<Term> set, Memory memory) {
        if (set.isEmpty()) {
            return null;
        } // special case: single component
        if (set.size() == 1) {
            return set.first();
        } // special case: single component
        final ArrayList<Term> argument = new ArrayList<>(set);
        final String name = CompoundTerm.makeCompoundName(Symbols.CONJUNCTION_OPERATOR, argument);
        final Term t = getFromNameOrNull(name, memory);
        return (t != null) ? t : new Conjunction(argument);
    }

    // overload this method by term type?
    /**
     * Try to make a new compound from two components. Called by the inference
     * rules.
     *
     * @param term1  The first component
     * @param term2  The second component
     * @param memory Reference to the memory
     * @return A compound generated or a term it reduced to
     */
    public static Term makeConjunction(Term term1, Term term2, Memory memory) {
        final TreeSet<Term> set;
        if (term1 instanceof Conjunction) {
            set = new TreeSet<>(((CompoundTerm) term1).cloneComponents());
            if (term2 instanceof Conjunction) {
                set.addAll(((CompoundTerm) term2).cloneComponents());
            } // (&,(&,P,Q),(&,R,S)) = (&,P,Q,R,S)
            else {
                set.add(term2.clone());
            } // (&,(&,P,Q),R) = (&,P,Q,R)
        } else if (term2 instanceof Conjunction) {
            set = new TreeSet<>(((CompoundTerm) term2).cloneComponents());
            set.add(term1.clone()); // (&,R,(&,P,Q)) = (&,P,Q,R)
        } else {
            set = new TreeSet<>();
            set.add(term1.clone());
            set.add(term2.clone());
        }
        return makeConjunction(set, memory);
    }

    /* DifferenceExt */

    /**
     * Try to make a new DifferenceExt. Called by StringParser.
     *
     * @return the Term generated from the arguments
     * @param argList The list of components
     * @param memory  Reference to the memory
     */
    public static Term makeDifferenceExt(ArrayList<Term> argList, Memory memory) {
        if (argList.size() == 1) // special case from CompoundTerm.reduceComponent
            return argList.get(0);
        if (argList.size() != 2)
            return null;
        if ((argList.get(0) instanceof SetExt) && (argList.get(1) instanceof SetExt)) {
            final TreeSet<Term> set = new TreeSet<Term>(((CompoundTerm) argList.get(0)).cloneComponents());
            set.removeAll(((CompoundTerm) argList.get(1)).cloneComponents()); // set difference
            return makeSetExt(set, memory);
        }
        final String name = CompoundTerm.makeCompoundName(Symbols.DIFFERENCE_EXT_OPERATOR, argList);
        final Term t = getFromNameOrNull(name, memory);
        return (t != null) ? t : new DifferenceExt(argList);
    }

    /**
     * Try to make a new compound from two components. Called by the inference
     * rules.
     *
     * @param t1     The first component
     * @param t2     The second component
     * @param memory Reference to the memory
     * @return A compound generated or a term it reduced to
     */
    public static Term makeDifferenceExt(Term t1, Term t2, Memory memory) {
        if (t1.equals(t2))
            return null;
        final ArrayList<Term> list = DifferenceExt.argumentsToList(t1, t2);
        return makeConjunction(list, memory);
    }

    /* DifferenceInt */

    /**
     * Try to make a new DifferenceExt. Called by StringParser.
     *
     * @return the Term generated from the arguments
     * @param argList The list of components
     * @param memory  Reference to the memory
     */
    public static Term makeDifferenceInt(ArrayList<Term> argList, Memory memory) {
        if (argList.size() == 1) // special case from CompoundTerm.reduceComponent
            return argList.get(0);
        if (argList.size() != 2)
            return null;
        if ((argList.get(0) instanceof SetInt) && (argList.get(1) instanceof SetInt)) {
            final TreeSet<Term> set = new TreeSet<Term>(((CompoundTerm) argList.get(0)).cloneComponents());
            set.removeAll(((CompoundTerm) argList.get(1)).cloneComponents()); // set difference
            return makeSetInt(set, memory);
        }
        final String name = CompoundTerm.makeCompoundName(Symbols.DIFFERENCE_INT_OPERATOR, argList);
        final Term t = getFromNameOrNull(name, memory);
        return (t != null) ? t : new DifferenceInt(argList);
    }

    /**
     * Try to make a new compound from two components. Called by the inference
     * rules.
     *
     * @param t1     The first component
     * @param t2     The second component
     * @param memory Reference to the memory
     * @return A compound generated or a term it reduced to
     */
    public static Term makeDifferenceInt(Term t1, Term t2, Memory memory) {
        if (t1.equals(t2))
            return null;
        final ArrayList<Term> list = DifferenceInt.argumentsToList(t1, t2);
        return makeDifferenceInt(list, memory);
    }

    /* Disjunction */

    /**
     * Try to make a new Disjunction from two components. Called by the inference
     * rules.
     *
     * @param term1  The first component
     * @param term2  The first component
     * @param memory Reference to the memory
     * @return A Disjunction generated or a Term it reduced to
     */
    public static Term makeDisjunction(Term term1, Term term2, Memory memory) {
        final TreeSet<Term> set;
        if (term1 instanceof Disjunction) {
            set = new TreeSet<>(((CompoundTerm) term1).cloneComponents());
            if (term2 instanceof Disjunction) {
                set.addAll(((CompoundTerm) term2).cloneComponents());
            } // (&,(&,P,Q),(&,R,S)) = (&,P,Q,R,S)
            else {
                set.add(term2.clone());
            } // (&,(&,P,Q),R) = (&,P,Q,R)
        } else if (term2 instanceof Disjunction) {
            set = new TreeSet<>(((CompoundTerm) term2).cloneComponents());
            set.add(term1.clone()); // (&,R,(&,P,Q)) = (&,P,Q,R)
        } else {
            set = new TreeSet<>();
            set.add(term1.clone());
            set.add(term2.clone());
        }
        return makeDisjunction(set, memory);
    }

    /**
     * Try to make a new IntersectionExt. Called by StringParser.
     *
     * @param argList a list of Term as components
     * @param memory  Reference to the memory
     * @return the Term generated from the arguments
     */
    public static Term makeDisjunction(ArrayList<Term> argList, Memory memory) {
        final TreeSet<Term> set = new TreeSet<>(argList); // sort/merge arguments
        return makeDisjunction(set, memory);
    }

    /**
     * Try to make a new Disjunction from a set of components. Called by the public
     * make methods.
     *
     * @param set    a set of Term as components
     * @param memory Reference to the memory
     * @return the Term generated from the arguments
     */
    public static Term makeDisjunction(TreeSet<Term> set, Memory memory) {
        if (set.size() == 1) {
            return set.first();
        } // special case: single component
        final ArrayList<Term> argument = new ArrayList<>(set);
        final String name = CompoundTerm.makeCompoundName(Symbols.DISJUNCTION_OPERATOR, argument);
        final Term t = getFromNameOrNull(name, memory);
        return (t != null) ? t : new Disjunction(argument);
    }

    /**
     * Try to make a new compound from two components. Called by the inference
     * rules.
     *
     * @param subject   The first component
     * @param predicate The second component
     * @param memory    Reference to the memory
     * @return A compound generated or null
     */
    public static Equivalence makeEquivalence(Term subject, Term predicate, Memory memory) {
        // to be extended to check if subject is Conjunction
        if ((subject instanceof Implication) || (subject instanceof Equivalence)) {
            return null;
        }
        if ((predicate instanceof Implication) || (predicate instanceof Equivalence)) {
            return null;
        }
        if (Equivalence.invalidStatement(subject, predicate)) {
            return null;
        }
        if (subject.compareTo(predicate) > 0) {
            final Term inner = subject;
            subject = predicate;
            predicate = inner;
        }
        final String name = Equivalence.makeStatementName(subject, Symbols.EQUIVALENCE_RELATION, predicate);
        final Term t = getFromNameOrNull(name, memory);
        if (t != null) {
            return (Equivalence) t;
        }
        final ArrayList<Term> argument = CompoundTerm.argumentsToList(subject, predicate);
        return new Equivalence(argument);
    }

    /* ImageExt */

    /**
     * Try to make a new ImageExt. Called by StringParser.
     *
     * @return the Term generated from the arguments
     * @param argList The list of components
     * @param memory  Reference to the memory
     */
    public static Term makeImageExt(ArrayList<Term> argList, Memory memory) {
        if (argList.size() < 2)
            return null;
        final Term relation = argList.get(0);
        final ArrayList<Term> argument = new ArrayList<Term>();
        int index = 0;
        for (int j = 1; j < argList.size(); j++) {
            if (argList.get(j).getName().charAt(0) == Symbols.IMAGE_PLACE_HOLDER) {
                index = j - 1;
                argument.add(relation);
            } else {
                argument.add(argList.get(j));
            }
        }
        return makeImageExt(argument, (short) index, memory);
    }

    /**
     * Try to make an Image from a Product and a relation. Called by the inference
     * rules.
     *
     * @param product  The product
     * @param relation The relation
     * @param index    The index of the place-holder
     * @return A compound generated or a term it reduced to
     */
    public static Term makeImageExt(Product product, Term relation, short index, Memory memory) {
        if (relation instanceof Product) {
            final Product p2 = (Product) relation;
            if ((product.size() == 2) && (p2.size() == 2)) {
                if ((index == 0) && product.componentAt(1).equals(p2.componentAt(1))) {
                    // (/,_,(*,a,b),b) is reduced to a
                    return p2.componentAt(0);
                }
                if ((index == 1) && product.componentAt(0).equals(p2.componentAt(0))) {
                    // (/,(*,a,b),a,_) is reduced to b
                    return p2.componentAt(1);
                }
            }
        }
        final ArrayList<Term> argument = product.cloneComponents();
        argument.set(index, relation);
        return makeImageExt(argument, index, memory);
    }

    /**
     * Try to make an Image from an existing Image and a component. Called by the
     * inference rules.
     *
     * @param oldImage  The existing Image
     * @param component The component to be added into the component list
     * @param index     The index of the place-holder in the new Image
     * @return A compound generated or a term it reduced to
     */
    public static Term makeImageExt(ImageExt oldImage, Term component, short index, Memory memory) {
        final ArrayList<Term> argList = oldImage.cloneComponents();
        final int oldIndex = oldImage.getRelationIndex();
        final Term relation = argList.get(oldIndex);
        argList.set(oldIndex, component);
        argList.set(index, relation);
        return makeImageExt(argList, index, memory);
    }

    /**
     * Try to make a new compound from a set of components. Called by the public
     * make methods.
     *
     * @param argument The argument list
     * @param index    The index of the place-holder in the new Image
     * @return the Term generated from the arguments
     */
    public static Term makeImageExt(ArrayList<Term> argument, short index, Memory memory) {
        final String name = CompoundTerm.makeImageName(Symbols.IMAGE_EXT_OPERATOR, argument, index);
        final Term t = getFromNameOrNull(name, memory);
        return (t != null) ? t : new ImageExt(name, argument, index);
    }

    /* ImageInt */

    /**
     * Try to make a new ImageExt. Called by StringParser.
     *
     * @return the Term generated from the arguments
     * @param argList The list of components
     * @param memory  Reference to the memory
     */
    public static Term makeImageInt(ArrayList<Term> argList, Memory memory) {
        if (argList.size() < 2)
            return null;
        final Term relation = argList.get(0);
        final ArrayList<Term> argument = new ArrayList<Term>();
        int index = 0;
        for (int j = 1; j < argList.size(); j++) {
            if (argList.get(j).getName().charAt(0) == Symbols.IMAGE_PLACE_HOLDER) {
                index = j - 1;
                argument.add(relation);
            } else {
                argument.add(argList.get(j));
            }
        }
        return makeImageInt(argument, (short) index, memory);
    }

    /**
     * Try to make an Image from a Product and a relation. Called by the inference
     * rules.
     *
     * @param product  The product
     * @param relation The relation
     * @param index    The index of the place-holder
     * @param memory   Reference to the memory
     * @return A compound generated or a term it reduced to
     */
    public static Term makeImageInt(Product product, Term relation, short index, Memory memory) {
        if (relation instanceof Product) {
            final Product p2 = (Product) relation;
            if ((product.size() == 2) && (p2.size() == 2)) {
                if ((index == 0) && product.componentAt(1).equals(p2.componentAt(1))) {
                    // (\,_,(*,a,b),b) is reduced to a
                    return p2.componentAt(0);
                }
                if ((index == 1) && product.componentAt(0).equals(p2.componentAt(0))) {
                    // (\,(*,a,b),a,_) is reduced to b
                    return p2.componentAt(1);
                }
            }
        }
        final ArrayList<Term> argument = product.cloneComponents();
        argument.set(index, relation);
        return makeImageInt(argument, index, memory);
    }

    /**
     * Try to make an Image from an existing Image and a component. Called by the
     * inference rules.
     *
     * @param oldImage  The existing Image
     * @param component The component to be added into the component list
     * @param index     The index of the place-holder in the new Image
     * @param memory    Reference to the memory
     * @return A compound generated or a term it reduced to
     */
    public static Term makeImageInt(ImageInt oldImage, Term component, short index, Memory memory) {
        final ArrayList<Term> argList = oldImage.cloneComponents();
        final int oldIndex = oldImage.getRelationIndex();
        final Term relation = argList.get(oldIndex);
        argList.set(oldIndex, component);
        argList.set(index, relation);
        return makeImageInt(argList, index, memory);
    }

    /**
     * Try to make a new compound from a set of components. Called by the public
     * make methods.
     *
     * @param argument The argument list
     * @param index    The index of the place-holder in the new Image
     * @param memory   Reference to the memory
     * @return the Term generated from the arguments
     */
    public static Term makeImageInt(ArrayList<Term> argument, short index, Memory memory) {
        final String name = CompoundTerm.makeImageName(Symbols.IMAGE_INT_OPERATOR, argument, index);
        final Term t = getFromNameOrNull(name, memory);
        return (t != null) ? t : new ImageInt(name, argument, index);
    }

    /* Implication */

    /**
     * Try to make a new compound from two components. Called by the inference
     * rules.
     *
     * @param subject   The first component
     * @param predicate The second component
     * @param memory    Reference to the memory
     * @return A compound generated or a term it reduced to
     */
    public static Implication makeImplication(Term subject, Term predicate, Memory memory) {
        if (subject == null || predicate == null)
            return null;
        if (subject instanceof Implication || subject instanceof Equivalence
                || predicate instanceof Equivalence)
            return null;
        if (Implication.invalidStatement(subject, predicate))
            return null;
        final String name = Implication.makeStatementName(subject, Symbols.IMPLICATION_RELATION, predicate);
        final Term t = getFromNameOrNull(name, memory);
        if (t != null)
            return (Implication) t;
        if (predicate instanceof Implication) {
            final Term oldCondition = ((Implication) predicate).getSubject();
            if ((oldCondition instanceof Conjunction) && ((Conjunction) oldCondition).containComponent(subject)) {
                return null;
            }
            final Term newCondition = makeConjunction(subject, oldCondition, memory);
            return makeImplication(newCondition, ((Implication) predicate).getPredicate(), memory);
        } else {
            final ArrayList<Term> argument = CompoundTerm.argumentsToList(subject, predicate);
            return new Implication(argument);
        }
    }

    /* Inheritance */

    /**
     * Try to make a new compound from two components. Called by the inference
     * rules.
     *
     * @param subject   The first component
     * @param predicate The second component
     * @param memory    Reference to the memory
     * @return A compound generated or null
     */
    public static Inheritance makeInheritance(Term subject, Term predicate, Memory memory) {
        if (Inheritance.invalidStatement(subject, predicate))
            return null;
        final String name = Inheritance.makeStatementName(subject, Symbols.INHERITANCE_RELATION, predicate);
        final Inheritance t = getFromNameOrNull(name, memory);
        if (t != null)
            return t;
        final ArrayList<Term> argument = CompoundTerm.argumentsToList(subject, predicate);
        return new Inheritance(argument);
    }

    /*
     * Instance
     * A Statement about an Instance relation, which is used only in Narsese for
     * I/O,
     * and translated into Inheritance for internal use.
     */

    /**
     * Try to make a new compound from two components. Called by the inference
     * rules.
     * <p>
     * A {-- B becomes {A} --> B
     *
     * @param subject   The first component
     * @param predicate The second component
     * @param memory    Reference to the memory
     * @return A compound generated or null
     */
    public static Statement makeInstance(Term subject, Term predicate, Memory memory) {
        return makeInheritance(makeSetExt(subject, memory), predicate, memory);
    }

    /*
     * InstanceProperty
     *
     * A Statement about an InstanceProperty relation, which is used only in Narsese
     * for I/O,
     * and translated into Inheritance for internal use.
     */

    /**
     * Try to make a new compound from two components. Called by the inference
     * rules.
     * <p>
     * A {-] B becomes {A} --> [B]
     *
     * @param subject   The first component
     * @param predicate The second component
     * @param memory    Reference to the memory
     * @return A compound generated or null
     */
    public static Statement makeInstanceProperty(Term subject, Term predicate, Memory memory) {
        return makeInheritance(makeSetExt(subject, memory), makeSetInt(predicate, memory), memory);
    }

    /* IntersectionExt */

    /**
     * Try to make a new compound from two components. Called by the inference
     * rules.
     *
     * @param term1  The first component
     * @param term2  The first component
     * @param memory Reference to the memory
     * @return A compound generated or a term it reduced to
     */
    public static Term makeIntersectionExt(Term term1, Term term2, Memory memory) {
        final TreeSet<Term> set;
        if ((term1 instanceof SetInt) && (term2 instanceof SetInt)) {
            set = new TreeSet<Term>(((CompoundTerm) term1).cloneComponents());
            set.addAll(((CompoundTerm) term2).cloneComponents()); // set union
            return makeSetInt(set, memory);
        }
        if ((term1 instanceof SetExt) && (term2 instanceof SetExt)) {
            set = new TreeSet<Term>(((CompoundTerm) term1).cloneComponents());
            set.retainAll(((CompoundTerm) term2).cloneComponents()); // set intersection
            return makeSetExt(set, memory);
        }
        if (term1 instanceof IntersectionExt) {
            set = new TreeSet<Term>(((CompoundTerm) term1).cloneComponents());
            if (term2 instanceof IntersectionExt) {
                set.addAll(((CompoundTerm) term2).cloneComponents());
            } // (&,(&,P,Q),(&,R,S)) = (&,P,Q,R,S)
            else {
                set.add(term2.clone());
            } // (&,(&,P,Q),R) = (&,P,Q,R)
        } else if (term2 instanceof IntersectionExt) {
            set = new TreeSet<Term>(((CompoundTerm) term2).cloneComponents());
            set.add(term1.clone()); // (&,R,(&,P,Q)) = (&,P,Q,R)
        } else {
            set = new TreeSet<Term>();
            set.add(term1.clone());
            set.add(term2.clone());
        }
        return makeIntersectionExt(set, memory);
    }

    /**
     * Try to make a new IntersectionExt. Called by StringParser.
     *
     * @return the Term generated from the arguments
     * @param argList The list of components
     * @param memory  Reference to the memory
     */
    public static Term makeIntersectionExt(ArrayList<Term> argList, Memory memory) {
        final TreeSet<Term> set = new TreeSet<Term>(argList); // sort/merge arguments
        return makeIntersectionExt(set, memory);
    }

    /**
     * Try to make a new compound from a set of components. Called by the public
     * make methods.
     *
     * @param set    a set of Term as components
     * @param memory Reference to the memory
     * @return the Term generated from the arguments
     */
    public static Term makeIntersectionExt(TreeSet<Term> set, Memory memory) {
        if (set.size() == 1) {
            return set.first();
        } // special case: single component
        final ArrayList<Term> argument = new ArrayList<Term>(set);
        final String name = CompoundTerm.makeCompoundName(Symbols.INTERSECTION_EXT_OPERATOR, argument);
        final Term t = getFromNameOrNull(name, memory);
        return (t != null) ? t : new IntersectionExt(argument);
    }

    /* IntersectionInt */
    /**
     * Try to make a new compound from two components. Called by the inference
     * rules.
     *
     * @param term1  The first component
     * @param term2  The first component
     * @param memory Reference to the memory
     * @return A compound generated or a term it reduced to
     */
    public static Term makeIntersectionInt(Term term1, Term term2, Memory memory) {
        final TreeSet<Term> set;
        if ((term1 instanceof SetExt) && (term2 instanceof SetExt)) {
            set = new TreeSet<Term>(((CompoundTerm) term1).cloneComponents());
            set.addAll(((CompoundTerm) term2).cloneComponents()); // set union
            return makeSetExt(set, memory);
        }
        if ((term1 instanceof SetInt) && (term2 instanceof SetInt)) {
            set = new TreeSet<Term>(((CompoundTerm) term1).cloneComponents());
            set.retainAll(((CompoundTerm) term2).cloneComponents()); // set intersection
            return makeSetInt(set, memory);
        }
        if (term1 instanceof IntersectionInt) {
            set = new TreeSet<Term>(((CompoundTerm) term1).cloneComponents());
            if (term2 instanceof IntersectionInt) {
                set.addAll(((CompoundTerm) term2).cloneComponents());
            } // (|,(|,P,Q),(|,R,S)) = (|,P,Q,R,S)
            else {
                set.add(term2.clone());
            } // (|,(|,P,Q),R) = (|,P,Q,R)
        } else if (term2 instanceof IntersectionInt) {
            set = new TreeSet<Term>(((CompoundTerm) term2).cloneComponents());
            set.add(term1.clone()); // (|,R,(|,P,Q)) = (|,P,Q,R)
        } else {
            set = new TreeSet<Term>();
            set.add(term1.clone());
            set.add(term2.clone());
        }
        return makeIntersectionInt(set, memory);
    }

    /**
     * Try to make a new IntersectionExt. Called by StringParser.
     *
     * @return the Term generated from the arguments
     * @param argList The list of components
     * @param memory  Reference to the memory
     */
    public static Term makeIntersectionInt(ArrayList<Term> argList, Memory memory) {
        final TreeSet<Term> set = new TreeSet<Term>(argList); // sort/merge arguments
        return makeIntersectionInt(set, memory);
    }

    /**
     * Try to make a new compound from a set of components. Called by the public
     * make methods.
     *
     * @param set    a set of Term as components
     * @param memory Reference to the memory
     * @return the Term generated from the arguments
     */
    public static Term makeIntersectionInt(TreeSet<Term> set, Memory memory) {
        if (set.size() == 1) {
            return set.first();
        } // special case: single component
        final ArrayList<Term> argument = new ArrayList<Term>(set);
        final String name = CompoundTerm.makeCompoundName(Symbols.INTERSECTION_INT_OPERATOR, argument);
        final Term t = getFromNameOrNull(name, memory);
        return (t != null) ? t : new IntersectionInt(argument);
    }

    /* Negation */

    /**
     * Try to make a Negation of one component. Called by the inference rules.
     *
     * @param t      The component
     * @param memory Reference to the memory
     * @return A compound generated or a term it reduced to
     */
    public static Term makeNegation(Term t, Memory memory) {
        if (t instanceof Negation) {
            return ((CompoundTerm) t).cloneComponents().get(0);
        } // (--,(--,P)) = P
        final ArrayList<Term> argument = new ArrayList<>();
        argument.add(t);
        return makeNegation(argument, memory);
    }

    /**
     * Try to make a new Negation. Called by StringParser.
     *
     * @return the Term generated from the arguments
     * @param argument The list of components
     * @param memory   Reference to the memory
     */
    public static Term makeNegation(ArrayList<Term> argument, Memory memory) {
        if (argument.size() != 1) {
            return null;
        }
        final String name = CompoundTerm.makeCompoundName(Symbols.NEGATION_OPERATOR, argument);
        final Term t = getFromNameOrNull(name, memory);
        return (t != null) ? t : new Negation(argument);
    }

    /* Product */

    /**
     * Try to make a new compound. Called by StringParser.
     *
     * @return the Term generated from the arguments
     * @param argument The list of components
     * @param memory   Reference to the memory
     */
    public static Term makeProduct(ArrayList<Term> argument, Memory memory) {
        final String name = CompoundTerm.makeCompoundName(Symbols.PRODUCT_OPERATOR, argument);
        final Term t = getFromNameOrNull(name, memory);
        return (t != null) ? t : new Product(argument);
    }

    /**
     * Try to make a Product from an ImageExt/ImageInt and a component. Called by
     * the inference rules.
     *
     * @param image     The existing Image
     * @param component The component to be added into the component list
     * @param index     The index of the place-holder in the new Image -- optional
     *                  parameter
     * @param memory    Reference to the memory
     * @return A compound generated or a term it reduced to
     */
    public static Term makeProduct(CompoundTerm image, Term component, int index, Memory memory) {
        final ArrayList<Term> argument = image.cloneComponents();
        argument.set(index, component);
        return makeProduct(argument, memory);
    }

    /*
     * Property
     * A Statement about a Property relation, which is used only in Narsese for I/O,
     * and translated into Inheritance for internal use.
     */

    /**
     * Try to make a new compound from two components. Called by the inference
     * rules.
     * <p>
     * A --] B becomes A --> [B]
     *
     * @param subject   The first component
     * @param predicate The second component
     * @param memory    Reference to the memory
     * @return A compound generated or null
     */
    public static Inheritance makeProperty(Term subject, Term predicate, Memory memory) {
        return makeInheritance(subject, makeSetInt(predicate, memory), memory);
    }

    /* SetExt */

    /**
     * Try to make a new set from one component. Called by the inference rules.
     *
     * @param t      The component
     * @param memory Reference to the memory
     * @return A compound generated or a term it reduced to
     */
    public static Term makeSetExt(Term t, Memory memory) {
        final TreeSet<Term> set = new TreeSet<Term>();
        set.add(t);
        return makeSetExt(set, memory);
    }

    /**
     * Try to make a new SetExt. Called by StringParser.
     *
     * @return the Term generated from the arguments
     * @param argList The list of components
     * @param memory  Reference to the memory
     */
    public static Term makeSetExt(ArrayList<Term> argList, Memory memory) {
        final TreeSet<Term> set = new TreeSet<Term>(argList); // sort/merge arguments
        return makeSetExt(set, memory);
    }

    /**
     * Try to make a new compound from a set of components. Called by the public
     * make methods.
     *
     * @param set    a set of Term as components
     * @param memory Reference to the memory
     * @return the Term generated from the arguments
     */
    public static Term makeSetExt(TreeSet<Term> set, Memory memory) {
        if (set.isEmpty()) {
            return null;
        }
        final ArrayList<Term> argument = new ArrayList<Term>(set);
        final String name = CompoundTerm.makeSetName(Symbols.SET_EXT_OPENER, argument, Symbols.SET_EXT_CLOSER);
        final Term t = getFromNameOrNull(name, memory);
        return (t != null) ? t : new SetExt(argument);
    }

    /* SetInt */

    /**
     * Try to make a new set from one component. Called by the inference rules.
     *
     * @param t      The component
     * @param memory Reference to the memory
     * @return A compound generated or a term it reduced to
     */
    public static Term makeSetInt(Term t, Memory memory) {
        final TreeSet<Term> set = new TreeSet<Term>();
        set.add(t);
        return makeSetInt(set, memory);
    }

    /**
     * Try to make a new SetExt. Called by StringParser.
     *
     * @return the Term generated from the arguments
     * @param argList The list of components
     * @param memory  Reference to the memory
     */
    public static Term makeSetInt(ArrayList<Term> argList, Memory memory) {
        final TreeSet<Term> set = new TreeSet<Term>(argList); // sort/merge arguments
        return makeSetInt(set, memory);
    }

    /**
     * Try to make a new compound from a set of components. Called by the public
     * make methods.
     *
     * @param set    a set of Term as components
     * @param memory Reference to the memory
     * @return the Term generated from the arguments
     */
    public static Term makeSetInt(TreeSet<Term> set, Memory memory) {
        if (set.isEmpty()) {
            return null;
        }
        final ArrayList<Term> argument = new ArrayList<Term>(set);
        final String name = CompoundTerm.makeSetName(Symbols.SET_INT_OPENER, argument, Symbols.SET_INT_CLOSER);
        final Term t = getFromNameOrNull(name, memory);
        return (t != null) ? t : new SetInt(argument);
    }

    /* Similarity */

    /**
     * Try to make a new compound from two components. Called by the inference
     * rules.
     *
     * @param subject   The first component
     * @param predicate The second component
     * @param memory    Reference to the memory
     * @return A compound generated or null
     */
    public static Similarity makeSimilarity(Term subject, Term predicate, Memory memory) {
        if (Similarity.invalidStatement(subject, predicate)) {
            return null;
        }
        if (subject.compareTo(predicate) > 0) {
            return makeSimilarity(predicate, subject, memory);
        }
        final String name = Similarity.makeStatementName(subject, Symbols.SIMILARITY_RELATION, predicate);
        final Term t = getFromNameOrNull(name, memory);
        if (t != null) {
            return (Similarity) t;
        }
        final ArrayList<Term> argument = CompoundTerm.argumentsToList(subject, predicate);
        return new Similarity(argument);
    }
}
