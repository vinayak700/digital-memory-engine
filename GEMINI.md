## Project Overview

This is a Java Spring Boot project that implements a "Digital Memory Engine". It's designed to store and manage memories, their outcomes, and relationships between them. The project uses a PostgreSQL database hosted on Supabase, with Flyway for database migrations. It's built with a "foundation-first" approach, prioritizing a solid and safe schema before implementing business logic.

The project is structured as a standard Maven project. The main application logic is in the `src/main/java` directory, and database migrations are in `src/main/resources/db/migration`.

## Building and Running

To build and run the project, you can use the following Maven commands:

*   **Build:**
    ```bash
    ./mvnw clean install
    ```
*   **Run:**
    ```bash
    ./mvnw spring-boot:run
    ```

The application will start on port 8082.

## Testing

To run the tests, use the following Maven command:

```bash
./mvnw test
```

## Development Conventions

*   **Database Migrations:** Database schema changes are managed by Flyway. Migration scripts are located in `src/main/resources/db/migration`. Hibernate's `ddl-auto` is set to `validate`, so it does not make any schema changes.
*   **Code Style:** The project uses Lombok to reduce boilerplate code.
*   **Dependencies:** Project dependencies are managed in the `pom.xml` file.

**NOTE:** There is a discrepancy between the schema defined in the JPA entities and the initial Flyway migration script (`V1__initial_schema.sql`). The entities describe a more complete schema than what is in the migration script. This suggests that the developer might be using a different method for schema generation that is not captured in the current project configuration.
