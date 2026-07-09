-- users
CREATE TABLE users (
                       id                BIGSERIAL PRIMARY KEY,
                       phone_number      VARCHAR(20)  NOT NULL UNIQUE,
                       nickname          VARCHAR(50),
                       pin_hash          VARCHAR(60)  NOT NULL,
                       email             VARCHAR(100),
                       is_elderly_mode   BOOLEAN      NOT NULL DEFAULT FALSE,
                       created_at        TIMESTAMP    NOT NULL DEFAULT NOW(),
                       updated_at        TIMESTAMP    NOT NULL DEFAULT NOW(),
                       login_fail_count  INT          NOT NULL DEFAULT 0,
                       is_locked         BOOLEAN      NOT NULL DEFAULT FALSE,
                       fcm_token         VARCHAR(255),
                       is_onboarded      BOOLEAN      NOT NULL DEFAULT FALSE
);

-- whitelist
CREATE TABLE whitelist (
                           id           BIGSERIAL PRIMARY KEY,
                           user_id      BIGINT       NOT NULL REFERENCES users(id),
                           sender       VARCHAR(20)  NOT NULL,
                           created_at   TIMESTAMP    NOT NULL DEFAULT NOW()
);

-- detection_logs
CREATE TABLE detection_logs (
                                id             BIGSERIAL PRIMARY KEY,
                                user_id        BIGINT       NOT NULL REFERENCES users(id),
                                sender         VARCHAR(20),
                                content_hash   VARCHAR(255),
                                sms_type       VARCHAR(20),
                                llm_score      INT,
                                url_score      INT,
                                pattern_score  INT,
                                total_score    INT,
                                risk_level     VARCHAR(10) CHECK (risk_level IN ('HIGH', 'MEDIUM', 'LOW')),
                                ai_explanation TEXT,
                                detected_at    TIMESTAMP    NOT NULL DEFAULT NOW()
);

-- url_scan_results
CREATE TABLE url_scan_results (
                                  id             BIGSERIAL PRIMARY KEY,
                                  detection_log_id BIGINT     NOT NULL REFERENCES detection_logs(id),
                                  original_url   VARCHAR(500),
                                  expanded_url   VARCHAR(500),
                                  is_malicious   BOOLEAN,
                                  engine_count   INT,
                                  scanned_at     TIMESTAMP    NOT NULL DEFAULT NOW()
);

-- voice_detection_logs
CREATE TABLE voice_detection_logs (
                                      id             BIGSERIAL PRIMARY KEY,
                                      user_id        BIGINT       NOT NULL REFERENCES users(id),
                                      caller         VARCHAR(20),
                                      stt_text_hash  VARCHAR(255),
                                      total_score    INT,
                                      risk_level     VARCHAR(10) CHECK (risk_level IN ('HIGH', 'MEDIUM', 'LOW')),
                                      ai_summary     TEXT,
                                      call_duration  INT,
                                      detected_at    TIMESTAMP    NOT NULL DEFAULT NOW()
);

-- reports
CREATE TABLE reports (
                         id                    BIGSERIAL PRIMARY KEY,
                         user_id               BIGINT    NOT NULL REFERENCES users(id),
                         detection_log_id      BIGINT    REFERENCES detection_logs(id),
                         voice_detection_log_id BIGINT   REFERENCES voice_detection_logs(id),
                         report_type           VARCHAR(20),
                         created_at            TIMESTAMP NOT NULL DEFAULT NOW()
);

-- chatbot_logs
CREATE TABLE chatbot_logs (
                              id               BIGSERIAL PRIMARY KEY,
                              user_id          BIGINT    NOT NULL REFERENCES users(id),
                              detection_log_id BIGINT    REFERENCES detection_logs(id),
                              role             VARCHAR(20),
                              message          TEXT,
                              created_at       TIMESTAMP NOT NULL DEFAULT NOW()
);

-- family_links
CREATE TABLE family_links (
                              id           BIGSERIAL PRIMARY KEY,
                              protected_id BIGINT       NOT NULL REFERENCES users(id),
                              protector_id BIGINT       NOT NULL REFERENCES users(id),
                              invite_code  VARCHAR(20),
                              status       VARCHAR(20),
                              created_at   TIMESTAMP    NOT NULL DEFAULT NOW()
);

-- trend_stats
CREATE TABLE trend_stats (
                             id           BIGSERIAL PRIMARY KEY,
                             stat_month   VARCHAR(7)   NOT NULL,
                             sms_type     VARCHAR(20),
                             count        INT,
                             top_keywords JSONB,
                             updated_at   TIMESTAMP    NOT NULL DEFAULT NOW()
);

-- 인덱스
CREATE INDEX idx_detection_logs_user_id ON detection_logs(user_id);
CREATE INDEX idx_detection_logs_risk_level ON detection_logs(risk_level);
CREATE INDEX idx_whitelist_user_id ON whitelist(user_id);
CREATE INDEX idx_voice_detection_logs_user_id ON voice_detection_logs(user_id);
CREATE INDEX idx_chatbot_logs_user_id ON chatbot_logs(user_id);
CREATE INDEX idx_reports_user_id ON reports(user_id);
CREATE INDEX idx_url_scan_results_detection_log_id ON url_scan_results(detection_log_id);
CREATE INDEX idx_family_links_protected_id ON family_links(protected_id);
CREATE INDEX idx_family_links_protector_id ON family_links(protector_id);
CREATE INDEX idx_trend_stats_stat_month ON trend_stats(stat_month);
CREATE INDEX idx_users_phone_number ON users(phone_number);