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
		rm -f requirements.txt && \
		venv/bin/pip freeze --exclude-editable | xargs -r venv/bin/pip uninstall -y && \
		venv/bin/pip install numpy==1.16.3 && \
		venv/bin/pip install --no-binary :all: -e ../python/ && \
		venv/bin/pip freeze > requirements.txt
