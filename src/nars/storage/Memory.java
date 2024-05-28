package nars.storage;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import nars.control.DerivationContext;
import nars.control.DerivationContextDirect;
import nars.control.DerivationContextReason;
import nars.entity.BudgetValue;
import nars.entity.Concept;
import nars.entity.Sentence;
import nars.entity.Stamp;
import nars.entity.Task;
import nars.entity.TaskLink;
import nars.entity.TermLink;
import nars.inference.BudgetFunctions;
import nars.inference.RuleTables;
import nars.io.IInferenceRecorder;
import nars.language.Term;
import nars.main_nogui.Parameters;
import nars.main_nogui.ReasonerBatch;

/**
 * The memory of the system.
 */
public class Memory {

    /**
     * The type of report.
     * * 🚩【2024-04-19 12:44:36】增加了多种输出方式
     */
    public enum ReportType {
        IN,
        OUT,
        ANSWER,
        EXE;

        /**
         * 将报告类型转换为字符串
         * * 📝Java在枚举的开头用一个语句定义所有枚举项
         *
         * @param type 报告类型
         * @return 字符串（仅名称）
         */
        @Override
        public String toString() {
            switch (this) {
                case IN:
                    return "IN";
                case OUT:
                    return "OUT";
                case ANSWER:
                    return "ANSWER";
                case EXE:
                    return "EXE";
                default: // * 穷举后不会发生
                    return "OTHER";
            }
        }
    }

    /**
     * Backward pointer to the reasoner
     */
    private final ReasonerBatch reasoner;

    /* ---------- Long-term storage for multiple cycles ---------- */
    /**
     * Concept bag. Containing all Concepts of the system
     */
    private final ConceptBag concepts;
    /**
     * New tasks with novel composed terms, for delayed and selective processing
     */
    private final NovelTaskBag novelTasks;
    /**
     * Inference record text to be written into a log file
     */
    private IInferenceRecorder recorder;
    private final AtomicInteger beliefForgettingRate = new AtomicInteger(Parameters.TERM_LINK_FORGETTING_CYCLE);
    private final AtomicInteger taskForgettingRate = new AtomicInteger(Parameters.TASK_LINK_FORGETTING_CYCLE);
    private final AtomicInteger conceptForgettingRate = new AtomicInteger(Parameters.CONCEPT_FORGETTING_CYCLE);

    /* ---------- Short-term workspace for a single cycle ---------- */
    /**
     * List of new tasks accumulated in one cycle, to be processed in the next
     * cycle
     */
    private final LinkedList<Task> newTasks;
    /**
     * List of Strings or Tasks to be sent to the output channels
     */
    private final ArrayList<String> exportStrings;

    // /**
    // * 🆕新的「推理上下文」对象
    // * * 🚩【2024-05-18 17:12:03】目前重复使用，好像它就是「记忆区中变量的一部分」一样
    // */
    // public DerivationContext context = new DerivationContext(this);

    /* ---------- Constructor ---------- */
    /**
     * Create a new memory
     * <p>
     * Called in Reasoner.reset only
     *
     * @param reasoner
     */
    public Memory(ReasonerBatch reasoner) {
        this.reasoner = reasoner;
        recorder = new NullInferenceRecorder();
        concepts = new ConceptBag(this);
        novelTasks = new NovelTaskBag(this);
        newTasks = new LinkedList<>();
        exportStrings = new ArrayList<>();
    }

    public void init() {
        concepts.init();
        novelTasks.init();
        newTasks.clear();
        exportStrings.clear();
        reasoner.initTimer();
        DerivationContext.init();
        recorder.append("\n-----RESET-----\n");
    }

    /* ---------- access utilities ---------- */

    public ArrayList<String> getExportStrings() {
        return exportStrings;
    }

    public IInferenceRecorder getRecorder() {
        return recorder;
    }

    public void setRecorder(IInferenceRecorder recorder) {
        this.recorder = recorder;
    }

    public long getTime() {
        return reasoner.getTime();
    }

    /**
     * 用于转发推理器的{@link ReasonerBatch#getTimer}
     */
    public long getTimer() {
        return reasoner.getTimer();
    }

    /**
     * 用于转发推理器的{@link ReasonerBatch#updateTimer}
     */
    public long updateTimer() {
        return reasoner.updateTimer();
    }

    /**
     * 获取「静默值」
     * * 🎯在「推理上下文」中无需获取「推理器」`getReasoner`
     *
     * @return 静默值
     */
    public AtomicInteger getSilenceValue() {
        return reasoner.getSilenceValue();
    }

