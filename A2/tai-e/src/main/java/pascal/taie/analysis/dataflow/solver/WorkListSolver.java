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

package pascal.taie.analysis.dataflow.solver;

import pascal.taie.analysis.dataflow.analysis.DataflowAnalysis;
import pascal.taie.analysis.dataflow.analysis.constprop.CPFact;
import pascal.taie.analysis.dataflow.fact.DataflowResult;
import pascal.taie.analysis.graph.cfg.CFG;

import java.util.LinkedList;
import java.util.Queue;

class WorkListSolver<Node, Fact> extends Solver<Node, Fact> {

    WorkListSolver(DataflowAnalysis<Node, Fact> analysis) {
        super(analysis);
    }

    @Override
    protected void doSolveForward(CFG<Node> cfg, DataflowResult<Node, Fact> result) {
        // TODO - finish me
        Queue<Node> workList = new LinkedList<>(cfg.getNodes()); // 初始化工作列表，包含CFG中的所有节点
        while (!workList.isEmpty()) { // 当工作列表不为空时
            Node node = workList.poll(); // 从工作列表中取出一个节点
            CPFact in = new CPFact(); // 创建一个新的CPFact对象，表示输入
            CPFact out = (CPFact) result.getOutFact(node); // 获取当前节点的输出事实
            for (Node pred : cfg.getPredsOf(node)) { // 遍历当前节点的所有前驱节点
                analysis.meetInto(result.getOutFact(pred), (Fact) in); // 将前驱节点的输出事实合并到输入事实中
            }
            if (analysis.transferNode(node, (Fact) in, (Fact) out)) { // 如果传递函数改变了输出事实
                cfg.getSuccsOf(node).forEach(workList::offer); // 将当前节点的所有后继节点加入工作列表
            }
            result.setInFact(node, (Fact) in); // 设置当前节点的输入事实
            result.setOutFact(node, (Fact) out); // 设置当前节点的输出事实
        }
    }

    @Override
    protected void doSolveBackward(CFG<Node> cfg, DataflowResult<Node, Fact> result) {
        throw new UnsupportedOperationException();
    }
}
