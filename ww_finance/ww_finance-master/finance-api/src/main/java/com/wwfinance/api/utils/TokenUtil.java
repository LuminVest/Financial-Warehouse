package com.wwfinance.api.utils;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wwfinance.api.entity.User;
import com.wwfinance.api.service.UserService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * token辅助类
 */
public class TokenUtil {

    private final static String secret = "vsofo-5grcs-secret";// 加解密密匙
    private final static long expiration = 604800; // 过期时间/秒
    private final static String tokenHead = "5grcs "; // #JWT负载中拿到开头

    private static String localGenerateToken(Map<String, Object> claims) {
        Date expirationTime = new Date(System.currentTimeMillis() + expiration * 1000);
        return Jwts.builder().setClaims(claims).setExpiration(expirationTime).signWith(SignatureAlgorithm.HS512, secret)
                .compact();
    }

    private static Claims getClaimsFromToken(String token) {
        Claims claims = null;
        try {
            claims = Jwts.parser().setSigningKey(secret).parseClaimsJws(token).getBody();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return claims;
    }

    /**
     * token是否失效
     *
     * @return true 失效 false 未失效
     */
    public static boolean isTokenExpired(String token) {
        if (token.startsWith(tokenHead)) {
            token = token.substring(tokenHead.length());
        } else {
            return true;
        }
        Claims claims = getClaimsFromToken(token);
        if (claims == null) {
            return true;
        }

        return false;
    }

    /**
     * 生成用户token
     * @param token_userid  用户手机号
     */
    public static String generateMerchantToken( String token_userid
    ) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("token_userid", token_userid);
        return tokenHead + localGenerateToken(claims);
    }


    /**
     * 获取token信息
     *  用户类型
     */
    public static Map<String, String> getMapInfoFromToken(String token) {
        if (token.startsWith(tokenHead)) {
            token = token.substring(tokenHead.length());
        } else {
            return new HashMap<>();
        }
        Claims claims = getClaimsFromToken(token);
        Map<String, String> tokenInfoMap = new HashMap<>();
        tokenInfoMap.put("token_userid", claims.get("token_userid").toString());
        return tokenInfoMap;
    }

    /**
     * 根据 token 中的手机号或用户ID反查用户（兼容两种 token claims）
     *  - 老师版：claims 存 token_phone(手机号)，按手机号反查
     *  - demo 版：claims 存 token_userid(用户ID)，直接按 ID 查
     */
    public static User getUserByPhoneOrId(String mobile, Map phone, UserService userService) {
        if (mobile != null && !mobile.isEmpty()) {
            LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(User::getMobile, mobile);
            User user = userService.getOne(queryWrapper);
            if (user != null) {
                return user;
            }
        }
        Object uid = phone.get("token_userid");
        if (uid == null) {
            throw new RuntimeException("token 中未找到用户信息");
        }
        return userService.getById(Long.valueOf(String.valueOf(uid)));
    }

    public static void main(String[] args) {

        String token = generateMerchantToken("3");
        System.out.println("args = " + token);
        Map<String, String> map = getMapInfoFromToken(token);
        System.out.println(map);
    }
}

