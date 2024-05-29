package nars.control;

import java.util.ArrayList;

import nars.entity.*;
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
    public static ArrayList<TermLink> prepareComponentLinks(CompoundTerm self) {
        final ArrayList<TermLink> componentLinks = new ArrayList<>();
        // * 🚩预备「默认类型」：自身为陈述⇒陈述，自身为复合⇒复合
        final short type = (self instanceof Statement) ? TermLink.COMPOUND_STATEMENT : TermLink.COMPOUND; // default
        // * 🚩建立连接：从自身到自身开始
        prepareComponentLinks(self, componentLinks, type, self);
        return componentLinks;
    }

    /**
     * Collect TermLink templates into a list, go down one level except in
     * special cases
     * <p>
     * * ❗重要逻辑：词项链的构造
     *
     * @param self           The CompoundTerm for which the links are built
     * @param componentLinks The list of TermLink templates built so far
     * @param type           The type of TermLink to be built
     * @param term           The CompoundTerm for which the links are built
     */
    private static void prepareComponentLinks(
            final CompoundTerm self,
            final ArrayList<TermLink> componentLinks,
            final short type,
            final CompoundTerm term) {
        // * 🚩从目标第一层元素出发
        for (int i = 0; i < term.size(); i++) { // first level components
            /** 第一层元素 */
            final Term t1 = term.componentAt(i);
            // * 🚩「常量」词项⇒直接链接
            if (t1.isConstant()) {
                componentLinks.add(new TermLink(t1, type, new int[] { i }));
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
                        componentLinks,
                        TermLink.COMPOUND_CONDITION, // * 🚩改变「默认类型」为「复合条件」
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
                            // * 🚩NAL-4「转换」相关
                            final int[] indexes = type == TermLink.COMPOUND_CONDITION
                                    // * 📝若背景的「链接类型」已经是「复合条件」⇒已经深入了一层，并且一定在「主项」位置
                                    ? new int[] { 0, i, j }
                                    // * 📝否则就还是第二层
                                    : new int[] { i, j };
                            componentLinks.add(new TermLink(t2, TermLink.TRANSFORM, indexes));
                        } else {
                            // * 🚩非「转换」相关：直接按类型添加
                            componentLinks.add(new TermLink(t2, type, new int[] { i, j }));
                        }
                    }
                    // * 🚩直接处理 @ 第三层
                    final boolean transformT2 = t2 instanceof Product || t2 instanceof ImageExt
                            || t2 instanceof ImageInt;
                    if (transformT2) {
                        // * 🚩NAL-4「转换」相关
                        for (int k = 0; k < ((CompoundTerm) t2).size(); k++) {
                            final Term t3 = ((CompoundTerm) t2).componentAt(k);
                            if (t3.isConstant()) { // third level
                                final int[] indexes = type == TermLink.COMPOUND_CONDITION
                                        // * 📝此处若是「复合条件」即为最深第四层
                                        ? new int[] { 0, i, j, k }
                                        // * 📝否则仅第三层
                                        : new int[] { i, j, k };
                                componentLinks.add(new TermLink(t3, TermLink.TRANSFORM, indexes));
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
        final Memory memory = context.getMemory();
        final Task task = context.getCurrentTask();
        final BudgetValue taskBudget = task.getBudget();
        // * 🚩对当前任务构造任务链，链接到传入的任务
        final TaskLink taskLink = new TaskLink(task, null, taskBudget); // link type: SELF
        insertTaskLink(self, memory, taskLink);
        // * 🚩仅在「自身为复合词项」且「词项链模板非空」时准备
        // * 📝只有复合词项会有「对子项的词项链」，子项不会持有「对所属词项的词项链」
        if (!(self.getTerm() instanceof CompoundTerm && self.getTermLinkTemplates().size() > 0))
            return;
        // * 🚩分发并指数递减预算值
        final BudgetValue subBudget = BudgetFunctions.distributeAmongLinks(
                taskBudget,
                self.getTermLinkTemplates().size());
        if (!subBudget.aboveThreshold())
            return;
        // * 🚩仅在「预算达到阈值」时：遍历预先构建好的所有「子项词项链模板」，递归链接到任务
        for (final TermLink termLink : self.getTermLinkTemplates()) {
            // if (!(task.isStructural() && (termLink.getType() == TermLink.TRANSFORM)))
            // continue;
            // // avoid circular transform
            final Term componentTerm = termLink.getTarget();
            // ! 📝数据竞争：不能在「其它概念被拿出去后」并行推理，会导致重复创建概念
            final Concept componentConcept = memory.getConceptOrCreate(componentTerm);
            if (componentConcept == null)
                continue;
            // * 🚩为子项的概念构造新词项链，并在其中使用模板
            final TaskLink tLink = new TaskLink(task, termLink, subBudget);
            // * ⚠️注意此处让「元素词项对应的概念」也插入了任务链——干涉其它「概念」的运作
            insertTaskLink(componentConcept, memory, tLink);
        }
        // * 🚩递归插入词项链
        buildTermLinks(self, memory, taskBudget); // recursively insert TermLink
    }

    /**
     * Insert a TaskLink into the TaskLink bag
     * <p>
     * called only from Memory.continuedProcess
     *
     * @param taskLink The termLink to be inserted
     */
    private static void insertTaskLink(final Concept self, final Memory memory, final TaskLink taskLink) {
        final BudgetValue linkBudget = taskLink.getBudget();
        // * 📝注意：任务链の预算 ≠ 任务の预算；「任务链」与「所链接的任务」是不同的Item对象
        self.putInTaskLink(taskLink);
        // * 🚩插入「任务链」的同时，以「任务链」激活概念
        memory.activateConcept(self, linkBudget);
    }

    /**
     * Recursively build TermLinks between a compound and its components
     * <p>
     * called only from Memory.continuedProcess
     * * ❌【2024-05-30 00:49:19】无法断言原先传入的「当前概念」「当前记忆区」「当前任务预算值」都来自「直接推理上下文」
     * * 📝原因：需要递归处理，并在这其中改变self、memory与taskBudget三个参数
     *
     * @param taskBudget The BudgetValue of the task
     */
    private static void buildTermLinks(final Concept self, final Memory memory, final BudgetValue taskBudget) {
        // * 🚩仅在有「词项链模板」时
        if (self.getTermLinkTemplates().isEmpty())
            return;
        // * 🚩分派链接，继续
        final BudgetValue subBudget = BudgetFunctions.distributeAmongLinks(
                taskBudget,
                self.getTermLinkTemplates().size());
        if (!subBudget.aboveThreshold())
            return;
        // * 🚩仅在超过阈值时：遍历所有「词项链模板」
        for (final TermLink template : self.getTermLinkTemplates()) {
            if (template.getType() == TermLink.TRANSFORM)
                continue;
            // * 🚩仅在链接类型不是「转换」时
            final Term component = template.getTarget();
            final Concept concept = memory.getConceptOrCreate(component);
            // * 🚩仅在「元素词项所对应概念」存在时
            if (concept == null)
                continue;
            // * 🚩建立双向链接
            final TermLink termLink1 = new TermLink(component, template, subBudget);
            insertTermLink(self, termLink1); // this termLink to that
            final TermLink termLink2 = new TermLink(self.getTerm(), template, subBudget);
            insertTermLink(concept, termLink2); // that termLink to this
            // * 🚩对复合子项 继续深入递归
            if (component instanceof CompoundTerm) {
                buildTermLinks(concept, memory, subBudget);
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
