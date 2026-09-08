FROM maven:3.9.16-eclipse-temurin-21-noble

RUN apt-get update \
    && apt-get install -y --no-install-recommends \
        bash less python3 latexmk texlive-latex-extra texlive-pictures \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /benchmark

# Initialize the ralib submodule on the host before building the image.
COPY build.sh ./
COPY ralib/ ./ralib/
RUN test -f ralib/pom.xml && bash ./build.sh

COPY scripts/ ./scripts/
COPY experiments/ ./experiments/
COPY configs/ ./configs/
COPY run-*-experiments.sh ./
COPY docker-entrypoint.sh /usr/local/bin/benchmark-entrypoint
RUN mkdir -p results && chmod +x scripts/*.sh run-*-experiments.sh

VOLUME ["/benchmark/results"]
ENTRYPOINT ["bash", "/usr/local/bin/benchmark-entrypoint"]
CMD ["--help"]
