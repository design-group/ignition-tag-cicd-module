#!/bin/bash
set -eo pipefail

#####################
# Tag Export Script #
# Version: 4.0      #
# Date: 2024-04-05  #
#####################

# Default values
DEFAULT_PROVIDER="default"
DEFAULT_BASE_TAG_PATH=""
DEFAULT_RECURSIVE="true"

# Initialize variables
PROVIDER="$DEFAULT_PROVIDER"
BASE_TAG_PATH="$DEFAULT_BASE_TAG_PATH"
RECURSIVE="$DEFAULT_RECURSIVE"

# Function to display script usage
display_help() {
  echo "Usage: $0 [options] <gateway_base_url>"
  echo "Options:"
  echo "  -p, --provider <provider>     Set the provider (default: $DEFAULT_PROVIDER)"
  echo "  -t, --base-tag-path <path>    Set the base tag path (default: $DEFAULT_BASE_TAG_PATH)"
  echo "  -r, --recursive <true|false>  Set recursive export (default: $DEFAULT_RECURSIVE)"
  echo "  -f, --file-path <path>        Set the file path to export"
  echo "  -h, --help                    Display this help message"
  echo "  -d, --delete-existing         Delete existing tags before exporting"
  exit 0
}

# Parse command line arguments
while [[ $# -gt 0 ]]; do
  key="$1"
  case $key in
    -p|--provider)
      PROVIDER="$2"
      shift
      shift
      ;;
    -t|--base-tag-path)
      BASE_TAG_PATH="$2"
      shift
      shift
      ;;
    -r|--recursive)
      RECURSIVE="$2"
      shift
      shift
      ;;
    -f|--file-path)
      FILE_PATH="$2"
      shift
      shift
      ;;
    -h|--help)
      display_help
      ;;
	-d|--delete-existing)
	  DELETE_EXISTING="true"
	  shift
	  ;;
    *)
      GATEWAY_BASE_URL="$1"
      shift
      ;;
  esac
done

# Check if gateway base URL is provided
if [ -z "$GATEWAY_BASE_URL" ]; then
  echo "Error: Gateway base URL is required."
  display_help
fi

construct_url() {
  local gateway_base_url="$1"
  local provider="$2"
  local base_path="$3"
  local recursive="$4"
  echo "${gateway_base_url}/data/tag-cicd/tags/export?provider=${provider}&recursive=${recursive}&baseTagPath=${base_path}&individualFilesPerObject=true&localPropsOnly=true&filePath=${FILE_PATH}&deleteExisting=${DELETE_EXISTING}"
}

process_url() {
  local gateway_base_url="$1"
  local provider="$2"
  local base_path="$3"
  local recursive="$4"

  URL=$(construct_url "$gateway_base_url" "$provider" "$base_path" "$recursive")
  echo "Constructed URL for $base_path: $URL"

  curl -kX POST "$URL"
}

main() {
  echo "Beginning tag export"
  echo "Gateway Base URL: $GATEWAY_BASE_URL"
  echo "Provider: $PROVIDER"
  echo "Base Tag Path: $BASE_TAG_PATH"
  echo "Recursive: $RECURSIVE"

  process_url "$GATEWAY_BASE_URL" "$PROVIDER" "$BASE_TAG_PATH" "$RECURSIVE"
}

main