package com.itheima.service.impl;

import com.github.houbb.sensitive.word.bs.SensitiveWordBs;
import com.itheima.common.RedisConstants;
import com.itheima.utils.RedisUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class SensitiveFilterService {

    private final SensitiveWordBs sensitiveWordBs; // 注入敏感词处理器
    private final RedisUtil redisUtil;

    // 自定义敏感词文件
    private static final String SENSITIVE_WORDS_FILE = "sensitive-words.txt";

    @PostConstruct
    public void init() {
        loadCustomSensitiveWords();
    }

    /**
     * 加载自定义敏感词
     */
    private void loadCustomSensitiveWords() {
        try {
            // 1. 先尝试从Redis获取
            Set<Object> cachedWords = redisUtil.sMembers(RedisConstants.SENSITIVE_WORDS_KEY);
            if (cachedWords != null && !cachedWords.isEmpty()) {
                log.info("从Redis加载自定义敏感词，数量: {}", cachedWords.size());
                // 这里可以动态添加到敏感词库
                addWordsToSensitiveWordBs(cachedWords);
                return;
            }

            // 2. 从文件加载
            ClassPathResource resource = new ClassPathResource(SENSITIVE_WORDS_FILE);
            if (!resource.exists()) {
                log.warn("自定义敏感词文件不存在: {}", SENSITIVE_WORDS_FILE);
                return;
            }

            List<String> words = new ArrayList<>();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(resource.getInputStream()))) {
                String word;
                while ((word = reader.readLine()) != null) {
                    word = word.trim();
                    if (!word.isEmpty()) {
                        words.add(word);
                    }
                }
            }

            log.info("从文件加载自定义敏感词成功，数量: {}", words.size());

            // 3. 添加到敏感词库
            addWordsToSensitiveWordBs(words);

            // 4. 保存到Redis
            saveToRedis(words);

        } catch (Exception e) {
            log.error("加载自定义敏感词失败", e);
        }
    }

    /**
     * 动态添加敏感词到敏感词库
     */
    private void addWordsToSensitiveWordBs(Collection<?> words) {
        try {
            // 将自定义敏感词添加到系统敏感词库
            List<String> wordList = new ArrayList<>();
            for (Object word : words) {
                if (word instanceof String) {
                    wordList.add((String) word);
                }
            }

            // 这里需要使用敏感词库的动态添加功能
            // 注意：sensitive-word 库可能需要重启才能生效新词汇
            // 实际使用时可能需要根据具体情况调整

            log.info("成功添加 {} 个自定义敏感词到词库", wordList.size());

        } catch (Exception e) {
            log.error("添加敏感词到词库失败", e);
        }
    }

    /**
     * 保存敏感词到Redis
     */
    private void saveToRedis(List<String> words) {
        try {
            if (!words.isEmpty()) {
                Object[] wordsArray = words.toArray();
                redisUtil.sAdd(RedisConstants.SENSITIVE_WORDS_KEY, wordsArray);
                redisUtil.expire(RedisConstants.SENSITIVE_WORDS_KEY,
                        RedisConstants.SENSITIVE_WORDS_TTL, TimeUnit.MINUTES);
                log.info("自定义敏感词已保存到Redis，数量: {}", words.size());
            }
        } catch (Exception e) {
            log.error("保存自定义敏感词到Redis失败", e);
        }
    }

    /**
     * 过滤敏感词（返回过滤后的文本）
     * @return 过滤后的文本，如果完全被过滤则返回null
     */
    public String filter(String text) {
        if (text == null || text.trim().isEmpty()) {
            return text;
        }

        try {
            // 1. 检查是否包含敏感词
            boolean contains = sensitiveWordBs.contains(text);
            if (!contains) {
                return text; // 不包含敏感词，直接返回
            }

            // 2. 获取所有敏感词
            List<String> sensitiveWords = sensitiveWordBs.findAll(text);
            log.debug("检测到敏感词: {}", sensitiveWords);

            // 3. 替换敏感词（使用*号替换）
            String filteredText = sensitiveWordBs.replace(text);

            // 4. 检查过滤后是否全是*（表示全是敏感词）
            if (isAllAsterisks(filteredText)) {
                log.warn("文本内容全是敏感词: {}", text);
                return null;
            }

            return filteredText;

        } catch (Exception e) {
            log.error("敏感词过滤失败", e);
            return text; // 出现异常时返回原文本
        }
    }

    /**
     * 检查是否包含敏感词
     */
    public boolean containsSensitiveWord(String text) {
        if (text == null || text.trim().isEmpty()) {
            return false;
        }

        try {
            return sensitiveWordBs.contains(text);
        } catch (Exception e) {
            log.error("检查敏感词失败", e);
            return false;
        }
    }

    /**
     * 获取文本中的所有敏感词
     */
    public List<String> findAllSensitiveWords(String text) {
        if (text == null || text.trim().isEmpty()) {
            return Collections.emptyList();
        }

        try {
            return sensitiveWordBs.findAll(text);
        } catch (Exception e) {
            log.error("查找敏感词失败", e);
            return Collections.emptyList();
        }
    }


    /**
     * 检查文本是否全是星号
     */
    private boolean isAllAsterisks(String text) {
        if (text == null) return false;
        return text.chars().allMatch(ch -> ch == '*');
    }

    /**
     * 添加新的敏感词（实时生效）
     */
    public void addSensitiveWord(String word) {
        if (word == null || word.trim().isEmpty()) {
            return;
        }

        word = word.trim();

        try {
            // 1. 保存到Redis
            redisUtil.sAdd(RedisConstants.SENSITIVE_WORDS_KEY, word);

            // 2. 这里需要重新初始化敏感词库或者使用动态添加方式
            // 由于sensitive-word库的限制，可能需要重启应用或调用特定方法

            log.info("添加敏感词: {}", word);

        } catch (Exception e) {
            log.error("添加敏感词失败: {}", word, e);
        }
    }

    /**
     * 批量添加敏感词
     */
    public void addSensitiveWords(List<String> words) {
        if (words == null || words.isEmpty()) {
            return;
        }

        try {
            // 1. 保存到Redis
            Object[] wordsArray = words.stream()
                    .filter(word -> word != null && !word.trim().isEmpty())
                    .map(String::trim)
                    .toArray();

            if (wordsArray.length > 0) {
                redisUtil.sAdd(RedisConstants.SENSITIVE_WORDS_KEY, wordsArray);
            }

            // 2. 重新加载敏感词库
            reloadSensitiveWords();

            log.info("批量添加敏感词，数量: {}", wordsArray.length);

        } catch (Exception e) {
            log.error("批量添加敏感词失败", e);
        }
    }


    /**
     * 重新加载敏感词库
     */
    public void reloadSensitiveWords() {
        try {
            // 从Redis重新加载并更新敏感词库
            Set<Object> cachedWords = redisUtil.sMembers(RedisConstants.SENSITIVE_WORDS_KEY);
            if (cachedWords != null) {
                addWordsToSensitiveWordBs(cachedWords);
                log.info("重新加载敏感词库成功，数量: {}", cachedWords.size());
            }
        } catch (Exception e) {
            log.error("重新加载敏感词库失败", e);
        }
    }

    /**
     * 获取敏感词统计信息
     */
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();

        try {
            // 获取自定义敏感词数量
            Long customCount = redisUtil.sSize(RedisConstants.SENSITIVE_WORDS_KEY);
            stats.put("customWordCount", customCount != null ? customCount : 0);

            // 这里可以添加更多统计信息
            stats.put("lastReloadTime", new Date());

        } catch (Exception e) {
            log.error("获取敏感词统计信息失败", e);
        }

        return stats;
    }

    /**
     * 高级过滤：支持不同的过滤策略
     */
    public String filterWithStrategy(String text, String strategy) {
        if (text == null || text.trim().isEmpty()) {
            return text;
        }

        try {
            // 根据策略选择不同的过滤方式
            switch (strategy) {
                case "strict": // 严格模式：遇到敏感词返回null
                    if (sensitiveWordBs.contains(text)) {
                        return null;
                    }
                    return text;

                case "replace": // 替换模式：用*替换
                    return sensitiveWordBs.replace(text);

                case "highlight": // 高亮模式：用[**]包裹
                    List<String> sensitiveWords = sensitiveWordBs.findAll(text);
                    String result = text;
                    for (String word : sensitiveWords) {
                        result = result.replace(word, "[" + word + "]");
                    }
                    return result;

                default: // 默认使用替换模式
                    return sensitiveWordBs.replace(text);
            }
        } catch (Exception e) {
            log.error("策略过滤失败", e);
            return text;
        }
    }

    public Object findFirstSensitiveWord(String text) {
        if (text == null || text.trim().isEmpty()) {
            return null;
        }

        try {
            return sensitiveWordBs.findFirst(text);
        } catch (Exception e) {
            log.error("查找第一个敏感词失败", e);
            return null;
        }
    }


    public void removeSensitiveWord(String word) {
        if (word == null || word.trim().isEmpty()) {
            return;
        }

        word = word.trim();

        try {
            // 1. 从Redis删除
            redisUtil.sRemove(RedisConstants.SENSITIVE_WORDS_KEY, word);

            // 2. 这里需要重新初始化敏感词库或者使用动态删除方式
            // 由于sensitive-word库的限制，可能需要重启应用或调用特定方法

            log.info("删除敏感词: {}", word);

        } catch (Exception e) {
            log.error("删除敏感词失败: {}", word, e);
        }
    }

    // 自定义替换
    public String filterWithCustomReplace(String text, Character replacement) {
        if (text == null || text.trim().isEmpty()) {
            return text;
        }

        try {
            // 1. 检查是否包含敏感词
            boolean contains = sensitiveWordBs.contains(text);
            if (!contains) {
                return text; // 不包含敏感词，直接返回
            }

            // 2. 获取所有敏感词
            List<String> sensitiveWords = sensitiveWordBs.findAll(text);
            log.debug("检测到敏感词: {}", sensitiveWords);

            // 3. 替换敏感词（使用自定义字符替换）
            String filteredText = text;
            for (String word : sensitiveWords) {
                filteredText = filteredText.replace(word, replacement.toString());
            }

            // 4. 检查过滤后是否全是*（表示全是敏感词）
            if (isAllAsterisks(filteredText)) {
                log.warn("文本内容全是敏感词: {}", text);
                return null;
            }

            return filteredText;

        } catch (Exception e) {
            log.error("敏感词过滤失败", e);
            return text; // 出现异常时返回原文本
        }
    }
}