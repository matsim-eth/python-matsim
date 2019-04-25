.PHONY: all maven python python_pbf

all: maven python

maven:
	cd java/ && mvn assembly:assembly

python_pbf: maven
	cp -v java/target/python/EventBuffer_pb2.py java/target/python/events_pb2.py python/pythonmatsim/protobuf/
	sed -i 's,from proto,from .,g' python/pythonmatsim/protobuf/EventBuffer_pb2.py

python: python_pbf
