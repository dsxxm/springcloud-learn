# Component Learning Skill

## Purpose

Use this process when learning a Spring Cloud, Spring Cloud Alibaba, middleware, observability, security, or distributed-systems component from a course and applying it to a local codebase.

## Input contract

Collect these before writing materials:

1. Course article, screenshots, source snippets, or repository supplied by the learner;
2. Official documentation for definitions, current behavior, production constraints, and APIs;
3. Existing local project code, if the learner asks how that project uses the component;
4. Learner constraints: version line, desired project location, whether they want a runnable project or only a skeleton.

Treat learner-supplied course content as the target learning route. Use official documentation to correct or qualify version-specific claims, not to silently replace the route with a different stack.

## Required deliverables

For component `<name>`, create:

```text
springcloud-learn/<name>/
├── README.md        # concepts, architecture, references
├── 实验手册.md       # precise hands-on instructions
└── demo/            # Maven skeleton only unless source generation is explicitly requested
```

Also create/update an Obsidian note containing concepts and **sourced** interview questions. If requested, write a separate technical blog; do not copy a learner's incident/debugging history into a general tutorial unless explicitly asked.

## Maven skeleton rule

Default skeleton contents:

- parent `pom.xml` with the course-compatible Spring Boot / Spring Cloud / Spring Cloud Alibaba BOMs;
- module `pom.xml` files with exactly the required dependencies;
- empty `src/main/java/<package>/` and `src/main/resources/` directory trees.

Do **not** generate controllers, application classes, YAML files, demo business code, Docker Compose, or test code unless the learner explicitly requests code generation. The manual may contain complete copyable code blocks without placing that code in `demo/`.

## Manual quality contract

Every experiment step must contain all of the following:

1. **Working directory**: exact directory from which to run a command;
2. **Target file path**: full workspace-relative path;
3. **Final file contents**: complete YAML/XML/Java content, not a fragment such as “set port to 8308”;
4. **Why**: explain the component interaction and the relevant invariant;
5. **Command**: exact startup, curl, or test command;
6. **Expected observation**: response, log, control-plane state, or error status;
7. **Boundary**: distinguish direct URI, service-discovery URI, component responsibility, and production constraints.

For a cross-service call, show the exact request path end-to-end:

```text
client request → gateway/consumer handler → service-name resolution → load balancer → provider endpoint → response
```

Never leave a sentence like “forward to the user service” without showing the actual code and target URL.

## Research and interview-question rule

1. Locate a maintained, identifiable question source for the question titles;
2. Verify answers against primary documentation;
3. Cite the question source and the official source after the relevant answer;
4. Keep course-version details distinct from current-version behavior;
5. Do not invent “high-frequency interview questions” from the current demo project.

## Project analysis rule

When mapping a real repository to a component list:

- classify each item as **implemented**, **indirectly used**, **not implemented**, or **documentation-only**;
- require dependency/configuration/code evidence before calling it implemented;
- identify substitutes precisely, for example `Sa-Token` is not Spring Authorization Server, and raw AMQP is not Spring Cloud Stream;
- report outdated README claims when source and dependencies disagree.

## Verification

- For a skeleton: run Maven validation or compilation; clean generated `target/` output afterwards.
- For a behavior change: launch the actual service chain and exercise the changed path.
- For documents: verify every mentioned file path exists or is explicitly labelled as a learner-created path; verify links and terminology against source.

## Completion checklist

- [ ] Course route retained and version boundaries documented.
- [ ] Official references present.
- [ ] README explains concepts and component boundaries.
- [ ] Manual contains exact paths, full files, commands, expected outputs.
- [ ] Demo contains only the scope requested by the learner.
- [ ] Obsidian note has externally sourced interview questions and official answer references.
- [ ] Build/runtime verification matches the actual deliverable.
