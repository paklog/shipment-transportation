# Docker Development Guide

This guide provides instructions on how to build, run, and manage this application using Docker and Docker Compose. This is the recommended way to run the service for local development.

## Prerequisites

- [Docker](https://docs.docker.com/get-docker/) installed and running on your machine.
- [Docker Compose](https://docs.docker.com/compose/install/) (usually included with Docker Desktop).

## Running the Application with Docker Compose (Recommended)

This method will build the application image and start all the necessary services (MongoDB, Kafka) in one command.

### 1. Start the Services

From the root directory of the project, run:

```bash
docker-compose up --build -d
```

- `--build`: This flag tells Docker Compose to build the application image from the `Dockerfile` before starting the services.
- `-d`: This flag runs the containers in detached mode (in the background).

### 2. Viewing Logs

To see the logs from the running application container, use the following command:

```bash
docker-compose logs -f app
```

### 3. Accessing the Application

Once the application has started, you can access it at `http://localhost:8080`.

- **Health Check:** Verify that the service is running by accessing the health check endpoint:
  ```bash
  curl http://localhost:8080/actuator/health
  ```
- **API:** You can now use the Postman collection or `curl` to interact with the API endpoints as described in the onboarding documentation.

### 4. Stopping the Services

To stop all the running containers, use the following command:

```bash
docker-compose down
```

This will stop and remove the containers and the network created by Docker Compose.

## Building the Docker Image Manually

If you only want to build the Docker image without running the services, you can use the standard `docker build` command:

```bash
docker build -t shipment-transportation:latest .
```

- `-t shipment-transportation:latest`: This tags the image with the name `shipment-transportation` and the tag `latest`.
- `.`: This specifies that the build context is the current directory (where the `Dockerfile` is located).
