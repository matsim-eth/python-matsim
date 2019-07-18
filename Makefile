PYTHON=python3.6
PYTHON_DIR=$(PWD)

.PHONY: all maven python python_pbf

all: python_dist

maven:
	cd java/ && mvn install -DskipTests=true

python_pbf: maven | java/venv
	cd java/ && ./generate_python_stubs.sh
	cp -v java/target/generated-sources/python/*_pb2.py* ./

%/venv:
	cd $(@D) && virtualenv -p $(PYTHON) venv/ && venv/bin/pip install -r requirements.txt

python: python_dist | $(PYTHON_DIR)/venv
	venv/bin/pip install dist/*

python_dist: python_pbf | $(PYTHON_DIR)/venv
	venv/bin/python setup.py codegen build bdist_wheel

examples: python | examples/venv
	cd examples && \
		venv/bin/pip install --no-binary :all: -e ../ && \
		venv/bin/pip freeze | grep '^-e' > requirements.txt
