.PHONY: all maven python python_pbf

all: maven python

maven:
	cd java/ && mvn assembly:assembly

python_pbf: maven
	cp java/target/python/*_pb2.py python/pythonmatsim/protobuf/

python: python_pbf
