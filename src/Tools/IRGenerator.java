package Tools;

import AST_Node.ASTVisitor;
import AST_Node.DeclNodes.ClassDeclNode;
import AST_Node.DeclNodes.DeclNode;
import AST_Node.DeclNodes.FuncDeclNode;
import AST_Node.DeclNodes.VarDeclNode;
import AST_Node.ExprNodes.*;
import AST_Node.ProgNode;
import AST_Node.StmtNodes.*;
import AST_Node.TypeNodes.ArrayTypeNode;
import AST_Node.TypeNodes.ClassTypeNode;
import AST_Node.TypeNodes.TypeNode;
import IR.IRNodes.*;
import Type.Type;

import java.util.*;

public class IRGenerator implements ASTVisitor {
    static private boolean stopVisit = false;

    static private LinkedList<IntValue> exprLinkedList = new LinkedList<>();

    static private LinkedList<BasicBlock> breakLinkedList = new LinkedList<>();
    static private LinkedList<BasicBlock> continueLinkedList = new LinkedList<>();

    static private Map<String, Integer> registerCntMap = new HashMap<>();
    static private Map<String, BasicBlock> funcBlockMap = new HashMap<>();

    private BasicBlock currentBlock;
    private BasicBlock startBlock;
    private BasicBlock shortcut2Block;

    public Map<String, Integer> getRegisterCntMap() {
        return registerCntMap;
    }

    public Map<String, BasicBlock> getFuncBlockMap() {
        return funcBlockMap;
    }

    private void initGlobalVar(ProgNode progNode) {
        for (DeclNode declNode : progNode.getDeclarations()) {
            if (declNode instanceof VarDeclNode) {
                if (((VarDeclNode) declNode).getVarinit() != null) {
                    ((VarDeclNode) declNode).getVarinit().accept(this);
                    currentBlock.append(new Assign(((VarDeclNode) declNode).getIntValue(), exprLinkedList.pop()));
                }
            }
        }
    }

    @Override
    public void visit(ProgNode progNode) {
        for (DeclNode declNode : progNode.getDeclarations()) {
            if (declNode instanceof VarDeclNode) {
                VarDeclNode varDeclNode = (VarDeclNode) declNode;
                varDeclNode.setIntValue(new GloalVar(varDeclNode.getVarname()));
            }
        }
        for (DeclNode declNode : progNode.getDeclarations()) {
            if (declNode instanceof FuncDeclNode) {
                startBlock = new BasicBlock();
                funcBlockMap.put(declNode.getDeclname(), startBlock);
                currentBlock = startBlock;
                if (((FuncDeclNode) declNode).getFunctionName().equals("main"))
                    initGlobalVar(progNode);
                declNode.accept(this);
            }
        }
    }

    @Override
    public void visit(FuncDeclNode funcDeclNode) {
        if (funcDeclNode.getFunctionStatements() != null) {
            Register.resetCnt();
            for (VarDeclNode varDeclNode : funcDeclNode.getFunctionParameterList().getVardeclnodeList()){
                varDeclNode.setIntValue(new Register());
            }
            funcDeclNode.getFunctionStatements().accept(this);
            registerCntMap.put(funcDeclNode.getFunctionName(), Register.getCntRegister() - 15);
        }
    }

    @Override
    public void visit(CompStmtNode compStmtNode) {
        for (StmtNode stmtNode : compStmtNode.getStmtNodeList()) {
            if (stopVisit) break;
            stmtNode.accept(this);
        }
        stopVisit = false;
    }

    @Override
    public void visit(ExprStmtNode exprStmtNode) {
        if (exprStmtNode.getExprnode() != null) {
            exprStmtNode.getExprnode().accept(this);
            exprLinkedList.pop();
        }
    }

    @Override
    public void visit(VarDeclStmtNode varDeclStmtNode) {
        varDeclStmtNode.getVardeclnode().accept(this);
    }

