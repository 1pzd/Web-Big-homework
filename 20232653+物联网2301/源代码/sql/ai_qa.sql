-- =====================================================================
--  智问 · AI 智能问答平台  数据库初始化脚本
--  数据库：MySQL 8.0
--  字符集：utf8mb4
--  说明：脚本可重复执行（先删后建），包含表结构、索引与演示数据
-- =====================================================================

DROP DATABASE IF EXISTS `ai_qa`;
CREATE DATABASE `ai_qa` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE `ai_qa`;

-- ---------------------------------------------------------------------
-- 1. 系统用户表
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `sys_user`;
CREATE TABLE `sys_user`
(
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `username`    VARCHAR(50)  NOT NULL COMMENT '登录名，唯一',
    `password`    VARCHAR(100) NOT NULL COMMENT '密码，格式为 盐值:哈希值',
    `nickname`    VARCHAR(50)  NOT NULL DEFAULT '' COMMENT '昵称',
    `email`       VARCHAR(100)          DEFAULT NULL COMMENT '邮箱',
    `avatar`      VARCHAR(255)          DEFAULT NULL COMMENT '头像地址',
    `role`        VARCHAR(20)  NOT NULL DEFAULT 'USER' COMMENT '角色：ADMIN 管理员 / USER 普通用户',
    `status`      TINYINT      NOT NULL DEFAULT 1 COMMENT '状态：1 正常 / 0 禁用',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`     TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除：0 未删除 / 1 已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`),
    KEY `idx_status` (`status`),
    KEY `idx_role` (`role`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='系统用户表';

-- ---------------------------------------------------------------------
-- 2. 知识分类表
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `qa_category`;
CREATE TABLE `qa_category`
(
    `id`          BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    `name`        VARCHAR(50) NOT NULL COMMENT '分类名称',
    `sort`        INT         NOT NULL DEFAULT 0 COMMENT '排序值，越小越靠前',
    `description` VARCHAR(255)         DEFAULT NULL COMMENT '分类描述',
    `icon`        VARCHAR(50)          DEFAULT 'icon-default' COMMENT '图标标识',
    `create_time` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`     TINYINT     NOT NULL DEFAULT 0 COMMENT '逻辑删除：0 未删除 / 1 已删除',
    PRIMARY KEY (`id`),
    KEY `idx_sort` (`sort`),
    KEY `idx_deleted` (`deleted`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='知识分类表';

-- ---------------------------------------------------------------------
-- 3. 知识库条目表（问答引擎的检索数据源）
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `qa_knowledge`;
CREATE TABLE `qa_knowledge`
(
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `category_id` BIGINT                DEFAULT NULL COMMENT '所属分类',
    `question`    VARCHAR(255) NOT NULL COMMENT '标准问题',
    `answer`      TEXT         NOT NULL COMMENT '标准答案',
    `keywords`    VARCHAR(255)          DEFAULT '' COMMENT '关键词，英文逗号分隔',
    `hit_count`   INT          NOT NULL DEFAULT 0 COMMENT '命中次数',
    `status`      TINYINT      NOT NULL DEFAULT 1 COMMENT '状态：1 启用 / 0 停用',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`     TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除：0 未删除 / 1 已删除',
    PRIMARY KEY (`id`),
    KEY `idx_category` (`category_id`),
    KEY `idx_status` (`status`),
    KEY `idx_hit_count` (`hit_count`),
    KEY `idx_deleted` (`deleted`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='知识库条目表';

-- ---------------------------------------------------------------------
-- 4. 问答会话表
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `qa_session`;
CREATE TABLE `qa_session`
(
    `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id`       BIGINT       NOT NULL COMMENT '所属用户',
    `title`         VARCHAR(100) NOT NULL DEFAULT '' COMMENT '会话标题',
    `message_count` INT          NOT NULL DEFAULT 0 COMMENT '消息条数',
    `last_time`     DATETIME              DEFAULT NULL COMMENT '最后活跃时间',
    `create_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`       TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除：0 未删除 / 1 已删除',
    PRIMARY KEY (`id`),
    KEY `idx_user_last` (`user_id`, `last_time`),
    KEY `idx_deleted` (`deleted`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='问答会话表';

-- ---------------------------------------------------------------------
-- 5. 问答消息表
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `qa_message`;
CREATE TABLE `qa_message`
(
    `id`           BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    `session_id`   BIGINT      NOT NULL COMMENT '所属会话',
    `user_id`      BIGINT      NOT NULL COMMENT '所属用户',
    `role`         VARCHAR(20) NOT NULL COMMENT '角色：user 提问 / assistant 回答',
    `content`      TEXT        NOT NULL COMMENT '消息正文',
    `knowledge_id` BIGINT               DEFAULT NULL COMMENT '命中的知识条目 ID',
    `score`        DOUBLE               DEFAULT NULL COMMENT '相似度得分 0~1',
    `source`       VARCHAR(20)          DEFAULT NULL COMMENT '答案来源：knowledge / llm / fallback',
    `feedback`     TINYINT     NOT NULL DEFAULT 0 COMMENT '反馈：1 有帮助 / -1 没帮助 / 0 未评价',
    `cost_ms`      INT                  DEFAULT NULL COMMENT '引擎耗时（毫秒）',
    `create_time`  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `deleted`      TINYINT     NOT NULL DEFAULT 0 COMMENT '逻辑删除：0 未删除 / 1 已删除',
    PRIMARY KEY (`id`),
    KEY `idx_session` (`session_id`),
    KEY `idx_user` (`user_id`),
    KEY `idx_role_time` (`role`, `create_time`),
    KEY `idx_knowledge` (`knowledge_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='问答消息表';

-- ---------------------------------------------------------------------
-- 6. 系统参数表
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `sys_config`;
CREATE TABLE `sys_config`
(
    `id`           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `config_key`   VARCHAR(100) NOT NULL COMMENT '参数键',
    `config_value` VARCHAR(500)          DEFAULT '' COMMENT '参数值',
    `remark`       VARCHAR(255)          DEFAULT NULL COMMENT '参数说明',
    `create_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_config_key` (`config_key`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='系统参数表';

-- =====================================================================
--  初始化数据
-- =====================================================================

-- 用户：admin / admin123（管理员）；user / 123456；demo / 123456
INSERT INTO `sys_user` (`username`, `password`, `nickname`, `email`, `role`, `status`)
VALUES ('admin', 'a1b2c3d4e5f60718:9879a6c081970a685052bdaf0cba84347009c2f6f758c7a05257f5dcd6485ca8', '系统管理员', 'admin@iot.edu.cn', 'ADMIN', 1),
       ('user', '0f1e2d3c4b5a6978:05cbb087c5617ddbab3dd8a63a399f1b32f572b47dfee313ce7cc751751bbabf', '张同学', 'user@iot.edu.cn', 'USER', 1),
       ('demo', '9988776655443322:064c8db0a5f8a8b053868baa7c7fdd2b006109f13da1f5ce6b95c7a3fed15ec1', '演示账号', 'demo@iot.edu.cn', 'USER', 1);

-- 知识分类
INSERT INTO `qa_category` (`id`, `name`, `sort`, `description`, `icon`)
VALUES (1, '账号与登录', 1, '注册、登录、密码与个人资料相关的问题', 'icon-user'),
       (2, '课程与学习', 2, '平台使用技巧、学习相关的问题', 'icon-book'),
       (3, '校园生活', 3, '图书馆、食堂、宿舍、校园卡等生活服务', 'icon-life'),
       (4, '技术支持', 4, '网站访问异常、浏览器兼容、问题反馈', 'icon-tool'),
       (5, '人工智能', 5, '问答技术原理、知识库检索与大模型接入', 'icon-ai'),
       (6, '其他问题', 6, '未能归入以上分类的其他问题', 'icon-more');

-- 知识库条目
INSERT INTO `qa_knowledge` (`category_id`, `question`, `answer`, `keywords`, `hit_count`, `status`)
VALUES
-- 账号与登录
(1, '如何注册账号？',
 '点击首页右上角的「注册」按钮，填写用户名、密码、昵称与邮箱即可完成注册。用户名需为 4~20 位字母、数字或下划线，密码长度 6~32 位。注册成功后系统会自动跳转到登录页。',
 '注册,账号,新用户,创建账号,怎么注册', 18, 1),
(1, '忘记密码怎么办？',
 '目前可以联系系统管理员在后台重置密码。登录后也可以在「个人中心 - 修改密码」中自行更换密码，新密码同样需要满足 6~32 位长度要求。',
 '忘记密码,找回密码,重置密码,密码忘了', 12, 1),
(1, '账号被禁用了怎么办？',
 '账号被禁用通常是因为存在异常操作。请联系系统管理员说明情况，管理员核实后可以在后台「用户管理」中重新启用账号。',
 '禁用,账号异常,解封,无法登录', 5, 1),
(1, '如何修改个人资料？',
 '登录后进入「个人中心」，点击「编辑资料」即可修改昵称和邮箱。修改保存后立即生效，无需重新登录。',
 '修改资料,昵称,邮箱,个人信息,资料', 7, 1),
(1, '如何退出登录？',
 '点击页面右上角的用户头像，在下拉菜单中选择「退出登录」即可。退出后系统会清除本地登录凭证。',
 '退出,注销,登出,退出登录', 4, 1),

-- 课程与学习
(2, '如何查看我的问答历史？',
 '登录后点击导航栏的「问答历史」，即可看到你所有的会话列表。点击任意会话可以展开当时的完整对话，也可以删除不再需要的会话记录。',
 '历史,记录,问答历史,查看记录,会话', 21, 1),
(2, '支持哪些类型的提问？',
 '平台支持账号登录、校园生活、技术支持、人工智能等各类问题。提问时描述得越具体，检索到的答案就越准确，例如把「怎么弄」改成「如何注册账号」。',
 '提问类型,支持问题,能问什么,范围', 9, 1),
(2, '一次提问可以输入多少字？',
 '单个问题最长支持 500 个字符。如果问题较长，建议拆分成几个小问题分别提问，这样检索准确率更高。',
 '字数限制,长度,最多多少字,字符', 6, 1),
(2, '回答不准确怎么办？',
 '你可以在答案下方的「有帮助 / 没帮助」按钮上进行反馈，系统会记录你的评价用于优化知识库。也可以换一种更具体的问法重新提问，或联系管理员补充该问题。',
 '回答不准,答案错误,反馈,不准确,不对', 14, 1),
(2, '如何提高提问的准确率？',
 '建议使用完整的疑问句，并包含关键名词，例如「图书馆开放时间是什么时候」比「图书馆」更容易命中。也可以直接点击页面上的「猜你想问」推荐问题。',
 '提高准确率,提问技巧,怎么问,准确', 11, 1),

-- 校园生活
(3, '图书馆开放时间是什么时候？',
 '图书馆周一至周五开放时间为 8:00-22:00，周末及节假日为 9:00-21:00。考试周期间会延长至 23:00，具体以图书馆门口公告为准。',
 '图书馆,开放时间,自习,几点开门', 26, 1),
(3, '如何申请校园一卡通？',
 '新生入学时由学校统一发放校园一卡通。若卡片遗失，请携带学生证到一卡通服务中心（行政楼一层）办理补卡，补卡工本费 20 元。',
 '一卡通,校园卡,办理,补卡,遗失', 13, 1),
(3, '宿舍报修怎么操作？',
 '关注「后勤服务」公众号，进入「宿舍报修」菜单填写宿舍号、故障类型与联系方式即可提交。一般故障 24 小时内上门处理，水电类紧急故障 2 小时内响应。',
 '宿舍,报修,维修,水电,坏了', 16, 1),
(3, '校园网怎么连接？',
 '在宿舍或教学楼搜索无线信号「IOT-WLAN」，使用学号和统一身份认证密码登录即可。有线网络需要在宿舍墙上网口接入网线，并在认证页面登录。',
 '校园网,WiFi,上网,网络连接,无线', 19, 1),
(3, '食堂就餐时间？',
 '第一食堂：早餐 6:30-9:00，午餐 11:00-13:00，晚餐 17:00-19:30。第二食堂营业时间相同，另设夜宵档口 20:00-22:30。',
 '食堂,就餐,吃饭,开饭,餐厅', 8, 1),

-- 技术支持
(4, '网站打不开怎么办？',
 '请先确认网络连接是否正常，然后尝试刷新页面或更换浏览器。如果仍然无法访问，可能是服务正在维护，请稍后重试，或联系管理员反馈。',
 '打不开,无法访问,网页错误,502,访问不了', 15, 1),
(4, '系统支持哪些浏览器？',
 '推荐使用 Chrome 90 及以上、Edge 90 及以上版本，或 Firefox 88 及以上版本。系统采用响应式布局，也支持手机浏览器访问。',
 '浏览器,兼容性,Chrome,Edge,火狐', 10, 1),
(4, '如何反馈系统问题？',
 '可以在问答页面使用「没帮助」按钮标记问题答案，并联系管理员说明具体情况。反馈时请附上操作步骤和截图，便于快速定位问题。',
 '反馈,bug,问题反馈,报错,建议', 7, 1),
(4, '数据会保存多久？',
 '你的问答记录会长期保存在服务器数据库中，你可以随时在「问答历史」中查看或主动删除。平台不会将个人问答数据用于其他用途。',
 '数据保存,隐私,记录保存,数据安全', 5, 1),
(4, '网站是否支持手机访问？',
 '支持。网站采用响应式设计，在手机、平板和电脑上都能正常显示。手机端会自动收起侧边栏，问答输入框固定在页面底部便于输入。',
 '手机,移动端,响应式,适配,平板', 9, 1),

-- 人工智能
(5, '这个网站用的是什么问答技术？',
 '平台采用「知识库检索 + 大模型生成」双策略架构。默认使用本地检索：把用户问题与知识库条目的标准问题和关键词做加权相似度计算，取得分最高且超过阈值的条目作为答案，因此完全离线可用。',
 '技术,问答原理,算法,相似度,原理', 22, 1),
(5, '什么是知识库检索？',
 '知识库检索是指事先把常见问题与标准答案整理成结构化条目，用户提问时通过相似度算法在库中寻找最匹配的条目。它的优点是答案可控、响应快、不依赖外部服务。',
 '知识库,检索,匹配,条目', 17, 1),
(5, '支持接入大模型吗？',
 '支持。管理员可以在后台「系统配置」中把问答模式切换为 llm，并填写 OpenAI 兼容接口的地址、API Key 与模型名。若大模型调用失败，系统会自动降级回本地知识库检索。',
 '大模型,GPT,LLM,接入,API', 20, 1),
(5, '相似度阈值是什么意思？',
 '阈值是判定「是否命中知识库」的分数线。只有相似度得分高于阈值的条目才会被采用，否则返回兜底提示。默认阈值为 0.25，调高会更严格，调低则更容易命中。',
 '阈值,相似度,命中,分数线', 12, 1),

-- 其他
(6, '如何联系管理员？',
 '可以通过管理员邮箱 admin@iot.edu.cn 联系，或在问答页面留言反馈。管理员会在 1 个工作日内回复。',
 '联系管理员,客服,联系方式,邮箱', 6, 1),
(6, '这个平台是免费的吗？',
 '平台是课程实践作品，完全免费使用，不收取任何费用。',
 '免费,收费,价格,费用', 4, 1),
(6, '网站有哪些功能？',
 '前台提供智能问答、会话历史、答案反馈与个人中心；后台提供数据看板、知识库管理、分类管理、用户管理、问答记录管理与系统参数配置。',
 '功能,能做什么,介绍,模块', 11, 1);

-- 系统参数
INSERT INTO `sys_config` (`config_key`, `config_value`, `remark`)
VALUES ('ai.engine.mode', 'local', '问答模式：local 本地知识库检索 / llm 大模型生成'),
       ('ai.engine.threshold', '0.25', '本地检索命中阈值，低于该值返回兜底提示'),
       ('ai.llm.baseUrl', 'https://api.openai.com/v1', '大模型接口地址（OpenAI 兼容）'),
       ('ai.llm.apiKey', '', '大模型接口密钥，留空则自动降级为本地检索'),
       ('ai.llm.model', 'gpt-4o-mini', '大模型名称'),
       ('site.name', '智问 · AI 智能问答平台', '站点名称'),
       ('site.notice', '欢迎使用智问平台，输入问题即可获得基于知识库的智能解答。', '首页公告');

-- =====================================================================
--  演示数据：会话与消息（用于后台数据看板展示效果，可自行删除）
-- =====================================================================
INSERT INTO `qa_session` (`id`, `user_id`, `title`, `message_count`, `last_time`, `create_time`)
VALUES (1, 2, '图书馆开放时间是什么时候？', 4, DATE_SUB(NOW(), INTERVAL 5 DAY), DATE_SUB(NOW(), INTERVAL 5 DAY)),
       (2, 2, '支持接入大模型吗？', 4, DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY)),
       (3, 3, '宿舍报修怎么操作？', 2, DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY));

INSERT INTO `qa_message` (`session_id`, `user_id`, `role`, `content`, `knowledge_id`, `score`, `source`, `feedback`, `cost_ms`, `create_time`)
VALUES
-- 会话 1
(1, 2, 'user', '图书馆开放时间是什么时候？', NULL, NULL, NULL, 0, NULL, DATE_SUB(NOW(), INTERVAL 5 DAY)),
(1, 2, 'assistant', '图书馆周一至周五开放时间为 8:00-22:00，周末及节假日为 9:00-21:00。考试周期间会延长至 23:00，具体以图书馆门口公告为准。', 11, 0.912, 'knowledge', 1, 23, DATE_SUB(NOW(), INTERVAL 5 DAY)),
(1, 2, 'user', '周末也是这个时间吗', NULL, NULL, NULL, 0, NULL, DATE_SUB(NOW(), INTERVAL 5 DAY)),
(1, 2, 'assistant', '周末及节假日为 9:00-21:00，比工作日稍晚开放、提前闭馆。', 11, 0.634, 'knowledge', 1, 18, DATE_SUB(NOW(), INTERVAL 5 DAY)),
-- 会话 2
(2, 2, 'user', '支持接入大模型吗？', NULL, NULL, NULL, 0, NULL, DATE_SUB(NOW(), INTERVAL 2 DAY)),
(2, 2, 'assistant', '支持。管理员可以在后台「系统配置」中把问答模式切换为 llm，并填写 OpenAI 兼容接口的地址、API Key 与模型名。若大模型调用失败，系统会自动降级回本地知识库检索。', 23, 0.878, 'knowledge', 1, 31, DATE_SUB(NOW(), INTERVAL 2 DAY)),
(2, 2, 'user', '怎么切换问答模式', NULL, NULL, NULL, 0, NULL, DATE_SUB(NOW(), INTERVAL 2 DAY)),
(2, 2, 'assistant', '登录管理员账号后进入「系统配置」页面，把 ai.engine.mode 的值改为 llm 并保存即可，无需重启服务。', 23, 0.556, 'knowledge', 0, 26, DATE_SUB(NOW(), INTERVAL 2 DAY)),
-- 会话 3
(3, 3, 'user', '宿舍报修怎么操作？', NULL, NULL, NULL, 0, NULL, DATE_SUB(NOW(), INTERVAL 1 DAY)),
(3, 3, 'assistant', '关注「后勤服务」公众号，进入「宿舍报修」菜单填写宿舍号、故障类型与联系方式即可提交。一般故障 24 小时内上门处理，水电类紧急故障 2 小时内响应。', 13, 0.895, 'knowledge', -1, 20, DATE_SUB(NOW(), INTERVAL 1 DAY));

-- =====================================================================
--  校验
-- =====================================================================
SELECT '用户数' AS 项目, COUNT(*) AS 数量 FROM `sys_user`
UNION ALL SELECT '分类数', COUNT(*) FROM `qa_category`
UNION ALL SELECT '知识条目数', COUNT(*) FROM `qa_knowledge`
UNION ALL SELECT '会话数', COUNT(*) FROM `qa_session`
UNION ALL SELECT '消息数', COUNT(*) FROM `qa_message`
UNION ALL SELECT '参数数', COUNT(*) FROM `sys_config`;