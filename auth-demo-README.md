
# auth-demo


## No Session Scenarios

1. One App, Multiple Clients Login,only latest login valid:
	- Login Process:
		+ Client1: login -> success
		+ Client2: login -> success & expire Client1 login
	- Visit controlled resources:
		+ Client1: 401 Unauthorized -> please login
		+ Client2: success
	- Logout Process:
		+ Client1: logout -> invalid token-> success
		+ Client2: logout -> valid token -> success
	- Implement:
		+ 使用Redis维护保存用户登陆信息:
			* `<prefix>:<token>`:`<user>` & expireTime
			* `<prefix>:<user.id>`:`<token>` & expireTime
		+ HttpHeader中带有token:
			* `<token>`
		+ login:
			* delete the two redis key-values (for invaliding other clients login)
			* generate new `<token>`
			* set the two redis key-values
			* set `<token>` to Http Response
		+ logout:
			* get `token1` from http header
			* get `user.id` from redis key-value `<prefix>:<token1>`:`<user>`
			* get `token2` from redis key-value `<prefix>:<user.id>`:`<token2>`
			* can't get `user.id` || can't get `token2` -> valid -> success
			* compare `token1` & `token2`
				- match -> valid -> delete the two redis key-values -> success
				- unmatch -> invalid -> fail
		+ getAuthentication
			* get `user` from redis key-value `<prefix>:<token>`:`<user>` (get `token` from http header) 

2. One AuthService,Multiple other Services call AuthService(seperate auth process)
	- AuthService API:
		+ login
		+ logout
		+ getAuthentication
	- Service1 -> AuthService
	- Service2 -> AuthService
	- Implement:
		+ 使用Redis维护保存用户登陆信息: (note: different services use different `<prefix>`)
			* `<prefix>:<token>`:`<user>` & expireTime 
		+ HttpHeader中带有token:
			* `<token>`
		+ login: 
			* generate new `token`
			* set redis key-value `<prefix>:<token>`:`<user>`
			* set `<token>` to HttpResponse
		+ logout: 
			* delete redis key-value `<prefix>:<token>`:`<user>` (get `token` from http header)
		+ getAuthentication: 
			* get `user` from redis key-value `<prefix>:<token>`:`<user>` (get `token` from http header)


### AuthService(AuthController) 示例

1. pom.xml
```xml
<!-- SpringBoot -->
<dependency>
	<groupId>org.springframework.boot</groupId>
	<artifactId>spring-boot-starter</artifactId>
</dependency>
<dependency>
	<groupId>org.springframework.boot</groupId>
	<artifactId>spring-boot-starter-web</artifactId>
</dependency>
<dependency>
	<groupId>org.springframework.boot</groupId>
	<artifactId>spring-boot-starter-test</artifactId>
	<scope>test</scope>
</dependency>

<!-- Redis -->
<dependency>
	<groupId>org.springframework.boot</groupId>
	<artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>

<!-- for StringUtils -->
<dependency>
	<groupId>org.apache.commons</groupId>
	<artifactId>commons-lang3</artifactId>
</dependency>

<!-- for MD5 -->
<dependency>
	<groupId>commons-codec</groupId>
	<artifactId>commons-codec</artifactId>
</dependency>
```

2. resources/application.yml
```yml
server:
      port: 8080
      servlet:
        context-path: /micro-auth
      
spring:
  redis:
    host: localhost
    port: 6379
    password: 123456
    timeout: 30000
    jedis:
      pool:
        max-active: 8
        max-wait: 1
        max-idle: 8
        min-idle: 0
        
auth:
  usersessionHeader: usersession
  principalHeader: micro-auth
  expireTime: 180
```