    @Override
    public void visit(IfStmtNode ifStmtNode) {
        if (ifStmtNode.getElsestmt() == null) {
            BasicBlock thenBlock = new BasicBlock();
            BasicBlock endBlock = new BasicBlock();

            shortcut2Block = endBlock;
            ifStmtNode.getIfexpr().accept(this);

            currentBlock.append(new Cjump(exprLinkedList.pop(), thenBlock, endBlock));

            currentBlock = thenBlock;
            ifStmtNode.getIfstmt().accept(this);
            thenBlock.append(new Jump(endBlock));

            currentBlock = endBlock;
        } else {
            BasicBlock thenBlock = new BasicBlock();
            BasicBlock elseBlock = new BasicBlock();
            BasicBlock endBlock = new BasicBlock();

            shortcut2Block = elseBlock;
            ifStmtNode.getIfexpr().accept(this);

            currentBlock.append(new Cjump(exprLinkedList.pop(), thenBlock, elseBlock));

            currentBlock = thenBlock;
            ifStmtNode.getIfstmt().accept(this);
            thenBlock.append(new Jump(endBlock));

            currentBlock = elseBlock;
            ifStmtNode.getElsestmt().accept(this);
            elseBlock.append(new Jump(endBlock));

            currentBlock = endBlock;
        }
    }

    @Override
    public void visit(WhileStmtNode whileStmtNode) {
        BasicBlock condBlock = new BasicBlock();
        BasicBlock whileBlock = new BasicBlock();
        BasicBlock endBlock = new BasicBlock();

        currentBlock.append(new Jump(condBlock));
        currentBlock = condBlock;

        shortcut2Block = endBlock;
        whileStmtNode.getWhileexpr().accept(this);

        currentBlock.append(new Cjump(exprLinkedList.pop(), whileBlock, endBlock));

        continueLinkedList.push(condBlock);
        breakLinkedList.push(endBlock);

        currentBlock = whileBlock;
        whileStmtNode.getWhilestmt().accept(this);
        currentBlock.append(new Jump(condBlock));

        continueLinkedList.pop();
        breakLinkedList.pop();

        currentBlock = endBlock;
    }

    @Override
    public void visit(ForStmtNode forStmtNode) {
        BasicBlock condBlock = new BasicBlock();
        BasicBlock forBlock = new BasicBlock();
        BasicBlock endBlock = new BasicBlock();

        if (forStmtNode.getForexprinit() != null) {
            forStmtNode.getForexprinit().accept(this);
        } else {
            for (VarDeclNode varDeclNode : forStmtNode.getForinit()) {
                varDeclNode.accept(this);
            }
        }
        currentBlock.append(new Jump(condBlock));
        currentBlock = condBlock;

        shortcut2Block = endBlock;
        forStmtNode.getForexprend().accept(this);

        currentBlock.append(new Cjump(exprLinkedList.pop(), forBlock, endBlock));

        continueLinkedList.push(condBlock);
        breakLinkedList.push(endBlock);

        currentBlock = forBlock;
        forStmtNode.getForstmt().accept(this);
        forStmtNode.getForexprupdate().accept(this);
        currentBlock.append(new Jump(condBlock));

        continueLinkedList.pop();
        breakLinkedList.pop();

        currentBlock = endBlock;
    }

    @Override
    public void visit(BreakStmtNode breakStmtNode) {
        currentBlock.append(new Jump(breakLinkedList.getLast()));
        stopVisit = true;
    }

    @Override
    public void visit(ContinueStmtNode continueStmtNode) {
        currentBlock.append(new Jump(continueLinkedList.getLast()));
        stopVisit = true;
    }

    @Override
    public void visit(ReturnStmtNode returnStmtNode) {
        returnStmtNode.getReturnexpr().accept(this);
        currentBlock.append(new ReturnInst(exprLinkedList.pop()));
    }

    //  ***************************** Stmt Over ***************************** //

