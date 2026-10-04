package com.wac.autocore.test;

import com.sun.source.tree.ArrayAccessTree;
import com.sun.source.tree.AssignmentTree;
import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.ConditionalExpressionTree;
import com.sun.source.tree.EnhancedForLoopTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.LiteralTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.ParenthesizedTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.TypeCastTree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.JavacTask;
import com.sun.source.util.SourcePositions;
import com.sun.source.util.TreePath;
import com.sun.source.util.TreePathScanner;
import com.sun.source.util.Trees;

import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * DATAFLÖDESANALYS (AK-15): följer var en sträng kommer ifrån, fram till dess att den används som
 * databasfråga eller processkommando.
 *
 * Varför den finns: mönstret i {@link SecurityAuditTest} fångar en fråga som byggs med "+" och körs
 * på samma rad. Bygger man frågan på en rad och kör den på en annan går den rakt igenom. Här följs
 * värdet i stället: en sträng som innehåller något annat än literaler och kända konstanter är
 * smittad, och en smittad sträng får inte hamna i en databasfråga eller i ett processanrop.
 *
 * Analysen görs på kompilatorns eget träd (JDK:ns JavacTask), inte på textrader, så den skiljer på
 * {@code "SELECT * FROM " + TABELL} (konstant, ofarligt) och {@code "SELECT * FROM " + namn}
 * (indata, farligt) även när de ser likadana ut i text.
 *
 * Kända gränser, med flit:
 * - Ingen typanalys: en sträng är en sträng. Analysen vet inte om ett värde är en konstant som
 *   beräknats fram, den ser bara var det kommer ifrån.
 * - Ingen interproceduranalys: ett värde som smittas i en annan metod (returneras, sparas i ett
 *   fält, skickas vidare) följs inte hit.
 * - Bara databas- och processanrop granskas. Loggning av känsliga uppgifter bevakas av mönstret i
 *   {@code SecurityAuditTest.testNoSensitiveDataLogging}.
 */
public class DataFlowAuditTest {

    private static final File SRC_ROOT = resolveSrcRoot();

    private static File resolveSrcRoot() {
        File[] candidates = new File[] {
                new File("WigellAutoCore/autocore/src"),
                new File("autocore/src"),
                new File("src")
        };
        for (File c : candidates) {
            if (c.exists() && c.isDirectory()) return c;
        }
        return candidates[0];
    }

    /** Anrop som skickar en fråga till databasen. */
    private static final Set<String> SQL_SINKS = new HashSet<String>(Arrays.asList(
            "executeQuery", "executeUpdate", "execute", "prepareStatement", "prepareCall", "addBatch"));

    /** Fält och lokala variabler som bevisligen bara innehåller literaler eller kända konstanter. */
    private final Set<String> clean = new HashSet<String>();

    /** Lokala variabler i den metod som just nu genomsöks (nollas vid varje metod). */
    private final Set<String> localClean = new HashSet<String>();

    /** Lokala variabler som byggts av data och därför är smittade. */
    private final Set<String> dirtyLocals = new HashSet<String>();

    /** StringBuilder/StringBuffer-variabler och de som har fått något icke-literalt tillagt. */
    private final Set<String> builders = new HashSet<String>();
    private final Set<String> taintedBuilders = new HashSet<String>();

    private final List<String> violations = new ArrayList<String>();
    private CompilationUnitTree unit;
    private SourcePositions positions;

    // ──────────────────────────────────────────────────────────────────────── tester

    /**
     * SÄKERHET: Ingen fråga som byggts av data får köras mot databasen, oavsett var i metoden
     * frågan byggdes.
     */
    public static void testNoTaintedSqlReachesTheDatabase() throws Exception {
        DataFlowAuditTest audit = new DataFlowAuditTest();
        audit.scanSources();

        List<String> sqlViolations = new ArrayList<String>();
        for (String v : audit.violations) {
            if (v.contains("[databas]")) sqlViolations.add(v);
        }
        TestRunner.assertTrue(sqlViolations.isEmpty(),
                "Frågor som byggts av data och körs mot databasen (använd PreparedStatement med ?): "
                        + join(sqlViolations));
    }

