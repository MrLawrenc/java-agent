package com.github.mrlawrenc.agent;

import com.github.mrlawrenc.interceptor.PreMainInterceptor;

import java.lang.instrument.Instrumentation;

/**
 * @author : MrLawrenc
 * @date : 2020/6/2 22:52
 * premain和agentmain为两组方法，每组都会优先寻找两个参数的方法，没有再使用一个参数的方法，通过
 * <Premain-Class>com.github.mrlawrenc.Premain</Premain-Class>
 * <Agent-Class>com.github.mrlawrenc.Premain</Agent-Class>
 * 两个参数指定，当然也可以手动写在配置中
 * <p>
 * 使用
 * 1. 启动时加载  -javaagent:F:\JavaProject\2020\empty-web\target\java-agent-1.0-SNAPSHOT-jar-with-dependencies.jar 方式可以指定代理的jar
 * 若想加参数则为-javaagent:F:\target\java-agent.jar=this-is-args  this-is-args为参数值，会传递到agentOps
 * 2. 运行中加载 使用com.sun.tools.attach.VirtualMachine加载 <a herf="https://article.itxueyuan.com/GoLyw4"></a>
 */
public class PreMain {

    /**
     * 该方法在main方法之前运行，与main方法运行在同一个JVM中
     * 并被同一个System ClassLoader装载
     * 被统一的安全策略(security policy)和上下文(context)管理
     * <p>
     * 该方法首先执行，若没有则执行{@link PreMain#premain(String)}
     */
    public static void premain(String agentOps, Instrumentation inst) {
        System.out.println("premain doing..........");
        System.out.println("agent args : " + agentOps);
        //add方法会对所有类进行拦截
        inst.addTransformer(new PreMainInterceptor(agentOps), true);
    }

    /**
     * 如果不存在 premain(String agentOps, Instrumentation inst)
     * 则会执行 premain(String agentOps)
     */
    public static void premain(String agentOps) {
    }


}