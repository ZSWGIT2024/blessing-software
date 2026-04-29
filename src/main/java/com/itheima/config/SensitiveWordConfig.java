package com.itheima.config;

import com.github.houbb.sensitive.word.api.IWordAllow;
import com.github.houbb.sensitive.word.api.IWordDeny;
import com.github.houbb.sensitive.word.bs.SensitiveWordBs;
import com.github.houbb.sensitive.word.support.allow.WordAllows;
import com.github.houbb.sensitive.word.support.deny.WordDenys;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.List;

@Configuration
public class SensitiveWordConfig {

    /**
     * 自定义敏感词拒绝列表（从数据库或文件加载）
     */
    @Bean
    public IWordDeny customWordDeny() {
        return () -> {
            // 这里可以从数据库、Redis或文件加载敏感词
            // 示例敏感词列表
            List<String> sensitiveWords = Arrays.asList(
                    "政治类",
                    "暴力类",
                    "色情类",
                    "辱骂类",
                    "广告类",
                    "违法类",
                    "其他"
            );
            return sensitiveWords;
        };
    }

    /**
     * 配置允许的词汇（白名单）
     */
    @Bean
    public IWordAllow customWordAllow() {
        return () -> Arrays.asList(
                "正常的词汇",
                "允许的词",
                "技术术语"
        );
    }

    /**
     * 敏感词过滤器
     */
    @Bean
    public SensitiveWordBs sensitiveWordBs(IWordDeny customWordDeny) {
        return SensitiveWordBs.newInstance()
                .wordDeny(WordDenys.chains(WordDenys.defaults(), customWordDeny())) // 系统+自定义敏感词
                .wordAllow(WordAllows.defaults()) // 系统白名单
                .ignoreCase(true) // 忽略大小写
                .ignoreWidth(true) // 忽略全角半角
                .ignoreNumStyle(true) // 忽略数字样式
                .ignoreChineseStyle(true) // 忽略中文样式
                .ignoreEnglishStyle(true) // 忽略英文样式
                .ignoreRepeat(true) // 忽略重复
                .enableNumCheck(true) // 启用数字检测
                .enableEmailCheck(true) // 启用邮箱检测
                .enableUrlCheck(true) // 启用URL检测
                .enableWordCheck(true) // 启用单词检测
                .numCheckLen(8) // 数字检测长度
                .init();
    }
}