    /**
     * SÄKERHET: Inget processkommando får byggas av data.
     */
    public static void testNoTaintedValueReachesAProcessCall() throws Exception {
        DataFlowAuditTest audit = new DataFlowAuditTest();
        audit.scanSources();

        List<String> processViolations = new ArrayList<String>();
        for (String v : audit.violations) {
            // RestartProofRunner startar appen för att bevisa att data överlever en omstart.
            // Mönsterletningen undantar samma fil av samma skäl, och kommandot är dynamiskt med flit.
            if (v.contains("[process]") && !v.contains("RestartProofRunner.java")) {
                processViolations.add(v);
            }
        }
        TestRunner.assertTrue(processViolations.isEmpty(),
                "Processkommandon som byggts av data: " + join(processViolations));
    }

    /**
     * ANALYSENS EGET PROV: analysen måste fälla det mönster mönsterletningen missar, och måste
     * lämna den ofarliga varianten i fred. Utan det här provet vet vi inte om analysen kan fälla
     * något alls — en granskning som aldrig larmar ser lika grön ut som en ren kodbas.
     */
    public static void testTheAnalysisCatchesWhatThePatternMisses() throws Exception {
        String byggtAvData = ""
                + "class ProvFarlig {\n"
                + "    void run(java.sql.Connection conn, String platta) throws Exception {\n"
                + "        String sql = \"DELETE FROM payments WHERE id = \" + platta;\n"
                + "        conn.createStatement().executeUpdate(sql);\n"
                + "    }\n"
                + "}\n";

        String byggtAvKonstanter = ""
                + "class ProvOfarlig {\n"
                + "    private static final String TABELL = \"payments\";\n"
                + "    void run(java.sql.Connection conn, int id) throws Exception {\n"
                + "        String sql = \"DELETE FROM \" + TABELL + \" WHERE id = ?\";\n"
                + "        java.sql.PreparedStatement ps = conn.prepareStatement(sql);\n"
                + "        ps.setInt(1, id);\n"
                + "    }\n"
                + "}\n";

        List<String> farlig = analyse("ProvFarlig", byggtAvData);
        List<String> ofarlig = analyse("ProvOfarlig", byggtAvKonstanter);

        TestRunner.assertTrue(!farlig.isEmpty(),
                "Analysen fångade inte frågan som byggs på en rad och körs på en annan — då bevakar "
                        + "den ingenting");
        TestRunner.assertTrue(ofarlig.isEmpty(),
                "Analysen fällde ofarlig kod (konstant tabellnamn och parametervärde): " + join(ofarlig));
    }

    // ──────────────────────────────────────────────────────────────────────── analysen

    private static List<String> analyse(String className, String source) throws Exception {
        JavaCompiler compiler = compiler();
        StandardJavaFileManager fileManager = compiler.getStandardFileManager(null, null, null);
        JavaFileObject unit = inMemory(className, source);

        JavacTask task = (JavacTask) compiler.getTask(null, fileManager, null,
                Arrays.asList("-proc:none"), null, Arrays.asList(unit));

        DataFlowAuditTest audit = new DataFlowAuditTest();
        audit.positions = Trees.instance(task).getSourcePositions();
        for (CompilationUnitTree tree : task.parse()) {
            audit.analyseUnit(tree);
        }
        fileManager.close();
        return audit.violations;
    }

    private void scanSources() throws Exception {
        JavaCompiler compiler = compiler();
        List<File> files = SecurityAuditTest.listJavaFiles(SRC_ROOT);
        TestRunner.assertTrue(!files.isEmpty(), "Minst en Java-källfil måste hittas");

        StandardJavaFileManager fileManager = compiler.getStandardFileManager(null, null, null);
        JavacTask task = (JavacTask) compiler.getTask(null, fileManager, null,
                Arrays.asList("-proc:none"), null, fileManager.getJavaFileObjectsFromFiles(files));
        positions = Trees.instance(task).getSourcePositions();

        for (CompilationUnitTree tree : task.parse()) {
            analyseUnit(tree);
        }
        fileManager.close();
    }

