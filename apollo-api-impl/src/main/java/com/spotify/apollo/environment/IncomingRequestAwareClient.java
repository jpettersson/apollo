/*
 * -\-\-
 * Spotify Apollo API Implementations
 * --
 * Copyright (C) 2013 - 2015 Spotify AB
 * --
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * -/-/-
 */
package com.spotify.apollo.environment;

import com.spotify.apollo.Client;
import com.spotify.apollo.Request;
import com.spotify.apollo.Response;

import java.io.Closeable;
import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.CompletionStage;

import okio.ByteString;

/**
 * An Apollo service client for calls that optionally originate from an incoming {@link Request}.
 *
 * This client interface is meant to be used by modules that decorate the client. Application code
 * will always only see the plain {@link Client} that then either is scoped or unscoped.
 *
 * See {@link ClientDecorator}
 */
@FunctionalInterface
public interface IncomingRequestAwareClient {

  /**
   * Send a Request with an optional originating Request and get an asynchronous Response as a
   * CompletionStage.
   *
   * @param request   the request to send
   * @param incoming  optional originating request that created this request
   * @return a CompletionStage that completes normally with a {@link Response<ByteString>},
   *     or completes exceptionally if there is a failure sending the request.
   *     An error status code returned by the service is a normal completion.
   */
  CompletionStage<Response<ByteString>> send(Request request, Optional<Request> incoming);

  /**
   * @return A {@link Client} that which makes all calls to {@link #send(Request, Optional)}
   * with an {@link Optional#empty()} second incoming request argument.
   */
  default Client asUnawareClient() {
    return new UnawareClient(this);
  }

  /**
   * Wraps a request and returns a {@link Client} where all calles use the wrapped request for the
   * incoming argument.
   *
   * @param request  The request to wrap
   * @return A client scoped to the wrapped request
   */
  default Client wrapRequest(Request request) {
    return new RequestScopedClient(this, request);
  }
}
