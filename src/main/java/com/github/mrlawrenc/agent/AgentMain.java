package com.github.mrlawrenc.agent;

import com.alibaba.fastjson.JSON;
import com.github.mrlawrenc.entity.JsonAgentArg;
import com.github.mrlawrenc.interceptor.AgentMainInterceptor;
import com.google.common.collect.HashBasedTable;
import com.sun.tools.attach.VirtualMachine;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.instrument.ClassDefinition;
import java.lang.instrument.Instrumentation;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * @author : MrLawrenc
 * date : 2020/6/5 22:05
 */
public class AgentMain {
    static boolean enableHot = false;


    /**
     * System.getProperties().put("sun.misc.ProxyGenerator.saveGeneratedFiles", "true"); 保存代理类
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
    public static void agentmain(String agentArgs, Instrumentation inst) {
        System.out.println("#####################agentmain  start#####################");
        System.out.println("agent args : " + agentArgs);
        JsonAgentArg jsonAgentArg = JSON.parseObject(agentArgs, JsonAgentArg.class);


        if (jsonAgentArg.isEnableHotUpdate()) {
            try {
                hot(jsonAgentArg.getRedefinePath()[0], inst);
            } catch (Exception exception) {
                exception.printStackTrace();
            }
        } else {
            try {
                /**
                 * 只会获得jvm已经加载的类，比如在main方法中还未使用的类，则不会被加载,如以下示例
                 * <pre>
                 *      public static void main(String[] args) throws InterruptedException {
                 *
                 *         String name = ManagementFactory.getRuntimeMXBean().getName();
                 *         String s = name.split("@")[0];
                 *         //打印当前Pid
                 *         System.out.println("pid:" + s);
                 *         new HelloServiceImpl().sayHello();
                 *         TimeUnit.SECONDS.sleep(2);
                 *         new Test1().test1();
                 *     }
                 * </pre>
                 * 当大爷sayHello方法的时候 立马使用attach注入我们的agent包，则inst.getAllLoadedClasses();不会获得Test1的class
                 */
                Class<?>[] allLoadedClasses = inst.getAllLoadedClasses();
                List<Class<?>> result = new ArrayList<>();
                for (Class<?> aClass : allLoadedClasses) {
                    if (inst.isModifiableClass(aClass) && aClass.getName().startsWith("com.swust")) {
                        System.out.println("重载:" + aClass.getName() + "  当前类加载器:" + aClass.getClassLoader());
                        result.add(aClass);
                    }
                }
                inst.addTransformer(new AgentMainInterceptor(), true);
                inst.retransformClasses(result.toArray(Class[]::new));
            } catch (Exception e) {
                e.printStackTrace();
            }
            System.out.println("#####################agentmain  end#####################");
        }
    }

    public static void agentmain(String agentArgs) {
        System.out.println("attach start ....");
    }

    /**
     * @param path     检索的目标路径
     * @param fileList 存储所有class文件
     * @param jarList  存储所有的jar文件
     */
    public static void getAllFileName(String path, ArrayList<File> fileList, ArrayList<File> jarList) {
        File parent = new File(path);
        File[] tempList = parent.listFiles();
        if (tempList == null) {
            return;
        }
        for (File file : tempList) {
            if (file.isFile()) {
                if (file.getName().endsWith(".jar")) {
                    jarList.add(file);
                }
                if (file.getName().endsWith(".class")) {
                    fileList.add(file);
                }
            }
            if (file.isDirectory()) {
                getAllFileName(file.getAbsolutePath(), fileList, jarList);
            }
        }
    }

    public static void hot(String agentArgs, Instrumentation inst) throws Exception {

        HashBasedTable<String, JarEntry, JarFile> table = HashBasedTable.create(128, 1);
        if (agentArgs == null) {
            throw new NullPointerException("Target dir is  null");
        }
        System.out.println("Target dir is " + agentArgs);
        ArrayList<File> clzList = new ArrayList<>();
        ArrayList<File> jarList = new ArrayList<>();
        getAllFileName(agentArgs, clzList, jarList);


        //当前环境加载的所有class
        Class<?>[] allLoadedClasses = inst.getAllLoadedClasses();

        jarList.forEach(jar -> {
            try {
                jar2Map(table, jar);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        //更新已加载的类
        for (Class<?> aClass : allLoadedClasses) {
            if (aClass.getName().startsWith("com.swust")) {
                //更新.class文件
                System.out.println("准备更新的class：" + aClass + " clzList：" + clzList.size());
                for (File currentFile : clzList) {
                    if (currentFile.getName().contains(aClass.getSimpleName())) {
                        System.out.println("Update class(.class)  start : " + currentFile.getName());
                        byte[] bytes = new FileInputStream(currentFile).readAllBytes();


                        ClassDefinition definition = new ClassDefinition(aClass, bytes);
                        inst.redefineClasses(definition);
                        System.out.println("Update class(.class)  end :" + currentFile.getName());
                    }
                }

                //更新jar包
                for (String jarClzFullName : table.rowKeySet()) {
                    if (jarClzFullName.equals(aClass.getName())) {
                        System.out.println("Update class(jar) start : " + jarClzFullName);

                        Map<JarEntry, JarFile> row = table.row(jarClzFullName);
                        JarEntry jarEntry = row.keySet().iterator().next();

                        byte[] bytes = row.get(jarEntry).getInputStream(jarEntry).readAllBytes();

                        ClassDefinition definition = new ClassDefinition(aClass, bytes);
                        inst.redefineClasses(definition);
                        System.out.println("Update class(jar) end : " + jarClzFullName);
                    }
                }
            }
        }

    }

    public static void jar2Map(HashBasedTable<String, JarEntry, JarFile> table, File jar) throws IOException {
        JarFile jarFile = new JarFile(jar);
        Enumeration<JarEntry> entries = jarFile.entries();
        while (entries.hasMoreElements()) {
            JarEntry jarEntry = entries.nextElement();
            String realName = jarEntry.getRealName();
            if (!jarEntry.isDirectory() && realName.endsWith(".class")) {
                String fullClzName = realName.replaceAll("/", ".").split("\\.class")[0];
                table.put(fullClzName, jarEntry, jarFile);
            }
        }
    }


   /* public static void main(String[] args) throws Exception {
        File file = new File("F:\\openSources\\java-agent\\target\\java-agent-1.0-SNAPSHOT.jar");
        JarFile jarFile = new JarFile(file);
        Enumeration<JarEntry> entries = jarFile.entries();
        while (entries.hasMoreElements()) {
            JarEntry jarEntry = entries.nextElement();
            String realName = jarEntry.getRealName();
            if (!jarEntry.isDirectory() && realName.endsWith(".class")) {
                String[] split = realName.split("/");
                String clzName = split[split.length - 1];

                byte[] jarByte = jarFile.getInputStream(jarEntry).readAllBytes();
                Thread.currentThread().getContextClassLoader().loadClass(realName.replaceAll("/", "."));
                System.out.println(clzName);
            }
        }
        System.out.println(file.listFiles());
    }*/

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