    // public MainWindow getMainWindow() {
    // return reasoner.getMainWindow();
    // }

    /* ---------- conversion utilities ---------- */
    /**
     * Get an existing Concept for a given name
     * <p>
     * called from Term and
     * ConceptWindow.
     *
     * @param name the name of a concept
     * @return a Concept or null
     */
    public Concept nameToConcept(String name) {
        return concepts.get(name);
    }

    /**
     * Get a Term for a given name of a Concept or Operator
     * <p>
     * called in
     * StringParser and the make methods of compound terms.
     *
     * @param name the name of a concept or operator
     * @return a Term or null (if no Concept/Operator has this name)
     */
    public Term nameToListedTerm(String name) {
        Concept concept = concepts.get(name);
        if (concept != null) {
            return concept.getTerm();
        }
        return null;
    }

    /**
     * Get an existing Concept for a given Term.
     *
     * @param term The Term naming a concept
     * @return a Concept or null
     */
    public Concept termToConcept(Term term) {
        return nameToConcept(term.getName());
    }

    /**
     * 🆕判断「记忆区中是否已有概念」
     * * 🚩Check if a Term has a Concept.
     *
     * @param term The Term naming a concept
     * @return true if the Term has a Concept in the memory
     */
    public boolean hasConcept(Term term) {
        return termToConcept(term) != null;
    }

    /**
     * Get the Concept associated to a Term, or create it.
     *
     * @param term indicating the concept
     * @return an existing Concept, or a new one, or null ( TODO bad smell )
     */
    public Concept getConceptOrCreate(Term term) {
        // * 🚩不给「非常量词项」新建概念
        if (!term.isConstant()) {
            return null;
        }
        // * 🚩尝试从概念袋中获取「已有概念」，否则创建概念
        final Concept concept = termToConcept(term);
        return concept == null ? makeNewConcept(term) : concept;
    }

    /**
     * 🆕新建一个概念
     * * 📌概念只可能由此被创建
     *
     * @param term 概念对应的词项
     * @return 已经被置入「概念袋」的概念 | 创建失败时返回`
     */
    private Concept makeNewConcept(Term term) {
        final Concept concept = new Concept(term, this); // the only place to make a new Concept
        final boolean created = concepts.putIn(concept);
        return created ? concept : null;
    }

    /**
     * Get the current activation level of a concept.
     *
     * @param t The Term naming a concept
     * @return the priority value of the concept
     */
    public float getConceptActivation(Term t) {
        Concept c = termToConcept(t);
        return (c == null) ? 0f : c.getPriority();
    }

    /* ---------- adjustment functions ---------- */
    /**
     * Adjust the activation level of a Concept
     * <p>
     * called in
     * Concept.insertTaskLink only
     *
     * @param c the concept to be adjusted
     * @param b the new BudgetValue
     */
    public void activateConcept(Concept c, BudgetValue b) {
        concepts.pickOut(c.getKey());
        BudgetFunctions.activate(c, b);
        concepts.putBack(c);
    }

    /* ---------- new task entries ---------- */

    /*
     * There are several types of new tasks, all added into the
     * newTasks list, to be processed in the next workCycle.
     * Some of them are reported and/or logged.
     */
    /**
     * Input task processing. Invoked by the outside or inside environment.
     * Outside: StringParser (input); Inside: Operator (feedback). Input tasks
     * with low priority are ignored, and the others are put into task buffer.
     *
     * @param task The input task
     */
    public void inputTask(Task task) {
        if (task.getBudget().aboveThreshold()) {
            recorder.append("!!! Perceived: " + task + "\n");
            this.report(task.getSentence(), ReportType.IN); // report input
            newTasks.add(task); // wait to be processed in the next workCycle
        } else {
            recorder.append("!!! Neglected: " + task + "\n");
        }
    }

    /**
     * Display input/output sentence in the output channels. The only place to
     * add Objects into exportStrings. Currently only Strings are added, though
     * in the future there can be outgoing Tasks; also if exportStrings is empty
     * display the current value of timer ( exportStrings is emptied in
     * {@link ReasonerBatch#doTick()} - TODO fragile mechanism)
     *
     */
    public void report(Sentence sentence, ReportType type) {
        report(DerivationContext.generateReportString(sentence, type));
    }

