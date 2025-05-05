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

package pascal.taie.analysis.graph.callgraph;

import pascal.taie.World;
import pascal.taie.ir.proginfo.MethodRef;
import pascal.taie.ir.stmt.Invoke;
import pascal.taie.language.classes.ClassHierarchy;
import pascal.taie.language.classes.JClass;
import pascal.taie.language.classes.JMethod;
import pascal.taie.language.classes.Subsignature;

import java.util.*;

/**
 * Implementation of the CHA algorithm.
 */
class CHABuilder implements CGBuilder<Invoke, JMethod> {

    private ClassHierarchy hierarchy;

    @Override
    public CallGraph<Invoke, JMethod> build() {
        hierarchy = World.get().getClassHierarchy();
        return buildCallGraph(World.get().getMainMethod());
    }

    private CallGraph<Invoke, JMethod> buildCallGraph(JMethod entry) {
        DefaultCallGraph callGraph = new DefaultCallGraph();
        callGraph.addEntryMethod(entry);
        // TODO - finish me
        Queue<JMethod> workList = new LinkedList<>(); // 初始化一个工作列表，用于存储待处理的方法
        workList.add(entry); // 将入口方法加入工作列表
        while (!workList.isEmpty()){ // 遍历工作列表，直到所有方法都被处理
            JMethod method = workList.poll(); // 从工作列表中取出一个方法
            if(callGraph.contains(method)){ // 如果调用图中已经包含该方法，则跳过
                continue;
            }
            callGraph.addReachableMethod(method); // 将方法标记为可达方法
            method.getIR().getStmts().stream(). // 遍历方法的中间表示（IR）中的所有语句
                    filter(stmt -> stmt instanceof Invoke).forEach(stmt -> { // 过滤出调用语句
                Invoke callSite = (Invoke) stmt; // 将语句转换为调用点
                CallKind kind = null; // 确定调用的类型（接口调用、特殊调用、静态调用或虚拟调用）
                if(callSite.isInterface()) kind = CallKind.INTERFACE;
                else if(callSite.isSpecial()) kind = CallKind.SPECIAL;
                else if(callSite.isStatic()) kind = CallKind.STATIC;
                else if(callSite.isVirtual()) kind = CallKind.VIRTUAL;
                if(kind != null){ // 如果调用类型不为空，则解析调用点的目标方法
                    for(JMethod newMethod : resolve(callSite)){
                        callGraph.addEdge(new Edge<>(kind, callSite, newMethod)); // 将调用边添加到调用图中
                        workList.add(newMethod); // 将新解析的方法加入工作列表，以便后续处理
                    }
                }
            });
        }
        return callGraph; // 返回构建好的调用图
    }

    /**
     * Resolves call targets (callees) of a call site via CHA.
     */
    private Set<JMethod> resolve(Invoke callSite) {
        // TODO - finish me
        MethodRef methodRef = callSite.getMethodRef(); // 获取调用点的目标方法引用
        Set<JMethod> result = new HashSet<>(); // 用于存储解析出的目标方法的集合
        if(callSite.isInterface() || callSite.isVirtual()){ // 如果调用点是接口调用或虚拟调用
            JClass rootCls = methodRef.getDeclaringClass(); // 获取声明该方法的类
            Queue<JClass> queue = new LinkedList<>(); // 初始化一个队列，用于广度优先搜索类层次结构
            queue.add(rootCls); // 将根类加入队列
            while(!queue.isEmpty()){ // 遍历类层次结构
                JClass cls = queue.poll(); // 从队列中取出一个类
                JMethod dispatchedMethod = dispatch(cls, methodRef.getSubsignature()); // 根据类和方法的子签名分派目标方法
                if(dispatchedMethod != null){
                    result.add(dispatchedMethod); // 如果找到目标方法，将其加入结果集合
                }
                if(cls.isInterface()){ // 如果当前类是接口
                    queue.addAll(hierarchy.getDirectSubinterfacesOf(cls)); // 将直接子接口和直接实现类加入队列
                    queue.addAll(hierarchy.getDirectImplementorsOf(cls));
                }else{ // 如果是普通类，将直接子类加入队列
                    queue.addAll(hierarchy.getDirectSubclassesOf(cls));
                }
            }
        }else if(callSite.isSpecial()){ // 如果调用点是特殊调用
            JMethod method = dispatch(methodRef.getDeclaringClass(), methodRef.getSubsignature()); // 根据声明类和方法的子签名分派目标方法
            if(method != null) {
                result.add(method); // 如果找到目标方法，将其加入结果集合
            }
        }else if(callSite.isStatic()){ // 如果调用点是静态调用
            result.add(methodRef.getDeclaringClass().getDeclaredMethod(methodRef.getSubsignature())); // 直接获取声明类中定义的目标方法
        }
        return result; // 返回解析出的目标方法集合
    }

    /**
     * Looks up the target method based on given class and method subsignature.
     *
     * @return the dispatched target method, or null if no satisfying method
     * can be found.
     */
    private JMethod dispatch(JClass jclass, Subsignature subsignature) {
        // TODO - finish me
        JMethod method = jclass.getDeclaredMethod(subsignature); // 根据给定的类和方法的子签名查找目标方法
        if(method != null && !method.isAbstract()){ // 如果在当前类中找到目标方法且该方法不是抽象方法，则直接返回
            return method;
        }else if(jclass.getSuperClass() == null){ // 如果当前类没有父类（即已经到达类层次结构的顶层），则返回null
            return null;
        }
        return dispatch(jclass.getSuperClass(), subsignature); // 如果当前类中未找到目标方法，则递归到父类中查找
    }
}
