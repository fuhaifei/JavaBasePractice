package SpringSecurity;

/**
 * Spring Security
 *      * 基于Servlet的filter实现：拦截请求或者相应，对请求进行预处理/响应进行处理
 *          * 请求->filter chain->web资源->响应->filter chain(逆向)
 *      * Spring Security向其添加了一个FilterChainProxy过滤器，而这个过滤器只是一个代理过滤器，
 *          通过这个代理过滤器创建一套SpringSecurity自定义的过滤器链(认证与授权过滤器就在这过滤器链中)
 *      * 主要的几个对象
 *          * SecurityContextHolder：用于拿到上下文对象的静态工具类
 *          * SecurityContext：上下文对象，Authentication对象会放在里面
 *          * Authentication：认证接口，存储了认证信息，代表当前登录用户
 *          * AuthenticationManager：用于校验Authentication，返回一个认证完成后的Authentication对象,默认使用的实现类为：ProviderManager
 *      * 维护了一个filter chain
 *          * ChannelProcessingFilter
 *          * SecurityContextPersistenceFilter（HttpSessionSecurityContextRepository）:
 *              * 请求进来：从session中获取SecurityContext，存放在SecurityContextHolder的ThreadLocal变量中
 *              * 请求走：将SecurityContext持久化到Session中
 *          * UsernamePasswordAuthenticationFilter(formLogin)
 *              * 调用UserDetailsService对用户表单用户名密码等的及进行验证,只对post请求方式的"/login"接口有效
 *              * 构造一个 Username Password Authentication Token,调用AuthenticationManager.authenticate方法，
 *                  传入username和password,获取Authentication对象
 *              * AuthenticationManager.authenticate -> DaoAuthenticationProvider ->
 *                  UserDetailsService.loadUserByUsername() -> UserDetails -> 将Authentication存放在SecurityContext
 *              * 验证失败-> failHandler.onAuthenticFailure(); 验证成功:successHandler.onAuthenticationSuccess()
 *         * BasicAuthenticationFilter(httpBasic)
 *              * 当一个HTTP请求中包含一个名字为Authorization的头部，并且其值格式是Basic xxx时，
 *                 该Filter会认为这是一个BASIC authorization头部
 *         * ExceptionTranslationFilter
 *              * 位于整个springSecurityFilterChain的后方，用来转换整个链路中出现的异常，将其转化
 *                (AccessDeniedException访问异常和AuthenticationException认证异常)
 *              * 重定向到登录页面/403错误等
 *         * FilterSecurityInterceptor:对验证成功的用户进行鉴权
 *              * 集成抽象类AbstractSecurityInterceptor
 *              * 将request对象封装到FilterInvocation对象->调用super.beforeInvocation()-> 获取Authentication对象+配置过滤规则
 *                -> 调用accessDecisionManager.decide(authenticated, object, attributes)进行权限验证
 *                -> AccessDecisionManager:三个实现类（AffirmativeBased 一票通过，UnanimousBased 一票否决，ConsensusBased 少数服从多数）
 *                -> 遍历投票器 AccessDecisionVoter.vote(投票器)，根据投票器的票判断鉴权是否通过
 * 认证实现
 *  * 重写UserDetailsService
 *      * 构建UserDetail对象(用户名/密码/角色信息)
 *
 *  JWT(Json Web Token)：规定了数据传输格式
 *      * 为什么要使用JWT token？ 就是为多种终端设备，提供统一的、安全的令牌格式
 *      * 包含三个由点（.）分隔的部分，Header.Payload.Signature : xxxxx.yyyyy.zzzzz
 *          * Header:令牌的类型 + 散列算法
 *          * Payload：
 *
 * */

public class BasicUse {
}
