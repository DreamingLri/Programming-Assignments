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
import pascal.taie.analysis.dataflow.analysis.constprop.ConstantPropagation;
import pascal.taie.analysis.graph.cfg.CFG;
import pascal.taie.analysis.graph.cfg.CFGBuilder;
import pascal.taie.analysis.graph.icfg.CallEdge;
import pascal.taie.analysis.graph.icfg.CallToReturnEdge;
import pascal.taie.analysis.graph.icfg.NormalEdge;
import pascal.taie.analysis.graph.icfg.ReturnEdge;
import pascal.taie.config.AnalysisConfig;
import pascal.taie.ir.IR;
import pascal.taie.ir.exp.InvokeExp;
import pascal.taie.ir.exp.Var;
import pascal.taie.ir.stmt.Invoke;
import pascal.taie.ir.stmt.Stmt;
import pascal.taie.language.classes.JMethod;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Implementation of interprocedural constant propagation for int values.
 */
public class InterConstantPropagation extends
        AbstractInterDataflowAnalysis<JMethod, Stmt, CPFact> {

    public static final String ID = "inter-constprop";

    private final ConstantPropagation cp;

    public InterConstantPropagation(AnalysisConfig config) {
        super(config);
        cp = new ConstantPropagation(new AnalysisConfig(ConstantPropagation.ID));
    }

    @Override
    public boolean isForward() {
        return cp.isForward();
    }

    @Override
    public CPFact newBoundaryFact(Stmt boundary) {
        IR ir = icfg.getContainingMethodOf(boundary).getIR();
        return cp.newBoundaryFact(ir.getResult(CFGBuilder.ID));
    }

    @Override
    public CPFact newInitialFact() {
        return cp.newInitialFact();
    }

    @Override
    public void meetInto(CPFact fact, CPFact target) {
        cp.meetInto(fact, target);
    }

    @Override
    protected boolean transferCallNode(Stmt stmt, CPFact in, CPFact out) {
        // TODO - finish me
        AtomicBoolean changed = new AtomicBoolean(false); // 用于记录是否有更新发生
        in.forEach(((var, value) -> { // 遍历输入的每个变量和值，如果输出中更新了变量的值，则标记为已更改
            if(out.update(var, value)){
                changed.set(true);
            }
        }));
        return changed.get(); // 返回是否发生了更改
    }

    @Override
    protected boolean transferNonCallNode(Stmt stmt, CPFact in, CPFact out) {
        // TODO - finish me
        return cp.transferNode(stmt, in, out); // 调用常量传播的节点传递逻辑
    }

    @Override
    protected CPFact transferNormalEdge(NormalEdge<Stmt> edge, CPFact out) {
        // TODO - finish me
        return out; // 对普通边不做任何修改，直接返回输出
    }

    @Override
    protected CPFact transferCallToReturnEdge(CallToReturnEdge<Stmt> edge, CPFact out) {
        // TODO - finish me
        Invoke callSite = (Invoke) edge.getSource(); // 获取调用点
        Var lVar = callSite.getLValue(); // 获取调用点的左值变量
        CPFact result = out.copy(); // 复制输出作为结果
        if(lVar != null){
            result.remove(lVar); // 如果左值变量不为空，从结果中移除该变量
        }
        return result; // 返回更新后的结果
    }

    @Override
    protected CPFact transferCallEdge(CallEdge<Stmt> edge, CPFact callSiteOut) {
        // TODO - finish me
        Invoke callSite = (Invoke) edge.getSource(); // 获取调用点
        CPFact result = new CPFact(); // 创建一个新的结果
        List<Var> args = edge.getCallee().getIR().getParams(); // 获取被调用方法的参数列表
        assert args.size() == callSite.getRValue().getArgs().size(); // 确保参数数量匹配
        for(int i = 0;i < args.size();i ++){
            result.update(args.get(i), callSiteOut.get(callSite.getRValue().getArg(i))); // 将调用点的实参值映射到被调用方法的形参
        }
        return result; // 返回更新后的结果
    }

    @Override
    protected CPFact transferReturnEdge(ReturnEdge<Stmt> edge, CPFact returnOut) {
        // TODO - finish me
        CPFact result = new CPFact(); // 创建一个新的结果
        Invoke callSite = (Invoke) edge.getCallSite(); // 获取调用点
        Var lVar = callSite.getLValue(); // 获取调用点的左值变量
        if(lVar != null){ // 如果左值变量不为空，将返回值与现有值合并
            edge.getReturnVars().forEach(var -> {
                result.update(lVar, cp.meetValue(result.get(lVar), returnOut.get(var)));
            });
        }
        return result; // 返回更新后的结果
    }
}
