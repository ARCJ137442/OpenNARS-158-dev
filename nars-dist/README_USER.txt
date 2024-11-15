
You can launch NARS by clicking the icon of NARS.jar, or in several ways from command window (shell):

- empty reasoner:
java -jar NARS.jar

- reasoner loaded from an experience file:
java -jar NARS.jar Examples/Example-NAL1-edited.txt --silence 90

- reasoner in batch:
java -cp NARS.jar nars.main.NARSBatch Examples/Example-NAL1-edited.txt

Example experience files are in directory Examples.
- "Example-NALn-*.txt" contains single step examples for most of the inference rules defined in NAL level n. The "edited" version contains English translations, and with the unrelated information removed; the "unedited" version contains the actual input/output data recorded by the "Save Experience" function of the GUI. The files can be loaded using the "Load Experience" function of the GUI.
- "Example-MultiStep-edited" contains multi-step inference examples described in http://code.google.com/p/open-nars/wiki/MultiStepExamples
- "Example-NLP-edited" contains an example of natural language processing described in the AGI-13 paper "Natural Language Processing by Reasoning and Learning".

An HTML user manual is here:
http://www.cis.temple.edu/~pwang/Implementation/NARS/NARS-GUI-Guide.html

The project home page:
https://code.google.com/p/open-nars/

Discussion Group:
https://groups.google.com/forum/?fromgroups#!forum/open-nars


