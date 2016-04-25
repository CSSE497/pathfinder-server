# Pathfinder API Server
[![Build Status](https://travis-ci.org/CSSE497/pathfinder-server.svg?branch=dev)](https://travis-ci.org/CSSE497/pathfinder-server)
[![Coverage Status](https://coveralls.io/repos/CSSE497/pathfinder-server/badge.svg?branch=dev&service=github)](https://coveralls.io/github/CSSE497/pathfinder-server?branch=dev)

This server exposes [https://api.thepathfinder.xyz](https://api.thepathfinder.xyz), the websocket API that powers the Pathfinder SDKs. Pathfinder users do not need to be familiar with this service as it is entirely wrapped by the [Pathfinder SDKs](https://pathfinder.readme.io/docs/platform-support).


## Development Guide
These instructions exist mostly as a reference for the development team.

### Local development
The server can be run and debugged locally with

    activator "~run"

### Standalone release
A standalone release can be built with

    activator dist

This release can be run with

    unzip -d /opt target/universal/pathfinder-server-<version>.zip
    /opt/pathfinder-server-<version>/bin/pathfinder-server -Dhttp.port=80 -Dhttps.port=443 -Dplay.server.https.keyStore.path=<path to jks> -Dplay.server.https.keyStore.password=<jks password>

### Docker

A Docker image cant be built with

    activator docker:publishLocal

The image can be run locally with

    docker run -p 9000:9000 pathfinder-server:<version>

The image can be pushed to gcloud with

    gcloud docker push beta.gcr.io/<gcloud project id>/pathfinder-server:<version>

The running Docker container can be updated with

    kubectl rolling-update pathfinder-server --image=beta.gcr.io/<gcloud project id>/pathfinder-server:<version>

#### Detailed GCR release instructions

1. Increase the version number in `build.sbt` and tag the repository


    vi build.sbt
    git tag -a v0.1.1
    git push --tag

2. Ensure that Docker is up and running


    docker-machine start default
    eval "$(docker-machine env default)"

3. Authenticate Docker to GCR


    docker login -e <email> -u _token -p "$(gcloud auth print-access-token)" https://beta.gcr.io

4. Build the image and push it to GCR (as defined in build.sbt)


    activator docker:publish

5. Update the pods


    kubectl rolling-update pathfinder-server --image=beta.gcr.io/<gcloud project id>/pathfinder-server:<version>

## License

[MIT](https://raw.githubusercontent.com/CSSE497/pathfinder-server/master/LICENSE).
