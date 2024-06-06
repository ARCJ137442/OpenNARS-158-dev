package test;

import java.util.ArrayList;

/**
 * 🆕长链演绎测试
 * * 🎯测试「信度为1的全保真演绎链」
 * * 📄<A --> B>, <B --> C> |- <A --> C>
 *
 * @author tc, ARCJ137442
 */
public class LongChainDeduction extends TestCommon {

    public static void main(final String[] args) {
        new LongChainDeduction(args);
    }

    public LongChainDeduction(final String[] args) {
        super(testLines(getN(args, 20), 5));
    }

    /**
     * 🎯生成测试用例
     *
     * @param n 测试用例长度
     * @return
     */
    public static String[] testLines(final int n, final int qPeriod) {
        final ArrayList<String> testLines = new ArrayList<>();
        int i;
        // * 🚩添加判断
        for (i = 0; i < n; i++) {
            final String dedJudgement = "<A" + (i + 1) + " --> A" + (i + 2) + ">. %1;1%\n";
            testLines.add(dedJudgement);
        }
        // * 🚩添加问题
        for (i = qPeriod - 1; i < n; i += qPeriod)
            testLines.add("<A" + 1 + " --> A" + (i + 2) + ">?");
        testLines.add("<A1 --> A2>?");
        testLines.add("<A" + 1 + " --> A" + (i + 1) + ">?");
        // * 🚩整理转换
        return arrayListToArray(testLines);
    }
}
