package com.github.mrlawrenc;

import com.sun.tools.attach.VirtualMachine;

import java.io.File;
import java.io.FileInputStream;
import java.lang.instrument.ClassDefinition;
import java.lang.instrument.Instrumentation;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

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
        //add方法会对所有类进行拦截
        inst.addTransformer(new Interceptor(agentOps), true);
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
     * <p>
     * 该方法用于在运行时使用agent,获取到需要重载的类，并对这些类进行拦截
     * <p>
     * 其中整体的执行依赖于VMThread，VMThread是一个在虚拟机创建时生成的单例原生线程，这个线程能派生出其他线程。同时，这个线程的主要的作用是维护一个vm操作队列(VMOperationQueue)，用于处理其他线程提交的vm operation，比如执行GC等。
     * <p>
     * VmThread在执行一个vm操作时，先判断这个操作是否需要在safepoint下执行。若需要safepoint下执行且当前系统
     * 不在safepoint下，则调用SafepointSynchronize的方法驱使所有线程进入safepoint中，再执行vm操作。
     * 执行完后再唤醒所有线程。若此操作不需要在safepoint下，或者当前系统已经在safepoint下，则可以直接执行该操作了。
     * 所以，在safepoint的vm操作下，只有vm线程可以执行具体的逻辑，其他线程都要进入safepoint下并被挂起，直到完成此次操作。
     */
    public static void agentmain(String agentArgs, Instrumentation inst)   {
        System.out.println("agentmain  start..............");
        if (true) {
            try {
                hot(agentArgs, inst);
            } catch (Exception exception) {
                exception.printStackTrace();
            }
        }
        try {
            List<Class<?>> retransformClasses = new LinkedList<>();
            Class<?>[] loadedClass = inst.getAllLoadedClasses();
            for (Class<?> aClass : loadedClass) {
                if (aClass.getName().contains("com.example")) {
                    System.out.println(aClass.getName());
                    retransformClasses.add(aClass);
                }
            }

            for (Class<?> result : retransformClasses) {
                inst.addTransformer(new Interceptor(agentArgs), true);
                inst.retransformClasses(result);
            }

            /*inst.addTransformer(new Interceptor(agentArgs));
            Class<?>[] classes = new Class[retransformClasses.size()];
            inst.retransformClasses(retransformClasses.toArray(classes));*/
            System.out.println("agentmain end..............");
        } catch (Exception ignored) {
        }
    }

    public static void agentmain(String agentArgs) {
        System.out.println("attach start ....");
    }


    public static void hot(String agentArgs, Instrumentation inst) throws Exception {
        Class[] allLoadedClasses = inst.getAllLoadedClasses();

        for (Class aClass : allLoadedClasses) {
            if (aClass.getName().equals("com.swust.HelloServiceImpl")) {
                File file = new File("F:\\openSources\\test\\out\\production\\test\\com\\swust\\HelloServiceImpl.class");
                byte[] bytes = new FileInputStream(file).readAllBytes();
                System.out.println("size:" + bytes.length);

                ClassDefinition definition = new ClassDefinition(aClass, bytes);
                inst.redefineClasses(definition);
            }
        }

    }

    public void attach() {
        try {
            String agentJarPath = "E:\\openSource\\java-agent\\target\\java-agent-1.0-SNAPSHOT-jar-with-dependencies.jar";
            //main方法名
            String applicationName = "Test";
            //查到需要监控的进程
            Optional<String> jvmProcessOpt = Optional.ofNullable(VirtualMachine.list()
                    .stream()
                    .filter(jvm -> {
                        System.out.println("jvm:" + jvm.displayName());
                        return jvm.displayName().contains(applicationName);
                    }).findFirst().get().id());

            if (jvmProcessOpt.isEmpty()) {
                System.out.println("Target Application not found");
                return;
            }

            String jvmPid = jvmProcessOpt.get();
            System.out.println("Attaching to target JVM with PID: " + jvmPid);
            VirtualMachine jvm = VirtualMachine.attach(jvmPid);
            jvm.loadAgent(agentJarPath);
            jvm.detach();
            System.out.println("Attached to target JVM and loaded Java agent successfully");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

}