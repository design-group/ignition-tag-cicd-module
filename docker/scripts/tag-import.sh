#!/bin/bash
set -eo pipefail

#####################
# Tag Import Script #
# Version: 4.0      #
# Date: 2024-04-05  #
#####################

# Set default values for optional arguments
DEFAULT_PROVIDER="default"
DEFAULT_POLICY="o"

# Initialize variables
PROVIDER="$DEFAULT_PROVIDER"
POLICY="$DEFAULT_POLICY"
RESTORED_FILES=0
AUTO_PROVIDER=false
FORCE=false
DEBUG=false

# Function to display script usage
display_help() {
  echo "Usage: $0 [options] <gateway_base_url>"
  echo "Options:"
  echo "  -p, --provider <provider>   Set the provider (default: $DEFAULT_PROVIDER)"
  echo "  -c, --policy <policy>       Set the collision policy (default: $DEFAULT_POLICY)"
  echo "  -d, --directory <path>      Specify the directory path to import"
  echo "  -a, --auto                  Automatically import tags from all providers in the specified directory"
  echo "  --force                     Force overwrite if collision policy is set to 'o'"
  echo "  --debug                     Enable debug mode"
  echo "  -h, --help                  Display this help message"
  exit 0
}

parse_arguments() {
  # If -d or --directory is not specified, flag error and exit
  if [ -z "$DIRECTORY_PATH" ]; then
    echo "Error: Directory path is required. Use --directory or -d to specify the directory path."
    exit 1
  fi

  # If --policy is set to 'o' or 'd' and --force != true, confirm overwrite
  if [[ ("$POLICY" == "o" || "$POLICY" == "d") && "$FORCE" == "false" ]]; then
    read -r -p "You've set POLICY to 'o' (overwrite) or 'd' (delete overwrite). Do you want to proceed? (y/n): " response
    case "$response" in
      [yY][eE][sS]|[yY])
        :
        ;;
      *)
        exit 1
        ;;
    esac
  fi
}

construct_url() {
  local gateway_base_url="$1"
  local provider="$2"
  echo "${gateway_base_url}/data/tag-cicd/tags/import?provider=${provider}&baseTagPath=&collisionPolicy=${POLICY}&recursive=true&individualFilesPerObject=true&filePath=${DIRECTORY_PATH}/"
}

import_tags() {
  local gateway_base_url="$1"
  local provider="$2"
  local directory_path="$3"

  if [ "$DEBUG" = true ]; then echo "Importing directory: $directory_path"; fi
  curl_response=$(curl -sS -kX POST "$(construct_url "$gateway_base_url" "$provider")")
  RESTORED_FILES=$((RESTORED_FILES+1))
  good_count=$(count=$(echo "$curl_response" | grep -o 'Good' | wc -l); echo "${count:-0}")
  bad_failure_count=$(count=$(echo "$curl_response" | grep -o 'Bad_Failure' | wc -l); echo "${count:-0}")

  echo "Tag import for $directory_path: Good ($good_count) Bad_Failure ($bad_failure_count)"
  if [ "$DEBUG" = true ]; then echo "  Response: $curl_response"; fi
}

auto_import_tags() {
  local gateway_base_url="$1"
  local directory_path="$2"
  local provider

  for provider_dir in "$directory_path"/*; do
    if [ -d "$provider_dir" ]; then
      provider=$(basename -- "$provider_dir")
      if [ "$DEBUG" = true ]; then echo "Importing $provider_dir"; fi
      import_tags "$gateway_base_url" "$provider" "$provider_dir"
    fi
  done
}

while [[ $# -gt 0 ]]; do
  key="$1"
  case $key in
    -p|--provider)
      PROVIDER="$2"
      shift
      shift
      ;;
    -c|--policy)
      POLICY="$2"
      shift
      shift
      ;;
    -d|--directory)
      DIRECTORY_PATH="$2"
      shift
      shift
      ;;
    -a|--auto)
      AUTO_PROVIDER=true
      shift
      ;;
    --force)
      FORCE=true
      shift
      ;;
    --debug)
      DEBUG=true
      shift
      ;;
    -h|--help)
      display_help
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

main() {
  parse_arguments

  echo "Running tag import with the following parameters:"
  echo "  Gateway Base URL: $GATEWAY_BASE_URL"
  echo "  Directory Path: $DIRECTORY_PATH"
  echo "  Provider: ${PROVIDER:-default (Default)}"
  echo "  Policy: ${POLICY:-o (Default)}"
  echo -e "  Auto: ${AUTO_PROVIDER}\n"

  if [ "$AUTO_PROVIDER" = true ]; then
    auto_import_tags "$GATEWAY_BASE_URL" "$DIRECTORY_PATH"
  else
    import_tags "$GATEWAY_BASE_URL" "$PROVIDER" "$DIRECTORY_PATH"
  fi

  echo -e "\nRestored $RESTORED_FILES directory(s)."
}

main