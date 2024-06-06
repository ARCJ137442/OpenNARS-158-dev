package test;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedList;

import nars.main_nogui.SimpleShell;

/**
 * 🆕测试用
 *
 * @author tc, ARCJ137442
 */
public class TestCommon {

    public TestCommon(String testLines) {
        this(testLines.split("\n"));
    }

    public TestCommon(String[] testLines) {
        // * 🚩复用「简单终端」但将输入通道更改
        final SimpleShell shell = new SimpleShell(System.out);
        shell.setIOChannel(
                new TestInput(shell, testLines),
                new SimpleShell.ShellOutput(System.out)).main();
    }

    /**
     * 🎯安全把ArrayList<T>转换为T[]
     * * ⚠️因为ArrayList.toArray没用（只能变成Object[]，并且没法强转）
     */
    public static String[] arrayListToArray(ArrayList<String> list) {
        String[] arr = new String[list.size()];
        for (int i = 0; i < list.size(); i++) {
            arr[i] = list.get(i);
        }
        list.clear();
        return arr;
    }

    /** 尝试从命令行参数中获取合法数字 */
    public static int getN(final String[] args, int defaultValue) {
        for (String probablyNum : args) {
            try {
                return Integer.parseInt(probablyNum);
            } catch (NumberFormatException e) {
                continue;
            }
        }
        return defaultValue;
    }

    /**
     * 终端输入通道
     * * 🚩【2024-05-21 21:02:14】经过一定的特别修改，只对推理器输入指定文本
     */
    public static final class TestInput extends SimpleShell.ShellInput {
        private final LinkedList<String> bufIn;

        public TestInput(final SimpleShell shell, String[] lines) {
            // * 🚩不使用SimpleShell的`bufIn`变量
            super(shell, InputStream.nullInputStream());
            this.bufIn = new LinkedList<>();
            for (final String line : lines) {
                bufIn.add(line);
            }
        }

        @Override
        public boolean nextInput() {
            try {
                final String line = bufIn.poll();
                if (line != null && !line.isEmpty())
                    inputLine(line);
            } catch (final Exception e) {
                throw new IllegalStateException("Could not read line.", e);
            }
            return true;
        }
    }
}
