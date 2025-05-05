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

package pascal.taie.analysis.dataflow.inter;

import pascal.taie.analysis.dataflow.analysis.constprop.CPFact;
import pascal.taie.analysis.dataflow.fact.DataflowResult;
import pascal.taie.analysis.graph.icfg.ICFG;
import pascal.taie.analysis.graph.icfg.ICFGEdge;
import pascal.taie.util.collection.SetQueue;

import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Solver for inter-procedural data-flow analysis.
 * The workload of inter-procedural analysis is heavy, thus we always
 * adopt work-list algorithm for efficiency.
 */
class InterSolver<Method, Node, Fact> {

    private final InterDataflowAnalysis<Node, Fact> analysis;

    private final ICFG<Method, Node> icfg;

    private DataflowResult<Node, Fact> result;

    private Queue<Node> workList;

    InterSolver(InterDataflowAnalysis<Node, Fact> analysis,
                ICFG<Method, Node> icfg) {
        this.analysis = analysis;
        this.icfg = icfg;
    }

    DataflowResult<Node, Fact> solve() {
        result = new DataflowResult<>();
        initialize();
        doSolve();
        return result;
    }

    private void initialize() {
        // TODO - finish me
        for(Node node : icfg){ // 初始化数据流分析的结果
            result.setOutFact(node, analysis.newInitialFact()); // 为每个节点设置初始的输出数据流事实
        }
        icfg.entryMethods().forEach(method -> { // 为每个入口方法设置边界条件
            Node entry = icfg.getEntryOf(method); // 获取方法的入口节点
            result.setOutFact(entry, analysis.newBoundaryFact(entry)); // 设置入口节点的输出数据流事实为边界条件
        });
    }

    @SuppressWarnings("unchecked")
    private void doSolve() {
        // TODO - finish me
        Queue<Node> workList = new LinkedList<>(icfg.getNodes()); // 初始化工作列表，包含所有节点
        while(!workList.isEmpty()){ // 当工作列表不为空时，进行迭代
            Node node = workList.poll(); // 从工作列表中取出一个节点
            CPFact in = new CPFact(); // 创建一个新的输入数据流事实
            CPFact out = (CPFact) result.getOutFact(node); // 获取当前节点的输出数据流事实
            for(ICFGEdge<Node> edge : icfg.getInEdgesOf(node)){ // 遍历当前节点的所有入边，将入边的输出数据流事实与当前节点的输入数据流事实进行合并
                analysis.meetInto(analysis.transferEdge(edge, result.getOutFact(edge.getSource())), (Fact) in);
            }
            if(analysis.transferNode(node, (Fact) in, (Fact) out)){ // 如果节点的输入数据流事实与输出数据流事实发生了变化
                icfg.getSuccsOf(node).forEach(workList::offer); // 将当前节点的所有后继节点加入工作列表
            }
            result.setInFact(node, (Fact) in); // 设置当前节点的输入数据流事实
            result.setOutFact(node, (Fact) out); // 设置当前节点的输出数据流事实
        }
    }
}