    /**
     * 🆕只报告字符串
     * * 🎯从「吸收上下文」中调用
     * * 🎯从「直接报告」中转发
     *
     * @param output 要输出的字符串
     */
    public void report(String output) {
        if (ReasonerBatch.DEBUG) {
            System.out.println("// report( clock " + getTime()
            // + ", input " + input
                    + ", timer " + getTimer()
                    + ", output " + output
                    + ", exportStrings " + exportStrings);
            System.out.flush();
        }
        if (exportStrings.isEmpty()) {
            long timer = updateTimer();
            if (timer > 0) {
                exportStrings.add(String.valueOf(timer));
            }
        }
        exportStrings.add(output);
    }

    /* ---------- system working workCycle ---------- */
    /**
     * An atomic working cycle of the system: process new Tasks, then fire a
     * concept
     * <p>
     * Called from Reasoner.tick only
     *
     * @param clock The current time to be displayed
     */
    public void workCycle(long clock) {
        recorder.append(" --- " + clock + " ---\n");

        // * 🚩本地任务直接处理 阶段 * //
        final boolean noResult = processDirect(this);

        // * 🚩从「直接推理」到「概念推理」过渡 阶段 * //
        // * 🚩选择概念、选择任务链、选择词项链（中间亦有推理）
        final DerivationContextReason context = new DerivationContextReason(this);
        final Iterable<TermLink> toReasonLinks = preprocessConcept(this, noResult, context);

        // * 🚩内部概念高级推理 阶段 * //
        if (toReasonLinks != null)
            // * 🚩都选好了⇒开始
            processConcept(toReasonLinks, context);

        // * 🚩最后收尾 阶段 * //
        // * 🚩原「清空上下文」已迁移至各「推理」阶段
        novelTasks.refresh();
    }

    /**
     * 吸收「推理上下文」
     * * 🚩【2024-05-19 18:39:44】现在会在每次「准备上下文⇒推理」的过程中执行
     * * 🎯变量隔离，防止「上下文串线」与「重复使用」
     */
    public void absorbContext(final DerivationContext context) {
        // final DerivationContext context = this.context;
        // * 🚩将推理导出的「新任务」添加到自身新任务中（先进先出）
        for (final Task newTask : context.getNewTasks()) {
            this.newTasks.add(newTask);
        }
        // * 🚩将推理导出的「导出字串」添加到自身「导出字串」中（先进先出）
        for (final String output : context.getExportStrings()) {
            this.report(output);
        }
        // * 清理上下文防串（同时清理「导出的新任务」与「导出字串」）
        context.clear();
    }

    /**
     * 🆕本地直接推理
     * * 🚩最终只和「本地规则」与{@link Concept#directProcess}有关
     */ // TODO: 待迁移
    private static boolean processDirect(final Memory self) {
        // * 🚩处理已有任务（新任务/新近任务）
        boolean noResult = processNewTask(self);
        // * 📝`processNewTask`可能会产生新任务，此举将影响到`noResult`的值
        if (noResult) { // necessary?
            // ! ❌【2024-05-19 22:51:03】不能内联逻辑：后边的「处理任务」受到前边任务处理条件的制约
            // * 🚩【2024-05-19 22:51:22】故不能同义实现「统一获取任务，统一立即处理」的机制
            final boolean noResultNovel = processNovelTask(self);
            if (!noResultNovel)
                noResult = false;
        }
        // * 🚩推理结束
        return noResult;
    }

    /**
     * Process the newTasks accumulated in the previous workCycle, accept input
     * ones and those that corresponding to existing concepts, plus one from the
     * buffer.
     */ // TODO: 待迁移
    private static boolean processNewTask(final Memory self) {
        // * 🚩获取新任务
        final LinkedList<Task> tasksToProcess = self.getNewTasks();
        // * 🚩处理新任务
        final boolean noResult = immediateProcess(self, tasksToProcess);
        // * 🚩清理收尾
        tasksToProcess.clear();
        return noResult;
    }

    /**
     * 🆕获取「要处理的新任务」列表
     */
    public LinkedList<Task> getNewTasks() {
        // * 🚩处理新输入：立刻处理 or 加入「新近任务」 or 忽略
        final LinkedList<Task> tasksToProcess = new LinkedList<>();
        // don't include new tasks produced in the current workCycle
        for (int counter = newTasks.size(); counter > 0; counter--) {
            final Task task = newTasks.removeFirst();
            if (task.isInput() || hasConcept(task.getContent())) {
                tasksToProcess.add(task); // new input or existing concept
            } else {
                final Sentence s = task.getSentence();
                if (s.isJudgment()) {
                    final double d = s.getTruth().getExpectation();
                    if (d > Parameters.DEFAULT_CREATION_EXPECTATION) {
                        novelTasks.putIn(task); // new concept formation
                    } else {
                        recorder.append("!!! Neglected: " + task + "\n");
                    }
                }
            }
        }
        return tasksToProcess;
    }