3. RedisConfig
```java
@Configuration
public class RedisConfig {
	@Bean
	public RedisTemplate<String,Object> jsonRedisTemplate(RedisConnectionFactory redisConnectionFactory){
		RedisTemplate<String, Object> template = new RedisTemplate<String,Object>();
		template.setConnectionFactory(redisConnectionFactory);
		
		RedisSerializer<String> stringSerializer = new StringRedisSerializer();
		template.setKeySerializer(stringSerializer);
		template.setHashKeySerializer(stringSerializer);
		
		Jackson2JsonRedisSerializer<Object>	jsonSerializer = initJsonSerializer();
		template.setValueSerializer(jsonSerializer);
		template.setHashValueSerializer(jsonSerializer);
		
		template.afterPropertiesSet();
		
		System.out.println("Create Customer JsonRedisTemplate-----");
		return template;
	}
	private Jackson2JsonRedisSerializer<Object> initJsonSerializer(){
		Jackson2JsonRedisSerializer<Object>	jsonSerializer = new Jackson2JsonRedisSerializer<Object>(Object.class);
		ObjectMapper objectMapper = initObjectMapper();
		jsonSerializer.setObjectMapper(objectMapper);
		return jsonSerializer;
	}
	private ObjectMapper initObjectMapper(){
		ObjectMapper objectMapper = new ObjectMapper();
		objectMapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
		objectMapper.enableDefaultTyping(ObjectMapper.DefaultTyping.NON_FINAL);
		objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES); 
		objectMapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
		//objectMapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
		return objectMapper;
	}
}
```

4. RedisService
```java
@Service
public class RedisService {		
	
	@Autowired
	private RedisTemplate<String, Object> jsonRedisTemplate;
	
	public Object get(String key) {
		return jsonRedisTemplate.opsForValue().get(key);
	}	
	public void set(String key, Object value) {
		jsonRedisTemplate.opsForValue().set(key, value);
	}
	public void set(String key,Object value,int timeout){
		jsonRedisTemplate.opsForValue().set(key, value,timeout,TimeUnit.SECONDS);
	}
	public Boolean delete(String key){
		return jsonRedisTemplate.delete(key);
	}
	public void expire(String key,int timeout){
		jsonRedisTemplate.expire(key, timeout, TimeUnit.SECONDS);
	}
}
```

5. AuthController
```java
@RestController
public class AuthController {
	
	@Value("${auth.principalHeader}")
	private String principalHeader;
	
	@Value("${auth.expireTime}")
	private int expireTime;
	
	@Autowired
	private RedisService redisService;

	@Autowired
	private UserService userService;
	
	@GetMapping("/")
	public Object index(){
		return ResponseEntity.ok("This is micro-authService!");
	}
	
	@PostMapping("/login")
	public Object login(@RequestBody User loginUser,
			@RequestHeader(name="${auth.usersessionHeader}") String sessionKey, 
			@RequestHeader(name="${auth.principalHeader}",required=false) String principle,
			HttpServletResponse response){
		// db: verify 
		if(loginUser==null || StringUtils.isAnyBlank(loginUser.getName(),loginUser.getPassword()))
			return MicroResponse.InvalidRequest;
		
		// check principle
		if(principle!=null){
			User user=(User)this.redisService.get(sessionKey+":"+principle);
			if(user!=null 
					&& loginUser.getName().equals(user.getName()) 
					&& MD5Utils.getMD5Str(loginUser.getPassword()).equals(user.getPassword())){
				this.redisService.expire(sessionKey+":"+principle, expireTime);
				this.redisService.expire(sessionKey+":"+user.getId(), expireTime);
				response.setHeader(principalHeader, principle);
				return MicroResponse.success("login success");
			}
		}
		
		// check name & password
		User user=userService.findByNameAndPassword(loginUser);
		if(user==null)
			return MicroResponse.AuthenticationFail;

		// delete other clients login
		String oldToken = (String)this.redisService.get(sessionKey+":"+user.getId());
		if(user!=null)
			this.redisService.delete(sessionKey+":"+oldToken);
		this.redisService.delete(sessionKey+":"+user.getId());
		
		// set new
		String token = UUID.randomUUID().toString();
		this.redisService.set(sessionKey+":"+token,user,expireTime);
		this.redisService.set(sessionKey+":"+user.getId(),token,expireTime);
		response.setHeader(principalHeader, token);
		return MicroResponse.success("login success");
	}
	
	@GetMapping("/logout")
	public Object logout(@RequestHeader(name="${auth.principalHeader}") String principle,@RequestHeader(name="${auth.usersessionHeader}") String sessionKey){
		User user=(User)this.redisService.get(sessionKey+":"+principle);
		if(user==null)
			return MicroResponse.success("logout success");

		String token=(String)this.redisService.get(sessionKey+":"+user.getId());
		if(token==null)
			return MicroResponse.success("logout success");
		
		if(principle.equals(token)){
			this.redisService.delete(sessionKey+":"+user.getId());
			this.redisService.delete(sessionKey+":"+token);
			return MicroResponse.success("logout success");
		}
		return MicroResponse.fail("logout fail");
	}
	
	@GetMapping("/authentication")
	public Object getAuthentication(@RequestHeader(name="${auth.principalHeader}") String principle,@RequestHeader(name="${auth.usersessionHeader}") String sessionKey){
		return MicroResponse.success(redisService.get(sessionKey+":"+principle));
	}

	@PostMapping("/regist")
	public Object regist(@RequestBody User user){
		if(user==null || StringUtils.isAnyBlank(user.getName(),user.getPassword()))
			return MicroResponse.InvalidRequest;
		boolean result=userService.save(user);
		return new MicroResponse(result,result?1:0,user);
	}
}
```

