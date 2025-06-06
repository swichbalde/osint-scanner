# Docker set up
To start whole app you need to execute following command. It will spin up 4 containers (app, db and 2 tools). 
CLI app will be accessible in a second
```shell
docker compose up --build 
```
By default, app container will keep running until you kill the process.
## Scan mode
Run following command to scan `example.com` domain
```shell
docker compose exec app java -jar app-all.jar scan example.com
```

## Retrieve mode
1. Take "${scanId}" from DB or output from previous execution 
2. Run one of following commands
Excel output (file can be found in "{project_root}/scan-results" folder):
```shell
docker compose exec app java -jar app-all.jar retrieve -o excel "${scanId}"
```
stdout output
```shell
docker compose exec app java -jar app-all.jar retrieve -o stdout "${scanId}"
```

## Stop and clear docker containers and volumes
```shell
docker compose down -v
```
