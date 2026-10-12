package com.iot.qa.engine;

import com.iot.qa.service.ConfigService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 问答引擎入口：根据配置选择回答策略，并负责失败降级与兜底话术。
 *
 * <p>策略选择顺序：配置指定的策略 → 本地知识库检索 → 兜底提示。
 * 这样即使大模型接口超时或密钥失效，用户依然能得到知识库中的标准答案。</p>
 */
@Slf4j
@Service
public class AnswerEngine {

    private static final String FALLBACK_TEMPLATE =
            "抱歉，我暂时没有找到与「%s」相关的标准答案。\n"
                    + "你可以：\n"
                    + "1. 换一种更具体的说法，例如把「怎么弄」改成「如何注册账号」；\n"
                    + "2. 在下方「猜你想问」里选择一个已有问题；\n"
                    + "3. 联系管理员把该问题补充进知识库。";

    private final Map<String, AnswerStrategy> strategies = new HashMap<>();

    private final ConfigService configService;

    public AnswerEngine(List<AnswerStrategy> strategyList, ConfigService configService) {
        this.configService = configService;
        for (AnswerStrategy strategy : strategyList) {
            strategies.put(strategy.mode(), strategy);
        }
        log.info("问答引擎已加载策略: {}", strategies.keySet());
    }

    /**
     * 回答问题。
     */
    public AnswerResult ask(String question) {
        String mode = configService.get("ai.engine.mode", "local");
        AnswerStrategy primary = strategies.get(mode);

        if (primary != null) {
            try {
                AnswerResult result = primary.answer(question);
                if (result != null) {
                    return result;
                }
            } catch (Exception e) {
                log.warn("策略[{}]执行异常: {}", mode, e.getMessage());
            }
        }

        // 降级：非本地策略失败时回退到知识库检索
        if (!"local".equals(mode)) {
            AnswerStrategy local = strategies.get("local");
            if (local != null) {
                try {
                    AnswerResult result = local.answer(question);
                    if (result != null) {
                        result.setSource("knowledge");
                        return result;
                    }
                } catch (Exception e) {
                    log.warn("降级到本地检索同样失败: {}", e.getMessage());
                }
            }
        }

        return AnswerResult.ofFallback(String.format(FALLBACK_TEMPLATE, question));
    }
}