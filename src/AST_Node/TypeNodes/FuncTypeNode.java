package AST_Node.TypeNodes;

import AST_Node.DeclNodes.FuncDeclNode;
import AST_Node.DeclNodes.VarDeclListNode;

public class FuncTypeNode extends TypeNode{
    private boolean isConstructFunction;
    private TypeNode functionReturnType;
    private VarDeclListNode functionParameterList;
    public FuncTypeNode(FuncDeclNode funcDeclNode) {
        isConstructFunction = funcDeclNode.isConstructFunction();
        functionReturnType = funcDeclNode.getFunctionReturnType();
        functionParameterList = funcDeclNode.getFunctionParameterList();
        this.basetype = functionReturnType.basetype;
        this.dim = functionReturnType.dim;
    }

    public boolean isConstructFunction() {
        return isConstructFunction;
    }

    public TypeNode getFunctionReturnType() {
        return functionReturnType;
    }

    public VarDeclListNode getFunctionParameterList() {
        return functionParameterList;
    }
}
