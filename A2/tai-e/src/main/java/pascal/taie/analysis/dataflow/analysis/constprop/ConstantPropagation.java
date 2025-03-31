/*
 * Tai-e: A Static Analysis Framework for Java
 *
 * Copyright (C) 2022 Tian Tan <tiantan@nju.edu.cn>
 * Copyright (C) 2022 Yue Li <yueli@nju.edu.cn>
 *
 * This file is part of Tai-e.
 *
 * Tai-e is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 *
 * Tai-e is distributed in the hope that it will be useful,but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General
 * Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Tai-e. If not, see <https://www.gnu.org/licenses/>.
 */

package pascal.taie.analysis.dataflow.analysis.constprop;

import pascal.taie.analysis.dataflow.analysis.AbstractDataflowAnalysis;
import pascal.taie.analysis.graph.cfg.CFG;
import pascal.taie.config.AnalysisConfig;
import pascal.taie.ir.IR;
import pascal.taie.ir.exp.ArithmeticExp;
import pascal.taie.ir.exp.BinaryExp;
import pascal.taie.ir.exp.BitwiseExp;
import pascal.taie.ir.exp.ConditionExp;
import pascal.taie.ir.exp.Exp;
import pascal.taie.ir.exp.IntLiteral;
import pascal.taie.ir.exp.ShiftExp;
import pascal.taie.ir.exp.Var;
import pascal.taie.ir.stmt.DefinitionStmt;
import pascal.taie.ir.stmt.Stmt;
import pascal.taie.language.type.PrimitiveType;
import pascal.taie.language.type.Type;
import pascal.taie.util.AnalysisException;

import java.util.concurrent.atomic.AtomicBoolean;

