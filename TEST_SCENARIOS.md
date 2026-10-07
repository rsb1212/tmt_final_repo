# Test Management Tool (TMT) — Master Test Scenarios

**Version:** 1.0  **Date:** 2026-10-01  **Scope:** End-to-end UAT / regression suite covering authentication, team-based data isolation (chenges.md § Plan A), core workflow (Projects → Modules → Test Cases → Execution → Defects → Reports), administration, and non-functional / security concerns.

**Legend**
- **P**riority: `P1` critical · `P2` high · `P3` medium
- **Role**: SUPER = super-admin (`isSuperAdmin=true`), TA = team-admin (role=ADMIN, isSuperAdmin=false), MGR = manager, SME, TST = tester, VWR = viewer
- Teams used in tests: **DM**, **NB**, **Claims**
   ## 1. Authentication & SSO (TC‑001 – TC‑012)

| ID | Priority | Role | Scenario | Steps | Expected Result |
|---|---|---|---|---|---|
| TC-001 | P1 | any | Fresh visit to root auto-redirects to IDEM | Clear cookies → open `https://testgenii.bajajlife.com/` | Browser lands on Keycloak login page via `/api/v1/auth/idem/login` |
| TC-002 | P1 | any | Successful SSO lands on dashboard | Complete IDEM login with valid user | URL becomes `/`; dashboard renders; `localStorage.token` present |
| TC-003 | P1 | any | Login with case-mismatch email (`Someshwar.Phadatare@…`) | IDEM returns mixed-case email; DB has lower-case | User is found via `findByEmailIgnoreCase`; login succeeds |
| TC-004 | P1 | any | Login with username-only (`preferred_username` = `Someshwar.Phadatare`) | Keycloak sends no email claim | Fallback `findByUsernameIgnoreCase` matches; login succeeds |
| TC-005 | P2 | any | User not in DB | IDEM returns email with no row in `users` | Redirects to `/login?sso=error&reason=user_not_registered` with friendly message |
| TC-006 | P2 | any | Disabled user | DB row has `active=false` | Redirects with `reason=user_disabled`; no token issued |
| TC-007 | P1 | any | Logout breaks the SSO loop | Click Logout button | Lands on `/login`, shows "You have been signed out" message, **does NOT** auto-redirect to IDEM |
| TC-008 | P1 | any | Logout then click "Sign in" | Logout → click "Try IDEM Sign-in Again" | Full IDEM round-trip; user re-enters app |
| TC-009 | P2 | any | Hard refresh during SSO round-trip | Reload `/login` while Keycloak redirect in flight | 30-s sentinel prevents 2nd `/idem/login` call; no `invalid_session` error |
| TC-010 | P2 | any | Open 2 tabs simultaneously | Tab A SSO in progress → open Tab B | Only one `/idem/login` fires per tab; both land on dashboard |
| TC-011 | P1 | TST | 401 on API call does not loop | Delete `localStorage.token` manually, hit any API | Interceptor fires `window.location.assign('/login')` once (rate-limited 2 s) |
| TC-012 | P3 | any | Expired JWT | Wait > 2 h | Next API call returns 401; interceptor redirects; SSO renews session |

## 2. Team-Based Data Isolation — Plan A (TC‑013 – TC‑030)

