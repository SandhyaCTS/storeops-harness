# Deployment: local Docker

The app is a Spring Boot fat jar. The image contains only a Java 17 runtime and that jar, so the build happens outside Docker and the image is small and quick to build.

## Build and run
```bash
mvn -B verify                       # builds target/storeops-api-0.1.0-SNAPSHOT.jar and runs every gate
docker build -t storeops-api .
docker run --rm -p 8080:8080 --name storeops storeops-api
```
The container runs as a non-root user and listens on port 8080. Storage is in memory, so data is lost when the container stops.

## Prove the new endpoint responds
Seed staff: `s1` is a STORE_MANAGER and `s2` is an ASSOCIATE.
```bash
H='Content-Type: application/json'
# create two activities, note their ids
curl -s -X POST localhost:8080/api/activities -H "$H" -d '{"storeId":"store-1","title":"Restock aisle 3"}'
curl -s -X POST localhost:8080/api/activities -H "$H" -d '{"storeId":"store-1","title":"Audit freezers"}'

# 1. manager succeeds (duplicates count once)
curl -s -i -X POST localhost:8080/api/activities/bulk-status -H "$H" -H 'X-Staff-Id: s1' \
  -d '{"activityIds":["<id1>","<id2>","<id1>"],"status":"DONE"}'

# 2. non-manager is refused
curl -s -i -X POST localhost:8080/api/activities/bulk-status -H "$H" -H 'X-Staff-Id: s2' \
  -d '{"activityIds":["<id1>"],"status":"TODO"}'

# 3. one unknown id rejects the whole batch
curl -s -i -X POST localhost:8080/api/activities/bulk-status -H "$H" -H 'X-Staff-Id: s1' \
  -d '{"activityIds":["<id1>","ghost"],"status":"TODO"}'
```
Expected: 1 returns `200` with both activities as `DONE`; 2 returns `403` with `"code":"FORBIDDEN"`; 3 returns `404` with `"code":"NOT_FOUND"` naming `ghost`, and `<id1>` is still `DONE` afterwards.

## Evidence
> Not yet captured. Run the commands above against the running container and paste the real output (or a screenshot) here. Do not edit it by hand.

Date run: _____  Image id (`docker images storeops-api`): _____

```
(paste curl output here)
```

## Not done
Cloud deployment (AWS, Azure, GCP) was not attempted.
