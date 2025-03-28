#!/bin/bash

# Script to measure Gradle test execution time
echo "Starting Gradle test timer..."

# Record the start time
start_time=$(date +%s.%N)

# Run gradle test
echo "Running 'gradle test'..."
# Save the current directory
CURRENT_DIR=$(pwd)
# Navigate to the parent directory
cd "$(dirname "$0")/.." || { echo "Failed to change directory"; exit 1; }
# Run gradle with proper error handling
./gradlew cleanTest test || { echo "Gradle tests failed"; cd "$CURRENT_DIR" || true; exit 1; }
# Return to the original directory
cd "$CURRENT_DIR" || { echo "Failed to return to original directory"; exit 1; }

# Record the end time
end_time=$(date +%s.%N)

# Calculate the runtime
runtime=$(echo "$end_time - $start_time" | bc)

# Format the runtime nicely with different units
total_seconds=$(printf "%.2f" $runtime)
minutes=$(echo "$runtime / 60" | bc)
seconds=$(echo "$runtime % 60" | bc)

# Print the results
echo "----------------------------------------"
echo "Gradle test execution completed!"
echo "Total runtime: $total_seconds seconds = $minutes minutes and $seconds seconds"
echo "----------------------------------------"
