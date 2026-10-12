package com.iot.qa.engine;

import lombok.Data;

import java.io.Serializable;

/**
 * 问答引擎的原始输出结果。
 */
@Data
public class AnswerResult implements Serializable {

    /** 回答内容 */
    private String answer;

    /** 答案来源：knowledge / llm / fallback */
    private String source;

    /** 相似度得分，0~1；大模型直接回答时为 null */
    private Double score;

    /** 命中的知识条目 ID */
    private Long knowledgeId;

    /** 命中的标准问题 */
    private String matchedQuestion;

    public static AnswerResult ofKnowledge(String answer, double score, Long knowledgeId, String matchedQuestion) {
        AnswerResult result = new AnswerResult();
        result.setAnswer(answer);
        result.setSource("knowledge");
        result.setScore(score);
        result.setKnowledgeId(knowledgeId);
        result.setMatchedQuestion(matchedQuestion);
        return result;
    }

    public static AnswerResult ofLlm(String answer) {
        AnswerResult result = new AnswerResult();
        result.setAnswer(answer);
        result.setSource("llm");
        return result;
    }

    public static AnswerResult ofFallback(String answer) {
        AnswerResult result = new AnswerResult();
        result.setAnswer(answer);
        result.setSource("fallback");
        result.setScore(0D);
        return result;
    }
}