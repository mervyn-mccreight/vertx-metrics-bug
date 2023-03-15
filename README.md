# vertx-metrics-bug

Project to showcase a bug with missing metrics when using `setServerRequestTagsProvider` with different tags.  
Related issue: https://github.com/vert-x3/vertx-micrometer-metrics/issues/173

## Update

Deeper analysis showed that this is actually not a bug in vert.x, but an issue with the Prometheus Java-Client (https://github.com/prometheus/client_java/issues/696).


