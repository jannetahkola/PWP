build:
	chmod +x ./mvnw
	./mvnw clean package

run-docker:
	docker-compose up -d --build --force-recreate && docker-compose logs -f

kill-docker:
	docker-compose down --remove-orphans