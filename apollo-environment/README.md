# Apollo Environment

The `apollo-environment` module lets you create a fully functional
[`Environment`](../apollo-api/src/main/java/com/spotify/apollo/Environment.java) for an
[`AppInit`](../apollo-api/src/main/java/com/spotify/apollo/AppInit.java).

The environment will contain a `RoutingEngine` and a configurable, managed
[`Client`](../apollo-api/src/main/java/com/spotify/apollo/Client.java).

The main module is `ApolloEnvironmentModule` providing an `ApolloEnvironment` instance that can be
used to initialize an `AppInit` instance, returning a `RequestHandler` to be used with any
server module (for example the [`jetty-http-server`](../modules/jetty-http-server)).

## Configuration

key | type | required | note
--- | --- | --- | ---
`apollo.backend` | string | optional | eg., `example.org`
`apollo.logIncomingRequests` | boolean | optional | default `true`


## Example

```java
public static void main(String[] args) {
  final Service service = Services.usingName("ping")
      .withModule(HttpServerModule.create())
      .withModule(ApolloEnvironmentModule.create())
      .build();

  try (Service.Instance i = service.start(args)) {
    // Create the application (possible to get instances from i.resolve())
    final Application app = new App();

    // Create Environment and call App.init(env)
    final ApolloEnvironment env = ApolloEnvironmentModule.environment(i);
    final RequestHandler handler = env.initialize(app);

    // Create servers
    final HttpServer httpServer = HttpServerModule.server(i);

    // Servers will not be bound until these calls
    httpServer.start(handler);

    i.waitForShutdown();
  } catch (InterruptedException | IOException e) {
    // handle errors
  }
}
```

For a runnable example, see [`MinimalRunner`]
(../apollo-http-service/src/test/java/com/spotify/apollo/httpservice/MinimalRunner.java)


## Extending incoming/outgoing request handling

`ApolloEnvironmentModule` has a few extension points that allow 3rd party modules to
decorate internal components involved in incoming/outgoing request handling.


### IncomingRequestAwareClient
One important aspect of the Apollo [`Client`](../apollo-api/src/main/java/com/spotify/apollo/Client.java)
is that it does not come with any protocol support out of the box. Instead, support for
different protocols should be added by modules. These modules do so by injecting themselves
into the [`IncomingRequestAwareClient`](../apollo-api-impl/src/main/java/com/spotify/apollo/environment/IncomingRequestAwareClient.java) decoration chain.

The decoration chain looks like:

1. [`OutgoingCallsGatheringClient`](../apollo-api-impl/src/main/java/com/spotify/apollo/meta/OutgoingCallsGatheringClient.java)
1. [`ServiceSettingClient`](../apollo-environment/src/main/java/com/spotify/apollo/environment/ServiceSettingClient.java)
1. [`[IncomingRequestAwareClient]*`](../apollo-api-impl/src/main/java/com/spotify/apollo/environment/IncomingRequestAwareClient.java) <- [`Set<ClientDecorator>`](../apollo-api-impl/src/main/java/com/spotify/apollo/environment/ClientDecorator.java)
1. [`NoopClient`](../apollo-environment/src/main/java/com/spotify/apollo/environment/NoopClient.java)


### RequestRunnableFactory

1. [`[RequestRunnableFactory]*`](../apollo-api-impl/src/main/java/com/spotify/apollo/request/RequestRunnableFactory.java) <- [`Set<RequestRunnableFactoryDecorator>`](../apollo-environment/src/main/java/com/spotify/apollo/environment/RequestRunnableFactoryDecorator.java)
1. [`RequestRunnableImpl`](../apollo-api-impl/src/main/java/com/spotify/apollo/request/RequestRunnableImpl.java)


### EndpointRunnableFactory

1. [`TrackingEndpointRunnableFactory`](../apollo-api-impl/src/main/java/com/spotify/apollo/request/TrackingEndpointRunnableFactory.java)
1. [`[EndpointRunnableFactory]*`](../apollo-api-impl/src/main/java/com/spotify/apollo/request/EndpointRunnableFactory.java) <- [`Set<EndpointRunnableFactoryDecorator>`](../apollo-environment/src/main/java/com/spotify/apollo/environment/EndpointRunnableFactoryDecorator.java)
1. [`EndpointInvocationHandler`](../apollo-api-impl/src/main/java/com/spotify/apollo/dispatch/EndpointInvocationHandler.java)


### RequestHandler
This is what is ultimately created from the
[`ApolloEnvironmentModule`](../apollo-environment/src/main/java/com/spotify/apollo/environment/ApolloEnvironmentModule.java). It will use the
[`RequestRunnableFactory`](../apollo-api-impl/src/main/java/com/spotify/apollo/request/RequestRunnableFactory.java),
[`EndpointRunnableFactory`](../apollo-api-impl/src/main/java/com/spotify/apollo/request/EndpointRunnableFactory.java)
and [`IncomingRequestAwareClient`](../apollo-api-impl/src/main/java/com/spotify/apollo/environment/IncomingRequestAwareClient.java)
decoration chains that were constructed above. See [`RequestHandlerImpl`](../apollo-api-impl/src/main/java/com/spotify/apollo/request/RequestHandlerImpl.java)
for how they are used, but in short terms it's something like:

```java
RequestHandler requestHandler = ongoingRequest ->
    rrf.create(ongoingRequest).run((ongoingRequest, match) ->
        erf.create(ongoingRequest, requestContext, endpoint).run());
```


### Injecting decorators
To contribute to any of the sets of decorators mentioned above, use Guice Multibinder.

Here's an example of how a `ClientDecorator` is injected:

```java
@Override
protected void configure() {
  Multibinder.newSetBinder(binder(), ClientDecorator.class)
      .addBinding().toProvider(HttpClientDecoratorProvider.class);
}
```
