# javacTreeApi
Hands on with the java compiler and Tree API

This repository is a study on how to use Javac, the Java Compiler, as a library.

In our case we don't want to actually compile anything, but instead we want to parse the code to get the [Abstract Syntax Tree](https://en.wikipedia.org/wiki/Abstract_syntax_tree)
and then walk the tree visiting the specific parts of code.
Javac offers has an API called Tree and a special class TreeScanner, which allows you to visit specific parts such as IF's, For's, imports, classes, etc, 
by overriding the existing methods.

This could be used for enforcing coding style checks or banning the use of some expressions, as mentioned here https://inside.java/2021/09/20/javac-tree-api