6. UserService & UserRepository & User
```java
@Service
public class UserService {
	@Autowired
	private UserRepository userRepository;

	public User findByNameAndPassword (User user){
		Optional<User> result= this.userRepository.findByNameAndPassword(user.getName(),user.getPassword());
		if(result.isPresent())
			return result.get();
		return null;
	}

	public boolean save(User user){
		return this.userRepository.save(user);
	}
}

@Repository
public class UserRepository {
	
	//private final ConcurrentMap<Integer,User> users = new ConcurrentHashMap<Integer,User>();
	private final ConcurrentMap<String,User> users=new ConcurrentHashMap<String,User>();
    private final static AtomicInteger idGenerator = new AtomicInteger();
    
    @PostConstruct
    public void init(){
    	users.put("admin", new User(idGenerator.incrementAndGet(),"admin",MD5Utils.getMD5Str("admin123")));
    }
    public boolean save(User user){
        Integer id = idGenerator.incrementAndGet();
        user.setId(id);
		user.setPassword(MD5Utils.getMD5Str(user.getPassword()));
        return users.put(user.getName(),user)==null;
    }
    public Collection<User> list(){
        return users.values();
    }
    public Optional<User> findByNameAndPassword(String name,String password){
    	User user=users.get(name);
    	if(user.getPassword().equals(MD5Utils.getMD5Str(password)))
    		return Optional.of(user);
    	return Optional.empty();
    }
    public boolean existsByName(String name){
    	return users.containsKey(name);
    }
}

public class User {
	private Integer id;
	private String name;
	private String password;
	
	public User(){}
	
	public User(Integer id,String name,String password){
		this.id=id;
		this.name=name;
		this.password=password;
	}
	@JsonIgnore
	public String getPassword() {
		return password;
	}
	@JsonProperty
	public void setPassword(String password) {
		this.password = password;
	}
	/* other getter & setter ... */
}
```

7. utils: MD5Utils & MicroResponse
```java
public class MD5Utils {
	 public static String getMD5Str(String strValue) {
		try {
			MessageDigest md5 = MessageDigest.getInstance("MD5");
			String newstr = Base64.encodeBase64String(md5.digest(strValue.getBytes()));
			return newstr;
		} catch (NoSuchAlgorithmException e) {
			//e.printStackTrace();
			System.out.println(e.getMessage());
		}
		return strValue;
	}
}

public class MicroResponse {
	public static final MicroResponse OK=new MicroResponse(true,1,null);
	public static final MicroResponse AuthenticationFail=new MicroResponse(false,2, "Authentication Fail");
	public static final MicroResponse UnAuthorized=new MicroResponse(false,3, "Not Authorized");
	public static final MicroResponse InvalidRequest=new MicroResponse(false,4,"Invalid Request");
	public static final MicroResponse Existed=new MicroResponse(false,5,"Already Existed");
	public static final MicroResponse NotExist=new MicroResponse(false,6,"Not Exist");

	public static MicroResponse success(Object data){
		return new MicroResponse(true,1,data);
	}
	public static MicroResponse fail(Object data){
		return new MicroResponse(false,0,data);
	}
	
	private boolean success;
	private Integer code;
	private Object data;
	
	public MicroResponse(boolean success, Integer code, Object data) {
		super();
		this.success = success;
		this.code = code;
		this.data = data;
	}
	/* getter & setter ...  */
}
```

8. main
```java
@SpringBootApplication
public class AuthServiceApplication {
	public static void main(String[] args) {
		SpringApplication.run(AuthServiceApplication.class, args);
	}
}
```

9. Verify: `http://localhost:8080/micro-auth`
	+ POST `/regist`
		* body: `{"name":"Tom","password":"123123"}`
	+ POST `/login`
		* header: `usersession:xx`
		* body: `{"name":"Tom","password":"123123"}`
	+ GET 	`/logout`
		* header: `usersession:xx`,`micro-auth:xxxxxxxxxxxxxx`
	+ GET `/authentication`
		* header: `usersession:xx`,`micro-auth:xxxxxxxxxxxxxx`

