-- Projects created before users existed keep a NULL owner and are not visible to anyone.
ALTER TABLE projects ADD COLUMN owner_id BIGINT;
ALTER TABLE projects ADD CONSTRAINT fk_projects_owner FOREIGN KEY (owner_id) REFERENCES users (id);
CREATE INDEX idx_projects_owner_id ON projects (owner_id);
