package tetris.quality;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.ImportTree;
import com.sun.source.util.JavacTask;
import com.sun.source.util.TreePathScanner;
import com.sun.source.util.Trees;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.util.Elements;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaFileObject;
import javax.tools.ToolProvider;

/** JDK 컴파일러가 해석한 심볼로 import를 검사한다. 주석·문자열은 사용으로 세지 않는다. */
public final class ImportAudit {
    public static void main(String[] args) throws Exception {
        int result = verify(Path.of(args[0]), args[1], System.out);
        if (result != 0) System.exit(result);
    }

    static int verify(Path root, String classpath, PrintStream output) throws Exception {
        var compiler = ToolProvider.getSystemJavaCompiler();
        try (var manager = compiler.getStandardFileManager(null, null, null);
             var files = Files.walk(root)) {
            var sources = files.filter(p -> p.toString().endsWith(".java")).map(Path::toFile).toList();
            var diagnostics = new DiagnosticCollector<JavaFileObject>();
            var task = (JavacTask) compiler.getTask(null, manager, diagnostics,
                    List.of("-proc:none", "--release", "21", "-classpath", classpath), null,
                    manager.getJavaFileObjectsFromFiles(sources));
            var units = new ArrayList<CompilationUnitTree>(); task.parse().forEach(units::add);
            task.analyze();
            if (diagnostics.getDiagnostics().stream().anyMatch(d -> d.getKind() == Diagnostic.Kind.ERROR)) {
                diagnostics.getDiagnostics().forEach(output::println); return 2;
            }
            var trees = Trees.instance(task);
            int unused = 0;
            for (var unit : units) {
                Set<String> types = new HashSet<>(), members = new HashSet<>();
                new TreePathScanner<Void, Void>() {
                    @Override public Void visitImport(ImportTree tree, Void unused) { return null; }
                    @Override public Void visitIdentifier(IdentifierTree tree, Void unused) {
                        Element symbol = trees.getElement(getCurrentPath());
                        if (symbol instanceof TypeElement type) types.add(type.getQualifiedName().toString());
                        else if (symbol != null && symbol.getEnclosingElement() instanceof TypeElement owner)
                            members.add(owner.getQualifiedName() + "." + symbol.getSimpleName());
                        return super.visitIdentifier(tree, unused);
                    }
                }.scan(unit, null);
                for (var imp : unit.getImports()) {
                    if (!used(imp, types, members, task.getElements())) {
                        output.println(Path.of(unit.getSourceFile().toUri()) + ": " + imp); unused++;
                    }
                }
            }
            output.println("Import audit: " + sources.size() + " files, " + unused + " unused imports");
            return unused == 0 ? 0 : 1;
        }
    }

    private static boolean used(ImportTree imp, Set<String> types, Set<String> members, Elements elements) {
        String name = imp.getQualifiedIdentifier().toString();
        boolean wildcard = name.endsWith(".*");
        String prefix = wildcard ? name.substring(0, name.length() - 1) : name;
        if (!imp.isStatic()) return types.stream().anyMatch(n -> wildcard
                ? n.startsWith(prefix) && n.substring(prefix.length()).indexOf('.') < 0 : n.equals(name));
        int dot = name.lastIndexOf('.');
        TypeElement owner = elements.getTypeElement(name.substring(0, dot));
        if (owner == null) return false;
        String memberName = name.substring(dot + 1);
        // 상속받은 정적 메서드를 import한 경우에도 실제 선언 클래스의 심볼을 비교한다.
        return elements.getAllMembers(owner).stream()
                .filter(m -> m.getModifiers().contains(Modifier.STATIC))
                .filter(m -> wildcard || m.getSimpleName().contentEquals(memberName))
                .anyMatch(m -> m instanceof TypeElement type ? types.contains(type.getQualifiedName().toString())
                        : m.getEnclosingElement() instanceof TypeElement declaring
                                && members.contains(declaring.getQualifiedName() + "." + m.getSimpleName()));
    }
}
