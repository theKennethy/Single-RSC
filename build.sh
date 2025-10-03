#!/bin/sh
# Build Single-RSC with Maven
# This will compile all code, download dependencies, and create JAR files

echo "========================================="
echo "  Building Single-RSC with Maven"
echo "========================================="
echo ""

# Check if Maven is installed
if ! command -v mvn >/dev/null 2>&1; then
    echo "ERROR: Maven is not installed!"
    echo "Please install Maven first:"
    echo "  Ubuntu/Debian: sudo apt-get install maven"
    echo "  macOS: brew install maven"
    exit 1
fi

# Run Maven build
echo "Running: mvn clean package"
echo ""
mvn clean package

# Check if build succeeded
if [ $? -eq 0 ]; then
    echo ""
    echo "========================================="
    echo "  ✓ Build successful!"
    echo "========================================="
    echo ""
    echo "Output files:"
    echo "  target/rsc.jar (2 MB)"
    echo "  target/rsc-standalone.jar (48 MB)"
    echo ""
    echo "To run the game:"
    echo "  ./run-fx.sh"
    echo ""
else
    echo ""
    echo "========================================="
    echo "  ✗ Build failed!"
    echo "========================================="
    echo ""
    echo "Please check the error messages above."
    exit 1
fi