    /**
     * Select a novel task to process.
     */ // TODO: 待迁移
    private static boolean processNovelTask(final Memory self) {
        // * 🚩获取新近任务
        final LinkedList<Task> tasksToProcess = self.getNovelTasks();
        // * 🚩处理新近任务
        final boolean noResult = immediateProcess(self, tasksToProcess);
        // * 🚩清理收尾
        tasksToProcess.clear();
        return noResult;
    }

    /**
     * 🆕获取「要处理的新近任务」列表
     */
    public LinkedList<Task> getNovelTasks() {
        final LinkedList<Task> tasksToProcess = new LinkedList<>();
        // select a task from novelTasks
        // one of the two places where this variable is set
        final Task task = this.novelTasks.takeOut();
        if (task != null)
            tasksToProcess.add(task);
        return tasksToProcess;
    }

    /* ---------- task processing ---------- */
    /**
     * Immediate processing of a new task, in constant time Local processing, in
     * one concept only
     *
     * @param taskInput the task to be accepted (owned)
     */ // TODO: 待迁移
    private static boolean immediateProcess(final Memory self, final Task taskInput) {
        self.getRecorder().append("!!! Insert: " + taskInput + "\n");

        // * 🚩准备上下文
        final DerivationContextDirect context = prepareDirectProcessContext(self, taskInput);

        // * 🚩上下文准备完毕⇒开始
        if (context != null) {
            // * 🚩调整概念的预算值
            self.activateConcept(context.getCurrentConcept(), taskInput.getBudget());
            // * 🔥开始「直接处理」
            Concept.directProcess(context);
        }

        final boolean noResult = context.noResult();

        // * 🚩吸收并清空上下文
        self.absorbContext(context);
        return noResult;
    }

    // TODO: 待迁移
    private static boolean immediateProcess(final Memory self, final Iterable<Task> tasksToProcess) {
        boolean noResult = true;
        for (final Task task : tasksToProcess) {
            // final BudgetValue oldBudgetValue = task.getBudget().clone();
            final boolean noResultSingle = immediateProcess(self, task);
            if (!noResultSingle)
                noResult = false;
            // ! 📝处理之后预算值可能改变，不能让整个函数与`processNovelTask`合并
            // * ⚠️需要「边处理（修改预算）边加入『新近任务』」
            // if (!task.getBudget().equals(oldBudgetValue)) {
            // recorder.append("!!! Budget changed: " + task + "\n");
            // }
        }
        return noResult;
    }

    /**
     * 🆕准备「直接推理」的推理上下文
     * * 🚩这其中不对「推理上下文」「记忆区」外的变量进行任何修改
     * * 📌捕获`taskInput`的所有权
     *
     * @param taskInput
     * @return 直接推理上下文 / 空
     */ // TODO: 待迁移
    private static DerivationContextDirect prepareDirectProcessContext(final Memory self, final Task taskInput) {
        // * 🚩强制清空上下文防串
        final DerivationContextDirect context = new DerivationContextDirect(self);
        // * 🚩准备上下文
        // one of the two places where this variable is set
        context.setCurrentTask(taskInput);
        context.setCurrentConcept(self.getConceptOrCreate(taskInput.getContent()));
        if (context.getCurrentConcept() != null) {
            // * ✅【2024-05-20 08:52:34】↓不再需要：自始至终都是「当前概念」所对应的词项
            // context.setCurrentTerm(context.getCurrentConcept().getTerm());
            return context; // * 📌准备就绪
        }
        return null; // * 📌准备失败：没有可供推理的概念
    }