    private void analyseUnit(CompilationUnitTree tree) {
        unit = tree;
        clean.clear();
        localClean.clear();
        dirtyLocals.clear();
        builders.clear();
        taintedBuilders.clear();

        // Kända konstanter först: en statisk final-sträng som bara innehåller literaler är ofarlig,
        // och det är den enda skillnaden mellan "SELECT ... " + TABELL och "SELECT ... " + namn.
        for (int pass = 0; pass < 3; pass++) {
            new TreePathScanner<Void, Void>() {
                @Override
                public Void visitVariable(VariableTree node, Void p) {
                    if (isClassLevel(getCurrentPath()) && isStaticFinal(node)
                            && node.getInitializer() != null && !isTainted(node.getInitializer())) {
                        clean.add(node.getName().toString());
                    }
                    return super.visitVariable(node, p);
                }
            }.scan(tree, null);
        }

        new TaintScanner().scan(tree, null);
    }

    /** Följer värden inom en metod och rapporterar när ett smittat värde når en känslig anrop. */
    private class TaintScanner extends TreePathScanner<Void, Void> {

        @Override
        public Void visitMethod(com.sun.source.tree.MethodTree node, Void p) {
            localClean.clear();
            dirtyLocals.clear();
            builders.clear();
            taintedBuilders.clear();
            return super.visitMethod(node, p);
        }

        @Override
        public Void visitVariable(VariableTree node, Void p) {
            String name = node.getName().toString();
            ExpressionTree init = node.getInitializer();
            if (init != null && !isClassLevel(getCurrentPath())) {
                if (isNewBuilder(init)) {
                    builders.add(name);
                } else if (isTainted(init)) {
                    dirtyLocals.add(name);
                } else {
                    localClean.add(name);
                }
            }
            return super.visitVariable(node, p);
        }

        @Override
        public Void visitEnhancedForLoop(EnhancedForLoopTree node, Void p) {
            // for (String sql : createStatements) — en konstant array ger ofarliga värden.
            ExpressionTree source = node.getExpression();
            if (source instanceof IdentifierTree && isKnown(source)) {
                localClean.add(node.getVariable().getName().toString());
            }
            return super.visitEnhancedForLoop(node, p);
        }

        @Override
        public Void visitAssignment(AssignmentTree node, Void p) {
            if (node.getVariable() instanceof IdentifierTree) {
                String name = ((IdentifierTree) node.getVariable()).getName().toString();
                if (isTainted(node.getExpression())) {
                    dirtyLocals.add(name);
                    localClean.remove(name);
                } else {
                    dirtyLocals.remove(name);
                    localClean.add(name);
                }
            }
            return super.visitAssignment(node, p);
        }

        @Override
        public Void visitMethodInvocation(MethodInvocationTree node, Void p) {
            String method = name(node);

            if ("append".equals(method) && node.getMethodSelect() instanceof MemberSelectTree) {
                String receiver = receiverName(node);
                if (receiver != null && builders.contains(receiver) && anyDynamic(node.getArguments())) {
                    taintedBuilders.add(receiver);
                }
            }

            // Bara första argumentet: i JDBC:s signaturer är det frågan. Resten är flaggor och
            // lägen (t.ex. Statement.RETURN_GENERATED_KEYS), och analysen har ingen typanalys.
            if (SQL_SINKS.contains(method) && firstIsTainted(node.getArguments())) {
                report("[databas]", node, method + "(" + trim(firstTainted(node.getArguments())) + ")");
            }

            if ("exec".equals(method) && isRuntimeGetRuntime(node) && firstIsTainted(node.getArguments())) {
                report("[process]", node, "Runtime.exec(" + trim(firstTainted(node.getArguments())) + ")");
            }

            return super.visitMethodInvocation(node, p);
        }

        @Override
        public Void visitNewClass(NewClassTree node, Void p) {
            if ("ProcessBuilder".equals(identifier(node.getIdentifier())) && anyTainted(node.getArguments())) {
                report("[process]", node, "ProcessBuilder(" + firstTainted(node.getArguments()) + ")");
            }
            return super.visitNewClass(node, p);
        }
    }

