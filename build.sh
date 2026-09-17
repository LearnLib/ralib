#!/usr/bin/env bash
set -euo pipefail

build_ralib=0
build_dtlsfuzzer=0
build_dtlssuts=0

print_usage() {
  echo "Usage $0 [--ralib][--dtlsfuzzer][--dtlssuts][-h|--help]"
  echo "Where:"
  echo " --ralib builds RALib;"
  echo " --dtlsfuzzer builds DTLS-Fuzzer;"
  echo " --dtlssuts builds DTLS SUTs;"
  echo " -h|--help prints usage instructions and exits gracefully."
}
while [[ $# -gt 0 && "$1" =~ ^- ]]; do case $1 in
    --ralib )
        build_ralib=1
        ;;
    --dtlsfuzzer )
        build_dtlsfuzzer=1
        ;;
    --dtlssuts )
        build_dtlssuts=1
        ;;
    -h | --help )
        print_usage
        exit 0
        ;;
    * )
        break
        ;;
    esac; shift; done

if (( build_ralib )); then 
    # Build RALib
    (
        cd ralib 
        mvn install assembly:single -DskipTests
    ) || {
        echo "RALib build failed" >&2
        exit 1
    }
fi

if (( build_dtlsfuzzer )); then 
    # Build DTLS-Fuzzer
    (
        cd dtls-fuzzer 
        # Deleting PSF and TLS-Attacker folders to force DTLS-Fuzzer to skip tests
        rm -rf ProtocolState-Fuzzer TLS-Attacker  
        ./install.sh
    ) || {
        echo "DTLS-Fuzzer build failed" >&2
        exit 1
    }
fi

if (( build_dtlssuts )); then 
    # Build five SUTS
    (
        cd dtls-fuzzer 
        # Deletings the folder with installed SUTs
        rm -rf suts
        ./setup_sut.sh mbedtls-2.26.0 openssl-1.1.1k  piondtls-2.0.9  scandium-2.6.2  wolfssl-5.7.6
    ) || {
        echo "Building SUTs failed" >&2
        exit 1
    }
fi