#!/bin/bash
# Arachne Plus - build and install (macOS / Linux)
cd "$(dirname "$0")" || exit 1

echo
echo "=== Arachne Plus - build and install ==="
echo

if ! command -v java >/dev/null 2>&1; then
  echo "Java is not installed. Minecraft 26.2 mods need Java 25 (JDK) to build."
  echo "Install 'Temurin 25' from https://adoptium.net/temurin/releases/?version=25"
  echo "(macOS with Homebrew: brew install --cask temurin@25), then run this again."
  exit 1
fi
MAJOR=$(java -version 2>&1 | head -n 1 | sed -E 's/.*"([0-9]+).*/\1/')
if [ -n "$MAJOR" ] && [ "$MAJOR" -lt 25 ]; then
  echo "Your Java is version $MAJOR, but Java 25 is needed."
  echo "Install 'Temurin 25' from https://adoptium.net/temurin/releases/?version=25"
  exit 1
fi

echo "Building. The first time this downloads a lot and can take several minutes..."
chmod +x gradlew
./gradlew build || { echo; echo "The build failed. Copy the error text above and report it to the mod author."; exit 1; }

JAR=$(ls build/libs/arachneplus-*.jar | grep -v -- '-sources' | head -n 1)
if [ -z "$JAR" ]; then echo "Could not find the built jar."; exit 1; fi

if [ "$(uname)" = "Darwin" ]; then
  MODS="$HOME/Library/Application Support/minecraft/mods"
else
  MODS="$HOME/.minecraft/mods"
fi
mkdir -p "$MODS"
rm -f "$MODS"/arachneplus-*.jar
cp "$JAR" "$MODS/"
echo
echo "Installed $JAR into: $MODS"
echo
echo "You also need Fabric API for 26.2: https://modrinth.com/mod/fabric-api"
echo "(put that jar in the same mods folder). In game, press Right Shift for settings."
