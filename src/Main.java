import AST_Node.ProgNode;
import Frontend.ASTBuilder;
import Parser.LMxLexer;
import Parser.LMxParser;
import Tools.ASTViewer;
import Tools.CodeGenerator;
import Tools.IRGenerator;
import Tools.SemanticCheck;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;

import java.io.InputStream;
import static java.lang.System.err;

public class Main {
    public static void main(String[] args) throws Exception {
        InputStream inS = System.in;

        CharStream stream = CharStreams.fromStream(inS);
        LMxLexer lexer              = new LMxLexer(stream);
        CommonTokenStream tokens    = new CommonTokenStream(lexer);
        LMxParser parser            = new LMxParser(tokens);
        ParseTree parsetree = parser.program();

        ASTBuilder builder = new ASTBuilder();
        ProgNode ast = (ProgNode) builder.visit(parsetree);

        SemanticCheck scoper = new SemanticCheck();
        try {
            scoper.visit(ast);
        }
        catch (Error e) {
            err.println(e);
        }

//        ASTViewer viewer = new ASTViewer();
//        viewer.visit(ast);

        IRGenerator irGenerator = new IRGenerator();
        irGenerator.visit(ast);

        CodeGenerator codeGenerator = new CodeGenerator();
        codeGenerator.generate(irGenerator, ast);
    }
}
