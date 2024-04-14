build:
	chmod +x ./mvnw
	./mvnw clean package -DskipTests

build-test:
	chmod +x ./mvnw
	./mvnw clean verify

docker-run:
	docker-compose up -d --build --force-recreate \
		postgres \
		redis \
		palikka-users-api \
		palikka-game-api \
		palikka-mock-file-server
	docker-compose logs -f

docker-kill:
	docker-compose down --remove-orphans

docker-logs:
	docker-compose logs -f

docker-reset:
	- docker-compose down --remove-orphans
	- docker-compose rm -f
	- docker volume rm $(shell docker volume ls -q)

init-data:
	docker-compose up -d --build --force-recreate postgres
	sleep 5
	docker exec -i postgres psql -Uuser palikka_users < ./conf/data/users/truncate_data.sql
	docker exec -i postgres psql -Uuser palikka_users < ./conf/data/users/schema.sql
	docker exec -i postgres psql -Uuser palikka_users < ./conf/data/users/seed_privileges.sql
	docker exec -i postgres psql -Uuser palikka_users < ./conf/data/users/seed_roles.sql
	docker exec -i postgres psql -Uuser palikka_users < ./conf/data/users/seed_users.sql