package test;

import java.util.ArrayList;

/**
 * 🆕长链分离测试
 * * 🎯测试「信度为1的全保真分离链」
 * * 📄<A ==> B>, <B ==> C>, A |- C
 *
 * @author tc, ARCJ137442
 */
public class LongChainDetachment extends TestCommon {

    public static void main(final String[] args) {
        new LongChainDetachment(args);
    }

    public LongChainDetachment(final String[] args) {
        super(testLines(getN(args, 25), 5));
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
        // * 🚩追加「分离」规则
        for (i = 1; i <= n; i++) {
            final String dedJudgement = "<A" + i + " ==> A" + (i + 1) + ">. %1;1%\n";
            testLines.add(dedJudgement);
        }
        // * 🚩批量提问
        for (i = 0; i < n; i++) {
            if (i % qPeriod == qPeriod - 1) {
                testLines.add("A" + i + "?");
            }
        }
        testLines.add("A" + n + "?");
        // * 🚩推理起点
        testLines.add("A1. %1;1%");
        return arrayListToArray(testLines);
    }
}
