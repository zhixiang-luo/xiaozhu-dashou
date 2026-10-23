# GitHub 公开发布 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 把仓库整理成可公开维护、可重复构建的 GitHub 项目并发布。

**Architecture:** 先完成许可证、品牌、社区模板、README 和自动化检查，再执行密钥与隐私审计，最后创建公开仓库并推送。

**Tech Stack:** GitHub CLI、GitHub Actions、Markdown、MIT License、DCO。

**Spec:** `docs/superpowers/specs/2026-10-23-github-open-source-release-design.md`

## Global Constraints

- 仓库所有者：`zhixiang-luo`。
- 仓库名：`xiaozhu-dashou`。
- 可见性：公开。
- 代码与文档许可证：MIT。
- 项目名称、Logo 和猪哥角色保留为官方品牌。
- 发布前不得存在密钥、个人聊天、录音、真实联系人或 `.env`。

### Task 1: License and community files

**Files:**
- Create: `LICENSE`
- Create: `TRADEMARK.md`
- Create: `CODE_OF_CONDUCT.md`
- Create: `SUPPORT.md`
- Modify: `CONTRIBUTING.md`
- Modify: `SECURITY.md`
- Create: `.github/CODEOWNERS`
- Create: `.github/dependabot.yml`
- Create: `.github/pull_request_template.md`
- Create: `.github/ISSUE_TEMPLATE/bug.yml`
- Create: `.github/ISSUE_TEMPLATE/feature.yml`
- Create: `.github/ISSUE_TEMPLATE/compatibility.yml`

- [ ] **Step 1: Add MIT license, trademark boundary and Contributor Covenant.**
- [ ] **Step 2: Switch contribution policy to DCO and document sign-off.**
- [ ] **Step 3: Add issue forms, PR template, CODEOWNERS and Dependabot.**
- [ ] **Step 4: Validate all YAML and Markdown links.**

Expected: documentation checker passes.

### Task 2: Public README and release documentation

**Files:**
- Modify: `README.md`
- Create: `ROADMAP.md`
- Modify: `CHANGELOG.md`
- Modify: `docs/HANDOFF.md`
- Modify: `docs/10-acceptance-record.md`

- [ ] **Step 1: Rewrite README for first-time visitors.**

Include hero, product story, demo/status, architecture, quickstart, natural-language
examples, safety limits, skill extension, roadmap, contribution and license.

- [ ] **Step 2: Record real voice and DeepSeek acceptance evidence.**
- [ ] **Step 3: Run documentation and link checks.**

Expected: all links resolve.

### Task 3: Secret, privacy and security audit

**Files:**
- Create: `docs/release/v0.1.0-checklist.md`

- [ ] **Step 1: Scan tracked files and Git history for credentials.**
- [ ] **Step 2: Check ignored artifacts and remove public-release blockers.**
- [ ] **Step 3: Run Codex Security standard repository scan.**
- [ ] **Step 4: Enable GitHub private vulnerability reporting after repository creation.**

Expected: no open high-severity release blockers and no credentials.

### Task 4: GitHub publication

**Files:**
- Modify: `.github/workflows/*` only if clean-environment CI requires fixes.

- [ ] **Step 1: Run clean full verification.**

Run docs, TypeScript, tests, Android tests and APK build.

- [ ] **Step 2: Commit and push `feat/xiaozhu-dashou-v1`.**
- [ ] **Step 3: Create public `zhixiang-luo/xiaozhu-dashou`.**
- [ ] **Step 4: Fast-forward `main`, push, configure description/topics/homepage.**
- [ ] **Step 5: Open release `v0.1.0` with verified features and limitations.**
- [ ] **Step 6: Verify Actions, security settings and public README rendering.**
