package nars.entity;

import nars.language.Term;
import nars.main_nogui.Parameters;

/**
 * Reference to a Task.
 * <p>
 * The reason to separate a Task and a TaskLink is that the same Task can be
 * linked from multiple Concepts, with different BudgetValue.
 */
public class TaskLink extends TLink<Task> implements Item {

    /**
     * 🆕Item令牌
     *
     * * ️📝可空性：非空
     * * 📝可变性：可变 | 需要在「预算值」中被修改
     * * 📝所有权：具所有权
     */
    private final Token token;

    @Override
    public String getKey() {
        return token.getKey();
    }

    @Override
    public BudgetValue getBudget() {
        return token.getBudget();
    }

    /**
     * Remember the TermLinks that has been used recently with this TaskLink
     * * 📌记忆【曾经匹配过的词项链】的key
     * * 🎯用于推理中判断{@link TaskLink#novel}「是否新近」
     *
     * * ️📝可空性：非空
     * * 📝可变性：可变 | 内部可变
     * * 📝所有权：具所有权
     */
    private final String recordedLinks[];
    /**
     * Remember the time when each TermLink is used with this TaskLink
     * * 📌记忆【曾经匹配过的词项链】的时间（序列号）
     * * 🎯用于推理中判断{@link TaskLink#novel}「是否新近」
     *
     * * ️📝可空性：非空
     * * 📝可变性：可变 | 内部可变
     * * 📝所有权：具所有权
     */
    private final long recordingTime[];
    /**
     * The number of TermLinks remembered
     * * 📌记忆【曾经匹配过的词项链】的个数
     * * 🎯用于推理中判断{@link TaskLink#novel}「是否新近」
     *
     * * ️📝可空性：非空
     * * 📝可变性：可变
     * * 📝所有权：具所有权
     */
    private int counter;

    /**
     * 🆕完全构造函数
     *
     * @param t
     * @param template
     * @param v
     */
    private TaskLink(final Task target, final BudgetValue budget, final short type, final short[] indices) {
        super(target, type, indices);
        this.token = new Token(generateKey(target, type, indices), budget);
        this.recordedLinks = new String[Parameters.TERM_LINK_RECORD_LENGTH];
        this.recordingTime = new long[Parameters.TERM_LINK_RECORD_LENGTH];
        this.counter = 0;
    }

    /**
     * Constructor
     * <p>
     * only called in Memory.continuedProcess
     * * 📝【2024-05-30 00:46:38】只在「链接概念到任务」中使用
     *
     * @param target   The target Task
     * @param template The TermLink template
     * @param budget   The budget
     */
    public TaskLink(final Task target, final TLink<Term> template, final BudgetValue budget) {
        this(target, budget,
                template.getType(), template.getIndices());
    }

    /**
     * 🆕专用于创建「自身」链接
     * * 🎯用于推理中识别并分派
     * * 🚩使用「SELF」类型，并使用空数组
     *
     * @param target
     * @param budget
     * @return
     */
    public static final TaskLink newSelf(final Task target, final BudgetValue budget) {
        return new TaskLink(
                target, budget,
                TermLink.SELF, new short[] {}); // * 🚩必须非空，即便使用空数组
    }

    private static final String generateKey(final Task target, final short type, final short[] indices) {
        // * 🚩生成最基础的
        String key = generateKey(type, indices); // as defined in TermLink
        if (target != null)
            key += target.getContent();
        key += target.getKey();
        return key;
    }

    /**
     * To check whether a TaskLink should use a TermLink, return false if they
     * interacted recently
     * <p>
     * called in TermLinkBag only
     * * 🎯用于从「新近任务袋」中获取「新近任务」：根据「新近」调配优先级
     * * 📝在「概念推理」的「准备待推理词项链」的过程中用到
     * * 🔗ProcessReason.chooseTermLinksToReason
     *
     * @param termLink    The TermLink to be checked
     * @param currentTime The current time
     * @return Whether they are novel to each other
     */
    public boolean novel(final TermLink termLink, final long currentTime) {
        final Term bTerm = termLink.getTarget();
        // * 🚩重复目标⇒非新近
        if (bTerm.equals(this.target.getContent()))
            return false;
        // * 🚩检查所有已被记录的词项链
        final String linkKey = termLink.getKey();
        for (int i = 0; i < counter; i++) {
            final int existedI = i % Parameters.TERM_LINK_RECORD_LENGTH;
            // * 🚩重复key⇒检查时间
            if (linkKey.equals(recordedLinks[existedI])) {
                if (currentTime < recordingTime[existedI] + Parameters.TERM_LINK_RECORD_LENGTH) {
                    return false;
                } else {
                    recordingTime[existedI] = currentTime;
                    return true;
                }
            }
        }
        // * 📝此处`i`必定为`counter`
        // * 🚩没检查到已有的：记录新匹配的词项链 | ️📝有可能覆盖
        final int next = counter % Parameters.TERM_LINK_RECORD_LENGTH;
        recordedLinks[next] = linkKey; // add knowledge reference to recordedLinks
        recordingTime[next] = currentTime;
        if (counter < Parameters.TERM_LINK_RECORD_LENGTH) { // keep a constant length
            counter++; // * 💭只增不减？似乎会导致「信念固化」（or 始终覆盖最新的，旧的得不到修改）
        }
        return true;
    }

    @Override
    public String toString() {
        final String superString = getBudget().toString() + " " + getKey().toString();
        return superString + " " + getTarget().getStamp();
    }
}
