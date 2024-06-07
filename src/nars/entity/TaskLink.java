package nars.entity;

import nars.inference.Budget;
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
    public ShortFloat __priority() {
        return this.token.__priority();
    }

    @Override
    public ShortFloat __durability() {
        return this.token.__durability();
    }

    @Override
    public ShortFloat __quality() {
        return this.token.__quality();
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
    private int nRecordedTermLinks;

    /**
     * 🆕统一收归的「任务链记录长度」
     */
    private static final int RECORD_LENGTH = Parameters.TERM_LINK_RECORD_LENGTH;

    /**
     * 🆕完全构造函数
     *
     * @param t
     * @param template
     * @param v
     */
    private TaskLink(
            final Task target,
            final Budget budget,
            final TLinkType type,
            final short[] indices,
            final int recordLength) {
        super(target, type, indices);
        final String key = generateKey(target, type, indices);
        this.token = new Token(key, budget);
        this.recordedLinks = new String[recordLength];
        this.recordingTime = new long[recordLength];
        this.nRecordedTermLinks = 0;
    }

    /** 🆕传递「链接记录长度」的默认值 */
    private TaskLink(
            final Task target,
            final Budget budget,
            final TLinkType type,
            final short[] indices) {
        this(target, budget, type, indices, RECORD_LENGTH);
    }

    /**
     * Constructor
     * <p>
     * only called in Memory.continuedProcess
     * * 🚩【2024-06-05 01:05:16】唯二的公开构造函数（入口），基于「词项链模板」构造
     * * 📝【2024-05-30 00:46:38】只在「链接概念到任务」中使用
     *
     * @param target   The target Task
     * @param template The TermLink template
     * @param budget   The budget
     */
    public static final TaskLink fromTemplate(
            final Task target,
            final TermLinkTemplate template,
            final Budget budget) {
        return new TaskLink(target, budget, template.getType(), template.getIndices());
    }

    /**
     * 🆕专用于创建「自身」链接
     * * 📝仅在「链接到任务」时被构造一次
     * * 🎯用于推理中识别并分派
     * * 🚩使用「SELF」类型，并使用空数组
     *
     * @param target
     * @param budget
     * @return
     */
    public static final TaskLink newSelf(final Task target) {
        return new TaskLink(
                target, new BudgetValue(target), // * 🚩此处将抽象的「预算」转换为具体的「预算值」 | 目前只会取「任务」自身的预算值
                TLinkType.SELF, new short[] {}); // * 🚩必须非空，即便使用空数组
    }

    private static final String generateKey(final Task target, final TLinkType type, final short[] indices) {
        // * 🚩生成最基础的
        String key = generateKey(type, indices); // as defined in TermLink
        // if (target != null) // ! 🚩【2024-06-05 01:06:21】此处「目标」绝对非空
        // key += target.getContent(); // * ✅target.getKey()已经存在词项，无需重复生成
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
        for (int i = 0; i < nRecordedTermLinks; i++) {
            final int existedI = i % recordedLinks.length;
            // * 🚩重复key⇒检查时间
            if (linkKey.equals(recordedLinks[existedI])) {
                // * 🚩并未足够「滞后」⇒非新近 | 💭或许是一种「短期记忆」的表示
                if (currentTime < recordingTime[existedI] + recordedLinks.length) {
                    return false;
                }
                // * 🚩足够「滞后」⇒更新时间，判定为「新近」
                else {
                    recordingTime[existedI] = currentTime;
                    return true;
                }
            }
        }
        // * 📝此处`i`必定为`counter`
        // * 🚩没检查到已有的：记录新匹配的词项链 | ️📝有可能覆盖
        final int next = nRecordedTermLinks % recordedLinks.length;
        recordedLinks[next] = linkKey; // add knowledge reference to recordedLinks
        recordingTime[next] = currentTime;
        if (nRecordedTermLinks < recordedLinks.length) { // keep a constant length
            nRecordedTermLinks++; // * 💭只增不减？似乎会导致「信念固化」（or 始终覆盖最新的，旧的得不到修改）
        }
        return true;
    }

    @Override
    public String toString() {
        final String superString = this.token.getBudgetValue().toString() + " " + getKey().toString();
        return superString + " " + getTarget().getStamp();
    }

    // 📌自原`abstract class Item`中继承而来 //

    /**
     * Return a String representation of the Item after simplification
     *
     * @return A simplified String representation of the content
     */
    public String toStringBrief() {
        return token.getBudgetValue().toStringBrief() + " " + getKey();
    }

    public String toStringLong() {
        return toString();
    }
}
