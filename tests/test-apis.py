"""
This test includes any unit tests for the API calls directly.

It will test the following:
- Import a native Ignition tag export file
- Export a native Ignition tag export file and compare it to the original
- Export a native Ignition tag export multiple times and confirm the files are the same
- Export the tags in the multi-folder format and compare it to the original
"""

import pytest
import os
import json
import requests

NATIVE_EXPORT_PATH = "resources/native-tag-export.json"
TEST_GATEWAY_ADDRESS = "https://tag-cicd.localtest.me"

EXPORT_TAGS_ENDPOINT = "/data/tag-cicd/tags/export"
IMPORT_TAGS_ENDPOINT = "/data/tag-cicd/tags/import"

def read_json_file(file_path):
	"""
	Reads a JSON file and returns the data as a dictionary
	"""
	with open(file_path, "r") as f:
		data = json.load(f)
	return data

def read_native_export():
	"""
	Reads the original native export file and returns the data as a dictionary
	"""
	return read_json_file(os.path.join(os.path.dirname(__file__), NATIVE_EXPORT_PATH))


def test_native_tag_import():
	"""
	Tests the import of a native Ignition tag export file
	"""
	# Read the original native export file
	original_data = read_native_export()

	# Import the native export file
	import_response = requests.post(TEST_GATEWAY_ADDRESS + IMPORT_TAGS_ENDPOINT + "?collisionPolicy=d", json=original_data, verify=False)

	assert import_response.status_code == 200


def test_native_tag_export():
	"""
	Tests the export of a native Ignition tag export file
	"""
	# Read the original native export file
	original_data = read_native_export()

	# Import the native export file
	export_string = requests.get(TEST_GATEWAY_ADDRESS + EXPORT_TAGS_ENDPOINT + "?recursive=true", verify=False).text
	export_data = json.loads(export_string)
	# Compare the imported data to the original data
	try:
		assert export_data == original_data
	except AssertionError:
		# Write the export data to a file for debugging
		with open("out/failed-export.json", "w") as f:
			json.dump(export_data, f, indent=4)
		
		# Write the original data to a file for debugging
		with open("out/original-export.json", "w") as f:
			json.dump(original_data, f, indent=4)
		raise

if __name__ == '__main__':
	pytest.main(["-s", __file__])