    @Override
    public void visit(VarDeclNode varDeclNode) {

//      Non-Array & Non-Class
        if (varDeclNode.getVartype() instanceof TypeNode) {
//          Int
            if (varDeclNode.getVartype().getBasetype() != Type.Types.STRING) {
                varDeclNode.setIntValue(new Register());
                if (varDeclNode.getVarinit() != null) {
                    if (varDeclNode.getVarinit() instanceof BinaryExprNode) {
                        BinaryExprNode.BinaryOP binaryOP = ((BinaryExprNode) varDeclNode.getVarinit()).getExprop();
                        if (binaryOP == BinaryExprNode.BinaryOP.LOGICAL_AND || binaryOP == BinaryExprNode.BinaryOP.LOGICAL_OR) {
                            BasicBlock nxtBlock = new BasicBlock();
                            shortcut2Block = nxtBlock;
                            varDeclNode.getVarinit().accept(this);
                            currentBlock.append(new Jump(nxtBlock));
                            currentBlock = nxtBlock;
                            currentBlock.append(new Assign(varDeclNode.getIntValue(), exprLinkedList.pop()));
                            return;
                        }
                    }
                    varDeclNode.getVarinit().accept(this);
                    currentBlock.append(new Assign(varDeclNode.getIntValue(), exprLinkedList.pop()));
                }
            }
        }
    }

    @Override
    public void visit(BinaryExprNode binaryExprNode) {
        switch (binaryExprNode.getExprop()) {
            case LOGICAL_AND: {
                Register register = new Register();
                BasicBlock shortCutBlock = new BasicBlock();
                shortCutBlock.append(new Assign(register, new ConstValue(0)));
                shortCutBlock.append(new Jump(shortcut2Block));

                BasicBlock nxtBlock = new BasicBlock();
                shortcut2Block = nxtBlock;
                binaryExprNode.getLhs().accept(this);
                currentBlock.append(new Cjump(exprLinkedList.pop(), nxtBlock, shortCutBlock));
                currentBlock = nxtBlock;

                nxtBlock = new BasicBlock();
                shortcut2Block = nxtBlock;
                binaryExprNode.getRhs().accept(this);
                currentBlock.append(new Cjump(exprLinkedList.pop(), nxtBlock, shortCutBlock));
                currentBlock = nxtBlock;

                currentBlock.append(new Assign(register, new ConstValue(1)));
                exprLinkedList.push(register);
                break;
            }
            case LOGICAL_OR: {
                Register register = new Register();
                BasicBlock shortCutBlock = new BasicBlock();
                shortCutBlock.append(new Assign(register, new ConstValue(1)));
                shortCutBlock.append(new Jump(shortcut2Block));

                BasicBlock nxtBlock = new BasicBlock();
                shortcut2Block = nxtBlock;
                binaryExprNode.getLhs().accept(this);
                currentBlock.append(new Cjump(exprLinkedList.pop(), shortCutBlock, nxtBlock));
                currentBlock = nxtBlock;

                nxtBlock = new BasicBlock();
                shortcut2Block = nxtBlock;
                binaryExprNode.getRhs().accept(this);
                currentBlock.append(new Cjump(exprLinkedList.pop(), shortCutBlock, nxtBlock));
                currentBlock = nxtBlock;

                currentBlock.append(new Assign(register, new ConstValue(0)));
                exprLinkedList.push(register);
                break;
            }
            case ASSIGN: {
                binaryExprNode.getRhs().accept(this);
                IntValue rhs = exprLinkedList.pop();
                binaryExprNode.getLhs().accept(this);
                IntValue lhs = exprLinkedList.pop();
                currentBlock.append(new Assign(lhs, rhs));
                exprLinkedList.push(new ConstValue(0));
                break;
            }
            case GREATER_EQUAL:
            case LESS_EQUAL:
            case EQUAL:
            case INEQUAL:
            case GREATER:
            case LESS:
            case BIT_XOR:
            case BIT_AND:
            case BIR_OR:
            case SHR:
            case SHL:
            case ADD:
            case SUB:
            case MUL:
            case MOD:
            case DIV: {
                binaryExprNode.getRhs().accept(this);
                IntValue rhs = exprLinkedList.pop();
                binaryExprNode.getLhs().accept(this);
                IntValue lhs = exprLinkedList.pop();
                Register register = new Register();
                currentBlock.append(new Bin(binaryExprNode.getExprop(), lhs, rhs, register));
                exprLinkedList.push(register);
                break;
            }
        }
    }

