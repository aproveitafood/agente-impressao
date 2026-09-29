FROM bellsoft/liberica-openjdk-debian:25 AS build
RUN apt-get update && apt-get install -y --no-install-recommends ant makeself && rm -rf /var/lib/apt/lists/*
COPY . /usr/src/agente-impressao
WORKDIR /usr/src/agente-impressao
RUN ant makeself

FROM bellsoft/liberica-openjre-debian:25 AS install
RUN apt-get update && apt-get install -y --no-install-recommends libglib2.0-bin && rm -rf /var/lib/apt/lists/*
COPY --from=build /usr/src/agente-impressao/out/*.run /tmp/
RUN find /tmp -name "*.run" -exec sh -c '{} -- --user' \;
WORKDIR /opt/agente-impressao
ENTRYPOINT ["/opt/agente-impressao/agente-impressao"]
