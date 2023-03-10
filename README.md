
# Node.js Express starter application

This starter application is a simple microservice that incorporates the [Express web framework](https://expressjs.com/) to provide a landing page and two endpoints. Use this repository as a template for your own application.

A microservice is an individual component of an application that follows the **microservice architecture** - an architectural style that structures an application as a collection of loosely coupled services, each of which implements business capability. The microservice exposes a RESTful API matching a [Swagger](http://swagger.io) definition.

You can access the cloud native microservice capabilities at the following endpoints:
- The [Swagger UI](http://swagger.io/swagger-ui/) is running on: `/swagger/api-docs`
- Health endpoint: `/health`

The microservice is ready to run locally in a Docker container or with the Node.js runtime that is hosted on your local operating system. 


> **Note:** This application does not connect to external services. 

## What's included

- Node.js application that functions as a microservice (server.js)
- Rudimentary landing page and two endpoints (/health and /swagger/api-docs)
- Extensible Node server code structure with directories for config, routes, and controllers
- Simple functional and unit tests
- Experience test script to verify your UI when the app is running
- Dockerfiles for container deployment


## Configuring your starter application

This application comes ready to run and requires no explicit configuration.

### Using the Docker CLI

```bash
docker build .
docker run -p 3000:3000 <my_image_id>
```

You can find the image ID in the console output of the docker build command. The `-p` flag represents `publish` and is necessary to expose the local endpoint outside the container.

### Using the native runtime

```bash
npm install
npm start
```

### Verifying that your local app is running

Your application is running at `http://localhost:3000`. Check the endpoints that are provided by the microservice.

## Testing your app

The starter app repo contains unit tests, functional tests, and an experience test script to check the user-facing elements (UI and endpoints) that are presented by your application. The starter app also includes a linting mechanism.

### Running tests and code coverage

To run tests and code coverage, use the following command:

```bash
 npm run test
 ```
 
  A `coverage` folder is created with code coverage results that can be reviewed for gaps. The code coverage thresholds are also defined in `package.json` under `nyc` and can be adjusted if needed. Also, you can use the script `npm run fix` to automatically fix linting problems.
