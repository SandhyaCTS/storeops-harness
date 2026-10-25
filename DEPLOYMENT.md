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

Date run: 25th October 2026  Image id (`docker images storeops-api`): Image:storeops-api:latest   ID: 1a9abe8e1a96 
<img width="975" height="509" alt="image" src="https://github.com/user-attachments/assets/18902206-6d74-47fa-8436-0ccf33b74334" />

```
$ H='Content-Type: application/json'
# create two activities, note their ids
curl -s -X POST localhost:8080/api/activities -H "$H" -d '{"storeId":"store-1","title":"Restock aisle 3"}'
$ $ {"id":"ca137179-36be-4c9a-92b1-c60cb1c1c450","storeId":"store-1","programmeId":null,"title":"Restock aisle 3","status":"TODO","priority":"MEDIUM","category":"GENERAL","assigneeId":null,"ownerId":null}$ 
$ 
$ curl -s -X POST localhost:8080/api/activities -H "$H" -d '{"storeId":"store-1","title":"Audit freezers"}'

{"id":"1f077f4a-b5a7-419d-b375-b3216c30e32d","storeId":"store-1","programmeId":null,"title":"Audit freezers","status":"TODO","priority":"MEDIUM","category":"GENERAL","assigneeId":null,"ownerId":null}$ $


$ # 1. manager succeeds (duplicates count once)
curl -s -i -X POST localhost:8080/api/activities/bulk-status -H "$H" -H 'X-Staff-Id: s1' \
  -d '{"activityIds":["ca137179-36be-4c9a-92b1-c60cb1c1c450","1f077f4a-b5a7-419d-b375-b3216c30e32d","ca137179-36b$ e-4c9a-92b1-c60cb1c1c450"],"status":"DONE"}'
> HTTP/1.1 200 
Content-Type: application/json
Transfer-Encoding: chunked
Date: Sun, 25 Oct 2026 14:52:48 GMT

[{"id":"ca137179-36be-4c9a-92b1-c60cb1c1c450","storeId":"store-1","programmeId":null,"title":"Restock aisle 3","status":"DONE","priority":"MEDIUM","category":"GENERAL","assigneeId":null,"ownerId":null},{"id":"1f077f4a-b5a7-419d-b375-b3216c30e32d","storeId":"store-1","programmeId":null,"title":"Audit freezers","status":"DONE","priority":"MEDIUM","category":"GENERAL","assigneeId":null,"ownerId":null}]$

$ # 2. non-manager is refused
curl -s -i -X POST localhost:8080/api/activities/bulk-status -H "$H" -H 'X-Staff-Id: s2' \
  -d '{"activityIds":["ca137179-36be-4c9a-92b1-c60cb1c1c450"],"status":"TODO"}'
$ > HTTP/1.1 403 
Content-Type: application/json
Transfer-Encoding: chunked
Date: Sun, 25 Oct 2026 14:54:19 GMT

{"code":"FORBIDDEN","message":"Only a store manager can change activity statuses in bulk","statusCode":403}$

$ # 3. one unknown id rejects the whole batch
curl -s -i -X POST localhost:8080/api/activities/bulk-status -H "$H" -H 'X-Staff-Id: s1' \
  -d '{"activityIds":["ca137179-36be-4c9a-92b1-c60cb1c1c450","ghost"],"status":"TODO"}'
$ > HTTP/1.1 404 
Content-Type: application/json
Transfer-Encoding: chunked
Date: Sun, 25 Oct 2026 14:56:26 GMT

{"code":"NOT_FOUND","message":"Activity not found: ghost","statusCode":404}$

```

## Not done
Cloud deployment (AWS, Azure, GCP) was not attempted.
