package com.itheima.controller;

import com.itheima.pojo.Result;
import com.itheima.service.impl.SensitiveFilterService;
import com.itheima.utils.ThreadLocalUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/sensitive")
@RequiredArgsConstructor
public class SensitiveWordController {

    private final SensitiveFilterService sensitiveFilterService;

    /**
     * 检查文本是否包含敏感词
     */
    @PostMapping("/check")
    public Result<Map<String, Object>> checkSensitiveWords(@RequestBody Map<String, String> params) {
        try {
            String text = params.get("text");
            if (text == null || text.trim().isEmpty()) {
                return Result.error("文本不能为空");
            }

            boolean contains = sensitiveFilterService.containsSensitiveWord(text);
            List<String> sensitiveWords = sensitiveFilterService.findAllSensitiveWords(text);

            return Result.success(Map.of(
                    "contains", contains,
                    "sensitiveWords", sensitiveWords,
                    "sensitiveWordCount", sensitiveWords.size(),
                    "firstSensitiveWord", sensitiveFilterService.findFirstSensitiveWord(text)
            ));

        } catch (Exception e) {
            log.error("检查敏感词失败", e);
            return Result.error("检查失败");
        }
    }

    /**
     * 过滤敏感词
     */
    @PostMapping("/filter")
    public Result<Map<String, Object>> filterText(@RequestBody Map<String, Object> params) {
        try {
            String text = (String) params.get("text");
            String strategy = (String) params.getOrDefault("strategy", "replace");

            if (text == null || text.trim().isEmpty()) {
                return Result.error("文本不能为空");
            }

            String filteredText;
            if ("strict".equals(strategy)) {
                filteredText = sensitiveFilterService.filterWithStrategy(text, "strict");
            } else {
                filteredText = sensitiveFilterService.filter(text);
            }

            boolean isBlocked = filteredText == null;
            String resultText = isBlocked ? "" : filteredText;

            return Result.success(Map.of(
                    "original", text,
                    "filtered", resultText,
                    "isBlocked", isBlocked,
                    "containsSensitive", sensitiveFilterService.containsSensitiveWord(text),
                    "strategy", strategy
            ));

        } catch (Exception e) {
            log.error("过滤敏感词失败", e);
            return Result.error("过滤失败");
        }
    }

    /**
     * 添加敏感词
     */
    @PostMapping("/add")
    public Result<String> addSensitiveWord(@RequestBody Map<String, String> params) {
        try {
            Map<String, Object> claims = ThreadLocalUtil.get();
            Integer userId = (Integer) claims.get("id");
            String word = params.get("word");

            if (word == null || word.trim().isEmpty()) {
                return Result.error("敏感词不能为空");
            }

            // 验证权限（只有管理员可以添加敏感词）
            // 这里需要根据实际业务添加权限验证

            sensitiveFilterService.addSensitiveWord(word);

            log.info("用户 {} 添加敏感词: {}", userId, word);

            return Result.success("添加成功");

        } catch (Exception e) {
            log.error("添加敏感词失败", e);
            return Result.error("添加失败");
        }
    }

    /**
     * 批量添加敏感词
     */
    @PostMapping("/batch-add")
    public Result<String> batchAddSensitiveWords(@RequestBody Map<String, Object> params) {
        try {
            Map<String, Object> claims = ThreadLocalUtil.get();
            Integer userId = (Integer) claims.get("id");
            @SuppressWarnings("unchecked")
            List<String> words = (List<String>) params.get("words");

            if (words == null || words.isEmpty()) {
                return Result.error("敏感词列表不能为空");
            }

            // 验证权限
            sensitiveFilterService.addSensitiveWords(words);

            log.info("用户 {} 批量添加敏感词，数量: {}", userId, words.size());

            return Result.success("批量添加成功");

        } catch (Exception e) {
            log.error("批量添加敏感词失败", e);
            return Result.error("批量添加失败");
        }
    }

    /**
     * 删除敏感词
     */
    @DeleteMapping("/remove/{word}")
    public Result<String> removeSensitiveWord(@PathVariable("word") String word) {
        try {
            Map<String, Object> claims = ThreadLocalUtil.get();
            Integer userId = (Integer) claims.get("id");

            if (word == null || word.trim().isEmpty()) {
                return Result.error("敏感词不能为空");
            }

            // 验证权限
            sensitiveFilterService.removeSensitiveWord(word);

            log.info("用户 {} 删除敏感词: {}", userId, word);

            return Result.success("删除成功");

        } catch (Exception e) {
            log.error("删除敏感词失败", e);
            return Result.error("删除失败");
        }
    }

    /**
     * 重新加载敏感词库
     */
    @PostMapping("/reload")
    public Result<String> reloadSensitiveWords() {
        try {
            Map<String, Object> claims = ThreadLocalUtil.get();
            Integer userId = (Integer) claims.get("id");

            // 验证权限（管理员）
            sensitiveFilterService.reloadSensitiveWords();

            log.info("用户 {} 重新加载敏感词库", userId);

            return Result.success("重新加载成功");

        } catch (Exception e) {
            log.error("重新加载敏感词库失败", e);
            return Result.error("重新加载失败");
        }
    }

    /**
     * 获取敏感词统计信息
     */
    @GetMapping("/statistics")
    public Result<Map<String, Object>> getStatistics() {
        try {
            // 验证权限
            Map<String, Object> stats = sensitiveFilterService.getStatistics();
            return Result.success(stats);

        } catch (Exception e) {
            log.error("获取敏感词统计信息失败", e);
            return Result.error("获取失败");
        }
    }

    /**
     * 测试敏感词过滤
     */
    @PostMapping("/test")
    public Result<Map<String, Object>> testFilter(@RequestBody Map<String, Object> params) {
        try {
            String text = (String) params.get("text");
            String strategy = (String) params.getOrDefault("strategy", "replace");
            Character replacement = params.containsKey("replacement") ?
                    ((String) params.get("replacement")).charAt(0) : '*';

            if (text == null || text.trim().isEmpty()) {
                return Result.error("测试文本不能为空");
            }

            String filteredText;
            if ("custom".equals(strategy)) {
                filteredText = sensitiveFilterService.filterWithCustomReplace(text, replacement);
            } else {
                filteredText = sensitiveFilterService.filterWithStrategy(text, strategy);
            }

            List<String> sensitiveWords = sensitiveFilterService.findAllSensitiveWords(text);

            return Result.success(Map.of(
                    "original", text,
                    "filtered", filteredText,
                    "sensitiveWords", sensitiveWords,
                    "strategy", strategy,
                    "replacement", replacement,
                    "containsSensitive", !sensitiveWords.isEmpty(),
                    "wordCount", text.length(),
                    "filteredWordCount", filteredText != null ? filteredText.length() : 0
            ));

        } catch (Exception e) {
            log.error("测试敏感词过滤失败", e);
            return Result.error("测试失败");
        }
    }
}