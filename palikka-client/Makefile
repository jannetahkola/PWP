docker-run:
	docker-compose up -d --build --force-recreate
	docker-compose logs -f

docker-kill:
	docker-compose down --remove-orphans

docker-reset:
	docker-compose down --remove-orphans
	docker-compose rm -f
	#docker builder prune