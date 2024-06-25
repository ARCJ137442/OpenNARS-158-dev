package nars.control;

/**
 * Collected system parameters. To be modified before compiling.
 */
public class Parameters {

    /* ---------- initial values of run-time adjustable parameters ---------- */
    /** Concept decay rate in ConceptBag, in [1, 99]. */
    public static final int CONCEPT_FORGETTING_CYCLE = 10;
    /** TaskLink decay rate in TaskLinkBag, in [1, 99]. */
    public static final int TASK_LINK_FORGETTING_CYCLE = 20;
    /** TermLink decay rate in TermLinkBag, in [1, 99]. */
    public static final int TERM_LINK_FORGETTING_CYCLE = 50;
    /** Silent threshold for task reporting, in [0, 100]. */
    public static final int SILENT_LEVEL = 0;

    /* ---------- time management ---------- */
    /** Task decay rate in TaskBuffer, in [1, 99]. */
    public static final int NEW_TASK_FORGETTING_CYCLE = 1;
    /** Maximum TermLinks checked for novelty for each TaskLink in TermLinkBag */
    public static final int MAX_MATCHED_TERM_LINK = 10;
    /** Maximum TermLinks used in reasoning for each Task in Concept */
    public static final int MAX_REASONED_TERM_LINK = 3;

    /* ---------- logical parameters ---------- */
    /** Evidential Horizon, the amount of future evidence to be considered. */
    public static final int HORIZON = 1; // or 2, can be float
    /** Reliance factor, the empirical confidence of analytical truth. */
    public static final float RELIANCE = 0.9f; // the same as default confidence

    /* ---------- budget thresholds ---------- */
    /** The budget threshold rate for task to be accepted. */
    public static final float BUDGET_THRESHOLD = 0.01f;

    /* ---------- default input values ---------- */
    /** Default expectation for confirmation. */
    public static final float DEFAULT_CONFIRMATION_EXPECTATION = 0.8f;
    /** Default expectation for confirmation. */
    public static final float DEFAULT_CREATION_EXPECTATION = 0.66f;
    /** Default confidence of input judgment. */
    public static final float DEFAULT_JUDGMENT_CONFIDENCE = 0.9f;
    /** Default priority of input judgment */
    public static final float DEFAULT_JUDGMENT_PRIORITY = 0.8f;
    /** Default durability of input judgment */
    public static final float DEFAULT_JUDGMENT_DURABILITY = 0.8f;
    /** Default priority of input question */
    public static final float DEFAULT_QUESTION_PRIORITY = 0.9f;
    /** Default durability of input question */
    public static final float DEFAULT_QUESTION_DURABILITY = 0.9f;

    /* ---------- space management ---------- */
    /** Level granularity in Bag, two digits */
    public static final int BAG_LEVEL = 100;
    /**
     * Level separation in Bag, one digit, for display (run-time adjustable) and
     * management (fixed)
     */
    public static final int BAG_THRESHOLD = 10;
    /** Hashtable load factor in Bag */
    public static final float LOAD_FACTOR = 0.5f;
    /** Size of ConceptBag */
    public static final int CONCEPT_BAG_SIZE = 10000;
    /** Size of TaskLinkBag */
    public static final int TASK_LINK_BAG_SIZE = 20;
    /** Size of TermLinkBag */
    public static final int TERM_LINK_BAG_SIZE = 20;
    /** Size of TaskBuffer */
    public static final int TASK_BUFFER_SIZE = 20;
    /** 🆕Initial priority of a new Concept */
    public static final float CONCEPT_INITIAL_PRIORITY = 0.01f;
    /** 🆕Initial durability of a new Concept */
    public static final float CONCEPT_INITIAL_DURABILITY = 0.01f;
    /** 🆕Initial quality of a new Concept */
    public static final float CONCEPT_INITIAL_QUALITY = 0.01f;

    /* ---------- avoiding repeated reasoning ---------- */
    /** Maximum length of Stamp, a power of 2 */
    public static final int MAXIMUM_STAMP_LENGTH = 8;
    /** Remember recently used TermLink on a Task */
    public static final int TERM_LINK_RECORD_LENGTH = 10;
    /** Maximum number of beliefs kept in a Concept */
    public static final int MAXIMUM_BELIEF_LENGTH = 7;
    /** Maximum number of goals kept in a Concept */
    public static final int MAXIMUM_QUESTIONS_LENGTH = 5;
}
