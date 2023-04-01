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
 *                AccessDeniedException访问异常 -> AccessDeniedHandler
 *                AuthenticationException认证异常 -> AuthenticationEntryPoint
 *                  * AuthenticationEntryPoint
 *                  * AccessDeniedHandler：默认抛出403Forbbiden
 *              * 重定向到登录页面/403错误等
 *         * FilterSecurityInterceptor:对验证成功的用户进行鉴权
 *              * 集成抽象类AbstractSecurityInterceptor
 *              * 将request对象封装到FilterInvocation对象->调用super.beforeInvocation()-> 获取Authentication对象+配置过滤规则
 *                -> 调用accessDecisionManager.decide(authenticated, object, attributes)进行权限验证
 *                -> AccessDecisionManager:三个实现类（AffirmativeBased 一票通过，UnanimousBased 一票否决，ConsensusBased 少数服从多数）
 *                -> 遍历投票器 AccessDecisionVoter.vote(投票器)，根据投票器的票判断鉴权是否通过
 * 认证实现
 *  * 重写UserDetailsService的loadUserByUsername()方法
 *      * 构建UserDetail对象(用户名/密码/角色信息)—>访问数据库，查询用户表和role表
 *  * 自定义登录方法
 *      * config类中注入authenticationManager
 *      * 生成UsernamePasswordAuthenticationToken对象，调用authenticationManagerauthenticate验证用户信息
 *      * 验证成功后，将用户信息存放在缓存中后 + 将authentication对象存放在SecurityContextHolder.getContext().setAuthentication()
 *      * 生成JWT Token写入到响应体返回给用户
 *  * 自定义JetAuthenticationTokenFilter
 *      * 从请求中获取JWT token,解析得到其中的登录账号，账号不为空时从缓存中获取用户信息
 *      * 若用户信息不为空，则生成UsernamePasswordAuthenticationToken，设置authentication()完成授权
 *      * .addFilterBefore(jwtAuthenticationTokenFilter(), UsernamePasswordAuthenticationFilter.class) 配置文件中添加到表单
 *        表单登录过滤器之前
 *  * 退出登录方法
 *      * 删除缓存中的用户信息
 *      * 删除SecurityContext中的authentication对象
 *  * 刷新token方法
 *      * 生成新token，更新缓存过期时间
 * 鉴权流程（基于自定义投票器的方式）
 *      * 实现AccessDecisionVoter<FilterInvocation>接口，实现自定义投票器
 *          * 从缓存中获取用户角色信息+数据查询url可访问角色+遍历判断是否存在于当前用户角色（若角色为ALL，允许所有人通过）
 *          * 根据是否包含返回：ACCESS_GRANTED 或者 ACCESS_DENIED;
 *      * 配置bean,设置 UnanimousBased（WebExpressionVoter() + 自定义动态权限加载）
 *      * 配置文件中设置：login.permitAll();其他任何请求必须经过认证后才能访问 .anyRequest().authenticated()
 * 异常处理
 *  * AuthenticationException -> 自定义EntryPoint类，返回 授权失败信息（commerce方法）
 *  * AccessDeniedHandler -> 返回 权限不足 错误（handle方法）
 *
 * JWT(Json Web Token)：规定了数据传输格式
 *      * 为什么要使用JWT token？ 就是为多种终端设备，提供统一的、安全的令牌格式
 *      * 包含三个由点（.）分隔的部分，Header.Payload.Signature : xxxxx.yyyyy.zzzzz
 *  *          * Header: 令牌的类型 + 加密算法类型
 *  *          * Payload：内容也是一个Json对象，它是存放有效信息的地方，它可以存放JWT提供的现成字段，
 *  *                      比 如：iss（签发者），exp（过期时间戳）， sub（面向的用户）等，也可自定义字段
 *  *          * Signature: 签名，用于防止JWT内容被篡改
 *  *             *   base64UrlEncode(header). base64UrlEncode(payload).secret
 *             & SignatureAlgorithm.HS512
 *      * 基于io.jsonwebtoken实现，负载中只存储用户名，其他密码，角色等信息存放在redis缓存中
 *      * JWT token存放在请求header的Authorization属性中
 * 跨域问题
 *      * 浏览器有同源策略限制，当前域名的js只能读取同域下的窗口属性，同域的定义为： 协议，域名，端口
 *      * No 'Access-Control-Allow-Origin' header is present on the requested resource
 *      * 解决方案：
 *          1. nginx作为代理服务器，所有请求都经过nginx
 *          2. CROS(Cross-origin resource sharing):跨域资源共享
 *              * 普通请求发送时（HEAD/GET/POST）发送发在头部添加Origin字段，表明跨域请求的字段
 *                  * Origin: http://xx.com
 *              * 非普通请求（请求方式是 PUT、DELETE，或者 Content-Type 字段类型是 application/json）,非简单请求的 CORS请求，
 *                  会在正式通信之前，增加一次 HTTP 查询请求，称为预检请求（preflight）,预检请求用的请求方法是 OPTIONS
 *              * 服务端在响应时，在响应添加对应属性，标示接收该域的跨域请求
 *                  Access-Control-Allow-Origin        允许请求的域
 *                  Access-Control-Allow-Methods        允许请求的方法
 *                  Access-Control-Allow-Headers        预检请求后，告知发送请求需要有的头部
 *                  Access-Control-Allow-Credentials        表示是否允许发送cookie，默认false；
 *                  Access-Control-Max-Age       本次预检的有效期，单位：秒；
 *      * Java中基于CROS的实现方案
 *          1. @CrossOrigin 注解当修饰类时，表示此类中的所有接口都可以跨域；当修饰方法时，表示此方法可以跨域
 *              * 问题是支支持局部方法（某个controller或者controller的一个方法）
 *              * 基于interceptor实现
 *          2. 通过配置类实现全局跨域配置/CorsFilter
 *              @Override
 *              public void addCorsMappings(CorsRegistry registry) {
 *                  registry.addMapping("/**");
 *              }
 *              * DispatchServlet根据addCorsMappings判断是否跨域请求进行过滤
 *      * Spring Security 跨域
 *          * @CrossOrigin和addCorsMappings均失效
 *          * 由于Spring Security基于Filter实现，在进入DispatchServlet前对请求过滤，预检请求由于不携带token被Spring Security过滤
 *          * 解决方案
 *              * 1. 配置放过所有的预检请求
 *                  .antMatchers(HttpMethod.OPTIONS).permitAll()
 *              * 2. 在SpringSecurity开启跨域支持(传入配置类)
 *                  .cors().configurationSource(corsConfigurationSource())
 *
 * */

public class BasicUse {
}
