# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project state

A non-web Spring Boot 4.2.0 command-line job on Java 17, built with Maven. It checks clinicians' license expiration dates and sends a notification each time a license enters a new expiration window, without repeating ones already sent. What it does and how it's built are defined in `specs/` (see below); the implementation is in progress, task by task.

## Commands

Use the Maven wrapper (`./mvnw`, which pins Maven 3.10.1). You don't need a local Maven install.

- Build: `./mvnw clean package`
- Run the job (runs once and exits; no web server):
  - `./mvnw spring-boot:run -Dspring-boot.run.arguments="--licenses-file=licenses.csv"`
  - Forecast for another date (sends to output but records nothing): add `--as-of=YYYY-MM-DD`
  - `--sent-file=<path>` sets where sent notifications are remembered (default `sent-notifications.csv` in the working directory)
  - Packaged: `./mvnw package` then `java -jar target/license-expr-0.0.1-SNAPSHOT.jar --licenses-file=licenses.csv`
  - stdout carries only notification lines and logs go to stderr, but `spring-boot:run` merges both into Maven's output. Use the jar when you need clean stdout.
- Run all tests: `./mvnw test`
- Run a single test class: `./mvnw test -Dtest=LicenseExprApplicationTests`
- Run a single test method: `./mvnw test -Dtest=LicenseExprApplicationTests#contextLoads`

No linter or formatter is configured.

## Conventions and notes

- Base package is `org.license.licenseexpr`. Keep new components under it so `@SpringBootApplication` component scanning picks them up.
- The app is deliberately non-web: dependencies are `spring-boot-starter` and `spring-boot-starter-test` (test scope). Don't add a web starter; it would start an embedded server and keep the job from exiting.
- `pom.xml` contains empty `<licenses>`, `<developers>` and `<scm>` elements on purpose. They block inheritance of those elements from the Spring Boot parent POM. Don't delete them unless you want that inheritance.


# Spec-driven workflow
- Source of truth: specs/SPEC.md, specs/PLAN.md, specs/TASKS.md. Read them before any work.
- SPEC.md = behavior only, no tech. PLAN.md = how. TASKS.md = small ordered tasks with done conditions.
- Never write code until I've approved the spec, plan and tasks.
- If the spec and plan conflict, or something is ambiguous, stop and ask. Don't guess.
- If you find the spec or plan is wrong, propose the doc fix first. Don't patch around it in code.

# Task execution
- Do exactly ONE task per request, then stop. No work on later tasks.
- Test first where possible. Run `./mvnw test` before declaring a task done.
- A task is done only when its done condition is met.
