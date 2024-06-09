package nars.control;

import java.util.ArrayList;

import nars.entity.*;
import nars.entity.TLink.TLinkType;
import nars.inference.*;
import nars.language.*;
import nars.storage.Memory;

/**
 * 负责「概念」中「词项链」「任务链」的建立
 * * ℹ️自先前`nars.entity.Concept`中分离出来
 * * 🔗{@link Concept}
 * * 🔗{@link TermLink}
 * * 🔗{@link TaskLinkLink}
 */
public abstract class ConceptLinking {

    /* ----- link CompoundTerm and its components ----- */
    /**
     * Build TermLink templates to constant components and sub-components
     * <p>
     * The compound type determines the link type; the component type determines
     * whether to build the link.
     *
     * @return A list of TermLink templates
     */
    public static ArrayList<TermLinkTemplate> prepareTermLinkTemplates(Term self) {
        // * 🚩创建返回值
        final ArrayList<TermLinkTemplate> linksToSelf = new ArrayList<>();
        // * 🚩不是复合词项⇒返回空
        if (!(self instanceof CompoundTerm))
            return linksToSelf;
        // * 🚩预备「默认类型」：自身为陈述⇒陈述，自身为复合⇒复合
        final TLinkType type = (self instanceof Statement) ? TLinkType.COMPOUND_STATEMENT : TLinkType.COMPOUND; // default
        // * 🚩建立连接：从「自身到自身」开始
        prepareComponentLinks((CompoundTerm) self, linksToSelf, type, (CompoundTerm) self);
        return linksToSelf;
    }

