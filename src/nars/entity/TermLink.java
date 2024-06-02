package nars.entity;

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
public class TermLink extends TLink<Term> implements Item {

    /**
     * 🆕Item令牌
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
     * Constructor to make actual TermLink from a template
     * <p>
     * called in Concept.buildTermLinks only
     * * 🚩【2024-05-30 20:31:47】现在直接调用超类的「完全构造函数」
     *
     * @param target   Target Term
     * @param template TermLink template previously prepared
     * @param budget   Budget value of the link
     */
    public TermLink(final Term target, final TLink<Term> template, final BudgetValue budget) {
        this(
                target,
                budget,
                generateTypeFromTemplate(target, template),
                template.getIndices());
    }

    /**
     * 🆕从「模板」中确定好「类型」与「索引」后，再进一步确定「键」
     */
    private TermLink(final Term target, final BudgetValue budget, final short type, final short[] indices) {
        super(target, type, indices);
        this.token = new Token(generateKey(target, type, indices), budget);
    }

    /**
     * 从「目标」、已生成的「类型」「索引」生成「键」
     *
     * @param target
     * @param type
     * @param indices
     * @return
     */
    private static final String generateKey(final Term target, final short type, final short[] indices) {
        String key = TLink.generateKey(type, indices);
        if (target != null) {
            key += target;
        }
        return key;
    }

    /**
     * 🆕从「目标」与「模板」中产生链接类型
     *
     * @param <Target>
     * @param t
     * @param template
     * @return
     */
    private static short generateTypeFromTemplate(final Term t, final TLink<Term> template) {
        short type = template.getType();
        if (template.getTarget().equals(t)) {
            type--; // point to component
        }
        return type;
    }
}
