package nars.control;

import java.util.LinkedList;

import nars.entity.*;
import nars.storage.*;

public abstract class ProcessDirect {

    /**
     * 🆕本地直接推理
     * * 🚩最终只和「本地规则」与{@link Concept#directProcess}有关
     */ // TODO: 待迁移
    public static boolean processDirect(final Memory self) {
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
}
