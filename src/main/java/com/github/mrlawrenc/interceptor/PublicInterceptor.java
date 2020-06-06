package com.github.mrlawrenc.interceptor;

import javassist.CtMethod;
import javassist.Modifier;

/**
 * @author : MrLawrenc
 * date  2020/6/6 14:09
 * <p>
 * 拦截器相关公共方法
 */
public class PublicInterceptor {

    public static boolean isNative(CtMethod method) {
        return Modifier.isNative(method.getModifiers());
    }
}