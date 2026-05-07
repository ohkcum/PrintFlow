# PrintFlowLite — Workflow State

> This file is the AI's dynamic brain. It tracks current state, the plan, embedded rules, and a running log. The AI reads this file at the start of every action and updates it after each step.

---

## State

```yaml
Status: IDLE
Phase: NONE
CurrentTask: Fix MissingResourceException for es_ES locale on login
Blocked: false
BlockReason: ""
LastUpdated: "2026-05-06"
```

---

## Plan

> Step-by-step plan generated during the BLUEPRINT phase. Each step should be concrete and verifiable.

- [ ] Step 1: (none)

---

## Rules

### Phase Definitions

| Phase | Trigger | Key Action |
|---|---|---|
| **ANALYZE** | User gives a task | Investigate codebase, understand context, identify affected files |
| **BLUEPRINT** | After ANALYZE | Create step-by-step plan in ## Plan section above |
| **CONSTRUCT** | After user approves plan | Implement code following the plan strictly |
| **VALIDATE** | After CONSTRUCT | Build, run tests, verify no lint errors |

### Error Handling During Workflow

- If a build fails: read the error, identify the root cause, fix it, and retry.
- If a test fails: analyze whether the test is correct or the code is wrong. Fix the code if incorrect. If the test is wrong, explain why and fix the test.
- If blocked on missing information: set `Blocked: true`, `BlockReason: <what is needed>`, and request input from the user.
- Never skip errors — always fix the root cause.

### Tool Usage in Each Phase

- **ANALYZE:** Read files, search code, explore structure. Use parallel tool calls for efficiency.
- **BLUEPRINT:** Write the plan to ## Plan. Request user confirmation by setting `Status: NEEDS_PLAN_APPROVAL`.
- **CONSTRUCT:** Edit/create files. Follow the plan exactly. Run `mvn compile` or `mvn test` to verify incrementally.
- **VALIDATE:** Run `mvn clean compile -DskipTests` to verify the build. Run `mvn test` to verify tests pass.

### Logging

After each significant action, append to ## Log:
- What was done
- What the result was
- Any decisions made
- Next step

Be concise. One to three lines per action.

### Construct Phase — Code Generation Rules

- Generate complete, functional code only — no TODOs, no placeholders, no incomplete sections.
- Follow the existing code style of the file being edited (matching style > "ideal" style).
- Include all necessary imports.
- Use conventional Java naming (camelCase for methods/variables, PascalCase for classes, UPPER_SNAKE_CASE for constants).
- Preserve the AGPL license header on existing files.
- Do not add extra functionality beyond what the plan specifies.
- Do not refactor surrounding code unless the task explicitly requires it.

### Autonomy Loop

1. **Read** this file to understand current State and Phase.
2. **Interpret** which Rule applies based on State and Phase.
3. **Act** using Cursor tools (edit code, run terminal commands).
4. **Update** State, Plan, and Log back into this file immediately.
5. **Repeat.**

---

## Log

- 2026-05-06: Investigated MissingResourceException for es_ES locale during login at /user#&ui-state=dialog. Root cause: `api/request/messages.xml` was empty (only had a comment, no entries). When the browser sent es_ES locale, the fallback to base bundle found no keys, causing MissingResourceException. Fix: Populated `api/request/messages.xml` with 124 message entries covering all keys used by `ReqMessageEnum` and all api/request classes. Build could not be validated as Maven is not available on this machine. XML syntax validated OK.
