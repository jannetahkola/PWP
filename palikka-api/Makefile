test=1

build:
	chmod +x ./mvnw
ifeq ($(test),1)
	./mvnw clean verify
else
	./mvnw clean package -DskipTests
endif

build-coverage:
	chmod +x ./mvnw
	./mvnw clean verify

docker-run:
	docker-compose up -d --build --force-recreate && docker-compose logs -f

docker-kill:
	docker-compose down --remove-orphans

reset-data:
	docker volume rm palikka-api_postgres
	docker volume rm palikka-api_redis

init-data:
	docker exec -i postgres psql -Uuser palikka_users < ./conf/data/reset.sql
	docker exec -i postgres psql -Uuser palikka_users < ./conf/data/seed_privileges.sql
	docker exec -i postgres psql -Uuser palikka_users < ./conf/data/seed_roles.sql
	docker exec -i postgres psql -Uuser palikka_users < ./conf/data/seed_users.sql