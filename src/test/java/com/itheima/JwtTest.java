package com.itheima;


import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.junit.jupiter.api.Test;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class JwtTest {


    //测试令牌生成代码
    @Test
    public void testGen(){
        Map<String,Object> claims = new HashMap<>();
        claims.put("id",1);
        claims.put("username","zhangsan");
        //生成jwt的代码
       String token = JWT.create()
                .withClaim("user",claims)  //添加载荷
                .withExpiresAt(new Date(System.currentTimeMillis() + 1000 * 60 * 60 * 24))  //设置过期时间24小时
                .sign(Algorithm.HMAC256("itheima"));//设置加密算法和密钥

        System.out.println(token);
    }

    //测试令牌解析代码
    @Test
    public void testParse(){
        //定义字符串，模拟用户生成的token
        String token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9" +
                ".eyJ1c2VyIjp7ImlkIjoxLCJ1c2VybmFtZSI6InpoYW5nc2FuIn0sImV4cCI6MTc2NDQ4NjAyNH0" +
                ".yxcyoHaoBslnmmhgOSGTka0_JREirb0SqvqJRDYsIiE";


        //解析jwt的代码
       JWTVerifier  verifier = JWT.require(Algorithm.HMAC256("itheima")).build();//构建一个jwt对象
        DecodedJWT decodedJWT = verifier.verify(token);//验证token
        Map<String, Claim> claims = decodedJWT.getClaims();
        System.out.println(claims.get("user"));//获取载荷
    }
}
