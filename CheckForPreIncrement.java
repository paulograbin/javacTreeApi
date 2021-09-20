import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.EnhancedForLoopTree;
import com.sun.source.tree.ForLoopTree;
import com.sun.source.tree.IfTree;
import com.sun.source.tree.ImportTree;
import com.sun.source.tree.LineMap;
import com.sun.source.tree.Tree;
import com.sun.source.tree.UnaryTree;
import com.sun.source.util.JavacTask;
import com.sun.source.util.TreeScanner;
import com.sun.source.util.Trees;

import javax.tools.DocumentationTool;
import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;

public class CheckForPreIncrement {

    public static void main(String[] args) throws IOException {
        Path path = Paths.get(args.length == 0 ? "." : args[0]);

        path = Paths.get("/home/paulograbin/Projects/Personal/confirmation");
        System.out.println(path.toString());

        Files.walkFileTree(path, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                // is this a .java file?
                if (file.getFileName().toString().endsWith(".java")) {
                    try {
                        // check for ++i and --i pattern and report
                        check(file.toAbsolutePath());
                    } catch (IOException exc) {
                        // report parse failures and continue scanning other files
                        System.err.printf("parsing failed for %s : %s\n", file, exc);
                    }
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                // if a file cannot be read, just print error and continue scanning
                if (exc != null) {
                    System.err.printf("file visit failed for %s : %s\n", file, exc);
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                if (exc != null) {
                    System.err.printf("Dir visit filed for %s: %s \n", dir, exc);
                }

                return FileVisitResult.CONTINUE;
            }
        });
    }

    // major version of JDK such as 16, 18 etc.
    private static int getJavaMajorVersion() {
        var version = System.getProperty("java.version");
        return Integer.parseInt(version.substring(0, version.indexOf('.')));
    }

    // javac options we pass to the compiler. We enable preview so that
    // all preview features can be parsed.
    private static final List<String> OPTIONS = List.of("--enable-preview", "--release=" + getJavaMajorVersion());


    private static final JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
    private static final DocumentationTool documentation = ToolProvider.getSystemDocumentationTool();

    private static void check(Path toAbsolutePath) throws IOException {
        System.out.println("Check " + toAbsolutePath);

        var compUnits = compiler.getStandardFileManager(null, null, null)
                .getJavaFileObjects(toAbsolutePath);

        var task = (JavacTask) compiler.getTask(null, null, null, OPTIONS, null, compUnits);

        var sourcePositions = Trees.instance(task).getSourcePositions();

        // TreeVisitor implementation using TreeScanner
        var scanner = new TreeScanner<Void, Void>() {
            private CompilationUnitTree compUnit;
            private LineMap lineMap;
            private String fileName;

            // store details of the current compilation unit in instance vars
            @Override
            public Void visitCompilationUnit(CompilationUnitTree t, Void v) {
                compUnit = t;
                lineMap = t.getLineMap();
                fileName = t.getSourceFile().getName();
                return super.visitCompilationUnit(t, v);
            }

            @Override
            public Void visitClass(ClassTree node, Void unused) {
                System.out.println("Visiting class " + node.getSimpleName());

                return super.visitClass(node, unused);
            }

            @Override
            public Void visitIf(IfTree node, Void unused) {
                System.out.println(node.getCondition());

                return super.visitIf(node, unused);
            }

            // found a for loop to analyze
            @Override
            public Void visitForLoop(ForLoopTree t, Void v) {
                System.out.println(t.getCondition());
                System.out.println(t.getUpdate());
                System.out.println(t.getInitializer());
                System.out.println(t.getStatement());

                // check each update expression
                for (var est : t.getUpdate()) {
                    // is this a UnaryTree expression statement?
                    if (est.getExpression() instanceof UnaryTree unary) {
                        // is this prefix decrement or increment?
                        var kind = unary.getKind();
                        if (kind == Tree.Kind.PREFIX_DECREMENT ||
                                kind == Tree.Kind.PREFIX_INCREMENT) {
                            // report file name, line number and column number
                            var pos = sourcePositions.getStartPosition(compUnit, unary);
                            var line = lineMap.getLineNumber(pos);
                            var col = lineMap.getColumnNumber(pos);
                            System.out.printf("Found ++i or --i in %s %d:%d\n",
                                    fileName, line, col);
                        }
                    }

                }
                return super.visitForLoop(t, v);
            }
        };

        // visit each compilation unit tree object with our scanner
        for (var compUnitTree : task.parse()) {
            compUnitTree.accept(scanner, null);
        }
    }
}