public class ConstantPropagation extends
        AbstractDataflowAnalysis<Stmt, CPFact> {

    public static final String ID = "constprop";

    public ConstantPropagation(AnalysisConfig config) {
        super(config);
    }

    @Override
    public boolean isForward() {
        return true;
    }

    @Override
    public CPFact newBoundaryFact(CFG<Stmt> cfg) {
        // TODO - finish me
        CPFact fact = new CPFact(); // 创建 CPFact 实例
        cfg.getIR().getParams().forEach(var -> { // // 遍历 CFG
            if (canHoldInt(var)) { // 如果变量可以存储整数
                fact.update(var, Value.getNAC()); // 更新 fact
            }
        });
        return fact; // 返回 fact
    }

    @Override
    public CPFact newInitialFact() {
        // TODO - finish me
        return new CPFact(); // 返回新的 CPFact 实例
    }

    @Override
    public void meetInto(CPFact fact, CPFact target) {
        // TODO - finish me
        fact.forEach(((var, value) -> { // 遍历fact
            target.update(var, meetValue(value, target.get(var))); // 更新target
        }));
    }

    /**
     * Meets two Values.
     */
    public Value meetValue(Value v1, Value v2) {
        // TODO - finish me
        if (v1.isConstant() && v2.isConstant()) { // 如果v1和v2都是常量
            if (v1.equals(v2)) { // 如果v1和v2相等
                return Value.makeConstant(v1.getConstant()); // 返回v1的常量值
            } else {
                return Value.getNAC(); // 否则返回NAC
            }
        } else if (v1.isNAC() || v2.isNAC()) { // 如果v1或v2是NAC
            return Value.getNAC(); // 返回NAC
        } else if (v1.isConstant() && v2.isUndef()) { // 如果v1是常量且v2是未定义
            return Value.makeConstant(v1.getConstant()); // 返回v1的常量值
        } else if (v2.isConstant() && v1.isUndef()) { // 如果v2是常量且v1是未定义
            return Value.makeConstant(v2.getConstant()); // 返回v2的常量值
        }
        return Value.getUndef(); // 返回未定义
    }

    @Override
    public boolean transferNode(Stmt stmt, CPFact in, CPFact out) {
        // TODO - finish me
        AtomicBoolean changed = new AtomicBoolean(false); // 创建AtomicBoolean实例
        in.forEach(((var, value) -> { // 遍历in
            if (out.update(var, value)) { // 更新out
                changed.set(true); // 设置changed为true
            }
        }));
        if (stmt instanceof DefinitionStmt<?, ?> s) { // 如果stmt是DefinitionStmt的实例
            if (s.getLValue() instanceof Var var && canHoldInt(var)) { // 如果LValue是Var类型且可以存储整数
                CPFact inCopy = in.copy(); // 复制in
                Value removedVal = inCopy.get(var); // 获取var的值
                inCopy.remove(var); // 移除var
                Value newVal = evaluate(s.getRValue(), in); // 计算RValue的值
                out.update(var, newVal); // 更新out
                return !removedVal.equals(newVal) || changed.get(); // 返回是否更改
            }
        }
        return changed.get(); // 返回changed
    }

    /**
     * @return true if the given variable can hold integer value, otherwise false.
     */
    public static boolean canHoldInt(Var var) {
        Type type = var.getType();
        if (type instanceof PrimitiveType) {
            switch ((PrimitiveType) type) {
                case BYTE:
                case SHORT:
                case INT:
                case CHAR:
                case BOOLEAN:
                    return true;
            }
        }
        return false;
    }

    /**
     * Evaluates the {@link Value} of given expression.
     *
     * @param exp the expression to be evaluated
     * @param in  IN fact of the statement
     * @return the resulting {@link Value}
     */
    public static Value evaluate(Exp exp, CPFact in) {
        // TODO - finish me
        if (exp instanceof IntLiteral e) { // 如果表达式是IntLiteral的实例
            return Value.makeConstant(e.getValue()); // 返回常量值
        } else if (exp instanceof Var var) { // 如果表达式是Var的实例
            if (in.get(var).isConstant()) { // 如果变量是常量
                return Value.makeConstant(in.get(var).getConstant()); // 返回常量值
            }
            return in.get(var); // 返回变量值
        } else if (exp instanceof BinaryExp b) { // 如果表达式是BinaryExp的实例
            Value v1 = evaluate(b.getOperand1(), in); // 计算第一个操作数的值
            Value v2 = evaluate(b.getOperand2(), in); // 计算第二个操作数的值
            if (v2.isConstant() && v2.getConstant() == 0
                    && b.getOperator() instanceof ArithmeticExp.Op op) { // 如果第二个操作数是常量且为0且操作符是除法或取余
                if (op == ArithmeticExp.Op.DIV || op == ArithmeticExp.Op.REM) {
                    return Value.getUndef(); // 返回未定义
                }
            }
            if (v1.isConstant() && v2.isConstant()) { // 如果两个操作数都是常量
                int c1 = v1.getConstant(), c2 = v2.getConstant(); // 获取常量值
                if (b.getOperator() instanceof ArithmeticExp.Op op) { // 如果操作符是算术操作符
                    switch (op) {
                        case ADD -> {
                            return Value.makeConstant(c1 + c2); // 返回相加结果
                        }
                        case SUB -> {
                            return Value.makeConstant(c1 - c2); // 返回相减结果
                        }
                        case MUL -> {
                            return Value.makeConstant(c1 * c2); // 返回相乘结果
                        }
                        case DIV -> {
                            return Value.makeConstant(c1 / c2); // 返回相除结果
                        }
                        case REM -> {
                            return Value.makeConstant(c1 % c2); // 返回取余结果
                        }
                    }
                } else if (b.getOperator() instanceof ShiftExp.Op op) { // 如果操作符是移位操作符
                    switch (op) {
                        case SHL -> {
                            return Value.makeConstant(c1 << c2); // 返回左移结果
                        }
                        case SHR -> {
                            return Value.makeConstant(c1 >> c2); // 返回右移结果
                        }
                        case USHR -> {
                            return Value.makeConstant(c1 >>> c2); // 返回无符号右移结果
                        }
                    }
                } else if (b.getOperator() instanceof BitwiseExp.Op op) { // 如果操作符是按位操作符
                    switch (op) {
                        case OR -> {
                            return Value.makeConstant(c1 | c2); // 返回按位或结果
                        }
                        case AND -> {
                            return Value.makeConstant(c1 & c2); // 返回按位与结果
                        }
                        case XOR -> {
                            return Value.makeConstant(c1 ^ c2); // 返回按位异或结果
                        }
                    }
                } else if (b.getOperator() instanceof ConditionExp.Op op) { // 如果操作符是条件操作符
                    switch (op) {
                        case EQ -> {
                            return Value.makeConstant(c1 == c2 ? 1 : 0); // 返回相等结果
                        }
                        case NE -> {
                            return Value.makeConstant(c1 != c2 ? 1 : 0); // 返回不相等结果
                        }
                        case LT -> {
                            return Value.makeConstant(c1 < c2 ? 1 : 0); // 返回小于结果
                        }
                        case GT -> {
                            return Value.makeConstant(c1 > c2 ? 1 : 0); // 返回大于结果
                        }
                        case LE -> {
                            return Value.makeConstant(c1 <= c2 ? 1 : 0); // 返回小于等于结果
                        }
                        case GE -> {
                            return Value.makeConstant(c1 >= c2 ? 1 : 0); // 返回大于等于结果
                        }
                    }
                }
            } else if (v1.isNAC() || v2.isNAC()) { // 如果任一操作数是NAC
                return Value.getNAC(); // 返回NAC
            }
            return Value.getUndef(); // 返回未定义
        }
        return Value.getNAC(); // 返回NAC
    }

}
