
FROM node:15
ARG BUILD_ENV
ENV DISTRICT_HEGEX_ENV=${BUILD_ENV}
ENV PORT=80

COPY . /build
RUN apt-get update && apt-get install clojure=1.* -yqq --no-install-recommends \
        && apt-get clean \
        && rm -rf /var/lib/apt/lists/*
ADD  https://github.com/ethereum/solidity/releases/download/v0.4.24/solc-static-linux /bin
ADD https://raw.githubusercontent.com/technomancy/leiningen/stable/bin/lein /bin

RUN ln -s /bin/solc-static-linux /usr/bin/solc
RUN chmod a+x /bin/solc-static-linux /usr/bin/solc /bin/lein

WORKDIR /build
RUN npm set progress=false && npm i -y && npm i -g -y -s truffle http-server
RUN truffle compile
RUN cp resources/external-abi/* resources/public/contracts/build/
RUN npx webpack
RUN lein build-prod-ui

EXPOSE 80
CMD ["http-server", "./resources/public"]