| ID | Priority | Role | Scenario | Steps | Expected Result |
|---|---|---|---|---|---|
| TC-013 | P1 | SUPER | Sees all teams' projects | Login as `admin@testmgmt.io` → open Projects | List contains projects from DM, NB, Claims |
| TC-014 | P1 | TA (DM) | Team Admin sees only own team's projects | Login as DM team admin | List contains only DM projects; `/projects` API returns filtered set |
| TC-015 | P1 | MGR (NB) | Manager restricted to own team | Login as NB manager → Projects | Only NB projects visible |
| TC-016 | P1 | TST (DM) | Direct GET on cross-team project returns 403 | `GET /api/v1/projects/{nb_project_id}` as DM tester | HTTP 403; message: "You do not have access to this project's data." |
| TC-017 | P1 | TST (DM) | Cannot see NB test cases | `GET /api/v1/test-cases?projectId={nb_id}` | HTTP 403 |
| TC-018 | P1 | TST (DM) | Global test case list is team-scoped | `GET /api/v1/test-cases` (no projectId) | Only DM test cases returned |
| TC-019 | P1 | TST (DM) | Cannot submit execution on cross-team TC | `POST /api/v1/executions` with `testCaseId` from NB | HTTP 403 |
| TC-020 | P1 | TST (DM) | Cannot update cross-team execution | `PUT /api/v1/executions/{nb_exec_id}` | HTTP 403 |
| TC-021 | P1 | MGR (DM) | Cannot delete cross-team execution | `DELETE` NB execution | HTTP 403 |
| TC-022 | P1 | TST (DM) | Cannot raise defect on NB project | `POST /api/v1/defects` with NB `projectId` | HTTP 403 |
| TC-023 | P1 | TST (DM) | Cannot update NB defect status | `PATCH /api/v1/defects/{nb_defect_id}/status` | HTTP 403 |
| TC-024 | P1 | MGR (DM) | Requirements list is team-scoped | GET requirements for own project OK; for NB project 403 | Correct 200/403 split |
| TC-025 | P1 | MGR (DM) | Cannot link NB test case to DM requirement | `POST /api/v1/requirements/link` with NB tcId | HTTP 403 |
| TC-026 | P1 | TA (DM) | Manager Dashboard numbers are team-only | Open Manager Dashboard | Pass/Fail counts reflect DM data only; no NB counts leaked |
| TC-027 | P1 | TST (DM) | Global search is team-scoped | Search for a term that matches both DM & NB test cases | Only DM matches returned |
| TC-028 | P2 | SUPER | Super admin executes any team's TC | POST execution for NB TC as super admin | HTTP 201 |
| TC-029 | P2 | user with `teamId=NULL` | Legacy user still sees everything | Create user without team, login | Backward-compat bypass — can see all |
| TC-030 | P1 | TST (DM) | Project with `teamId=NULL` is shared | Super admin creates project without team → DM tester views | Shared project visible to all teams (by design) |

## 3. Project Management (TC‑031 – TC‑040)

| ID | Priority | Role | Scenario | Steps | Expected |
|---|---|---|---|---|---|
| TC-031 | P1 | TA (DM) | Create project — team auto-inherited | POST project with no `teamId` as DM team admin | Project saved with `teamId=DM` |
| TC-032 | P1 | SUPER | Create project with explicit teamId | POST with `teamId=NB` | Project owned by NB |
| TC-033 | P2 | TA (DM) | Attempt to create project for another team | POST with `teamId=NB` as DM team admin | HTTP 403 or silently coerced to DM team |
| TC-034 | P2 | TA (DM) | Update own project | PUT project with new name | 200; audit record created |
| TC-035 | P1 | TA (DM) | Update NB project | PUT NB project | HTTP 403 |
| TC-036 | P2 | TA (DM) | Delete own project | DELETE | 204 |
| TC-037 | P1 | TA (DM) | Delete NB project | DELETE NB project | HTTP 403 |
| TC-038 | P3 | MGR | List archived projects | Filter list by `active=false` | Only own-team archived shown |
| TC-039 | P2 | TA | Duplicate project name within team | Create 2 projects with same name | 409 Conflict |
| TC-040 | P3 | VWR | Viewer can read but not mutate | Login as viewer → attempt PUT | 403 |

## 4. Modules & Test Cases (TC‑041 – TC‑060)

| ID | Priority | Role | Scenario | Steps | Expected |
|---|---|---|---|---|---|
| TC-041 | P1 | MGR | Create module under own project | POST module | 201 |
| TC-042 | P1 | MGR | Create module under NB project | POST module with NB projectId | 403 |
| TC-043 | P1 | MGR | Create test case (DRAFT) | POST test case | 201, status=DRAFT, code=TC-NNN |
| TC-044 | P1 | TST | Edit DRAFT test case | PUT | 200 |
| TC-045 | P2 | TST | Edit RELEASED test case | PUT | 409 (immutable per workflow) |
| TC-046 | P2 | MGR | Reassign test case to tester | PATCH assignedTo | 200; appears on tester's My Cases |
| TC-047 | P1 | MGR | Forward DRAFT → PENDING_SME_REVIEW | Call workflow endpoint | Status transitions; SME queue populated |
| TC-048 | P1 | SME | Approve test case | SME review approve | SME_APPROVED |
| TC-049 | P1 | SME | Reject test case with notes | SME review reject | Returns to DRAFT with review note |
| TC-050 | P2 | MGR | Delete test case | DELETE | Executions + attachments cascaded; defects unlinked |
| TC-051 | P1 | TST | Cannot delete cross-team TC | DELETE NB TC | 403 |
| TC-052 | P2 | MGR | Bulk import 100 test cases via Excel | Upload XLSX | All created; errors reported per row |
| TC-053 | P2 | MGR | Bulk import with duplicate titles | Upload XLSX with duplicates | Returns per-row conflict messages |
| TC-054 | P3 | MGR | Export test cases to Excel | Click Export | Download XLSX with current filter applied |
| TC-055 | P2 | MGR | Attach file to test case (<10 MB) | Upload PDF | 201; download works |
| TC-056 | P2 | MGR | Attach file > 10 MB | Upload | 413 Payload Too Large |
| TC-057 | P2 | MGR | Attach executable (.exe) | Upload | Rejected by MIME filter |
| TC-058 | P1 | MGR | Test case versioning | Edit title; view history | 2 versions visible with diff |
| TC-059 | P2 | MGR | Restore previous version | Click restore | Test case reverts; new version row created |
| TC-060 | P3 | MGR | Tag a test case | Add tag "regression" | Tag persisted, searchable |