    /**
     * Collect TermLink templates into a list, go down one level except in
     * special cases
     * <p>
     * * ❗重要逻辑：词项链的构造 | ❓看似构造了「从元素链接到自身」但实际上「目标」却是「元素」
     *
     * @param self        The CompoundTerm for which the links are built
     * @param linksToSelf The list of TermLink templates built so far
     * @param type        The type of TermLink to be built
     * @param term        The CompoundTerm for which the links are built
     */
    private static void prepareComponentLinks(
            final CompoundTerm self,
            final ArrayList<TermLinkTemplate> linksToSelf,
            final TLinkType type,
            final CompoundTerm term) {
        // * 🚩从目标第一层元素出发
        for (int i = 0; i < term.size(); i++) { // first level components
            /** 第一层元素 */
            final Term t1 = term.componentAt(i);
            // * 🚩「常量」词项⇒直接链接 | 构建「元素→自身」的「到复合词项」类型
            if (t1.isConstant()) {
                linksToSelf.add(new TermLinkTemplate(t1, type, new int[] { i }));
                // * 📝【2024-05-15 18:21:25】案例笔记 概念="<(&&,A,B) ==> D>"：
                // * 📄self="<(&&,A,B) ==> D>" ~> "(&&,A,B)" [i=0]
                // * @ 4=COMPOUND_STATEMENT "At C, point to <C --> A>"
                // * 📄self="(&&,A,B)" ~> "A" [i=0]
                // * @ 6=COMPOUND_CONDITION "At C, point to <(&&, C, B) ==> A>"
                // * 📄self="(&&,A,B)" ~> "B" [i=1]
                // * @ 6=COMPOUND_CONDITION "At C, point to <(&&, C, B) ==> A>"
                // * 📄self="<(&&,A,B) ==> D>" ~> "D" [i=1]
                // * @ 4=COMPOUND_STATEMENT "At C, point to <C --> A>"
                // * 📄self="(&&,A,B)" ~> "A" [i=0]
                // * @ 2=COMPOUND "At C, point to (&&, A, C)"
                // * 📄self="(&&,A,B)" ~> "B" [i=1]
                // * @ 2=COMPOUND "At C, point to (&&, A, C)"
            }
            // * 🚩条件类链接⇒递归
            final boolean isConditionalCompound =
                    // * 📌自身和索引必须先是「蕴含の主词」或「等价」，如 <# ==> C> 或 <# <=> #>
                    self instanceof Equivalence || (self instanceof Implication && i == 0);
            final boolean isConditionalComponent =
                    // * 🚩然后「内部词项」必须是「合取」或「否定」
                    t1 instanceof Conjunction || t1 instanceof Negation;
            final boolean isConditional = isConditionalCompound && isConditionalComponent;
            if (isConditional)
                // * 📝递归深入，将作为「入口」的「自身向自身建立链接」缩小到「组分」区域
                prepareComponentLinks(
                        (CompoundTerm) t1,
                        linksToSelf,
                        TLinkType.COMPOUND_CONDITION, // * 🚩改变「默认类型」为「复合条件」
                        (CompoundTerm) t1);
            // * 🚩其它情况⇒若元素为复合词项，再度深入
            else if (t1 instanceof CompoundTerm) {
                for (int j = 0; j < ((CompoundTerm) t1).size(); j++) { // second level components
                    /** 第二层元素 */
                    final Term t2 = ((CompoundTerm) t1).componentAt(j);
                    // * 🚩直接处理 @ 第二层
                    if (t2.isConstant()) {
                        // * 📌【2024-05-27 21:24:32】先前就是此处尝试「正交化」导致语义改变
                        final boolean transformT1 = t1 instanceof Product || t1 instanceof ImageExt
                                || t1 instanceof ImageInt;
                        if (transformT1) {
                            // * 🚩NAL-4「转换」相关 | 构建「复合→复合」的「转换」类型（仍然到复合词项）
                            final int[] indexes = type == TLinkType.COMPOUND_CONDITION
                                    // * 📝若背景的「链接类型」已经是「复合条件」⇒已经深入了一层，并且一定在「主项」位置
                                    ? new int[] { 0, i, j }
                                    // * 📝否则就还是第二层
                                    : new int[] { i, j };
                            linksToSelf.add(new TermLinkTemplate(t2, TLinkType.TRANSFORM, indexes));
                        } else {
                            // * 🚩非「转换」相关：直接按类型添加 | 构建「元素→自身」的「到复合词项」类型
                            linksToSelf.add(new TermLinkTemplate(t2, type, new int[] { i, j }));
                        }
                    }
                    // * 🚩直接处理 @ 第三层
                    final boolean transformT2 = t2 instanceof Product || t2 instanceof ImageExt
                            || t2 instanceof ImageInt;
                    if (transformT2) {
                        // * 🚩NAL-4「转换」相关 | 构建「复合→复合」的「转换」类型（仍然到复合词项）
                        for (int k = 0; k < ((CompoundTerm) t2).size(); k++) {
                            final Term t3 = ((CompoundTerm) t2).componentAt(k);
                            if (t3.isConstant()) { // third level
                                final int[] indexes = type == TLinkType.COMPOUND_CONDITION
                                        // * 📝此处若是「复合条件」即为最深第四层
                                        ? new int[] { 0, i, j, k }
                                        // * 📝否则仅第三层
                                        : new int[] { i, j, k };
                                linksToSelf.add(new TermLinkTemplate(t3, TLinkType.TRANSFORM, indexes));
                            }
                        }
                    }
                }
            }
        }
    }

    /* ---------- insert Links for indirect processing ---------- */

    /**
     * Link to a new task from all relevant concepts for continued processing in
     * the near future for unspecified time.
     * <p>
     * The only method that calls the TaskLink constructor.
     * * 📝【2024-05-30 00:37:39】此时该方法从「直接推理」被调用，同时「概念」「任务」「记忆区」均来自「直接推理上下文」
     *
     * @param task    The task to be linked
     * @param content The content of the task
     */
    public static void linkConceptToTask(final DerivationContextDirect context) {
        final Concept self = context.getCurrentConcept();
        final Memory memory = context.mutMemory(); // ! 可变：需要「取/创建 概念」
        final Task task = context.getCurrentTask();
        // * 🚩对当前任务构造任务链，链接到传入的任务 | 构造「自身」
        final TaskLink selfLink = TaskLink.newSelf(task); // link type: SELF
        insertTaskLink(self, memory, selfLink);
        // * 🚩仅在「自身为复合词项」且「词项链模板非空」时准备
        // * 📝只有复合词项会有「对子项的词项链」，子项不会持有「对所属词项的词项链」
        if (!(self.getTerm() instanceof CompoundTerm && self.getLinkTemplatesToSelf().size() > 0))
            return;
        // * 🚩分发并指数递减预算值
        final Budget subBudget = BudgetFunctions.distributeAmongLinks(
                task,
                self.getLinkTemplatesToSelf().size());
        if (!subBudget.budgetAboveThreshold())
            return;
        // * 🚩仅在「预算达到阈值」时：遍历预先构建好的所有「子项词项链模板」，递归链接到任务
        for (final TermLinkTemplate template : self.getLinkTemplatesToSelf()) {
            // if (!(task.isStructural() && (termLink.getType() == TLinkType.TRANSFORM)))
            // continue;
            // // avoid circular transform
            final Term componentTerm = template.getTarget();
            // ! 📝数据竞争：不能在「其它概念被拿出去后」并行推理，会导致重复创建概念
            final Concept componentConcept = memory.getConceptOrCreate(componentTerm);
            if (componentConcept == null)
                continue;
            // * 🚩为子项的概念构造新词项链，并在其中使用模板（的类型和索引）
            final TaskLink tLink = TaskLink.fromTemplate(task, template, subBudget);
            // * ⚠️注意此处让「元素词项对应的概念」也插入了任务链——干涉其它「概念」的运作
            insertTaskLink(componentConcept, memory, tLink);
        }
        // * 🚩从当前词项开始，递归插入词项链 | 📌
        buildTermLinks(self, memory, task); // recursively insert TermLink
    }

