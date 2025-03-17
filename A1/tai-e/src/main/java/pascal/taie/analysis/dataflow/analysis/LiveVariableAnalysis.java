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

import pascal.taie.analysis.dataflow.fact.SetFact;
import pascal.taie.analysis.graph.cfg.CFG;
import pascal.taie.config.AnalysisConfig;
import pascal.taie.ir.exp.Exp;
import pascal.taie.ir.exp.LValue;
import pascal.taie.ir.exp.RValue;
import pascal.taie.ir.exp.Var;
import pascal.taie.ir.stmt.Stmt;

/**
 * Implementation of classic live variable analysis.
 */
public class LiveVariableAnalysis extends
        AbstractDataflowAnalysis<Stmt, SetFact<Var>> {

    public static final String ID = "livevar";

    public LiveVariableAnalysis(AnalysisConfig config) {
        super(config);
    }

    @Override
    public boolean isForward() {
        return false;
    }

    @Override
    public SetFact<Var> newBoundaryFact(CFG<Stmt> cfg) {
        // TODO - finish me
        // backwards的边界节点是 exit节点，将其初始化为空
        return new SetFact<Var>();
    }

    @Override
    public SetFact<Var> newInitialFact() {
        // TODO - finish me
        // 除了exit节点的其他节点初始化为空
        return new SetFact<Var>();
    }

    @Override
    public void meetInto(SetFact<Var> fact, SetFact<Var> target) {
        // TODO - finish me
        // 将这两个并起来
        target.union(fact);
    }

    @Override
    public boolean transferNode(Stmt stmt, SetFact<Var> in, SetFact<Var> out) {
        // TODO - finish me
        // 复制出集合，初始化新的 InFact
        SetFact<Var> newInFact = new SetFact<>();
        // 将 out 中的变量全部复制到 newInFact 中
        newInFact.union(out);
        // 如果语句定义了一个变量，移除该变量（定义操作覆盖之前的值）
        if (stmt.getDef().isPresent()) {
            LValue def = stmt.getDef().get();
            if (def instanceof Var) {
                newInFact.remove((Var) def);
            }
        }
        // 遍历语句中的所有使用变量，将它们添加到 newInFact 中
        for (RValue use : stmt.getUses()) {
            if (use instanceof Var) {
                newInFact.add((Var) use);
            }
        }
        // 如果新的入集合与原入集合不同，就更新 in 并返回 true（表示集合有变化）
        if (!newInFact.equals(in)) {
            in.set(newInFact);
            return true;
        }
        // 否则返回 false，表示集合没有变化
        return false;
    }
}