10. Result:
```bash
# 1. POST /regist
> curl -i -H "Content-Type: application/json" -H "usersession:s1" -X POST -d '{"name":"Tom","password":"123123"}' http://localhost:8080/micro-auth/regist

HTTP/1.1 200
Content-Type: application/json;charset=UTF-8
Transfer-Encoding: chunked
Date: Thu, 07 Feb 2019 05:38:22 GMT

{"success":true,"code":1,"data":{"id":2,"name":"Tom"}}

# 2. POST /login
> curl -i -H "Content-Type: application/json" -H "usersession:s1" -X POST -d '{"name":"Tom","password":"123123"}' http://localhost:8080/micro-auth/login
HTTP/1.1 200
micro-auth: 1d0fa647-9dbe-410a-8d9c-0e1c973a98e2
Content-Type: application/json;charset=UTF-8
Transfer-Encoding: chunked
Date: Thu, 07 Feb 2019 05:39:33 GMT

{"success":true,"code":1,"data":"login success"}

# check redis:
redis:6379> keys *
1) "s1:2"
2) "s1:1d0fa647-9dbe-410a-8d9c-0e1c973a98e2"

redis:6379> get s1:2
"\"1d0fa647-9dbe-410a-8d9c-0e1c973a98e2\""

redis:6379> get s1:1d0fa647-9dbe-410a-8d9c-0e1c973a98e2
"[\"com.cj.auth.entity.User\",{\"id\":2,\"name\":\"Tom\",\"password\":\"Qpf0SxOVUjUkWySXOZ16kw==\"}]"

redis:6379> ttl s1:2
(integer) 100

redis:6379> ttl s1:1d0fa647-9dbe-410a-8d9c-0e1c973a98e2
(integer) 99

# 3. GET /authentication
> curl -i -H "micro-auth:1d0fa647-9dbe-410a-8d9c-0e1c973a98e2" -H "usersession:s1" -X GET http://localhost:8080/micro-auth/authentication

HTTP/1.1 200
Content-Type: application/json;charset=UTF-8
Transfer-Encoding: chunked
Date: Thu, 07 Feb 2019 05:40:49 GMT

{"success":true,"code":1,"data":{"id":2,"name":"Tom"}}

# 4. GET /logout
> curl -i -H "micro-auth: 1d0fa647-9dbe-410a-8d9c-0e1c973a98e2" -H "usersession:s1" -X GET http://localhost:8080/micro-auth/logout

HTTP/1.1 200
Content-Type: application/json;charset=UTF-8
Transfer-Encoding: chunked
Date: Thu, 07 Feb 2019 05:04:30 GMT

{"success":true,"code":1,"data":"logout success"}

# check redis:
redis:6379> keys *
(empty list or set)
```


### Call AuthService(No Session) 示例

使用RestTemplate方式call发布的AuthService：
(也可考虑使用Dobbo等其他RPC方式，AuthService发布方式也需要改变)

```java
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT)
public class ServiceCallTest {

	@Autowired
	private TestRestTemplate restTemplate;
	
	private String authURL="http://localhost:8080/micro-auth";
	private String principalHeader="micro-auth";
	private String usersessionHeader="usersession";
	private String usersessionKey="s1";
	
	String principle="a37377ec-dc92-41f8-95af-4d0a494f3eb8";
	
	@Test
	public void callTest(){
		
		//getAuthentication
		callGetAuthentication();
				
		// login
		callLoginTest();
		
		// callGetAuthentication
		callGetAuthentication();
		
		// logout
		callLogoutTest();
		
		// getAuthentication
		callGetAuthentication();
	}
	
	@Test
	public void callLoginTest(){
		System.out.println("call login...");
		
		HttpHeaders headers = new HttpHeaders(); 
		headers.set(usersessionHeader,usersessionKey); 
		headers.setContentType(MediaType.APPLICATION_JSON);
		
		Map<String,String> userMap=new HashMap<String,String>();
		userMap.put("name", "Tom");
		userMap.put("password", "123123");
		
		HttpEntity<Map> entity=new HttpEntity<Map>(userMap,headers);
		HttpEntity<Map> response=restTemplate.exchange(authURL+"/login",HttpMethod.POST,entity,Map.class);
		System.out.println(response);
		
		principle=(String)response.getHeaders().getFirst(principalHeader);
		System.out.println("token:"+principle);
	}
	
	@Test
	public void callLogoutTest(){
		System.out.println("call logout...");
		HttpHeaders headers = new HttpHeaders(); 
		headers.set(principalHeader,principle); 
		headers.set(usersessionHeader,usersessionKey); 
		headers.setContentType(MediaType.APPLICATION_JSON);
		HttpEntity<String> entity=new HttpEntity<String>(null,headers);
		HttpEntity<String> response=restTemplate.exchange(authURL+"/logout",HttpMethod.GET,entity,String.class);
		System.out.println(response);
	}
	
	@Test
	public void callGetAuthentication(){
		System.out.println("call getAuthentication...");
		HttpHeaders headers = new HttpHeaders(); 
		headers.set(principalHeader,principle); 
		headers.set(usersessionHeader,usersessionKey); 
		headers.setContentType(MediaType.APPLICATION_JSON);
		HttpEntity<String> entity=new HttpEntity<String>(null,headers);
		HttpEntity<Map> response=restTemplate.exchange(authURL+"/authentication",HttpMethod.GET,entity,Map.class);
		System.out.println(response);
	}
}
```

