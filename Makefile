.PHONY: all maven python python_pbf

all: maven python examples

maven:
	cd java/ && mvn install -DskipTests=true

python_pbf: maven
	cp -v java/target/generated-sources/python/*_pb2.py python/

python: python_pbf
	cd python &&  venv/bin/python setup.py build egg_info install

examples: python
	cd examples && \
		venv/bin/pip install --no-binary :all: -e ../python/ && \
		venv/bin/pip freeze | grep '^-e' > requirements.txt
