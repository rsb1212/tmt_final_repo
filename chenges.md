# Repository Module Enhancement - Development Guide

## Objective

Enhance the Repository Module with:

1. Dynamic Top-Level Repository Creation
2. Dynamic Folder Management
3. Bulk File & Folder Upload
4. Advanced Enterprise Search
5. Role-Based Access Control

---

# 1. Dynamic Top-Level Repository Creation

## Requirement

Allow ADMIN users to create new repository spaces from UI.

### Example

```text
Repository
│
├── AGILIC
├── OPUS
├── Data Migration
├── Group Policy
├── Life Asia
├── CRM
└── Payments
```

### UI

Button:

```text
+ Create Repository Space
```

Fields:

```text
Repository Name *
Description
Icon
Color
```

### Backend API

```http
POST /api/repository/modules
```

### Validation

```text
Repository Name Mandatory
Repository Name Unique
ADMIN Only
 
# 2. Dynamic Folder Structure Management

## Requirement

Allow users to create folders/subfolders dynamically.

### Folder Structure

```text
AGILIC
│
└── Product A
    │
    ├── NB & UW
    │   ├── Module 1
    │   └── Module 2
    │
    ├── CRT
    ├── Claims
    └── Taxation
```

### Actions

```text
+ Folder
Rename Folder
Move Folder
Delete Folder
Copy Folder
```

### Backend

Reuse:

```java
createNode()
updateNode()
deleteNode()
```

Add:

```java
moveNode()
copyNode()
 
# 3. Multiple File Upload

## Requirement

Support:

```text
Single File
Multiple Files
Drag & Drop
```

### API

```http
POST /api/repository/upload-multiple
```

### Request

```java
List<MultipartFile> files
```

### Upload Flow

```text
Select Files
    ↓
Upload
    ↓
Store Files
    ↓
Create Metadata
 
# 4. Folder Upload

## Requirement

Allow complete folder upload while preserving structure.

### User Upload

```text
NB_UW
├── BRD
│   ├── BRD.docx
│   └── BRD_v2.docx
│
├── API
│   ├── Request.json
│   └── Response.json
│
└── TestCases
    └── TC.xlsx
```

### Repository After Upload

```text
Repository
│
└── AGILIC
    │
    └── NB_UW
        │
        ├── BRD
        ├── API
        └── TestCases
```

### UI

```html
<input type="file" webkitdirectory multiple>
```

Button:

```text
Upload Folder
```

### Backend API

```http
POST /api/repository/upload-folder
```

### Processing Flow

```text
Receive Files
      ↓
Read Relative Path
      ↓
Create Missing Folders
      ↓
Store Files
      ↓
Create Metadata
```

### Database Enhancement

Add fields:

```java
relativePath
folderName
uploadedFolderName
```

to:

```java
RepositoryNodeDocument
 
# 5. Document Preview

## Requirement

Preview files without downloading.

### Supported Formats

```text
PDF
DOCX
XLSX
PPTX
PNG
JPG
TXT
```

### API

```http
GET /api/repository/document/{id}/preview
```

### Actions

```text
Preview
Download
Version History
 
# 6. Advanced Enterprise Search

## Search Scope

Search across:

```text
Repository Names
Folder Names
Document Names
Document Content
Confluence Pages
Comments
Descriptions
Tags
Projects
 
## Search Architecture

```text
                     User Search
                           │
                           ▼
                  Global Search API
                           │
         ┌─────────────────┼─────────────────┐
         │                 │                 │
         ▼                 ▼                 ▼
      Trie Index     Inverted Index   Full Text Search
         │                 │                 │
         └─────────────────┼─────────────────┘
                           │
                           ▼
                     Ranking Engine
                           │
                           ▼
                    Search Results
 
## Level 1: Auto Suggest Search

### Example

User Types:

```text
Pro
```

Suggestions:

```text
Product
Product Name
Product Testing
Product APIs
```

### Implementation

```java
TrieNode
TrieService
SearchSuggestionService
 
## Level 2: Inverted Index Search

### Structure

```text
api     → doc1, doc2, doc3
claims  → doc5, doc6
policy  → doc7, doc8
```

### Reindex On

```text
Upload
Update
Delete
Page Save
Comment Save
 
## Level 3: PostgreSQL Full Text Search

### Database

```sql
ALTER TABLE repository_node
ADD COLUMN search_vector tsvector;
```

### Index

```sql
CREATE INDEX idx_repo_search
ON repository_node
USING GIN(search_vector);
 
## Level 4: Fuzzy Search

### Example

Search:

```text
polcy
```

Results:

```text
policy
policy servicing
policy document
```

### Implementation

```text
Levenshtein Distance
pg_trgm
 
## Search Ranking

### Weight Calculation

```text
Document Name Match = 10
Folder Match        = 8
Page Title Match    = 8
Content Match       = 6
Description Match   = 4
Comment Match       = 2
 
## Search API

```http
GET /api/repository/search?q=api
```

### Response

```json
{
  "results": [
    {
      "type": "DOCUMENT",
      "name": "API Specification",
      "path": "AGILIC/NB_UW/API",
      "score": 98
    }
  ]
}
 
# 7. Security & Permissions

## ADMIN

```text
Create Repository
Delete Repository
Create Folder
Upload Files
Upload Folders
Edit Documents
Manage Access
```

## MANAGER

```text
Create Folder
Upload Files
Edit Documents
View Repository
```

## SME

```text
Upload Files
Update Documents
View Repository
```

## TESTER

```text
View Documents
Download Documents
Comments
Favorites
 
# Development Sequence

## Sprint 1

```text
Repository Creation
Folder Management
Role Permissions
```

## Sprint 2

```text
Multiple File Upload
Folder Upload
Document Preview
```

## Sprint 3

```text
Auto Suggest Search
Inverted Index
Global Search API
```

## Sprint 4

```text
Full Text Search
Fuzzy Search
Search Ranking
 
# Expected Final Structure

```text
Repository
│
├── Unlimited Top-Level Repositories
│
├── Dynamic Folder Structure
│
├── Multiple File Upload
│
├── Folder Upload
│
├── Document Preview
│
├── Confluence Pages
│
├── Version History
│
├── Comments
│
├── Role-Based Access
│
└── Enterprise Search
