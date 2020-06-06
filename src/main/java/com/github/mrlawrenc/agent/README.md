## agent方式基本不会导致源程序崩溃，除非oom
## 若使用一个class pool存储的类过多，则会造成oom，此时推荐每次使用再创建oom，以便于jvm回收
## 若类以加载，想要使用inst.retransformClasses(r);方法重新装载目标类，
## 并且需要对重新装载的类进行拦截，使用 inst.addTransformer(new AgentMainInterceptor(),true); 方法时，第二个参数必须显示指定为true，否则不会触发拦截