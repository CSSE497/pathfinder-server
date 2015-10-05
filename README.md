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

#### Publis to GCP Docker server

```
activator docker:publish
gcloud docker push gcr.io/${PROJECT_ID}/pathfinder-server
```

### Tests

```
activator test
```
