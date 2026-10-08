# dist/ is pre-built on the host (`npm run build`) and copied in here rather than built inside
# this image. This machine's Docker network routes npm registry traffic through a corporate
# proxy that fails auth from inside the build context, so `npm ci`/`npm run build` happen on
# the host (where they work) and Docker only packages the static assets behind nginx.
FROM nginx:alpine

COPY dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY docker-entrypoint.sh /docker-entrypoint.sh
RUN chmod +x /docker-entrypoint.sh

EXPOSE 80

ENTRYPOINT ["/docker-entrypoint.sh"]
