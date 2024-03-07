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