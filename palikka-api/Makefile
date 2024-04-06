test=1

build:
	chmod +x ./mvnw
ifeq ($(test),1)
	./mvnw clean package
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

docker-logs:
	docker-compose logs -f

docker-reset:
	- docker-compose down --remove-orphans
	- docker volume rm $(shell docker volume ls -q)

init-data:
	docker exec -i postgres psql -Uuser palikka_users < ./conf/data/users/truncate_data.sql
	docker exec -i postgres psql -Uuser palikka_users < ./conf/data/users/seed_privileges.sql
	docker exec -i postgres psql -Uuser palikka_users < ./conf/data/users/seed_roles.sql
	docker exec -i postgres psql -Uuser palikka_users < ./conf/data/users/seed_users.sql