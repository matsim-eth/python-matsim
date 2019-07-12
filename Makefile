PYTHON=python3.6

.PHONY: all maven python python_pbf

all: maven python examples

maven:
	cd java/ && mvn install -DskipTests=true

python_pbf: maven | java/venv
	cd java/ && ./generate_python_stubs.sh
	cp -v java/target/generated-sources/python/*_pb2.py* python/

%/venv:
	cd $(@D) && virtualenv -p $(PYTHON) venv/ && venv/bin/pip install -r requirements.txt

python: python_pbf | python/venv
	cd python &&  venv/bin/python setup.py build egg_info install

python_dist: python_pbf | python/venv
	cd python && venv/bin/python setup.py build egg_info sdist bdist_wheel

examples: python | examples/venv
	cd examples && \
		venv/bin/pip install --no-binary :all: -e ../python/ && \
		venv/bin/pip freeze | grep '^-e' > requirements.txt
