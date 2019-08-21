.PHONY: help composeUp deploy

help: ## print this help
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | sort | awk 'BEGIN {FS = ":.*?## "}; {printf "\033[36m%-30s\033[0m %s\n", $$1, $$2}'

composeUp: ## create and start the containers
	docker-compose up -d

deploy: ## deploy the artifact to the Sonatype repository
	mvn clean deploy -Prelease
