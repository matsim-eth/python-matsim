#!/bin/sh

source venv/bin/activate
protoc --proto_path=src/main/protobuf/ --mypy_out=target/generated-sources/python/ src/main/protobuf/*.proto