    @Override
    public void visit(SuffixExprNode suffixExprNode) {
        suffixExprNode.getSuffixexpr().accept(this);

        IntValue intValue = exprLinkedList.pop();
        Register register = new Register();

        //copy
        currentBlock.append(new Assign(register, intValue));

        if (suffixExprNode.getExprop() == SuffixExprNode.SuffixOP.SELF_INC)
            currentBlock.append(new Uni(UnaryExprNode.UnaryOP.SELF_INC, intValue, new Register()));
        else currentBlock.append(new Uni(UnaryExprNode.UnaryOP.SELF_DEC, intValue, new Register()));

        exprLinkedList.push(register);
    }

    @Override
    public void visit(UnaryExprNode unaryExprNode) {
        Register register = new Register();
        if (unaryExprNode.getUnaryexpr() instanceof BinaryExprNode) {
            BinaryExprNode.BinaryOP binaryOP = ((BinaryExprNode) unaryExprNode.getUnaryexpr()).getExprop();
            if (binaryOP == BinaryExprNode.BinaryOP.LOGICAL_AND || binaryOP == BinaryExprNode.BinaryOP.LOGICAL_OR) {
                BasicBlock nxtBlock = new BasicBlock();
                shortcut2Block = nxtBlock;
                unaryExprNode.getUnaryexpr().accept(this);
                currentBlock.append(new Jump(nxtBlock));
                currentBlock = nxtBlock;
                currentBlock.append(new Uni(unaryExprNode.getExprop(), exprLinkedList.pop(), register));
                exprLinkedList.push(register);
                return;
            }
        }
        unaryExprNode.getUnaryexpr().accept(this);
        currentBlock.append(new Uni(unaryExprNode.getExprop(), exprLinkedList.pop(), register));
        exprLinkedList.push(register);
    }

    @Override
    public void visit(BoolExprNode boolExprNode) {
        if (boolExprNode.isTrue()) exprLinkedList.push(new ConstValue(1));
        else exprLinkedList.push(new ConstValue(0));
    }

    @Override
    public void visit(IntExprNode intExprNode) {
        exprLinkedList.push(new ConstValue(intExprNode.getIntvalue()));
    }

    @Override
    public void visit(IDExprNode idExprNode) {
        exprLinkedList.push(idExprNode.getVarDeclNode().getIntValue());
    }

    @Override
    public void visit(FuncCallExprNode funcCallExprNode) {
        String funcName = "NotFuncName";
        if (funcCallExprNode.getFunction() instanceof IDExprNode) {
            funcName = ((IDExprNode) funcCallExprNode.getFunction()).getId();
        }

        List<IntValue> intValueList = new ArrayList<>();
        for (ExprNode exprNode : funcCallExprNode.getParameters()){
            exprNode.accept(this);
            intValueList.add(exprLinkedList.pop());
        }

        currentBlock.append(new Call(funcName, intValueList));
        exprLinkedList.push(new Register(Register.RegisterName.RAX));
    }

    @Override
    public void visit(ArrayIndexExprNode arrayIndexExprNode) {

    }

    @Override
    public void visit(ArrayTypeNode arrayTypeNode) {

    }

    @Override
    public void visit(ClassDeclNode classDeclNode) {

    }

    @Override
    public void visit(ClassMethodExprNode classMethodExprNode) {

    }

    @Override
    public void visit(ClassTypeNode classTypeNode) {

    }

    @Override
    public void visit(ClassThisExprNode classThisExprNode) {

    }

    @Override
    public void visit(NewExprNode newExprNode) {

    }

    @Override
    public void visit(NullExprNode nullExprNode) {

    }

    @Override
    public void visit(StringExprNode stringExprNode) {

    }

    @Override
    public void visit(TypeNode typeNode) {

    }
}
