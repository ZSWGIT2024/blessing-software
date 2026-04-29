package com.itheima;
import com.aliyun.oss.*;
import com.aliyun.oss.common.auth.*;
import com.aliyun.oss.common.comm.SignVersion;
import com.aliyun.oss.model.Bucket;

import java.util.List;

/**
 * OSS SDK 快速接入示例
 * 演示如何初始化 OSS 客户端并列出所有 Bucket
 */
public class Test {

    public static void main(String[] args) {
        // 从环境变量获取访问凭证
        String accessKeyId = System.getenv("OSS_ACCESS_KEY_ID");
        String accessKeySecret = System.getenv("OSS_ACCESS_KEY_SECRET");

        // 设置OSS地域和Endpoint
        String region = "cn-shanghai";
        String endpoint = "oss-cn-shanghai.aliyuncs.com";

        // 创建凭证提供者
        DefaultCredentialProvider provider = new DefaultCredentialProvider(accessKeyId, accessKeySecret);

        // 配置客户端参数
        ClientBuilderConfiguration clientBuilderConfiguration = new ClientBuilderConfiguration();
        // 显式声明使用V4签名算法
        clientBuilderConfiguration.setSignatureVersion(SignVersion.V4);

        // 初始化OSS客户端
        OSS ossClient = OSSClientBuilder.create()
                .credentialsProvider(provider)
                .clientConfiguration(clientBuilderConfiguration)
                .region(region)
                .endpoint(endpoint)
                .build();

        // 列出当前用户的所有Bucket
        List<Bucket> buckets = ossClient.listBuckets();
        System.out.println("成功连接到OSS服务，当前账号下的Bucket列表：");

        if (buckets.isEmpty()) {
            System.out.println("当前账号下暂无Bucket");
        } else {
            for (Bucket bucket : buckets) {
                System.out.println("- " + bucket.getName());
            }
        }

        // 释放资源
        ossClient.shutdown();
        System.out.println("OSS客户端已关闭");
    }
}
