SONAR_BASE_URL=http://localhost:9000/api

build:
	chmod +x ./mvnw
	./mvnw clean package -DskipTests

build-test:
	chmod +x ./mvnw
	./mvnw clean verify

sonar-report:
	chmod +x ./mvnw
	./mvnw sonar:sonar -Dsonar.login=$(shell cat sonar.txt)

docker-run:
	docker-compose up -d --build --force-recreate \
		postgres \
		redis \
		palikka-users-api \
		palikka-game-api \
		palikka-mock-file-server
	docker-compose logs -f

docker-run-sonar:
	docker-compose up -d postgres sonarqube
	@echo
	@echo "Starting PostgresSQL & SonarQube"
	@while [ -z "$$(docker-compose logs sonarqube | grep 'SonarQube is operational')" ]; \
	do \
	  echo "Waiting for 3 seconds..."; \
	  sleep 3; \
	done
	@echo
	@echo "SonarQube ready"
	@echo "Changing SonarQube default password"
	@if [ "$$(curl -w '%{http_code}\n' -X POST "$(SONAR_BASE_URL)/authentication/login?login=admin&password=pass")" == 200 ]; \
	then \
	  	echo ; \
		echo "Password already changed. Login at http://localhost:9000 with admin/pass"; \
	else \
	  	if [ "$$(curl -w '%{http_code}\n' -u admin:admin "$(SONAR_BASE_URL)/users/change_password?login=admin&password=pass&previousPassword=admin")" == 204 ]; \
	  	then \
			echo ; \
			echo "Success! Generating user token"; \
			curl -v -u admin:pass -X POST $(SONAR_BASE_URL)/user_tokens/generate?name=palikka | grep -o 'squ_[^"]*' > sonar.txt; \
			echo ; \
			echo "Success! Login at http://localhost:9000 with admin/pass"; \
		else \
		  echo ; \
  		  echo "Failed to change default password. Run the curl command again with -v for details"; \
  		fi \
	fi

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