    /**
     * Insert a TaskLink into the TaskLink bag
     * <p>
     * called only from Memory.continuedProcess
     *
     * @param taskLink The termLink to be inserted
     */
    private static void insertTaskLink(final Concept self, final Memory memory, final TaskLink taskLink) {
        // * 📝注意：任务链の预算 ≠ 任务の预算；「任务链」与「所链接的任务」是不同的Item对象
        self.putInTaskLink(taskLink);
        // * 🚩插入「任务链」的同时，以「任务链」激活概念 | 直接传入【可预算】的任务链
        memory.activateConcept(self, taskLink);
    }

    /**
     * Recursively build TermLinks between a compound and its components
     * <p>
     * called only from Memory.continuedProcess
     * * ❌【2024-05-30 00:49:19】无法断言原先传入的「当前概念」「当前记忆区」「当前任务预算值」都来自「直接推理上下文」
     * * 📝原因：需要递归处理，并在这其中改变self、memory与taskBudget三个参数
     *
     * @param sourceBudget The Budget of the task
     */
    private static void buildTermLinks(final Concept self, final Memory memory, final Budget sourceBudget) {
        // * 🚩仅在有「词项链模板」时
        if (self.getLinkTemplatesToSelf().isEmpty())
            return;
        // * 🚩分派链接，更新预算值，继续
        // * 📝太大的词项、太远的链接 根据AIKR有所取舍
        final Budget subBudget = BudgetFunctions.distributeAmongLinks(
                sourceBudget,
                self.getLinkTemplatesToSelf().size());
        if (!subBudget.budgetAboveThreshold())
            return;
        // * 🚩仅在超过阈值时：遍历所有「词项链模板」
        for (final TermLinkTemplate template : self.getLinkTemplatesToSelf()) {
            if (template.getType() == TLinkType.TRANSFORM)
                continue;
            // * 🚩仅在链接类型不是「转换」时
            final Term component = template.getTarget();
            final Term selfTerm = self.getTerm();
            final Concept componentConcept = memory.getConceptOrCreate(component);
            // * 🚩仅在「元素词项所对应概念」存在时
            if (componentConcept == null)
                continue;
            // * 🚩建立双向链接：整体⇒元素
            final TermLink termLink1 = TermLink.fromTemplate(component, template, subBudget);
            insertTermLink(self, termLink1); // this termLink to that
            // * 🚩建立双向链接：元素⇒整体
            final TermLink termLink2 = TermLink.fromTemplate(selfTerm, template, subBudget);
            insertTermLink(componentConcept, termLink2); // that termLink to this
            // * 🚩对复合子项 继续深入递归
            if (component instanceof CompoundTerm) {
                buildTermLinks(componentConcept, memory, subBudget);
            }
        }
    }

    /**
     * Insert a TermLink into the TermLink bag
     * <p>
     * called from buildTermLinks only
     *
     * @param termLink The termLink to be inserted
     */
    private static void insertTermLink(final Concept self, final TermLink termLink) {
        self.putInTermLink(termLink);
    }
}
