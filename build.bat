@echo off
REM Very simple build script

rm -rf classes
mkdir classes
@REM javac -d classes -Xlint:unchecked nars_gui\src\main\java\*\*\*.java nars_core_java\src\main\java\*\*\*.java nars_core_java\*\*\*\*.java
@REM javac -d classes -Xlint:unchecked nars_gui/src/main/java/*.java nars_core_java/src/main/java/*/*/*.java nars_core_java/*/*/*/*.java
@REM 路径：
@REM * nars_gui/src/main/java/*.java
@REM * nars_core_java/src/main/java/*/*/*.java
@REM * nars_core_java/*/*/*/*.java
@REM javac -d classes -Xlint:unchecked nars_core_java\nars\main_nogui\*.java
@REM * 2024-04-19 11:25:38 构建参考：https://www.baeldung.com/javac-compile-classes-directory/

@REM ↓ 2024-04-19 11:30:34 现在又可以了：并不存在所谓「编译顺序」一说

@REM ↓使用 /A:-D 忽略目录
dir src /b /s /A:-D *.java > sources.txt
@REM ↑这是所有Java代码，可能编译顺序不能保证
@REM dir nars_gui /b /s /A:-D *.java > sources.txt
@REM dir nars_core_java /b /s /A:-D *.java >> sources.txt
@REM ↑上边两行按顺序将所有涉及到的.java文件添加到sources.txt中，
@REM ! 但有两个问题未解决：1 无法排除html，且部分文件重复 2 需要手动排除一些孤立（但影响依赖）的文件
@REM * nars_gui/src/main/java/*.java
@REM * nars_core_java/src/main/java/*/*/*.java
@REM * nars_core_java/*/*/*/*.java
@REM javac -classpath open-nars\src\lib\JSAP-2.1.jar -d classes @sources.txt -Xstdout compile.log
javac -classpath open-nars\src\lib\*.jar -d classes @sources.txt -Xstdout compile.log
@REM H:\A137442\Develop\AGI\NARS\OpenNARS-158\open-nars\tools\com\googlecode\opennars\tools\actuary\Actuary.java
@REM ! ↑这是个孤立文件（核心shell、GUI不依赖它），但会导致编译失败——故需在sources.txt忽略

@REM ==后续其它构建代码==
@REM 构建NARS代码（classes）到jar文件（GUI）
echo Main-Class: nars.main.NARS > manifest.txt
jar -cvfm opennars-158.jar manifest.txt -C classes .
DEL manifest.txt

@REM 构建NARS代码（classes）到jar文件（Shell）
echo Main-Class: nars.main_nogui.Shell > manifest.txt
jar -cvfm opennars-158-shell.jar manifest.txt -C classes .
DEL manifest.txt

echo You can now launch:
echo java -jar opennars-158.jar
echo or
echo java -jar opennars-158.jar nars-dist/Examples/Example-NAL1-edited.txt --silence 90
echo or
echo java -jar opennars-158-shell.jar
@REM echo or
@REM ↓这条无法奏效
@REM echo java -jar opennars-158-shell.jar nars-dist/Examples/Example-NAL1-edited.txt
