SONAR_BASE_URL=http://localhost:9000/api

##@
##@ API services
##@

docker-run: ##@ Start all API service containers. Images are built if needed, but
			##@ cache & existing images are used when available
	# Run socat in case this is the first run
	- $(MAKE) docker-run-socat
	docker build \
		-f deps.Dockerfile \
		-t deps .
	docker build \
		--network=host \
		-f palikka-core.Dockerfile \
		-t build-core-with-deps .
	docker-compose up -d --build
	- docker stop palikka-socat
	docker-compose logs -f

docker-rebuild: ##@ Rebuild all API service images from scratch without cache.
				##@ Existing volume data is kept. Does not start the containers
	docker-compose down
	- $(MAKE) docker-run-socat
	# Uncomment below to also refresh dependencies
	#docker build --no-cache -f deps.Dockerfile -t deps .
	docker build \
		--no-cache --network=host \
		-f palikka-core.Dockerfile \
		-t build-core-with-deps .
	docker-compose build --no-cache
	- docker stop palikka-socat

docker-run-socat:
	# internal Make target to give Testcontainers access to local Docker daemon. Required only when
	# tests are executed.
	# source: https://stackoverflow.com/a/76842771
	# more details: https://stackoverflow.com/a/59934439
	docker run --rm --name palikka-socat -d \
		-p 2375:2375 \
		-v /var/run/docker.sock:/var/run/docker.sock \
		alpine/socat \
		tcp-listen:2375,reuseaddr,fork \
		unix-connect:/var/run/docker.sock \

##@
##@ Data & utility services
##@

init-postgres: ##@ Start the PostgreSQL container & populate it with seed data. Only recreates
			   ##@ the container if executed more than once without reset
	docker-compose -f docker-compose-postgres.yaml up -d --build
	@echo
	@echo "Starting PostgresSQL..."
	@while [ -z "$$(docker-compose -f docker-compose-postgres.yaml logs | grep 'database system is ready')" ]; \
	do \
	  echo "Waiting for 3 seconds..."; \
	  sleep 3; \
	done
	@echo "PostgresSQL ready"
	@echo
	@echo "Creating seed data..."
	docker exec -i postgres psql -Uuser palikka_users < ./conf/data/users/truncate_data.sql
	docker exec -i postgres psql -Uuser palikka_users < ./conf/data/users/schema.sql
	docker exec -i postgres psql -Uuser palikka_users < ./conf/data/users/seed_privileges.sql
	docker exec -i postgres psql -Uuser palikka_users < ./conf/data/users/seed_roles.sql
	docker exec -i postgres psql -Uuser palikka_users < ./conf/data/users/seed_users.sql
	@echo "Seed data ready"

init-sonar: ##@ Start the SonarQube container & set it up. Only recreates
			##@ the container if executed more than once without reset
	docker-compose -f docker-compose-sonar.yaml up -d
	@echo
	@echo "Starting SonarQube..."
	@while [ -z "$$(docker-compose -f docker-compose-sonar.yaml logs | grep 'SonarQube is operational')" ]; \
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

##@
##@ Tools & help
##@

env-dev: ##@ Setup the local environment for development. Rebuilds the Docker images
	$(MAKE) docker-down
	@cp ./conf/dockerfiles/palikka-core.dev.Dockerfile ./palikka-core.Dockerfile
	@cp ./conf/dockerfiles/palikka-game-api.dev.Dockerfile ./palikka-game-api.Dockerfile
	@cp ./conf/dockerfiles/palikka-users-api.dev.Dockerfile ./palikka-users-api.Dockerfile
	$(MAKE) docker-rebuild

env-prod: ##@ Setup the local environment for production. Rebuilds the Docker images. Requires SonarQube to be up
	$(MAKE) docker-down
	@cp ./conf/dockerfiles/palikka-core.Dockerfile .
	@cp ./conf/dockerfiles/palikka-game-api.Dockerfile .
	@cp ./conf/dockerfiles/palikka-users-api.Dockerfile .
	$(MAKE) docker-rebuild

docker-up: ##@ Start all stopped containers
	# Start Postgres first because it manages the custom network
	docker-compose -f docker-compose-postgres.yaml up -d
	docker-compose -f docker-compose-sonar.yaml up -d
	docker-compose up -d
	docker-compose logs -f

docker-down: ##@ Stop all running containers
	docker-compose down
	docker-compose -f docker-compose-sonar.yaml down
	# Stop Postgres last because it manages the custom network
	docker-compose -f docker-compose-postgres.yaml down

docker-reset: ##@ Stop & remove all containers, images & volumes. Essentially
			  ##@ a full clean up of all Docker components created for the
   			  ##@ palikka system
	# https://docs.docker.com/reference/cli/docker/compose/down/
	docker-compose down --rmi=all --volumes
	docker-compose -f docker-compose-sonar.yaml down --rmi=all --volumes
	# Handle Postgres last because it manages the custom network
	docker-compose -f docker-compose-postgres.yaml down --rmi=all --volumes
	- docker stop palikka-socat
	- docker image rm alpine/socat

help: ##@ (Default) Print this listing of key targets with their descriptions
	# source: https://gist.github.com/prwhite/8168133
	@printf "\nUsage: make <command>\n"
	@grep -F -h "##@" $(MAKEFILE_LIST) | grep -F -v grep -F | sed -e 's/\\$$//' | awk 'BEGIN {FS = ":*[[:space:]]*##@[[:space:]]*"}; \
	{ \
		if($$2 == "") \
			printf ""; \
		else if($$0 ~ /^#/) \
			printf "\n%s\n", $$2; \
		else if($$1 == "") \
			printf "     %-20s%s\n", "", $$2; \
		else \
			printf "\n    \033[34m%-20s\033[0m %s\n", $$1, $$2; \
	}'

.DEFAULT_GOAL := help