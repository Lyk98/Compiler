package AST_Node.StmtNodes;

import AST_Node.ASTVisitor;
import Tools.Scope.Scope;

import java.util.ArrayList;
import java.util.List;

public class CompStmtNode extends StmtNode{
    public List<StmtNode> stmtNodeList;
    public Scope astscope;

    public void setAstscope(Scope astscope) {
        this.astscope = astscope;
    }

    public CompStmtNode() {
        this.stmtNodeList = new ArrayList<>();
    }

    public CompStmtNode(List<StmtNode> stmtNodeList) {
        this.stmtNodeList = stmtNodeList;
    }

    @Override
    public void accept(ASTVisitor visitor) {
        visitor.visit(this);
    }
}