Run Junit Test: callTest
```
call getAuthentication...
<200,{success=true, code=1, data=null},{Content-Type=[application/json;charset=UTF-8], Transfer-Encoding=[chunked], Date=[Thu, 07 Feb 2019 06:37:22 GMT]}>

call login...
<200,{success=true, code=1, data=login success},{micro-auth=[80d89cb8-f8e3-4b6c-8344-06d9e3a88d41], Content-Type=[application/json;charset=UTF-8], Transfer-Encoding=[chunked], Date=[Thu, 07 Feb 2019 06:37:22 GMT]}>
token:80d89cb8-f8e3-4b6c-8344-06d9e3a88d41

call getAuthentication...
<200,{success=true, code=1, data={id=2, name=Tom}},{Content-Type=[application/json;charset=UTF-8], Transfer-Encoding=[chunked], Date=[Thu, 07 Feb 2019 06:37:22 GMT]}>

call logout...
<200,{"success":true,"code":1,"data":"logout success"},{Content-Type=[application/json;charset=UTF-8], Transfer-Encoding=[chunked], Date=[Thu, 07 Feb 2019 06:37:22 GMT]}>

call getAuthentication...
<200,{success=true, code=1, data=null},{Content-Type=[application/json;charset=UTF-8], Transfer-Encoding=[chunked], Date=[Thu, 07 Feb 2019 06:37:22 GMT]}>
```


## SpringSession Scenarios

1. One App,Multiple Clients Login => seperated,all success

2. One AuthService,Multiple other Services call AuthService => seperated,all successs

3. One App,Multiple ports (Nginx/Apache+Tomcat) => depends on session async strategy

### AuthService (AuthSessionController) 示例

1. pom.xml
```xml
<!-- SpringBoot -->
<dependency>
	<groupId>org.springframework.boot</groupId>
	<artifactId>spring-boot-starter</artifactId>
</dependency>
<dependency>
	<groupId>org.springframework.boot</groupId>
	<artifactId>spring-boot-starter-web</artifactId>
</dependency>
<!-- Springboot redis -->
<dependency>
	<groupId>org.springframework.boot</groupId>
	<artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>

<!-- Spring Session -->
<dependency>
	<groupId>org.springframework.session</groupId>
	<artifactId>spring-session-data-redis</artifactId>
</dependency>	
```

2. resources/application.yml
```
server:
  port: 8080
  servlet:
	context-path: /micro-auth	  
spring:
  redis:
	host: localhost
	port: 6379
	password: 123456
	timeout: 30000
	jedis:
	  pool:
		max-active: 8
		max-wait: 1
		max-idle: 8
		min-idle: 0  	
#  session:
#    store-type: redis  
#	 timeout: 180  
#    redis:
#      namespace: sps		# default prefix is `spring:session`
#      flush-mode: on-save
```

