#!/usr/bin/env bash
set -eo pipefail

declare -a CONTAINERS


strip_gwbk() {
    ZIP_FILE="$1"
    zip -q -d "${ZIP_FILE}" "projects/" > /dev/null 2>&1 || \
        if [[ ${ZIP_EXIT_CODE:=$?} == 12 ]]; then \
            echo "INFO: No projects folder found in gwbk ${ZIP_FILE}."; \
        else \
            echo "ERROR: Unknown error (${ZIP_EXIT_CODE}) encountered during interaction with ${ZIP_FILE}, exiting." && \
            exit "${ZIP_EXIT_CODE}"; \
        fi
}


take_backup() {
	local container_name="$1"

	docker exec "${container_name}" bash gwcmd.sh -b backup.gwbk -y > /dev/null 2>&1
}

# shellcheck disable=SC2207
set_container_names() {
	mapfile -t temp_containers < <(docker-compose ps -q | xargs docker inspect --format '{{ index .Config.Labels "com.docker.compose.service" }} {{ .Name }} {{ .Config.Image }}' | grep -E '/ignition-docker|/ignition' | sed 's/\///g' | awk '{print $1, $2}')
	CONTAINERS=($(printf '%s\n' "${temp_containers[@]}" | awk '{print $2, $1}')) 
}

take_container_backups() {
	for ((i=0; i<${#CONTAINERS[@]}; i+=2)); do
		take_backup "${CONTAINERS[i]}"
	done
}

copy_backups_to_host() {
	mkdir -p backups
	for ((i=0; i<${#CONTAINERS[@]}; i+=2)); do
		docker cp "${CONTAINERS[i]}:/usr/local/bin/ignition/backup.gwbk" "backups/${CONTAINERS[i+1]}.gwbk"
	done
}

strip_projects_from_backups() {
	for ((i=0; i<${#CONTAINERS[@]}; i+=2)); do
		strip_gwbk "backups/${CONTAINERS[i+1]}.gwbk"
	done
}

main() {

	set_container_names
	echo "Taking backups of $(( ${#CONTAINERS[@]} / 2)) containers"
	take_container_backups
	echo "Copying backups to host"
	copy_backups_to_host
	echo "Stripping projects from backups"
	strip_projects_from_backups

	exit 0
}

main