    /**
     * Select a concept to fire.
     */ // TODO: 待迁移
    private static void processConcept(
            final Iterable<TermLink> toReasonLinks,
            final DerivationContextReason context) {
        // * 🚩开始推理；【2024-05-17 17:50:05】此处代码分离仅为更好演示其逻辑
        // * 📝【2024-05-19 18:40:54】目前将这类「仅修改一个变量的推理」视作一组推理，共用一个上下文
        for (final TermLink termLink : toReasonLinks) {
            // * 🚩每次「概念推理」只更改「当前信念」与「当前信念链」
            final TermLink newBeliefLink = termLink;
            final Sentence newBelief;
            final Stamp newStamp;
            final Concept beliefConcept = context.getMemory().termToConcept(termLink.getTarget());
            if (beliefConcept != null) {
                newBelief = beliefConcept.getBelief(context.getCurrentTask()); // ! may be null
                if (newBelief != null) {
                    newStamp = Stamp.uncheckedMerge( // ! may be null
                            context.getCurrentTask().getSentence().getStamp(),
                            // * 📌此处的「时间戳」一定是「当前信念」的时间戳
                            // * 📄理由：最后返回的信念与「成功时比对的信念」一致（只隔着`clone`）
                            newBelief.getStamp(),
                            context.getTime());
                } else {
                    newStamp = null;
                }
            } else {
                newBelief = null;
                newStamp = null;
            }
            // * 🚩实际上就是「当前信念」「当前信念链」更改后的「新上下文」
            // this.context.currentBelief = newBelief;
            // this.context.currentBeliefLink = newBeliefLink;
            // this.context.newStamp = newStamp;
            context.switchToNewBelief(newBeliefLink, newBelief, newStamp);
            // * 🔥启动概念推理：点火！
            RuleTables.reason(context);
            context.getCurrentConcept().__putTermLinkBack(termLink);
        }
        context.getCurrentConcept().__putTaskLinkBack(context.getCurrentTaskLink());
        // * 🚩吸收并清空上下文
        context.getMemory().absorbContext(context);
    }

    /* ---------- main loop ---------- */

    /**
     * 🆕✨预点火
     * * 📝属于「直接推理」和「概念推理」的过渡部分
     * * 📌仍有「参与构建『推理上下文』」的作用
     * * 🚩在此开始为「概念推理」建立上下文
     * * 🎯从「记忆区」拿出「概念」并从其中拿出「任务链」：若都有，则进入「概念推理」阶段
     *
     * @return 预点火结果 {@link PreFireResult}
     */ // TODO: 待迁移
    private static Iterable<TermLink> preprocessConcept(
            final Memory self,
            final boolean noResult,
            final DerivationContextReason context) {
        // * 🚩推理前判断「是否有必要」
        if (!noResult) // necessary?
            return null;

        // * 🚩强制清空旧上下文 | 防止「概念推理后直接推理导致变量遗留」的情况
        context.clear();

        // * 🚩从「记忆区」拿出一个「概念」准备推理 | 源自`processConcept`

        // * 🚩拿出一个概念，准备点火
        context.setCurrentConcept(self.concepts.takeOut());
        if (context.getCurrentConcept() == null) {
            return null;
        }
        // * ✅【2024-05-20 08:52:34】↓不再需要：自始至终都是「当前概念」所对应的词项
        // context.setCurrentTerm(context.getCurrentConcept().getTerm());
        self.recorder.append(" * Selected Concept: " + context.getCurrentTerm() + "\n");
        self.concepts.putBack(context.getCurrentConcept()); // current Concept remains in the bag all the time
        // a working workCycle
        // * An atomic step in a concept, only called in {@link Memory#processConcept}
        // * 🚩预点火（实质上仍属于「直接推理」而非「概念推理」）

        // * 🚩从「概念」拿出一个「任务链」准备推理 | 源自`Concept.fire`
        final TaskLink currentTaskLink = context.getCurrentConcept().__takeOutTaskLink();
        if (currentTaskLink == null) {
            return null;
        }
        context.setCurrentTaskLink(currentTaskLink);
        // * 📝【2024-05-21 11:54:04】断言：直接推理不会涉及「词项链/信念链」
        // * ❓这里的「信念链」是否可空
        // * 📝此处应该是「重置信念链，以便后续拿取词项链做『概念推理』」
        self.getRecorder().append(" * Selected TaskLink: " + currentTaskLink + "\n");
        final Task task = currentTaskLink.getTargetTask();
        context.setCurrentTask(task); // one of the two places where this variable is set
        // self.getRecorder().append(" * Selected Task: " + task + "\n");
        // for debugging
        if (currentTaskLink.getType() == TermLink.TRANSFORM) {
            RuleTables.transformTask(currentTaskLink, context);
            // to turn this into structural inference as below?
            // ? ↑【2024-05-17 23:13:45】似乎该注释意味着「应该放在『概念推理』而非『直接推理』中」
            // ! 🚩放回并结束 | 虽然导致代码重复，但以此让`switch`不再必要
            context.getCurrentConcept().__putTaskLinkBack(currentTaskLink);
            return null;
        }

        // * 🚩终于要轮到「点火」：从选取的「任务链」获取要（分别）参与推理的「词项链」
        return chooseTermLinksToReason(
                self,
                context.getCurrentConcept(),
                currentTaskLink);
    }

