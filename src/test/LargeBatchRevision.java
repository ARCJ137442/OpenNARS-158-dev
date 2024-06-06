package test;

import java.util.ArrayList;

/**
 * 🆕大批量修正
 * * 🎯测试「谎言说一百遍就会成为真理」，弱结论在修正后成为强信念
 * * 📄<A --> B> |- <B --> A>
 *
 * @author tc, ARCJ137442
 */
public class LargeBatchRevision extends TestCommon {

    public static void main(final String[] args) {
        new LargeBatchRevision(args);
    }

    public LargeBatchRevision(final String[] args) {
        super(testLines(getN(args, 300)));
    }

    /**
     * 🎯生成测试用例
     *
     * @param n 测试用例长度
     * @return
     */
    public static String[] testLines(final int n) {
        final ArrayList<String> testLines = new ArrayList<>();
        int i;
        // * 🚩重复添加弱推理前提
        // testLines.add("*volume=100");
        for (i = 1; i <= n; i++) {
            final String judgement = "<A --> B>. %1.0;0.2%";
            testLines.add(judgement);
        }
        // * 🚩提问「只有在弱推理后才成立的结论」
        testLines.add("<A --> B>?");
        testLines.add("<B --> A>?");
        testLines.add("10");
        for (i = 1; i <= n; i++) {
            final String judgement = "<A --> B>. %0.0;0.2%";
            testLines.add(judgement);
        }
        testLines.add("<A --> B>?");
        testLines.add("<B --> A>?");
        return arrayListToArray(testLines);
    }
}
