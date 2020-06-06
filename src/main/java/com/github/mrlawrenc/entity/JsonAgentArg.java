package com.github.mrlawrenc.entity;

import com.alibaba.fastjson.JSON;
import lombok.Data;

/**
 * @author : MrLawrenc
 * date  2020/6/6 18:00
 * agentArgs 带入的参数模型
 *
 * <p>
 * 参数格式:
 * <pre>
 *     {
 *     "enableHotUpdate":true,
 *     "redefinePath":[
 *         "D:/src/target/",
 *         "F:/target"],
 *     "retransformPkg":[
 *         "com.swust",
 *         "com.hello"]
 *      }
 * </pre>
 */
@Data
public class JsonAgentArg {

    /**
     * 是否开启热更新
     */
    private boolean enableHotUpdate;

    /**
     * 热更新监控的class路径
     */
    private String[] redefinePath;


    /**
     * 重新装载类的包限制
     */
    private String[] retransformPkg;


    public static void main(String[] args) {
        JsonAgentArg jsonAgentArg = new JsonAgentArg();

        jsonAgentArg.setEnableHotUpdate(true);
        jsonAgentArg.setRedefinePath(new String[]{"D://src/target/", "F://target"});
        jsonAgentArg.setRetransformPkg(new String[]{"com.swust", "com.hello"});
        System.out.println(JSON.toJSONString(jsonAgentArg));
    }
}