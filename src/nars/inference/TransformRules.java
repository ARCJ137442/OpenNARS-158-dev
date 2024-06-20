package nars.inference;

import static nars.language.MakeTerm.*;

import java.util.ArrayList;

import nars.control.DerivationContextTransform;
import nars.entity.Task;
import nars.entity.TaskLink;
import nars.language.CompoundTerm;
import nars.language.Conjunction;
import nars.language.Equivalence;
import nars.language.ImageExt;
import nars.language.ImageInt;
import nars.language.Statement;
import nars.language.Implication;
import nars.language.Inheritance;
import nars.language.Product;
import nars.language.Term;

/**
 * 用于存储「介于『直接推理』与『概念推理』之间的『转换推理』」
 * * 📝其中只有概念和任务链，没有「当前信念」与词项链
 * * 📌理论上基于NAL-4，但代码上因「没用到变量 / 变量没有值」而需单独设置一体系
 * * 📍实质上是「概念+任务链」单任务推理
 */
public class TransformRules {

    /* ----- inference with one TaskLink only ----- */
    /**
     * The TaskLink is of type TRANSFORM,
     * and the conclusion is an equivalent transformation
     * * 📝【2024-05-20 11:46:32】在「直接推理」之后、「概念推理」之前使用
     * * 📌推理引擎「转换推理」的唯一入口
     *
     * @param tLink   The task link
     * @param context Reference to the derivation context
     */
    static void transformTask(DerivationContextTransform context) {
        // TODO: 过程笔记注释
        // * 🚩预处理 | 📌【2024-06-07 23:12:34】断定其中的「tLink」就是「当前任务链」
        final TaskLink tLink = context.getCurrentTaskLink();
        final CompoundTerm clonedContent = (CompoundTerm) context.getCurrentTask().getContent().clone();
        final short[] indices = tLink.getIndices();
        final Term inh;
        if ((indices.length == 2) || (clonedContent instanceof Inheritance)) { // <(*, term, #) --> #>
            inh = clonedContent;
        } else if (indices.length == 3) { // <<(*, term, #) --> #> ==> #>
            inh = clonedContent.componentAt(indices[0]);
        } else if (indices.length == 4) { // <(&&, <(*, term, #) --> #>, #) ==> #>
            Term component = clonedContent.componentAt(indices[0]);
            if ((component instanceof Conjunction)
                    && (((clonedContent instanceof Implication) && (indices[0] == 0))
                            || (clonedContent instanceof Equivalence))) {
                inh = ((CompoundTerm) component).componentAt(indices[1]);
            } else {
                return;
            }
        } else {
            inh = null;
        }
        if (inh instanceof Inheritance) {
            transformProductImage((Inheritance) inh, clonedContent, indices, context);
        }
    }

    /* -------------------- products and images transform -------------------- */
    /**
     * Equivalent transformation between products and images
     * {<(*, S, M) --> P>, S@(*, S, M)} |- <S --> (/, P, _, M)>
     * {<S --> (/, P, _, M)>, P@(/, P, _, M)} |- <(*, S, M) --> P>
     * {<S --> (/, P, _, M)>, M@(/, P, _, M)} |- <M --> (/, P, S, _)>
     *
     * @param inh        An Inheritance statement
     * @param oldContent The whole content
     * @param indices    The indices of the TaskLink
     * @param task       The task
     * @param context    Reference to the derivation context
     */
    private static void transformProductImage(
            Inheritance inh, CompoundTerm oldContent,
            short[] indices,
            DerivationContextTransform context) {
        // TODO: 过程笔记注释
        final Term inhSubject = inh.getSubject();
        final Term inhPredicate = inh.getPredicate();
        if (inh.equals(oldContent)) {
            if (inhSubject instanceof CompoundTerm) {
                transformSubjectProductImage((CompoundTerm) inhSubject, inhPredicate, context);
            }
            if (inhPredicate instanceof CompoundTerm) {
                transformPredicateProductImage(inhSubject, (CompoundTerm) inhPredicate, context);
            }
            return;
        }
        final short index = indices[indices.length - 1];
        final short side = indices[indices.length - 2];
        final CompoundTerm comp = (CompoundTerm) inh.componentAt(side);
        final Term subject;
        final Term predicate;
        if (comp instanceof Product) {
            if (side == 0) {
                subject = comp.componentAt(index);
                predicate = makeImageExt((Product) comp, inh.getPredicate(), index);
            } else {
                subject = makeImageInt((Product) comp, inh.getSubject(), index);
                predicate = comp.componentAt(index);
            }
        } else if ((comp instanceof ImageExt) && (side == 1)) {
            if (index == ((ImageExt) comp).getRelationIndex()) {
                subject = makeProduct(comp, inh.getSubject(), index);
                predicate = comp.componentAt(index);
            } else {
                subject = comp.componentAt(index);
                predicate = makeImageExt((ImageExt) comp, inh.getSubject(), index);
            }
        } else if ((comp instanceof ImageInt) && (side == 0)) {
            if (index == ((ImageInt) comp).getRelationIndex()) {
                subject = comp.componentAt(index);
                predicate = makeProduct(comp, inh.getPredicate(), index);
            } else {
                subject = makeImageInt((ImageInt) comp, inh.getPredicate(), index);
                predicate = comp.componentAt(index);
            }
        } else {
            return;
        }
        final Inheritance newInh = makeInheritance(subject, predicate);
        Term content = null;
        if (indices.length == 2) {
            content = newInh;
        } else if ((oldContent instanceof Statement) && (indices[0] == 1)) {
            content = makeStatement((Statement) oldContent, oldContent.componentAt(0), newInh);
        } else {
            ArrayList<Term> componentList;
            Term condition = oldContent.componentAt(0);
            if (((oldContent instanceof Implication) || (oldContent instanceof Equivalence))
                    && (condition instanceof Conjunction)) {
                componentList = ((CompoundTerm) condition).cloneComponents();
                componentList.set(indices[1], newInh);
                Term newCond = makeCompoundTerm((CompoundTerm) condition, componentList);
                content = makeStatement((Statement) oldContent, newCond, ((Statement) oldContent).getPredicate());
            } else {
                componentList = oldContent.cloneComponents();
                componentList.set(indices[0], newInh);
                if (oldContent instanceof Conjunction) {
                    content = makeCompoundTerm(oldContent, componentList);
                } else if ((oldContent instanceof Implication) || (oldContent instanceof Equivalence)) {
                    content = makeStatement((Statement) oldContent, componentList.get(0), componentList.get(1));
                }
            }
        }
        if (content == null) {
            return;
        }
        final Task task = context.getCurrentTask();
        final Budget budget;
        if (task.isQuestion()) {
            budget = BudgetInference.compoundBackward(content, context);
        } else {
            budget = BudgetInference.compoundForward(task.asJudgement(), content, context);
        }
        context.singlePremiseTask(content, task, budget);
    }

