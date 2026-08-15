CREATE TABLE IF NOT EXISTS preconsult_submission_symptom_category (
    submission_id BIGINT NOT NULL,
    symptom_category VARCHAR(50) NOT NULL
);

DO $migration$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'uk_preconsult_submission_symptom_category'
          AND conrelid = 'preconsult_submission_symptom_category'::regclass
    ) THEN
        ALTER TABLE preconsult_submission_symptom_category
            ADD CONSTRAINT uk_preconsult_submission_symptom_category
            UNIQUE (submission_id, symptom_category);
    END IF;
END;
$migration$;

DO $migration$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'fk_preconsult_submission_symptom_category_submission'
          AND conrelid = 'preconsult_submission_symptom_category'::regclass
          AND confdeltype = 'c'
    ) THEN
        ALTER TABLE preconsult_submission_symptom_category
            DROP CONSTRAINT IF EXISTS
                fk_preconsult_submission_symptom_category_submission;
        ALTER TABLE preconsult_submission_symptom_category
            ADD CONSTRAINT fk_preconsult_submission_symptom_category_submission
            FOREIGN KEY (submission_id)
            REFERENCES preconsult_submission(submission_id)
            ON DELETE CASCADE;
    END IF;
END;
$migration$;

INSERT INTO preconsult_submission_symptom_category (
    submission_id,
    symptom_category
)
SELECT
    submission_id,
    symptom_category
FROM preconsult_submission
WHERE symptom_category IS NOT NULL
ON CONFLICT (submission_id, symptom_category) DO NOTHING;
