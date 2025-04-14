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

package pascal.taie.analysis.dataflow.analysis;

import pascal.taie.analysis.MethodAnalysis;
import pascal.taie.analysis.dataflow.analysis.constprop.CPFact;
import pascal.taie.analysis.dataflow.analysis.constprop.ConstantPropagation;
import pascal.taie.analysis.dataflow.analysis.constprop.Value;
import pascal.taie.analysis.dataflow.fact.DataflowResult;
import pascal.taie.analysis.dataflow.fact.SetFact;
import pascal.taie.analysis.graph.cfg.CFG;
import pascal.taie.analysis.graph.cfg.CFGBuilder;
import pascal.taie.analysis.graph.cfg.Edge;
import pascal.taie.config.AnalysisConfig;
import pascal.taie.ir.IR;
import pascal.taie.ir.exp.ArithmeticExp;
import pascal.taie.ir.exp.ArrayAccess;
import pascal.taie.ir.exp.CastExp;
import pascal.taie.ir.exp.FieldAccess;
import pascal.taie.ir.exp.NewExp;
import pascal.taie.ir.exp.RValue;
import pascal.taie.ir.exp.Var;
import pascal.taie.ir.stmt.AssignStmt;
import pascal.taie.ir.stmt.If;
import pascal.taie.ir.stmt.Stmt;
import pascal.taie.ir.stmt.SwitchStmt;
import pascal.taie.util.collection.Pair;

import java.util.*;

public class DeadCodeDetection extends MethodAnalysis {

    public static final String ID = "deadcode";

    public DeadCodeDetection(AnalysisConfig config) {
        super(config);
    }

    @Override
    public Set<Stmt> analyze(IR ir) {
        // obtain CFG
        CFG<Stmt> cfg = ir.getResult(CFGBuilder.ID);
        // obtain result of constant propagation
        DataflowResult<Stmt, CPFact> constants =
                ir.getResult(ConstantPropagation.ID);
        // obtain result of live variable analysis
        DataflowResult<Stmt, SetFact<Var>> liveVars =
                ir.getResult(LiveVariableAnalysis.ID);
        // keep statements (dead code) sorted in the resulting set
        Set<Stmt> deadCode = new TreeSet<>(Comparator.comparing(Stmt::getIndex));
        // TODO - finish me
        // Your task is to recognize dead code in ir and add it to deadCode
        Set<Stmt> liveCode = new TreeSet<>(Comparator.comparing(Stmt::getIndex)); // 存储活跃代码
        Queue<Stmt> queue = new LinkedList<>(); // 使用队列进行广度优先搜索

        queue.add(cfg.getEntry()); // 从入口节点开始
        while (!queue.isEmpty()) {
            Stmt stmt = queue.poll();
            if (stmt instanceof AssignStmt<?, ?> s && s.getLValue() instanceof Var var) { // 如果是赋值语句且左值是变量
                if (!liveVars.getResult(stmt).contains(var) && hasNoSideEffect(s.getRValue())) { // 如果变量不在活跃变量集合中且右值没有副作用
                    queue.addAll(cfg.getSuccsOf(stmt)); // 将后继节点加入队列
                    continue; // 继续处理下一个节点
                }
            }
            if (!liveCode.add(stmt)) { // 如果当前节点已经在活跃代码集合中，跳过
                continue;
            }
            if (stmt instanceof If s) { // 如果是条件语句
                Value cond = ConstantPropagation.evaluate(s.getCondition(), constants.getInFact(stmt));
                if (cond.isConstant()) { // 如果条件是常量
                    for (Edge<Stmt> edge : cfg.getOutEdgesOf(stmt)) { // 遍历所有出边
                        if ((cond.getConstant() == 1 && edge.getKind() == Edge.Kind.IF_TRUE) ||
                                (cond.getConstant() == 0 && edge.getKind() == Edge.Kind.IF_FALSE)) { // 如果条件为真，添加真分支的后继节点
                            queue.add(edge.getTarget());
                        }
                    }
                } else { // 如果条件不是常量，处理所有后继节点
                    queue.addAll(cfg.getSuccsOf(stmt));
                }
            } else if (stmt instanceof SwitchStmt s) { // 如果是 switch 语句
                Value val = ConstantPropagation.evaluate(s.getVar(), constants.getInFact(stmt)); // 计算 switch 变量的值
                if (val.isConstant()) { // 如果值是常量
                    boolean hit = false;
                    for (Pair<Integer, Stmt> pair : s.getCaseTargets()) { // 遍历所有 case 分支
                        if (pair.first() == val.getConstant()) { // 如果 case 分支匹配
                            hit = true;
                            queue.add(pair.second()); // 添加匹配的 case 分支的后继节点
                        }
                    }
                    if (!hit) { // 如果没有匹配的 case 分支，处理默认分支
                        queue.add(s.getDefaultTarget());
                    }
                } else { // 如果值不是常量，处理所有后继节点
                    queue.addAll(cfg.getSuccsOf(stmt));
                }
            } else { // 处理其他类型的语句的后继节点
                queue.addAll(cfg.getSuccsOf(stmt));
            }
        }
        deadCode.addAll(cfg.getNodes()); // 将 CFG 中的所有节点添加到死代码集合中
        deadCode.removeAll(liveCode); // 从死代码集合中移除活跃代码
        deadCode.remove(cfg.getExit()); // 移除 exit 节点
        return deadCode;
    }

    /**
     * @return true if given RValue has no side effect, otherwise false.
     */
    private static boolean hasNoSideEffect(RValue rvalue) {
        // new expression modifies the heap
        if (rvalue instanceof NewExp ||
                // cast may trigger ClassCastException
                rvalue instanceof CastExp ||
                // static field access may trigger class initialization
                // instance field access may trigger NPE
                rvalue instanceof FieldAccess ||
                // array access may trigger NPE
                rvalue instanceof ArrayAccess) {
            return false;
        }
        if (rvalue instanceof ArithmeticExp) {
            ArithmeticExp.Op op = ((ArithmeticExp) rvalue).getOperator();
            // may trigger DivideByZeroException
            return op != ArithmeticExp.Op.DIV && op != ArithmeticExp.Op.REM;
        }
        return true;
    }
}