    /**
     * Equivalent transformation between products and images when the subject is a
     * compound
     * {<(*, S, M) --> P>, S@(*, S, M)} |- <S --> (/, P, _, M)>
     * {<S --> (/, P, _, M)>, P@(/, P, _, M)} |- <(*, S, M) --> P>
     * {<S --> (/, P, _, M)>, M@(/, P, _, M)} |- <M --> (/, P, S, _)>
     *
     * @param subject   The subject term
     * @param predicate The predicate term
     * @param context   Reference to the derivation context
     */
    private static void transformSubjectProductImage(CompoundTerm subject, Term predicate,
            DerivationContextTransform context) {
        // TODO: 过程笔记注释
        final Task task = context.getCurrentTask();
        Budget budget;
        Inheritance inheritance;
        Term newSubj, newPred;
        if (subject instanceof Product) {
            Product product = (Product) subject;
            for (short i = 0; i < product.size(); i++) {
                newSubj = product.componentAt(i);
                newPred = makeImageExt(product, predicate, i);
                inheritance = makeInheritance(newSubj, newPred);
                if (inheritance != null) {
                    if (task.isQuestion()) {
                        budget = BudgetInference.compoundBackward(inheritance, context);
                    } else {
                        budget = BudgetInference.compoundForward(task.asJudgement(), inheritance, context);
                    }
                    context.singlePremiseTask(inheritance, task, budget);
                }
            }
        } else if (subject instanceof ImageInt) {
            final ImageInt image = (ImageInt) subject;
            final int relationIndex = image.getRelationIndex();
            for (short i = 0; i < image.size(); i++) {
                if (i == relationIndex) {
                    newSubj = image.componentAt(relationIndex);
                    newPred = makeProduct(image, predicate, relationIndex);
                } else {
                    newSubj = makeImageInt((ImageInt) image, predicate, i);
                    newPred = image.componentAt(i);
                }
                inheritance = makeInheritance(newSubj, newPred);
                if (inheritance != null) {
                    if (task.isQuestion()) {
                        budget = BudgetInference.compoundBackward(inheritance, context);
                    } else {
                        budget = BudgetInference.compoundForward(task.asJudgement(), inheritance, context);
                    }
                    context.singlePremiseTask(inheritance, task, budget);
                }
            }
        }
    }

    /**
     * Equivalent transformation between products and images when the predicate is a
     * compound
     * {<(*, S, M) --> P>, S@(*, S, M)} |- <S --> (/, P, _, M)>
     * {<S --> (/, P, _, M)>, P@(/, P, _, M)} |- <(*, S, M) --> P>
     * {<S --> (/, P, _, M)>, M@(/, P, _, M)} |- <M --> (/, P, S, _)>
     *
     * @param subject   The subject term
     * @param predicate The predicate term
     * @param context   Reference to the derivation context
     */
    private static void transformPredicateProductImage(Term subject, CompoundTerm predicate,
            DerivationContextTransform context) {
        // TODO: 过程笔记注释
        final Task task = context.getCurrentTask();
        Budget budget;
        Inheritance inheritance;
        Term newSubj, newPred;
        if (predicate instanceof Product) {
            final Product product = (Product) predicate;
            for (short i = 0; i < product.size(); i++) {
                newSubj = makeImageInt(product, subject, i);
                newPred = product.componentAt(i);
                inheritance = makeInheritance(newSubj, newPred);
                if (inheritance != null) {
                    if (task.isQuestion()) {
                        budget = BudgetInference.compoundBackward(inheritance, context);
                    } else {
                        budget = BudgetInference.compoundForward(task.asJudgement(), inheritance, context);
                    }
                    context.singlePremiseTask(inheritance, task, budget);
                }
            }
        } else if (predicate instanceof ImageExt) {
            final ImageExt image = (ImageExt) predicate;
            final int relationIndex = image.getRelationIndex();
            for (short i = 0; i < image.size(); i++) {
                if (i == relationIndex) {
                    newSubj = makeProduct(image, subject, relationIndex);
                    newPred = image.componentAt(relationIndex);
                } else {
                    newSubj = image.componentAt(i);
                    newPred = makeImageExt((ImageExt) image, subject, i);
                }
                inheritance = makeInheritance(newSubj, newPred);
                if (inheritance != null) { // jmv <<<<<
                    if (task.isQuestion()) {
                        budget = BudgetInference.compoundBackward(inheritance, context);
                    } else {
                        budget = BudgetInference.compoundForward(task.asJudgement(), inheritance, context);
                    }
                    context.singlePremiseTask(inheritance, task, budget);
                }
            }
        }
    }
}
