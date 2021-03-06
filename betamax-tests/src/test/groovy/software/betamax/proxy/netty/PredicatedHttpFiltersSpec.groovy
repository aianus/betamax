/*
 * Copyright 2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package software.betamax.proxy.netty

import com.google.common.base.Predicate
import io.netty.handler.codec.http.DefaultHttpRequest
import io.netty.handler.codec.http.DefaultHttpResponse
import io.netty.handler.codec.http.DefaultLastHttpContent
import io.netty.handler.codec.http.HttpRequest
import org.littleshoot.proxy.HttpFilters
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Unroll

import static io.netty.handler.codec.http.HttpMethod.GET
import static io.netty.handler.codec.http.HttpResponseStatus.OK
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1

@Unroll
class PredicatedHttpFiltersSpec extends Specification {

	@Subject
	PredicatedHttpFilters filters

	Predicate<HttpRequest> predicate = Stub(Predicate)
	def delegate = Mock(HttpFilters)
	def request = new DefaultHttpRequest(HTTP_1_1, GET, "http://freeside.co/betamax")
	def response = new DefaultHttpResponse(HTTP_1_1, OK)
	def httpObj = new DefaultLastHttpContent()

	void setup() {
		filters = new PredicatedHttpFilters(delegate, predicate, request)
	}

	void "Request: #method does not call delegate.#method when the predicate returns false"() {
		given:
		predicate.apply(request) >> false

		when:
		def result = filters."$method" httpObj

		then:
		result == null

		and:
		0 * delegate._

		where:
		method << ["clientToProxyRequest", "proxyToServerRequest"]
	}

	void "Response: #method does not call delegate.#method when the predicate returns false"() {
		given:
		predicate.apply(request) >> false

		when:
		filters."$method" httpObj

		then:
		0 * delegate._

		where:
		method << ["serverToProxyResponse", "proxyToClientResponse"]
	}

	void "Request: #method calls delegate.#method when the predicate returns true"() {
		given:
		predicate.apply(request) >> true

		when:
		def result = filters."$method" httpObj

		then:
		result == response

		and:
		1 * delegate."$method"(httpObj) >> response

		where:
		method << ["clientToProxyRequest", "proxyToServerRequest"]
	}

	void "Response: #method calls delegate.#method when the predicate returns true"() {
		given:
		predicate.apply(request) >> true

		when:
		filters."$method" httpObj

		then:
		1 * delegate."$method"(httpObj)

		where:
		method << ["serverToProxyResponse", "proxyToClientResponse"]
	}

}