3. RedisConfig: 使用Jackson2JsonRedisSerializer解析Redis Value值
```java
@Configuration
// 使用`@EnableRedisHttpSession`或在application.yml中配置`spring.session`
@EnableRedisHttpSession(maxInactiveIntervalInSeconds = 180,
	redisFlushMode=RedisFlushMode.ON_SAVE,
	redisNamespace="sps")
public class RedisConfig {

	/* 
	 * 1. 同上面NoSession示例 
	 * @Bean RedisTemplate<Object,Object> jsonRedisTemplate 
	*/
	// ...

	/* 2. Method1: SessionRepository
	 * SessionRepository 
	 * 定义了创建、保存、删除以及检索session的方法
	 * (将Session实例真正保存到数据存储的逻辑是在这个接口的实现中编码完成的)
	 * 
	 * RedisOperationsSessionRepository implements SessionRepository:
	 * 会在Redis中创建、存储和删除session
	 * 
	 * {@link RedisHttpSessionConfiguration} 
	*/
	//	@SuppressWarnings("unchecked")
	//	@Bean
	//	public SessionRepository<?> sessionRepository( @Qualifier("jsonRedisTemplate") RedisOperations<Object, Object> redisTemplate){
	//		RedisOperationsSessionRepository sessionRepository =  new RedisOperationsSessionRepository(redisTemplate);
	//		// sessionRepository.setDefaultSerializer(initJsonSerializer());
	//		sessionRepository.setDefaultSerializer((RedisSerializer<Object>) redisTemplate.getValueSerializer());
	//		sessionRepository.setDefaultMaxInactiveInterval(180);
	//		sessionRepository.setRedisKeyNamespace("sps");
	//		sessionRepository.setRedisFlushMode(RedisFlushMode.ON_SAVE);
	//		System.out.println("Create Customer RedisOperationsSessionRepository --- ");
	//		return sessionRepository;
	//	}

	/* 2. Method2: DefaultSerializer - Recomend */
	@Bean
	public RedisSerializer<Object> springSessionDefaultRedisSerializer(@Qualifier("jsonRedisTemplate") RedisOperations<Object, Object> redisTemplate){
		return (RedisSerializer<Object>)redisTemplate.getValueSerializer();
	}
}
```

4. Controller
```java
@RestController
@RequestMapping("/session")
public class AuthSessionController {

	@Autowired
	private UserService userService;
	
	@GetMapping("/")
	public Object index(){
		return ResponseEntity.ok("This is micro-authService using springsession!");
	}
	
	@PostMapping("/login")
	public Object login(@RequestBody User loginUser,HttpServletRequest request){
		if(loginUser==null || StringUtils.isAnyBlank(loginUser.getName(),loginUser.getPassword()))
			return MicroResponse.InvalidRequest;
		
		// check name & password
		User user=userService.findByNameAndPassword(loginUser);
		if(user==null)
			return MicroResponse.AuthenticationFail;
				
		HttpSession session=request.getSession();
		session.setAttribute(session.getId(), user);
		System.out.println(session.getId());
		return MicroResponse.success(user);
	}
	
	@GetMapping("/logout")
	public Object logout(HttpServletRequest request){
		HttpSession session=request.getSession(false);
		if(session!=null){
			session.removeAttribute(session.getId());
			System.out.println(session.getId());
		}else
			System.out.println("logout: session is null");
		return MicroResponse.OK;
	}
	
	@GetMapping("/authentication")
	public Object getAuthentication(HttpServletRequest request /*HttpSession session*/){
		HttpSession session=request.getSession(false);
		if(session!=null){
			System.out.println(session.getId());
			return MicroResponse.success(session.getAttribute(session.getId()));
		}
		System.out.println("getAuthentication: session is null");
		return MicroResponse.success(null);
	}
}
```

5. UserService & UserRepository & User & MicroResponse & main 同上

6. Verify: `http://localhost:8080/micro-auth/session`
	+ POST `/login`
		* body: `{"name":"Tom","password":"123123"}`
	+ GET `/logout`
	+ GET `/authentication`

