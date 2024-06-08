package nars.entity;

/**
 * 🆕疑问句：「问题」类型
 */
public interface Question extends Sentence {
    // @Override
    // public default char getPunctuation() {
    // return QUESTION_MARK;
    // }

    /**
     * Get a String representation of the sentence for key of Task and TaskLink
     *
     * @return The String
     */
    @Override
    public default String toKey() {
        final StringBuilder s = new StringBuilder();
        s.append(this.getContent().toString());
        s.append(this.getPunctuation()).append(" ");
        return s.toString();
    }

    /**
     * Get a String representation of the sentence
     *
     * @return The String
     */
    @Override
    public default String sentenceToString() {
        StringBuilder s = new StringBuilder();
        s.append(this.getContent().toString());
        s.append(this.getPunctuation()).append(" ");
        s.append(this.stampToString());
        return s.toString();
    }
}
