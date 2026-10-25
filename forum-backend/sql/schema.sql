-- ═══════════════════════════════════════════════════════════════════
-- 咖啡港湾 COFFEE HARBOR · 数据库结构（幂等，可重复执行）
--
-- 说明：
--   1. 全部使用 CREATE TABLE IF NOT EXISTS，不会 DROP 任何已存在的数据；
--   2. 应用启动时 DatabaseUpgrader 会补齐后续新增的列/表，本文件仅用于首次建库；
--   3. 未建立物理外键：删除/计数一致性由服务层事务保证（见 README「数据一致性」）。
-- 用法：
--   mysql -u root -p -e "CREATE DATABASE IF NOT EXISTS forum DEFAULT CHARSET utf8mb4"
--   mysql -u root -p forum < sql/schema.sql
-- ═══════════════════════════════════════════════════════════════════
SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS `users` (
  `id`         BIGINT       NOT NULL AUTO_INCREMENT,
  `username`   VARCHAR(50)  NOT NULL,
  `password`   VARCHAR(255) NOT NULL COMMENT 'BCrypt 哈希',
  `nickname`   VARCHAR(50)  DEFAULT NULL,
  `avatar`     VARCHAR(200) DEFAULT NULL,
  `email`      VARCHAR(100) DEFAULT NULL,
  `role`       VARCHAR(20)  NOT NULL DEFAULT 'USER',
  `points`     INT          NOT NULL DEFAULT 0,
  `created_at` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_users_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `posts` (
  `id`            BIGINT       NOT NULL AUTO_INCREMENT,
  `title`         VARCHAR(200) NOT NULL,
  `content`       MEDIUMTEXT,
  `category`      VARCHAR(20)  NOT NULL COMMENT 'GAME=单机游戏, ANIME=动漫',
  `images`        TEXT COMMENT 'JSON 数组，如 ["/picture/a.jpg"]',
  `links`         TEXT,
  `videos`        TEXT COMMENT '视频链接 JSON 数组，如 ["https://www.bilibili.com/video/BV1xx411c7mD"]',
  `user_id`       BIGINT       DEFAULT NULL,
  `author_name`   VARCHAR(50)  DEFAULT NULL,
  `like_count`    INT          NOT NULL DEFAULT 0,
  `collect_count` INT          NOT NULL DEFAULT 0,
  `comment_count` INT          NOT NULL DEFAULT 0,
  `is_top`        TINYINT(1)   NOT NULL DEFAULT 0,
  `is_essence`    TINYINT(1)   NOT NULL DEFAULT 0,
  `tags`          VARCHAR(255) DEFAULT NULL COMMENT 'JSON 数组，如 ["攻略","只狼"]',
  `created_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_posts_category` (`category`),
  KEY `idx_posts_user` (`user_id`),
  KEY `idx_posts_created` (`created_at`),
  KEY `idx_posts_hot` (`like_count`, `collect_count`, `comment_count`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `comments` (
  `id`         BIGINT      NOT NULL AUTO_INCREMENT,
  `content`    TEXT        NOT NULL,
  `post_id`    BIGINT      NOT NULL,
  `user_id`    BIGINT      NOT NULL,
  `user_name`  VARCHAR(50) DEFAULT NULL,
  `parent_id`  BIGINT      DEFAULT NULL COMMENT '父评论 ID，NULL 表示主评论',
  `created_at` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_comments_post` (`post_id`, `parent_id`),
  KEY `idx_comments_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `likes` (
  `id`         BIGINT   NOT NULL AUTO_INCREMENT,
  `user_id`    BIGINT   NOT NULL,
  `post_id`    BIGINT   NOT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_likes_user_post` (`user_id`, `post_id`),
  KEY `idx_likes_post` (`post_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `collects` (
  `id`         BIGINT   NOT NULL AUTO_INCREMENT,
  `user_id`    BIGINT   NOT NULL,
  `post_id`    BIGINT   NOT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_collects_user_post` (`user_id`, `post_id`),
  KEY `idx_collects_post` (`post_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `tags` (
  `id`         BIGINT      NOT NULL AUTO_INCREMENT,
  `name`       VARCHAR(50) NOT NULL,
  `post_count` INT         NOT NULL DEFAULT 0,
  `created_at` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tags_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `sign_ins` (
  `id`         BIGINT   NOT NULL AUTO_INCREMENT,
  `user_id`    BIGINT   NOT NULL,
  `sign_date`  DATE     NOT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_sign_user_date` (`user_id`, `sign_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `notifications` (
  `id`         BIGINT       NOT NULL AUTO_INCREMENT,
  `user_id`    BIGINT       NOT NULL,
  `type`       VARCHAR(20)  NOT NULL COMMENT 'COMMENT / LIKE',
  `content`    VARCHAR(255) NOT NULL,
  `related_id` BIGINT       DEFAULT NULL COMMENT '关联帖子 ID',
  `is_read`    TINYINT(1)   NOT NULL DEFAULT 0,
  `created_at` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_notifications_user` (`user_id`, `is_read`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `announcements` (
  `id`         BIGINT       NOT NULL AUTO_INCREMENT,
  `title`      VARCHAR(100) NOT NULL,
  `content`    TEXT,
  `is_active`  TINYINT(1)   NOT NULL DEFAULT 1,
  `created_at` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
