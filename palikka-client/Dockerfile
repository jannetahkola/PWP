# https://kariera.future-processing.pl/blog/dockerfile-good-practices-for-node-and-npm/
# https://dev.to/ayesh_nipun/how-to-dockerize-a-react-application-kpa

FROM node:21 AS builder
RUN mkdir -p /app
WORKDIR /app
COPY package*.json .
RUN npm ci && npm cache clean --force
COPY . .
RUN npm run build

FROM nginx:1.25
WORKDIR /usr/share/nginx/html
RUN rm -rf ./*
COPY --from=builder /app/build .
ENTRYPOINT ["nginx", "-g", "daemon off;"]