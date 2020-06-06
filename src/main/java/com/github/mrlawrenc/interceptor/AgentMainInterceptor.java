package com.github.mrlawrenc.interceptor;

import javassist.ClassClassPath;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;

/**
 * @author : MrLawrenc
 * @date : 2020/6/6 14:05
 * <p>
 * 主要用于attach方式加入的agent包
 */
public class AgentMainInterceptor implements ClassFileTransformer {

    /*
     */
/**
 * 当需要重载的类过多时，会造成oom
 *//*

    private ClassPool classPool;

    */

    /**
     * 筛选需要改变字节码的class
     *//*

    private Function<Class<?>, Boolean> classRule;

    public AgentMainInterceptor(ClassPool classPool, Function<Class<?>, Boolean> classRule) {
        this.classPool = classPool;
        this.classRule = classRule;
    }
*/
    public AgentMainInterceptor() {
    }

    /**
     * 当 {@link AgentMainInterceptor#transform(Module, ClassLoader, String, Class, ProtectionDomain, byte[])}方法未被复写
     * 才执行该方法
     *
     * @param loader              当前使用的类加载器
     * @param className           类名 为 / 分割
     * @param classBeingRedefined 类
     * @param protectionDomain    domain
     * @param classfileBuffer     字节码二进制数据
     * @return 真正使用的字节码数据
     * @throws IllegalClassFormatException 异常
     */
/*    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {

        *//*if (!classRule.apply(classBeingRedefined)) {
            System.out.println("拒绝的类:" + classBeingRedefined);
            return classfileBuffer;
        }*//*

    }*/

    @Override
    public byte[] transform(Module module, ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {

        try {
            ClassPool classPool = new ClassPool(true);
            classPool.appendClassPath(new ClassClassPath(classBeingRedefined));
            try {
                System.out.println(classBeingRedefined + " ### " + classPool);
            } catch (Exception exception) {
                System.out.println("class:" + className);
                return classfileBuffer;
            }
            System.out.println(className+"||");
            CtClass cl = classPool.get(className.replaceAll("/", "."));
            System.out.println(className+"##"+cl);

            for (CtMethod method : cl.getDeclaredMethods()) {
                if (!PublicInterceptor.isNative(method)) {
                    method.addLocalVariable("start", CtClass.longType);
                    method.insertBefore("start = System.currentTimeMillis();");
                    String methodName = method.getLongName();
                    method.insertAfter("System.out.println(\"" + methodName + " cost: \" + (System" +
                            ".currentTimeMillis() - start));");
                }
            }
            return cl.toBytecode();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}