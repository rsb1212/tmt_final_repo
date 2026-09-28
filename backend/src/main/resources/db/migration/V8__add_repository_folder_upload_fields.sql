-- V8: Support folder upload metadata on repository node documents
-- (matches chenges.md — RepositoryNodeDocument fields relativePath, folderName, uploadedFolderName)

ALTER TABLE repository_node_documents
    ADD COLUMN IF NOT EXISTS relative_path         VARCHAR(1000),
    ADD COLUMN IF NOT EXISTS folder_name           VARCHAR(255),
    ADD COLUMN IF NOT EXISTS uploaded_folder_name  VARCHAR(255);

CREATE INDEX IF NOT EXISTS idx_repo_doc_uploaded_folder
    ON repository_node_documents (uploaded_folder_name);

CREATE INDEX IF NOT EXISTS idx_repo_doc_folder_name
    ON repository_node_documents (folder_name);
