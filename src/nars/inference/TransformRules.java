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

    /** 数组转字符串 */
    static String s2s(short[] s) {
        String str = "[";
        for (int i = 0; i < s.length; i++) {
            str += s[i];
            if (i != s.length - 1) {
                str += ", ";
            }
        }
        return str + "]";
    }

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
        // * 🚩预处理 | 📌【2024-06-07 23:12:34】断定其中的「tLink」就是「当前任务链」
        final TaskLink tLink = context.getCurrentTaskLink();
        final CompoundTerm taskContent = (CompoundTerm) context.getCurrentTask().getContent();
        final short[] indices = tLink.getIndices();

        // * 🚩提取其中的继承词项
        final Term inh;
        // * 🚩本身是乘积 | <(*, term, #) --> #>
        if (indices.length == 2 || (taskContent instanceof Inheritance)) {
            inh = taskContent;
            // * 📄currentConcept = "a",
            // * * content = "<(*,a,b) --> like>",
            // * * indices = [0, 0]
            // * * => inh = "<(*,a,b) --> like>"
            // * 📄currentConcept = "a",
            // * * content = "<like --> (*,a,b)>",
            // * * indices = [1, 0]
            // * * => inh = "<like --> (*,a,b)>"
            // * 📄currentConcept = "a",
            // * * content = "<like <-> (*,a,b)>",
            // * * indices = [1, 0]
            // * * => inh = "<like <-> (*,a,b)>"
            // * 📄currentConcept = "(*,0)",
            // * * content = "<(/,(*,0),_) --> num>",
            // * * indices = [0, 0]
            // * * => inh = "<(/,(*,0),_) --> num>"
            // * 📄currentConcept = "(*,0)",
            // * * content = "<num --> (/,(*,0),_)>",
            // * * indices = [1, 0]
            // * * => inh = "<num --> (/,(*,0),_)>"
            // * 📄currentConcept = "(*,0)",
            // * * content = "<(/,num,_) --> (/,(*,0),_)>",
            // * * indices = [1, 0]
            // * * => inh = "<(/,num,_) --> (/,(*,0),_)>"
            // * 📄currentConcept = "worms",
            // * * content = "<(*,{Tweety},worms) --> food>",
            // * * indices = [0, 1]
            // * * => inh = "<(*,{Tweety},worms) --> food>"
            // * 📄currentConcept = "{lock1}",
            // * * content = "<(/,open,_,{lock1}) --> key>",
            // * * indices = [0, 1]
            // * * => inh = "<(/,open,_,{lock1}) --> key>"
            // * 📄currentConcept = "{lock1}",
            // * * content = "<key --> (/,open,_,{lock1})>",
            // * * indices = [1, 1]
            // * * => inh = "<key --> (/,open,_,{lock1})>"
            // * 📄currentConcept = "acid",
            // * * content = "<soda <-> (\,reaction,acid,_)>",
            // * * indices = [1, 0]
            // * * => inh = "<soda <-> (\,reaction,acid,_)>"
        }
        // * 🚩乘积在蕴含里边 | <<(*, term, #) --> #> ==> #>
        else if (indices.length == 3) {
            inh = taskContent.componentAt(indices[0]);
            // * 📄currentConcept = "(*,0)",
            // * * content = "<(*,(*,(*,0))) ==> num>",
            // * * indices = [0, 0, 0]
            // * * => inh = "(*,(*,(*,0)))"
            // * 📄currentConcept = "(*,0)",
            // * * content = "<num <-> (*,(*,(*,0)))>",
            // * * indices = [1, 0, 0]
            // * * => inh = "(*,(*,(*,0)))"
            // * 📄currentConcept = "(*,0)",
            // * * content = "<num <=> (*,(*,(*,0)))>",
            // * * indices = [1, 0, 0]
            // * * => inh = "(*,(*,(*,0)))"
            // * 📄currentConcept = "a",
            // * * content = "<like <-> (*,a,(/,like,_,a))>",
            // * * indices = [1, 1, 1]
            // * * => inh = "(*,a,(/,like,_,a))"
            // * 📄currentConcept = "b",
            // * * content = "<like <-> (*,(/,like,b,_),b)>",
            // * * indices = [1, 0, 0]
            // * * => inh = "(*,(/,like,b,_),b)"
            // * 📄currentConcept = "(/,num,_)",
            // * * content = "<num <-> (/,(*,(/,num,_)),_)>",
            // * * indices = [1, 0, 0]
            // * * => inh = "(/,(*,(/,num,_)),_)"
            // * 📄currentConcept = "num",
            // * * content = "<<$1 --> (/,num,_)> <=> <$1 --> (/,(*,num),_)>>",
            // * * indices = [0, 1, 0]
            // * * => inh = "<$1 --> (/,num,_)>"
            // * 📄currentConcept = "(*,num)",
            // * * content = "(&&,<#1 --> num>,<#1 --> (/,(*,num),_)>)",
            // * * indices = [1, 1, 0]
            // * * => inh = "<#1 --> (/,(*,num),_)>"
            // * 📄currentConcept = "(*,num)",
            // * * content = "<<$1 --> (/,(*,num),_)> ==> <$1 --> num>>",
            // * * indices = [0, 1, 0]
            // * * => inh = "<$1 --> (/,(*,num),_)>"
            // * 📄currentConcept = "(*,num)",
            // * * content = "<<$1 --> num> <=> <$1 --> (/,(*,num),_)>>",
            // * * indices = [1, 1, 0]
            // * * => inh = "<$1 --> (/,(*,num),_)>"
        }
        // * 🚩乘积在蕴含的条件中 | <(&&, <(*, term, #) --> #>, #) ==> #>
        else if (indices.length == 4) {
            // * 🚩提取其中的继承项
            final Term contentSubject = taskContent.componentAt(indices[0]);
            // * 🚩判断「条件句」
            // * 主项是「合取」
            final boolean conditionSubject = contentSubject instanceof Conjunction;
            // * 整体是「等价」或「合取在前头的『蕴含』」
            final boolean conditionWhole = (taskContent instanceof Implication && indices[0] == 0)
                    || taskContent instanceof Equivalence;
            if (conditionSubject && conditionWhole) {
                // * 🚩条件句⇒提取
                inh = ((CompoundTerm) contentSubject).componentAt(indices[1]);
                // * 📄currentConcept = "worms",
                // ****content="<(&&,<$1-->[with_wings]>,<(*,$1,worms)-->food>)==><$1-->bird>>",
                // * * indices = [0, 1, 0, 1]
                // * * => inh = "<(*,$1,worms) --> food>"
                // * 📄currentConcept = "worms",
                // ****content="<(&&,<$1-->flyer>,<$1-->[chirping]>,<(*,$1,worms)-->food>)==><$1-->bird>>",
                // * * indices = [0, 2, 0, 1]
                // * * => inh = "<(*,$1,worms) --> food>"
                // * 📄currentConcept = "open",
                // ****content="<(&&,<$1-->[(/,open,$2,_)]>,<$1-->(/,open,key,_)>)==><$1-->[(/,open,{$2},_)]>>",
                // * * indices = [0, 1, 1, 1]
                // * * => inh = "<$1 --> (/,open,key,_)>"
                // * 📄currentConcept = "open",
                // ****content="<(&&,<#1-->lock>,<#1-->(/,open,$2,_)>)==>(&,<#1-->lock>,<#1-->(/,open,$2,_)>)>",
                // * * indices = [0, 1, 1, 1]
                // * * => inh = "<#1 --> (/,open,$2,_)>"
                // * 📄currentConcept = "open",
                // ****content="<(&&,<#1-->lock>,<#1-->(/,open,$2,_)>)==>(*,<#1-->lock>,<#1-->(/,open,$2,_)>)>",
                // * * indices = [0, 1, 1, 1]
                // * * => inh = "<#1 --> (/,open,$2,_)>"
                // * 📄currentConcept = "open",
                // ****content="<(&&,<#1-->lock>,<#1-->(/,open,$2,_)>)==>(-,<#1-->lock>,<#1-->(/,open,$2,_)>)>",
                // * * indices = [0, 1, 1, 1]
                // * * => inh = "<#1 --> (/,open,$2,_)>"
                // * 📄currentConcept = "open",
                // ****content="<(&&,<#1-->lock>,<#1-->(/,open,$2,_)>)==>(|,<#1-->lock>,<#1-->(/,open,$2,_)>)>",
                // * * indices = [0, 1, 1, 1]
                // * * => inh = "<#1 --> (/,open,$2,_)>"
                // * 📄currentConcept = "open",
                // ****content="<(&&,<#1-->lock>,<#1-->(/,open,$2,_)>)==>(~,<#1-->lock>,<#1-->(/,open,$2,_)>)>",
                // * * indices = [0, 1, 1, 1]
                // * * => inh = "<#1 --> (/,open,$2,_)>"
                // * 📄currentConcept = "open",
                // ****content="<(&&,<#1-->lock>,<#1-->(/,open,$2,_)>)==>(||,<#1-->lock>,<#1-->(/,open,$2,_)>)>",
                // * * indices = [0, 1, 1, 1]
                // * * => inh = "<#1 --> (/,open,$2,_)>"
                // * 📄currentConcept = "worms",
                // ****content="<(&&,<{Tweety}-->[chirping]>,<(*,{Tweety},worms)-->food>)==><{Tweety}-->bird>>",
                // * * indices = [0, 1, 0, 1]
                // * * => inh = "<(*,{Tweety},worms) --> food>"
            } else
                // * 🚩失败⇒空⇒返回
                inh = null;
        } else
            // * 🚩失败⇒空⇒返回
            inh = null;

        // * 🚩提取出了继承项⇒开始转换
        if (inh instanceof Inheritance)
            // * 🚩【2024-07-03 11:35:40】修改：传入时复制
            transformProductImage((Inheritance) inh.clone(), taskContent.clone(), indices, context);
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
        // * 🚩提取参数
        final Term inhSubject = inh.getSubject();
        final Term inhPredicate = inh.getPredicate();
        final boolean backward = context.isBackward();
        // * 🚩预先分派 @ 转换的是整体
        if (inh.equals(oldContent)) {
            if (inhSubject instanceof CompoundTerm)
                // * 🚩转换前项
                transformSubjectProductImage((CompoundTerm) inhSubject, inhPredicate, context);
            if (inhPredicate instanceof CompoundTerm)
                // * 🚩转换后项
                transformPredicateProductImage(inhSubject, (CompoundTerm) inhPredicate, context);
            return;
        }
        // * 🚩词项 * //
        // * 📝此处针对各类「条件句」等复杂逻辑
        // * 📄inh="<#1 --> (*,(/,num,_))>"
        // * * oldContent="(&&,<#1 --> num>,<#1 --> (*,(/,num,_))>)"
        // * * indices=[1, 1, 0]
        // * 📄inh="<$1 --> (*,(/,num,_))>"
        // * * oldContent="<<$1 --> (*,(/,num,_))> ==> <$1 --> num>>"
        // * * indices=[0, 1, 0]
        // * 📄inh="<$1 --> (*,(/,num,_))>"
        // * * oldContent="<<$1 --> num> <=> <$1 --> (*,(/,num,_))>>"
        // * * indices=[1, 1, 0]
        // * 📄inh="<$1 --> (*,(/,num,_))>"
        // * * oldContent="<<$1 --> num> ==> <$1 --> (*,(/,num,_))>>"
        // * * indices=[1, 1, 0]
        // * 📄inh="<$1 --> (/,(*,num),_)>"
        // * * oldContent="<<$1 --> (/,(*,num),_)> ==> <$1 --> num>>"
        // * * indices=[0, 1, 0]
        // * 📄inh="<$1 --> (/,(*,num),_)>"
        // * * oldContent="<<$1 --> num> ==> <$1 --> (/,(*,num),_)>>"
        // * * indices=[1, 1, 0]
        // * 📄inh="<$1 --> (/,num,_)>"
        // * * oldContent="<<$1 --> (/,num,_)> <=> <$1 --> (/,(*,num),_)>>"
        // * * indices=[0, 1, 0]
        // * 📄inh="<(*,$1,lock1) --> open>"
        // * * oldContent="<<$1 --> key> ==> <(*,$1,lock1) --> open>>"
        // * * indices=[1, 0, 1]
        // * 📄inh="<#1 --> (*,acid,base)>"
        // * * oldContent="(&&,<#1 --> reaction>,<#1 --> (*,acid,base)>)"
        // * * indices=[1, 1, 1]
        // * 📄inh="<$1 --> (/,(*,num),_)>"
        // * * oldContent="<<$1 --> (/,(*,num),_)> <=> <(*,$1) --> num>>"
        // * * indices=[0, 1, 0]
        // * 🚩转换构造新的「继承」
        final Inheritance newInh = transformInheritance(inh, indices);
        if (newInh == null)
            return;

        // * 🚩选择或构建最终内容：模仿链接重构词项
        final Term content;
        if (indices.length == 2) // * 📄A @ <(*, A, B) --> R>
            content = newInh;
        else if (oldContent instanceof Statement && indices[0] == 1)
            content = makeStatement((Statement) oldContent, oldContent.componentAt(0), newInh);
        else {
            final ArrayList<Term> componentList;
            final Term condition = oldContent.componentAt(0);
            if ((oldContent instanceof Implication || oldContent instanceof Equivalence)
                    && (condition instanceof Conjunction)) {
                componentList = ((CompoundTerm) condition).cloneComponents();
                componentList.set(indices[1], newInh);
                final Term newCond = makeCompoundTerm((CompoundTerm) condition, componentList);
                content = makeStatement((Statement) oldContent, newCond, ((Statement) oldContent).getPredicate());
            } else {
                componentList = oldContent.cloneComponents();
                componentList.set(indices[0], newInh);
                if (oldContent instanceof Conjunction)
                    content = makeCompoundTerm(oldContent, componentList);
                else if ((oldContent instanceof Implication) || (oldContent instanceof Equivalence))
                    content = makeStatement((Statement) oldContent, componentList.get(0), componentList.get(1));
                else
                    content = null;
            }
        }
        if (content == null)
            return;

        // * 🚩预算 * //
        final Task task = context.getCurrentTask();
        final Budget budget = backward
                // * 🚩复合反向
                ? BudgetInference.compoundBackward(content, context)
                // * 🚩复合前向
                : BudgetInference.compoundForward(task.asJudgement(), content, context);

        // * 🚩结论 * //
        // * 📝「真值」在「导出任务」时（从「当前任务」）自动生成
        context.singlePremiseTask(content, task, budget);
    }

    /** 🆕从「转换 乘积/像」中提取出的「转换继承」函数 */
    private static Inheritance transformInheritance(
            final Statement inh,
            final short[] indices) {
        // * 📄inh="<$1 --> (/,num,_)>"
        // * * oldContent="<<$1 --> (/,num,_)> <=> <$1 --> (/,(*,num),_)>>"
        // * * indices=[0, 1, 0]
        // *=> newInh="<(*,$1) --> num>"
        // * 📄inh="<#1 --> (/,(*,num),_)>"
        // * * oldContent="(&&,<#1 --> num>,<#1 --> (/,(*,num),_)>)"
        // * * indices=[1, 1, 0]
        // *=> newInh="<(*,#1) --> (*,num)>"
        // * 📄inh="<$1 --> (/,(*,num),_)>"
        // * * oldContent="<<$1 --> (/,(*,num),_)> ==> <$1 --> num>>"
        // * * indices=[0, 1, 0]
        // *=> newInh="<(*,$1) --> (*,num)>"
        // * 📄inh="<$1 --> (/,(*,num),_)>"
        // * * oldContent="<<$1 --> num> <=> <$1 --> (/,(*,num),_)>>"
        // * * indices=[1, 1, 0]
        // *=> newInh="<(*,$1) --> (*,num)>"
        // * 📄inh="<$1 --> (/,(*,num),_)>"
        // * * oldContent="<<$1 --> num> ==> <$1 --> (/,(*,num),_)>>"
        // * * indices=[1, 1, 0]
        // *=> newInh="<(*,$1) --> (*,num)>"
        // * 📄inh="<#1 --> (*,(/,num,_))>"
        // * * oldContent="(&&,<#1 --> num>,<#1 --> (*,(/,num,_))>)"
        // * * indices=[1, 1, 0]
        // *=> newInh="<(\,#1,_) --> (/,num,_)>"
        // * 📄inh="<$1 --> (*,(/,num,_))>"
        // * * oldContent="<<$1 --> (*,(/,num,_))> ==> <$1 --> num>>"
        // * * indices=[0, 1, 0]
        // *=> newInh="<(\,$1,_) --> (/,num,_)>"
        // * 📄inh="<$1 --> (*,(/,num,_))>"
        // * * oldContent="<<$1 --> num> <=> <$1 --> (*,(/,num,_))>>"
        // * * indices=[1, 1, 0]
        // *=> newInh="<(\,$1,_) --> (/,num,_)>"
        // * 📄inh="<$1 --> (*,(/,num,_))>"
        // * * oldContent="<<$1 --> num> ==> <$1 --> (*,(/,num,_))>>"
        // * * indices=[1, 1, 0]
        // *=> newInh="<(\,$1,_) --> (/,num,_)>"
        // * 📄inh="<$1 --> (/,(*,num),_)>"
        // * * oldContent="<<$1 --> (/,(*,num),_)> <=> <(*,$1) --> num>>"
        // * * indices=[0, 1, 0]
        // *=> newInh="<(*,$1) --> (*,num)>"
        // * 🚩决定前后项（此时已完成对「继承」的转换）
        final short index = indices[indices.length - 1]; // * 📝取索引 @ 复合词项内 | 📄B@(/, R, B, _) => 1
        final short side = indices[indices.length - 2]; // * 📝取索引 @ 复合词项所属继承句 | (*, A, B)@<(*, A, B) --> R> => 0
        final CompoundTerm inhInner = (CompoundTerm) inh.componentAt(side); // * 📝拿到「继承」中的复合词项
        final Term subject;
        final Term predicate;
        if (inhInner instanceof Product)
            // * 🚩乘积⇒转像
            if (side == 0) {
                // * 🚩乘积在左侧⇒外延像
                // * 📝占位符位置：与词项链位置有关
                subject = inhInner.componentAt(index);
                predicate = makeImageExt((Product) inhInner, inh.getPredicate(), index);
            } else {
                // * 🚩乘积在右侧⇒内涵像
                // * 📝占位符位置：与词项链位置有关
                subject = makeImageInt((Product) inhInner, inh.getSubject(), index);
                predicate = inhInner.componentAt(index);
            }
        else if (inhInner instanceof ImageExt && (side == 1))
            // * 🚩外延像⇒乘积/换索引
            if (index == ((ImageExt) inhInner).getRelationIndex()) {
                // * 🚩链接来源正好是「关系词项」⇒转乘积
                // * * 📄「关系词项」如："open" @ "(/,open,$1,_)" | 始终在第一位，只是存储时放占位符的位置上
                subject = makeProduct(inhInner, inh.getSubject(), index);
                predicate = inhInner.componentAt(index);
            } else {
                // * 🚩其它⇒调转占位符位置
                // * * 📄「关系词项」如
                subject = inhInner.componentAt(index);
                predicate = makeImageExt((ImageExt) inhInner, inh.getSubject(), index);
            }
        else if (inhInner instanceof ImageInt && (side == 0))
            if (index == ((ImageInt) inhInner).getRelationIndex()) {
                subject = inhInner.componentAt(index);
                predicate = makeProduct(inhInner, inh.getPredicate(), index);
            } else {
                subject = makeImageInt((ImageInt) inhInner, inh.getPredicate(), index);
                predicate = inhInner.componentAt(index);
            }
        else
            return null;
        // * 🚩最终返回二元数组
        return makeInheritance(subject, predicate);
    }

    /**
     * Equivalent transformation between products and images when the subject is a
     * compound
     * {<(*, S, M) --> P>, S@(*, S, M)} |- <S --> (/, P, _, M)>
     * {<(\, P, _, M) --> S>, P@(\, P, _, M)} |- <P --> (*, S, M)>
     * {<(\, P, _, M) --> S>, M@(\, P, _, M)} |- <(\, P, S, _) --> M>
     *
     * @param subject   The subject term
     * @param predicate The predicate term
     * @param context   Reference to the derivation context
     */
    private static void transformSubjectProductImage(
            CompoundTerm subject, Term predicate,
            DerivationContextTransform context) {
        // * 🚩预置变量
        final Task task = context.getCurrentTask();
        final boolean backward = task.isQuestion();
        Budget budget;
        Inheritance inheritance;
        Term newSubj, newPred;
        // * 🚩积⇒外延像
        if (subject instanceof Product) {
            final Product product = (Product) subject;
            // * 🚩一次多个：遍历所有可能的索引
            for (short i = 0; i < product.size(); i++) {
                // * 🚩词项 * //
                newSubj = product.componentAt(i);
                newPred = makeImageExt(product, predicate, i);
                inheritance = makeInheritance(newSubj, newPred);
                if (inheritance == null)
                    continue;
                // * 🚩预算 * //
                budget = backward
                        // * 🚩复合反向
                        ? BudgetInference.compoundBackward(inheritance, context)
                        // * 🚩复合前向
                        : BudgetInference.compoundForward(task.asJudgement(), inheritance, context);
                // * 🚩结论 * //
                // * 📝「真值」在「导出任务」时（从「当前任务」）自动生成
                context.singlePremiseTask(inheritance, task, budget);
            }
        }
        // * 🚩内涵像⇒积/其它内涵像
        else if (subject instanceof ImageInt) {
            final ImageInt image = (ImageInt) subject;
            final int relationIndex = image.getRelationIndex();
            // * 🚩一次多个：遍历所有可能的索引
            for (short i = 0; i < image.size(); i++) {
                // * 🚩词项 * //
                // * 🚩根据「链接索引」与「关系索引（占位符位置）」的关系决定「积/像」
                if (i == relationIndex) {
                    // * 🚩转换回「积」
                    newSubj = image.componentAt(relationIndex);
                    newPred = makeProduct(image, predicate, relationIndex);
                } else {
                    // * 🚩更改位置
                    newSubj = makeImageInt((ImageInt) image, predicate, i);
                    newPred = image.componentAt(i);
                }
                inheritance = makeInheritance(newSubj, newPred);
                if (inheritance == null)
                    continue;
                // * 🚩预算 * //
                budget = backward
                        // * 🚩复合反向
                        ? BudgetInference.compoundBackward(inheritance, context)
                        // * 🚩复合前向
                        : BudgetInference.compoundForward(task.asJudgement(), inheritance, context);
                // * 🚩结论 * //
                // * 📝「真值」在「导出任务」时（从「当前任务」）自动生成
                context.singlePremiseTask(inheritance, task, budget);
            }
        }
    }

    /**
     * Equivalent transformation between products and images when the predicate is a
     * compound
     * {<P --> (*, S, M)>, S@(*, S, M)} |- <(\, P, _, M) --> S>
     * {<S --> (/, P, _, M)>, P@(/, P, _, M)} |- <(*, S, M) --> P>
     * {<S --> (/, P, _, M)>, M@(/, P, _, M)} |- <M --> (/, P, S, _)>
     *
     * @param subject   The subject term
     * @param predicate The predicate term
     * @param context   Reference to the derivation context
     */
    private static void transformPredicateProductImage(Term subject, CompoundTerm predicate,
            DerivationContextTransform context) {
        // * 🚩预置变量
        final Task task = context.getCurrentTask();
        final boolean backward = task.isQuestion();
        Budget budget;
        Inheritance inheritance;
        Term newSubj, newPred;
        // * 🚩积⇒外延像
        if (predicate instanceof Product) {
            final Product product = (Product) predicate;
            // * 🚩一次多个：遍历所有可能的索引
            for (short i = 0; i < product.size(); i++) {
                // * 🚩词项 * //
                newSubj = makeImageInt(product, subject, i);
                newPred = product.componentAt(i);
                inheritance = makeInheritance(newSubj, newPred);
                if (inheritance == null)
                    continue;
                // * 🚩预算 * //
                budget = backward
                        // * 🚩复合反向
                        ? BudgetInference.compoundBackward(inheritance, context)
                        // * 🚩复合前向
                        : BudgetInference.compoundForward(task.asJudgement(), inheritance, context);
                // * 🚩结论 * //
                // * 📝「真值」在「导出任务」时（从「当前任务」）自动生成
                context.singlePremiseTask(inheritance, task, budget);
            }
        }
        // * 🚩内涵像⇒积/其它内涵像
        else if (predicate instanceof ImageExt) {
            final ImageExt image = (ImageExt) predicate;
            final int relationIndex = image.getRelationIndex();
            // * 🚩一次多个：遍历所有可能的索引
            for (short i = 0; i < image.size(); i++) {
                // * 🚩词项 * //
                // * 🚩根据「链接索引」与「关系索引（占位符位置）」的关系决定「积/像」
                if (i == relationIndex) {
                    // * 🚩转换回「积」
                    newSubj = makeProduct(image, subject, relationIndex);
                    newPred = image.componentAt(relationIndex);
                } else {
                    // * 🚩更改位置
                    newSubj = image.componentAt(i);
                    newPred = makeImageExt((ImageExt) image, subject, i);
                }
                inheritance = makeInheritance(newSubj, newPred);
                if (inheritance == null)
                    continue;
                // * 🚩预算 * //
                budget = backward // jmv <<<<<
                        // * 🚩复合反向
                        ? BudgetInference.compoundBackward(inheritance, context)
                        // * 🚩复合前向
                        : BudgetInference.compoundForward(task.asJudgement(), inheritance, context);
                // * 🚩结论 * //
                // * 📝「真值」在「导出任务」时（从「当前任务」）自动生成
                context.singlePremiseTask(inheritance, task, budget);
            }
        }
    }
}