## 5. Execution Workflow (TC‑061 – TC‑075)

| ID | Priority | Role | Scenario | Steps | Expected |
|---|---|---|---|---|---|
| TC-061 | P1 | TST | Submit PASSED execution | POST execution with all steps PASSED | 201; `isLatest=true`; TC status updated |
| TC-062 | P1 | TST | Submit FAILED execution with defect | POST FAILED + create linked defect | Execution + defect linked |
| TC-063 | P2 | TST | Partial execution (steps missing results) | POST with incomplete steps | 400 BadRequest |
| TC-064 | P2 | TST | Re-execute test case | 2nd execution | runNumber=2; prior `isLatest=false` |
| TC-065 | P1 | TST | Execute on DEPRECATED TC | Try to execute | 400 WorkflowException |
| TC-066 | P2 | TST | Attach screenshot to execution | Upload | File saved under `uploads/test_execution/{execId}/` |
| TC-067 | P2 | MGR | Update tester's execution | PUT someone else's execution as MGR | 200 |
| TC-068 | P2 | TST | Update another tester's execution | PUT other TST's exec | 403 (owner check) |
| TC-069 | P1 | TST | Cross-team execution | Execute NB TC as DM TST | 403 |
| TC-070 | P2 | MGR | View execution history for a TC | GET history endpoint | List includes all runs sorted desc |
| TC-071 | P3 | MGR | Execution summary dashboard | GET summary | Pass/Fail/Blocked/Skipped/InProgress counts correct |
| TC-072 | P3 | MGR | Daily tracking page shows today's runs | Open Daily Tracking | Runs executed today listed |
| TC-073 | P2 | MGR | Filter executions by environment (SIT/UAT/PROD) | Query with env param | Results filtered |
| TC-074 | P3 | MGR | Build version filter | Query with `buildVersion` | Only matching executions |
| TC-075 | P2 | MGR | Delete execution | DELETE | Step executions + attachments cascaded |

## 6. Defects (TC‑076 – TC‑083)

| ID | Priority | Role | Scenario | Steps | Expected |
|---|---|---|---|---|---|
| TC-076 | P1 | TST | Raise defect from execution | Click "Raise Defect" on FAILED run | Defect created, linked, status=NEW |
| TC-077 | P2 | TST | Raise standalone defect | New defect without executionId | 201 |
| TC-078 | P1 | MGR | Update defect status NEW → ASSIGNED → FIXED → CLOSED | Sequence of status updates | Workflow transitions all allowed |
| TC-079 | P2 | MGR | Invalid status transition (CLOSED → NEW) | PATCH | 400 (if workflow enforces) |
| TC-080 | P2 | MGR | Assign defect to a tester | PATCH assignedTo | 200; appears in assignee's inbox |
| TC-081 | P1 | TST | Cross-team defect read | GET NB defect | 403 |
| TC-082 | P2 | MGR | Create Jira issue from defect | Click "Push to Jira" | External Jira key stored in `jira_issue_key` |
| TC-083 | P3 | MGR | Filter defects by severity | Query `?severity=CRITICAL` | Only CRITICAL defects |

## 7. Dashboards & Reports (TC‑084 – TC‑090)