    /**
     * Är uttrycket byggt av något annat än literaler och kända konstanter? Okända värden räknas som
     * smittade: hellre en falsk träff att titta på än ett hål.
     */
    private boolean isTainted(ExpressionTree e) {
        if (e == null) {
            return false;
        }
        if (e instanceof LiteralTree) {
            return false;
        }
        if (e instanceof IdentifierTree) {
            return dirtyLocals.contains(((IdentifierTree) e).getName().toString());
        }
        if (e instanceof ParenthesizedTree) {
            return isTainted(((ParenthesizedTree) e).getExpression());
        }
        if (e instanceof TypeCastTree) {
            return isTainted(((TypeCastTree) e).getExpression());
        }
        if (e instanceof BinaryTree) {
            // Sammansättning är vägen till smitta: en literal plus något annat än en literal eller
            // en känd konstant blir data. Ett värde som kommer in i metoden (en parameter) är inte
            // byggt här och rapporteras inte — det är den kända gränsen, se klasstexten.
            BinaryTree binary = (BinaryTree) e;
            return dynamic(binary.getLeftOperand()) || dynamic(binary.getRightOperand());
        }
        if (e instanceof ConditionalExpressionTree) {
            ConditionalExpressionTree conditional = (ConditionalExpressionTree) e;
            return isTainted(conditional.getTrueExpression())
                    || isTainted(conditional.getFalseExpression());
        }
        if (e instanceof ArrayAccessTree) {
            return isTainted(((ArrayAccessTree) e).getExpression());
        }
        if (e instanceof com.sun.source.tree.NewArrayTree) {
            // private static final String[] FRAGOR = { "..." , "..." } — literaler är ofarliga.
            com.sun.source.tree.NewArrayTree array = (com.sun.source.tree.NewArrayTree) e;
            return array.getInitializers() != null && anyTainted(array.getInitializers());
        }
        if (e instanceof MethodInvocationTree) {
            MethodInvocationTree call = (MethodInvocationTree) e;
            String method = name(call);
            String receiver = receiverName(call);
            if ("toString".equals(method) && receiver != null) {
                return taintedBuilders.contains(receiver);
            }
            if ("format".equals(method) || "valueOf".equals(method) || "join".equals(method)
                    || "concat".equals(method)) {
                return anyDynamic(call.getArguments());
            }
            return false;
        }
        return false;
    }

    /**
     * Är uttrycket något annat än en literal eller en känd konstant? Frågan ställs bara inuti en
     * sammansättning: det är där ett värde blir data.
     */
    private boolean dynamic(ExpressionTree e) {
        if (e == null || e instanceof LiteralTree) {
            return false;
        }
        if (e instanceof IdentifierTree) {
            return !isKnown(e);
        }
        if (e instanceof BinaryTree) {
            // En nästlad sammansättning är bara dynamisk om något inuti den är det:
            // "DELETE FROM " + TABELL + " WHERE id = ?" är ofarlig när TABELL är en konstant.
            BinaryTree binary = (BinaryTree) e;
            return dynamic(binary.getLeftOperand()) || dynamic(binary.getRightOperand());
        }
        if (e instanceof ParenthesizedTree) {
            return dynamic(((ParenthesizedTree) e).getExpression());
        }
        if (e instanceof TypeCastTree) {
            return dynamic(((TypeCastTree) e).getExpression());
        }
        return true;
    }

    private boolean anyDynamic(List<? extends ExpressionTree> expressions) {
        for (ExpressionTree e : expressions) {
            if (dynamic(e)) {
                return true;
            }
        }
        return false;
    }

    private boolean isKnown(ExpressionTree e) {
        String n = e instanceof IdentifierTree ? ((IdentifierTree) e).getName().toString()
                : e instanceof MemberSelectTree ? ((MemberSelectTree) e).getIdentifier().toString() : null;
        return n != null && (clean.contains(n) || localClean.contains(n));
    }

    private boolean firstIsTainted(List<? extends ExpressionTree> arguments) {
        return !arguments.isEmpty() && isTainted(arguments.get(0));
    }

    private boolean anyTainted(List<? extends ExpressionTree> arguments) {
        for (ExpressionTree e : arguments) {
            if (isTainted(e)) {
                return true;
            }
        }
        return false;
    }

