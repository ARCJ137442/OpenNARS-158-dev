package test;

/**
 * 🎯🆕复刻OpenNARS `long_term_stability.nal`测试
 *
 * @author tc, ARCJ137442
 */
public class LongTermStability {

    private static final String[] TEST_LINES = "<{tim} --> (/,livingIn,_,{graz})>. %0%\n100\n<<(*,$1,sunglasses) --> own> ==> <$1 --> [aggressive]>>.\n<(*,{tom},sunglasses) --> own>.\n<<$1 --> [aggressive]> ==> <$1 --> murder>>.\n<<$1 --> (/,livingIn,_,{graz})> ==> <$1 --> murder>>.\n<{?who} --> murder>?\n<{tim} --> (/,livingIn,_,{graz})>.\n<{tim} --> (/,livingIn,_,{graz})>. %0%\n100\n<<(*,$1,sunglasses) --> own> ==> <$1 --> [aggressive]>>.\n<(*,{tom},(&,[black],glasses)) --> own>.\n<<$1 --> [aggressive]> ==> <$1 --> murder>>.\n<<$1 --> (/,livingIn,_,{graz})> ==> <$1 --> murder>>.\n<sunglasses --> (&,[black],glasses)>.\n<{?who} --> murder>?\n<(*,toothbrush,plastic) --> made_of>.\n<(&/,<(*,$1,plastic) --> made_of>,(^lighter,{SELF},$1)) =/> <$1 --> [heated]>>.\n<<$1 --> [heated]> =/> <$1 --> [melted]>>.\n<<$1 --> [melted]> <|> <$1 --> [pliable]>>.\n<(&/,<$1 --> [pliable]>,(^reshape,{SELF},$1)) =/> <$1 --> [hardened]>>.\n<<$1 --> [hardened]> =|> <$1 --> [unscrewing]>>.\n<toothbrush --> object>.\n(&&,<#1 --> object>,<#1 --> [unscrewing]>)!\n<{SELF} --> [hurt]>! %0%\n<{SELF} --> [hurt]>. :|: %0%\n<(&/,<(*,{SELF},wolf) --> close_to>,+1000) =/> <{SELF} --> [hurt]>>.\n<(*,{SELF},wolf) --> close_to>. :|:\n<(&|,(^want,{SELF},$1,FALSE),(^anticipate,{SELF},$1)) =|> <(*,{SELF},$1) --> afraid_of>>.\n<(*,{SELF},?what) --> afraid_of>?\n<a --> A>. :|: %1.00;0.90%\n8\n<b --> B>. :|: %1.00;0.90%\n8\n<c --> C>. :|: %1.00;0.90%\n8\n<a --> A>. :|: %1.00;0.90%\n100\n<b --> B>. :|: %1.00;0.90%\n100\n<?1 =/> <c --> C>>?\n<(*,cup,plastic) --> made_of>.\n<cup --> object>.\n<cup --> [bendable]>.\n<toothbrush --> [bendable]>.\n<toothbrush --> object>.\n<(&/,<(*,$1,plastic) --> made_of>,(^lighter,{SELF},$1)) =/> <$1 --> [heated]>>.\n<<$1 --> [heated]> =/> <$1 --> [melted]>>.\n<<$1 --> [melted]> <|> <$1 --> [pliable]>>.\n<(&/,<$1 --> [pliable]>,(^reshape,{SELF},$1)) =/> <$1 --> [hardened]>>.\n<<$1 --> [hardened]> =|> <$1 --> [unscrewing]>>.\n(&&,<#1 --> object>,<#1 --> [unscrewing]>)!\n2000000"
            .split("\n");

    public static void main(final String[] args) {
        new TestCommon(TEST_LINES);
    }
}
