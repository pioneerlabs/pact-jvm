package io.pioneerlabs.microtest.pact.model

import io.pioneerlabs.microtest.pact.model.v3.messaging.MessagePact
import com.amazonaws.services.s3.AmazonS3Client
import com.amazonaws.services.s3.model.S3Object
import com.amazonaws.services.s3.model.S3ObjectInputStream
import groovyx.net.http.AuthConfig
import groovyx.net.http.RESTClient
import spock.lang.Specification

@SuppressWarnings('DuplicateMapLiteral')
class PactReaderSpec extends Specification {

  def setup() {
    GroovySpy(PactReader, global: true)
  }

  def 'loads a pact with no metadata as V2'() {
      given:
      def pactUrl = PactReaderSpec.classLoader.getResource('pact.json')

      when:
      def pact = PactReader.loadPact(pactUrl)

      then:
      1 * PactReader.loadV2Pact(pactUrl, _)
      0 * PactReader.loadV3Pact(pactUrl, _)
      pact instanceof RequestResponsePact
  }

  def 'loads a pact with V1 version using existing loader'() {
      given:
      def pactUrl = PactReaderSpec.classLoader.getResource('v1-pact.json')

      when:
      def pact = PactReader.loadPact(pactUrl)

      then:
      1 * PactReader.loadV2Pact(pactUrl, _)
      0 * PactReader.loadV3Pact(pactUrl, _)
      pact instanceof RequestResponsePact
  }

  def 'loads a pact with V2 version using existing loader'() {
      given:
      def pactUrl = PactReaderSpec.classLoader.getResource('v2-pact.json')

      when:
      def pact = PactReader.loadPact(pactUrl)

      then:
      1 * PactReader.loadV2Pact(pactUrl, _)
      0 * PactReader.loadV3Pact(pactUrl, _)
      pact instanceof RequestResponsePact
  }

  def 'loads a pact with V3 version using V3 loader'() {
      given:
      def pactUrl = PactReaderSpec.classLoader.getResource('v3-pact.json')

      when:
      def pact = PactReader.loadPact(pactUrl)

      then:
      0 * PactReader.loadV2Pact(pactUrl, _)
      1 * PactReader.loadV3Pact(pactUrl, _)
      pact instanceof RequestResponsePact
  }

  def 'loads a message pact with V3 version using V3 loader'() {
      given:
      def pactUrl = PactReaderSpec.classLoader.getResource('v3-message-pact.json')

      when:
      def pact = PactReader.loadPact(pactUrl)

      then:
      1 * PactReader.loadV3Pact(pactUrl, _)
      0 * PactReader.loadV2Pact(pactUrl, _)
      pact instanceof MessagePact
  }

  def 'loads a pact from an inputstream'() {
      given:
      def pactInputStream = PactReaderSpec.classLoader.getResourceAsStream('pact.json')

      when:
      def pact = PactReader.loadPact(pactInputStream)

      then:
      1 * PactReader.loadV2Pact(pactInputStream, _)
      0 * PactReader.loadV3Pact(pactInputStream, _)
      pact instanceof RequestResponsePact
  }

  def 'loads a pact from a json string'() {
    given:
    def pactString = PactReaderSpec.classLoader.getResourceAsStream('pact.json').text

    when:
    def pact = PactReader.loadPact(pactString)

    then:
    1 * PactReader.loadV2Pact(pactString, _)
    0 * PactReader.loadV3Pact(pactString, _)
    pact instanceof RequestResponsePact
  }

  def 'throws an exception if it can not load the pact file'() {
    given:
    def pactString = 'this is not a pact file!'

    when:
    PactReader.loadPact(pactString)

    then:
    thrown(UnsupportedOperationException)
    0 * PactReader.loadV2Pact(pactString, _)
    0 * PactReader.loadV3Pact(pactString, _)
  }

  def 'handles invalid version metadata'() {
    given:
    def pactString = PactReaderSpec.classLoader.getResourceAsStream('pact-invalid-version.json').text

    when:
    PactReader.loadPact(pactString)

    then:
    1 * PactReader.loadV2Pact(pactString, _)
    0 * PactReader.loadV3Pact(pactString, _)
  }

  @SuppressWarnings('UnnecessaryGetter')
  def 'if authentication is set, loads the pact file from a URL with auth'() {
    given:
    def pactUrl = new URL('http://url.that.requires.auth:8080/')
    def mockAuth = Mock(AuthConfig)
    def mockHttp = Mock(RESTClient) {
      getAuth() >> mockAuth
      get(_) >> [data: [:]]
    }
    PactReader.newHttpClient(pactUrl) >> mockHttp

    when:
    def pact = PactReader.loadPact(pactUrl, authentication: ['basic', 'user', 'pwd'])

    then:
    1 * mockAuth.basic('user', 'pwd')
    1 * PactReader.loadV2Pact(pactUrl, _)
    0 * PactReader.loadV3Pact(pactUrl, _)
    pact instanceof RequestResponsePact
  }

  def 'correctly loads V2 pact query strings'() {
    given:
    def pactUrl = PactReaderSpec.classLoader.getResource('v2_pact_query.json')

    when:
    def pact = PactReader.loadPact(pactUrl)

    then:
    pact instanceof RequestResponsePact
    pact.interactions[0].request.query == [q: ['p', 'p2'], r: ['s']]
    pact.interactions[1].request.query == [datetime: ['2011-12-03T10:15:30+01:00'], description: ['hello world!']]
    pact.interactions[2].request.query == [options: ['delete.topic.enable=true'], broker: ['1']]
  }

  def 'correctly load pact file from S3'() {
    given:
    def pactUrl = 'S3://some/bucket/aFile.json'
    def s3ClientMock = Mock(AmazonS3Client)
    String pactJson = this.class.getResourceAsStream('/v2-pact.json').text
    S3Object object = Mock()
    object.objectContent >> new S3ObjectInputStream(new ByteArrayInputStream(pactJson.bytes), null)
    PactReader.s3Client() >> s3ClientMock

    when:
    def pact = PactReader.loadPact(pactUrl)

    then:
    1 * s3ClientMock.getObject('some', 'bucket/aFile.json') >> object
    pact instanceof RequestResponsePact
  }

}
