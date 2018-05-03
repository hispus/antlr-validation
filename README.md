# antlr-validation
Demonstrate DHIS2 data validation expression parsing using ANTLR lexer / parser.

To use:
Download http://www.antlr.org/download/antlr-4.7.1-complete.jar, save as, for example, /usr/local/lib/antlr-4.7.1-complete.jar

Then run:
```
alias antlr4="java -jar /usr/local/lib/antlr-4.7.1-complete.jar"
cd antlr-validation
mkdir out
cd src
antlr4 -o ../gen/ -no-listener -visitor DataValidation.g4
javac -d ../out ../gen/*.java
javac -cp "/usr/local/lib/antlr-4.7.1-complete.jar" -sourcepath ../gen -d ../out  *.java
cd ../out
java Test
```
