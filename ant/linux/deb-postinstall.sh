#!/bin/bash
set -e

INSTALL_DIR="/opt/agente-impressao"
LOG_FILE="/tmp/agente-impressao-install.log"

cd "$INSTALL_DIR"

> "$LOG_FILE"

run_task() {
    echo "Running $1 task..."
    "$INSTALL_DIR/agente-impressao" "$@" &>> "$LOG_FILE" && ret=$? || ret=$?
    if [ $ret -ne 0 ] && [ "$1" != "spawn" ]; then
        echo "Task $1 failed. See $LOG_FILE"
        exit 1
    fi
}

run_task preinstall
run_task install --dest "$INSTALL_DIR"
run_task certgen

# Symlink para /usr/local/bin
ln -sf "$INSTALL_DIR/agente-impressao" /usr/local/bin/agente-impressao 2>/dev/null || true

update-desktop-database &>/dev/null &

run_task spawn "$INSTALL_DIR/agente-impressao"
