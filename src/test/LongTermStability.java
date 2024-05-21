package test;

import java.io.InputStream;
import java.util.LinkedList;

import nars.main_nogui.SimpleShell;

/**
 * 🆕一个更简单的交互终端
 * * 📌单线程，仅输入输出
 *
 * @author tc, ARCJ137442
 */
public class LongTermStability {

    private static final String[] TEST_LINES = "<{tim} --> (/,livingIn,_,{graz})>. %0%\n100\n<<(*,$1,sunglasses) --> own> ==> <$1 --> [aggressive]>>.\n<(*,{tom},sunglasses) --> own>.\n<<$1 --> [aggressive]> ==> <$1 --> murder>>.\n<<$1 --> (/,livingIn,_,{graz})> ==> <$1 --> murder>>.\n<{?who} --> murder>?\n<{tim} --> (/,livingIn,_,{graz})>.\n<{tim} --> (/,livingIn,_,{graz})>. %0%\n100\n<<(*,$1,sunglasses) --> own> ==> <$1 --> [aggressive]>>.\n<(*,{tom},(&,[black],glasses)) --> own>.\n<<$1 --> [aggressive]> ==> <$1 --> murder>>.\n<<$1 --> (/,livingIn,_,{graz})> ==> <$1 --> murder>>.\n<sunglasses --> (&,[black],glasses)>.\n<{?who} --> murder>?\n<(*,toothbrush,plastic) --> made_of>.\n<(&/,<(*,$1,plastic) --> made_of>,(^lighter,{SELF},$1)) =/> <$1 --> [heated]>>.\n<<$1 --> [heated]> =/> <$1 --> [melted]>>.\n<<$1 --> [melted]> <|> <$1 --> [pliable]>>.\n<(&/,<$1 --> [pliable]>,(^reshape,{SELF},$1)) =/> <$1 --> [hardened]>>.\n<<$1 --> [hardened]> =|> <$1 --> [unscrewing]>>.\n<toothbrush --> object>.\n(&&,<#1 --> object>,<#1 --> [unscrewing]>)!\n<{SELF} --> [hurt]>! %0%\n<{SELF} --> [hurt]>. :|: %0%\n<(&/,<(*,{SELF},wolf) --> close_to>,+1000) =/> <{SELF} --> [hurt]>>.\n<(*,{SELF},wolf) --> close_to>. :|:\n<(&|,(^want,{SELF},$1,FALSE),(^anticipate,{SELF},$1)) =|> <(*,{SELF},$1) --> afraid_of>>.\n<(*,{SELF},?what) --> afraid_of>?\n<a --> A>. :|: %1.00;0.90%\n8\n<b --> B>. :|: %1.00;0.90%\n8\n<c --> C>. :|: %1.00;0.90%\n8\n<a --> A>. :|: %1.00;0.90%\n100\n<b --> B>. :|: %1.00;0.90%\n100\n<?1 =/> <c --> C>>?\n<(*,cup,plastic) --> made_of>.\n<cup --> object>.\n<cup --> [bendable]>.\n<toothbrush --> [bendable]>.\n<toothbrush --> object>.\n<(&/,<(*,$1,plastic) --> made_of>,(^lighter,{SELF},$1)) =/> <$1 --> [heated]>>.\n<<$1 --> [heated]> =/> <$1 --> [melted]>>.\n<<$1 --> [melted]> <|> <$1 --> [pliable]>>.\n<(&/,<$1 --> [pliable]>,(^reshape,{SELF},$1)) =/> <$1 --> [hardened]>>.\n<<$1 --> [hardened]> =|> <$1 --> [unscrewing]>>.\n(&&,<#1 --> object>,<#1 --> [unscrewing]>)!\n2000000"
            .split("\n");

    public static void main(final String[] args) {
        // * 🚩复用「简单终端」但将输入通道更改
        final SimpleShell shell = new SimpleShell(System.out);
        shell.setIOChannel(
                new TestInput(shell, TEST_LINES),
                new SimpleShell.ShellOutput(System.out)).main();
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