    /**
     * 🆕围绕任务链，获取可推理的词项链列表
     *
     * @param currentTaskLink 当前任务链
     * @return 将要被拿去推理的词项链列表
     */ // TODO: 待迁移
    private static ArrayList<TermLink> chooseTermLinksToReason(Memory self, Concept concept,
            TaskLink currentTaskLink) {
        final ArrayList<TermLink> toReasonLinks = new ArrayList<>();
        int termLinkCount = Parameters.MAX_REASONED_TERM_LINK;
        // while (self.noResult() && (termLinkCount > 0)) {
        while (termLinkCount > 0) {
            final TermLink termLink = concept.__takeOutTermLink(currentTaskLink, self.getTime());
            if (termLink == null)
                break;
            self.getRecorder().append(" * Selected TermLink: " + termLink + "\n");
            toReasonLinks.add(termLink);
            termLinkCount--;
        }
        return toReasonLinks;
    }

    /**
     * Actually means that there are no new Tasks
     */
    public boolean noResult() {
        return newTasks.isEmpty();
    }

    /* ---------- display ---------- */
    /**
     * Start display active concepts on given bagObserver, called from MainWindow.
     *
     * we don't want to expose fields concepts and novelTasks, AND we want to
     * separate GUI and inference, so this method takes as argument a
     * {@link BagObserver} and calls
     * {@link ConceptBag#addBagObserver(BagObserver, String)} ;
     *
     * see design for {@link Bag} and {@link nars.gui.BagWindow}
     * in {@link Bag#addBagObserver(BagObserver, String)}
     *
     * @param bagObserver bag Observer that will receive notifications
     * @param title       the window title
     */
    public void conceptsStartPlay(BagObserver<Concept> bagObserver, String title) {
        bagObserver.setBag(concepts);
        concepts.addBagObserver(bagObserver, title);
    }

    /**
     * Display new tasks, called from MainWindow. see
     * {@link #conceptsStartPlay(BagObserver, String)}
     *
     * @param bagObserver
     * @param s           the window title
     */
    public void taskBuffersStartPlay(BagObserver<Task> bagObserver, String s) {
        bagObserver.setBag(novelTasks);
        novelTasks.addBagObserver(bagObserver, s);
    }

    @Override
    public String toString() {
        String result = toStringLongIfNotNull(concepts, "concepts")
                + toStringLongIfNotNull(novelTasks, "novelTasks")
                + toStringIfNotNull(newTasks, "newTasks");
        // ! ❌【2024-05-21 10:52:53】因为现在「推理上下文」仅为临时变量，故不再提供其信息
        // if (context != null) {
        // result += toStringLongIfNotNull(context.getCurrentTask(), "currentTask")
        // + toStringLongIfNotNull(context.getCurrentBeliefLink(), "currentBeliefLink")
        // + toStringIfNotNull(context.getCurrentBelief(), "currentBelief");
        // }
        return result;
    }

    private String toStringLongIfNotNull(Bag<?> item, String title) {
        return item == null ? ""
                : "\n " + title + ":\n"
                        + item.toStringLong();
    }

    // private String toStringLongIfNotNull(Item item, String title) {
    // return item == null ? ""
    // : "\n " + title + ":\n"
    // + item.toStringLong();
    // }

    private String toStringIfNotNull(Object item, String title) {
        return item == null ? ""
                : "\n " + title + ":\n"
                        + item.toString();
    }

    public AtomicInteger getTaskForgettingRate() {
        return taskForgettingRate;
    }

    public AtomicInteger getBeliefForgettingRate() {
        return beliefForgettingRate;
    }

    public AtomicInteger getConceptForgettingRate() {
        return conceptForgettingRate;
    }

    class NullInferenceRecorder implements IInferenceRecorder {

        @Override
        public void init() {
        }

        @Override
        public void show() {
        }

        @Override
        public void play() {
        }

        @Override
        public void stop() {
        }

        @Override
        public void append(String s) {
        }

        @Override
        public void openLogFile() {
        }

        @Override
        public void closeLogFile() {
        }

        @Override
        public boolean isLogging() {
            return false;
        }
    }
}