7. Result:
```bash
# 1. POST /login
> curl -c cookie.txt -i -H "Content-Type:application/json" -X POST -d '{"name": "admin", "password":"admin123"}' http://localhost:8080/micro-auth/session/login
HTTP/1.1 200
Set-Cookie: SESSION=MzAxZWJkNjMtOWYzMy00NWJiLWFhZjMtMGM0ZjJlMzIyYWM1; Path=/micro-auth/; HttpOnly
Content-Type: application/json;charset=UTF-8
Transfer-Encoding: chunked
Date: Thu, 07 Feb 2019 16:27:43 GMT

{"success":true,"code":1,"data":{"id":1,"name":"admin"}}

# check redis:
redis:6379> keys *
1) "sps:sessions:293d00d7-359f-4b82-87be-bbbe1fc64c8d"
2) "sps:sessions:expires:293d00d7-359f-4b82-87be-bbbe1fc64c8d"
3) "sps:expirations:1549558500000"

redis:6379> hgetall sps:sessions:293d00d7-359f-4b82-87be-bbbe1fc64c8d
1) "creationTime"
2) "1549558319123"
3) "maxInactiveInterval"
4) "180"
5) "sessionAttr:293d00d7-359f-4b82-87be-bbbe1fc64c8d"
6) "[\"com.cj.auth.entity.User\",{\"id\":1,\"name\":\"admin\",\"password\":\"AZICOnu9cyUFFvBp3xi1AA==\"}]"
7) "lastAccessedTime"
8) "1549558319123"

redis:6379> ttl sps:sessions:expires:293d00d7-359f-4b82-87be-bbbe1fc64c8d
(integer) 158

redis:6379> smembers sps:expirations:1549558500000
1) "\"expires:293d00d7-359f-4b82-87be-bbbe1fc64c8d\""

# check cookie
> cat cookie.txt
# Netscape HTTP Cookie File
# http://curl.haxx.se/docs/http-cookies.html
# This file was generated by libcurl! Edit at your own risk.

#HttpOnly_localhost	FALSE	/micro-auth/	FALSE	0	SESSION	MzAxZWJkNjMtOWYzMy00NWJiLWFhZjMtMGM0ZjJlMzIyYWM1

# 2. GET /authentication
> curl -b cookie.txt -i -H "Content-Type:application/json" -X GET http://localhost:8080/micro-auth/session/authentication

HTTP/1.1 200
Content-Type: application/json;charset=UTF-8
Transfer-Encoding: chunked
Date: Thu, 07 Feb 2019 16:27:48 GMT

{"success":true,"code":1,"data":{"id":1,"name":"admin"}}

# check redis:
redis:6379> keys *
1) "sps:expirations:1549558560000"		# new
2) "sps:sessions:293d00d7-359f-4b82-87be-bbbe1fc64c8d"
3) "sps:sessions:expires:293d00d7-359f-4b82-87be-bbbe1fc64c8d"

redis:6379> hgetall sps:sessions:293d00d7-359f-4b82-87be-bbbe1fc64c8d
1) "creationTime"
2) "1549558319123"
3) "maxInactiveInterval"
4) "180"
5) "sessionAttr:293d00d7-359f-4b82-87be-bbbe1fc64c8d"
6) "[\"com.cj.auth.entity.User\",{\"id\":1,\"name\":\"admin\",\"password\":\"AZICOnu9cyUFFvBp3xi1AA==\"}]"
7) "lastAccessedTime"
8) "1549558358599"						# changed

redis:6379> ttl sps:sessions:expires:293d00d7-359f-4b82-87be-bbbe1fc64c8d
(integer) 166							# changed

# 3. GET /logout
> curl -b cookie.txt -i -H "Content-Type:application/json" -X GET http://localhost:8080/micro-auth/session/logout

HTTP/1.1 200
Content-Type: application/json;charset=UTF-8
Transfer-Encoding: chunked
Date: Thu, 07 Feb 2019 16:28:38 GMT

{"success":true,"code":1,"data":null}

# check redis:
redis:6379> keys *
1) "sps:sessions:293d00d7-359f-4b82-87be-bbbe1fc64c8d"
2) "sps:expirations:1549558680000"								# new
3) "sps:sessions:expires:293d00d7-359f-4b82-87be-bbbe1fc64c8d"

redis:6379> hgetall sps:sessions:293d00d7-359f-4b82-87be-bbbe1fc64c8d
1) "creationTime"
2) "1549558319123"
3) "maxInactiveInterval"
4) "180"
5) "sessionAttr:293d00d7-359f-4b82-87be-bbbe1fc64c8d"
6) ""									# removed
7) "lastAccessedTime"
8) "1549558447429"						# changed

redis:6379> ttl sps:sessions:expires:293d00d7-359f-4b82-87be-bbbe1fc64c8d
(integer) 151

# 4. GET /authentication
> curl -b cookie.txt -i -H "Content-Type:application/json" -X GET http://localhost:8080/micro-auth/session/authentication

HTTP/1.1 200
Content-Type: application/json;charset=UTF-8
Transfer-Encoding: chunked
Date: Thu, 07 Feb 2019 16:29:58 GMT

{"success":true,"code":1,"data":null}

# check redis:
redis:6379> keys *
1) "sps:sessions:301ebd63-9f33-45bb-aaf3-0c4f2e322ac5"

redis:6379> keys *
(empty list or set)
```