    private String firstTainted(List<? extends ExpressionTree> arguments) {
        for (ExpressionTree e : arguments) {
            if (isTainted(e)) {
                return e.toString();
            }
        }
        return "";
    }

    private void report(String kind, Tree node, String detail) {
        long start = positions.getStartPosition(unit, node);
        long line = unit.getLineMap() == null || start < 0 ? 0 : unit.getLineMap().getLineNumber(start);
        String source = unit.getSourceFile().toUri().getScheme() != null
                && "file".equals(unit.getSourceFile().toUri().getScheme())
                        ? new File(unit.getSourceFile().toUri()).getName()
                        : unit.getSourceFile().getName();
        violations.add(String.format("%s %s:%d — %s", kind, source, line, detail));
    }

    // ──────────────────────────────────────────────────────────────────────── småhjälp

    private static JavaCompiler compiler() {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            try {
                Class<?> javacToolClass = Class.forName("com.sun.tools.javac.api.JavacTool");
                compiler = (JavaCompiler) javacToolClass.getMethod("create").invoke(null);
            } catch (Throwable ignored) {
            }
        }
        TestRunner.assertNotNull(compiler,
                "Dataflödesanalysen behöver JDK:ns kompilator (javac), men den hittades inte. "
                        + "Kör sviten med ett JDK och inte ett JRE — annars bevakas ingenting.");
        return compiler;
    }

    private static JavaFileObject inMemory(final String className, final String source) {
        return new SimpleJavaFileObject(URI.create("string:///" + className + ".java"),
                JavaFileObject.Kind.SOURCE) {
            @Override
            public CharSequence getCharContent(boolean ignoreEncodingErrors) {
                return source;
            }
        };
    }

    private static boolean isClassLevel(TreePath path) {
        TreePath parent = path == null ? null : path.getParentPath();
        return parent != null && parent.getLeaf() instanceof ClassTree;
    }

    private static boolean isStaticFinal(VariableTree node) {
        String modifiers = node.getModifiers().toString();
        return modifiers.contains("static") && modifiers.contains("final");
    }

    private static boolean isNewBuilder(ExpressionTree e) {
        if (!(e instanceof NewClassTree)) {
            return false;
        }
        String name = identifier(((NewClassTree) e).getIdentifier());
        return "StringBuilder".equals(name) || "StringBuffer".equals(name);
    }

    private static boolean isRuntimeGetRuntime(MethodInvocationTree call) {
        if (!(call.getMethodSelect() instanceof MemberSelectTree)) {
            return false;
        }
        ExpressionTree receiver = ((MemberSelectTree) call.getMethodSelect()).getExpression();
        return receiver instanceof MethodInvocationTree
                && "getRuntime".equals(name((MethodInvocationTree) receiver));
    }

    private static String receiverName(MethodInvocationTree call) {
        if (!(call.getMethodSelect() instanceof MemberSelectTree)) {
            return null;
        }
        ExpressionTree receiver = ((MemberSelectTree) call.getMethodSelect()).getExpression();
        return receiver instanceof IdentifierTree ? ((IdentifierTree) receiver).getName().toString() : null;
    }

    private static String name(MethodInvocationTree call) {
        if (call.getMethodSelect() instanceof MemberSelectTree) {
            return ((MemberSelectTree) call.getMethodSelect()).getIdentifier().toString();
        }
        return identifier(call.getMethodSelect());
    }

    private static String identifier(Tree tree) {
        return tree instanceof IdentifierTree ? ((IdentifierTree) tree).getName().toString()
                : tree == null ? "" : tree.toString();
    }

    private static String trim(String text) {
        String oneLine = text.replace('\n', ' ');
        return oneLine.length() > 60 ? oneLine.substring(0, 57) + "..." : oneLine;
    }

    /** En rad, inte flera: svitens loggutdrag läser en rad per test och klipper resten. */
    private static String join(List<String> lines) {
        StringBuilder builder = new StringBuilder();
        int shown = 0;
        for (String line : lines) {
            if (shown++ == 6) {
                builder.append(" | ... och ").append(lines.size() - 6).append(" till");
                break;
            }
            if (builder.length() > 0) {
                builder.append(" | ");
            }
            builder.append(line);
        }
        return builder.toString();
    }
}
