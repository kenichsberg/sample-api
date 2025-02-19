ifneq (,$(wildcard ./.env))
	include .env
	export
endif

.EXPORT_ALL_VARIABLES:


test:
	clj -M:test

build:
	clj -X:build uber

repl:
	clj -M:test:nrepl