| ID | Priority | Role | Scenario | Steps | Expected |
|---|---|---|---|---|---|
| TC-084 | P1 | MGR | Manager dashboard filtered by team | Open dashboard | Counts/charts reflect own team only |
| TC-085 | P1 | SUPER | Super admin dashboard cross-team | Open dashboard | Shows all teams; no filtering applied |
| TC-086 | P2 | SME | SME Dashboard pending reviews | Open SME dashboard | List limited to assigned SME + own team |
| TC-087 | P2 | TST | My Productivity page | Open page as tester | Only own stats shown |
| TC-088 | P2 | MGR | Team Productivity page | Open page | Breakdown per tester within team |
| TC-089 | P2 | MGR | Workload page shows team members | Open workload | Only own-team users listed |
| TC-090 | P3 | MGR | Module Breakdown chart | Open module breakdown | Aggregated by module within team's projects |

## 8. Admin / Team / User Management (TC‑091 – TC‑095)

| ID | Priority | Role | Scenario | Steps | Expected |
|---|---|---|---|---|---|
| TC-091 | P1 | SUPER | Access Tenants page | Visit `/tenants` | Page loads; CRUD controls visible |
| TC-092 | P1 | TA | Tenants page hidden | Login as team admin → sidebar | "Tenants" menu NOT shown; direct `/tenants` → redirect to `/` |
| TC-093 | P1 | TA | Add member to own team | Open Manage Members → add user | Success; checkbox styling intact (fixed earlier) |
| TC-094 | P2 | TA | Remove member from own team | Click Remove | user.teamId cleared |
| TC-095 | P1 | SUPER | Promote user to Super Admin | UPDATE SQL / future UI toggle | user.isSuperAdmin=true; after re-login sees all |

## 9. Security / Non-Functional (TC‑096 – TC‑100)

| ID | Priority | Scenario | Steps | Expected |
|---|---|---|---|---|
| TC-096 | P1 | SQL injection in search | Enter `' OR 1=1 --` in global search | Treated as literal; no row leakage |
| TC-097 | P1 | XSS in test case description | Submit `<script>alert(1)</script>` | Rendered as text; no script execution |
| TC-098 | P1 | Rate limit on login | 6 rapid IDEM login attempts | 6th returns 429 (per RateLimitFilter 5/min) |
| TC-099 | P2 | Session cookie flags | Inspect `TMTSESSION` cookie | `HttpOnly; Secure; SameSite=None; Path=/` |
| TC-100 | P1 | Secrets not in logs | Trigger any error after fix | `tmt-backend.log` contains no JWT, no bind params, no password |
   ## Appendix A — Test Data Setup

```sql
-- Minimum seed to run suite
INSERT INTO teams (id, name, active) VALUES
  (gen_random_uuid(), 'DM',     true),
  (gen_random_uuid(), 'NB',     true),
  (gen_random_uuid(), 'Claims', true);

-- Super Admin (already exists after V10):
--   admin@testmgmt.io  → role=ADMIN, is_super_admin=TRUE

-- One Team Admin per team (demote role=ADMIN super-admins or create new):
-- UPDATE users SET team_id=<DM_id>,   is_super_admin=FALSE WHERE email='ta.dm@…';
-- UPDATE users SET team_id=<NB_id>,   is_super_admin=FALSE WHERE email='ta.nb@…';

-- At least 2 Managers, 2 SMEs, 3 Testers per team.
-- At least 1 Project per team, 3 Modules, 10 Test Cases, 5 Executions, 2 Defects.

## Appendix B — Execution Checklist (per release)

- [ ] All P1 scenarios pass (49 scenarios)
- [ ] All P2 scenarios pass (37 scenarios)
- [ ] All P3 scenarios pass (14 scenarios)
- [ ] No cross-team data visible to non-super-admin on any screen
- [ ] No PII in log files (`grep -i 'password\|bind' logs/*.log` returns empty)
- [ ] Nginx access log shows no `401` → `/idem/login` loop
- [ ] Logout flow does not auto-re-authenticate within same tab

## Appendix C — Traceability to `chenges.md`

| chenges.md requirement | Covered by |
|---|---|
| Team-based data isolation | TC-013 … TC-030 |
| Mandatory team ownership | TC-031, TC-032 |
| Super Admin override | TC-013, TC-028, TC-085, TC-091, TC-095 |
| Team-wise Project / Module / TC / Defect / Dashboard / Report Isolation | TC-014‑TC-027, TC-084, TC-086, TC-089 |
| Ownership validation on CUD | TC-033‑TC-037, TC-050‑TC-051, TC-068‑TC-069 |
| Role-based access control | TC-040, TC-091‑TC-095 |
   **Total: 100 scenarios** · P1 = 49 · P2 = 37 · P3 = 14
