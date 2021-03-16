FROM node:15
COPY . /build

RUN apt-get update && apt-get install clojure -yqq --no-install-recommends
ADD  https://github.com/ethereum/solidity/releases/download/v0.4.24/solc-static-linux /bin
ADD https://raw.githubusercontent.com/technomancy/leiningen/stable/bin/lein /bin

RUN ln -s /bin/solc-static-linux /usr/bin/solc
RUN chmod a+x /bin/solc-static-linux /usr/bin/solc /bin/lein


WORKDIR /build
RUN npm set progress=false \
    &&  npm i -s -y

RUN  npm i -g -y -s truffle http-server
RUN truffle compile
RUN cp resources/external-abi/* resources/public/contracts/build/

RUN npx webpack --mode development
RUN lein build-css
RUN lein build-prod-ui
ENV PORT=80
EXPOSE 80
CMD ["http-server", "./resources/public"]
