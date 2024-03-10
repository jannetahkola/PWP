#!/bin/bash
echo "Working directory: $PWD"
ls -la
exec "$@"