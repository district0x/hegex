FROM node:15
COPY . /build
WORKDIR /build
RUN apt-get update && apt-get install clojure -yqq --no-install-recommends
ADD  https://github.com/ethereum/solidity/releases/download/v0.4.24/solc-static-linux /bin
ADD https://raw.githubusercontent.com/technomancy/leiningen/stable/bin/lein /bin
RUN ln -s /bin/solc-static-linux /usr/bin/solc
RUN chmod a+x /bin/solc-static-linux /usr/bin/solc /bin/lein
RUN npm i -g truffle --quiet

RUN npm i --quiet
RUN truffle compile
RUN cp resources/external-abi/* resources/public/contracts/build/

RUN lein build-css
EXPOSE 4177
CMD [" npx webpack --mode development && (echo \"(start-ui\!)\"; cat <&0) | lein repl"]
