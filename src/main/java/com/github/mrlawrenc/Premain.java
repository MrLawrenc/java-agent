package com.github.mrlawrenc;

import java.lang.instrument.Instrumentation;

/**
 * @author : MrLawrenc
 * @date : 2020/6/2 22:52
 * @description : TODO
 * <p>
 * 使用
 * -javaagent:F:\JavaProject\2020\empty-web\target\java-agent-1.0-SNAPSHOT-jar-with-dependencies.jar 方式可以指定代理的jar
 * 若想加参数则为-javaagent:F:\target\java-agent.jar=this-is-args  this-is-args为参数值，会传递到agentOps
 */
public class Premain {

    /**
     * 该方法在main方法之前运行，与main方法运行在同一个JVM中
     * 并被同一个System ClassLoader装载
     * 被统一的安全策略(security policy)和上下文(context)管理
     * <p>
     * 该方法首先执行，若没有则执行{@link Premain#premain(String)}
     */
    public static void premain(String agentOps, Instrumentation inst) {
        System.out.println("premain doing..........");
        System.out.println("agent args : " + agentOps);
        customLogic(agentOps, inst);
    }

    /**
     * 如果不存在 premain(String agentOps, Instrumentation inst)
     * 则会执行 premain(String agentOps)
     */
    public static void premain(String agentOps) {
    }

    /**
     * 动态 attach 方式启动，运行此方法
     * <p>
     * manifest需要配置属性Agent-Class
     */
    public static void agentmain(String agentArgs, Instrumentation inst) {
        System.out.println("attach start ....");
        customLogic(null, inst);
    }

    /**
     * 统计方法耗时
     */
    private static void customLogic(String agentOps, Instrumentation inst) {
        //add方法会对所有类进行拦截
        inst.addTransformer(new CostTransformer(agentOps), true);
    }

}