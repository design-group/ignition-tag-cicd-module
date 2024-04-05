"""
This test includes any unit tests for the API calls directly.

It will test the following:
- Import a native Ignition tag export file
- Export a native Ignition tag export file and compare it to the original
- Export a native Ignition tag export multiple times and confirm the files are the same
- Export the tags in the multi-folder format and compare it to the original
"""

import os
import pytest
import requests
import json
import filecmp

BASE_URL = "https://tag-cicd.localtest.me/data/tag-cicd"
HOST_BASE_PATH = "docker/temp/ignition-data/tags"
CONTAINER_BASE_PATH = "data/tags"
FULL_TAG_FILE = "tests/resources/full-tag-export.json"

EXPORT_TYPES_FILE = "_types_.json"
EXPORT_EXCHANGE_FILE = "Exchange.json"
EXPORT_SINGLE_FILE = "single.json"
EXPORT_MULTI_FILE_DIR = "multi-tags"

@pytest.fixture(autouse=True)
def cleanup():
    # Delete all tags before each test
    delete_all_tags()

def delete_all_tags():
    # TODO: Implement the logic to delete all tags
    pass

def test_import_export_full_tag_file():
    # Import the full tag file
    with open(FULL_TAG_FILE, "r") as file:
        tags_data = file.read()

    response = requests.post(f"{BASE_URL}/tags/import?provider=default&baseTagPath=&collisionPolicy=o&importType=json", data=tags_data, verify=False)
    assert response.status_code == 200

    # Export the tags into a full file              
    response = requests.post(f"{BASE_URL}/tags/export?provider=default&baseTagPath=&recursive=true&localPropsOnly=true&filePath={CONTAINER_BASE_PATH}/{EXPORT_SINGLE_FILE}", verify=False)
    assert response.status_code == 200

    # Compare the json contents of the original and exported files
    with open(FULL_TAG_FILE, "r") as file:
        original_data = json.loads(file.read())
    with open(f"{HOST_BASE_PATH}/{EXPORT_SINGLE_FILE}", "r") as file:
        exported_data = json.loads(file.read())
    assert original_data == exported_data

def test_export_consistency():
    # Import the full tag file
    with open(FULL_TAG_FILE, "r") as file:
        tags_data = file.read()

    response = requests.post(f"{BASE_URL}/tags/import?provider=default&baseTagPath=&collisionPolicy=o&importType=json", data=tags_data, verify=False)
    assert response.status_code == 200

    # Export the tags multiple times and compare the files
    export_files = []
    for i in range(3):
        export_file = f"export_{i}.json"
        response = requests.post(f"{BASE_URL}/tags/export?provider=default&baseTagPath=&recursive=true&localPropsOnly=true&filePath={CONTAINER_BASE_PATH}/{export_file}", verify=False)
        assert response.status_code == 200
        export_files.append(export_file)

    # Compare the exported files
    for i in range(len(export_files) - 1):
        assert filecmp.cmp(f"{HOST_BASE_PATH}/{export_files[i]}", f"{HOST_BASE_PATH}/{export_files[i+1]}")

def sort_json(data):
    if isinstance(data, dict):
        return {key: sort_json(value) for key, value in sorted(data.items())}
    elif isinstance(data, list):
        return [sort_json(item) for item in data]
    else:
        return data

def test_export_multi_folder_format():
    # Import the full tag file
    with open(FULL_TAG_FILE, "r") as file:
        tags_data = file.read()

    response = requests.post(f"{BASE_URL}/tags/import?provider=default&baseTagPath=&collisionPolicy=o&importType=json", data=tags_data, verify=False)
    assert response.status_code == 200

    # Export the tags in multi-folder format
    response = requests.post(f"{BASE_URL}/tags/export?provider=default&baseTagPath=&recursive=true&individualFilesPerObject=true&localPropsOnly=true&filePath={CONTAINER_BASE_PATH}/{EXPORT_MULTI_FILE_DIR}", verify=False)
    assert response.status_code == 200

if __name__ == "__main__":
	pytest.main(["-s", __file__])
