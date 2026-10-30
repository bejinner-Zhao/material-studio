-- 建表脚本：MySQL 8，字符集 utf8mb4
-- 由 docker-compose 挂载到 /docker-entrypoint-initdb.d，仅在数据卷首次初始化时执行

CREATE TABLE IF NOT EXISTS app_user (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    public_id   CHAR(36)     NOT NULL COMMENT '客户端 Cookie 中携带的匿名身份标识',
    nickname    VARCHAR(64)  NOT NULL,
    email       VARCHAR(128) NULL,
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_app_user_public_id (public_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '用户（轻量匿名身份）';

CREATE TABLE IF NOT EXISTS template (
    id                    VARCHAR(64)  NOT NULL,
    title                 VARCHAR(120) NOT NULL,
    category              VARCHAR(64)  NOT NULL,
    cover_url             VARCHAR(512) NOT NULL,
    description           VARCHAR(500) NOT NULL DEFAULT '',
    required_image_count  INT          NOT NULL DEFAULT 1,
    accepted_mime_types   VARCHAR(255) NOT NULL DEFAULT 'image/jpeg,image/png' COMMENT '逗号分隔的 MIME 白名单',
    max_file_size_bytes   BIGINT       NOT NULL DEFAULT 5242880,
    sort_order            INT          NOT NULL DEFAULT 0,
    enabled               TINYINT(1)   NOT NULL DEFAULT 1,
    created_at            DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_template_category (category, sort_order)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '制作模板';

CREATE TABLE IF NOT EXISTS upload_asset (
    id             BIGINT       NOT NULL AUTO_INCREMENT,
    user_id        BIGINT       NOT NULL,
    template_id    VARCHAR(64)  NULL,
    original_name  VARCHAR(255) NOT NULL,
    stored_path    VARCHAR(512) NOT NULL COMMENT '相对于 STORAGE_ROOT 的路径',
    mime_type      VARCHAR(100) NOT NULL,
    size_bytes     BIGINT       NOT NULL,
    created_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_upload_asset_user (user_id, created_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '用户上传的素材';

CREATE TABLE IF NOT EXISTS creation_task (
    id                BIGINT       NOT NULL AUTO_INCREMENT,
    user_id           BIGINT       NOT NULL,
    template_id       VARCHAR(64)  NOT NULL,
    template_title    VARCHAR(120) NOT NULL COMMENT '提交时的模板标题快照',
    template_cover_url VARCHAR(512) NOT NULL COMMENT '提交时的模板封面快照',
    status            VARCHAR(20)  NOT NULL COMMENT 'PENDING / RUNNING / SUCCEEDED / FAILED',
    stage             VARCHAR(30)  NULL COMMENT 'MATERIAL_CHECK / GENERATING / FINALIZING',
    error_stage       VARCHAR(30)  NULL,
    error_message     VARCHAR(500) NULL,
    result_url        VARCHAR(512) NULL,
    created_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    started_at        DATETIME     NULL,
    finished_at       DATETIME     NULL,
    updated_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_creation_task_user (user_id, created_at),
    KEY idx_creation_task_status (status, created_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '制作任务';

CREATE TABLE IF NOT EXISTS task_asset (
    task_id    BIGINT NOT NULL,
    asset_id   BIGINT NOT NULL,
    sort_order INT    NOT NULL DEFAULT 0,
    PRIMARY KEY (task_id, asset_id),
    KEY idx_task_asset_asset (asset_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '任务与素材的关联';

CREATE TABLE IF NOT EXISTS task_stage_log (
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    task_id    BIGINT       NOT NULL,
    stage      VARCHAR(30)  NOT NULL,
    status     VARCHAR(20)  NOT NULL COMMENT 'STARTED / SUCCEEDED / FAILED',
    message    VARCHAR(500) NULL,
    created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_task_stage_log_task (task_id, id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '任务执行阶段日志（可观察的执行边界）';
