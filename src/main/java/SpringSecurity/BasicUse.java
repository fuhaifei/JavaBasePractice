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
 *      * 生成UsernamePasswordAuthenticationToken对象，调用authenticationManager.authenticate验证用户信息
 *      * 验证成功后，将用户信息存放在缓存中后 + 将authentication对象存放在SecurityContextHolder.getContext().setAuthentication()
 *      * 生成JWT Token写入到响应体返回给用户
 *  * 自定义JetAuthenticationTokenFilter
 *      * 从请求中获取JWT token,解析得到其中的登录账号
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
 *      * Java中基于CORS的实现方案
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
 *  项目功能点实现总结
 *  1. 登录
 *      * 提供"/login"方法，实现基于表单的的登录
 *          * 调用CustomUserDetailsService implements UserDetailsService
 *              * 用户名对应对象中的principal属性，而密码对应credentials属性
 *              * 如果密码错误，返回登陆失败(401 UnAuthorized (用户名不存在/登录失败))
 *              * 如果密码正确，生成JWT token，其中JWT token存放信息包括：
 *                  * 用户id:userID
 *                  * 超时时间：exp（unix时间戳）
 *          * Authentication对象存储在SecurityContext中.
 *          * 若有效查询用户角色信息，以列表类型键值对存储在redis中
 *              * key: auth:userrole:userid value:list[role_id]
 *              * 超时时间等于jwt token超时时间
 *          * 返回JWT token给用户
 *      * 自定义了一个filter实现了OncePerRequestFilter,添加到了spring登录鉴权过滤器链中，该过滤器负责检查请求中是否存在token
 *          * 获取请求头中Authorization字段中的JWT token,校验token有效性，以及token是否超时
 *              * 解析成功，初始化Authentication设置到SecurityContext中
 *                  * 判断JWT_Token是否即将过期（<3分钟），重新生成新的Token,更新过期时间
 *              * 若解析失败/超时，直接进入下一个过滤器
 *                  * 如果为permitAll则会进入AnonymousAuthenticationFilter赋予一个匿名验证身份
 *          * 进入下一个过滤器AnonymousAuthenticationFilter
 * 2. 鉴权(FilterSecurityInterceptor)
 *      * AccessDecisionManager 根据权限配置信息进行投票
 *          * 三个实现类（AffirmativeBased 一票通过，UnanimousBased 一票否决，ConsensusBased 少数服从多数）
 *          * 默认的配置文件过滤器（WebExpressionVoter）
 *      * 自定义自动鉴权投票器（AccessDecisionProcessor implements AccessDecisionVoter<FilterInvocation>）
 *          * 实现vote方法
 *              * 首先查询redis缓存（auth:urlrole:url），是否存在当前URL的可访问角色列表
 *              * 存在直接返回，否则数据库查询并更新缓存（过期时间为5分钟）
 *          * 判断当前用户的角色列表是否在url可访问角色列表之中/URL角色列表为ALL
 *              * 存在/URL无角色则投出同意票
 *              * 否则投出反对票
 *     * UnanimousBased(WebExpressionVoter + 自定义动态鉴权Votoer)
 * 3. 未登录/鉴权失败（ExceptionTranslationFilter）
 *      * AuthenticationException认证异常 -> AuthenticationEntryPoint： 401: Unauthorized
 *      * AccessDeniedException访问异常 -> AccessDeniedHandler:         403: Forbidden
 *
 * 4. 基于RabbitMQ的异步调用
 *      * 涉及到生成模型的在线调用，而模型调用往往需要15-20分钟的推理时间，且调用分不同阶段有多个返回结果
 *      * 采用RabbitMQ实现接口的异步调用返回
 *          * 后端接收到调用请求->将请求写入到RabbitMQ中(generate:userID:ip:timestamp)
 *            ->python消费监听队列,执行模型推理->阶段结果写入到返回队列中
 *            -> java后端监听对应消息,读取返回结果，并写入数据库
 *          * 前端可查询到各阶段结果输出时间
 * 5. 基于Spring AOP+Redis实现接口限流
 *      * 目标：一个小时最多调用三次接口，否则python模型推理会出现爆内存的问题
 *      * 自定义注解（@VisitLimit(frequency=15, duration, msg)）+AOP注解扫描
 *      * Redis Key(APIcalllimit:url),第一次调用设置超时时间为duration,之后每次调用frequency+1,直到键超时/次数=15
 *      * 为了避免并发问题，使用lua脚本基于cas机制实现自增
 *
 * 6. 后端存储密码的加密方式
 *      1. Hash对密码进行加密：MD5，SHA-1,SHA-256
 *          * 单向加密算法，加密后无法还原为原始密码
 *          * 攻击方法：穷举法（遍历所有密码组合）/字典攻击法（构建明文->密文的映射）/彩虹表攻击方法
 *          （改进的字典攻击法，用来破解固定且字符范围固）
 *          * 改进：加盐的加密算法
 *              * 对每个用户密码添加一个固定字符或者随机字符串（salt），使得上述攻击方法失效
 *          * 改进：Bcrypt算法（相同明文，每次生成的密文均不相同 + 慢哈希算法 + Bcrypt加密长度为60位）
 *      2. 对称加密算法
 *          * DES算法（56/64）
 *          * AES算法（128/192/256）
 *          * PBE算法：由于上述两个加密算法需要密钥为固定长度，PBE类似与加盐的方法
 *      3. 非对称加密算法
 *          * RSA
 *      5. 数字签名算法
 *          * MD5withRSA
 *          * SHA1withRSA
 *          * SHA256withRSA
 * */

public class BasicUse {
}
