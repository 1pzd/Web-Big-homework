package com.iot.qa.engine;

/**
 * 回答策略接口。
 *
 * <p>采用策略模式把「本地知识库检索」与「大模型生成」两种回答方式解耦，
 * 新增第三种方式（例如对接向量库）时只需实现本接口并在引擎中注册，符合开闭原则。</p>
 */
public interface AnswerStrategy {

    /** 策略标识，与配置项 ai.engine.mode 对应 */
    String mode();

    /**
     * 针对用户问题生成回答。
     *
     * @param question 用户原始问题
     * @return 回答结果；无法回答时返回 null，由引擎决定是否降级
     */
    AnswerResult answer(String question);
}