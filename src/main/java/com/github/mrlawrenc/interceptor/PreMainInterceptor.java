package com.github.mrlawrenc.interceptor;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;

import java.io.ByteArrayInputStream;
import java.lang.instrument.ClassFileTransformer;
import java.security.ProtectionDomain;

/**
 * @author : MrLawrenc
 * date : 2020/6/2 23:06
 * javassist修改字节码
 * <p>
 * 若agentOps不为空，则根据agentOps值来对所有类的方法进行拦截
 */
public class PreMainInterceptor implements ClassFileTransformer {
    private final String agentOps;

    public PreMainInterceptor(String agentOps) {
        if (null != agentOps) {
            agentOps = agentOps.replaceAll("\\.", "/");
        }
        this.agentOps = agentOps;
    }

    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
                            ProtectionDomain protectionDomain, byte[] classfileBuffer) {

        // 这里我们限制下
        if (agentOps != null && !className.startsWith(agentOps)) {
            return classfileBuffer;
        }
        if (!className.startsWith("com/swust")) {
            return classfileBuffer;
        }
        System.out.println("拦截到的类:" + className);
        try {
            ClassPool classPool = ClassPool.getDefault();
            CtClass cl = classPool.makeClass(new ByteArrayInputStream(classfileBuffer));


            for (CtMethod method : cl.getDeclaredMethods()) {
                if (PublicInterceptor.isNative(method)) {
                    continue;
                }
                // 所有方法，统计耗时；请注意，需要通过`addLocalVariable`来声明局部变量
                method.addLocalVariable("start", CtClass.longType);
                method.insertBefore("start = System.currentTimeMillis();");
                String methodName = method.getLongName();
                method.insertAfter("System.out.println(\"" + methodName + " cost: \" + (System" +
                        ".currentTimeMillis() - start));");
            }

            return cl.toBytecode();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return classfileBuffer;
    }

}
