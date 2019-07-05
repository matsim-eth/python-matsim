#!/bin/sh

virtualenv -p python3.6 venv
source venv/bin/activate
pip install mypy-protobuf protobuf
protoc --proto_path=src/main/protobuf/ --mypy_out=target/generated-sources/python/ src/main/protobuf/*.proto
