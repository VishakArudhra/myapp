# myapp

A small Spring Boot service, used to practise build → publish → Chef deploy.

| Endpoint | What it shows |
|---|---|
| `GET /` | greeting, environment, `buildVersion` (from the jar) and `deployedVersion` (from Chef's config) |
| `GET /actuator/health` | `{"status":"UP"}` |

```bash
mvn spring-boot:run            # optional, needs JDK 21 + Maven on your Mac
curl localhost:8080/
```

Pushing to `main` triggers the Jenkins pipeline in `Jenkinsfile`:
checkout → build → unit tests → publish jar → Chef validation → deploy dev → uat → (approve) → prod.
