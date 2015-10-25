# Pathfinder Backend Server
[![Build Status](https://travis-ci.org/CSSE497/pathfinder-server.svg?branch=dev)](https://travis-ci.org/CSSE497/pathfinder-server)
[![Coverage Status](https://coveralls.io/repos/CSSE497/pathfinder-server/badge.svg?branch=dev&service=github)](https://coveralls.io/github/CSSE497/pathfinder-server?branch=dev)
[![Code Climate](https://codeclimate.com/github/CSSE497/pathfinder-server/badges/gpa.svg)](https://codeclimate.com/github/CSSE497/pathfinder-server)
[![Stories in Ready](https://badge.waffle.io/CSSE497/pathfinder-server.svg?label=ready&title=Ready)](http://waffle.io/CSSE497/pathfinder-server)
[![Stories in Progress](https://badge.waffle.io/CSSE497/pathfinder-server.svg?label=In%20Progress&title=In%20Progress)](http://waffle.io/CSSE497/pathfinder-server)
[![Stories under Review](https://badge.waffle.io/CSSE497/pathfinder-server.svg?label=Under%20Review&title=Under%20Review)](http://waffle.io/CSSE497/pathfinder-server)

This is the magic behind Pathfinder. This server provides the Rest API that is the Pathfinder service.

This is a placeholder file until we create a more comprehensive README.

## Running the server

### Locally

```
activator run
```

### Docker

#### Prepare Dockerfile

```
activator docker:stage
```

#### Publish to local Docker server

```
activator docker:publishLocal
docker run -p 9000:9000 pathfinder-server:1.0-SNAPSHOT
```

#### Publish new release to GCP Docker server

1. Increase the version number in `build.sbt` and tag the repository

    ```
    vi build.sbt
    git tag -a v0.1.1
    git push --tag
    ```

2. Ensure that Docker is up and running

    ```
    docker-machine start default
    eval "$(docker-machine env default)"
    ```

3. Authenticate Docker to GCR

    ```
    docker login -e <email> -u _token -p "$(gcloud auth print-access-token)" https://beta.gcr.io
    ```


4. Build the image and push it to GCR (as defined in build.sbt)

    ```
    activator docker:publish
    ```

5. Update the pods

    ```
    kubectl rolling-update 
    ```

### Tests

```
activator test
```
