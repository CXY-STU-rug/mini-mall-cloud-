package com.minimall.user.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.minimall.common.core.domain.Result;
import com.minimall.common.security.util.JwtUtil;
import com.minimall.user.entity.User;
import com.minimall.user.mapper.UserMapper;
import com.minimall.user.properties.GithubOAuthProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.net.URL;
import java.net.URLEncoder;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;// ✅ Spring 的, 带 set/setBearerAuth
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * OAuth2 第三方登录 Controller
 *
 * 端点 (网关代理 /user/oauth/** → user 服务):
 *   GET  /user/oauth/github/login    → 返回 GitHub 授权页 URL, 前端拿到后跳转
 *   GET  /user/oauth/github/callback → GitHub 授权完跳回这里, 拿 code 换 token, 返我们 JWT
 *
 * 设计:
 *   不返 302 redirect 让浏览器直接跳, 而是返 JSON 让前端拿到 URL 自己 window.location.href=...
 *   原因: 前后端分离, 后端不知道前端域名; 前端可以加 loading 动画再跳转
 */
@RestController
@RequestMapping("/user/oauth")
public class OAuthController {

    @Autowired
    private GithubOAuthProperties githubOAuthProperties;
    @Autowired
    private RestTemplate restTemplate;       // 调 GitHub HTTP

    @Autowired
    private UserMapper userMapper;           // 查/插 user 表

    @Autowired
    private JwtUtil jwtUtil;                 // 签发我们自己的 JWT

    @Autowired
    private ObjectMapper objectMapper;
    /**
     * ① 拿 GitHub 授权页 URL
     *
     * 前端调用流程:
     *   ① 用户点 "GitHub 登录" 按钮
     *   ② 前端 GET /user/oauth/github/login
     *   ③ 后端返 { "code":200, "data":{"url":"https://github.com/login/oauth/authorize?..."} }
     *   ④ 前端 window.location.href = data.url   ← 跳转到 GitHub
     *   ⑤ 用户在 GitHub 同意授权
     *   ⑥ GitHub 302 跳回我们的 callback URL (带 code=xxx)
     *
     * 拼装的 GitHub authorize URL 格式:
     *   https://github.com/login/oauth/authorize
     *     ?client_id=xxx              ← 告诉 GitHub 是哪个 OAuth App
     *     &redirect_uri=xxx           ← 跳回我们的 callback (必须 URL 编码)
     *     &scope=read:user user:email ← 申请的权限范围
     *     &state=xxx                  ← (可选) 防 CSRF, 这里暂省略
     */
    @GetMapping("/github/login")
    public Result<Map<String, String>> githubLogin() {
        // 拼 GitHub 授权页 URL
        // ⚠️ redirect_uri 必须 URL 编码, 因为它本身含特殊字符 (冒号/斜杠)
        String encodedCallback = URLEncoder.encode(
                githubOAuthProperties.getCallbackUrl(),
                StandardCharsets.UTF_8
        );

        // String.format 拼 query string
        String authorizeUrl = String.format(
                "%s?client_id=%s&redirect_uri=%s&scope=read:user user:email",
                githubOAuthProperties.getAuthorizeUrl(),
                githubOAuthProperties.getClientId(),
                encodedCallback
        );

        // 返回 JSON, 前端拿到 url 跳转
        Map<String, String> data = new HashMap<>();
        data.put("url", authorizeUrl);
        return Result.success(data);
    }

    // OAUTH.7 实现 callback 端点 - 下一步

    // ═══════════════════════════════════════════════════════════
    // ② GitHub 跳回来 → 4 步换成我们的 JWT
    // ═══════════════════════════════════════════════════════════

    /**
     * GitHub 回调端点
     *
     * GitHub 302 跳到这里时, URL 形如:
     *   /user/oauth/github/callback?code=abc123xyz
     *
     * 我们用这个 code 走 4 步, 最后返我们自己的 JWT 给前端.
     *
     * @param code GitHub 给的临时授权码 (10 分钟内有效, 一次性)
     */
    @GetMapping("/github/callback")
    public Result<Map<String, Object>> githubCallback(@RequestParam String code) {
        try {
            // ─── 步骤 1: code → access_token (POST github.com) ─────────
            // GitHub 文档: https://docs.github.com/en/apps/oauth-apps/building-oauth-apps/authorizing-oauth-apps
            // POST https://github.com/login/oauth/access_token
            // 请求体 form:  client_id=xxx & client_secret=xxx & code=xxx
            // 响应默认是 url-encoded:  access_token=gho_xxx&scope=...&token_type=bearer
            // 加 Accept: application/json header 让 GitHub 返 JSON, 解析更方便
            //
            // TODO 1: 用 restTemplate.postForObject() 拿 access_token
            //   提示:
            //     a. 拼 URL (or 用 query string), 把 client_id/client_secret/code 塞进去
            //     b. headers.set("Accept", "application/json")
            //     c. new HttpEntity<>(null, headers) 包装
            //     d. restTemplate.postForObject(url, entity, String.class)  返 JSON 字符串
            //     e. objectMapper.readTree(json).get("access_token").asText()  抽出 token

            // ─── 步骤 1: code → access_token (POST github.com) ─────────

            // (a) 拼 token URL: 把 client_id / client_secret / code 都挂在 query string 里
            //     ⚠️ GitHub 这个接口支持把参数放在 query 或 body, 我们走 query 最简单
            //     最终 URL 形如:
            //     https://github.com/login/oauth/access_token
            //          ?client_id=Ov23li...
            //          &client_secret=abc123xxx
            //          &code=8d1b6c9e2f3a4b5c
            String tokenUrl = String.format(
                    "%s?client_id=%s&client_secret=%s&code=%s",
                    githubOAuthProperties.getTokenUrl(),
                    githubOAuthProperties.getClientId(),
                    githubOAuthProperties.getClientSecret(),
                    code
            );
            // (b) 加 Accept: application/json header
            //     GitHub 默认返回的是 url-encoded 格式:  access_token=gho_xxx&scope=...
            //     加这个 header 让 GitHub 改返 JSON:     {"access_token":"gho_xxx","scope":"..."}
            //     JSON 我们用 Jackson 一行解析, url-encoded 还得自己拆字符串, 麻烦
            HttpHeaders headers = new HttpHeaders();
            headers.set("Accept", "application/json");
// (c) HttpEntity 包装 headers
            //     第 1 个参数 null = 请求体为空 (参数都在 URL 的 query string 里, 不用塞 body)
            //     第 2 个参数 headers = 上面准备好的 Header 集合
            HttpEntity<String> entity = new HttpEntity<>(null, headers);
            // (d) restTemplate 发 POST, 拿到 GitHub 返的 JSON 字符串
            //     postForObject(url, requestEntity, responseType)
            //       url           = 上面拼的 tokenUrl
            //       requestEntity = entity (header + body 的封装)
            //       responseType  = String.class  → 我们要 GitHub 返回的原始 JSON 字符串
            //     返回示例: "{\"access_token\":\"gho_xxxxxx\",\"scope\":\"...\",\"token_type\":\"bearer\"}"
            String tokenJson = restTemplate.postForObject(tokenUrl, entity, String.class);
            System.out.println("[OAuth] GitHub /access_token response = " + tokenJson);  // 调试

            // (e) Jackson 解析 — 用 path() 避免字段不存在时 NPE
            JsonNode tokenNode = objectMapper.readTree(tokenJson);
            String accessToken = tokenNode.path("access_token").asText();

            // (f) 防御: 万一 code 过期 / client_secret 错, GitHub 会返
            //     {"error":"bad_verification_code","error_description":"..."}
            //     没有 access_token 字段, accessToken 会是 null, 这里早抛出错误
            if (accessToken == null || accessToken.isEmpty()) {
                return Result.error("GitHub access_token 换取失败: " + tokenJson);
            }

            // ─── 步骤 2: access_token → 用户信息 (GET api.github.com) ──
            // GET https://api.github.com/user
            // Header:  Authorization: Bearer gho_xxxx
            // 返回 JSON: { "id": 12345, "login": "alice", "email": "x@y.com",
            //              "avatar_url": "...", "name": "Alice Wang" }
            //
            // TODO 2: 用 restTemplate.exchange() 带 Authorization header 调
            //   提示:
      //headers.set("Authorization", "Bearer " + accessToken);
         //b. exchange(url, HttpMethod.GET, entity, String.class).getBody();
       // c. objectMapper.readTree(json) 解析, 把 id/login/email/avatar_url 抽出来
            HttpHeaders userHeaders = new HttpHeaders();
            userHeaders.set("Accept", "application/json");
            userHeaders.setBearerAuth(accessToken);
            HttpEntity<Void> userEntity = new HttpEntity<>(userHeaders);

            // (c) exchange 发 GET — postForObject 不能发 GET, 必须用 exchange
            ResponseEntity<String> userResp = restTemplate.exchange(
                    githubOAuthProperties.getUserInfoUrl(),   // 你写的 url 这变量没定义!
                    HttpMethod.GET,
                    userEntity,
                    String.class                              // 返 String, 跟步骤 1 一样
            );
            String userJson = userResp.getBody();             // 拿 JSON 字符串
            System.out.println("[OAuth] GitHub /user response = " + userJson);  // 调试: 看 GitHub 真实返回
            JsonNode userNode = objectMapper.readTree(userJson);
            // ⭐ 用 path() 而不是 get(): 字段不存在时 path() 返回 MissingNode (不是 null),
            //   再调 .asText("") 安全返回 ""; get() 找不到字段返 Java null, .asText() 直接 NPE.
            String githubUserId = userNode.path("id").asText();
            String githubLogin  = userNode.path("login").asText();
            String githubEmail  = userNode.path("email").asText("");      // 邮箱可能不返/为 null
            String githubAvatar = userNode.path("avatar_url").asText("");


            // ─── 步骤 3: MySQL 查 user, 没有就插 ────────────────────────
            // 关键: 用 (oauth_provider="github", oauth_id=githubUserId) 当唯一标识,
            //       数据库 uk_oauth 索引保证这俩组合唯一.
            //
            // TODO 3: MyBatis-Plus 查找或新建
            //   提示:
    QueryWrapper<User> wrapper = new QueryWrapper<>();
 wrapper.eq("oauth_provider", "github")
                  .eq("oauth_id", githubUserId);
            User user = userMapper.selectOne(wrapper);   // 外层声明 user
            if (user == null) {
                user = new User();                        // ✅ 没有 "User" 类型, 这是赋值, 不是声明
                user.setOauthProvider("github");
                user.setOauthId(githubUserId);
                user.setUsername("gh_" + githubLogin);
                user.setNickname(githubLogin);
                user.setEmail(githubEmail);
                user.setAvatar(githubAvatar);
                user.setRole((byte) 0);
                user.setStatus((byte) 1);
                user.setCreateTime(LocalDateTime.now());  // ⚠️ 还是漏了, 这次别忘
                user.setUpdateTime(LocalDateTime.now());
                userMapper.insert(user);                  // 插入后 MyBatis-Plus 回填自增 id 到 user.id
            }


            // ─── 步骤 4: 签我们自己的 JWT ───────────────────────────────
            // 注意: 这一步开始, GitHub 已经无关. 我们给前端的是 mini-mall 自家 JWT,
            //       后续访问任何接口都用这个 token, 跟普通账号登录拿到的一模一样.
            //
            // TODO 4: 用 jwtUtil.generateToken 签 token
            //   提示: String token = jwtUtil.generateToken(user.getId(), user.getUsername());
            String token =jwtUtil.generateToken(user.getId(), user.getUsername());


            // ─── 返回 ─────────────────────────────────────────────────
            Map<String, Object> data = new HashMap<>();
            data.put("token", token);
            data.put("user", user);  // Jackson 会自动序列化, password 字段 @JsonIgnore 不会泄露
            return Result.success(data);

        } catch (Exception e) {
            // 任何一步失败 (GitHub 网络挂/code 过期/JSON 格式变了) 都走这里
            return Result.error("GitHub 登录失败: " + e.getMessage());
        }
    }

}