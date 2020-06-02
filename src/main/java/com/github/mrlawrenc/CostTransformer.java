package com.github.mrlawrenc;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.Modifier;

import java.io.ByteArrayInputStream;
import java.lang.instrument.ClassFileTransformer;
import java.security.ProtectionDomain;

/**
 * @author : MrLawrenc
 * @date : 2020/6/2 23:06
 * @description : javassist修改字节码
 *
 * 若agentOps不为空，则根据agentOps值来对所有类的方法进行拦截
 */
public class CostTransformer implements ClassFileTransformer {
    private String agentOps;

    public CostTransformer(String agentOps) {
        if (null != agentOps) {
            agentOps = agentOps.replaceAll("\\.", "/");
        }
        this.agentOps = agentOps;
    }

    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
                            ProtectionDomain protectionDomain, byte[] classfileBuffer) {

        if (agentOps != null && !className.startsWith(agentOps)) {
            return classfileBuffer;
        }

        // 这里我们限制下，只针对目标包下进行耗时统计
        if (className.startsWith("com/sun") || className.startsWith("javax") || !className.startsWith("com")) {
            return classfileBuffer;
        }

        CtClass cl = null;
        try {
            ClassPool classPool = ClassPool.getDefault();
            cl = classPool.makeClass(new ByteArrayInputStream(classfileBuffer));

            for (CtMethod method : cl.getDeclaredMethods()) {
                if (isNative(method)) {
                    continue;
                }
                // 所有方法，统计耗时；请注意，需要通过`addLocalVariable`来声明局部变量
                method.addLocalVariable("start", CtClass.longType);
                method.insertBefore("start = System.currentTimeMillis();");
                String methodName = method.getLongName();
                method.insertAfter("System.out.println(\"" + methodName + " cost: \" + (System" +
                        ".currentTimeMillis() - start));");
            }

            byte[] transformed = cl.toBytecode();
            return transformed;
        } catch (Exception ignored) {

        }
        return classfileBuffer;
    }

    public boolean isNative(CtMethod method) {
        return Modifier.isNative(method.getModifiers());
    }
}