### Call AuthService(AuthSessionController) 示例

```java
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT)
public class ServiceCallAuthNoSessionTest {

	@Autowired
	private TestRestTemplate restTemplate;
	private String authURL="http://localhost:8080/micro-auth/session";
	private String cookie="";
	
	private String name="admin";
	private String password="admin123";
	
	@Test
	public void callTest(){
		
		//getAuthentication
		callGetAuthentication();
				
		// login
		callLoginTest();
		
		// callGetAuthentication
		callGetAuthentication();
		
		// logout
		callLogoutTest();
		
		// getAuthentication
		callGetAuthentication();
	}
	
	@Test
	public void callLoginTest(){
		System.out.println("call login...");
		
		HttpHeaders headers = new HttpHeaders(); 
		headers.setContentType(MediaType.APPLICATION_JSON);
		
		Map<String,String> userMap=new HashMap<String,String>();
		userMap.put("name", name);
		userMap.put("password", password);
		
		HttpEntity<Map> entity=new HttpEntity<Map>(userMap,headers);
		HttpEntity<Map> response=restTemplate.exchange(authURL+"/login",HttpMethod.POST,entity,Map.class);
		System.out.println(response);
		
		cookie=(String)response.getHeaders().getFirst(HttpHeaders.SET_COOKIE);
		System.out.println("cookie:"+cookie);
	}
	
	@Test
	public void callLogoutTest(){
		System.out.println("call logout...");
		HttpHeaders headers = new HttpHeaders(); 
		headers.set(HttpHeaders.COOKIE,cookie); 
		headers.setContentType(MediaType.APPLICATION_JSON);
		HttpEntity<String> entity=new HttpEntity<String>(null,headers);
		HttpEntity<String> response=restTemplate.exchange(authURL+"/logout",HttpMethod.GET,entity,String.class);
		System.out.println(response);
	}
	
	@Test
	public void callGetAuthentication(){
		System.out.println("call getAuthentication...");
		HttpHeaders headers = new HttpHeaders(); 
		headers.set(HttpHeaders.COOKIE,cookie); 
		headers.setContentType(MediaType.APPLICATION_JSON);
		HttpEntity<String> entity=new HttpEntity<String>(null,headers);
		HttpEntity<Map> response=restTemplate.exchange(authURL+"/authentication",HttpMethod.GET,entity,Map.class);
		System.out.println(response);
	}
}
```

Run Junit Test: callTest
```
call getAuthentication...
<200,{success=true, code=1, data=null},{Set-Cookie=[SESSION=ZmM1NDQ1M2EtZTU4ZC00NTNmLWI5ODEtNWQ0YmUyODI3MmE0; Path=/micro-auth/; HttpOnly], Content-Type=[application/json;charset=UTF-8], Transfer-Encoding=[chunked], Date=[Sat, 09 Feb 2019 07:02:26 GMT]}>
call login...
<200,{success=true, code=1, data={id=1, name=admin}},{Set-Cookie=[SESSION=ZmYxNDJiNTctNGU5Ny00MzFjLWFkMWYtNzkwMTJlMmUzZjIy; Path=/micro-auth/; HttpOnly], Content-Type=[application/json;charset=UTF-8], Transfer-Encoding=[chunked], Date=[Sat, 09 Feb 2019 07:02:26 GMT]}>
cookie:SESSION=ZmYxNDJiNTctNGU5Ny00MzFjLWFkMWYtNzkwMTJlMmUzZjIy; Path=/micro-auth/; HttpOnly
call getAuthentication...
<200,{success=true, code=1, data={id=1, name=admin}},{Content-Type=[application/json;charset=UTF-8], Transfer-Encoding=[chunked], Date=[Sat, 09 Feb 2019 07:02:26 GMT]}>
call logout...
<200,{"success":true,"code":1,"data":null},{Content-Type=[application/json;charset=UTF-8], Transfer-Encoding=[chunked], Date=[Sat, 09 Feb 2019 07:02:26 GMT]}>
call getAuthentication...
<200,{success=true, code=1, data=null},{Content-Type=[application/json;charset=UTF-8], Transfer-Encoding=[chunked], Date=[Sat, 09 Feb 2019 07:02:26 GMT]}>
```




