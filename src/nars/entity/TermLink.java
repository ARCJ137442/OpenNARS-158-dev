package nars.entity;

import nars.inference.Budget;
import nars.io.ToStringBriefAndLong;
import nars.language.Term;

/**
 * A link between a compound term and a component term
 * <p>
 * A TermLink links the current Term to a target Term, which is
 * either a component of, or compound made from, the current term.
 * <p>
 * Neither of the two terms contain variable shared with other terms.
 * <p>
 * The index value(s) indicates the location of the component in the compound.
 * <p>
 * This class is mainly used in inference.RuleTable to dispatch premises to
 * inference rules
 */
public class TermLink implements TLink<Term>, Item, ToStringBriefAndLong {

    // struct TermLink

    /**
     * 🆕纯粹的T链接类型
     *
     * * ️📝可空性：非空
     * * 📝可变性：不可变
     * * 📝所有权：具所有权
     */
    private final TLinkage<Term> inner;

    /**
     * 🆕Item令牌
     *
     * * ️📝可空性：非空
     * * 📝可变性：可变 | 需要在「预算值」中被修改
     * * 📝所有权：具所有权
     */
    private final Token token;

    // impl Budget for TermLink

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

    // impl Item for TermLink

    @Override
    public String getKey() {
        return token.getKey();
    }

    // impl TLink<Term> for TermLink

    @Override
    public final Term getTarget() {
        return this.inner.getTarget();
    }

    @Override
    public final TLinkType getType() {
        return this.inner.getType();
    }

    @Override
    public final short[] getIndices() {
        return this.inner.getIndices();
    }

    /**
     * Constructor to make actual TermLink from a template
     * <p>
     * called in Concept.buildTermLinks only
     * * 🚩【2024-05-30 20:31:47】现在直接调用超类的「完全构造函数」
     * * 📌经断言，必定是「从整体链接到子项」
     * * 🚩【2024-06-04 20:28:15】目前仅此一处公开构造函数
     *
     * @param target   Target Term
     * @param template TermLink template previously prepared
     * @param budget   Budget value of the link
     */
    public static final TermLink fromTemplate(
            final Term target,
            final TermLinkTemplate template,
            final Budget budget) {
        // * 🚩生成类型与索引
        final TLinkType type = generateTypeFromTemplate(target, template);
        final short[] indices = template.getIndices();
        // * 🚩构造
        return new TermLink(target, budget, type, indices);
    }

    /**
     * 🆕从「模板」中确定好「类型」与「索引」后，再进一步确定「键」
     * * 📌完全参数构造函数
     */
    private TermLink(final Term target, final Budget budget, final TLinkType type, final short[] indices) {
        // * 🚩构造
        this.inner = new TLinkage<Term>(target, type, indices);
        // * 🚩生成令牌 | 从抽象的「预算」到具体的「预算值」
        final String key = generateKey(target, type, indices);
        this.token = new Token(key, budget);
    }

    /**
     * 从「目标」、已生成的「类型」「索引」生成「键」
     *
     * @param target
     * @param type
     * @param indices
     * @return
     */
    private static final String generateKey(final Term target, final TLinkType type, final short[] indices) {
        // * 🚩先生成标准T链接子串
        String key = TLink.generateKey(type, indices);
        // * 🚩此处假定「目标」不为空
        if (target == null)
            throw new AssertionError("target is null");
        key += target;
        return key;
    }

    /**
     * 🆕从「目标」与「模板」中产生链接类型
     * * 📝可能在构建「自身→元素」时，也可在构建「元素→自身」时
     *
     * @param <Target>
     * @param target
     * @param template
     * @return
     */
    private static final TLinkType generateTypeFromTemplate(final Term target, final TermLinkTemplate template) {
        final TLinkType templateType = template.getType();
        // * 🚩断言此时「链接模板」的链接类型
        if (!templateType.isToCompound())
            throw new AssertionError("模板必定是「从元素链接到整体」");
        // * 🚩开始计算类型
        final TLinkType result;
        if (template.willFromSelfTo().equals(target))
            // * 🚩自「元素→整体」来（复合词项的「模板链接」指向自身）
            // * 🚩到「整体→元素」去
            // * 📄【2024-06-04 20:35:22】
            // * Concept@48 "<{tim} --> (/,livingIn,_,{graz})>" ~> target="{tim}"
            // * + template: willFromSelfTo="{tim}"
            // * 📄【2024-06-04 20:35:32】
            // * Concept@52 "<{tim} --> (/,livingIn,_,{graz})>" ~> target="tim"
            // * + template: willFromSelfTo="tim"
            result = templateType.tryPointToComponent(); // point to component
        else
            result = templateType;
        // * 🚩到此处可能是「元素→整体」也可能是「整体→元素」
        return result;
    }

    // impl ToStringBriefAndLong for TermLink

    /**
     * Return a String representation of the Item
     *
     * @return The String representation of the full content
     */
    @Override
    public String toString() {
        return this.token.getBudgetValue().toString() + " " + getKey();
    }

    /**
     * Return a String representation of the Item after simplification
     *
     * @return A simplified String representation of the content
     */
    @Override
    public String toStringBrief() {
        return this.token.getBudgetValue().toStringBrief() + " " + getKey();
    }

    @Override
    public String toStringLong() {
        return toString();
    }
}
