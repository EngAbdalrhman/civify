CREATE TABLE IF NOT EXISTS job_match_history (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    resume_id BIGINT,
    job_title VARCHAR(255),
    company_name VARCHAR(255),
    job_description TEXT,
    overall_score INT,
    skills_score INT,
    experience_score INT,
    keyword_score INT,
    match_result_json JSONB,
    analyzed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_job_match_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_job_match_resume FOREIGN KEY (resume_id) REFERENCES resumes (id) ON DELETE CASCADE
);

CREATE INDEX idx_job_match_history_user_id ON job_match_history(user_id);
